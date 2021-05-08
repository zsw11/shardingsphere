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

package org.apache.shardingsphere.sharding.route.engine.type.broadcast;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.route.context.RouteMapper;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.apache.shardingsphere.sharding.route.engine.type.ShardingRouteEngine;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.TableRule;
import org.apache.shardingsphere.sql.parser.sql.common.segment.ddl.index.IndexSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.ddl.DropIndexStatement;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Sharding broadcast routing engine for tables.
 */
@RequiredArgsConstructor
public final class ShardingTableBroadcastRoutingEngine implements ShardingRouteEngine {
    
    private final ShardingSphereSchema schema;
    
    private final SQLStatementContext<?> sqlStatementContext;
    
    @Override
    public void route(final RouteContext routeContext, final ShardingRule shardingRule) {
        Collection<String> logicTableNames = getLogicTableNames();
        if (logicTableNames.isEmpty()) {
            routeContext.getRouteUnits().addAll(getBroadcastTableRouteUnits(shardingRule, ""));
            return;
        }
        for (String each : logicTableNames) {
            if (shardingRule.getBroadcastTables().contains(each)) {
                routeContext.getRouteUnits().addAll(getBroadcastTableRouteUnits(shardingRule, each));
            } else {
                routeContext.getRouteUnits().addAll(getAllRouteUnits(shardingRule, each));
            }
        }
    }
    
    private Collection<String> getLogicTableNames() {
        Collection<String> tableNamesInSQL = sqlStatementContext.getTablesContext().getTableNames();
        if (!tableNamesInSQL.isEmpty()) {
            return tableNamesInSQL;
        }
        return sqlStatementContext.getSqlStatement() instanceof DropIndexStatement ? getTableNamesFromMetaData((DropIndexStatement) sqlStatementContext.getSqlStatement()) : Collections.emptyList();
    }
    
    private Collection<String> getTableNamesFromMetaData(final DropIndexStatement dropIndexStatement) {
        Collection<String> result = new LinkedList<>();
        for (IndexSegment each : dropIndexStatement.getIndexes()) {
            findLogicTableNameFromMetaData(each.getIdentifier().getValue()).ifPresent(result::add);
        }
        return result;
    }
    
    private Optional<String> findLogicTableNameFromMetaData(final String logicIndexName) {
        for (String each : schema.getAllTableNames()) {
            if (schema.get(each).getIndexes().containsKey(logicIndexName)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }
    
    private Collection<RouteUnit> getBroadcastTableRouteUnits(final ShardingRule shardingRule, final String broadcastTableName) {
        Collection<RouteUnit> result = new LinkedList<>();
        for (String each : shardingRule.getDataSourceNames()) {
            result.add(new RouteUnit(new RouteMapper(each, each), Collections.singletonList(new RouteMapper(broadcastTableName, broadcastTableName))));
        }
        return result;
    }
    
    private Collection<RouteUnit> getAllRouteUnits(final ShardingRule shardingRule, final String logicTableName) {
        Collection<RouteUnit> result = new LinkedList<>();
        TableRule tableRule = shardingRule.getTableRule(logicTableName);
        for (DataNode each : tableRule.getActualDataNodes()) {
            result.add(new RouteUnit(new RouteMapper(each.getDataSourceName(), each.getDataSourceName()), Collections.singletonList(new RouteMapper(logicTableName, each.getTableName()))));
        }
        return result;
    }
}
