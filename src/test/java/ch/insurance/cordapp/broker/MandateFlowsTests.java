package ch.insurance.cordapp.broker;

import ch.insurance.cordapp.verifier.StateVerifier;
import ch.insurance.cordapp.broker.MandateState.LineOfBusiness;
import ch.insurance.cordapp.broker.flows.MandateRequestFlow;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class MandateFlowsTests extends BaseTests {

    @Override
    @Before
    public void setup() {
        this.setup(true);
    }

    private SignedTransaction newRequestFlow(LineOfBusiness allowedBusines) throws ExecutionException, InterruptedException {
        MandateRequestFlow.Initiator flow = new MandateRequestFlow.Initiator(
                bobTheBrokerNode.getInfo().getLegalIdentities().get(0),
                Instant.now().plus(10, ChronoUnit.DAYS),
                allowedBusines.makeImmutable());
        CordaFuture<SignedTransaction> future = aliceTheCustomerNode.startFlow(flow);
        network.runNetwork();
        return future.get();
    }

    @Test
    public void request_transaction_ConstructedByFlowUsesTheCorrectNotary() throws Exception {
        SignedTransaction tx = this.newRequestFlow(LineOfBusiness.all());

        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        verifier.output().one().object();
        TransactionState output = tx.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
    }

    @Test
    public void request_transaction_ConstructedByFlowHasRightParties() throws Exception {
        SignedTransaction tx = this.newRequestFlow(LineOfBusiness.all());
        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        MandateState mandate = verifier
                .output().one()
                .one(MandateState.class)
                .object();

        assertEquals("alice is the expetected client", aliceTheCustomer, mandate.getClient());
        assertEquals("Bob is the expeted broker", bobTheBroker, mandate.getBroker());
    }

}