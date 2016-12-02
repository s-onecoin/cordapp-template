package com.example.contract;

import com.example.model.PurchaseOrder;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionType;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The state object which we will use the record the agreement of a valid purchase order issued by a buyer to a seller.
 *
 * There are a few key state interfaces. The most fundamental of which is [ContractState]. We have defined other
 * interfaces for different requirements. In this case we are implementing a [DealState] which defines a few helper
 * properties and methods for managing states pertaining to deals.
 */
public class PurchaseOrderState implements DealState {
    private PurchaseOrder po;
    private Party buyer;
    private Party seller;
    private PurchaseOrderContract contract;
    private UniqueIdentifier linearId;
    /** Another ref field, for matching with data in external systems. In this case the external Id is the po number. */
    private String ref;
    /** List of parties involved in this particular deal. */
    private ArrayList<Party> parties;
    /** The public keys of party that is able to consume this state in a valid transaction. */
    private List<CompositeKey> participants;

    public PurchaseOrderState(PurchaseOrder po,
                              Party buyer,
                              Party seller,
                              PurchaseOrderContract contract) {
        this.po = po;
        this.buyer = buyer;
        this.seller = seller;
        this.contract = contract;
        UniqueIdentifier id = new UniqueIdentifier(Integer.toString(po.getOrderNumber()), UUID.randomUUID());
        this.linearId = id;
        this.ref = id.getExternalId();
        ArrayList<Party> ps = new ArrayList<Party>() {{ add(buyer); add(seller); }};
        this.parties = ps;
        this.participants = ps.stream().map(party -> party.getOwningKey()).collect(Collectors.toList());
    }

    public PurchaseOrder getPo() { return po; }
    public Party getBuyer() { return buyer; }
    public Party getSeller() { return seller; }
    @Override
    public PurchaseOrderContract getContract() { return contract; }
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }
    @Override
    public String getRef() { return ref; }
    @Override
    public ArrayList<Party> getParties() { return parties; }
    @Override
    public List<CompositeKey> getParticipants() { return participants; }
    @Override
    public Integer getEncumbrance() { return null; }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override
    public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        List<PublicKey> partyKeys = parties.stream().flatMap(party -> party.getOwningKey().getKeys().stream()).collect(Collectors.toList());

        return !ourKeys.stream().filter(partyKeys::contains).collect(Collectors.toList()).isEmpty();
    }

    /**
     * Helper function to generate a new Issue() purchase order transaction. For more details on building transactions
     * see the API for [TransactionBuilder] in the JavaDocs.
     * <p>
     * https://docs.corda.net/api/net.corda.core.transactions/-transaction-builder/index.html
     */
    @Override
    public TransactionBuilder generateAgreement(Party notary) {
        return new TransactionType.General.Builder(notary)
                .withItems(this, new Command(new PurchaseOrderContract.Commands.Place(), participants));
    }
}