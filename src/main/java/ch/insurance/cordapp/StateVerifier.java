package ch.insurance.cordapp;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.flows.FlowException;
import net.corda.core.identity.AbstractParty;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

interface TransactionDelegate {
    public List<CommandWithParties<CommandData>> getCommands();

    public List<ContractState> inputsOfType(Class stateClass);

    public List<ContractState> outputsOfType(Class stateClass);

    public List<ContractState> getOutputStates();

    public List<ContractState> getInputStates();
}


public class StateVerifier {
    protected TransactionDelegate tx;
    protected StateVerifier parent;
    protected String description = "";
    protected Class<? extends CommandData> commandClazz;
    protected CommandWithParties<? extends CommandData> command;
    protected String text;

    public static StateVerifier fromTransaction(SignedTransaction tx, ServiceHub serviceHub) {
        return new SignedStateVerifier(tx, serviceHub);
    }
    public static <T extends CommandData> StateVerifier fromTransaction(LedgerTransaction tx, Class<T> clazz) {
        return new LedgerStateVerifier(tx, clazz);
    }
    public static StateVerifier fromTransaction(LedgerTransaction tx) {
        return new LedgerStateVerifier(tx);
    }


    protected StateVerifier() {
        this.parent = null;
    }
    protected void setTransaction(TransactionDelegate tx) {
        this.tx = tx;
    }

    /*
    public <T extends CommandData> StateVerifier(TransactionDelegate tx, Class<T> clazz) {
        this.tx = tx;
        this.parent = null;
        this.commandClazz = clazz;
        this.command = requireSingleCommand(tx.getCommands(), (Class<? extends CommandData>)this.commandClazz);
    }
    */
    protected StateVerifier(StateVerifier parent) {
        this.tx = parent.tx;
        this.parent = parent;
        this.commandClazz = parent.commandClazz;
        this.command = parent.command;
    }
    protected StateVerifier(StateVerifier parent, String text) {
        this.tx = parent.tx;
        this.parent = parent;
        this.commandClazz = parent.commandClazz;
        this.command = parent.command;
        this.text = text;
    }

    protected String s(String text) {
        String t = this.text == null ? text : this.text;
        return this.description == null ? t : t+this.description;
    }

    public CommandData command() {
        return this.command.getValue();
    }

    public List<PublicKey> getSigners() {
        return this.command.getSigners();
    }

    public StateVerifier verify() {
        return this;
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
        return new ArrayList<>();
    }


    public <T extends ContractState> List<T> list() {
        return (List<T>)this.getParentList();
    }
    public <T extends ContractState> T object() {
        return this.object(0);
    }
    public <T extends ContractState> T object(int index) {
        return (T)this.getParentList().get(index);
    }


    public StateVerifier input() {
        return new InputList(this).verify();
    }
    public StateVerifier input(Class<? extends ContractState> stateClass) {
        return new InputList(this, stateClass).verify();
    }
    public StateVerifier output() {
        return new OutputList(this).verify();
    }
    public StateVerifier output(Class<? extends ContractState> stateClass) {
        return new OutputList(this, stateClass).verify();
    }
    public <T extends  ContractState> StateVerifier use(T state) {
        return new UseThis<T>(this, state).verify();
    }
    public <T extends  ContractState> StateVerifier use(List<T> states) {
        return new UseThese<T>(this, states).verify();
    }
    public <T extends ContractState> StateVerifier filter(Class<T> stateClass) {
        return new FilterList(this, stateClass).verify();
    }
    public <T extends ContractState> StateVerifier filterWhere(Function<T, Boolean> mapper) {
        return new FilterWhere(this, mapper).verify();
    }


    public StateVerifier one() {
        return new One(this).verify();
    }
    public StateVerifier one(String text) {
        return new One(this).verify();
    }
    public <T extends ContractState> StateVerifier one(Class<T> stateClass) {
        return this.filter(stateClass).one();
    }
    public StateVerifier empty() {
        return new Empty(this).verify();
    }
    public StateVerifier empty(String text) {
        return new Empty(this, text).verify();
    }
    public StateVerifier notEmpty() {
        return new NotEmpty(this).verify();
    }
    public StateVerifier notEmpty(String text) {
        return new NotEmpty(this, text).verify();
    }
    public StateVerifier moreThanZero() {
        return new MoreThanZero(this).verify();
    }
    public StateVerifier moreThanZero(int size) {
        return new MoreThanZero(this, size).verify();
    }
    public StateVerifier count(int size) {
        return new Count(this, size).verify();
    }
    public StateVerifier max(int size) {
        return new Max(this, size).verify();
    }
    public StateVerifier min(int size) {
        return new Min(this, size).verify();
    }
    public StateVerifier moreThanOne() {
        return new MoreThanOne(this).verify();
    }
    public StateVerifier moreThanOne(int size) {
        return new MoreThanOne(this, size).verify();
    }
    public StateVerifier amountNot0(String name, Function<ContractState, Amount> mapper) {
        return new AmountNot0(this, name, mapper).verify();
    }
    public StateVerifier participantsAreSigner() {
        return new ParticipantsAreSigners(this).verify(); }
    public StateVerifier participantsAreSigner(String text) {
        return new ParticipantsAreSigners(this, text).verify(); }
    public StateVerifier signer(String text, Function<ContractState, ? extends AbstractParty> mapper) {
        return new Signers(this, text, mapper).verify(); }
    public StateVerifier differentParty(String name1, Function<ContractState, ? extends AbstractParty> party1Mapper,
                                        String name2, Function<ContractState, ? extends AbstractParty> party2Mapper
                                      ) { return new DifferentParty(this, name1, party1Mapper, name2, party2Mapper).verify(); }
    public StateVerifier sameParty(String name1, Function<ContractState, ? extends AbstractParty> party1Mapper,
                                   String name2, Function<ContractState, ? extends AbstractParty> party2Mapper
    ) { return new SameParty(this, name1, party1Mapper, name2, party2Mapper).verify(); }


}

class Signers extends StateVerifier {

    private Function<ContractState, ? extends AbstractParty> mapper;

    public Signers(StateVerifier parent, @NotNull String text, Function<ContractState, ? extends AbstractParty> mapper) {
        super(parent, text);
        this.mapper = mapper;
    }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            for (ContractState state : this.getParentList()) {
                AbstractParty party = this.mapper.apply(state);
                req.using(s("party <" + party.nameOrNull()+ "> is not a signer."),
                        this.getSigners().contains(party.getOwningKey()));
            }
            return null;
        });
        return this;
    }
}



class DifferentParty extends StateVerifier {

    private String name1;
    private Function<ContractState, ? extends AbstractParty> party1Mapper;
    private String name2;
    private Function<ContractState, ? extends AbstractParty> party2Mapper;

    public DifferentParty(StateVerifier parent,
                          @NotNull String name1, Function<ContractState, ? extends AbstractParty> party1Mapper,
                          @NotNull String name2, Function<ContractState, ? extends AbstractParty> party2Mapper
    ) {
        super(parent);
        this.name1 = name1;
        this.party1Mapper = party1Mapper;
        this.name2 = name2;
        this.party2Mapper = party2Mapper;
    }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            for (ContractState state : this.getParentList()) {
                AbstractParty party1 = this.party1Mapper.apply(state);
                AbstractParty party2 = this.party2Mapper.apply(state);
                req.using(s("party <"+this.name1+"> should be different than party <"+this.name2+">."),
                        !party1.equals(party2));
            }
            return null;
        });
        return this;
    }
}


class SameParty extends StateVerifier {

    private String name1;
    private Function<ContractState, ? extends AbstractParty> party1Mapper;
    private String name2;
    private Function<ContractState, ? extends AbstractParty> party2Mapper;

    public SameParty(StateVerifier parent,
                          @NotNull String name1, Function<ContractState, ? extends AbstractParty> party1Mapper,
                          @NotNull String name2, Function<ContractState, ? extends AbstractParty> party2Mapper
    ) {
        super(parent);
        this.name1 = name1;
        this.party1Mapper = party1Mapper;
        this.name2 = name2;
        this.party2Mapper = party2Mapper;
    }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            for (ContractState state : this.getParentList()) {
                AbstractParty party1 = this.party1Mapper.apply(state);
                AbstractParty party2 = this.party2Mapper.apply(state);
                req.using(s("party <"+this.name1+"> should be the same than party <"+this.name2+">."),
                        party1.equals(party2));
            }
            return null;
        });
        return this;
    }
}


class ParticipantsAreSigners extends StateVerifier {

    public ParticipantsAreSigners(StateVerifier parent) {
        super(parent);
    }
    public ParticipantsAreSigners(StateVerifier parent, String text) {
        super(parent, text);
    }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            for (ContractState state : this.getParentList()) {
                req.using(s("Not all participants are transaction signers."),
                        this.getSigners().containsAll(
                                state.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
            }
            return null;
        });
        return this;
    }
}


class AmountNot0 extends StateVerifier {

    private String name;
    private Function<ContractState, Amount> amountMapper;

    public AmountNot0(StateVerifier parent, String name, Function<ContractState, Amount> amountMapper) {
        super(parent);
        this.name = name;
        this.amountMapper = amountMapper;
    }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            for (ContractState state : this.getParentList()) {
                req.using(s("Amount <"+this.name+"> should be not 0."),
                        this.amountMapper.apply(state).getQuantity() > 0 );
            }
            return null;
        });
        return this;
    }
}




class Size extends StateVerifier {

    private int size = -1;

    public Size(StateVerifier parent) {
        super(parent);
    }
    public Size(StateVerifier parent, String text) {
        super(parent, text);
    }
    public Size(StateVerifier parent, int size) { super(parent); this.size = size; }

    protected int size() {
        return this.size;
    }

    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            if (this.size != -1) {
                req.using(s("List must have "+this.size+" entries."), this.getParentList().size() == this.size);
            }
            return null;
        });
        return this;
    }

}

class One extends Size {
    public One(StateVerifier parent) {  super(parent); }
    public One(StateVerifier parent, String text) {  super(parent, text); }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            req.using(s("List must contain only 1 entry."), this.getParentList().size() == 1);
            return null;
        });
        return this;
    }
}
class OneClass<T> extends StateList<T> {
    public OneClass(StateVerifier parent, Class<T> stateClass) {  super(parent, stateClass); }

    @Override
    public List<ContractState> getList() {
        List<ContractState> list = this.parent.getParentList();
        return list.stream()
                .filter(contractState -> contractState.getClass().equals(this.stateClass))
                .collect(Collectors.toList());
    }

    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            req.using(s("List must contain only 1 entry."), this.getList().size() == 1);
            return null;
        });
        return this;
    }
}
class Empty extends Size {
    public Empty(StateVerifier parent) {  super(parent); }
    public Empty(StateVerifier parent, String text) {  super(parent, text); }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            req.using(s("List must be empty."), this.getParentList().isEmpty());
            return null;
        });
        return this;
    }
}
class NotEmpty extends Size {
    public NotEmpty(StateVerifier parent) {  super(parent); }
    public NotEmpty(StateVerifier parent, String text) {  super(parent, text); }
    @Override
    public StateVerifier verify() {
        requireThat(req -> {
            req.using(s("List should not be empty."), !this.getParentList().isEmpty());
            return null;
        });
        return this;
    }
}
class MoreThanZero extends Size {
    public MoreThanZero(StateVerifier parent) {  super(parent); }
    public MoreThanZero(StateVerifier parent, int size) {  super(parent, size); }
    @Override
    public StateVerifier verify() {
        super.verify();
        requireThat(req -> {
            req.using(s("List must contain at least 1 entry."), this.getParentList().size() > 0);
            return null;
        });
        return this;
    }
}
class MoreThanOne extends Size {
    public MoreThanOne(StateVerifier parent) {  super(parent); }
    public MoreThanOne(StateVerifier parent, int size) {  super(parent, size); }
    @Override
    public StateVerifier verify() {
        super.verify();
        requireThat(req -> {
            req.using(s("List must more than 1 entry."), this.getParentList().size() > 1);
            return null;
        });
        return this;
    }
}
class Count extends Size {
    public Count(StateVerifier parent, int size) {  super(parent, size); }
    @Override
    public StateVerifier verify() {
        super.verify();
        return this;
    }
}
class Max extends Size {
    public Max(StateVerifier parent, int size) {  super(parent, size); }
    @Override
    public StateVerifier verify() {
        super.verify();
        requireThat(req -> {
            req.using(s("List must contain max "+this.size()+"."), this.getParentList().size() <= this.size());
            return null;
        });
        return this;
    }
}
class Min extends Size {
    public Min(StateVerifier parent, int size) {  super(parent, size); }
    @Override
    public StateVerifier verify() {
        super.verify();
        requireThat(req -> {
            req.using(s("List must contain max "+this.size()+"."), this.getParentList().size() >= this.size());
            return null;
        });
        return this;
    }
}


abstract class StateList<T> extends StateVerifier {
    protected Class<T> stateClass;

    protected StateList(StateVerifier parent) {
        super(parent);
    }

    protected StateList(StateVerifier parent, Class<T> stateClass) {
        super(parent);
        this.stateClass = stateClass;
        this.description = "For class " + stateClass.getSimpleName();
    }

    abstract public List<ContractState> getList();

    @Override
    public StateVerifier verify() {
        return this;
    }

    protected List<ContractState> getParentList() {
        return this.getList();
    }
}


class OutputList<T> extends StateList {
    OutputList(StateVerifier parent) {  super(parent); }
    OutputList(StateVerifier parent, Class<T> stateClass) {  super(parent, stateClass); }
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
    InputList(StateVerifier parent) {  super(parent); }
    InputList(StateVerifier parent, Class<T> stateClass) {  super(parent, stateClass); }
    @Override
    public List<ContractState> getList() {
        if (this.stateClass != null) {
            return tx.inputsOfType(this.stateClass);
        } else {
            return tx.getInputStates();
        }
    }
}

class FilterList<T> extends  StateList {
    FilterList(StateVerifier parent, Class<T> stateClass) {  super(parent, stateClass); }
    @Override
    public List<ContractState> getList() {
        List<ContractState> list = this.parent.getParentList();
        return list.stream()
                .filter(contractState -> contractState.getClass().equals(this.stateClass))
                .collect(Collectors.toList());
    }
}


class UseThis<T extends ContractState> extends  StateList {

    private final T state;

    UseThis(StateVerifier parent, T state) {  super(parent); this.state = state; }

    @Override
    public List<ContractState> getList() {
        return ImmutableList.of(this.state);
    }
}


class UseThese<T extends ContractState> extends  StateList {

    private final List<T> states;

    UseThese(StateVerifier parent, List<T> state) {  super(parent); this.states = state; }

    @Override
    public List<ContractState> getList() {
        return (List<ContractState>)this.states;
    }
}


class FilterWhere<T extends  ContractState> extends  StateList {
    private final Function<T, Boolean> mapper;

    FilterWhere(StateVerifier parent, Function<T, Boolean> mapper) {  super(parent); this.mapper = mapper; }
    @Override
    public List<ContractState> getList() {
        List<T> list = (List<T>)this.parent.getParentList();
        return list.stream()
                .filter(x -> this.mapper.apply(x))
                .collect(Collectors.toList());
    }
}


