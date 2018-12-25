package ch.insurance.cordapp;

import net.corda.core.contracts.*;
import net.corda.core.flows.FlowException;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SignedStateVerifier extends StateVerifier implements TransactionDelegate {

    private SignedTransaction stx;
    private ServiceHub serviceHub;

    public SignedStateVerifier(SignedTransaction stx, ServiceHub serviceHub) {
        super();
        this.setTransaction(this);
        this.stx = stx;
        this.serviceHub = serviceHub;
        this.parent = null;
        this.commandClazz = null;
        this.command = null;
    }

    @Override
    public List<CommandWithParties<CommandData>> getCommands() {
        return new ArrayList<>();
    }

    @Override
    public List<ContractState> inputsOfType(Class stateClass) {
        return this.getInputStates().stream().filter(
                x -> stateClass.isInstance(x)
        ).collect(Collectors.toList());
    }

    @Override
    public List<ContractState> outputsOfType(Class stateClass) {
        return this.getOutputStates().stream().filter(
                x -> stateClass.isInstance(x)
        ).collect(Collectors.toList());
    }

    @Override
    public List<ContractState> getOutputStates() {
        return stx.getTx().getOutputStates();
    }

    @Override
    public List<ContractState> getInputStates() {
        ArrayList<ContractState> inputStates = new ArrayList<>();
        for (StateRef stateRef : this.stx.getInputs()) {
            try {
                StateAndRef stateAndRef = this.serviceHub.toStateAndRef(stateRef);
                inputStates.add(stateAndRef.getState().getData());
            } catch (TransactionResolutionException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return inputStates;
    }

}
