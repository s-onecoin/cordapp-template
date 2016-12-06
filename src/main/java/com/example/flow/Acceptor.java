package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.UtilsKt;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Collections;

/**
 * This is the acceptor's side of the "Hello World" of flows!
 *
 * It is a generic flow which facilitates the workflow required for two parties; an [Initiator] and an [Acceptor],
 * to come to an agreement about some arbitrary data (in this case, a [PurchaseOrder]) encapsulated within a [DealState].
 *
 * As this is just an example there's no way to handle any counter-proposals. The [Acceptor] always accepts the
 * proposed state assuming it satisfies the referenced [Contract]'s issuance constraints.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * NB. All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * The flows below have been heavily commented to aid your understanding. It may also be worth reading the CorDapp
 * tutorial documentation on the Corda docsite (https://docs.corda.net) which includes a sequence diagram which clearly
 * explains each stage of the flow.
 */
public class Acceptor extends FlowLogic<ExampleFlowResult> {

    private Party otherParty;
    private ProgressTracker progressTracker;

    private static ProgressTracker.Step WAITING_FOR_PROPOSAL = new ProgressTracker.Step("Receiving proposed purchase order from buyer.");
    private static ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on proposed purchase order.");
    private static ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing proposed transaction with our private key.");
    private static ProgressTracker.Step SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE = new ProgressTracker.Step("Sending partially signed transaction to buyer and wait for a response.");
    private static ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying signatures and contract constraints.");
    private static ProgressTracker.Step RECORDING = new ProgressTracker.Step("Recording transaction in vault.");

    public Acceptor(Party otherParty, ProgressTracker tracker) {
        this.otherParty = otherParty;
        this.progressTracker = tracker;
    }

    @Override public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public static ProgressTracker tracker() {
        return new ProgressTracker(
                WAITING_FOR_PROPOSAL,
                GENERATING_TRANSACTION,
                SIGNING,
                SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
                VERIFYING_TRANSACTION,
                RECORDING
        );
    }

    @Suspendable
    @Override public ExampleFlowResult call() {
        try {
            // Prep.
            // Obtain a reference to our key pair.
            KeyPair keyPair = getServiceHub().getLegalIdentityKey();
            // Stage 3.
            progressTracker.setCurrentStep(WAITING_FOR_PROPOSAL);
            // All messages come off the wire as UntrustworthyData. You need to 'unwrap' it. This is an appropriate
            // place to perform some validation over what you have just received.
            TransactionState<DealState> message = this.receive(otherParty, TransactionState.class).unwrap(data -> (TransactionState<DealState>) data );
            // Stage 4.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction. See PurchaseOrderState for further details.
            TransactionBuilder utx = message.getData().generateAgreement(message.getNotary());
            // Add a timestamp as the contract code in PurchaseOrderContract mandates that ExampleStates are timestamped.
            Instant currentTime = getServiceHub().getClock().instant();
            // As we are running in a distributed system, we allocate a 30 second time window for the transaction to
            // be timestamped by the Notary service.
            utx.setTime(currentTime, UtilsKt.getSeconds(30));
            // Stage 5.
            progressTracker.setCurrentStep(SIGNING);
            SignedTransaction stx = utx.signWith(keyPair).toSignedTransaction(false);
            // Stage 6.
            // ------------------------
            // Flow jumps to Initiator.
            // ------------------------
            progressTracker.setCurrentStep(SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE);
            // Stage 12.
            // Receive the notarised transaction off the wire.
            SignedTransaction ntx = this.sendAndReceive(otherParty, stx, SignedTransaction.class).unwrap(data -> {
                return data;
            });

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Validate transaction.
            // No need to allow for any omitted signatures as everyone should have signed.
            ntx.verifySignatures();
            // Check it's valid.
            ntx.toLedgerTransaction(getServiceHub()).verify();

            // Record the transaction.
            progressTracker.setCurrentStep(RECORDING);
            getServiceHub().recordTransactions(Collections.singletonList(ntx));
            return new ExampleFlowResult.Success(String.format("Transaction id %s committed to ledger.", ntx.getId().toString()));
        } catch (Exception ex) {
            return new ExampleFlowResult.Failure(ex.getMessage());
        }
    }
}