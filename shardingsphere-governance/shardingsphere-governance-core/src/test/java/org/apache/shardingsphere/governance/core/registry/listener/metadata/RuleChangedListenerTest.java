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

package org.apache.shardingsphere.governance.core.registry.listener.metadata;

import org.apache.shardingsphere.encrypt.api.config.EncryptRuleConfiguration;
import org.apache.shardingsphere.governance.core.event.model.GovernanceEvent;
import org.apache.shardingsphere.governance.core.event.model.rule.RuleConfigurationsChangedEvent;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent.Type;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.readwrite.splitting.api.ReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public final class RuleChangedListenerTest extends MetaDataListenerTest {
    
    private static final String SHARDING_RULE_FILE = "yaml/sharding-rule.yaml";
    
    private static final String READ_WRITE_SPLITTING_RULE_FILE = "yaml/read-write-splitting-rule.yaml";
    
    private static final String ENCRYPT_RULE_FILE = "yaml/encrypt-rule.yaml";
    
    private RuleChangedListener ruleChangedListener;
    
    @Before
    public void setUp() {
        ruleChangedListener = new RuleChangedListener(getRegistryRepository(), Arrays.asList("sharding_db", "replica_query_db", "encrypt_db"));
    }
    
    @Test
    public void assertCreateEventWithoutSchema() {
        DataChangedEvent dataChangedEvent = new DataChangedEvent("/metadata", readYAML(SHARDING_RULE_FILE), Type.UPDATED);
        Optional<GovernanceEvent> actual = ruleChangedListener.createEvent(dataChangedEvent);
        assertFalse(actual.isPresent());
    }
    
    @Test
    public void assertCreateEventWithEmptyValue() {
        DataChangedEvent dataChangedEvent = new DataChangedEvent("/metadata/sharding_db/rule", "", Type.UPDATED);
        Optional<GovernanceEvent> actual = ruleChangedListener.createEvent(dataChangedEvent);
        assertFalse(actual.isPresent());
    }
    
    @Test
    public void assertCreateEventWithShardingRule() {
        DataChangedEvent dataChangedEvent = new DataChangedEvent("/metadata/sharding_db/rule", readYAML(SHARDING_RULE_FILE), Type.UPDATED);
        Optional<GovernanceEvent> actual = ruleChangedListener.createEvent(dataChangedEvent);
        assertTrue(actual.isPresent());
        assertThat(((RuleConfigurationsChangedEvent) actual.get()).getSchemaName(), is("sharding_db"));
        Collection<RuleConfiguration> ruleConfigs = ((RuleConfigurationsChangedEvent) actual.get()).getRuleConfigurations();
        assertThat(ruleConfigs.size(), is(1));
        assertThat(((ShardingRuleConfiguration) ruleConfigs.iterator().next()).getTables().size(), is(1));
    }
    
    @Test
    public void assertCreateEventWithEncryptRule() {
        DataChangedEvent dataChangedEvent = new DataChangedEvent("/metadata/encrypt_db/rule", readYAML(ENCRYPT_RULE_FILE), Type.UPDATED);
        Optional<GovernanceEvent> actual = ruleChangedListener.createEvent(dataChangedEvent);
        assertTrue(actual.isPresent());
        RuleConfigurationsChangedEvent event = (RuleConfigurationsChangedEvent) actual.get();
        assertThat(event.getSchemaName(), is("encrypt_db"));
        assertThat(event.getRuleConfigurations().iterator().next(), instanceOf(EncryptRuleConfiguration.class));
        EncryptRuleConfiguration encryptRuleConfig = (EncryptRuleConfiguration) event.getRuleConfigurations().iterator().next();
        assertThat(encryptRuleConfig.getEncryptors().size(), is(1));
        ShardingSphereAlgorithmConfiguration encryptAlgorithmConfig = encryptRuleConfig.getEncryptors().get("order_encryptor");
        assertThat(encryptAlgorithmConfig.getType(), is("AES"));
        assertThat(encryptAlgorithmConfig.getProps().get("aes-key-value"), is(123456));
    }
    
    @Test
    public void assertCreateEventWithReadWriteSplittingRule() {
        DataChangedEvent dataChangedEvent = new DataChangedEvent("/metadata/read_write_splitting_db/rule", readYAML(READ_WRITE_SPLITTING_RULE_FILE), Type.UPDATED);
        Optional<GovernanceEvent> actual = ruleChangedListener.createEvent(dataChangedEvent);
        assertTrue(actual.isPresent());
        RuleConfigurationsChangedEvent event = (RuleConfigurationsChangedEvent) actual.get();
        assertThat(event.getSchemaName(), is("read_write_splitting_db"));
        assertThat(event.getRuleConfigurations().iterator().next(), instanceOf(ReadWriteSplittingRuleConfiguration.class));
        ReadWriteSplittingRuleConfiguration ruleConfig = (ReadWriteSplittingRuleConfiguration) event.getRuleConfigurations().iterator().next();
        assertThat(ruleConfig.getDataSources().iterator().next().getWriteDataSourceName(), is("write_ds"));
    }
}
