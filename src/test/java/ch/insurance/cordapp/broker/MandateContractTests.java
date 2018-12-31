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
            MandateState mandate = newMandate();
            tx.output(MandateContract.ID, mandate);
            tx.command(mandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void mandate_request_clientAndBrokerNotSame() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = new MandateState(aliceTheCustomer, aliceTheCustomer);
            tx.output(MandateContract.ID, mandate);
            tx.command(mandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.failsWith("should be different than");
            return null;
        });

        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            tx.output(MandateContract.ID, mandate);
            tx.command(mandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void mandate_request_invalidSigner() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            tx.output(MandateContract.ID, mandate);
            tx.command(aliceTheCustomer.getOwningKey(), new MandateContract.Commands.Request());
            tx.failsWith("all participants must be signer");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            tx.output(MandateContract.ID, mandate);
            tx.command(mandate.getParticipantKeys(), new MandateContract.Commands.Request());
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
            tx.command(state.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.failsWith("mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            tx.output(MandateContract.ID, mandate);
            tx.command(mandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.verifies();
            return null;
        });
    }

    private MandateState newMandate(@NotNull Instant startAt, @NotNull Instant expiredAt) {
        return new MandateState(aliceTheCustomer, bobTheBroker, startAt, expiredAt, LineOfBusiness.all().toList());
    }
    private MandateState newMandate() {
        return new MandateState(aliceTheCustomer, bobTheBroker);
    }

    @Test
    public void mandate_request_invalidTimeframe() {
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant end = start.plus(10, ChronoUnit.DAYS);
            MandateState newMandate = newMandate(start, end);
            tx.output(MandateContract.ID, newMandate);
            tx.command(newMandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.failsWith("start date must be in the future");
            return null;
        });

        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().minus(10, ChronoUnit.DAYS);
            Instant end = start.plus(5, ChronoUnit.DAYS);
            MandateState newMandate = newMandate(start, end);
            tx.output(MandateContract.ID, newMandate);
            tx.command(newMandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.failsWith("expired date must be in the future");
            return null;
        });
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant end = start.minus(1, ChronoUnit.DAYS);
            MandateState newMandate = newMandate(start, end);
            tx.output(MandateContract.ID, newMandate);
            tx.command(newMandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.failsWith("expired date must be later than start");
            return null;
        });
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant end = start.plus(100, ChronoUnit.SECONDS);
            MandateState newMandate = newMandate(start, end);
            tx.output(MandateContract.ID, newMandate);
            tx.command(newMandate.getParticipantKeys(), new MandateContract.Commands.Request());
            tx.failsWith("difference between start and end must be at least 1 day");
            return null;
        });
        transaction(ledgerServices, tx -> {
            Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant end = start.plus(365, ChronoUnit.DAYS);
            MandateState newMandate = newMandate(start, end);
            tx.output(MandateContract.ID, newMandate);
            tx.command(newMandate.getParticipantKeys(), new MandateContract.Commands.Request());
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
            tx.command(mandate2.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.failsWith("id must be same");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState update = mandate.updateTimestamps(Instant.now().plus(1, ChronoUnit.DAYS), 10);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, update);
            tx.command(update.getParticipantKeys(), new MandateContract.Commands.Update());
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
            tx.command(mandate2.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.failsWith("client must be the same");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandate2 = new MandateState(aliceTheCustomer, cesarTheInsurer);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandate2);
            tx.command(mandate2.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.failsWith("broker must be the same");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState update = mandate.updateTimestamps(Instant.now().plus(1, ChronoUnit.DAYS), 10);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, update);
            tx.command(update.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void mandate_update_invalidTimeframes() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandate2 = mandate.updateTimestamps(
                    Instant.now().minus(15, ChronoUnit.DAYS), 10);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandate2);
            tx.command(mandate2.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.failsWith("expired date must be in the future");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState update = mandate.updateTimestamps(
                    Instant.now().plus(1, ChronoUnit.DAYS), 10);
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, update);
            tx.command(update.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void mandate_update_emptyAllowances() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().toList());
            // already fails here due to error in serializing empty EnumSet
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(mandatePnC.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.failsWith("one line of business must be choosen");
            return null;
        });
    }

    @Test
    public void mandate_update_allowances() {
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().PnC().toList());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(mandatePnC.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().IL().toList());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(mandatePnC.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().GL().toList());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(mandatePnC.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().Health().toList());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(mandatePnC.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState mandate = newMandate();
            MandateState mandatePnC = mandate.updateAllowedBusiness(new LineOfBusiness().LnS().toList());
            tx.input(MandateContract.ID, mandate);
            tx.output(MandateContract.ID, mandatePnC);
            tx.command(mandatePnC.getParticipantKeys(), new MandateContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_denied() {
        transaction(ledgerServices, tx -> {
            MandateState withdraw = newMandate().withdraw();
            tx.input(MandateContract.ID, withdraw);
            tx.output(MandateContract.ID, withdraw);
            tx.command(withdraw.getParticipantKeys(), new MandateContract.Commands.Deny());
            tx.failsWith("input mandate must be REQUESTED / ACCEPTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState deny = newMandate().deny();
            tx.input(MandateContract.ID, deny);
            tx.output(MandateContract.ID, deny);
            tx.command(deny.getParticipantKeys(), new MandateContract.Commands.Deny());
            tx.failsWith("input mandate must be REQUESTED / ACCEPTED");
            return null;
        });

        transaction(ledgerServices, tx -> {
            MandateState accept = newMandate().accept();
            MandateState deny = accept.deny();
            tx.input(MandateContract.ID, accept);
            tx.output(MandateContract.ID, deny);
            tx.command(deny.getParticipantKeys(), new MandateContract.Commands.Deny());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            MandateState requested = newMandate();
            MandateState deny = requested.deny();
            tx.input(MandateContract.ID, requested);
            tx.output(MandateContract.ID, deny);
            tx.command(deny.getParticipantKeys(), new MandateContract.Commands.Deny());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_accepted() {
        transaction(ledgerServices, tx -> {
            MandateState accept = newMandate().accept();
            tx.input(MandateContract.ID, accept);
            tx.output(MandateContract.ID, accept);
            tx.command(accept.getParticipantKeys(), new MandateContract.Commands.Accept());
            tx.failsWith("input mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState withdraw = newMandate().withdraw();
            tx.input(MandateContract.ID, withdraw);
            tx.output(MandateContract.ID, withdraw);
            tx.command(withdraw.getParticipantKeys(), new MandateContract.Commands.Accept());
            tx.failsWith("input mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState deny = newMandate().deny();
            tx.input(MandateContract.ID, deny);
            tx.output(MandateContract.ID, deny);
            tx.command(deny.getParticipantKeys(), new MandateContract.Commands.Accept());
            tx.failsWith("input mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState requested = newMandate();
            MandateState accepted = requested.accept();
            tx.input(MandateContract.ID, requested);
            tx.output(MandateContract.ID, accepted);
            tx.command(accepted.getParticipantKeys(), new MandateContract.Commands.Accept());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void mandate_withdraw() {
        transaction(ledgerServices, tx -> {
            MandateState accept = newMandate().accept();
            tx.input(MandateContract.ID, accept);
            tx.output(MandateContract.ID, accept);
            tx.command(accept.getParticipantKeys(), new MandateContract.Commands.Withdraw());
            tx.failsWith("input mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState withdraw = newMandate().withdraw();
            tx.input(MandateContract.ID, withdraw);
            tx.output(MandateContract.ID, withdraw);
            tx.command(withdraw.getParticipantKeys(), new MandateContract.Commands.Withdraw());
            tx.failsWith("input mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState deny = newMandate().deny();
            tx.input(MandateContract.ID, deny);
            tx.output(MandateContract.ID, deny);
            tx.command(deny.getParticipantKeys(), new MandateContract.Commands.Withdraw());
            tx.failsWith("input mandate must be REQUESTED");
            return null;
        });
        transaction(ledgerServices, tx -> {
            MandateState requested = newMandate();
            MandateState withdraw = requested.withdraw();
            tx.input(MandateContract.ID, requested);
            tx.output(MandateContract.ID, requested.withdraw());
            tx.command(withdraw.getParticipantKeys(), new MandateContract.Commands.Withdraw());
            tx.verifies();
            return null;
        });
    }

}
