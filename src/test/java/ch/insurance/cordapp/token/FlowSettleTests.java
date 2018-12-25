package ch.insurance.cordapp.token;

import ch.insurance.cordapp.token.flows.TokenIssue;
import ch.insurance.cordapp.token.flows.TokenSettlement;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.Currencies.SWISS_FRANCS;
import static org.junit.Assert.assertEquals;

public class FlowSettleTests extends BaseTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void transactionSettlingByFlow() throws Exception {
        selfIssueCash(nodeB, SWISS_FRANCS(99));

        // create tokenstate to get new ID
        // current: A --> B
        Party owner = nodeB.getInfo().getLegalIdentities().get(0);
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(owner, amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();
        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);

        UniqueIdentifier linearId = output.getLinearId();

        TokenSettlement.TokenSettlementFlow settleFlow = new TokenSettlement.TokenSettlementFlow(linearId, amount99CHF);
        CordaFuture<SignedTransaction> futureTransfer = nodeB.startFlow(settleFlow);
        network.runNetwork();

        SignedTransaction signedSettleTransaction = futureTransfer.get();
        List<ContractState> outputs = signedSettleTransaction.getTx().getOutputStates();
        List<Cash.State> outputCash = signedSettleTransaction.getTx().outputsOfType(Cash.State.class);

        Cash.State change = this.getCashOutputByOwner(outputCash, nodeB);
        Cash.State received = this.getCashOutputByOwner(outputCash, nodeA);

        assertEquals(change, null);
        assertEquals("payment down to A", SWISS_FRANCS(99), withoutIssuer(received.getAmount()));

    }


    @Test
    public void transactionNotFullySettledByFlow() throws Exception {
        selfIssueCash(nodeB, SWISS_FRANCS(99));

        // create tokenstate to get new ID
        // current: A --> B
        Party owner = nodeB.getInfo().getLegalIdentities().get(0);
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(owner, amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);

        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);

        UniqueIdentifier linearId = output.getLinearId();

        TokenSettlement.TokenSettlementFlow settleFlow = new TokenSettlement.TokenSettlementFlow(linearId, amount90CHF);
        CordaFuture<SignedTransaction> futureTransfer = nodeB.startFlow(settleFlow);
        network.runNetwork();

        SignedTransaction signedSettleTransaction = futureTransfer.get();
        List<ContractState> outputs = signedSettleTransaction.getTx().getOutputStates();
        List<Cash.State> outputCash = signedSettleTransaction.getTx().outputsOfType(Cash.State.class);
        List<TokenState> outputState = signedSettleTransaction.getTx().outputsOfType(TokenState.class);

        Cash.State change = this.getCashOutputByOwner(outputCash, nodeB);
        Cash.State received = this.getCashOutputByOwner(outputCash, nodeA);

        assertEquals("partially settled has some states left", 1, outputState.size());
        assertEquals("payment still open from B", amount9CHF, withoutIssuer(change.getAmount()));
        assertEquals("payment down to A", amount90CHF, withoutIssuer(received.getAmount()));

    }



    @Test(expected = FlowException.class)
    public void transactionNotEnoughCashByFlow() throws Exception {
        selfIssueCash(nodeB, SWISS_FRANCS(1));

        // create tokenstate to get new ID
        // current: A --> B
        Party owner = nodeB.getInfo().getLegalIdentities().get(0);
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(owner, amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();
        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);
        UniqueIdentifier linearId = output.getLinearId();

        // Attempt settlement.
        exception.expectMessage("net.corda.core.flows.FlowException");

        TokenSettlement.TokenSettlementFlow settleFlow = new TokenSettlement.TokenSettlementFlow(linearId, amount90CHF);
        CordaFuture<SignedTransaction> futureTransfer = nodeB.startFlow(settleFlow);
        network.runNetwork();
        
    }


}