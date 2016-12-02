package com.example.model;

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
 * A simple class representing a purchase order items.
 */
public class Item {
    private String name;
    private int amount;

    public Item() {}

    public Item(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() { return name; }
    public int getAmount() { return amount; }
}
