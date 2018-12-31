package ch.insurance.cordapp.broker;

import ch.insurance.cordapp.FlowHelper;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockServices;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;

import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

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

        if (withNodes) {
            network = new MockNetwork(ImmutableList.of("ch.insurance.cordapp", "net.corda.finance"));
            aliceTheCustomerNode = network.createPartyNode(aliceID.getName());
            bobTheBrokerNode = network.createPartyNode(bobID.getName());
            cesarTheInsurerNode = network.createPartyNode(cesarID.getName());

            aliceTheCustomer = aliceTheCustomerNode.getInfo().getLegalIdentities().get(0);
            bobTheBroker = bobTheBrokerNode.getInfo().getLegalIdentities().get(0);
            cesarTheInsurer = cesarTheInsurerNode.getInfo().getLegalIdentities().get(0);

            network.runNetwork();
        } else {
            aliceTheCustomer = aliceID.getParty();
            bobTheBroker = bobID.getParty();
            cesarTheInsurer = cesarID.getParty();
        }


        ledgerServices = new MockServices(
                ImmutableList.of("ch.insurance.cordapp"),
                aliceTheCustomer.getName()
        );

    }

    @After
    public void tearDown() {
        if (network != null) network.stopNodes();
    }


    protected List<PublicKey> getPublicKeys(Party... parties) {
        ImmutableList<Party> list = ImmutableList.copyOf(parties);
        return list.stream().map(party -> party.getOwningKey()).collect(Collectors.toList());
    }

    protected Party getParty(StartedMockNode node) {
        return node.getInfo().getLegalIdentities().get(0);
    }
}
