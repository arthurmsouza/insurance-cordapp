package ch.insurance.cordapp;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.dsl.TransactionDSL;
import net.corda.testing.dsl.TransactionDSLInterpreter;
import net.corda.testing.node.MockServices;
import org.junit.Before;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractVerifierTests {
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private MockServices ledgerServices = null;
    private Amount amount2CHF = Amount.parseCurrency("2 CHF");
    private Amount amount1CHF = Amount.parseCurrency("1 CHF");
    private Amount amount0CHF = Amount.parseCurrency("0 CHF");
    private TokenState tokenState = new TokenState(
            alice.getParty(), bob.getParty(), amount1CHF);

    @Before
    public void setup() {
        ledgerServices = new MockServices(
                ImmutableList.of("ch.insurance.cordapp"),
                new TestIdentity(new CordaX500Name("TestId", "", "GB"))

        );
    }

    @Test
    public void tokenIssue_Standard() {
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueNormal());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueNormal());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueNormal());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void tokenIssue_MoreThanOne() {
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueMoreThanOne());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueMoreThanOne());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueMoreThanOne());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestIssueMoreThanOne());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void tokenTransfer_1_1() {
        transaction(ledgerServices, tx -> {
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.output(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.input(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.input(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.fails();
            return null;
        });
        transaction(ledgerServices, tx -> {
            tx.input(TestVerifierContract.ID, tokenState);
            tx.output(TestVerifierContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TestVerifierContract.Commands.TestTransfer_1_1());
            tx.verifies();
            return null;
        });
    }
}
