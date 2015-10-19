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
  public static Method getAnnotatedMethod(final Class<?> clazz, final Class<? extends Annotation> annotation) {
    for (final Method method : clazz.getMethods()) {
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
  public static String getKeyField(final Class<?> anotatedClass) {
    final Method method = getAnnotatedMethod(anotatedClass, Key.class);
    if (method != null) {
      for (final PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(anotatedClass)) {
        final Method writeMethod = descriptor.getWriteMethod();
        // PropertyDescriptor is not used directly to get the annotated method because a bug Beanutils class.
        if (writeMethod != null && writeMethod.getName().equals(method.getName())) {
          return descriptor.getName();
        }
      }
    }
    LOG.error("Key field not found in class {}", anotatedClass.getName());
    return null;
  }


  /**
   * Helper method for initializing the map of SolrFields <-> Facet definitions using the Field Solr annotations.
   *
   * @param annotatedClass that contains the Solr @Field annotation.
   *
   * @return a map with SolrFieldName <-> FieldFacet definition.
   */
  public static Map<String, FacetField> initFacetFieldDefs(final Class<?> annotatedClass) {
    final Map<String, FacetField> fieldDefs = Maps.newHashMap();
    if (annotatedClass.isAnnotationPresent(SearchMapping.class)) {
      for (final FacetField f : annotatedClass.getAnnotation(SearchMapping.class).facets()) {
        fieldDefs.put(f.field(), f);
      }
    }
    return fieldDefs;
  }


  /**
   * Helper method for initializing the map of SolrFields <-> Java fields mapped using an annotation type.
   * The facet names are uppercased to allow case insensitive faceted parameters.
   * Facet fields are case sensitive.
   */
  public static <P extends Enum<?> & SearchParameter> BiMap<String, P> initFacetFieldsPropertiesMap(
      final Class<?> annotatedClass, final Class<P> enumClass) {
    BiMap<String, P> fieldsPropertiesMap = HashBiMap.create();

    if (annotatedClass.isAnnotationPresent(SearchMapping.class)) {
      for (final FacetField annotation : annotatedClass.getAnnotation(SearchMapping.class).facets()) {
        fieldsPropertiesMap.put(annotation.field(), VocabularyUtils.lookupEnum(annotation.name(), enumClass));
      }
    }
    for (final Method method : annotatedClass.getMethods()) {
      if (method.isAnnotationPresent(FacetField.class)) {
        final FacetField annotation = method.getAnnotation(FacetField.class);
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
  public static BiMap<String, String> initFieldsPropertiesMap(final Class<?> annotatedClass) {
    BiMap<String, String> fieldsPropertiesMap = HashBiMap.create();

    // we iterate over all methods and look for our custom annotations
    final Method[] methods = annotatedClass.getMethods();
    for (final Method m : methods) {
      if (m.isAnnotationPresent(Field.class)) {
        // they should be on setters only
        final Field annotation = m.getAnnotation(Field.class);
        final String solrFieldName = annotation.value();
        // remove set and lowercase first char
        final String propertyName =
            new StringBuilder().append(Character.toLowerCase(m.getName().substring(3).charAt(0)))
            .append(m.getName().substring(4)).toString();
        fieldsPropertiesMap.put(solrFieldName, propertyName);
      }
    }

    return fieldsPropertiesMap;
  }

  /**
   * Helper method for initializing the map of SolrFields <-> Java fields mapped using an annotation type.
   */
  public static List<FullTextSearchField> initFullTextFieldsPropertiesMap(final Class<?> annotatedClass) {
    List<FullTextSearchField> fullTextSearchFields = new ArrayList<FullTextSearchField>();
    if (annotatedClass.isAnnotationPresent(SearchMapping.class)) {
      fullTextSearchFields.addAll(Arrays.asList(annotatedClass.getAnnotation(SearchMapping.class).fulltextFields()));
    }
    for (final PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(annotatedClass)) {
      final Method writeMethod = descriptor.getWriteMethod();
      if (writeMethod != null && writeMethod.isAnnotationPresent(FullTextSearchField.class)) {
        fullTextSearchFields.add(writeMethod.getAnnotation(FullTextSearchField.class));
      }
    }
    return fullTextSearchFields;
  }

}
