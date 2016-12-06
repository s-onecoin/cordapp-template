package com.example;

import kotlin.Unit;
import net.corda.core.node.services.ServiceInfo;
import net.corda.node.driver.DriverKt;
import net.corda.node.driver.PortAllocation;
import net.corda.node.services.User;
import net.corda.node.services.transactions.ValidatingNotaryService;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;

import static java.time.ZoneOffset.UTC;

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Firstly, run the "Run Example CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports which should be output to the console for each node. They typically start at 5006, 5007,
 *    5008. The "Debug CorDapp" configuration runs with port 5007, which should be "NodeB". In any case, double check
 *    the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
public class Main {
    public static void main(String[] args) {
        // No permissions required as we are not invoking flows.
        User user = new User("user1", "test", Collections.emptySet());
        DriverKt.driver(
                // TODO: Can I use getTimestampAsDirectoryName() from Driver.kt?
                Paths.get("build", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC).format(Instant.now())),
                new PortAllocation.Incremental(10000),
                new PortAllocation.Incremental(5005),
                false,
                true,
                dsl -> {dsl.startNode("Controller",
                        Collections.singleton(new ServiceInfo(ValidatingNotaryService.Companion.getType(), null)),
                        Collections.emptyList(),
                        Collections.emptyMap());
                    dsl.startNode("NodeA", new HashSet<>(), Collections.singletonList(user), Collections.emptyMap());
                    dsl.startNode("NodeB", new HashSet<>(), Collections.singletonList(user), Collections.emptyMap());
                    dsl.startNode("NodeC", new HashSet<>(), Collections.singletonList(user), Collections.emptyMap());
                    dsl.waitForAllNodesToFinish();
                    return Unit.INSTANCE; }
        );
    }
}
