package com.example.api

import com.example.contract.PurchaseOrderContract
import com.example.contract.PurchaseOrderState
import com.example.model.PurchaseOrder
import com.example.model.Address
import com.example.model.Item
import com.example.flow.ExampleFlow
import com.example.flow.ExampleFlowResult
import net.corda.core.contracts.TransactionState
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.linearHeadsOfType
import java.util.Date
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(val services: ServiceHub) {
    val me: String = services.myInfo.legalIdentity.name

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to me)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapCache.partyNodes
            .map { it.legalIdentity.name }
            .filter { it != me && it != "Controller" })

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseOrders() = services.vaultService.linearHeadsOfType<PurchaseOrderState>()

    /**
     * This should only be called from the 'buyer' node. It initiates a flow to agree a purchase order with a
     * seller. Once the flow finishes it will have written the purchase order to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("{party}/create-purchase-order")
    // TODO: Test version of the endpoint to test the java model/contract/state in a realistic setting
    fun createPurchaseOrder(po: String, @PathParam("party") partyName: String) {
        // TODO: Generate the PurchaseOrder from the .json, instead of using this dummy
        val address = Address("London", "UK")
        val item = Item("thing", 4)
        val date = Date()
        val po = PurchaseOrder(1, date, address, mutableListOf(item))

        val otherParty = services.identityService.partyFromName(partyName)
        val notary = services.networkMapCache.notaryNodes.single().notaryIdentity
        val state = PurchaseOrderState(po, services.myInfo.legalIdentity, otherParty, PurchaseOrderContract())
        val offerMessage = TransactionState(state, notary)
    }
}