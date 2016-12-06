package com.example.api;

import com.example.contract.PurchaseOrderContract;
import com.example.contract.PurchaseOrderState;
import com.example.flow.ExampleFlowResult;
import com.example.flow.Initiator;
import com.example.model.PurchaseOrder;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.Party;
import net.corda.core.node.ServiceHub;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
    private ServiceHub services;
    private String me;

    public ExampleApi(ServiceHub services) {
        this.services = services;
        this.me = services.getMyInfo().getLegalIdentity().getName();
    }

    /** Returns the party name of the node providing this end-point. */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() {
        Map<String, String> meMap = new HashMap<>();
        meMap.put("me", me);
        return meMap;
    }

    /** Returns all parties registered with the [NetworkMapService]. The names can be used to look up identities by
     * using the [IdentityService]. */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        Map<String, List<String>> peers = new HashMap<>();
        peers.put("peers", services.getNetworkMapCache().getPartyNodes()
                .stream()
                .map(node -> node.getLegalIdentity().getName())
                .filter(name -> !name.equals(me) && !name.equals("Controller"))
                .collect(Collectors.toList()));
        return peers;
    }

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<UniqueIdentifier, StateAndRef<PurchaseOrderState>> getPurchaseOrders() {
        return services.getVaultService().linearHeadsOfType_(PurchaseOrderState.class);
    }

    /**
     * This should only be called from the 'buyer' node. It initiates a flow to agree a purchase order with a
     * seller. Once the flow finishes it will have written the purchase order to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
     * <p>
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     * <p>
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("{party}/create-purchase-order")
    public Response createPurchaseOrder(PurchaseOrder po, @PathParam("party") String partyName) throws InterruptedException, ExecutionException {
        Party otherParty = services.getIdentityService().partyFromName(partyName);
        if (otherParty != null) {
            PurchaseOrderState state = new PurchaseOrderState(po, services.getMyInfo().getLegalIdentity(), otherParty, new PurchaseOrderContract());
            // The line below blocks and waits for the future to resolve.
            ExampleFlowResult result = services.invokeFlowAsync(Initiator.class, state, otherParty, Initiator.tracker()).getResultFuture().get();
            if (result instanceof ExampleFlowResult.Success) {
                return Response
                        .status(Response.Status.CREATED)
                        .entity(result.toString())
                        .build();
            } else {
                return Response
                        .status(Response.Status.BAD_REQUEST)
                    .entity(result.toString())
                    .build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}