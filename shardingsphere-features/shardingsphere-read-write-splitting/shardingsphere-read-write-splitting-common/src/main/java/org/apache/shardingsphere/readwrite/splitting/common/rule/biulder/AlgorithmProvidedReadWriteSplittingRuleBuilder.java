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

package org.apache.shardingsphere.readwrite.splitting.common.rule.biulder;

import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.rule.builder.SchemaRuleBuilder;
import org.apache.shardingsphere.readwrite.splitting.common.algorithm.config.AlgorithmProvidedReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.common.constant.ReadWriteSplittingOrder;
import org.apache.shardingsphere.readwrite.splitting.common.rule.ReadWriteSplittingRule;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Algorithm provided read write splitting rule builder.
 */
public final class AlgorithmProvidedReadWriteSplittingRuleBuilder implements SchemaRuleBuilder<ReadWriteSplittingRule, AlgorithmProvidedReadWriteSplittingRuleConfiguration> {
    
    @Override
    public ReadWriteSplittingRule build(final String schemaName, final Map<String, DataSource> dataSourceMap, final DatabaseType databaseType,
                                        final AlgorithmProvidedReadWriteSplittingRuleConfiguration ruleConfig) {
        return new ReadWriteSplittingRule(ruleConfig);
    }
    
    @Override
    public int getOrder() {
        return ReadWriteSplittingOrder.ORDER + 1;
    }
    
    @Override
    public Class<AlgorithmProvidedReadWriteSplittingRuleConfiguration> getTypeClass() {
        return AlgorithmProvidedReadWriteSplittingRuleConfiguration.class;
    }
}
