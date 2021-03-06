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

package com.spotify.heroic.consumer;

import com.spotify.heroic.dagger.PrimaryComponent;
import com.spotify.heroic.ingestion.IngestionGroup;
import com.spotify.heroic.lifecycle.LifeCycle;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import lombok.RequiredArgsConstructor;

public interface ConsumerSchema {
    Exposed setup(Depends depends);

    interface Consumer {
        void consume(byte[] message) throws ConsumerSchemaException;
    }

    @ConsumerSchemaScope
    @Component(modules = DependsModule.class,
        dependencies = {PrimaryComponent.class, ConsumerModule.Depends.class})
    interface Depends extends PrimaryComponent, ConsumerModule.Depends {
        IngestionGroup group();
    }

    interface Exposed {
        Consumer consumer();

        default LifeCycle life() {
            return LifeCycle.empty();
        }
    }

    @RequiredArgsConstructor
    @Module
    class DependsModule {
        private final IngestionGroup group;

        @Provides
        @ConsumerSchemaScope
        IngestionGroup group() {
            return group;
        }
    }
}
