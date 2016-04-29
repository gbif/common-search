package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.configuration.FacetFieldConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.common.search.util.QueryUtils.PARAMS_JOINER;
import static org.gbif.common.search.util.QueryUtils.PARAMS_OR_JOINER;
import static org.gbif.common.search.util.QueryUtils.isNegated;
import static org.gbif.common.search.util.QueryUtils.removeNegation;
import static org.gbif.common.search.util.QueryUtils.setFacetMethod;
import static org.gbif.common.search.util.SolrConstants.APOSTROPHE;
import static org.gbif.common.search.util.SolrConstants.FACET_FILTER_EX;
import static org.gbif.common.search.util.SolrConstants.FACET_FILTER_TAG;
import static org.gbif.common.search.util.SolrConstants.NOT_OP;
import static org.gbif.common.search.util.SolrConstants.PARAM_FACET_MISSING;
import static org.gbif.common.search.util.SolrConstants.PARAM_FACET_SORT;
import static org.gbif.common.search.util.SolrConstants.TAG_FIELD_PARAM;

/**
 * Utility class to perform transformations from API to Solr queries and vice versa.
 */
public class SolrQueryUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SolrQueryUtils.class);
  public static final FacetField.SortOrder DEFAULT_FACET_SORT = FacetField.SortOrder.COUNT;
  public static final int DEFAULT_FACET_COUNT = 1;
  public static final boolean DEFAULT_FACET_MISSING = true;
  private static final Pattern TAG_FIELD_PARAM_PATTERN = Pattern.compile(TAG_FIELD_PARAM, Pattern.LITERAL);

  /**
   * Helper method that sets the parameter for a faceted query.
   *
   * @param searchRequest the searchRequest used to extract the parameters
   * @param solrQuery this object is modified by adding the facets parameters
   */
  public static <P extends SearchParameter> void applyFacetSettings(FacetedSearchRequest<P> searchRequest, SolrQuery solrQuery, Map<P,FacetFieldConfiguration> configurations) {


    if (!searchRequest.getFacets().isEmpty()) {
      // Only show facets that contains at least 1 record
      solrQuery.setFacet(true);
      // defaults if not overridden on per field basis
      solrQuery.setFacetMinCount(MoreObjects.firstNonNull(searchRequest.getFacetMinCount(), DEFAULT_FACET_COUNT));
      solrQuery.setFacetMissing(DEFAULT_FACET_MISSING);
      solrQuery.setFacetSort(DEFAULT_FACET_SORT.toString().toLowerCase());

      for (final P facet : searchRequest.getFacets()) {
        if (!configurations.containsKey(facet)) {
          LOG.warn("{} is no valid facet. Ignore", facet);
          continue;
        }
        FacetFieldConfiguration facetFieldConfiguration = configurations.get(facet);
        final String field = facetFieldConfiguration.getField();
        if (searchRequest.isMultiSelectFacets()) {
          // use exclusion filter with same name as used in filter query
          // http://wiki.apache.org/solr/SimpleFacetParameters#Tagging_and_excluding_Filters
          solrQuery.addFacetField(FACET_FILTER_EX.replace(TAG_FIELD_PARAM, field));
        } else {
          solrQuery.addFacetField(field);
        }
        if (facetFieldConfiguration.isMissing() != DEFAULT_FACET_MISSING) {
          solrQuery.setParam(perFieldParamName(field, PARAM_FACET_MISSING), facetFieldConfiguration.isMissing());
        }
        if (facetFieldConfiguration.getSortOrder() != DEFAULT_FACET_SORT) {
          solrQuery.setParam(perFieldParamName(field, PARAM_FACET_SORT), facetFieldConfiguration.getSortOrder().toString().toLowerCase());
        }
        setFacetMethod(solrQuery, field, facetFieldConfiguration.getMethod());
      }
    }
  }

  /**
   * Utility method that creates the resulting Solr expression for facet and general query filters parameters.
   */
  public static StringBuilder buildFilterQuery(final boolean isFacetedRequest, final String solrFieldName,
                                                List<String> filterQueriesComponents) {
    StringBuilder filterQuery = new StringBuilder();
    if (isFacetedRequest) {
      filterQuery.append(FACET_FILTER_TAG.replace(TAG_FIELD_PARAM, solrFieldName));
    }
    if (filterQueriesComponents.size() > 1) {
      filterQuery.append('(');
      filterQuery.append(PARAMS_OR_JOINER.join(filterQueriesComponents));
      filterQuery.append(')');
    } else {
      filterQuery.append(PARAMS_OR_JOINER.join(filterQueriesComponents));
    }
    return filterQuery;
  }


  /**
   * Interprets the value of parameter "value" using types pType (Parameter type) and eType (Enumeration).
   */
  public static String getInterpretedValue(final Class<?> pType, final String value) {
    // By default use a phrase query is surrounded by "
    String interpretedValue = APOSTROPHE + value + APOSTROPHE;
    if (Enum.class.isAssignableFrom(pType)) {
      // treat country codes special, they use iso codes
      Enum<?> e;
      if (Country.class.isAssignableFrom(pType)) {
        e = Country.fromIsoCode(value);
      } else {
        e = VocabularyUtils.lookupEnum(value, (Class<? extends Enum<?>>) pType);
      }
      if (value == null) {
        throw new IllegalArgumentException("Value Null is invalid for filter parameter " + pType.getName());
      }
      interpretedValue = String.valueOf(e.ordinal());

    } else if (UUID.class.isAssignableFrom(pType)) {
      interpretedValue = UUID.fromString(value).toString();

    } else if (Double.class.isAssignableFrom(pType)) {
      interpretedValue = String.valueOf(Double.parseDouble(value));

    } else if (Integer.class.isAssignableFrom(pType)) {
      interpretedValue = String.valueOf(Integer.parseInt(value));

    } else if (Boolean.class.isAssignableFrom(pType)) {
      interpretedValue = String.valueOf(Boolean.parseBoolean(value));
    }

    return interpretedValue;
  }

  /**
   * @param field the solr field
   * @param param the parameter to use on a per field basis
   * @return per field facet parameter, e.g. f.dataset_type.facet.sort
   */
  public static String perFieldParamName(String field, String param) {
    return "f." + field + "." + param;
  }

  public static String taggedField(String solrFieldName){
    return  TAG_FIELD_PARAM_PATTERN.matcher(FACET_FILTER_TAG).replaceAll(Matcher.quoteReplacement(solrFieldName));
  }

  /**
   * Helper method that takes Solr response and extracts the facets results.
   * The facets are converted to a list of Facets understood by the search API.
   * The result of this method can be a empty list.
   *
   * @param queryResponse that contains the facets information returned by Solr
   * @return the List of facets retrieved from the Solr response
   */
  public static <P extends SearchParameter> List<Facet<P>> getFacetsFromResponse(final QueryResponse queryResponse, Map<String,P> fieldToParamMap) {
    List<Facet<P>> facets = Lists.newArrayList();
    if (queryResponse.getFacetFields() != null) {
      for (final org.apache.solr.client.solrj.response.FacetField facetField : queryResponse.getFacetFields()) {
        P facetParam = fieldToParamMap.get(facetField.getName());
        Facet<P> facet = new Facet<P>(facetParam);

        List<Facet.Count> counts = Lists.newArrayList();
        if (facetField.getValues() != null) {
          for (final org.apache.solr.client.solrj.response.FacetField.Count count : facetField.getValues()) {
            String value = count.getName();
            if (!Strings.isNullOrEmpty(value) && Enum.class.isAssignableFrom(facetParam.type())) {
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
  public static <P extends SearchParameter> String getFacetEnumValue(P facetParam, String value) {
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
}
