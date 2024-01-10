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
package de.fraunhofer.iosb.app.model.aas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.adminshell.aas.v3.model.EmbeddedDataSpecification;

/*
 * Collect common attributes of every AAS element.
 */
public class AASElement extends IdsAssetElement {

    protected String idShort;
    
    @JsonProperty("semanticId")
    protected CustomSemanticId customSemanticId;

    protected List<EmbeddedDataSpecification> embeddedDataSpecifications;
    
    public String getIdShort() {
        return idShort;
    }

    public void setIdShort(String idShort) {
        this.idShort = idShort;
    }

    public CustomSemanticId getCustomSemanticId() {
        return customSemanticId;
    }

    public void setSemanticId(CustomSemanticId semanticId) {
        this.customSemanticId = semanticId;
    }

    public List<EmbeddedDataSpecification> getEmbeddedDataSpecifications() {
        return embeddedDataSpecifications;
    }

    public void setEmbeddedDataSpecifications(List<EmbeddedDataSpecification> embeddedDataSpecifications) {
        this.embeddedDataSpecifications = embeddedDataSpecifications;
    }
}
