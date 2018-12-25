package ch.insurance.cordapp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.FlowException;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

import java.util.List;

public class LedgerStateVerifier extends StateVerifier implements TransactionDelegate {

    private LedgerTransaction ltx;
    private ServiceHub serviceHub;

    protected <T extends CommandData> LedgerStateVerifier(LedgerTransaction ltx, Class<T> clazz) {
        super();
        this.ltx = ltx;
        this.setTransaction(this);
        this.commandClazz = clazz;
        this.command = requireSingleCommand(this.ltx.getCommands(), (Class<? extends CommandData>)this.commandClazz);
    }
    protected LedgerStateVerifier(LedgerTransaction ltx) {
        super();
        this.ltx = ltx;
        this.setTransaction(this);
        this.commandClazz = null;
        this.command = null;
    }


    @Override
    public List<CommandWithParties<CommandData>> getCommands() {
        return ltx.getCommands();
    }

    @Override
    public List<ContractState> inputsOfType(Class stateClass) {
        return ltx.inputsOfType(stateClass);
    }

    @Override
    public List<ContractState> outputsOfType(Class stateClass) {
        return ltx.outputsOfType(stateClass);
    }

    @Override
    public List<ContractState> getOutputStates() {
        return ltx.getOutputStates();
    }

    @Override
    public List<ContractState> getInputStates() {
        return ltx.getInputStates();
    }

}
