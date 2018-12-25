package ch.insurance.cordapp.token;

import ch.insurance.cordapp.BaseFlow;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

public class TokenTransfer {

    /* Our flow, automating the process of updating the ledger.
     * See src/main/java/examples/ArtTransferFlowInitiator.java for an example. */
    @InitiatingFlow
    @StartableByRPC
    public static class TokenTransferFlow extends BaseFlow<TokenState> {
        private final Party newOwner;
        private final UniqueIdentifier id;

        public TokenTransferFlow(Party newOwner, UniqueIdentifier id) {
            super();
            this.newOwner = newOwner;
            this.id = id;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(PREPARATION);

            // We choose our transaction's notary (the notary prevents double-spends).
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // We get a reference to our own identity.
            Party issuer = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - get our TokenState by id!
             *                - create new TokenState based on Input, update owner and set same amount
             * ===========================================================================*/
            // We create our new TokenState.
            StateAndRef<TokenState> tokenInputStateToTransfer =  this.getStateByLinearId(
                    TokenState.class, this.id);
            TokenState tokenInputState = this.getStateByRef(tokenInputStateToTransfer);
            final Party oldIssuer = tokenInputState.getIssuer();
            final Party oldOwner = tokenInputState.getOwner();

            // Stage 3. Abort if not current owner started this flow.
            if (!getOurIdentity().equals(tokenInputState.getOwner())) {
                throw new IllegalStateException("Token transfer can only be initiated by the current owner.");
            }
            progressTracker.setCurrentStep(BUILDING);
            // Stage 4. Create the new obligation state reflecting a new lender.
            // We create our new TokenState.
            TokenState tokenOutputState = tokenInputState.withNewOwner(this.newOwner);

            /* ============================================================================
             *      TODO 3 - Build our token issuance transaction to update the ledger!
             * ===========================================================================*/

            // Stage 5. Create a transaction builder, then add the states and commands
            // add new owner to signers
            // add all old participants
            ImmutableList<PublicKey> requiredSigner = new ImmutableList.Builder<PublicKey>()
                    .addAll(tokenInputState.getParticipantKeys())
                    .add(tokenOutputState.getOwner().getOwningKey()).build();
            // We build our transaction.
            TransactionBuilder transactionBuilder = this.getTransactionBuilder(
                    getOurIdentity(),
                    requiredSigner,
                    new TokenContract.Commands.Transfer());
            transactionBuilder.setNotary(notary);

            transactionBuilder.addInputState(tokenInputStateToTransfer);
            transactionBuilder.addOutputState(tokenOutputState, TokenContract.ID);


            /* ============================================================================
             *          TODO 2 - Write our TokenContract to control token issuance!
             * ===========================================================================*/
            // Stage 6. Verify and sign the transaction.
            // We check our transaction is valid based on its contracts.
            progressTracker.setCurrentStep(SIGNING);
            transactionBuilder.verify(getServiceHub());

            // We sign the transaction with our private key, making it immutable.
            // signer must be the old owner
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(
                    transactionBuilder, oldOwner.getOwningKey());

            // Stage 7. Get a Party object for the old issuer.
            progressTracker.setCurrentStep(SYNCING);

            // Stage 8. Send any keys and certificates so the signers can verify each other's identity.
            // We call `toSet` in case the old issuer and the new owner are the same party.
            Set<FlowSession> sessions = new HashSet<>();
            Set<Party> parties = ImmutableSet.of(oldIssuer, newOwner);
            for (Party party : parties) {
                sessions.add(initiateFlow(party));
            }
            subFlow(new IdentitySyncFlow.Send(sessions, signedTransaction.getTx(), SYNCING.childProgressTracker()));

            // Stage 9. Collect signatures from the issuer and the new owner.
            progressTracker.setCurrentStep(COLLECTING);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    signedTransaction,
                    sessions,
                    ImmutableList.of(oldOwner.getOwningKey()),
                    COLLECTING.childProgressTracker()));

            // We get the transaction notarised and recorded automatically by the platform.
            // send a copy to current issuer
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, ImmutableSet.of(issuer)));
        }
    }

    @InitiatedBy(TokenTransferFlow.class)
    public static class TokenTransferResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public TokenTransferResponder(FlowSession otherFlow) {
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