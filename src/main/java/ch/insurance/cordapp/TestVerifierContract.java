package ch.insurance.cordapp;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TestVerifierContract extends BaseContract {
    public static String ID = "ch.insurance.cordapp.TestVerifierContract";

    public interface Commands extends CommandData {
        class TestIssueNormal implements Commands { }
        class TestIssueMoreThanOne implements Commands { }
        class TestTransfer_1_1 implements Commands { }
        class TestTransfer_2_1 implements Commands { }
        class Test4 implements Commands { }
        class Test5 implements Commands { }
    }

	@Override
	public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<Command<Commands>> commands = tx.commandsOfType(Commands.class);
        if (commands.size() != 1) throw new IllegalArgumentException();

        Command<Commands> command = commands.get(0);
        Commands commandData = commands.get(0).getValue();

        StateVerifier verifier = new StateVerifier(tx, Commands.class);

        if (commandData instanceof Commands.TestIssueNormal) {
            verifier.input().empty();
            verifier.output().one().one(TokenState.class).signer("issuer", x -> ((TokenState)x).getIssuer());

        } else if (commandData instanceof Commands.TestIssueMoreThanOne) {
            verifier.input().empty();
            verifier.output().moreThanOne().signer("issuer", x -> ((TokenState)x).getIssuer());

        } else if (commandData instanceof Commands.TestTransfer_1_1) {
            verifier.input().notEmpty().one().moreThanZero();
            verifier.output().notEmpty().one().moreThanZero();
            verifier.output(TokenState.class).one()
                    .signer("issuer", x -> ((TokenState)x).getIssuer())
                    .differentParty(
                            "issuer", x -> ((TokenState)x).getIssuer(),
                            "owner", x -> ((TokenState)x).getOwner()
                    );

        } else if (commandData instanceof Commands.TestTransfer_2_1) {
            verifier.input().notEmpty().input(TokenState.class).count(2).max(2).min(2);
            verifier.output().notEmpty().output(TokenState.class).one().min(1).max(1).count(1);
            verifier.input(TokenState.class).moreThanZero().moreThanZero(2).moreThanOne(2);
            verifier.output(TokenState.class).one().moreThanZero().moreThanZero(1);
        }
	}

}