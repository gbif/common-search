/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.common.search.solr;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.util.IsoDateParsingUtils;
import org.gbif.api.util.SearchTypeValidator;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.TermsParams;
import org.apache.solr.parser.QueryParser;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import static org.gbif.common.search.solr.SolrConstants.APOSTROPHE;
import static org.gbif.common.search.solr.SolrConstants.BLANK;
import static org.gbif.common.search.solr.SolrConstants.DEFAULT_FILTER_QUERY;
import static org.gbif.common.search.solr.SolrConstants.FACET_FILTER_TAG;
import static org.gbif.common.search.solr.SolrConstants.HTTP_AND_OP;
import static org.gbif.common.search.solr.SolrConstants.MAX_PAGE_SIZE;
import static org.gbif.common.search.solr.SolrConstants.PARAM_AND_OP;
import static org.gbif.common.search.solr.SolrConstants.PARAM_OR_OP;
import static org.gbif.common.search.solr.SolrConstants.PARAM_VALUE_SEP;
import static org.gbif.common.search.solr.SolrConstants.PARENTHESES_EXP_FORMAT;
import static org.gbif.common.search.solr.SolrConstants.SCORE_OP;
import static org.gbif.common.search.solr.SolrConstants.TAG_FIELD_PARAM;
import static org.gbif.common.search.solr.SolrConstants.TERMS_COMP_PATH;


/**
 * Utility class for common query methods.
 */
@UtilityClass
public class QueryUtils {

  // Solr parameters joiners
  // TODO: maybe we should return a non-guava class or expose a method instead of the variable
  // that prevents the shading of Guava
  public static final Joiner PARAMS_JOINER = Joiner.on(PARAM_VALUE_SEP).skipNulls();
  public static final Joiner PARAMS_OR_JOINER = Joiner.on(PARAM_OR_OP).skipNulls();
  public static final Joiner PARAMS_AND_JOINER = Joiner.on(PARAM_AND_OP).skipNulls();
  public static final Joiner HTTP_PARAMS_JOINER = Joiner.on(HTTP_AND_OP).skipNulls();
  public static final String DEFAULT_FACET_SORT = "count";
  public static final int DEFAULT_FACET_COUNT = 1;
  public static final boolean DEFAULT_FACET_MISSING = true;
  public static final String ISO8601_FMT = "yyyy-MM-dd'T'00:00:00'Z'";
  public static final String NOT_OP = "!";

  private static final Pattern REGEX_MULTIPLE_BLANKS = Pattern.compile("\\s(\\s)+");
  private static final Pattern REGEX_NOT_OP = Pattern.compile(NOT_OP);
  private static final Pattern TAG_FIELD_PARAM_PATTERN = Pattern.compile(TAG_FIELD_PARAM, Pattern.LITERAL);
  private static final String SINGLE_BLANK = "\\ ";

  // Pattern for setting the facet method on single field
  private static final String FACET_METHOD_FMT = "f.%s." + FacetParams.FACET_METHOD;

  /**
   * Builds SolrQuery suitable to the TermsComponent.
   *
   * @param fieldName solr field to be searched
   * @param limit maximum number of results
   * @return a {@link SolrQuery}
   */
  public static SolrQuery buildTermQuery(String fieldName, Integer limit) {
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setParam(CommonParams.QT, TERMS_COMP_PATH);
    solrQuery.setParam(TermsParams.TERMS, true);
    solrQuery.setParam(TermsParams.TERMS_LIMIT, limit.toString());
    solrQuery.setParam(TermsParams.TERMS_FIELD, fieldName);
    solrQuery.setParam(TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_INDEX);
    return solrQuery;
  }

  /**
   * Builds SolrQuery suitable to the TermsComponent.
   *
   * @param prefix search term
   * @param fieldName solr field to be searched
   * @param limit maximum number of results
   * @return a {@link SolrQuery}
   */
  public static SolrQuery buildTermQuery(String prefix, String fieldName, Integer limit) {
    SolrQuery solrQuery = buildTermQuery(fieldName, limit);
    solrQuery.setParam(TermsParams.TERMS_PREFIX_STR, prefix);
    return solrQuery;
  }

  /**
   * Removes all the consecutive whitespaces and leave only 1.
   */
  public static String clearConsecutiveBlanks(String query) {
    return REGEX_MULTIPLE_BLANKS.matcher(query).replaceAll(SINGLE_BLANK);
  }

  /**
   * If the query parameter is empty returns the default query "*".
   *
   * @return default query if q parameter is empty
   */
  public static String emptyToDefaultQuery(String q) {
    // return default query for empty queries
    String qValue = Strings.nullToEmpty(q).trim();
    if (Strings.isNullOrEmpty(qValue)) {
      return DEFAULT_FILTER_QUERY;
    }
    return qValue;
  }

  /**
   * @param field the solr field
   * @param param the parameter to use on a per field basis
   * @return per field facet parameter, e.g. f.dataset_type.facet.sort
   */
  public static String perFieldParamName(String field, String param) {
    return "f." + field + "." + param;
  }

  public static String taggedField(String fieldName){
    return  taggedField(fieldName,FACET_FILTER_TAG);
  }

  public static String taggedField(String solrFieldName, String matcher){
    return  TAG_FIELD_PARAM_PATTERN.matcher(matcher).replaceAll(Matcher.quoteReplacement(solrFieldName));
  }

  /**
   * Determines if the value is negated with the NOT operator.
   */
  public static boolean isNegated(String value) {
    return value.startsWith(NOT_OP);
  }

  /**
   * Checks if the value parameters is a Range query.
   */
  public static boolean isRangeQuery(String value) {
    return SearchTypeValidator.isRange(value);
  }

  /**
   * Escapes a query value and transform it into a phrase query if necessary.
   */
  public static String parseQueryValue(final String q) {
    // return default query for empty queries
    String qValue = Strings.nullToEmpty(q).trim();
    if (Strings.isNullOrEmpty(qValue)) {
      return DEFAULT_FILTER_QUERY;
    }
    if (isRangeQuery(qValue)) {
      String[] rangeValue = qValue.split(",");
      if (rangeValue.length == 2) {
        return String.format(SolrConstants.RANGE_FORMAT, rangeValue[0].trim(), rangeValue[1].trim());
      }
    } else {
      // If default query was sent, must not be escaped
      if (!qValue.equals(DEFAULT_FILTER_QUERY)) {
        qValue = clearConsecutiveBlanks(qValue);
        qValue = escapeQuery(qValue);
      }
      // make it a phrase query if it contains blanks
      if (qValue.contains(BLANK)) {
        qValue = toPhraseQuery(qValue);
      }
    }
    return qValue;
  }

  /**
   * Escape special characters and Solr reserved words.
   */
  public static String escapeQuery(String value) {
    if (value.equals(QueryParser.Operator.AND.name()) || value.equals(QueryParser.Operator.OR.name())
        || value.equals(SolrConstants.SOLR_NOT_OP)) {
      return toPhraseQuery(ClientUtils.escapeQueryChars(value));
    }
    return ClientUtils.escapeQueryChars(value);
  }

  /**
   * If the value parameter starts with NOT it is removed.
   */
  public static String removeNegation(String value) {
    if (value.startsWith(NOT_OP)) {
      return REGEX_NOT_OP.matcher(value).replaceFirst("");
    }
    return value;
  }


  /**
   * Helper method that sets the parameters for a paginated query.
   *
   * @param pageable the Pageable used to extract the parameters
   * @param solrQuery this object is modified adding the pagination parameters
   */
  public static void setQueryPaging(Pageable pageable, SolrQuery solrQuery) {
    Long offset = pageable.getOffset();
    solrQuery.setRows(Math.min(pageable.getLimit(), MAX_PAGE_SIZE));
    solrQuery.setStart(Math.max(0, offset.intValue()));
  }

  /**
   * Helper method that sets the parameters for a paginated query.
   *
   * @param pageable the Pageable used to extract the parameters
   * @param solrQuery this object is modified adding the pagination parameters
   * @param maxPageSize the maximum page size allowed
   */
  public static void setQueryPaging(Pageable pageable, SolrQuery solrQuery, Integer maxPageSize) {
    Long offset = pageable.getOffset();
    solrQuery.setRows(Math.min(pageable.getLimit(), maxPageSize));
    solrQuery.setStart(Math.max(0, offset.intValue()));
  }

  /**
   * Adds the qt parameter if necessary.
   *
   * @param solrQuery to be modified.
   */
  public static void setRequestHandler(SolrQuery solrQuery, String requestHandler) {
    if (!Strings.isNullOrEmpty(requestHandler)) {
      solrQuery.setRequestHandler(requestHandler);
    }
  }

  /**
   * Adds the shards parameter if necessary.
   *
   * @param solrQuery to be modified.
   */
  public static void setShardsInfo(SolrQuery solrQuery, String shards) {
    if (!Strings.isNullOrEmpty(shards)) {
      solrQuery.set("shards", shards);
    }
  }

  /**
   * Sets the sort order query information.
   *
   * @param solrQuery to be modified.
   */
  public static void setSortOrder(SolrQuery solrQuery, Map<String, SolrQuery.ORDER> sortOrder) {
    if (sortOrder != null) {
      for (Map.Entry<String, SolrQuery.ORDER> so : sortOrder.entrySet()) {
        solrQuery.addSort(so.getKey(), so.getValue());
      }
    }
  }

  /**
   * Produces a query with the form: "(query)^boostValue".
   *
   * @param query to be boosted
   * @param boostValue Solr boost factor
   * @return a query in format "(query)^boostValue."
   */
  public static String toBoostedQuery(String query, Integer boostValue, boolean withParentheses) {
    if (withParentheses) {
      return toParenthesesQuery(query + SCORE_OP + boostValue);
    }
    return query + SCORE_OP + boostValue;
  }

  /**
   * Transforms a Date instance into a String using the default date parser.
   */
  public static String toDateQueryFormat(LocalDate value) {
    return IsoDateParsingUtils.ISO_DATE_FORMATTER.format(value);
  }


  /**
   * Transforms the query into query surrounded by parentheses.
   */
  public static String toParenthesesQuery(String query) {
    return String.format(PARENTHESES_EXP_FORMAT, query);
  }

  /**
   * Adds the apostrophes to convert the input pattern into a phrase query pattern.
   *
   * @param query the input search pattern
   */
  public static String toPhraseQuery(String query) {
    return APOSTROPHE + query + APOSTROPHE;
  }


}
