package com.example.model;

/**
 * The name, amount and price of an item to be purchased. It is assumed that the buyer has the seller's item catalogue
 * and will only place orders for valid items. Of course, a reference to a particular version of the catalogue could be
 * included with the Issue() purchase order transaction as an attachment, such that the seller can check the items are valid.
 * For more details on attachments see
 *
 * samples/attachment-demo/src/kotlin/net/corda/attachmentdemo
 *
 * in the main Corda repo (http://github.com/corda/corda).
 *
 * In the contract verify code, we have written some constraints about items.
 */
public class Item {
    private String name;
    private int amount;

    public Item(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() { return name; }
    public int getAmount() { return amount; }
}