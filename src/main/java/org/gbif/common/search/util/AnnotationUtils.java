package org.gbif.common.search.util;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.Key;
import org.gbif.common.search.model.SearchMapping;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.solr.client.solrj.beans.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for search annotations handling.
 */
public class AnnotationUtils {

  private static final Logger LOG = LoggerFactory.getLogger(AnnotationUtils.class);

  /**
   * Private default/hidden constructor.
   */
  private AnnotationUtils() {
    // empty block
  }

  /**
   * Gets the first method annotated with the annotation parameter in the input clazz.
   *
   * @param clazz      to be analyzed
   * @param annotation to be searched
   */
  public static Method getAnnotatedMethod(Class<?> clazz, Class<? extends Annotation> annotation) {
    for (Method method : clazz.getMethods()) {
      if (method.isAnnotationPresent(annotation)) {
        return method;
      }
    }
    return null;
  }

  /**
   * Gets the name of the java property annotated with {@link Key}.
   *
   * @return name of the key field, null if no key field is defined.
   */
  public static String getKeyField(Class<?> annotatedClass) {
    Method method = getAnnotatedMethod(annotatedClass, Key.class);
    if (method != null) {
      for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(annotatedClass)) {
        Method writeMethod = descriptor.getWriteMethod();
        // PropertyDescriptor is not used directly to get the annotated method because a bug BeanUtils class.
        if (writeMethod != null && writeMethod.getName().equals(method.getName())) {
          return descriptor.getName();
        }
      }
    }
    LOG.error("Key field not found in class {}", annotatedClass.getName());
    return null;
  }


  /**
   * Helper method for initializing the map of SolrFields <-> Facet definitions using the Field Solr annotations.
   *
   * @param annotatedClass that contains the Solr @Field annotation.
   *
   * @return a map with SolrFieldName <-> FieldFacet definition.
   */
  public static Map<String, FacetField> initFacetFieldDefs(Class<?> annotatedClass) {
    if (annotatedClass.isAnnotationPresent(SearchMapping.class)) {
      FacetField[] facetFields = annotatedClass.getAnnotation(SearchMapping.class).facets();
      Map<String, FacetField> fieldDefs = Maps.newHashMapWithExpectedSize(facetFields.length);
      for (FacetField f : facetFields) {
        fieldDefs.put(f.field(), f);
      }
      return fieldDefs;
    }
    return Maps.newHashMap();
  }


  /**
   * Helper method for initializing the map of SolrFields <-> Java fields mapped using an annotation type.
   * The facet names are uppercased to allow case insensitive faceted parameters.
   * Facet fields are case sensitive.
   */
  public static <P extends Enum<?> & SearchParameter> BiMap<String, P> initFacetFieldsPropertiesMap(
      Class<?> annotatedClass, Class<P> enumClass) {
    BiMap<String, P> fieldsPropertiesMap = HashBiMap.create();

    if (annotatedClass.isAnnotationPresent(SearchMapping.class)) {
      for (FacetField annotation : annotatedClass.getAnnotation(SearchMapping.class).facets()) {
        fieldsPropertiesMap.put(annotation.field(), VocabularyUtils.lookupEnum(annotation.name(), enumClass));
      }
    }
    for (Method method : annotatedClass.getMethods()) {
      if (method.isAnnotationPresent(FacetField.class)) {
        FacetField annotation = method.getAnnotation(FacetField.class);
        fieldsPropertiesMap.put(annotation.field(), VocabularyUtils.lookupEnum(annotation.name(), enumClass));
      }
    }
    return fieldsPropertiesMap;
  }

  /**
   * Helper method for initializing the map of SolrFields <-> Java fields mapped using the Field Solr annotation
   * found on setter methods.
   *
   * @param annotatedClass that contains the Solr @Field annotation on setters
   *
   * @return a map with SolrFieldName <-> PropertyName.
   */
  public static BiMap<String, String> initFieldsPropertiesMap(Class<?> annotatedClass) {
    BiMap<String, String> fieldsPropertiesMap = HashBiMap.create();

    // we iterate over all methods and look for our custom annotations
    Method[] methods = annotatedClass.getMethods();
    for (Method m : methods) {
      if (m.isAnnotationPresent(Field.class)) {
        // they should be on setters only
        Field annotation = m.getAnnotation(Field.class);
        String solrFieldName = annotation.value();
        // remove set and lowercase first char
        String propertyName = Character.toLowerCase(m.getName().substring(3).charAt(0)) +
                              m.getName().substring(4);
        fieldsPropertiesMap.put(solrFieldName, propertyName);
      }
    }

    return fieldsPropertiesMap;
  }

  /**
   * Helper method for initializing the map of SolrFields <-> Java fields mapped using an annotation type.
   */
  public static List<FullTextSearchField> initFullTextFieldsPropertiesMap(Class<?> annotatedClass) {
    List<FullTextSearchField> fullTextSearchFields = new ArrayList<FullTextSearchField>();
    if (annotatedClass.isAnnotationPresent(SearchMapping.class)) {
      fullTextSearchFields.addAll(Arrays.asList(annotatedClass.getAnnotation(SearchMapping.class).fulltextFields()));
    }
    for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(annotatedClass)) {
      Method writeMethod = descriptor.getWriteMethod();
      if (writeMethod != null && writeMethod.isAnnotationPresent(FullTextSearchField.class)) {
        fullTextSearchFields.add(writeMethod.getAnnotation(FullTextSearchField.class));
      }
    }
    return fullTextSearchFields;
  }

}
