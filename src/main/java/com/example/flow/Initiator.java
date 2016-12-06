package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.CryptoUtilitiesKt;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.NotaryFlow;

import java.security.KeyPair;
import java.util.Collections;

/**
 * This is the initiator's side of the "Hello World" of flows!
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
public class Initiator extends FlowLogic<ExampleFlowResult> {

    private DealState po;
    private Party otherParty;
    private ProgressTracker progressTracker;

    private static ProgressTracker.Step CONSTRUCTING_OFFER = new ProgressTracker.Step("Constructing proposed purchase order.");
    private static ProgressTracker.Step SENDING_OFFER = new ProgressTracker.Step("Sending purchase order to seller for review.");
    private static ProgressTracker.Step RECEIVED_PARTIAL_TRANSACTION = new ProgressTracker.Step("Received partially signed transaction from seller.");
    private static ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verifying signatures and contract constraints.");
    private static ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction with our private key.");
    private static ProgressTracker.Step NOTARY = new ProgressTracker.Step("Obtaining notary signature.");
    private static ProgressTracker.Step RECORDING = new ProgressTracker.Step("Recording transaction in vault.");
    private static ProgressTracker.Step SENDING_FINAL_TRANSACTION = new ProgressTracker.Step("Sending fully signed transaction to seller.");

    public Initiator(DealState po, Party otherParty, ProgressTracker tracker) {
        this.po = po;
        this.otherParty = otherParty;
        this.progressTracker = tracker;
    }

    @Override public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
     * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
     */
    public static ProgressTracker tracker() {
        return new ProgressTracker(
                CONSTRUCTING_OFFER,
                SENDING_OFFER,
                RECEIVED_PARTIAL_TRANSACTION,
                VERIFYING,
                SIGNING,
                NOTARY,
                RECORDING,
                SENDING_FINAL_TRANSACTION
        );
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override public ExampleFlowResult call() {
        // Naively, wrapped the whole flow in a try ... catch block so we can
        // push the exceptions back through the web API.
        try {
            // Prep.
            // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered with
            // the NetWorkMapService. In a future milestone release we'll implement HD key generation such that new keys
            // can be generated for each transaction.
            KeyPair myKeyPair = getServiceHub().getLegalIdentityKey();
            // Obtain a reference to the notary we want to use and its public key.
            // TODO: get(0) is lazy replacement for select()
            Party notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0).getNotaryIdentity();
            CompositeKey notaryPubKey = notary.getOwningKey();
            // Stage 1.
            progressTracker.setCurrentStep(CONSTRUCTING_OFFER);
            // Construct a state object which encapsulates the PurchaseOrder object.
            // We add the public keys for us and the counterparty as well as a reference to the contract code.
            TransactionState offerMessage = new TransactionState(po, notary);
            // Stage 2.
            progressTracker.setCurrentStep(SENDING_OFFER);
            // Send the state across the wire to the designated counterparty.
            // -----------------------
            // Flow jumps to Acceptor.
            // -----------------------
            progressTracker.setCurrentStep(RECEIVED_PARTIAL_TRANSACTION);
            SignedTransaction ptx = this.sendAndReceive(otherParty, offerMessage, SignedTransaction.class).unwrap(data -> data);

            // Stage 7.
            // Receive the partially signed transaction off the wire from the other party.
            // Check that the signature of the other party is valid.
            // Our signature and the Notary's signature are allowed to be omitted at this stage as this is only a
            // partially signed transaction.
            progressTracker.setCurrentStep(VERIFYING);
            WireTransaction wtx = ptx.verifySignatures(CryptoUtilitiesKt.getComposite(myKeyPair.getPublic()), notaryPubKey);
            // Run the contract's verify function.
            // We want to be sure that the PurchaseOrderState agreed upon is a valid instance of an PurchaseOrderContract, to do
            // this we need to run the contract's verify() function.
            wtx.toLedgerTransaction(getServiceHub()).verify();

            // Stage 8.
            progressTracker.setCurrentStep(SIGNING);
            // Sign the transaction with our key pair and add it to the transaction.
            // We now have 'validation consensus'. We still require uniqueness consensus.
            // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
            DigitalSignature.WithKey mySig = CryptoUtilitiesKt.signWithECDSA(myKeyPair, ptx.getId().getBytes());
            // '+' in this case is just an overloaded operator defined in 'signedTransaction.kt'.
            SignedTransaction vtx = ptx.plus(mySig);
            // Stage 9.
            progressTracker.setCurrentStep(NOTARY);
            // Obtain the notary's signature.
            // We do this by firing-off a sub-flow. This illustrates the power of protocols as reusable workflows.
            DigitalSignature.WithKey notarySignature = subFlow(new NotaryFlow.Client(vtx, NotaryFlow.Client.Companion.tracker()), false);
            // Add the notary signature to the transaction.
            SignedTransaction ntx = vtx.plus(notarySignature);
            // Stage 10.
            progressTracker.setCurrentStep(RECORDING);
            // Record the transaction in our vault.
            getServiceHub().recordTransactions(Collections.singletonList(ntx));
            // Stage 11.
            progressTracker.setCurrentStep(SENDING_FINAL_TRANSACTION);
            // Send a copy of the transaction to our counter-party.
            send(otherParty, ntx);
            return new ExampleFlowResult.Success(String.format("Transaction id %s committed to ledger.", ntx.getId().toString()));
        } catch(Exception ex) {
            // Just catch all exception types.
            return new ExampleFlowResult.Failure(ex.getMessage());
        }
    }
}