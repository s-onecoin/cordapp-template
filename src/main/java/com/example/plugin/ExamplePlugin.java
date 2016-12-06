package com.example.plugin;

import com.esotericsoftware.kryo.Kryo;
import com.example.api.ExampleApi;
import com.example.contract.PurchaseOrderContract;
import com.example.contract.PurchaseOrderState;
import com.example.flow.ExampleFlow;
import com.example.model.PurchaseOrder;
import com.example.service.ExampleService;
import net.corda.core.crypto.Party;
import net.corda.core.node.CordaPluginRegistry;

import java.util.*;

public class ExamplePlugin extends CordaPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private List<Class<?>> webApis = Collections.singletonList(ExampleApi.class);

    /**
     * A list of flows required for this CorDapp.
     * <p>
     * Any flow which is invoked from from the web API needs to be registered as an entry into this Map. The Map
     * takes the form of:
     * <p>
     * Name of the flow to be invoked -> Set of the parameter types passed into the flow.
     * <p>
     * E.g. In the case of this CorDapp:
     * <p>
     * "ExampleFlow.Initiator" -> Set(PurchaseOrderState, Party)
     * <p>
     * This map also acts as a white list. Such that, if a flow is invoked via the API and not registered correctly
     * here, then the flow state machine will _not_ invoke the flow. Instead, an exception will be raised.
     */
    private Map<String, Set<String>> requiredFlows;

    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow. See the [ExampleService.Service] class for an implementation which sets up a
     */
    private List<Class<?>> servicePlugins = Collections.singletonList(ExampleService.class);
    /**
     * A list of directories in the resources directory that will be served by Jetty under /web
     */
    private Map<String, String> staticServeDirs = Collections.singletonMap(
            // This will serve the exampleWeb directory in resources to /web/example
            "example", this.getClass().getClassLoader().getResource("exampleWeb").toExternalForm()
    );

    public ExamplePlugin() {
        Set<String> classNames = new HashSet<>();
        classNames.add(PurchaseOrderState.class.getName());
        classNames.add(Party.class.getName());
        classNames.add(ExampleFlow.Acceptor.tracker().getClass().getName());

        requiredFlows = Collections.singletonMap(ExampleFlow.Initiator.class.getName(), classNames);
    }

    @Override public List<Class<?>> getWebApis() {
        return webApis;
    }

    @Override public Map<String, Set<String>> getRequiredFlows() {
        return requiredFlows;
    }

    @Override public List<Class<?>> getServicePlugins() {
        return servicePlugins;
    }

    @Override public Map<String, String> getStaticServeDirs() {
        return staticServeDirs;
    }

    /**
     * Register required types with Kryo (our serialisation framework)..
     */
    @Override public boolean registerRPCKryoTypes(Kryo kryo) {
        kryo.register(PurchaseOrderState.class);
        kryo.register(PurchaseOrderContract.class);
        kryo.register(PurchaseOrder.class);
        return true;
    }
}