package ch.insurance.cordapp.broker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class MandateStateTests extends BaseTests {
    private final Instant expiryYearly = this.start.plus(1365, ChronoUnit.DAYS);
    private final Instant expiryMonthly = this.start.plus(6, ChronoUnit.DAYS);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        super.setup(false);
    }

    @Test
    public void testSimpleMandateCreator() {
        new MandateState(aliceTheCustomer, bobTheBroker);
    }

    @Test
    public void testHasGetterForParties() {
        MandateState mandate = new MandateState(aliceTheCustomer, bobTheBroker);
        assertEquals(aliceTheCustomer, mandate.getClient());
        assertEquals(bobTheBroker, mandate.getBroker());
    }

    @Test
    public void testImplementsContractState() {
        assert(new MandateState(aliceTheCustomer, bobTheBroker) instanceof MandateState);
    }

    @Test
    public void testHasTwoParticipantsTheIssuerAndTheOwner() {
        MandateState mandate = new MandateState(aliceTheCustomer, bobTheBroker);
        assertEquals(2, mandate.getParticipants().size());
        assert(mandate.getParticipants().contains(aliceTheCustomer));
        assert(mandate.getParticipants().contains(bobTheBroker));
    }


    @Test
    public void testAccepted() {
        MandateState mandate = new MandateState(aliceTheCustomer, bobTheBroker);
        MandateState updatedMandate = mandate.accept();
        assert(updatedMandate.isAccepted());
    }

    @Test
    public void testAcceptedWithNewStart() {
        Instant newStart = Instant.now();
        Instant newEnd = newStart.plus(10, ChronoUnit.DAYS);
        MandateState mandate = new MandateState(aliceTheCustomer, bobTheBroker);
        MandateState updatedMandate = mandate.accept(newStart, 10, ChronoUnit.DAYS);
        assert(updatedMandate.getExpiredAt().equals(newEnd));
        assert(updatedMandate.isAccepted());
    }


    @Test
    public void testUpdatedWithNewStart() {
        Instant newStart = Instant.now();
        Instant newEnd = newStart.plus(10, ChronoUnit.DAYS);
        MandateState mandate = new MandateState(aliceTheCustomer, bobTheBroker);
        MandateState updatedMandate = mandate.updateTimestamps(newStart, 10, ChronoUnit.DAYS);
        assert(updatedMandate.getExpiredAt().equals(newEnd));
        assert(!updatedMandate.isAccepted());
    }

}