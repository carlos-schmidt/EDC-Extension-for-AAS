package de.fraunhofer.iosb.app.model.aas.util;

import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.SubmodelElement;
import io.adminshell.aas.v3.model.SubmodelElementCollection;

import java.util.ArrayList;
import java.util.Collection;

public final class SubmodelUtil {
    
    private SubmodelUtil() {
    }

    /**
     * Get Custom Submodel Elements with Structure From Submodel

     * @param submodel Submodel whose elements are to be returned in a flat list
     * @return flat list of submodels elements
     */
    public static Collection<CustomSubmodelElement> getCustomSubmodelElementStructureWithUrlsFromSubmodel(
            Submodel submodel) {
        return unpackElements(submodel.getSubmodelElements());
    }

    /**
     * Make structure of submodelElements inside this submodel flat and return list
     * of them

     * @param submodel Submodel whose elements are to be returned in a flat list
     * @return flat list of submodels elements
     */
    public static Collection<CustomSubmodelElement> getAllSubmodelElements(CustomSubmodel submodel) {
        return flattenElements(new ArrayList<CustomSubmodelElement>(), submodel.getSubmodelElements());
    }

    /**
     * Recursive unpacking of submodelElementCollections + their contents into
     * custom submodel. Also creates relative URLs.
     */
    private static Collection<CustomSubmodelElement> unpackElements(Collection<SubmodelElement> submodelElements) {
        Collection<CustomSubmodelElement> customSubmodelElements = new ArrayList<>();
        for (SubmodelElement submodelElement : submodelElements) {
            if (submodelElement instanceof SubmodelElementCollection) {
                var customSubmodelElementCollection = new CustomSubmodelElementCollection();
                customSubmodelElementCollection.setIdShort(submodelElement.getIdShort());
                customSubmodelElementCollection
                        .setValues(unpackElements(((SubmodelElementCollection) submodelElement).getValues()));
                customSubmodelElements.add(customSubmodelElementCollection);
            } else {
                var customSubmodelElement = new CustomSubmodelElement();
                customSubmodelElement.setIdShort(submodelElement.getIdShort());
                customSubmodelElements.add(customSubmodelElement);
            }
        }
        return customSubmodelElements;
    }

    /**
     * Recursive unpacking of submodelElementCollections + their contents into
     * flat structure.
     */
    private static Collection<CustomSubmodelElement> flattenElements(Collection<CustomSubmodelElement> flatList,
            Collection<CustomSubmodelElement> submodelElements) {

        for (CustomSubmodelElement submodelElement : submodelElements) {

            if (submodelElement instanceof CustomSubmodelElementCollection) {
                flattenElements(new ArrayList<CustomSubmodelElement>(),
                        ((CustomSubmodelElementCollection) submodelElement).getValue())
                        .forEach(flatList::add);
            }

            flatList.add(submodelElement);
        }

        return flatList;
    }
}
