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

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.api.model.HttpMethod;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.sync.Synchronizer;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * EDC Extension supporting usage of Asset Administration Shells.
 */
public class AasExtension implements ServiceExtension {


    private static final String SETTINGS_PREFIX = "edc.aas";
    @Inject
    private AasDataProcessorFactory aasDataProcessorFactory;
    @Inject // Register AAS services with self-signed certs to communicate with them
    private AasServiceRegistry aasServiceRegistry;
    @Inject // Register public endpoints
    private PublicApiManagementService publicApiManagementService;
    @Inject // Create / manage EDC assets
    private AssetIndex assetIndex;
    @Inject // Create / manage EDC contracts
    private ContractDefinitionStore contractStore;
    @Inject // Create / manage EDC policies
    private PolicyDefinitionStore policyStore;
    @Inject // Register http endpoint at EDC
    private WebService webService;

    private Monitor monitor;
    private ScheduledExecutorService syncExecutor;
    private AasController aasController;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.monitor = context.getMonitor().withPrefix("EDC4AAS");

        var configurationController = new ConfigurationController(context.getConfig(SETTINGS_PREFIX), monitor);

        // Distribute controllers, repository
        var selfDescriptionRepository = new SelfDescriptionRepository();
        this.aasController = new AasController(monitor, aasDataProcessorFactory);
        var endpoint = new Endpoint(selfDescriptionRepository, this.aasController, configurationController, monitor);

        // Initialize/Start synchronizer, start AAS services defined in configuration
        initializeSynchronizer(selfDescriptionRepository);
        // This makes the connector shutdown if an exception occurs while starting config services
        registerServicesByConfig(selfDescriptionRepository);

        // Add public endpoint if wanted by config
        if (Configuration.getInstance().isExposeSelfDescription()) {
            publicApiManagementService.addEndpoints(List.of(new de.fraunhofer.iosb.api.model.Endpoint(Endpoint.SELF_DESCRIPTION_PATH, HttpMethod.GET, Map.of())));
        }

        webService.registerResource(endpoint);
    }

    private void registerServicesByConfig(SelfDescriptionRepository selfDescriptionRepository) {
        var configInstance = Configuration.getInstance();

        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            selfDescriptionRepository.createSelfDescription(configInstance.getRemoteAasLocation());
        }

        if (Objects.isNull(configInstance.getLocalAasModelPath())) {
            return;
        }

        Path aasConfigPath = null;

        if (Objects.nonNull(configInstance.getAasServiceConfigPath())) {
            aasConfigPath = Path.of(configInstance.getAasServiceConfigPath());
        }

        URL serviceUrl;
        try {
            serviceUrl = aasController.startService(
                    Path.of(configInstance.getLocalAasModelPath()),
                    configInstance.getLocalAasServicePort(),
                    aasConfigPath);

        } catch (IOException startAssetAdministrationShellException) {
            throw new EdcException("Could not start AAS service provided by configuration", startAssetAdministrationShellException);
        }

        selfDescriptionRepository.createSelfDescription(serviceUrl);
    }

    private void initializeSynchronizer(SelfDescriptionRepository selfDescriptionRepository) {
        var synchronizer = new Synchronizer(selfDescriptionRepository, aasController,
                new ResourceController(assetIndex, contractStore, policyStore, monitor),
                aasServiceRegistry);
        selfDescriptionRepository.registerListener(synchronizer);

        // Task: get all AAS service URLs, synchronize EDC and AAS
        syncExecutor = new ScheduledThreadPoolExecutor(1);
        syncExecutor.scheduleAtFixedRate(synchronizer::synchronize, 1,
                Configuration.getInstance().getSyncPeriod(), TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        monitor.info("Shutting down EDC4AAS extension...");
        if (syncExecutor != null) {
            syncExecutor.shutdown();
        }
        aasController.stopServices();
    }
}
