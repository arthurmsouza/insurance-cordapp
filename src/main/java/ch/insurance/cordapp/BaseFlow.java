package ch.insurance.cordapp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseFlow<T extends ContractState> extends FlowLogic<SignedTransaction> {
    public BaseFlow() {
        super();
    }


    protected TransactionBuilder getTransactionBuilder(ImmutableList<PublicKey> requiredSigner, CommandData command) throws FlowException {
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(getFirstNotary());
        transactionBuilder.addCommand(command, requiredSigner);
        return transactionBuilder;
    }
    protected TransactionBuilder getMyTransactionBuilder(CommandData command) throws FlowException {
        return getTransactionBuilder(
                ImmutableList.of(getOurIdentity().getOwningKey()),
                command);
    }

    protected SignedTransaction synchronizeCounterpartiesAndFinalize(Party me, Set<Party> counterparties, TransactionBuilder transactionBuilder) throws FlowException {
        progressTracker.setCurrentStep(SIGNING);
        transactionBuilder.verify(getServiceHub());

        // We sign the transaction with our private key, making it immutable.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        // Send any keys and certificates so the signers can verify each other's identity
        progressTracker.setCurrentStep(SYNCING);
        Set<FlowSession> counterpartySessions = counterparties.stream().map(
                party -> initiateFlow(party)).collect(Collectors.toSet());
        // why IdentySyncFlow must be executed
        // subFlow(new IdentitySyncFlow.Send(otherPartySessions, signedTx.getTx(), SYNCING.childProgressTracker()));

        // Obtaining the counterparty's signature.
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, counterpartySessions, CollectSignaturesFlow.tracker()));

        // We get the transaction notarised and recorded automatically by the platform.
        // send a copy to current issuer
        progressTracker.setCurrentStep(FINALISING);
        return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(me)));
    }


    protected Party getFirstNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) {
            throw new FlowException("No available notary.");
        }
        return notaries.get(0);
    }

    protected StateAndRef<T> getStateByLinearId(Class stateClass, UniqueIdentifier linearId) throws FlowException {
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED,
                null);

        List<StateAndRef<T>> data = getServiceHub().getVaultService().queryBy(
                stateClass, queryCriteria).getStates();
        if (data.size() != 1) {
            throw new FlowException(String.format("State of class '%s' with id %s not found.", stateClass.getName(), linearId));
        }
        return data.get(0);
    }
    protected T getStateByRef(StateAndRef<T> ref){
        return ref.getState().getData();
    }

    protected final ProgressTracker.Step PREPARATION = new ProgressTracker.Step("Obtaining data from vault.");
    protected final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Building and verifying transaction.");
    protected final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    protected final ProgressTracker.Step SYNCING = new ProgressTracker.Step("Syncing identities.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return IdentitySyncFlow.Send.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counterparty signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    protected final ProgressTracker progressTracker = new ProgressTracker(
            PREPARATION, BUILDING, SIGNING, SYNCING, COLLECTING, FINALISING
    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public static class SignTxFlowNoChecking extends SignTransactionFlow {
        public SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
            super(otherFlow, progressTracker);
        }

        @Override
        public void checkTransaction(SignedTransaction tx) {
            // no checking
        }
    }


}
