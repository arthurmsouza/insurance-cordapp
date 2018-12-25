package ch.insurance.cordapp.broker;

import ch.insurance.cordapp.broker.MandateState.LineOfBusiness;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class MandateContractTests extends BaseTests {

    @Before
    public void setup() {
        super.setup(false);
    }
    @Test
    public void mandate_request_requiresZeroInputsInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has an input, will fail.
            tx.input(MandateContract.ID, newMandate());
            tx.output(MandateContract.ID, newMandate());
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("input must be empty");
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(MandateContract.ID, newMandate());
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void mandate_request_clientAndBrokerNotSame() {
        transaction(ledgerServices, tx -> {
            tx.output(MandateContract.ID, new MandateState(aliceTheCustomer, aliceTheCustomer));
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("should be different than");
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(MandateContract.ID, newMandate());
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_request_invalidSigner() {
        transaction(ledgerServices, tx -> {
            tx.output(MandateContract.ID, newMandate());
            tx.command(bobTheBroker.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("client must be the signer");
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.output(MandateContract.ID, newMandate());
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_request_not_accepted() {
        transaction(ledgerServices, tx -> {
            MandateState state = newMandate();
            state = state.accept();
            tx.output(MandateContract.ID, state);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("mandate must be not accepted");
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.output(MandateContract.ID, newMandate());
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }

    private MandateState newMandate(@NotNull Instant startAt, @NotNull Instant expiredAt) {
        return new MandateState(aliceTheCustomer, bobTheBroker, startAt, expiredAt, LineOfBusiness.all().makeImmutable());
    }
    private MandateState newMandate() {
        return new MandateState(aliceTheCustomer, bobTheBroker);
    }

    @Test
    public void mandate_request_invalidTimeframe() {
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant end = start.plus(10, ChronoUnit.DAYS);
            tx.output(MandateContract.ID, newMandate(start, end));
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("start date must be in the future");
            return null;
        });

        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().minus(10, ChronoUnit.DAYS);
            Instant end = start.plus(5, ChronoUnit.DAYS);
            tx.output(MandateContract.ID, newMandate(start, end));
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("expired date must be in the future");
            return null;
        });
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant end = start.minus(1, ChronoUnit.DAYS);
            tx.output(MandateContract.ID, newMandate(start, end));
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("expired date must be later than start");
            return null;
        });
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant end = start.plus(100, ChronoUnit.SECONDS);
            tx.output(MandateContract.ID, newMandate(start, end));
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("difference between start and end must be at least 1 day");
            return null;
        });
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant end = start.plus(365, ChronoUnit.DAYS);
            tx.output(MandateContract.ID, newMandate(start, end));
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });

    }



    @Test
    public void mandate_update_notSameId() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandate2 = newMandate();
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandate2);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.failsWith("id must be same");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState update = mandate.updateTimestamps(Instant.now().plus(1, ChronoUnit.DAYS), 10, ChronoUnit.DAYS);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, update);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_update_notSameValues() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandate2 = new MandateState(cesarTheInsurer, bobTheBroker);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandate2);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.failsWith("client must be the same");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandate2 = new MandateState(aliceTheCustomer, cesarTheInsurer);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandate2);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.failsWith("broker must be the same");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState update = mandate.updateTimestamps(Instant.now().plus(1, ChronoUnit.DAYS), 10, ChronoUnit.DAYS);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, update);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void mandate_update_invalidTimeframes() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandate2 = mandate.updateTimestamps(
                    Instant.now().minus(15, ChronoUnit.DAYS), 10, ChronoUnit.DAYS);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandate2);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.failsWith("expired date must be in the future");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState update = mandate.updateTimestamps(
                    Instant.now().plus(1, ChronoUnit.DAYS), 10, ChronoUnit.DAYS);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, update);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_update_allowances() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.failsWith("one line of business must be choosen");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().PnC());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().IL());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().GL());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().Health());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().LnS());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }


}
