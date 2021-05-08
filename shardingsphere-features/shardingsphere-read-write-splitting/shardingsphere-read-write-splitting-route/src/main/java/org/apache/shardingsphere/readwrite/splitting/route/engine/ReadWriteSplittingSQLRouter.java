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

package org.apache.shardingsphere.readwrite.splitting.route.engine;

import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.DefaultSchema;
import org.apache.shardingsphere.infra.route.SQLRouter;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.route.context.RouteMapper;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.binder.LogicSQL;
import org.apache.shardingsphere.readwrite.splitting.route.engine.impl.ReadWriteSplittingDataSourceRouter;
import org.apache.shardingsphere.readwrite.splitting.common.constant.ReadWriteSplittingOrder;
import org.apache.shardingsphere.readwrite.splitting.common.rule.ReadWriteSplittingDataSourceRule;
import org.apache.shardingsphere.readwrite.splitting.common.rule.ReadWriteSplittingRule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Read write splitting SQL router.
 */
public final class ReadWriteSplittingSQLRouter implements SQLRouter<ReadWriteSplittingRule> {
    
    @Override
    public RouteContext createRouteContext(final LogicSQL logicSQL, final ShardingSphereMetaData metaData, final ReadWriteSplittingRule rule, final ConfigurationProperties props) {
        RouteContext result = new RouteContext();
        String dataSourceName = new ReadWriteSplittingDataSourceRouter(rule.getSingleDataSourceRule()).route(logicSQL.getSqlStatementContext().getSqlStatement());
        result.getRouteUnits().add(new RouteUnit(new RouteMapper(DefaultSchema.LOGIC_NAME, dataSourceName), Collections.emptyList()));
        return result;
    }
    
    @Override
    public void decorateRouteContext(final RouteContext routeContext,
                                     final LogicSQL logicSQL, final ShardingSphereMetaData metaData, final ReadWriteSplittingRule rule, final ConfigurationProperties props) {
        Collection<RouteUnit> toBeRemoved = new LinkedList<>();
        Collection<RouteUnit> toBeAdded = new LinkedList<>();
        for (RouteUnit each : routeContext.getRouteUnits()) {
            String dataSourceName = each.getDataSourceMapper().getLogicName();
            Optional<ReadWriteSplittingDataSourceRule> dataSourceRule = rule.findDataSourceRule(dataSourceName);
            if (dataSourceRule.isPresent() && dataSourceRule.get().getName().equalsIgnoreCase(each.getDataSourceMapper().getActualName())) {
                toBeRemoved.add(each);
                String actualDataSourceName = new ReadWriteSplittingDataSourceRouter(dataSourceRule.get()).route(logicSQL.getSqlStatementContext().getSqlStatement());
                toBeAdded.add(new RouteUnit(new RouteMapper(each.getDataSourceMapper().getLogicName(), actualDataSourceName), each.getTableMappers()));
            }
        }
        routeContext.getRouteUnits().removeAll(toBeRemoved);
        routeContext.getRouteUnits().addAll(toBeAdded);
    }
    
    @Override
    public int getOrder() {
        return ReadWriteSplittingOrder.ORDER;
    }
    
    @Override
    public Class<ReadWriteSplittingRule> getTypeClass() {
        return ReadWriteSplittingRule.class;
    }
}
