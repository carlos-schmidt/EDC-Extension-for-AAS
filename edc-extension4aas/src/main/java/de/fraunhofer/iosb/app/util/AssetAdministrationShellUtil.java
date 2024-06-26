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
package de.fraunhofer.iosb.app.util;

import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementList;
import de.fraunhofer.iosb.app.model.aas.IdsAssetElement;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class AssetAdministrationShellUtil {

    private AssetAdministrationShellUtil() {
    }

    /**
     * Get Custom Submodel Elements with Structure From Submodel
     *
     * @param submodel Submodel whose elements are to be returned in a flat list
     * @return flat list of submodels elements
     */
    public static Collection<CustomSubmodelElement> getCustomSubmodelElementStructureFromSubmodel(
            Submodel submodel) {
        Objects.requireNonNull(submodel);
        return unpackElements(submodel.getSubmodelElements());
    }

    /**
     * Returns all AAS elements in a flattened list format.
     *
     * @param env AAS environment with elements in structure
     * @return Flat list of IdsAssetElements from given environment
     */
    public static List<? extends IdsAssetElement> getAllElements(CustomAssetAdministrationShellEnvironment env) {
        return Stream.of(env.getConceptDescriptions(),
                        env.getAssetAdministrationShells(),
                        env.getSubmodels(),
                        env.getSubmodels()
                                .stream()
                                .map(AssetAdministrationShellUtil::getAllSubmodelElements)
                                .flatMap(Collection::stream)
                                .toList())
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * Make structure of submodelElements inside this submodel flat and return list
     * of them
     *
     * @param submodel Submodel whose elements are to be returned in a flat list
     * @return flat list of submodels elements
     */
    public static Collection<CustomSubmodelElement> getAllSubmodelElements(CustomSubmodel submodel) {
        Objects.requireNonNull(submodel);
        return flattenElements(new ArrayList<>(), submodel.getSubmodelElements());
    }

    /**
     * Recursive unpacking of submodelElementCollections + their contents into
     * custom submodel.
     */
    private static Collection<CustomSubmodelElement> unpackElements(Collection<SubmodelElement> submodelElements) {
        if (submodelElements == null) {
            return new ArrayList<>(); //empty collection
        }

        Collection<CustomSubmodelElement> customSubmodelElements = new ArrayList<>();
        for (SubmodelElement submodelElement : submodelElements) {
            CustomSubmodelElement toAdd;

            if (submodelElement instanceof SubmodelElementCollection) {
                toAdd = new CustomSubmodelElementCollection(
                        submodelElement.getIdShort(),
                        unpackElements(((SubmodelElementCollection) submodelElement).getValue()));
            } else if (submodelElement instanceof SubmodelElementList) {
                toAdd = new CustomSubmodelElementCollection(
                        submodelElement.getIdShort(),
                        unpackElements(((SubmodelElementList) submodelElement).getValue()));
            } else {
                toAdd = new CustomSubmodelElement();
                toAdd.setIdShort(submodelElement.getIdShort());
                toAdd.setSemanticId(submodelElement.getSemanticId());
            }
            customSubmodelElements.add(toAdd);

        }
        return customSubmodelElements;
    }

    /**
     * Recursive unpacking of submodelElementCollections + their contents into
     * flat structure.
     */
    private static Collection<CustomSubmodelElement> flattenElements(Collection<CustomSubmodelElement> flatList,
                                                                     Collection<CustomSubmodelElement> submodelElements) {
        if (Objects.isNull(submodelElements)) {
            return flatList;
        }
        for (CustomSubmodelElement submodelElement : submodelElements) {

            if (submodelElement instanceof CustomSubmodelElementCollection) {
                flatList.addAll(flattenElements(new ArrayList<>(),
                        ((CustomSubmodelElementCollection) submodelElement).getValue()));
            } else if (submodelElement instanceof CustomSubmodelElementList) {
                flatList.addAll(flattenElements(new ArrayList<>(),
                        ((CustomSubmodelElementList) submodelElement).getValue()));
            }

            flatList.add(submodelElement);
        }

        return flatList;
    }
}
