package ch.insurance.cordapp;

import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ContractStateVerifier {
    protected LedgerTransaction tx;
    protected ContractStateVerifier parent;
    protected String description = "";

    public ContractStateVerifier(LedgerTransaction tx) {
        this.tx = tx;
        this.parent = null;
    }
    public ContractStateVerifier(LedgerTransaction tx, ContractStateVerifier parent) {
        this.tx = tx;
        this.parent = parent;
    }

    public void verifyAll() {
        if (this.parent != null) {
            this.parent.verifyAll();
        }
        this.verify();
    }

    public void verify() {
    }

    protected List<ContractState> getList() {
        return new ArrayList<>();
    }

    protected List<ContractState> getParentList() {
        if (this.parent != null) {
            if (this.parent instanceof StateList) {
                return this.parent.getList();
            } else {
                return this.parent.getParentList();
            }
        }
        return this.getParentList();
    }


    public ContractStateVerifier input() {
        return new InputList(tx, this);
    }
    public ContractStateVerifier output() {
        return new OutputList(tx, this);
    }
    public ContractStateVerifier one() {
        return new One(tx, this);
    }
    public ContractStateVerifier empty() {
        return new Empty(tx, this);
    }
    public ContractStateVerifier moreThanZero() {
        return new MoreThanZero(tx, this);
    }
    public ContractStateVerifier moreThanZero(int size) {
        return new MoreThanZero(tx, this, size);
    }
    public ContractStateVerifier moreThanOne() {
        return new MoreThanOne(tx, this);
    }
    public ContractStateVerifier moreThanOne(int size) {
        return new MoreThanOne(tx, this, size);
    }
}


class Size extends ContractStateVerifier {

    private int size = -1;

    public Size(LedgerTransaction tx, ContractStateVerifier parent) {
        super(tx, parent);
    }
    public Size(LedgerTransaction tx, ContractStateVerifier parent, int size) { super(tx, parent); this.size = size; }

    @Override
    public void verify() {
        requireThat(req -> {
            if (this.size != -1) {
                req.using("List must contain only 1 entry."+this.description, this.getParentList().size() == this.size);
            }
            return null;
        });
    }

}

class One extends Size {
    public One(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List must contain only 1 entry."+this.description, this.getParentList().size() == 1);
            return null;
        });
    }
}
class Empty extends Size {
    public Empty(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List must be empty."+this.description, this.getParentList().size() == 0);
            return null;
        });
    }
}
class MoreThanZero extends Size {
    public MoreThanZero(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    public MoreThanZero(LedgerTransaction tx, ContractStateVerifier parent, int size) {  super(tx, parent, size); }
    @Override
    public void verify() {
        super.verify();
        requireThat(req -> {
            req.using("List must contain at least 1 entry."+this.description, this.getParentList().size() > 0);
            return null;
        });
    }
}
class MoreThanOne extends Size {
    public MoreThanOne(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    public MoreThanOne(LedgerTransaction tx, ContractStateVerifier parent, int size) {  super(tx, parent, size); }
    @Override
    public void verify() {
        super.verify();
        requireThat(req -> {
            req.using("List must more than 1 entry."+this.description, this.getParentList().size() > 1);
            return null;
        });
    }
}


abstract class StateList extends  ContractStateVerifier {
    protected Class<ContractState> stateClass;

    protected StateList(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    protected StateList(LedgerTransaction tx, ContractStateVerifier parent, Class<ContractState> stateClass) {  super(tx, parent); this.stateClass = stateClass;
        this.stateClass = stateClass;
    }
    abstract public List<ContractState> getList();
    @Override
    public void verify() {
    }
}


class OutputList extends  StateList {
    OutputList(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    OutputList(LedgerTransaction tx, ContractStateVerifier parent, Class<ContractState> stateClass) {  super(tx, parent, stateClass); }
    @Override
    public List<ContractState> getList() {
        if (this.stateClass != null) {
            return tx.outputsOfType(this.stateClass);
        } else {
            return tx.getOutputStates();
        }
    }
}


class InputList extends  StateList {
    InputList(LedgerTransaction tx, ContractStateVerifier parent) {  super(tx, parent); }
    InputList(LedgerTransaction tx, ContractStateVerifier parent, Class<ContractState> stateClass) {  super(tx, parent, stateClass); }
    @Override
    public List<ContractState> getList() {
        if (this.stateClass != null) {
            return tx.inputsOfType(this.stateClass);
        } else {
            return tx.getInputStates();
        }
    }
}
