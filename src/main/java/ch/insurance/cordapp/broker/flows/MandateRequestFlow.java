package ch.insurance.cordapp.broker.flows;

import ch.insurance.cordapp.BaseFlow;
import ch.insurance.cordapp.ResponderBaseFlow;
import ch.insurance.cordapp.broker.MandateContract;
import ch.insurance.cordapp.broker.MandateState;
import co.paralleluniverse.fibers.Suspendable;
import kotlin.Unit;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

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

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            getProgressTracker().setCurrentStep(PREPARATION);
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
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    mandate,
                    new MandateContract.Commands.Request());
            transactionBuilder.addOutputState(mandate, MandateContract.ID);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            return signCollectAndFinalize(getOurIdentity(), mandate.getBroker(), transactionBuilder);
        }

    }
    @InitiatedBy(MandateRequestFlow.Initiator.class)
    public static class Responder extends ResponderBaseFlow<MandateState> {

        public Responder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }}