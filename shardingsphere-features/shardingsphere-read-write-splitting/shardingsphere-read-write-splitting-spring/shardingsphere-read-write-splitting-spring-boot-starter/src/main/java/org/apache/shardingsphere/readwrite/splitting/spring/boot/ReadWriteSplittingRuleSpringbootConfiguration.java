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

package org.apache.shardingsphere.readwrite.splitting.spring.boot;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.common.algorithm.config.AlgorithmProvidedReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.spi.ReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.readwrite.splitting.spring.boot.algorithm.ReadWriteSplittingAlgorithmProvidedBeanRegistry;
import org.apache.shardingsphere.readwrite.splitting.spring.boot.condition.ReadWriteSplittingSpringBootCondition;
import org.apache.shardingsphere.readwrite.splitting.spring.boot.rule.YamlReadWriteSplittingRuleSpringBootConfiguration;
import org.apache.shardingsphere.readwrite.splitting.common.yaml.config.YamlReadWriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwrite.splitting.common.yaml.swapper.ReadWriteSplittingRuleAlgorithmProviderConfigurationYamlSwapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Rule spring boot configuration for read write splitting.
 */
@Configuration
@EnableConfigurationProperties(YamlReadWriteSplittingRuleSpringBootConfiguration.class)
@ConditionalOnClass(YamlReadWriteSplittingRuleConfiguration.class)
@Conditional(ReadWriteSplittingSpringBootCondition.class)
@RequiredArgsConstructor
public class ReadWriteSplittingRuleSpringbootConfiguration {
    
    private final ReadWriteSplittingRuleAlgorithmProviderConfigurationYamlSwapper swapper = new ReadWriteSplittingRuleAlgorithmProviderConfigurationYamlSwapper();
    
    private final YamlReadWriteSplittingRuleSpringBootConfiguration yamlConfig;
    
    /**
     * Read write splitting rule configuration for spring boot.
     *
     * @param loadBalanceAlgorithms load balance algorithms
     * @return read write splitting rule configuration
     */
    @Bean
    public RuleConfiguration readWriteSplittingRuleConfiguration(final ObjectProvider<Map<String, ReplicaLoadBalanceAlgorithm>> loadBalanceAlgorithms) {
        AlgorithmProvidedReadWriteSplittingRuleConfiguration result = swapper.swapToObject(yamlConfig.getReadWriteSplitting());
        Map<String, ReplicaLoadBalanceAlgorithm> balanceAlgorithmMap = Optional.ofNullable(loadBalanceAlgorithms.getIfAvailable()).orElse(Collections.emptyMap());
        result.setLoadBalanceAlgorithms(balanceAlgorithmMap);
        return result;
    }
    
    /**
     * Read write splitting algorithm provided bean registry.
     *
     * @param environment environment
     * @return read write splitting algorithm provided bean registry
     */
    @Bean
    public static ReadWriteSplittingAlgorithmProvidedBeanRegistry readWriteSplittingAlgorithmProvidedBeanRegistry(final Environment environment) {
        return new ReadWriteSplittingAlgorithmProvidedBeanRegistry(environment);
    }
}
