package ch.insurance.cordapp.broker.flows;

import ch.insurance.cordapp.BaseFlow;
import ch.insurance.cordapp.broker.MandateContract;
import ch.insurance.cordapp.broker.MandateState;
import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Set;


public class MandateAcceptFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends BaseFlow<MandateState> {

        private final UniqueIdentifier id;
        private final Instant startAt;
        private final long amountDuration;
        private final TemporalUnit unit;

        public Initiator(UniqueIdentifier id, Instant startAt, long amountDuration, TemporalUnit unit) {
            this.id = id;
            this.startAt = startAt;
            this.amountDuration = amountDuration;
            this.unit = unit;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - search for our object by <id>
             * ===========================================================================*/
            StateAndRef<MandateState> mandateToTransfer =  this.getStateByLinearId(MandateState.class, this.id);
            MandateState mandate = this.getStateByRef(mandateToTransfer);

            if (!me.equals(mandate.getBroker())) {
                throw new FlowException("Mandate can only be accepted by broker.");
            }

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            progressTracker.setCurrentStep(BUILDING);
            MandateState acceptedMandate = mandate.accept(this.startAt, this.amountDuration, this.unit);

            // add new owner to signers
            // add all old participants
            ImmutableList<PublicKey> requiredSigner = new ImmutableList.Builder<PublicKey>()
                    .addAll(mandate.getParticipantKeys())
                    .build();

            TransactionBuilder transactionBuilder = getTransactionBuilder(
                    requiredSigner,
                    new MandateContract.Commands.Accept());
            transactionBuilder.addInputState(mandateToTransfer);
            transactionBuilder.addOutputState(acceptedMandate, MandateContract.ID);


            /* ============================================================================
             *          TODO 2 - Write our TokenContract to control token issuance!
             * ===========================================================================*/
            // Stage 6. Verify and sign the transaction.
            // We check our transaction is valid based on its contracts.
            progressTracker.setCurrentStep(SIGNING);
            transactionBuilder.verify(getServiceHub());

            // We sign the transaction with our private key, making it immutable.
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(transactionBuilder);

            // Send any keys and certificates so the signers can verify each other's identity
            progressTracker.setCurrentStep(SYNCING);
            FlowSession otherPartySession = initiateFlow(acceptedMandate.getClient());
            Set<FlowSession> otherPartySessions = ImmutableSet.of(otherPartySession);
            // why IdentySyncFlow must be executed
            // subFlow(new IdentitySyncFlow.Send(otherPartySessions, signedTx.getTx(), SYNCING.childProgressTracker()));

            // Obtaining the counterparty's signature.
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    signedTx, otherPartySessions, CollectSignaturesFlow.tracker()));

            // We get the transaction notarised and recorded automatically by the platform.
            // send a copy to current issuer
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(me)));
        }

    }


    @InitiatedBy(Initiator.class)
    public static class TokenTransferResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public TokenTransferResponder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //subFlow(new IdentitySyncFlow.Receive(otherFlow));
            SignedTransaction stx = subFlow(new BaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
    }
}