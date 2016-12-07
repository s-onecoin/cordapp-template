package com.example.client;

import com.example.contract.PurchaseOrderState;
import com.google.common.net.HostAndPort;
import kotlin.Pair;
import kotlin.Unit;
import net.corda.client.CordaRPCClient;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.node.services.config.ConfigUtilitiesKt;
import net.corda.node.services.messaging.CordaRPCOps;
import org.apache.activemq.artemis.api.core.ActiveMQNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/
public class ExampleClientRPC {
    static Logger logger = LoggerFactory.getLogger(ExampleClientRPC.class);

    public static void main(String[] args) throws ActiveMQNotConnectedException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleClientRPC <node address>");
        }

        HostAndPort nodeAddress = HostAndPort.fromString(args[0]);
        CordaRPCClient client = new CordaRPCClient(nodeAddress, ConfigUtilitiesKt.configureTestSSL());

        // Can be amended in the com.example.Main file.
        client.start("user1", "test");
        CordaRPCOps proxy = client.proxy(null, 0);

        // Grab all signed transactions and all future signed transactions.
        Pair<List<SignedTransaction>, Observable<SignedTransaction>> txsAndFutureTxs =
            proxy.verifiedTransactions();
        List<SignedTransaction> txs = txsAndFutureTxs.getFirst();
        Observable<SignedTransaction> futureTxs = txsAndFutureTxs.getSecond();

        // Log the 'placed' purchase order states and listen for new ones.
        futureTxs.startWith(txs).subscribe(
                (SignedTransaction transaction) ->
                        transaction.getTx().getOutputs().forEach(
                            (TransactionState<ContractState> output) -> {
                                PurchaseOrderState poState = (PurchaseOrderState) output.getData();
                                logger.info(poState.getPo().toString());
                            })
        );

        new CompletableFuture<Unit>().get();
    }
}