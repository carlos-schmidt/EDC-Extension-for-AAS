/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.app;

import okhttp3.OkHttpClient;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@ExtendWith(DependencyInjectionExtension.class)
public class AasExtensionTest {

    private AasExtension extension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(AssetIndex.class, mock(AssetIndex.class));
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));
        context.registerService(CatalogService.class, mock(CatalogService.class));
        context.registerService(ConsumerContractNegotiationManager.class,
                mock(ConsumerContractNegotiationManager.class));
        context.registerService(ContractDefinitionStore.class, mock(ContractDefinitionStore.class));
        context.registerService(ContractNegotiationStore.class, mock(ContractNegotiationStore.class));
        context.registerService(ContractNegotiationObservable.class, mock(ContractNegotiationObservable.class));
        context.registerService(OkHttpClient.class, mock(OkHttpClient.class));
        context.registerService(PolicyDefinitionStore.class, mock(PolicyDefinitionStore.class));
        context.registerService(TransferProcessManager.class, mock(TransferProcessManager.class));
        context.registerService(WebService.class, mock(WebService.class));
        Config mockConf = mock(Config.class);

        this.context = spy(context); // used to inject the config
        when(this.context.getMonitor()).thenReturn(mock(Monitor.class));
        when(this.context.getConfig()).thenReturn(mockConf);
        when(mockConf.getString("edc.dsp.callback.address")).thenReturn("http://localhost:8080");
        when(mockConf.getString("web.http.port")).thenReturn("1234");
        when(mockConf.getString("web.http.path")).thenReturn("api-path");

        extension = factory.constructInstance(AasExtension.class);
    }

    @Test
    public void initializeTest() {
        // See if initialization works
        extension.initialize(this.context);
    }
}
