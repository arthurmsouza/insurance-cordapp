package ch.insurance.cordapp;

import net.corda.core.contracts.*;
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
public class TokenContract implements Contract {
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
        Commands commandData = commands.get(0).getValue();

        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx, command);
        } else if (commandData instanceof Commands.Transfer) {
            verifyTransfer(tx, command);
        } else if (commandData instanceof Commands.Settle) {
            verifySettle(tx, command);
        }
	}

    private void verifyIssue(LedgerTransaction tx, Command<Commands> command) {
        List<ContractState> inputStates = tx.getInputStates();
        List<TransactionState<ContractState>> outputs = tx.getOutputs();

        List<TokenState> tokenStates = tx.outputsOfType(TokenState.class);
        if (inputStates.size() != 0) throw new IllegalArgumentException("Must be no input.");
        if (outputs.size() != 1) throw new IllegalArgumentException("Must be one output.");
        if (tokenStates.size() != 1) throw new IllegalArgumentException("output should be an TokenState.");
        if ((tx.outputsOfType(TokenState.class).get(0).getAmount().getQuantity() == 0)) throw new IllegalArgumentException("output state needed");
        if (tokenStates.get(0).getIssuer().equals(tokenStates.get(0).getOwner()))
            throw new IllegalArgumentException("issuer and owner cannot be the same");

        // Grabbing the transaction's contents.
        final List<PublicKey> requiredSigners = command.getSigners();
        if (!requiredSigners.contains(tokenStates.get(0).getIssuer().getOwningKey())) {
            throw new IllegalArgumentException();
        }
    }
    private void verifyTransfer(LedgerTransaction tx, Command<Commands> command) {
        List<ContractState> inputStates = tx.getInputStates();
        List<TransactionState<ContractState>> outputs = tx.getOutputs();

        List<TokenState> tokenInputStates = tx.inputsOfType(TokenState.class);
        List<TokenState> tokenOutputStates = tx.outputsOfType(TokenState.class);
        if (inputStates.size() != 1) throw new IllegalArgumentException("Must be one input with id.");
        if (outputs.size() != 1) throw new IllegalArgumentException("Must be one output.");
        if (tokenInputStates.size() != 1) throw new IllegalArgumentException("input should be an TokenState.");
        if (tokenOutputStates.size() != 1) throw new IllegalArgumentException("output should be an TokenState.");
        if (tokenOutputStates.get(0).getIssuer().equals(tokenOutputStates.get(0).getOwner()))
            throw new IllegalArgumentException("issuer and owner cannot be the same");
        //validate old owner is new issuer
        if (!tokenOutputStates.get(0).getIssuer().equals(tokenInputStates.get(0).getOwner()))
            throw new IllegalArgumentException("Only current owner can transfer it to new owner");

        // Grabbing the transaction's contents.
        final List<PublicKey> requiredSigners = command.getSigners();
        if (!requiredSigners.contains(tokenOutputStates.get(0).getIssuer().getOwningKey())) {
            throw new IllegalArgumentException("new owner needs to sign");
        }
        //assumption: old issuer must sign transfer to new owner
        if (!requiredSigners.contains(tokenInputStates.get(0).getIssuer().getOwningKey())) {
            throw new IllegalArgumentException("old issuer must sign transfer to new owner");
        }
    }
    private void verifySettle(LedgerTransaction tx, Command<Commands> command) {
        requireThat(req -> {
            // Grabbing the transaction's contents.
            final List<PublicKey> requiredSigners = command.getSigners();

            // Check for the presence of an input token state.
            List<TokenState> tokenInputStates = tx.inputsOfType(TokenState.class);
            req.using("There must be one input token state.", tokenInputStates.size() == 1);

            // Check there are output cash states.
            // We don't care about cash inputs, the Cash contract handles those.
            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
            req.using("There must be output cash.", !cash.isEmpty());

            // Check that the cash is being assigned to us.
            TokenState tokenInputState = tokenInputStates.get(0);
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