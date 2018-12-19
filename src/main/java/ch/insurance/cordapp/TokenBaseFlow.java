package ch.insurance.cordapp;

import com.google.common.collect.ImmutableList;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.CommandData;
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

public abstract class TokenBaseFlow extends FlowLogic<SignedTransaction> {
    public TokenBaseFlow() {
        super();
    }


    @NotNull
    protected TransactionBuilder getTransactionBuilder(Party issuer, ImmutableList<PublicKey> requiredSigner, CommandData command) {
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(OneNotary());
        transactionBuilder.addCommand(command, requiredSigner);
        return transactionBuilder;
    }
    protected TransactionBuilder getTransactionBuilder(ImmutableList<PublicKey> requiredSigner, CommandData command) {
        return getTransactionBuilder(getOurIdentity(), requiredSigner, command);
    }
    protected TransactionBuilder getMyTransactionBuilder(CommandData command) {
        return getTransactionBuilder(
                getOurIdentity(),
                ImmutableList.of(getOurIdentity().getOwningKey()),
                command);
    }

    protected Party OneNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }



    protected StateAndRef<TokenState> getStateByLinearId(UniqueIdentifier linearId) throws FlowException {
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED,
                null);

        List<StateAndRef<TokenState>> tokens = getServiceHub().getVaultService().queryBy(
                TokenState.class, queryCriteria).getStates();
        if (tokens.size() != 1) {
            throw new FlowException(String.format("TokenState with id %s not found.", linearId));
        }
        return tokens.get(0);
    }
    protected TokenState getStateByRef(StateAndRef<TokenState> ref){
        return ref.getState().getData();
    }

    protected final ProgressTracker.Step PREPARATION = new ProgressTracker.Step("Obtaining Obligation from vault.");
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

    protected static class SignTxFlowNoChecking extends SignTransactionFlow {
        SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
            super(otherFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction tx) {
            // no checking
        }
    }


}
