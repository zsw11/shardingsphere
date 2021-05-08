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

package org.apache.shardingsphere.infra.rule.builder;

import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.user.ShardingSphereUser;
import org.apache.shardingsphere.infra.rule.scope.GlobalRule;
import org.apache.shardingsphere.infra.spi.ordered.OrderedSPI;

import java.util.Collection;
import java.util.Map;

/**
 * Global rule builder.
 * 
 * @param <R> type of global rule
 * @param <T> type of rule configuration
 */
public interface GlobalRuleBuilder<R extends GlobalRule, T extends RuleConfiguration> extends OrderedSPI<T> {
    
    /**
     * Build global rule.
     *
     * @param ruleConfig rule configuration
     * @param mataDataMap mata data map
     * @param users users
     * @return global rule
     */
    R build(T ruleConfig, Map<String, ShardingSphereMetaData> mataDataMap, Collection<ShardingSphereUser> users);
}
