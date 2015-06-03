package com.spotify.heroic.metric.astyanax;

import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Singleton;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.netflix.astyanax.AstyanaxConfiguration;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import com.spotify.heroic.concurrrency.ReadWriteThreadPools;
import com.spotify.heroic.metric.MetricBackend;
import com.spotify.heroic.metric.MetricModule;
import com.spotify.heroic.statistics.LocalMetricManagerReporter;
import com.spotify.heroic.statistics.MetricBackendReporter;
import com.spotify.heroic.utils.GroupedUtils;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Managed;
import eu.toolchain.async.ManagedSetup;

@Data
public final class AstyanaxMetricModule implements MetricModule {
    public static final Set<String> DEFAULT_SEEDS = ImmutableSet.of("localhost");
    public static final String DEFAULT_KEYSPACE = "heroic";
    public static final String DEFAULT_GROUP = "heroic";
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 50;

    private final String id;
    private final Set<String> groups;
    private final String keyspace;
    private final Set<String> seeds;
    private final int maxConnectionsPerHost;
    private final ReadWriteThreadPools.Config pools;

    @JsonCreator
    public AstyanaxMetricModule(@JsonProperty("id") String id, @JsonProperty("seeds") Set<String> seeds,
            @JsonProperty("keyspace") String keyspace,
            @JsonProperty("maxConnectionsPerHost") Integer maxConnectionsPerHost, @JsonProperty("group") String group,
            @JsonProperty("groups") Set<String> groups, @JsonProperty("pools") ReadWriteThreadPools.Config pools) {
        this.id = id;
        this.groups = GroupedUtils.groups(group, groups, DEFAULT_GROUP);
        this.keyspace = Optional.fromNullable(keyspace).or(DEFAULT_KEYSPACE);
        this.seeds = Optional.fromNullable(seeds).or(DEFAULT_SEEDS);
        this.maxConnectionsPerHost = Optional.fromNullable(maxConnectionsPerHost).or(DEFAULT_MAX_CONNECTIONS_PER_HOST);
        this.pools = Optional.fromNullable(pools).or(ReadWriteThreadPools.Config.provideDefault());
    }

    @Override
    public PrivateModule module(final Key<MetricBackend> key, final String id) {
        return new PrivateModule() {
            @Provides
            @Singleton
            public MetricBackendReporter reporter(LocalMetricManagerReporter reporter) {
                return reporter.newBackend(id);
            }

            @Provides
            @Singleton
            public ReadWriteThreadPools pools(AsyncFramework async, MetricBackendReporter reporter) {
                return pools.construct(async, reporter.newThreadPool());
            }

            @Provides
            @Singleton
            public Managed<Context> context(final AsyncFramework async) {
                return async.managed(new ManagedSetup<Context>() {
                    @Override
                    public AsyncFuture<Context> construct() {
                        return async.call(new Callable<Context>() {
                            public Context call() throws Exception {
                                final AstyanaxConfiguration config = new AstyanaxConfigurationImpl().setCqlVersion(
                                        "3.0.0").setTargetCassandraVersion("2.0");

                                final String seeds = buildSeeds();

                                final AstyanaxContext<Keyspace> ctx = new AstyanaxContext.Builder()
                                        .withConnectionPoolConfiguration(
                                                new ConnectionPoolConfigurationImpl("HeroicConnectionPool")
                                                        .setPort(9160).setMaxConnsPerHost(maxConnectionsPerHost)
                                                        .setSeeds(seeds)).forKeyspace(keyspace)
                                        .withAstyanaxConfiguration(config)
                                        .buildKeyspace(ThriftFamilyFactory.getInstance());

                                ctx.start();
                                final Keyspace keyspace = ctx.getClient();
                                return new Context(ctx, keyspace);
                            };
                        });
                    }

                    private String buildSeeds() {
                        return StringUtils.join(seeds, ",");
                    }

                    @Override
                    public AsyncFuture<Void> destruct(final Context ctx) {
                        return async.call(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                ctx.context.shutdown();
                                return null;
                            }
                        });
                    }
                });
            }

            @Override
            protected void configure() {
                bind(key).toInstance(new AstyanaxBackend(groups));
                expose(key);
            }
        };
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String buildId(int i) {
        return String.format("heroic#%d", i);
    }
}
