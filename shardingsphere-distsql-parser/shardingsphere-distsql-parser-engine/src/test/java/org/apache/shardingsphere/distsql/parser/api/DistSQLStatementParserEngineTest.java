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

package org.apache.shardingsphere.distsql.parser.api;

import org.apache.shardingsphere.distsql.parser.segment.DataSourceSegment;
import org.apache.shardingsphere.distsql.parser.segment.TableRuleSegment;
import org.apache.shardingsphere.distsql.parser.segment.rdl.ShardingBindingTableRuleSegment;
import org.apache.shardingsphere.distsql.parser.statement.rdl.create.impl.AddResourceStatement;
import org.apache.shardingsphere.distsql.parser.statement.rdl.create.impl.CreateShardingBindingTableRulesStatement;
import org.apache.shardingsphere.distsql.parser.statement.rdl.create.impl.CreateShardingBroadcastTableRulesStatement;
import org.apache.shardingsphere.distsql.parser.statement.rdl.create.impl.CreateShardingTableRuleStatement;
import org.apache.shardingsphere.distsql.parser.statement.rdl.drop.impl.DropResourceStatement;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class DistSQLStatementParserEngineTest {
    
    private static final String RDL_ADD_RESOURCE_SINGLE_WITHOUT_PASSWORD = "ADD RESOURCE ds_0(HOST=127.0.0.1,PORT=3306,DB=test0,USER=ROOT);";
    
    private static final String RDL_ADD_RESOURCE_SINGLE_WITH_PASSWORD = "ADD RESOURCE ds_0(HOST=127.0.0.1,PORT=3306,DB=test0,USER=ROOT,PASSWORD=123456);";
    
    private static final String RDL_ADD_RESOURCE_MULTIPLE = "ADD RESOURCE ds_0(HOST=127.0.0.1,PORT=3306,DB=test0,USER=ROOT,PASSWORD=123456),"
            + "ds_1(HOST=127.0.0.1,PORT=3306,DB=test1,USER=ROOT,PASSWORD=123456);";
    
    private static final String RDL_DROP_RESOURCE = "DROP RESOURCE ds_0,ds_1";
    
    private static final String RDL_CREATE_SHARDING_TABLE_RULE = "CREATE SHARDING TABLE RULE t_order (" 
            + "RESOURCES(ms_group_0,ms_group_1)," 
            + "SHARDING_COLUMN=order_id," 
            + "TYPE(NAME=hash_mod,PROPERTIES('sharding-count'=4))," 
            + "GENERATED_KEY(COLUMN=another_id,TYPE(NAME=snowflake,PROPERTIES(\"worker-id\"=123))))";
    
    private static final String RDL_CREATE_SHARDING_BINDING_TABLE_RULES = "CREATE SHARDING BINDING TABLE RULES (" 
            + "(t_order,t_order_item), (t_1,t_2))";
    
    private static final String RDL_CREATE_SHARDING_BROADCAST_TABLE_RULES = "CREATE SHARDING BROADCAST TABLE RULES(t_1,t_2)";
    
    private final DistSQLStatementParserEngine engine = new DistSQLStatementParserEngine();
    
    @Test
    public void assertParseAddSingleResourceWithoutPassword() {
        SQLStatement sqlStatement = engine.parse(RDL_ADD_RESOURCE_SINGLE_WITHOUT_PASSWORD);
        assertTrue(sqlStatement instanceof AddResourceStatement);
        assertThat(((AddResourceStatement) sqlStatement).getDataSources().size(), is(1));
        DataSourceSegment dataSourceSegment = ((AddResourceStatement) sqlStatement).getDataSources().iterator().next();
        assertThat(dataSourceSegment.getName(), is("ds_0"));
        assertThat(dataSourceSegment.getHostName(), is("127.0.0.1"));
        assertThat(dataSourceSegment.getPort(), is("3306"));
        assertThat(dataSourceSegment.getDb(), is("test0"));
        assertThat(dataSourceSegment.getUser(), is("ROOT"));
    }
    
    @Test
    public void assertParseAddSingleResourceWithPassword() {
        SQLStatement sqlStatement = engine.parse(RDL_ADD_RESOURCE_SINGLE_WITH_PASSWORD);
        assertTrue(sqlStatement instanceof AddResourceStatement);
        assertThat(((AddResourceStatement) sqlStatement).getDataSources().size(), is(1));
        DataSourceSegment dataSourceSegment = ((AddResourceStatement) sqlStatement).getDataSources().iterator().next();
        assertThat(dataSourceSegment.getName(), is("ds_0"));
        assertThat(dataSourceSegment.getHostName(), is("127.0.0.1"));
        assertThat(dataSourceSegment.getPort(), is("3306"));
        assertThat(dataSourceSegment.getDb(), is("test0"));
        assertThat(dataSourceSegment.getUser(), is("ROOT"));
        assertThat(dataSourceSegment.getPassword(), is("123456"));
    }
    
    @Test
    public void assertParseAddMultipleResources() {
        SQLStatement sqlStatement = engine.parse(RDL_ADD_RESOURCE_MULTIPLE);
        assertTrue(sqlStatement instanceof AddResourceStatement);
        assertThat(((AddResourceStatement) sqlStatement).getDataSources().size(), is(2));
        List<DataSourceSegment> dataSourceSegments = new ArrayList<>(((AddResourceStatement) sqlStatement).getDataSources());
        DataSourceSegment dataSourceSegment = dataSourceSegments.get(0);
        assertThat(dataSourceSegment.getName(), is("ds_0"));
        assertThat(dataSourceSegment.getHostName(), is("127.0.0.1"));
        assertThat(dataSourceSegment.getPort(), is("3306"));
        assertThat(dataSourceSegment.getDb(), is("test0"));
        assertThat(dataSourceSegment.getUser(), is("ROOT"));
        assertThat(dataSourceSegment.getPassword(), is("123456"));
        dataSourceSegment = dataSourceSegments.get(1);
        assertThat(dataSourceSegment.getName(), is("ds_1"));
        assertThat(dataSourceSegment.getHostName(), is("127.0.0.1"));
        assertThat(dataSourceSegment.getPort(), is("3306"));
        assertThat(dataSourceSegment.getDb(), is("test1"));
        assertThat(dataSourceSegment.getUser(), is("ROOT"));
        assertThat(dataSourceSegment.getPassword(), is("123456"));
    }
    
    @Test
    public void assertParseDropResource() {
        SQLStatement sqlStatement = engine.parse(RDL_DROP_RESOURCE);
        assertTrue(sqlStatement instanceof DropResourceStatement);
        assertThat(((DropResourceStatement) sqlStatement).getResourceNames().size(), is(2));
        assertTrue(((DropResourceStatement) sqlStatement).getResourceNames().containsAll(Arrays.asList("ds_0", "ds_1")));
    }
    
    @Test
    public void assertParseCreateShardingTableRule() {
        SQLStatement sqlStatement = engine.parse(RDL_CREATE_SHARDING_TABLE_RULE);
        assertTrue(sqlStatement instanceof CreateShardingTableRuleStatement);
        assertThat(((CreateShardingTableRuleStatement) sqlStatement).getTables().size(), is(1));
        TableRuleSegment tableRuleSegment = ((CreateShardingTableRuleStatement) sqlStatement).getTables().iterator().next();
        assertThat(tableRuleSegment.getLogicTable(), is("t_order"));
        assertTrue(tableRuleSegment.getDataSources().containsAll(Arrays.asList("ms_group_0", "ms_group_1")));
        assertThat(tableRuleSegment.getTableStrategyColumn(), is("order_id"));
        assertThat(tableRuleSegment.getKeyGenerateStrategy().getAlgorithmName(), is("snowflake"));
        assertThat(tableRuleSegment.getKeyGenerateStrategy().getAlgorithmProps().getProperty("worker-id"), is("123"));
        assertThat(tableRuleSegment.getKeyGenerateStrategyColumn(), is("another_id"));
        assertThat(tableRuleSegment.getTableStrategy().getAlgorithmName(), is("hash_mod"));
        assertThat(tableRuleSegment.getTableStrategy().getAlgorithmProps().getProperty("sharding-count"), is("4"));
    }
    
    @Test
    public void assertParseCreateShardingBindingTableRules() {
        SQLStatement sqlStatement = engine.parse(RDL_CREATE_SHARDING_BINDING_TABLE_RULES);
        assertTrue(sqlStatement instanceof CreateShardingBindingTableRulesStatement);
        List<ShardingBindingTableRuleSegment> shardingBindingTableRuleSegments = new ArrayList<>(((CreateShardingBindingTableRulesStatement) sqlStatement).getRules());
        assertThat(shardingBindingTableRuleSegments.size(), is(2));
        ShardingBindingTableRuleSegment segment = shardingBindingTableRuleSegments.get(0);
        assertThat(segment.getTables(), is("t_order,t_order_item"));
        segment = shardingBindingTableRuleSegments.get(1);
        assertThat(segment.getTables(), is("t_1,t_2"));
    }
    
    @Test
    public void assertParseCreateShardingBroadcastTableRules() {
        SQLStatement sqlStatement = engine.parse(RDL_CREATE_SHARDING_BROADCAST_TABLE_RULES);
        assertTrue(sqlStatement instanceof CreateShardingBroadcastTableRulesStatement);
        assertThat(((CreateShardingBroadcastTableRulesStatement) sqlStatement).getTables(), is(Arrays.asList("t_1", "t_2")));
    }
}
