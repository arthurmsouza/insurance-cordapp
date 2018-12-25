package ch.insurance.cordapp.token;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockServices;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;

import java.util.Currency;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;

public class BaseTests {

    protected MockNetwork network;
    protected StartedMockNode nodeA;
    protected StartedMockNode nodeB;
    protected StartedMockNode nodeC;
    protected Amount amount9CHF = Amount.parseCurrency("9 CHF");
    protected Amount amount90CHF = Amount.parseCurrency("90 CHF");
    protected Amount amount99CHF = Amount.parseCurrency("99 CHF");

    private final TestIdentity ledgerTestID = new TestIdentity(new CordaX500Name("Test Node", "Uster", "CH"));
    protected MockServices ledgerServices = null;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("ch.insurance.cordapp", "net.corda.finance"));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        nodeC = network.createPartyNode(null);
        network.runNetwork();
        ledgerServices = new MockServices(
                ImmutableList.of("ch.insurance.cordapp.token"),
                ledgerTestID
        );
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }



    protected SignedTransaction selfIssueCash(StartedMockNode party,
                                              Amount<Currency> amount) throws InterruptedException, ExecutionException {
        Party notary = party.getServices().getNetworkMapCache().getNotaryIdentities().get(0);
        OpaqueBytes issueRef = OpaqueBytes.of("0".getBytes());
        CashIssueFlow.IssueRequest issueRequest = new CashIssueFlow.IssueRequest(amount, issueRef, notary);
        CashIssueFlow flow = new CashIssueFlow(issueRequest);
        return party.startFlow(flow).get().getStx();
    }

    // Helper for extracting the cash output owned by a the node.
    protected Cash.State getCashOutputByOwner(
            List<Cash.State> cashStates,
            StartedMockNode node) {
        Cash.State ownersCashState = null;
        for (Cash.State cashState : cashStates) {
            Party cashOwner = node.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(cashState.getOwner());
            if (cashOwner == chooseIdentity(node.getInfo())) {
                ownersCashState = cashState;
                break;
            }
        }
        return ownersCashState;
    }
}
