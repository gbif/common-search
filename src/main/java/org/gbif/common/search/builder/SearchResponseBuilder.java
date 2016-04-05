/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.common.search.builder;


import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.common.search.exception.SearchException;
import org.gbif.common.search.model.HighlightableList;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.common.search.util.AnnotationUtils.getKeyField;
import static org.gbif.common.search.util.AnnotationUtils.initFacetFieldsPropertiesMap;
import static org.gbif.common.search.util.AnnotationUtils.initFieldsPropertiesMap;
import static org.gbif.common.search.util.SolrConstants.HL_POST;
import static org.gbif.common.search.util.SolrConstants.HL_PRE;
import static org.gbif.common.search.util.SolrConstants.HL_PRE_REGEX;


/**
 * Builder class that helps in the creation process of {@link SearchResponse} objects from
 * {@link org.apache.solr.client.solrj.response.QueryResponse} objects.
 * The build method is not thread safe, supports the cloneable interface for allowing holds a partial state,
 * that can be cloned and repeated in other instances.
 *
 * @param <T> is the response type.
 * @param <ST> is the type of the annotated class that holds the mapping information.
 * @param <P> is the search parameter enumeration.
 */
public class SearchResponseBuilder<T, ST extends T, P extends Enum<?> & SearchParameter> {

  // Logger
  private static final Logger LOG = LoggerFactory.getLogger(SearchResponseBuilder.class);

  // Holds a map containing the solr fields vs. application facet names
  private final BiMap<String, P> solrField2ParamEnumMap;

  // Holds a map containing the solr fields vs. java fields names
  private final BiMap<String, String> solrField2javaPropertiesMap;

  // Holds a map containing the highlightable solr fields vs. java fields names
  private Map<String, String> hlFieldPropertyPropertiesMap;

  // Name of the field annotated with the Key annotation.
  private final String keyField;


  // Class that contains the annotations for creating the response objects.
  private final Class<T> responseClass;
  private final Class<ST> annotatedClass;

  /**
   * Private full constructor of final fields only.
   */
  private SearchResponseBuilder(Class<T> responseClass, Class<ST> annotatedClass,
                                BiMap<String, P> solrField2ParamEnumMap,
                                BiMap<String, String> solrField2javaPropertiesMap) {
    this.responseClass = responseClass;
    this.annotatedClass = annotatedClass;
    this.solrField2ParamEnumMap = solrField2ParamEnumMap;
    this.solrField2javaPropertiesMap = solrField2javaPropertiesMap;
    keyField = getKeyField(annotatedClass);
  }

  /**
   * Private default constructor.
   */
  private SearchResponseBuilder(Class<T> responseClass, Class<ST> annotatedClass, Class<P> enumClass) {
    this.responseClass = responseClass;
    this.annotatedClass = annotatedClass;
    solrField2ParamEnumMap = initFacetFieldsPropertiesMap(annotatedClass, enumClass);
    solrField2javaPropertiesMap = initFieldsPropertiesMap(annotatedClass);
    keyField = getKeyField(annotatedClass);
  }


  /**
   * Factory method that reuses a facets mapping map and the default factory method.
   *
   * @param annotatedClass that contains search related mappings and is the result the of the response
   *        content.
   * @param facetFieldsPropertiesMap map containing facet fields names <-> solr fields names.
   * @return a new instance of a SearchResponseBuilder
   */
  public static <T, ST extends T, P extends Enum<?> & SearchParameter> SearchResponseBuilder<T, ST, P> create(
      Class<T> responseClass, Class<ST> annotatedClass, BiMap<String, P> facetFieldsPropertiesMap,
      BiMap<String, String> fieldPropertyPropertiesMap) {

    return new SearchResponseBuilder<T, ST, P>(responseClass, annotatedClass, facetFieldsPropertiesMap,
      fieldPropertyPropertiesMap);
  }

  /**
   * Default factory method.
   *
   * @param annotatedClass that contains search related mappings and is the result the of the response content.
   * @return a new instance of a SearchResponseBuilder
   */
  public static <T, ST extends T, P extends Enum<?> & SearchParameter> SearchResponseBuilder<T, ST, P> create(
      Class<T> responseClass, Class<ST> annotatedClass, Class<P> enumClass) {
    return new SearchResponseBuilder<T, ST, P>(responseClass, annotatedClass, enumClass);
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<T, P> build(Pageable searchRequest, QueryResponse queryResponse) {
    // Create response
    SearchResponse<T, P> searchResponse = new SearchResponse<T, P>(searchRequest);
    searchResponse.setCount(queryResponse.getResults().getNumFound());
    searchResponse.setLimit(queryResponse.getResults().size());
    // The results and facets are copied into the response
    final List<ST> resultsST = queryResponse.getBeans(annotatedClass);
    // convert types
    final List<T> results = Lists.newArrayList();
    for (ST x : resultsST) {
      results.add(x);
    }
    searchResponse.setResults(results);
    searchResponse.setFacets(getFacetsFromResponse(queryResponse));
    setHighlightedFields(searchResponse, queryResponse);
    if(queryResponse.getSpellCheckResponse() != null){
      searchResponse.setSpellCheckResponse(SpellCheckResponseBuilder.build(queryResponse.getSpellCheckResponse()));
    }
    return searchResponse;
  }

  /**
   * Builds a simple SearchResponse instance using the current builder state, facets and highlighting reponses are
   * ommited.
   *
   * @return a new instance of a SearchResponse.
   */
  public static <T, ST extends T, P extends SearchParameter> SearchResponse<T, P> buildSuggestReponse(
      Pageable searchRequest, QueryResponse queryResponse, Class<ST> annotatedClass) {
    // Create response
    SearchResponse<T, P> searchResponse = new SearchResponse<T, P>(searchRequest);
    searchResponse.setCount(queryResponse.getResults().getNumFound());
    searchResponse.setLimit(queryResponse.getResults().size());
    // The results and facets are copied into the response
    final List<ST> resultsST = queryResponse.getBeans(annotatedClass);
    // convert types
    final List<T> results = Lists.newArrayList();
    for (ST x : resultsST) {
      results.add(x);
    }
    searchResponse.setResults(results);
    return searchResponse;
  }


  /**
   * Copies the object's content into a new object.
   */
  public SearchResponseBuilder<T, ST, P> getCopy() {
    SearchResponseBuilder<T, ST, P> searchResponseBuilder =
      new SearchResponseBuilder<T, ST, P>(responseClass, annotatedClass, solrField2ParamEnumMap,
        solrField2javaPropertiesMap);
    searchResponseBuilder.hlFieldPropertyPropertiesMap = hlFieldPropertyPropertiesMap;
    return searchResponseBuilder;
  }


  /**
   * Sets the list of highlighted fields.
   */
  public SearchResponseBuilder<T, ST, P> withHighlightFields(List<String> highlightFields) {
    hlFieldPropertyPropertiesMap = new HashMap<String, String>();
    for (String hlField : highlightFields) {
      if (solrField2javaPropertiesMap.containsKey(hlField)) {
        hlFieldPropertyPropertiesMap.put(hlField, solrField2javaPropertiesMap.get(hlField));
      }
    }
    return this;
  }

  /**
   * Cleans all occurrences of highlighted tags/marks in the parameter and returns an new instance clean of those
   * marks.
   */
  private static String cleanHighlightingMarks(final String hlText) {
    String hlLiteral = hlText;
    int indexPre = hlLiteral.indexOf(HL_PRE);
    while (indexPre > -1) {
      int indexPost = hlLiteral.indexOf(HL_POST, indexPre + HL_PRE.length());
      if (indexPost > -1) {
        String post = hlLiteral.substring(indexPost + HL_POST.length());
        String pre = hlLiteral.substring(0, indexPost);
        Matcher preMatcher = HL_PRE_REGEX.matcher(pre);
        pre = preMatcher.replaceFirst("");
        hlLiteral = pre + post;
      }
      indexPre = hlLiteral.indexOf(HL_PRE);
    }
    return hlLiteral;
  }


  private T getByKey(SearchResponse<T, P> response, String key) {
    for (T bean : response.getResults()) {
      try {
        String value = BeanUtils.getProperty(bean, keyField);
        if (value.equals(key)) {
          return bean;
        }
      } catch (IllegalAccessException e) {
        LOG.error("Error accesing key field", e);
        throw new SearchException(e);
      } catch (InvocationTargetException e) {
        LOG.error("Error invoking set key field", e);
        throw new SearchException(e);
      } catch (NoSuchMethodException e) {
        LOG.error("Error invoking setter method", e);
        throw new SearchException(e);
      }
    }
    return null;
  }

  /**
   * Helper method that takes Solr response and extracts the facets results.
   * The facets are converted to a list of Facets understood by the search API.
   * The result of this method can be a empty list.
   *
   * @param queryResponse that contains the facets information returned by Solr
   * @return the List of facets retrieved from the Solr response
   */
  private List<Facet<P>> getFacetsFromResponse(final QueryResponse queryResponse) {
    List<Facet<P>> facets = Lists.newArrayList();
    if (queryResponse.getFacetFields() != null) {
      for (final FacetField facetField : queryResponse.getFacetFields()) {
        P facetParam = solrField2ParamEnumMap.get(facetField.getName());
        Facet<P> facet = new Facet<P>(facetParam);

        List<Facet.Count> counts = Lists.newArrayList();
        if (facetField.getValues() != null) {
          for (final FacetField.Count count : facetField.getValues()) {
            String value = count.getName();
            if (Enum.class.isAssignableFrom(facetParam.type())) {
              value = getFacetEnumValue(facetParam, value);
            }
            counts.add(new Facet.Count(value, count.getCount()));
          }
        }
        facet.setCounts(counts);
        facets.add(facet);
      }
    }
    return facets;
  }


  /**
   * Gets the facet value of Enum type parameter.
   * If the Enum is either a Country or a Language, its iso2Letter code it's used.
   */
  private String getFacetEnumValue(P facetParam, String value) {
    // the expected enum type for the value if it is an enum - otherwise null
    final Enum<?>[] enumValues = ((Class<? extends Enum<?>>) facetParam.type()).getEnumConstants();
    // if we find integers these are ordinals, translate back to enum names
    final Integer intValue = Ints.tryParse(value);
    if (intValue != null) {
      final Enum<?> enumValue = enumValues[intValue];
      if (Country.class.equals(facetParam.type())) {
        return ((Country) enumValue).getIso2LetterCode();
      } else if (Language.class.equals(facetParam.type())) {
        return ((Language) enumValue).getIso2LetterCode();
      } else {
        return enumValue.name();
      }
    } else {
      if (Country.class.equals(facetParam.type())) {
        return Country.fromIsoCode(value).getIso2LetterCode();
      } else if (Language.class.equals(facetParam.type())) {
        return Language.fromIsoCode(value).getIso2LetterCode();
      } else {
        return VocabularyUtils.lookupEnum(value, (Class<? extends Enum<?>>) facetParam.type()).name();
      }
    }
  }

  /**
   * Takes the highlighted fields form solrResponse and copies them to the response object.
   *
   * @param response to set the highlighted fields.
   * @param solrResponse to extract the highlighting information
   */
  private void setHighlightedFields(SearchResponse<T, P> response, final QueryResponse solrResponse) {
    if ((hlFieldPropertyPropertiesMap != null) && (solrResponse.getHighlighting() != null) && !solrResponse
      .getHighlighting().isEmpty()) {
      for (String docId : solrResponse.getHighlighting().keySet()) {
        setHighlightedFieldValues(response, solrResponse, docId);
      }
    }
  }

  /**
   * Sets the value of the highlighted field in object 'bean'.
   */
  private void setHighlightedFieldValue(T bean, String solrField, String hlValue) {
    try {
      if (List.class
        .isAssignableFrom(PropertyUtils.getPropertyType(bean, solrField2javaPropertiesMap.get(solrField)))) {
        List<String> listProperty =
          (List<String>) PropertyUtils.getProperty(bean, solrField2javaPropertiesMap.get(solrField));
        // Cleans the hl markers
        String hlCleanValue = cleanHighlightingMarks(hlValue);
        // Position of the value
        int hlValueIndex = listProperty.indexOf(hlCleanValue);
        if (hlValueIndex != -1) { // replace the value with the highlighted value
          listProperty.remove(hlValueIndex);
          listProperty.add(hlValueIndex, hlValue);
        }

      } else if (HighlightableList.class
        .isAssignableFrom(PropertyUtils.getPropertyType(bean, solrField2javaPropertiesMap.get(solrField)))) {
        HighlightableList property =
          (HighlightableList) PropertyUtils.getProperty(bean, solrField2javaPropertiesMap.get(solrField));
        // Cleans the hl markers
        String hlCleanValue = cleanHighlightingMarks(hlValue);
        // Position of the value
        List<String> values = property.getValues();
        int hlValueIndex = values.indexOf(hlCleanValue);
        if (hlValueIndex != -1) { // replace the value with the highlighted value
          property.replaceValue(hlValueIndex, hlValue);
        }

      } else {
        String propertyName = solrField2javaPropertiesMap.get(solrField);
        if (PropertyUtils.getPropertyDescriptor(bean, propertyName).getPropertyType().isAssignableFrom(String.class)) {
          BeanUtils.setProperty(bean, propertyName, hlValue);
        }
      }
    } catch (IllegalAccessException e) {
      LOG.error("Error accessing field for setting highlighted value", e);
      throw new SearchException(e);
    } catch (InvocationTargetException e) {
      LOG.error("Error setting highlighted value", e);
      throw new SearchException(e);
    } catch (NoSuchMethodException e) {
      LOG.error("Error invoking method to set highlighted value", e);
      throw new SearchException(e);
    }
  }

  /**
   * Sets the highlighted field values of beans in the response object.
   */
  private void setHighlightedFieldValues(SearchResponse<T, P> response, final QueryResponse solrResponse,
    String docId) {
    T bean = getByKey(response, docId);
    if (bean != null) {
      Map<String, List<String>> docHighlights = solrResponse.getHighlighting().get(docId);
      for (String solrField : hlFieldPropertyPropertiesMap.keySet()) {
        if (docHighlights.containsKey(solrField)) {
          for (String hlValue : docHighlights.get(solrField)) { // get each snippet
            setHighlightedFieldValue(bean, solrField, hlValue);
          }
        }
      }
    }
  }

}
