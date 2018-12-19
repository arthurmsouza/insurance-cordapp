package ch.insurance.cordapp;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StateTests {
    private final Party alice = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();
    private final Amount amount1CHF = Amount.parseCurrency("1 CHF");

    @Test
    public void tokenStateHasIssuerOwnerAndAmountParamsOfCorrectTypeInConstructor() {
        new TokenState(alice, bob, amount1CHF);
    }

    @Test
    public void tokenStateHasGettersForIssuerOwnerAndAmount() {
        TokenState tokenState = new TokenState(alice, bob, amount1CHF);
        assertEquals(alice, tokenState.getIssuer());
        assertEquals(bob, tokenState.getOwner());
        assertEquals(Amount.parseCurrency("1 CHF"), tokenState.getAmount());
    }

    @Test
    public void tokenStateImplementsContractState() {
        assert(new TokenState(alice, bob, amount1CHF) instanceof ContractState);
    }

    @Test
    public void tokenStateHasTwoParticipantsTheIssuerAndTheOwner() {
        TokenState tokenState = new TokenState(alice, bob, amount1CHF);
        assertEquals(2, tokenState.getParticipants().size());
        assert(tokenState.getParticipants().contains(alice));
        assert(tokenState.getParticipants().contains(bob));
    }
}