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

package org.apache.shardingsphere.proxy.backend.text.distsql.rdl.impl;

import com.google.common.collect.Maps;
import org.apache.shardingsphere.distsql.parser.segment.FunctionSegment;
import org.apache.shardingsphere.distsql.parser.segment.rdl.EncryptColumnSegment;
import org.apache.shardingsphere.distsql.parser.segment.rdl.EncryptRuleSegment;
import org.apache.shardingsphere.distsql.parser.statement.rdl.create.impl.CreateEncryptRuleStatement;
import org.apache.shardingsphere.encrypt.api.config.EncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.api.config.rule.EncryptTableRuleConfiguration;
import org.apache.shardingsphere.encrypt.spi.EncryptAlgorithm;
import org.apache.shardingsphere.infra.context.metadata.MetaDataContexts;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.exception.DuplicateRuleNamesException;
import org.apache.shardingsphere.proxy.backend.exception.InvalidEncryptorsException;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.update.UpdateResponseHeader;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class CreateEncryptRuleBackendHandlerTest {
    
    @Mock
    private BackendConnection backendConnection;
    
    @Mock
    private CreateEncryptRuleStatement sqlStatement;
    
    @Mock
    private MetaDataContexts metaDataContexts;
    
    @Mock
    private TransactionContexts transactionContexts;
    
    @Mock
    private ShardingSphereMetaData shardingSphereMetaData;
    
    @Mock
    private ShardingSphereRuleMetaData ruleMetaData;
    
    private CreateEncryptRuleBackendHandler handler = new CreateEncryptRuleBackendHandler(sqlStatement, backendConnection);
    
    @Before
    public void setUp() {
        ShardingSphereServiceLoader.register(EncryptAlgorithm.class);
        ProxyContext.getInstance().init(metaDataContexts, transactionContexts);
        when(metaDataContexts.getAllSchemaNames()).thenReturn(Collections.singletonList("test"));
        when(metaDataContexts.getMetaData(eq("test"))).thenReturn(shardingSphereMetaData);
        when(shardingSphereMetaData.getRuleMetaData()).thenReturn(ruleMetaData);
    }
    
    @Test
    public void assertExecute() {
        EncryptRuleSegment encryptRuleSegment = new EncryptRuleSegment("t_encrypt", buildColumns("MD5"));
        when(sqlStatement.getEncryptRules()).thenReturn(Collections.singletonList(encryptRuleSegment));
        ResponseHeader responseHeader = handler.execute("test", sqlStatement);
        assertNotNull(responseHeader);
        assertTrue(responseHeader instanceof UpdateResponseHeader);
    }
    
    @Test(expected = DuplicateRuleNamesException.class)
    public void assertExecuteWithDuplicateEncryptRule() {
        EncryptTableRuleConfiguration encryptTableRuleConfiguration = new EncryptTableRuleConfiguration("t_encrypt", Collections.emptyList());
        when(ruleMetaData.getConfigurations()).thenReturn(Collections.singletonList(new EncryptRuleConfiguration(Collections
                .singleton(encryptTableRuleConfiguration), Maps.newHashMap())));
        EncryptRuleSegment encryptRuleSegment = new EncryptRuleSegment("t_encrypt", buildColumns("MD5"));
        when(sqlStatement.getEncryptRules()).thenReturn(Collections.singletonList(encryptRuleSegment));
        handler.execute("test", sqlStatement);
    }

    @Test(expected = InvalidEncryptorsException.class)
    public void assertExecuteWithInvalidEncryptors() {
        EncryptRuleSegment encryptRuleSegment = new EncryptRuleSegment("t_encrypt", buildColumns("notExistEncryptor"));
        when(sqlStatement.getEncryptRules()).thenReturn(Collections.singletonList(encryptRuleSegment));
        handler.execute("test", sqlStatement);
    }

    private Collection<EncryptColumnSegment> buildColumns(final String encryptorName) {
        EncryptColumnSegment encryptColumnSegment = new EncryptColumnSegment();
        encryptColumnSegment.setName("user_id");
        encryptColumnSegment.setPlainColumn("user_plain");
        encryptColumnSegment.setCipherColumn("user_cipher");
        FunctionSegment functionSegment = new FunctionSegment();
        functionSegment.setAlgorithmName(encryptorName);
        encryptColumnSegment.setEncryptor(functionSegment);
        return Collections.singleton(encryptColumnSegment);
    }
}
