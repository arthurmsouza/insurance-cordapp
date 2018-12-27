package ch.insurance.cordapp.broker;

import ch.insurance.cordapp.FlowHelper;
import ch.insurance.cordapp.broker.MandateState.Line;
import ch.insurance.cordapp.broker.MandateState.LineOfBusiness;
import ch.insurance.cordapp.broker.flows.*;
import ch.insurance.cordapp.verifier.StateVerifier;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    private SignedTransaction newAcceptFlow(UniqueIdentifier id, Instant startAt, long amountDuration, TemporalUnit unit) throws ExecutionException, InterruptedException {
        MandateAcceptFlow.Initiator flow = new MandateAcceptFlow.Initiator(
                id, startAt, amountDuration, unit);
        CordaFuture<SignedTransaction> future = bobTheBrokerNode.startFlow(flow);
        network.runNetwork();
        return future.get();
    }
    private SignedTransaction newDenyFlow(UniqueIdentifier id) throws ExecutionException, InterruptedException {
        MandateDenyFlow.Initiator flow = new MandateDenyFlow.Initiator(id);
        CordaFuture<SignedTransaction> future = bobTheBrokerNode.startFlow(flow);
        network.runNetwork();
        return future.get();
    }
    private SignedTransaction newUpdateFlow(UniqueIdentifier id, LineOfBusiness allowedBusiness, Instant startAt) throws ExecutionException, InterruptedException {
        MandateUpdateFlow.Initiator flow = new MandateUpdateFlow.Initiator(
                id, allowedBusiness.makeImmutable(), startAt);
        CordaFuture<SignedTransaction> future = aliceTheCustomerNode.startFlow(flow);
        network.runNetwork();
        return future.get();
    }

    private SignedTransaction newWithdrawFlow(UniqueIdentifier id) throws ExecutionException, InterruptedException {
        MandateWithdrawFlow.Initiator flow = new MandateWithdrawFlow.Initiator(id);
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


    @Test
    public void accept_transaction_updateAndAccept() throws Exception {
        SignedTransaction tx = this.newRequestFlow(LineOfBusiness.all());
        StateVerifier verifier = StateVerifier.fromTransaction(tx, aliceTheCustomerNode.getServices());
        MandateState mandate = verifier
                .output().one()
                .one(MandateState.class)
                .object();


        SignedTransaction atx = this.newAcceptFlow(
                mandate.getId(), Instant.now().plus(10, ChronoUnit.DAYS),
                365, ChronoUnit.DAYS);
        verifier = StateVerifier.fromTransaction(atx, bobTheBrokerNode.getServices());
        MandateState acceptedMandate = verifier
                .output().one().one(MandateState.class)
                .object();

        assertTrue("mandate is updated and accepted", acceptedMandate.isAccepted());
        assertEquals("days between start + end is 365", 365,
                ChronoUnit.DAYS.between(acceptedMandate.getStartAt(), acceptedMandate.getExpiredAt()));
    }


    @Test
    public void deny_transaction_updateAndAccept() throws Exception {
        SignedTransaction tx = this.newRequestFlow(LineOfBusiness.all());
        StateVerifier verifier = StateVerifier.fromTransaction(tx, aliceTheCustomerNode.getServices());
        MandateState mandate = verifier
                .output().one()
                .one(MandateState.class)
                .object();

        SignedTransaction dtx = this.newDenyFlow(mandate.getId());
        verifier = StateVerifier.fromTransaction(dtx, bobTheBrokerNode.getServices());
        MandateState deniedMandate = verifier
                .output().one().one(MandateState.class)
                .object();

        assertTrue("mandate is updated and denied", deniedMandate.isDenied());
        assertEquals("days between start + end is 365", 365,
                ChronoUnit.DAYS.between(deniedMandate.getStartAt(), deniedMandate.getExpiredAt()));
    }


    private LocalDate get1stDayOfNextMonth() {
        YearMonth yearMonth = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()));
        return yearMonth.atEndOfMonth().plus(1, ChronoUnit.DAYS);
    }
    private Instant get1stDayOfNextMonth_Instant() {
        return this.get1stDayOfNextMonth().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
    @Test
    public void update_transaction_byclient() throws Exception {
        SignedTransaction tx = this.newRequestFlow(LineOfBusiness.all());
        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        MandateState mandate = verifier
                .output().one()
                .one(MandateState.class)
                .object();

        Instant startAt = get1stDayOfNextMonth_Instant();
        SignedTransaction atx = this.newUpdateFlow(
                mandate.getId(), new LineOfBusiness().PnC().Health(), startAt);
        verifier = StateVerifier.fromTransaction(atx, this.ledgerServices);
        MandateState updatedMandate = verifier
                .output().one().one(MandateState.class)
                .object();

        LineOfBusiness newAllowance = new LineOfBusiness(updatedMandate.getAllowedBusiness());
        assertTrue("mandate is updated and PnC is allowed", newAllowance.contains(Line.PnC));
        assertTrue("mandate is updated and Health is allowed", newAllowance.contains(Line.Health));

        assertTrue("mandate is updated and IL is NOT allowed", !newAllowance.contains(Line.IL));
        assertTrue("mandate is updated and GL is NOT allowed", !newAllowance.contains(Line.GL));
    }

    @Test
    public void withdraw_transaction_byclient() throws Exception {
        SignedTransaction stx = this.newRequestFlow(LineOfBusiness.all());
        StateVerifier verifier = StateVerifier.fromTransaction(stx, aliceTheCustomerNode.getServices());
        MandateState mandate = verifier
                .output().one()
                .one(MandateState.class)
                .object();

        SignedTransaction wtx = this.newWithdrawFlow(mandate.getId());

        verifier = StateVerifier.fromTransaction(wtx, aliceTheCustomerNode.getServices());
        MandateState withdrawnMandate = verifier
                .output().one().one(MandateState.class)
                .object();

        FlowHelper<MandateState> flowHelper = new FlowHelper<>(aliceTheCustomerNode.getServices());
        StateAndRef<MandateState> mandateState = aliceTheCustomerNode.transaction(() -> {
            return flowHelper.getLastStateByLinearId(MandateState.class, mandate.getId());
        });

        assertTrue("mandate found and must be WITHDRAWN",
                mandateState.getState().getData().isWithdrawn());

    }

}