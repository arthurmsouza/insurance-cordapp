package ch.insurance.cordapp;

import co.paralleluniverse.fibers.Suspendable;
import kotlin.Unit;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

abstract public class ResponderBaseFlow<T extends ContractState>  extends FlowLogic<Unit> {
    protected final FlowSession otherFlow;

    public ResponderBaseFlow(FlowSession otherFlow) {
        this.otherFlow = otherFlow;
    }
    @Suspendable
    protected Unit receiveIdentitiesCounterpartiesNoTxChecking() throws FlowException {
        Unit none = subFlow(new IdentitySyncFlow.Receive(otherFlow));
        subFlow(new BaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
        return Unit.INSTANCE;
    }
    @Suspendable
    protected Unit receiveCounterpartiesNoTxChecking() throws FlowException {
        subFlow(new BaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
        return Unit.INSTANCE;
    }

}
