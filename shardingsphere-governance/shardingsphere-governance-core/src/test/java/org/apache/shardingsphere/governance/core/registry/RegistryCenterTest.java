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

package org.apache.shardingsphere.governance.core.registry;

import lombok.SneakyThrows;
import org.apache.shardingsphere.db.discovery.api.config.DatabaseDiscoveryRuleConfiguration;
import org.apache.shardingsphere.encrypt.api.config.EncryptRuleConfiguration;
import org.apache.shardingsphere.governance.core.event.model.datasource.DataSourceAddedEvent;
import org.apache.shardingsphere.governance.core.event.model.datasource.DataSourceAlteredEvent;
import org.apache.shardingsphere.governance.core.event.model.metadata.MetaDataCreatedEvent;
import org.apache.shardingsphere.governance.core.event.model.metadata.MetaDataDroppedEvent;
import org.apache.shardingsphere.governance.core.event.model.rule.RuleConfigurationsAlteredEvent;
import org.apache.shardingsphere.governance.core.event.model.rule.SwitchRuleConfigurationEvent;
import org.apache.shardingsphere.governance.core.lock.node.LockNode;
import org.apache.shardingsphere.governance.core.yaml.config.YamlRuleConfigurationWrap;
import org.apache.shardingsphere.governance.core.yaml.config.schema.YamlSchema;
import org.apache.shardingsphere.governance.core.yaml.swapper.SchemaYamlSwapper;
import org.apache.shardingsphere.governance.repository.api.RegistryRepository;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.datasource.DataSourceConfiguration;
import org.apache.shardingsphere.infra.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.schema.refresher.event.SchemaAlteredEvent;
import org.apache.shardingsphere.infra.metadata.user.Grantee;
import org.apache.shardingsphere.infra.metadata.user.ShardingSphereUser;
import org.apache.shardingsphere.infra.metadata.user.yaml.config.YamlUsersConfigurationConverter;
import org.apache.shardingsphere.infra.yaml.config.YamlRootRuleConfigurations;
import org.apache.shardingsphere.infra.yaml.engine.YamlEngine;
import org.apache.shardingsphere.infra.yaml.swapper.YamlRuleConfigurationSwapperEngine;
import org.apache.shardingsphere.readwrite.splitting.api.ReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.shadow.api.config.ShadowRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class RegistryCenterTest {
    
    private static final String DATA_SOURCE_YAM = "yaml/registryCenter/data-source.yaml";
    
    private static final String SHARDING_RULE_YAML = "yaml/registryCenter/data-sharding-rule.yaml";
    
    private static final String SHARDING_AND_ENCRYPT_RULE_YAML = "yaml/registryCenter/data-sharding-encrypt-rule.yaml";

    private static final String READ_WRITE_SPLITTING_RULE_YAML = "yaml/registryCenter/data-read-write-splitting-rule.yaml";
    
    private static final String DB_DISCOVERY_RULE_YAML = "yaml/registryCenter/data-database-discovery-rule.yaml";
    
    private static final String ENCRYPT_RULE_YAML = "yaml/registryCenter/data-encrypt-rule.yaml";
    
    private static final String SHADOW_RULE_YAML = "yaml/registryCenter/data-shadow-rule.yaml";
    
    private static final String USERS_YAML = "yaml/registryCenter/data-users.yaml";
    
    private static final String PROPS_YAML = ConfigurationPropertyKey.SQL_SHOW.getKey() + ": false\n";
    
    private static final String DATA_SOURCE_YAML_WITH_CONNECTION_INIT_SQL = "yaml/registryCenter/data-source-init-sql.yaml";
    
    private static final String META_DATA_YAML = "yaml/schema.yaml";
    
    @Mock
    private RegistryRepository registryRepository;
    
    @Mock
    private RegistryCacheManager registryCacheManager;
    
    private RegistryCenter registryCenter;
    
    @Before
    public void setUp() throws ReflectiveOperationException {
        registryCenter = new RegistryCenter(registryRepository);
        Field field = registryCenter.getClass().getDeclaredField("repository");
        field.setAccessible(true);
        field.set(registryCenter, registryRepository);
    }
    
    @Test
    public void assertPersistInstanceOnline() {
        registryCenter.persistInstanceOnline();
        verify(registryRepository).persistEphemeral(anyString(), anyString());
    }
    
    @Test
    public void assertPersistDataSourcesNode() {
        registryCenter.persistDataNodes();
        verify(registryRepository).persist("/states/datanodes", "");
    }
    
    @Test
    public void assertPersistInstanceData() {
        registryCenter.persistInstanceData("test");
        verify(registryRepository).persist(anyString(), eq("test"));
    }
    
    @Test
    public void assertLoadInstanceData() {
        registryCenter.loadInstanceData();
        verify(registryRepository).get(anyString());
    }
    
    @Test
    public void assertLoadDisabledDataSources() {
        List<String> disabledDataSources = Collections.singletonList("replica_ds_0");
        when(registryRepository.getChildrenKeys(anyString())).thenReturn(disabledDataSources);
        registryCenter.loadDisabledDataSources("replica_query_db");
        verify(registryRepository).getChildrenKeys(anyString());
        verify(registryRepository).get(anyString());
    }
    
    @Test
    public void assertTryLock() {
        registryCenter.tryLock("test", 50L);
        verify(registryRepository).tryLock(eq(new LockNode().getLockNodePath("test")), eq(50L), eq(TimeUnit.MILLISECONDS));
    }
    
    @Test
    public void assertReleaseLock() {
        registryCenter.releaseLock("test");
        verify(registryRepository).releaseLock(eq(new LockNode().getLockNodePath("test")));
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertMoreSchema() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), false);
        verify(registryRepository, times(0)).persist("/metadata", "myTest1,myTest2,sharding_db");
    }
    
    @Test
    public void assertMoreAndContainsSchema() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), false);
        verify(registryRepository, times(0)).persist("/metadata", "myTest1,sharding_db");
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithoutAuthenticationAndIsOverwrite() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(SHARDING_RULE_YAML));
    }
    
    @Test
    public void assertPersistConfigurationForReplicaQueryRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createReadWriteSplittingRuleConfiguration(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForReplicaQueryRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createReadWriteSplittingRuleConfiguration(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForReadWriteSplittingWithoutAuthenticationAndIsOverwrite() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createReadWriteSplittingRuleConfiguration(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(READ_WRITE_SPLITTING_RULE_YAML));
    }
    
    @Test
    public void assertPersistConfigurationForDatabaseDiscoveryRuleWithoutAuthenticationAndIsOverwrite() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createDatabaseDiscoveryRuleConfiguration(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(DB_DISCOVERY_RULE_YAML));
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithAuthenticationAndIsOverwrite() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(SHARDING_RULE_YAML));
    }
    
    @Test
    public void assertPersistConfigurationForReplicaQueryRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createReadWriteSplittingRuleConfiguration(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForReadWriteSplittingRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createReadWriteSplittingRuleConfiguration(), false);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), any());
    }
    
    @Test
    public void assertPersistConfigurationForReadWriteSplittingRuleWithAuthenticationAndIsOverwrite() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createReadWriteSplittingRuleConfiguration(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(READ_WRITE_SPLITTING_RULE_YAML));
    }
    
    @Test
    public void assertPersistConfigurationForDatabaseDiscoveryRuleWithAuthenticationAndIsOverwrite() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createDatabaseDiscoveryRuleConfiguration(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(DB_DISCOVERY_RULE_YAML));
    }
    
    @Test
    public void assertPersistConfigurationForEncrypt() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createEncryptRuleConfiguration(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(ENCRYPT_RULE_YAML));
    }
    
    @Test
    public void assertNullRuleConfiguration() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), Collections.emptyList(), true);
    }
    
    @Test
    public void assertPersistConfigurationForShadow() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createShadowRuleConfiguration(), true);
        verify(registryRepository).persist(eq("/metadata/sharding_db/datasource"), any());
        verify(registryRepository, times(0)).persist("/metadata/sharding_db/rule", readYAML(SHADOW_RULE_YAML));
    }
    
    @Test
    public void assertPersistGlobalConfiguration() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistGlobalConfiguration(YamlUsersConfigurationConverter.convertShardingSphereUser(YamlEngine.unmarshal(readYAML(USERS_YAML), Collection.class)), createProperties(), true);
        verify(registryRepository, times(0)).persist("/users", readYAML(USERS_YAML));
        verify(registryRepository).persist("/props", PROPS_YAML);
    }
    
    private Map<String, DataSourceConfiguration> createDataSourceConfigurations() {
        return createDataSourceMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> 
                DataSourceConfiguration.getDataSourceConfiguration(entry.getValue()), (oldValue, currentValue) -> oldValue, LinkedHashMap::new));
    }
    
    private DataSourceConfiguration createDataSourceConfiguration(final DataSource dataSource) {
        return DataSourceConfiguration.getDataSourceConfiguration(dataSource);
    }
    
    private Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> result = new LinkedHashMap<>(2, 1);
        result.put("ds_0", createDataSource("ds_0"));
        result.put("ds_1", createDataSource("ds_1"));
        return result;
    }
    
    private DataSource createDataSource(final String name) {
        MockDataSource result = new MockDataSource();
        result.setDriverClassName("com.mysql.jdbc.Driver");
        result.setUrl("jdbc:mysql://localhost:3306/" + name);
        result.setUsername("root");
        result.setPassword("root");
        return result;
    }
    
    private Collection<RuleConfiguration> createRuleConfigurations() {
        return new YamlRuleConfigurationSwapperEngine().swapToRuleConfigurations(YamlEngine.unmarshal(readYAML(SHARDING_RULE_YAML), YamlRuleConfigurationWrap.class).getRules());
    }
    
    private Collection<RuleConfiguration> createReadWriteSplittingRuleConfiguration() {
        return new YamlRuleConfigurationSwapperEngine().swapToRuleConfigurations(YamlEngine.unmarshal(readYAML(READ_WRITE_SPLITTING_RULE_YAML), YamlRootRuleConfigurations.class).getRules());
    }
    
    private Collection<RuleConfiguration> createDatabaseDiscoveryRuleConfiguration() {
        return new YamlRuleConfigurationSwapperEngine().swapToRuleConfigurations(YamlEngine.unmarshal(readYAML(DB_DISCOVERY_RULE_YAML), YamlRootRuleConfigurations.class).getRules());
    }
    
    private Collection<RuleConfiguration> createEncryptRuleConfiguration() {
        return new YamlRuleConfigurationSwapperEngine().swapToRuleConfigurations(YamlEngine.unmarshal(readYAML(ENCRYPT_RULE_YAML), YamlRootRuleConfigurations.class).getRules());
    }
    
    private Collection<RuleConfiguration> createShadowRuleConfiguration() {
        return new YamlRuleConfigurationSwapperEngine().swapToRuleConfigurations(YamlEngine.unmarshal(readYAML(SHADOW_RULE_YAML), YamlRootRuleConfigurations.class).getRules());
    }

    private Properties createProperties() {
        Properties result = new Properties();
        result.put(ConfigurationPropertyKey.SQL_SHOW.getKey(), Boolean.FALSE);
        return result;
    }
    
    @Test
    public void assertLoadDataSourceConfigurations() {
        when(registryRepository.get("/metadata/sharding_db/datasource")).thenReturn(readYAML(DATA_SOURCE_YAM));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Map<String, DataSourceConfiguration> actual = registryCenter.loadDataSourceConfigurations("sharding_db");
        assertThat(actual.size(), is(2));
        assertDataSourceConfiguration(actual.get("ds_0"), createDataSourceConfiguration(createDataSource("ds_0")));
        assertDataSourceConfiguration(actual.get("ds_1"), createDataSourceConfiguration(createDataSource("ds_1")));
    }
    
    private void assertDataSourceConfiguration(final DataSourceConfiguration actual, final DataSourceConfiguration expected) {
        assertThat(actual.getDataSourceClassName(), is(expected.getDataSourceClassName()));
        assertThat(actual.getProps().get("url"), is(expected.getProps().get("url")));
        assertThat(actual.getProps().get("username"), is(expected.getProps().get("username")));
        assertThat(actual.getProps().get("password"), is(expected.getProps().get("password")));
    }
    
    @Test
    public void assertLoadDataSourceConfigurationsNotExistPath() {
        when(registryRepository.get("/metadata/sharding_db/datasource")).thenReturn("");
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Map<String, DataSourceConfiguration> actual = registryCenter.loadDataSourceConfigurations("sharding_db");
        assertThat(actual.size(), is(0));
    }
    
    @Test
    public void assertLoadShardingAndEncryptRuleConfiguration() {
        when(registryRepository.get("/metadata/sharding_db/rule")).thenReturn(readYAML(SHARDING_AND_ENCRYPT_RULE_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Collection<RuleConfiguration> ruleConfigurations = registryCenter.loadRuleConfigurations("sharding_db");
        assertThat(ruleConfigurations.size(), is(2));
        for (RuleConfiguration each : ruleConfigurations) {
            if (each instanceof ShardingRuleConfiguration) {
                ShardingRuleConfiguration shardingRuleConfig = (ShardingRuleConfiguration) each;
                assertThat(shardingRuleConfig.getTables().size(), is(1));
                assertThat(shardingRuleConfig.getTables().iterator().next().getLogicTable(), is("t_order"));
            } else if (each instanceof EncryptRuleConfiguration) {
                EncryptRuleConfiguration encryptRuleConfig = (EncryptRuleConfiguration) each;
                assertThat(encryptRuleConfig.getEncryptors().size(), is(2));
                ShardingSphereAlgorithmConfiguration encryptAlgorithmConfig = encryptRuleConfig.getEncryptors().get("aes_encryptor");
                assertThat(encryptAlgorithmConfig.getType(), is("AES"));
                assertThat(encryptAlgorithmConfig.getProps().get("aes-key-value").toString(), is("123456abcd"));
            }
        }
    }
    
    @Test
    public void assertLoadShardingRuleConfiguration() {
        when(registryRepository.get("/metadata/sharding_db/rule")).thenReturn(readYAML(SHARDING_RULE_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Collection<RuleConfiguration> actual = registryCenter.loadRuleConfigurations("sharding_db");
        assertThat(actual.size(), is(1));
        ShardingRuleConfiguration actualShardingRuleConfig = (ShardingRuleConfiguration) actual.iterator().next();
        assertThat(actualShardingRuleConfig.getTables().size(), is(1));
        assertThat(actualShardingRuleConfig.getTables().iterator().next().getLogicTable(), is("t_order"));
    }
    
    @Test
    public void assertLoadReadWriteSplittingRuleConfiguration() {
        when(registryRepository.get("/metadata/sharding_db/rule")).thenReturn(readYAML(READ_WRITE_SPLITTING_RULE_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Collection<RuleConfiguration> actual = registryCenter.loadRuleConfigurations("sharding_db");
        ReadWriteSplittingRuleConfiguration config = (ReadWriteSplittingRuleConfiguration) actual.iterator().next();
        assertThat(config.getDataSources().size(), is(1));
        assertThat(config.getDataSources().iterator().next().getWriteDataSourceName(), is("write_ds"));
        assertThat(config.getDataSources().iterator().next().getReadDataSourceNames().size(), is(2));
    }
    
    @Test
    public void assertLoadDatabaseDiscoveryRuleConfiguration() {
        when(registryRepository.get("/metadata/sharding_db/rule")).thenReturn(readYAML(DB_DISCOVERY_RULE_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Collection<RuleConfiguration> actual = registryCenter.loadRuleConfigurations("sharding_db");
        DatabaseDiscoveryRuleConfiguration config = (DatabaseDiscoveryRuleConfiguration) actual.iterator().next();
        assertThat(config.getDataSources().size(), is(1));
        assertThat(config.getDataSources().iterator().next().getDataSourceNames().size(), is(3));
    }
    
    @Test
    public void assertLoadEncryptRuleConfiguration() {
        when(registryRepository.get("/metadata/sharding_db/rule")).thenReturn(readYAML(ENCRYPT_RULE_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        EncryptRuleConfiguration actual = (EncryptRuleConfiguration) registryCenter.loadRuleConfigurations("sharding_db").iterator().next();
        assertThat(actual.getEncryptors().size(), is(1));
        ShardingSphereAlgorithmConfiguration encryptAlgorithmConfig = actual.getEncryptors().get("order_encryptor");
        assertThat(encryptAlgorithmConfig.getType(), is("AES"));
        assertThat(encryptAlgorithmConfig.getProps().get("aes-key-value").toString(), is("123456"));
    }
    
    @Test
    public void assertLoadShadowRuleConfiguration() {
        when(registryRepository.get("/metadata/sharding_db/rule")).thenReturn(readYAML(SHADOW_RULE_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        ShadowRuleConfiguration actual = (ShadowRuleConfiguration) registryCenter.loadRuleConfigurations("sharding_db").iterator().next();
        assertThat(actual.getSourceDataSourceNames(), is(Arrays.asList("ds", "ds1")));
        assertThat(actual.getShadowDataSourceNames(), is(Arrays.asList("shadow_ds", "shadow_ds1")));
        assertThat(actual.getColumn(), is("shadow"));
    }
    
    @Test
    public void assertLoadUsers() {
        when(registryRepository.get("/users")).thenReturn(readYAML(USERS_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Collection<ShardingSphereUser> actual = registryCenter.loadUsers();
        Optional<ShardingSphereUser> user = actual.stream().filter(each -> each.getGrantee().equals(new Grantee("root1", ""))).findFirst();
        assertTrue(user.isPresent());
        assertThat(user.get().getPassword(), is("root1"));
    }
    
    @Test
    public void assertLoadProperties() {
        when(registryRepository.get("/props")).thenReturn(PROPS_YAML);
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Properties actual = registryCenter.loadProperties();
        assertThat(actual.get(ConfigurationPropertyKey.SQL_SHOW.getKey()), is(Boolean.FALSE));
    }
    
    @Test
    public void assertGetAllSchemaNames() {
        when(registryRepository.get("/metadata")).thenReturn("sharding_db,replica_query_db");
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Collection<String> actual = registryCenter.getAllSchemaNames();
        assertThat(actual.size(), is(2));
        assertThat(actual, hasItems("sharding_db"));
        assertThat(actual, hasItems("replica_query_db"));
    }
    
    @Test
    public void assertLoadDataSourceConfigurationsWithConnectionInitSqls() {
        when(registryRepository.get("/metadata/sharding_db/datasource")).thenReturn(readYAML(DATA_SOURCE_YAML_WITH_CONNECTION_INIT_SQL));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Map<String, DataSourceConfiguration> actual = registryCenter.loadDataSourceConfigurations("sharding_db");
        assertThat(actual.size(), is(2));
        assertDataSourceConfigurationWithConnectionInitSqls(actual.get("ds_0"), createDataSourceConfiguration(createDataSourceWithConnectionInitSqls("ds_0")));
        assertDataSourceConfigurationWithConnectionInitSqls(actual.get("ds_1"), createDataSourceConfiguration(createDataSourceWithConnectionInitSqls("ds_1")));
    }
    
    private DataSource createDataSourceWithConnectionInitSqls(final String name) {
        MockDataSource result = new MockDataSource();
        result.setDriverClassName("com.mysql.jdbc.Driver");
        result.setUrl("jdbc:mysql://localhost:3306/" + name);
        result.setUsername("root");
        result.setPassword("root");
        result.setConnectionInitSqls(Arrays.asList("set names utf8mb4;", "set names utf8;"));
        return result;
    }
    
    private void assertDataSourceConfigurationWithConnectionInitSqls(final DataSourceConfiguration actual, final DataSourceConfiguration expected) {
        assertThat(actual.getDataSourceClassName(), is(expected.getDataSourceClassName()));
        assertThat(actual.getProps().get("url"), is(expected.getProps().get("url")));
        assertThat(actual.getProps().get("username"), is(expected.getProps().get("username")));
        assertThat(actual.getProps().get("password"), is(expected.getProps().get("password")));
        assertThat(actual.getProps().get("connectionInitSqls"), is(expected.getProps().get("connectionInitSqls")));
    }
    
    @SneakyThrows({IOException.class, URISyntaxException.class})
    private String readYAML(final String yamlFile) {
        return Files.readAllLines(Paths.get(ClassLoader.getSystemResource(yamlFile).toURI()))
                .stream().filter(each -> !each.startsWith("#")).map(each -> each + System.lineSeparator()).collect(Collectors.joining());
    }
    
    @Test
    public void assertPersistSchemaNameWithExistSchema() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        when(registryRepository.get("/metadata")).thenReturn("sharding_db");
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), true);
        verify(registryRepository, times(0)).persist(eq("/metadata"), eq("sharding_db"));
    }
    
    @Test
    public void assertPersistSchemaNameWithExistAndNewSchema() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        when(registryRepository.get("/metadata")).thenReturn("replica_query_db");
        registryCenter.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), true);
        verify(registryRepository).persist(eq("/metadata"), eq("replica_query_db,sharding_db"));
    }
    
    @Test
    public void assertRenewDataSourceEvent() {
        DataSourceAddedEvent event = new DataSourceAddedEvent("sharding_db", createDataSourceConfigurations());
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository).persist(startsWith("/metadata/sharding_db/datasource"), anyString());
    }
    
    @Test
    public void assertRenewDataSourceEventHasDataSourceConfig() {
        DataSourceAddedEvent event = new DataSourceAddedEvent("sharding_db", createDataSourceConfigurations());
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        String dataSourceYaml = "dataSources:\n"
            + " ds_0:\n"
            + "   dataSourceClassName: xxx\n"
            + "   url: jdbc:mysql://127.0.0.1:3306/demo_ds_0?serverTimezone=UTC&useSSL=false\n"
            + "   username: root\n"
            + "   password: root\n"
            + "   connectionTimeoutMilliseconds: 30000\n"
            + "   idleTimeoutMilliseconds: 60000\n"
            + "   maxLifetimeMilliseconds: 1800000\n"
            + "   maxPoolSize: 50\n"
            + "   minPoolSize: 1\n"
            + "   maintenanceIntervalMilliseconds: 30000\n";
        when(registryRepository.get("/metadata/sharding_db/datasource")).thenReturn(dataSourceYaml);
        registryCenter.renew(event);
        verify(registryRepository).persist(startsWith("/metadata/sharding_db/datasource"), anyString());
    }
    
    @Test
    public void assertRenewRuleEvent() {
        RuleConfigurationsAlteredEvent event = new RuleConfigurationsAlteredEvent("sharding_db", createRuleConfigurations());
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository).persist(startsWith("/metadata/sharding_db/rule"), anyString());
    }
    
    @Test
    public void assertRenewSchemaNameEventWithDrop() {
        MetaDataDroppedEvent event = new MetaDataDroppedEvent("sharding_db");
        when(registryRepository.get("/metadata")).thenReturn("sharding_db,replica_query_db");
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository).persist(eq("/metadata"), eq("replica_query_db"));
    }
    
    @Test
    public void assertRenewSchemaNameEventWithDropAndNotExist() {
        MetaDataDroppedEvent event = new MetaDataDroppedEvent("sharding_db");
        when(registryRepository.get("/metadata")).thenReturn("replica_query_db");
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository, times(0)).persist(eq("/metadata"), eq("replica_query_db"));
    }
    
    @Test
    public void assertRenewSchemaNameEventWithAdd() {
        MetaDataCreatedEvent event = new MetaDataCreatedEvent("sharding_db");
        when(registryRepository.get("/metadata")).thenReturn("replica_query_db");
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository).persist(eq("/metadata"), eq("replica_query_db,sharding_db"));
    }
    
    @Test
    public void assertRenewSchemaNameEventWithAddAndExist() {
        MetaDataCreatedEvent event = new MetaDataCreatedEvent("sharding_db");
        when(registryRepository.get("/metadata")).thenReturn("sharding_db,replica_query_db");
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository, times(0)).persist(eq("/metadata"), eq("sharding_db,replica_query_db"));
    }
    
    @Test
    public void assertPersistSchema() {
        ShardingSphereSchema schema = new SchemaYamlSwapper().swapToObject(YamlEngine.unmarshal(readYAML(META_DATA_YAML), YamlSchema.class));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.persistSchema("sharding_db", schema);
        verify(registryRepository).persist(eq("/metadata/sharding_db/schema"), anyString());
    }
    
    @Test
    public void assertLoadSchema() {
        when(registryRepository.get("/metadata/sharding_db/schema")).thenReturn(readYAML(META_DATA_YAML));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Optional<ShardingSphereSchema> schemaOptional = registryCenter.loadSchema("sharding_db");
        assertTrue(schemaOptional.isPresent());
        Optional<ShardingSphereSchema> empty = registryCenter.loadSchema("test");
        assertThat(empty, is(Optional.empty()));
        ShardingSphereSchema schema = schemaOptional.get();
        verify(registryRepository).get(eq("/metadata/sharding_db/schema"));
        assertThat(schema.getAllTableNames(), is(Collections.singleton("t_order")));
        assertThat(schema.get("t_order").getIndexes().keySet(), is(Collections.singleton("primary")));
        assertThat(schema.getAllColumnNames("t_order").size(), is(1));
        assertThat(schema.get("t_order").getColumns().keySet(), is(Collections.singleton("id")));
    }
    
    @Test
    public void assertRenewSchemaAlteredEvent() {
        SchemaAlteredEvent event = new SchemaAlteredEvent("sharding_db", new SchemaYamlSwapper().swapToObject(YamlEngine.unmarshal(readYAML(META_DATA_YAML), YamlSchema.class)));
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository).persist(eq("/metadata/sharding_db/schema"), anyString());
    }
    
    @Test
    public void assertDeleteSchema() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.deleteSchema("sharding_db");
        verify(registryRepository).delete(eq("/metadata/sharding_db"));
    }
    
    @Test
    @SneakyThrows
    public void assertRenewSwitchRuleConfigurationEvent() {
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        Field field = RegistryCenter.class.getDeclaredField("registryCacheManager");
        field.setAccessible(true);
        field.set(registryCenter, registryCacheManager);
        when(registryCacheManager.loadCache(anyString(), eq("testCacheId"))).thenReturn(readYAML(SHARDING_RULE_YAML));
        SwitchRuleConfigurationEvent event = new SwitchRuleConfigurationEvent("sharding_db", "testCacheId");
        registryCenter.renew(event);
        verify(registryRepository).persist(eq("/metadata/sharding_db/rule"), anyString());
        verify(registryCacheManager).deleteCache(eq("/metadata/sharding_db/rule"), eq("testCacheId"));
    }
    
    @Test
    public void assertRenewDataSourceAlteredEvent() {
        DataSourceAlteredEvent event = new DataSourceAlteredEvent("sharding_db", createDataSourceConfigurations());
        RegistryCenter registryCenter = new RegistryCenter(registryRepository);
        registryCenter.renew(event);
        verify(registryRepository).persist(startsWith("/metadata/sharding_db/datasource"), anyString());
    }
}
