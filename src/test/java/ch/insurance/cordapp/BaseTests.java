package ch.insurance.cordapp;

import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.testing.node.StartedMockNode;

import java.util.Currency;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;

public class BaseTests {

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
