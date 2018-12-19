package ch.insurance.cordapp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Currency;


public class TokenIssue {

    /* Our flow, automating the process of updating the ledger.
     * See src/main/java/examples/ArtTransferFlowInitiator.java for an example. */
    @InitiatingFlow
    @StartableByRPC
    public static class TokenIssueFlow extends TokenBaseFlow {
        private final Party owner;
        private final Amount<Currency> amount;

        public TokenIssueFlow(Party owner, Amount<Currency> amount) {
            this.owner = owner;
            this.amount = amount;
        }

        private final ProgressTracker progressTracker = new ProgressTracker();

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // We choose our transaction's notary (the notary prevents double-spends).
            Party notary = OneNotary();
            // We get a reference to our own identity.
            Party issuer = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - Create our TokenState to represent on-ledger tokens!
             * ===========================================================================*/
            // We create our new TokenState.
            TokenState tokenState = new TokenState(issuer, this.owner, this.amount);

            /* ============================================================================
             *      TODO 3 - Build our token issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            TransactionBuilder transactionBuilder = getMyTransactionBuilder(new TokenContract.Commands.Issue());
            transactionBuilder.addOutputState(tokenState, TokenContract.ID);

            /* ============================================================================
             *          TODO 2 - Write our TokenContract to control token issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            transactionBuilder.verify(getServiceHub());

            // We sign the transaction with our private key, making it immutable.
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // We get the transaction notarised and recorded automatically by the platform.
            return subFlow(new FinalityFlow(signedTransaction));
        }

    }
}