package ch.insurance.cordapp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseFlow<T extends ContractState> extends FlowLogic<SignedTransaction> {
    public BaseFlow() {
        super();
    }


    protected TransactionBuilder getTransactionBuilderSignedBySigners(ImmutableList<PublicKey> requiredSigner, CommandData command) throws FlowException {
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(getFirstNotary());
        transactionBuilder.addCommand(command, requiredSigner);
        return transactionBuilder;
    }

    protected TransactionBuilder getTransactionBuilderSignedByParticipants(ContractState state, CommandData command) throws FlowException {
        List<PublicKey> publicKeys = state.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
        ImmutableList<PublicKey> requiredSigner = new ImmutableList.Builder<PublicKey>()
                .addAll(publicKeys)
                .build();
        return getTransactionBuilderSignedBySigners(requiredSigner, command);
    }

    protected TransactionBuilder getMyTransactionBuilderSignedByMe(CommandData command) throws FlowException {
        return getTransactionBuilderSignedBySigners(
                ImmutableList.of(getOurIdentity().getOwningKey()),
                command);
    }

    @Suspendable
    protected SignedTransaction signAndFinalize(TransactionBuilder transactionBuilder) throws FlowException {
        progressTracker_nosync.setCurrentStep(VERIFYING);
        transactionBuilder.verify(getServiceHub());

        // We sign the transaction with our private key, making it immutable.
        progressTracker_nosync.setCurrentStep(SIGNING);
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        // We get the transaction notarised and recorded automatically by the platform.
        progressTracker_nosync.setCurrentStep(FINALISING);
        return subFlow(new FinalityFlow(signedTransaction, FINALISING.childProgressTracker()));
    }

    @Suspendable
    protected SignedTransaction signCollectAndFinalize(Party me, Party counterparty, TransactionBuilder transactionBuilder) throws FlowException {
        progressTracker_nosync.setCurrentStep(VERIFYING);
        transactionBuilder.verify(getServiceHub());

        progressTracker_nosync.setCurrentStep(SIGNING);
        // We sign the transaction with our private key, making it immutable.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        // Send any keys and certificates so the signers can verify each other's identity
        progressTracker_nosync.setCurrentStep(COLLECTING);
        // Send the state to the counterparty, and receive it back with their signature.
        FlowSession otherPartySession = initiateFlow(counterparty);
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(
                        signedTx,
                        ImmutableSet.of(otherPartySession),
                        ImmutableSet.of(getOurIdentity().getOwningKey()),
                        COLLECTING.childProgressTracker()));
        // We get the transaction notarised and recorded automatically by the platform.
        // send a copy to current issuer
        progressTracker_nosync.setCurrentStep(FINALISING);
        return subFlow(new FinalityFlow(fullySignedTx, FINALISING.childProgressTracker()));
    }

    protected Party getFirstNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) {
            throw new FlowException("No available notary.");
        }
        return notaries.get(0);
    }

    protected StateAndRef<T> getLastStateByLinearId(Class stateClass, UniqueIdentifier linearId) throws FlowException {
        StateAndRef stateRef = new FlowHelper<T>(getServiceHub()).getLastStateByLinearId(stateClass, linearId);
        if (stateRef == null) {
            throw new FlowException(String.format("State of class '%s' with id %s not found.", stateClass.getName(), linearId));
        }
        return stateRef;
    }

    protected T getStateByRef(StateAndRef<T> ref){
        return ref.getState().getData();
    }

    protected final ProgressTracker.Step PREPARATION = new ProgressTracker.Step("Obtaining data from vault.");
    protected final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Building transaction.");
    protected final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verifying transaction.");
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

    protected final ProgressTracker progressTracker_full = new ProgressTracker(
            PREPARATION,    // none
            BUILDING,       // none
            VERIFYING,      // none
            SIGNING,        // none
            SYNCING,        // + Identity Sync Flow: Unit / Void
            COLLECTING,     // + Collect Signatures Flow: SignedTransaction
            FINALISING      // + Finality Flow: SignedTransaction
    );
    protected final ProgressTracker progressTracker_nosync = new ProgressTracker(
            PREPARATION,    // none
            BUILDING,       // none
            VERIFYING,      // none
            SIGNING,        // none
            COLLECTING,     // + Collect Signatures Flow: SignedTransaction
            FINALISING      // + Finality Flow: SignedTransaction
    );
    protected final ProgressTracker progressTracker_nosync_nocollect = new ProgressTracker(
            PREPARATION,    // none
            BUILDING,       // none
            VERIFYING,      // none
            SIGNING,        // none
            FINALISING      // + Finality Flow: SignedTransaction
    );

    @Override
    public abstract ProgressTracker getProgressTracker();

    public static class SignTxFlowNoChecking extends SignTransactionFlow {
        public SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
            super(otherFlow, progressTracker);
        }

        @Suspendable
        @Override
        public void checkTransaction(SignedTransaction tx) {
            // no checking
        }
    }


}
