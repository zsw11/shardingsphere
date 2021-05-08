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

package org.apache.shardingsphere.spring.namespace.governance;

import org.apache.shardingsphere.driver.governance.internal.datasource.GovernanceShardingSphereDataSource;
import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.context.metadata.MetaDataContexts;
import org.apache.shardingsphere.readwrite.splitting.common.algorithm.RandomReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.readwrite.splitting.common.algorithm.RoundRobinReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.readwrite.splitting.common.rule.ReadWriteSplittingDataSourceRule;
import org.apache.shardingsphere.readwrite.splitting.common.rule.ReadWriteSplittingRule;
import org.apache.shardingsphere.readwrite.splitting.spi.ReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.spring.namespace.governance.util.EmbedTestingServer;
import org.apache.shardingsphere.spring.namespace.governance.util.FieldValueUtil;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = "classpath:META-INF/rdb/read-write-splitting-governance.xml")
public class GovernanceReadWriteSplittingNamespaceTest extends AbstractJUnit4SpringContextTests {
    
    @BeforeClass
    public static void init() {
        EmbedTestingServer.start();
    }
    
    @Test
    public void assertReplicaQueryDataSourceType() {
        assertNotNull(applicationContext.getBean("defaultGovernanceDataSource", GovernanceShardingSphereDataSource.class));
    }
    
    @Test
    public void assertDefaultReplicaQueryDataSource() {
        ReadWriteSplittingRule rule = getReplicaQueryRule("defaultGovernanceDataSource");
        Optional<ReadWriteSplittingDataSourceRule> dataSourceRule = rule.findDataSourceRule("default_dbtbl_0");
        assertTrue(dataSourceRule.isPresent());
        assertThat(dataSourceRule.get().getWriteDataSourceName(), is("dbtbl_write_0"));
        assertTrue(dataSourceRule.get().getReadDataSourceNames().contains("dbtbl_0_read_0"));
        assertTrue(dataSourceRule.get().getReadDataSourceNames().contains("dbtbl_0_read_1"));
    }
    
    @Test
    public void assertTypeReplicaQueryDataSource() {
        ReadWriteSplittingRule randomRule = getReplicaQueryRule("randomGovernanceDataSource");
        Optional<ReadWriteSplittingDataSourceRule> randomDataSourceRule = randomRule.findDataSourceRule("random_dbtbl_0");
        assertTrue(randomDataSourceRule.isPresent());
        assertTrue(randomDataSourceRule.get().getLoadBalancer() instanceof RandomReplicaLoadBalanceAlgorithm);
        ReadWriteSplittingRule roundRobinRule = getReplicaQueryRule("roundRobinGovernanceDataSource");
        Optional<ReadWriteSplittingDataSourceRule> roundRobinDataSourceRule = roundRobinRule.findDataSourceRule("roundRobin_dbtbl_0");
        assertTrue(roundRobinDataSourceRule.isPresent());
        assertTrue(roundRobinDataSourceRule.get().getLoadBalancer() instanceof RoundRobinReplicaLoadBalanceAlgorithm);
    }
    
    @Test
    @Ignore
    // TODO load balance algorithm have been construct twice for SpringDatasource extends ReplicaQueryDatasource.
    public void assertRefReplicaQueryDataSource() {
        ReplicaLoadBalanceAlgorithm randomLoadBalanceAlgorithm = applicationContext.getBean("randomLoadBalanceAlgorithm", ReplicaLoadBalanceAlgorithm.class);
        ReadWriteSplittingRule rule = getReplicaQueryRule("refGovernanceDataSource");
        Optional<ReadWriteSplittingDataSourceRule> dataSourceRule = rule.findDataSourceRule("randomLoadBalanceAlgorithm");
        assertTrue(dataSourceRule.isPresent());
        assertThat(dataSourceRule.get().getLoadBalancer(), is(randomLoadBalanceAlgorithm));
    }
    
    private ReadWriteSplittingRule getReplicaQueryRule(final String dataSourceName) {
        GovernanceShardingSphereDataSource dataSource = applicationContext.getBean(dataSourceName, GovernanceShardingSphereDataSource.class);
        MetaDataContexts metaDataContexts = (MetaDataContexts) FieldValueUtil.getFieldValue(dataSource, "metaDataContexts");
        return (ReadWriteSplittingRule) metaDataContexts.getDefaultMetaData().getRuleMetaData().getRules().iterator().next();
    }
    
    @Test
    public void assertProperties() {
        boolean showSQL = getProperties("defaultGovernanceDataSource").getValue(ConfigurationPropertyKey.SQL_SHOW);
        assertTrue(showSQL);
    }
    
    private ConfigurationProperties getProperties(final String dataSourceName) {
        GovernanceShardingSphereDataSource dataSource = applicationContext.getBean(dataSourceName, GovernanceShardingSphereDataSource.class);
        MetaDataContexts metaDataContexts = (MetaDataContexts) FieldValueUtil.getFieldValue(dataSource, "metaDataContexts");
        return metaDataContexts.getProps();
    }
}
