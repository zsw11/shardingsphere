/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.governance.context.authority.listener.factory;

import org.apache.shardingsphere.governance.context.authority.listener.UserChangedListener;
import org.apache.shardingsphere.governance.core.registry.listener.GovernanceListener;
import org.apache.shardingsphere.governance.core.registry.listener.GovernanceListenerFactory;
import org.apache.shardingsphere.governance.repository.api.RegistryRepository;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent.Type;

import java.util.Collection;
import java.util.Collections;

/**
 * User changed listener factory.
 */
public final class UserChangedListenerFactory implements GovernanceListenerFactory {
    
    @Override
    public GovernanceListener create(final RegistryRepository registryRepository, final Collection<String> schemaNames) {
        return new UserChangedListener(registryRepository);
    }
    
    @Override
    public Collection<Type> getWatchTypes() {
        return Collections.singleton(Type.UPDATED);
    }
}
