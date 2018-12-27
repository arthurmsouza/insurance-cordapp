package ch.insurance.cordapp.broker.flows;

import ch.insurance.cordapp.BaseFlow;
import ch.insurance.cordapp.broker.MandateContract;
import ch.insurance.cordapp.broker.MandateState;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.Sets;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;


public class MandateWithdrawFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends BaseFlow<MandateState> {

        private final UniqueIdentifier id;

        public Initiator(UniqueIdentifier id) {
            this.id = id;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - search for our object by <id>
             * ===========================================================================*/
            StateAndRef<MandateState> mandateToTransfer =  this.getStateByLinearId(MandateState.class, this.id);
            MandateState mandate = this.getStateByRef(mandateToTransfer);

            if (!me.equals(mandate.getClient())) {
                throw new FlowException("Mandate can only be updated by client.");
            }

            /* ============================================================================
             *      TODO 2 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            progressTracker.setCurrentStep(BUILDING);
            MandateState withdrawnMandate = mandate.withdraw();

            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    mandate,
                    new MandateContract.Commands.Withdraw());
            transactionBuilder.addInputState(mandateToTransfer);
            transactionBuilder.addOutputState(withdrawnMandate, MandateContract.ID);

            /* ============================================================================
             *          TODO 3 - Synchronize counterpart parties, send, sign and finalize!
             * ===========================================================================*/
            Set<Party> counterparties = Sets.newHashSet(mandate.getBroker());
            return synchronizeCounterpartiesAndFinalize(me, counterparties, transactionBuilder);
        }

    }


    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //subFlow(new IdentitySyncFlow.Receive(otherFlow));
            SignedTransaction stx = subFlow(new BaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
    }
}