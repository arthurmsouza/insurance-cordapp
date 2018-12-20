package ch.insurance.cordapp;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class StateVerifier {
    protected LedgerTransaction tx;
    protected StateVerifier parent;
    protected String description = "";
    protected Class commandClazz;

    public StateVerifier(LedgerTransaction tx) {
        this.tx = tx;
        this.parent = null;
    }
    public <T extends CommandData> StateVerifier(LedgerTransaction tx, Class<T> clazz) {
        this.tx = tx;
        this.parent = null;
        this.commandClazz = clazz;
    }
    public StateVerifier(LedgerTransaction tx, StateVerifier parent) {
        this.tx = tx;
        this.parent = parent;
    }

    public CommandWithParties<? extends Commnad commands() {
        return requireSingleCommand(tx.getCommands(), this.commandClazz);
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
        return this.getList();
    }


    public StateVerifier input() {
        return new InputList(tx, this);
    }
    public StateVerifier input(Class<? extends ContractState> stateClass) {
        return new InputList(tx, this, stateClass);
    }
    public StateVerifier output() {
        return new OutputList(tx, this);
    }
    public StateVerifier output(Class<? extends ContractState> stateClass) {
        return new OutputList(tx, this, stateClass);
    }
    public StateVerifier one() {
        return new One(tx, this);
    }
    public <T extends ContractState> StateVerifier one(Class<T> stateClass) {
        return new OneClass(tx, this, stateClass);
    }
    public StateVerifier empty() {
        return new Empty(tx, this);
    }
    public StateVerifier notEmpty() {
        return new NotEmpty(tx, this);
    }
    public StateVerifier moreThanZero() {
        return new MoreThanZero(tx, this);
    }
    public StateVerifier moreThanZero(int size) {
        return new MoreThanZero(tx, this, size);
    }
    public StateVerifier moreThanOne() {
        return new MoreThanOne(tx, this);
    }
    public StateVerifier moreThanOne(int size) {
        return new MoreThanOne(tx, this, size);
    }
}

class Party extends StateVerifier {

    private Class<? extends CommandData> clazz;

    public Party(LedgerTransaction tx, StateVerifier parent) {
        super(tx, parent);
    }
    public <T extends CommandData> Party(LedgerTransaction tx, StateVerifier parent, Class<T> clazz) {
        super(tx, parent);
        this.clazz = clazz;
    }
    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List must contain only 1 entry."+this.description, this.getParentList().size() == 1);
            return null;
        });
    }




}


class Size extends StateVerifier {

    private int size = -1;

    public Size(LedgerTransaction tx, StateVerifier parent) {
        super(tx, parent);
    }
    public Size(LedgerTransaction tx, StateVerifier parent, int size) { super(tx, parent); this.size = size; }

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
    public One(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List must contain only 1 entry."+this.description, this.getParentList().size() == 1);
            return null;
        });
    }
}
class OneClass<T> extends StateList<T> {
    public OneClass(LedgerTransaction tx, StateVerifier parent, Class<T> stateClass) {  super(tx, parent, stateClass); }

    @Override
    public List<ContractState> getList() {
        List<ContractState> list = this.getParentList();
        return list.stream()
                .filter(contractState -> contractState.getClass().equals(this.stateClass))
                .collect(Collectors.toList());
    }

    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List must contain only 1 entry."+this.description, this.getList().size() == 1);
            return null;
        });
    }
}
class Empty extends Size {
    public Empty(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List must be empty."+this.description, this.getParentList().size() == 0);
            return null;
        });
    }
}
class NotEmpty extends Size {
    public NotEmpty(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    @Override
    public void verify() {
        requireThat(req -> {
            req.using("List should not be empty."+this.description, this.getParentList().size() != 0);
            return null;
        });
    }
}
class MoreThanZero extends Size {
    public MoreThanZero(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    public MoreThanZero(LedgerTransaction tx, StateVerifier parent, int size) {  super(tx, parent, size); }
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
    public MoreThanOne(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    public MoreThanOne(LedgerTransaction tx, StateVerifier parent, int size) {  super(tx, parent, size); }
    @Override
    public void verify() {
        super.verify();
        requireThat(req -> {
            req.using("List must more than 1 entry."+this.description, this.getParentList().size() > 1);
            return null;
        });
    }
}


abstract class StateList<T> extends StateVerifier {
    protected Class<T> stateClass;

    protected StateList(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    protected StateList(LedgerTransaction tx, StateVerifier parent, Class<T> stateClass) {  super(tx, parent); this.stateClass = stateClass;
        this.stateClass = stateClass;
        this.description = "For class "+stateClass.getSimpleName();
    }
    abstract public List<ContractState> getList();
    @Override
    public void verify() {
    }
}


class OutputList<T> extends StateList {
    OutputList(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    OutputList(LedgerTransaction tx, StateVerifier parent, Class<T> stateClass) {  super(tx, parent, stateClass); }
    @Override
    public List<ContractState> getList() {
        if (this.stateClass != null) {
            return tx.outputsOfType(this.stateClass);
        } else {
            return tx.getOutputStates();
        }
    }
}


class InputList<T> extends  StateList {
    InputList(LedgerTransaction tx, StateVerifier parent) {  super(tx, parent); }
    InputList(LedgerTransaction tx, StateVerifier parent, Class<T> stateClass) {  super(tx, parent, stateClass); }
    @Override
    public List<ContractState> getList() {
        if (this.stateClass != null) {
            return tx.inputsOfType(this.stateClass);
        } else {
            return tx.getInputStates();
        }
    }
}
