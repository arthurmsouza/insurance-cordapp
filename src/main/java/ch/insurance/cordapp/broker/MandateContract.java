package ch.insurance.cordapp.broker;

import ch.insurance.cordapp.StateVerifier;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class MandateContract implements Contract {
    public static String ID = "ch.insurance.cordapp.broker.MandateContract";

    public interface Commands extends CommandData {
        class Request implements Commands { }
        class Update implements Commands { }
        class Accept implements Commands { }
        class Deny implements Commands { }
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        StateVerifier verifier = new StateVerifier(tx, MandateContract.Commands.class);
        CommandData commandData = verifier.command();
        if (commandData instanceof MandateContract.Commands.Request) {
            verifyCreation(tx, verifier);
        } else if (commandData instanceof MandateContract.Commands.Accept) {
            verifyAcceptance(tx, verifier);
        } else if (commandData instanceof MandateContract.Commands.Deny) {
            verifyDenial(tx, verifier);
        } else if (commandData instanceof MandateContract.Commands.Update) {
            verifyUpdate(tx, verifier);
        }
    }

    private void verifyCreation(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            verifier.input().empty("input must be empty");
            MandateState mandate = verifier
                    .output().one().one(MandateState.class)
                    .differentParty(
                            "client", p1 -> ((MandateState)p1).getClient(),
                            "broker", p2 -> ((MandateState)p2).getBroker())
                    .signer("client must be the signer", state -> ((MandateState)state).getClient())
                    .object();
            req.using("mandate must be not accepted",
                    !mandate.isAccepted());
            this.verifyAllowances(mandate);
            this.verifyTimestamps(mandate);
            return null;
        });
    }

    private void verifyTimestamps(MandateState mandate) {
        requireThat(req -> {
            req.using(
                    "expired date must be later than start",
                    mandate.getExpiredAt().isAfter(mandate.getStartAt()));
            req.using(
                    "difference between start and end must be at least 1 day",
                    mandate.getStartAt().plus(1, ChronoUnit.DAYS).isBefore(
                            mandate.getExpiredAt()));
            req.using(
                    "expired date must be in the future",
                    mandate.getExpiredAt().isAfter(Instant.now()));
            req.using(
                    "start date must be in the future",
                    mandate.getStartAt().isAfter(Instant.now()));
            return null;
        });
    }


    private void verifyAllowances(MandateState mandate) {
        requireThat(req -> {
            req.using(
                    "one line of business must be choosen",
                    mandate.isAllowGL() || mandate.isAllowHealth() || mandate.isAllowIL() || mandate.isAllowPnC());
            return null;
        });
    }
    private void verifySameValues(MandateState input, MandateState output) {
        requireThat(req -> {
            req.using("client must be the same",
                    input.getClient().equals(output.getClient()));
            req.using("broker must be the same",
                    output.getBroker().equals(input.getBroker()));
            req.using("id must be same",
                    input.getId().equals(output.getId()));
            return null;
        });
    }

    private void verifyInputOnUpdate(MandateState input) {
        requireThat(req -> {
            req.using("input mandate must be not accepted",
                    !input.isAccepted());
            return null;
        });
    }


    private void verifyUpdate(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            MandateState input = verifier
                    .input().one().one(MandateState.class)
                    .signer("client must be the signer", state -> ((MandateState)state).getClient())
                    .object();
            MandateState output = verifier
                    .output().one().one(MandateState.class)
                    .object();
            req.using("mandate is still not accepted",
                    !output.isAccepted());
            this.verifyInputOnUpdate(input);
            this.verifyAllowances(output);
            this.verifyTimestamps(output);
            this.verifySameValues(input, output);
            return null;
        });
    }

    private void verifyAcceptance(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {

            MandateState input = verifier
                    .input().one().one(MandateState.class)
                    .participantsAreSigner("all participants must be signer")
                    .object();
            MandateState output = verifier
                    .output().one().one(MandateState.class)
                    .object();
            req.using("mandate must be accepted",
                    output.isAccepted());
            this.verifyInputOnUpdate(input);
            this.verifyAllowances(output);
            this.verifyTimestamps(output);
            this.verifySameValues(input, output);

            return null;
        });
    }
    private void verifyDenial(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            MandateState input = verifier
                    .input().moreThanOne().one(MandateState.class)
                    .object();
            verifier
                    .output().empty();

            // Checks the required parties have signed.
            verifier.input().participantsAreSigner("Both owner and issuer together only must sign token state settle transaction");
            return null;
        });
    }
}