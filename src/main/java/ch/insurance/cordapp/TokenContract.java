package ch.insurance.cordapp;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandData;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.utils.StateSumming;

import java.util.Currency;
import java.util.List;

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
        StateVerifier verifier = StateVerifier.fromTransaction(tx, Commands.class);
        CommandData commandData = verifier.command();
        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx, verifier);
        } else if (commandData instanceof Commands.Transfer) {
            verifyTransfer(tx, verifier);
        } else if (commandData instanceof Commands.Settle) {
            verifySettle(tx, verifier);
        }
	}

    private void verifyIssue(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            verifier.input().empty();
            TokenState tokenState = verifier
                    .output().one().one(TokenState.class)
                    .amountNot0("amount",
                            x -> ((TokenState)x).getAmount())
                    .differentParty(
                        "issuser", p1 -> ((TokenState)p1).getIssuer(),
                        "owner", p2 -> ((TokenState)p2).getOwner())
                    .signer("issuer", state -> ((TokenState)state).getIssuer())
                    .object();

            /*
            req.using("issuer and owner must be different parties.",
                    !tokenState.getIssuer().equals(tokenState.getOwner()));
            final List<PublicKey> requiredSigners = command.getSigners();
            req.using("issuer must be a signer.",
                    requiredSigners.contains(tokenState.getIssuer().getOwningKey()));
            */
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {

            TokenState input = verifier
                    .input().one().one(TokenState.class)
                    .signer("old issuer must sign transfer to new owner",
                            x -> ((TokenState)x).getIssuer())
                    .object();
            TokenState output = verifier
                    .output().one().one(TokenState.class)
                    .signer("new owner needs to sign",
                            x -> ((TokenState)x).getIssuer())
                    .object();
            req.using("issuer and owner needs to be different parties",
                    !input.getIssuer().equals(output.getOwner()));
            req.using("Only current owner can transfer it to new owner",
                    output.getIssuer().equals(input.getOwner()));

            /*
            final List<PublicKey> requiredSigners = command.getSigners();
            req.using("new owner needs to sign",
                    requiredSigners.contains(output.getIssuer().getOwningKey()));
            req.using("old issuer must sign transfer to new owner",
                    !requiredSigners.contains(input.getIssuer().getOwningKey()));
                    */
            return null;
        });
    }
    private void verifySettle(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            TokenState tokenInputState = verifier
                    .input().moreThanOne().one(TokenState.class)
                    .object();

            // Check there are output cash states.
            // the new output cash will belong to the issuer payed by the owner
            // We don't care about cash inputs, the Cash contract handles those.
            List<Cash.State> acceptableCash = verifier
                    .output().output(Cash.State.class)
                    .notEmpty()
                    .filterWhere(x -> ((Cash.State)x).getOwner().equals(tokenInputState.getIssuer()))
                    .notEmpty("There must be output cash paid to the issuer party")
                    .list();

            // Sum the cash being sent to us (we don't care about the issuer).
            Amount<Currency> sumAcceptableCash = withoutIssuer(StateSumming.sumCash(acceptableCash));
            Amount<Currency> amountOutstanding = tokenInputState.getAmount().minus(tokenInputState.getPaid());
            req.using("The amount settled cannot be more than the amount outstanding.",
                    amountOutstanding.compareTo(sumAcceptableCash) >= 0);

            // Check to see if we need an output token state or not.
            if (amountOutstanding.equals(sumAcceptableCash)) {
                // If the obligation has been fully settled then there should be no token state output state.
                verifier.output(TokenState.class)
                        .empty("There must be no output token state as it has been fully settled");
            } else {
                // If the obligation has been partially settled then it should still exist.
                TokenState tokenOutputState = verifier
                        .output()
                        .one(TokenState.class)
                        .notEmpty()
                        .one("There must be one output token state")
                        .object();

                // Check only the paid property changes.
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
            verifier.input().participantsAreSigner("Both owner and issuer together only must sign token state settle transaction");
            return null;
        });
    }

}