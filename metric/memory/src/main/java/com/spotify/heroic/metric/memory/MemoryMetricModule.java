/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.metric.memory;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.spotify.heroic.common.Groups;
import com.spotify.heroic.metric.Metric;
import com.spotify.heroic.metric.MetricBackend;
import com.spotify.heroic.metric.MetricModule;
import com.spotify.heroic.statistics.LocalMetricManagerReporter;
import com.spotify.heroic.statistics.MetricBackendReporter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.inject.Singleton;

import eu.toolchain.async.AsyncFramework;
import lombok.Data;

@Data
public final class MemoryMetricModule implements MetricModule {
    public static final String DEFAULT_GROUP = "memory";

    private final String id;
    private final Groups groups;
    private final boolean synchronizedStorage;

    @JsonCreator
    public MemoryMetricModule(@JsonProperty("id") String id, @JsonProperty("group") String group,
            @JsonProperty("groups") Set<String> groups, Optional<Boolean> synchronizedStorage) {
        this.id = id;
        this.groups = Groups.groups(group, groups, DEFAULT_GROUP);
        this.synchronizedStorage = synchronizedStorage.orElse(false);
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
            public Groups groups() {
                return groups;
            }

            @Provides
            @Singleton
            public MetricBackend metricBackend(final AsyncFramework async) {
                final Map<MemoryBackend.MemoryKey, NavigableMap<Long, Metric>> storage;

                if (synchronizedStorage) {
                    storage = Collections.synchronizedMap(new HashMap<>());
                } else {
                    storage = new ConcurrentSkipListMap<>(MemoryBackend.COMPARATOR);
                }

                return new MemoryBackend(async, groups, storage);
            }

            @Override
            protected void configure() {
                bind(key).to(MetricBackend.class).in(Scopes.SINGLETON);
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
        return String.format("memory#%d", i);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String group;
        private Set<String> groups;
        private Optional<Boolean> synchronizedStorage = empty();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder groups(Set<String> groups) {
            this.groups = groups;
            return this;
        }

        public Builder synchronizedStorage(final boolean synchronizedStorage) {
            this.synchronizedStorage = of(synchronizedStorage);
            return this;
        }

        public MemoryMetricModule build() {
            return new MemoryMetricModule(id, group, groups, synchronizedStorage);
        }
    }
}
