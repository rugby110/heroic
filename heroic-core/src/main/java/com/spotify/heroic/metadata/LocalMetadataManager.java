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

package com.spotify.heroic.metadata;

import com.spotify.heroic.common.GroupSet;
import eu.toolchain.async.AsyncFramework;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

@MetadataScope
public class LocalMetadataManager implements MetadataManager {
    private final AsyncFramework async;
    private final GroupSet<MetadataBackend> groupSet;

    @Inject
    public LocalMetadataManager(
        final AsyncFramework async, @Named("groupSet") final GroupSet<MetadataBackend> groupSet
    ) {
        this.async = async;
        this.groupSet = groupSet;
    }

    @Override
    public MetadataBackend useOptionalGroup(Optional<String> group) {
        return new MetadataBackendGroup(groupSet.useOptionalGroup(group), async);
    }

    @Override
    public GroupSet<MetadataBackend> groupSet() {
        return groupSet;
    }
}
