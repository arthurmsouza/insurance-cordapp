package ch.insurance.cordapp;

import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ContractStateVerifier {
    protected LedgerTransaction tx;
    protected ContractStateVerifier parent;

    public ContractStateVerifier(LedgerTransaction tx) {
        this.tx = tx;
        this.parent = null;
    }
    public ContractStateVerifier(LedgerTransaction tx, ContractStateVerifier parent) {
        this.tx = tx;
        this.parent = parent;
    }

    public ContractStateVerifier input() {
        return new InputList(tx, this, 1);
    }
    public ContractStateVerifier noInput() {
        return new InputList(tx, this, 0);
    }
    public ContractStateVerifier multipleInput() {
        return new InputList(tx, this, -1);
    }
    public ContractStateVerifier output() {
        return new OutputList(tx, this, 1);
    }
    public ContractStateVerifier noOutput() {
        return new OutputList(tx, this, 0);
    }
    public ContractStateVerifier multipleOutput() {
        return new OutputList(tx, this, -1);
    }}

class InputList extends  ContractStateVerifier {

    private int count;
    InputList(LedgerTransaction tx, ContractStateVerifier parent, int count) {
        super(tx, parent);
        this.count = count;
    }
    public void verify() {
        requireThat(req -> {
            List list = tx.getInputStates();
            if (this.count == -1) {
                req.using("Must be more than 1 input..", list.size() > 1);
            } else {
                req.using("Must be " + (count == 0 ? "no" : count) + " input..", list.size() == count);
            }
            return null;
        });
    }
}


class OutputList extends  ContractStateVerifier {

    private int count;
    OutputList(LedgerTransaction tx, ContractStateVerifier parent, int count) {
        super(tx, parent);
        this.count = count;
    }
    public void verify() {
        requireThat(req -> {
            List list = tx.getOutputStates();
            if (this.count == -1) {
                req.using("Must be more than 1 output..", list.size() > 1);
            } else {
                req.using("Must be " + (count == 0 ? "no" : count) + " output..", list.size() == count);
            }
            return null;
        });
    }
}