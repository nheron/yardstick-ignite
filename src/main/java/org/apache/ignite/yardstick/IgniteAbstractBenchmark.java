/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.yardstick;

import org.apache.ignite.*;
import org.apache.ignite.events.*;
import org.apache.ignite.lang.*;
import org.yardstickframework.*;

import java.util.concurrent.*;

import static org.apache.ignite.internal.processors.cache.CacheDistributionMode.*;
import static org.apache.ignite.events.EventType.*;
import static org.yardstickframework.BenchmarkUtils.*;

/**
 * Abstract class for Ignite benchmarks.
 */
public abstract class IgniteAbstractBenchmark extends BenchmarkDriverAdapter {
    /** Arguments. */
    protected final IgniteBenchmarkArguments args = new IgniteBenchmarkArguments();

    /** Node. */
    private IgniteDriverNode node;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        jcommander(cfg.commandLineArguments(), args, "<ignite-driver>");

        if (Ignition.state() != IgniteState.STARTED) {
            node = new IgniteDriverNode(args.distributionMode() == CLIENT_ONLY);

            node.start(cfg);
        }
        else
            // Support for mixed benchmarks mode.
            node = new IgniteDriverNode(args.distributionMode() == CLIENT_ONLY, Ignition.ignite());

        waitForNodes();
    }

    /** {@inheritDoc} */
    @Override public void tearDown() throws Exception {
        if (node != null)
            node.stop();
    }

    /** {@inheritDoc} */
    @Override public String description() {
        String desc = BenchmarkUtils.description(cfg, this);

        return desc.isEmpty() ?
            getClass().getSimpleName() + args.description() + cfg.defaultDescription() : desc;
    }

    /** {@inheritDoc} */
    @Override public String usage() {
        return BenchmarkUtils.usage(args);
    }

    /**
     * @return Grid.
     */
    protected Ignite ignite() {
        return node.ignite();
    }

    /**
     * @throws Exception If failed.
     */
    private void waitForNodes() throws Exception {
        final CountDownLatch nodesStartedLatch = new CountDownLatch(1);

        ignite().events().localListen(new IgnitePredicate<Event>() {
            @Override public boolean apply(Event gridEvt) {
                if (nodesStarted())
                    nodesStartedLatch.countDown();

                return true;
            }
        }, EVT_NODE_JOINED);

        if (!nodesStarted()) {
            println(cfg, "Waiting for " + (args.nodes() - 1) + " nodes to start...");

            nodesStartedLatch.await();
        }
    }

    /**
     * @return {@code True} if all nodes are started, {@code false} otherwise.
     */
    private boolean nodesStarted() {
        return ignite().cluster().nodes().size() >= args.nodes();
    }

    /**
     * @param max Key range.
     * @return Next key.
     */
    protected int nextRandom(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * @param min Minimum key in range.
     * @param max Maximum key in range.
     * @return Next key.
     */
    protected int nextRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(max - min) + min;
    }
}
