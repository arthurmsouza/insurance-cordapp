package ch.insurance.cordapp;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.utils.StateSumming;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract extends BaseContract {
    public static String ID = "ch.insurance.cordapp.TokenContract";

    public interface Commands extends CommandData {
        class Issue implements Commands { }
        class Transfer implements Commands { }
        class Settle implements Commands { }
    }

	@Override
	public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<Command<Commands>> commands = tx.commandsOfType(Commands.class);
        if (commands.size() != 1) throw new IllegalArgumentException();

        Command<Commands> command = commands.get(0);
        CommandData commandData = command.getValue();

        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx, command);
        } else if (commandData instanceof Commands.Transfer) {
            verifyTransfer(tx, command);
        } else if (commandData instanceof Commands.Settle) {
            verifySettle(tx, command);
        }
	}

    private void verifyIssue(LedgerTransaction tx, Command<Commands> command) {
        StateVerifier verifier = new StateVerifier(tx, Commands.class);
        requireThat(req -> {
            verifier.input().empty().verifyAll();
            verifier.output().one().one(TokenState.class).verifyAll();
            return null;
        });

        TokenState tokenState = this.oneOutput(tx, TokenState.class);
        this.requireAmountNone0(tokenState.getAmount());
        requireThat(req -> {
            req.using("issuer and owner must be different parties.",
                    !tokenState.getIssuer().equals(tokenState.getOwner()));
            final List<PublicKey> requiredSigners = command.getSigners();
            req.using("issuer must be a signer.",
                    requiredSigners.contains(tokenState.getIssuer().getOwningKey()));
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, Command<Commands> command) {
        TokenState tokenInputState = oneInput(tx, TokenState.class);
        TokenState tokenOutputState = oneOutput(tx, TokenState.class);
        this.requireUpdateCounts(tx, 1, TokenState.class);
        TokenState input = oneInput(tx, TokenState.class);
        TokenState output = oneInput(tx, TokenState.class);
        requireThat(req -> {
            final List<PublicKey> requiredSigners = command.getSigners();
            req.using("issuer and owner needs to be different parties",
                    !input.getIssuer().equals(output.getOwner()));
            req.using("Only current owner can transfer it to new owner",
                    output.getIssuer().equals(input.getOwner()));
            req.using("new owner needs to sign",
                    requiredSigners.contains(output.getIssuer().getOwningKey()));
            req.using("old issuer must sign transfer to new owner",
                    !requiredSigners.contains(input.getIssuer().getOwningKey()));
            return null;
        });
    }
    private void verifySettle(LedgerTransaction tx, Command<Commands> command) {
        requireThat(req -> {
            // Grabbing the transaction's contents.
            final List<PublicKey> requiredSigners = command.getSigners();
            this.requireTransferCounts(tx, 1, TokenState.class, 1, Cash.State.class);

            TokenState tokenInputState = oneInput(tx, TokenState.class);

            // Check there are output cash states.
            // We don't care about cash inputs, the Cash contract handles those.
            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
            req.using("There must be output cash.", !cash.isEmpty());

            List<Cash.State> acceptableCash = cash.stream().filter(
                        it -> it.getOwner().equals(tokenInputState.getIssuer())).collect(Collectors.toList());
            req.using("There must be output cash paid to the recipient.", !acceptableCash.isEmpty());

            // Sum the cash being sent to us (we don't care about the issuer).
            Amount<Currency> sumAcceptableCash = withoutIssuer(StateSumming.sumCash(acceptableCash));
            Amount<Currency> amountOutstanding = tokenInputState.getAmount().minus(tokenInputState.getPaid());
            req.using("The amount settled cannot be more than the amount outstanding.", amountOutstanding.compareTo(sumAcceptableCash) >= 0);

            List<TokenState> tokenOutputStates = tx.outputsOfType(TokenState.class);
            // Check to see if we need an output token state or not.
            if (amountOutstanding.equals(sumAcceptableCash)) {
                // If the obligation has been fully settled then there should be no token state output state.
                req.using("There must be no output token state as it has been fully settled.", tokenOutputStates.isEmpty());
            } else {
                // If the obligation has been partially settled then it should still exist.
                req.using("There must be one output token state.", tokenOutputStates.size() == 1);

                // Check only the paid property changes.
                TokenState tokenOutputState = tokenOutputStates.get(0);
                req.using("The amount may not change when settling.", tokenInputState.getAmount().equals(tokenOutputState.getAmount()));
                req.using("The owner may not change when settling.", tokenInputState.getOwner().equals(tokenOutputState.getOwner()));
                req.using("The issuer may not change when settling.", tokenInputState.getIssuer().equals(tokenOutputState.getIssuer()));
                req.using("The linearId may not change when settling.", tokenInputState.getLinearId().equals(tokenOutputState.getLinearId()));

                // Check the paid property is updated correctly.
                req.using("Paid property incorrectly updated.",
                        tokenOutputState.getPaid().equals(
                                tokenInputState.getPaid().plus(sumAcceptableCash)));
            }
            // Checks the required parties have signed.
            req.using("Both owner and issuer together only must sign token state settle transaction.",
                    requiredSigners.equals(tokenInputState.getParticipantKeys()));
            return null;
        });
    }

}