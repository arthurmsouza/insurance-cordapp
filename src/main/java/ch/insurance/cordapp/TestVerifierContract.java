package ch.insurance.cordapp;

import ch.insurance.cordapp.BaseContract;
import ch.insurance.cordapp.TokenState;
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
public class TestVerifierContract extends BaseContract {
    public static String ID = "ch.insurance.cordapp.TestVerifierContract";

    public interface Commands extends CommandData {
        class TestIssueNormal implements Commands { }
        class Test2 implements Commands { }
        class Test3 implements Commands { }
        class Test4 implements Commands { }
        class Test5 implements Commands { }
    }

	@Override
	public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<Command<Commands>> commands = tx.commandsOfType(Commands.class);
        if (commands.size() != 1) throw new IllegalArgumentException();

        Command<Commands> command = commands.get(0);
        Commands commandData = commands.get(0).getValue();

        ContractStateVerifier verifier = new ContractStateVerifier(tx);

        if (commandData instanceof Commands.TestIssueNormal) {
            verifier.input().empty();
            verifier.output().one();

        } else if (commandData instanceof Commands.Test2) {

        } else if (commandData instanceof Commands.Test3) {

        } else if (commandData instanceof Commands.Test4) {

        } else if (commandData instanceof Commands.Test5) {

        }
	}

}