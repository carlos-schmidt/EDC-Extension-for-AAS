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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.app.model.aas.service.Service;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.ssl.impl.NoOpSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import static de.fraunhofer.iosb.api.model.HttpMethod.GET;
import static de.fraunhofer.iosb.app.model.aas.service.Service.CONCEPT_DESCRIPTIONS_PATH;
import static de.fraunhofer.iosb.app.model.aas.service.Service.SHELLS_PATH;
import static de.fraunhofer.iosb.app.model.aas.service.Service.SUBMODELS_PATH;
import static de.fraunhofer.iosb.app.pipeline.PipelineFailure.Type.WARNING;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEnvironment;
import static de.fraunhofer.iosb.app.testutils.StringMethods.resultOfCollection;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

class ServiceAgentTest {

    private static final int PORT = getFreePort();
    private final URL mockServerUrl = new URL("http://localhost:%s".formatted(PORT));
    private static ClientAndServer mockServer;

    private static ServiceAgent testSubject;

    ServiceAgentTest() throws MalformedURLException {
    }

    @BeforeAll
    static void setUp() {
        testSubject = new ServiceAgent(
                new AllAasDataProcessorFactory(
                        new NoOpSelfSignedCertificateRetriever(),
                        new OkHttpClient(),
                        RetryPolicy.ofDefaults(),
                        new ConsoleMonitor()
                ));

        mockServer = startClientAndServer(PORT);
    }

    @AfterEach
    void tearDown() {
        mockServer.reset();
    }

    @AfterAll
    static void shutdown() {
        mockServer.stop();
    }

    @Test
    void testApplyEmptyEnvironment() {
        var emptyEnvironment = getEmptyEnvironment();
        answerWith(emptyEnvironment);

        PipelineResult<Environment> result = testSubject.apply(new Service(mockServerUrl));

        assertTrue(result.succeeded());
        assertNotNull(result.getContent());

        var resultEnvironment = result.getContent();

        assertEquals(emptyEnvironment, resultEnvironment);
    }

    @Test
    void testApplyEnvironment() {
        var environment = getEnvironment();
        answerWith(environment);

        PipelineResult<Environment> result = testSubject.apply(new Service(mockServerUrl));

        assertTrue(result.succeeded());
        assertNotNull(result.getContent());
        var resultEnvironment = result.getContent();
        assertEquals(environment, resultEnvironment);
    }

    @Test
    void testApplyUnknownHost() throws MalformedURLException {
        var result = testSubject.apply(new Service(new URL("http://anonymous.invalid")));

        assertTrue(result.failed());
        assertEquals(WARNING, result.getFailure().getFailureType());
        assertTrue(result.getFailureDetail().contains(UnknownHostException.class.getSimpleName()));
    }

    @Test
    void testApplyUnreachable() throws MalformedURLException {
        var result = testSubject.apply(new Service(new URL("http://localhost:" + getFreePort())));

        assertTrue(result.failed());
        assertEquals(WARNING, result.getFailure().getFailureType());
        // This throws a connectionException
    }

    @Test
    void testApplyNotActuallyService() {
        // Here, mock server returns no valid response (it is not an AAS service)
        var result = testSubject.apply(new Service(mockServerUrl));

        assertTrue(result.failed());
        assertEquals(WARNING, result.getFailure().getFailureType());
    }

    private void answerWith(Environment environment) {
        try {
            mockServer.when(request()
                            .withMethod(GET.toString())
                            .withPath(SHELLS_PATH))
                    .respond(HttpResponse.response()
                            .withBody(resultOfCollection(environment.getAssetAdministrationShells())));

            mockServer.when(request()
                            .withMethod(GET.toString())
                            .withPath(SUBMODELS_PATH))
                    .respond(HttpResponse.response()
                            .withBody(resultOfCollection(environment.getSubmodels())));

            mockServer.when(request()
                            .withMethod(GET.toString())
                            .withPath(CONCEPT_DESCRIPTIONS_PATH))
                    .respond(HttpResponse.response()
                            .withBody(resultOfCollection(environment.getConceptDescriptions())));

        } catch (SerializationException e) {
            fail("Failed setting up mock response: %s".formatted(e.getMessage()));
        }
    }
}