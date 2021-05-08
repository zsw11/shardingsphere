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

package org.apache.shardingsphere.governance.core.registry.listener;

import org.apache.shardingsphere.governance.core.registry.listener.metadata.MetaDataListener;
import org.apache.shardingsphere.governance.repository.api.RegistryRepository;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent.Type;

import java.util.Collection;

/**
 * Registry listener manager.
 */
public final class RegistryListenerManager {
    
    private final TerminalStateChangedListener terminalStateChangedListener;
    
    private final DataSourceStateChangedListener dataSourceStateChangedListener;
    
    private final LockChangedListener lockChangedListener;

    private final MetaDataListener metaDataListener;

    private final PropertiesChangedListener propertiesChangedListener;

    private final UserChangedListener authenticationChangedListener;
    
    private final PrivilegeNodeChangedListener privilegeNodeChangedListener;
    
    public RegistryListenerManager(final RegistryRepository registryRepository, final Collection<String> schemaNames) {
        terminalStateChangedListener = new TerminalStateChangedListener(registryRepository);
        dataSourceStateChangedListener = new DataSourceStateChangedListener(registryRepository, schemaNames);
        lockChangedListener = new LockChangedListener(registryRepository);
        metaDataListener = new MetaDataListener(registryRepository, schemaNames);
        propertiesChangedListener = new PropertiesChangedListener(registryRepository);
        authenticationChangedListener = new UserChangedListener(registryRepository);
        privilegeNodeChangedListener = new PrivilegeNodeChangedListener(registryRepository);
    }
    
    /**
     * Initialize all state changed listeners.
     */
    public void initListeners() {
        terminalStateChangedListener.watch(Type.UPDATED);
        dataSourceStateChangedListener.watch(Type.UPDATED, Type.DELETED, Type.ADDED);
        lockChangedListener.watch(Type.ADDED, Type.DELETED);
        metaDataListener.watch();
        propertiesChangedListener.watch(Type.UPDATED);
        authenticationChangedListener.watch(Type.UPDATED);
        privilegeNodeChangedListener.watch(Type.UPDATED);
    }
}
