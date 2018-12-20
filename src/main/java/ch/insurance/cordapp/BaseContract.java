package ch.insurance.cordapp;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.Currency;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public abstract class BaseContract implements Contract {

    public <T extends ContractState> void requireIssueCounts(LedgerTransaction tx, int count, Class<T>... stateClasses) {
        List<ContractState> inputs = tx.getInputStates();
        List<TransactionState<ContractState>> outputs = tx.getOutputs();
        requireThat(req -> {
            req.using("Must be no input.", inputs.size() == 0);
            req.using("Must be "+count+" output.", outputs.size() == count);
            for (Class<T> stateClass : stateClasses) {
                List<T> statesByClass = tx.outputsOfType(stateClass);
                req.using("Output state should be a " + stateClass.getName(), statesByClass.size() == count);
            }
            return null;
        });
    }
    public <T extends ContractState> void requireIssueCounts(LedgerTransaction tx, int count, Class<T> stateClass) {
        List<ContractState> inputs = tx.getInputStates();
        List<TransactionState<ContractState>> outputs = tx.getOutputs();
        requireThat(req -> {
            req.using("Must be no input.", inputs.size() == 0);
            req.using("Must be "+count+" output.", outputs.size() == count);
            List<T> statesByClass = tx.outputsOfType(stateClass);
            req.using("Output state should be a " + stateClass.getName(), statesByClass.size() == count);
            return null;
        });
    }
    public <T extends ContractState> void requireUpdateCounts(LedgerTransaction tx, int count, Class<T> stateClass) {
        requireTransferCounts(tx, count, stateClass, count, stateClass);
    }

    public <T extends ContractState, S extends ContractState> void requireTransferCounts(
            LedgerTransaction tx,
            int inputCount, Class<T> inputClass,
            int outputCount, Class<S> outputClass) {
        List<ContractState> inputs = tx.getInputStates();
        List<TransactionState<ContractState>> outputs = tx.getOutputs();
        requireThat(req -> {
            req.using("Must be "+(inputCount == 0 ? "no" : inputCount)+" input.", inputs.size() == inputCount);
            req.using("Must be "+(outputCount == 0 ? "no" : outputCount)+" output.", outputs.size() == outputCount);
            List<T> inputStatesByClass = tx.inputsOfType(inputClass);
            req.using("Input state should be a " + inputClass.getName(), inputStatesByClass.size() == inputCount);
            List<S> outpuStatesByClass = tx.outputsOfType(outputClass);
            req.using("Output state should be a " + outputClass.getName(), outpuStatesByClass.size() == outputCount);
            return null;
        });
    }

    public void requireAmountNone0(Amount<Currency> amount) {
        requireThat(req -> {
            req.using("Amount must be none 0.", amount.getQuantity() > 0);
            return null;
        });
    }

    public <T extends ContractState> T oneInput(LedgerTransaction tx, Class<T> stateClass) {
        return tx.inputsOfType(stateClass).get(0);
    }
    public <T extends ContractState> T oneOutput(LedgerTransaction tx, Class<T> stateClass) {
        return tx.outputsOfType(stateClass).get(0);
    }

}
