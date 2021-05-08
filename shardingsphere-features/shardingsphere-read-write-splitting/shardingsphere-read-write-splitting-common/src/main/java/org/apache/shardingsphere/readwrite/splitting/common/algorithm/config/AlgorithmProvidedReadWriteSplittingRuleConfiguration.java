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

package org.apache.shardingsphere.readwrite.splitting.common.algorithm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.api.rule.ReadWriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.spi.ReplicaLoadBalanceAlgorithm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Algorithm provided read write splitting rule configuration.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public final class AlgorithmProvidedReadWriteSplittingRuleConfiguration implements RuleConfiguration {
    
    private Collection<ReadWriteSplittingDataSourceRuleConfiguration> dataSources = new LinkedList<>();
    
    private Map<String, ReplicaLoadBalanceAlgorithm> loadBalanceAlgorithms = new LinkedHashMap<>();
}
