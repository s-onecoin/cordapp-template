package com.example.model;

import java.util.Date;
import java.util.List;

/**
 * These files contain the data structures which the parties using this CorDapp will reach an agreement over. States can
 * support arbitrary complex object graphs. For a more complicated one, see
 *
 * samples/irs-demo/src/kotlin/net/corda/irs/contract/IRS.kt
 *
 * in the main Corda repo (http://github.com/corda/corda).
 *
 * These structures could be embedded within the ContractState. However, for clarity, we have moved them in to a
 * separate file.
 */

/**
 * A simple class representing a purchase order.
 */
public class PurchaseOrder {
    private int orderNumber;
    private Date deliveryDate;
    private Address deliveryAddress;
    private List<Item> items;

    public PurchaseOrder() {}

    public PurchaseOrder(int orderNumber, Date deliveryDate, Address deliveryAddress, List<Item> items) {
        this.orderNumber = orderNumber;
        this.deliveryDate = deliveryDate;
        this.deliveryAddress = deliveryAddress;
        this.items = items;
    }

    public int getOrderNumber() { return orderNumber; }
    public Date getDeliveryDate() { return deliveryDate; }
    public Address getDeliveryAddress() { return deliveryAddress; }
    public List<Item> getItems() { return items; }
}