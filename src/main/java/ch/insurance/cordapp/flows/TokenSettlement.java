package ch.insurance.cordapp.flows;

import ch.insurance.cordapp.BaseFlow;
import ch.insurance.cordapp.TokenContract;
import ch.insurance.cordapp.TokenState;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.finance.contracts.asset.Cash;

import static net.corda.finance.contracts.GetBalances.getCashBalance;

import java.security.PublicKey;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TokenSettlement {

    /* Our flow, automating the process of updating the ledger.
     * See src/main/java/examples/ArtTransferFlowInitiator.java for an example. */
    @InitiatingFlow
    @StartableByRPC
    public static class TokenSettlementFlow extends BaseFlow<TokenState> {
        private final UniqueIdentifier id;
        private final Amount<Currency> amount;

        public TokenSettlementFlow(UniqueIdentifier id, Amount<Currency> amount) {
            super();
            this.id = id;
            this.amount = amount;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(PREPARATION);

            // We choose our transaction's notary (the notary prevents double-spends).
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // We get a reference to our own identity.
            Party settlerOwner = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - get our TokenState by id!
             *                - validate if amount to settle is available
             *                - validate amount left to settle
             *         example from https://github.com/corda/samples/blob/bb291eeeeef480ce35b3767727e0fd40b8c9e5dc/obligation-cordapp/java-source/src/main/java/net/corda/examples/obligation/flows/SettleObligation.java
             *         process
             *              create new input state based on id
             *              add cash from owner->issuer to the transaction
             * ===========================================================================*/
            // We create our new TokenState.
            StateAndRef<TokenState> tokenInputStateToSettle =  this.getStateByLinearId(
                    TokenState.class, this.id);
            TokenState tokenInputState = this.getStateByRef(tokenInputStateToSettle);
            final Party issuer = tokenInputState.getIssuer();
            final Party owner = tokenInputState.getOwner();

            // Stage 3. Abort if not current owner started this flow.
            if (!settlerOwner.equals(tokenInputState.getOwner())) {
                throw new IllegalStateException("Token settling can only be initiated by the current owner.");
            }

            // Stage 4. Check we have enough cash to settle the requested amount.
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), this.amount.getToken());
            final Amount<Currency> amountLeftToSettle = tokenInputState.getAmount().minus(tokenInputState.getPaid());

            if (cashBalance.getQuantity() <= 0L) {
                throw new FlowException(String.format("Owner has no %s to settle.", amount.getToken()));
            } else if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format(
                        "Owner has only %s but needs %s to settle.", cashBalance, amount));
            } else if (amountLeftToSettle.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format(
                        "There's only %s left to settle but you pledged %s.", amountLeftToSettle, amount));
            }

            /* ============================================================================
             *      TODO 3 - Build our token issuance transaction to update the ledger!
             * ===========================================================================*/

            progressTracker.setCurrentStep(BUILDING);

            // Stage 5. Create a settle command.
            final List<PublicKey> requiredSigners = tokenInputState.getParticipantKeys();
            final Command settleCommand = new Command<>(new TokenContract.Commands.Settle(), requiredSigners);
            final TransactionBuilder builder = new TransactionBuilder(getFirstNotary())
                    .addInputState(tokenInputStateToSettle)
                    .addCommand(settleCommand);

            // Stage 7. Get some cash from the vault and add a spend to our transaction builder.
            // owner pays to issuer
            final List<PublicKey> cashSigningKeys = Cash.generateSpend(
                    getServiceHub(),
                    builder,
                    amount,
                    //getOurIdentityAndCert(),    // from
                    tokenInputState.getIssuer(), // to
                    ImmutableSet.of())
                .getSecond();

            // Stage 8. Only add an output state if the token has not been fully settled.
            final Amount<Currency> amountRemaining = amountLeftToSettle.minus(amount);
            if (amountRemaining.getQuantity() > 0) {
                TokenState outputState = tokenInputState.pay(amount);
                builder.addOutputState(outputState, TokenContract.ID);
            }


            /* ============================================================================
             *          TODO 2 - Write our TokenContract to control token issuance!
             * ===========================================================================*/
            // Stage 6. Verify and sign the transaction.
            // We check our transaction is valid based on its contracts.
            progressTracker.setCurrentStep(SIGNING);
            builder.verify(getServiceHub());

            // add the issuer to the singers beside the current owner from the cash transaction
            final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>()
                    .addAll(cashSigningKeys)
                    .add(tokenInputState.getIssuer().getOwningKey())
                    .build();
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder, signingKeys);


            // Stage 10. Get counterparty signature.
            progressTracker.setCurrentStep(COLLECTING);
            final FlowSession session = initiateFlow(settlerOwner);
            subFlow(new IdentitySyncFlow.Send(session, ptx.getTx()));
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    ImmutableSet.of(session),
                    signingKeys,
                    COLLECTING.childProgressTracker()));

            // We get the transaction notarised and recorded automatically by the platform.
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, ImmutableSet.of(issuer)));
        }
    }

    @InitiatedBy(TokenSettlementFlow.class)
    public static class TokenSettlementResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public TokenSettlementResponder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            subFlow(new IdentitySyncFlow.Receive(otherFlow));
            SignedTransaction stx = subFlow(new BaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
    }

}