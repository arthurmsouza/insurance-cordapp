package ch.insurance.cordapp.broker.flows;

import ch.insurance.cordapp.BaseFlow;
import ch.insurance.cordapp.ResponderBaseFlow;
import ch.insurance.cordapp.broker.MandateContract;
import ch.insurance.cordapp.broker.MandateState;
import co.paralleluniverse.fibers.Suspendable;
import kotlin.Unit;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.time.Instant;
import java.util.List;


public class MandateUpdateFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends BaseFlow<MandateState> {

        private final UniqueIdentifier id;
        private Instant startAt;
        private List<String> allowedBusiness;

        public Initiator(UniqueIdentifier id, Instant startAt, List<String> allowedBusiness) {
            this.id = id;
            this.startAt = startAt;
            this.allowedBusiness = allowedBusiness;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync_nocollect;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            getProgressTracker().setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - search for our object by <id>
             * ===========================================================================*/
            StateAndRef<MandateState> mandateToTransfer =  this.getLastStateByLinearId(MandateState.class, this.id);
            MandateState mandate = this.getStateByRef(mandateToTransfer);

            if (!me.equals(mandate.getClient())) {
                throw new FlowException("Mandate can only be updated by client.");
            }

            /* ============================================================================
             *      TODO 2 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            MandateState updatedMandate = mandate
                    .updateAllowedBusiness(this.allowedBusiness)
                    .updateTimestamps(this.startAt, 365);

            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    mandate,
                    new MandateContract.Commands.Update());
            transactionBuilder.addInputState(mandateToTransfer);
            transactionBuilder.addOutputState(updatedMandate, MandateContract.ID);

            /* ============================================================================
             *          TODO 3 - Synchronize counterpart parties, send, sign and finalize!
             * ===========================================================================*/
            //return signCollectAndFinalize(me, updatedMandate.getBroker(), transactionBuilder);
            return signCollectAndFinalize(getOurIdentity(), mandate.getBroker(), transactionBuilder);
        }

    }


    @InitiatedBy(MandateUpdateFlow.Initiator.class)
    public static class Responder extends ResponderBaseFlow<MandateState> {

        public Responder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }
}