package ch.insurance.cordapp.broker;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockServices;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;

import java.time.Instant;

abstract public class BaseTests {
    protected final Instant start = Instant.now();

    protected final TestIdentity aliceID = new TestIdentity(new CordaX500Name("Alice", "Uster", "CH"));
    protected final TestIdentity bobID = new TestIdentity(new CordaX500Name("Bob", "Zurich", "CH"));
    protected final TestIdentity cesarID = new TestIdentity(new CordaX500Name("Cesar Insurance AG", "Winterthur", "CH"));
    protected MockNetwork network;
    protected StartedMockNode aliceTheCustomerNode;
    protected Party aliceTheCustomer;
    protected StartedMockNode bobTheBrokerNode;
    protected Party bobTheBroker;
    protected StartedMockNode cesarTheInsurerNode;
    protected Party cesarTheInsurer;

    protected MockServices ledgerServices = null;

    // must be called to initialize using setup(true | false) and annotate with @Before
    public abstract void setup();

    public void setup(boolean withNodes) {

        aliceTheCustomer = aliceID.getParty();
        bobTheBroker = bobID.getParty();
        cesarTheInsurer = cesarID.getParty();

        if (withNodes) {
            network = new MockNetwork(ImmutableList.of("ch.insurance.cordapp.broker", "net.corda.finance"));
            aliceTheCustomerNode = network.createPartyNode(aliceID.getName());
            bobTheBrokerNode = network.createPartyNode(bobID.getName());
            cesarTheInsurerNode = network.createPartyNode(cesarID.getName());
            network.runNetwork();
        }

        ledgerServices = new MockServices(
                ImmutableList.of("ch.insurance.cordapp.broker"),
                aliceID
        );

    }

    @After
    public void tearDown() {
        if (network != null) network.stopNodes();
    }


    protected Party getParty(StartedMockNode node) {
        return node.getInfo().getLegalIdentities().get(0);
    }
}
