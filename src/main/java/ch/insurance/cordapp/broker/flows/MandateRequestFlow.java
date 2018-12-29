package ch.insurance.cordapp.broker.flows;

import ch.insurance.cordapp.BaseFlow;
import ch.insurance.cordapp.broker.MandateContract;
import ch.insurance.cordapp.broker.MandateState;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


public class MandateRequestFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends BaseFlow<MandateState> {
        private final Party broker;
        private final Instant startAt;
        private final List<String> allowedBusiness;

        public Initiator(Party broker, Instant startAt, List<String> allowedBusiness) {
            this.broker = broker;
            this.startAt = startAt;
            this.allowedBusiness = allowedBusiness;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party issuer = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - Create our object !
             * ===========================================================================*/
            // We create our new TokenState.
            Instant oneYear = startAt.plus(365, ChronoUnit.DAYS);
            MandateState mandate = new MandateState(issuer, this.broker, this.startAt, oneYear, this.allowedBusiness);

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            progressTracker.setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getMyTransactionBuilderSignedByMe(new MandateContract.Commands.Request());
            transactionBuilder.addOutputState(mandate, MandateContract.ID);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            return synchronizeAndFinalize(transactionBuilder);
        }

    }
}