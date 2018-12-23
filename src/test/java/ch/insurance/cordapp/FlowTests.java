package ch.insurance.cordapp;

import ch.insurance.cordapp.flows.TokenIssue;
import ch.insurance.cordapp.flows.TokenSettlement;
import ch.insurance.cordapp.flows.TokenTransfer;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.Currencies.SWISS_FRANCS;
import static org.junit.Assert.assertEquals;

public class FlowTests extends BaseTests{

    @Test
    public void transactionConstructedByFlowUsesTheCorrectNotary() throws Exception {
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
    }

    @Test
    public void transactionConstructedByFlowHasOneTokenStateOutputWithTheCorrectAmountAndOwner() throws Exception {
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);

        assertEquals(nodeB.getInfo().getLegalIdentities().get(0), output.getOwner());
        assertEquals(Amount.parseCurrency("99 CHF"), output.getAmount());
    }

    @Test
    public void transactionConstructedByFlowHasOneOutputUsingTheCorrectContract() throws Exception {
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals("ch.insurance.cordapp.TokenContract", output.getContract());
    }

    @Test
    public void transactionConstructedByFlowHasOneIssueCommand() throws Exception {
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assert(command.getValue() instanceof TokenContract.Commands.Issue);
    }

    @Test
    public void transactionConstructedByFlowHasOneCommandWithTheIssueAsASigner() throws Exception {
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assertEquals(1, command.getSigners().size());
        assert(command.getSigners().contains(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey()));
    }

    @Test
    public void transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() throws Exception {
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(0, signedTransaction.getTx().getInputs().size());
        // The single attachment is the contract attachment.
        assertEquals(1, signedTransaction.getTx().getAttachments().size());
        assertEquals(null, signedTransaction.getTx().getTimeWindow());

    }


    @Test
    public void transactionTransferredByFlow() throws Exception {
        // create tokenstate to get new ID
        // old: A --> B
        Party oldOwner = nodeB.getInfo().getLegalIdentities().get(0);
        TokenIssue.TokenIssueFlow flow = new TokenIssue.TokenIssueFlow(oldOwner, amount99CHF);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);

        // get ID to transfer to nodeC
        UniqueIdentifier linearId = output.getLinearId();

        // new: B --> C
        Party newOwner = nodeC.getInfo().getLegalIdentities().get(0);
        TokenTransfer.TokenTransferFlow transferFlow = new TokenTransfer.TokenTransferFlow(newOwner, linearId);
        CordaFuture<SignedTransaction> futureTransfer = nodeB.startFlow(transferFlow);
        network.runNetwork();
        SignedTransaction signedTransferTransaction = futureTransfer.get();
        TokenState transferOutput = signedTransferTransaction.getTx().outputsOfType(TokenState.class).get(0);

        assertEquals(newOwner, transferOutput.getOwner());
        assertEquals(oldOwner, transferOutput.getIssuer());

    }


}