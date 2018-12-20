package ch.insurance.cordapp.broker.mandate;

import ch.insurance.cordapp.TokenState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class MandateStateTests {
    private final Party alice = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party brokerBob = new TestIdentity(new CordaX500Name("Broker Bob", "", "GB")).getParty();
    private final Instant start = Instant.now();
    private final Instant expiryYearly = this.start.plus(1365, ChronoUnit.DAYS);
    private final Instant expiryMonthly = this.start.plus(6, ChronoUnit.DAYS);

    @Test
    public void testSimpleMandateCreator() {
        new MandateState(alice, brokerBob);
    }

    @Test
    public void testHasGetterForParties() {
        MandateState mandate = new MandateState(alice, brokerBob);
        assertEquals(alice, mandate.getClient());
        assertEquals(brokerBob, mandate.getBroker());
    }

    @Test
    public void testImplementsContractState() {
        assert(new MandateState(alice, brokerBob) instanceof MandateState);
    }

    @Test
    public void testHasTwoParticipantsTheIssuerAndTheOwner() {
        MandateState mandate = new MandateState(alice, brokerBob);
        assertEquals(2, mandate.getParticipants().size());
        assert(mandate.getParticipants().contains(alice));
        assert(mandate.getParticipants().contains(brokerBob));
    }
}