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
 * A simple class representing a purchase order address.
 */
public class Address {
    private String city;
    private String country;

    public Address() {}

    public Address(String city, String country) {
        this.city = city;
        this.country = country;
    }

    public String getCity() { return city; }
    public String getCountry() { return country; }
}