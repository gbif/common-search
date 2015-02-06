package org.gbif.common.search.util;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.WildcardPadding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.TermsParams;

import static org.gbif.common.search.util.SolrConstants.APOSTROPHE;
import static org.gbif.common.search.util.SolrConstants.BLANK;
import static org.gbif.common.search.util.SolrConstants.DEFAULT_FILTER_QUERY;
import static org.gbif.common.search.util.SolrConstants.DEFAULT_PAGE_SIZE;
import static org.gbif.common.search.util.SolrConstants.HTTP_AND_OP;
import static org.gbif.common.search.util.SolrConstants.MAX_PAGE_SIZE;
import static org.gbif.common.search.util.SolrConstants.PARAM_AND_OP;
import static org.gbif.common.search.util.SolrConstants.PARAM_OR_OP;
import static org.gbif.common.search.util.SolrConstants.PARAM_VALUE_SEP;
import static org.gbif.common.search.util.SolrConstants.PARENTHESES_EXP_FORMAT;
import static org.gbif.common.search.util.SolrConstants.SCORE_OP;
import static org.gbif.common.search.util.SolrConstants.TERMS_COMP_PATH;


/**
 * Utility class for common query methods.
 */
public class QueryUtils {

  // Solr parameters joiners
  public static final Joiner PARAMS_JOINER = Joiner.on(PARAM_VALUE_SEP).skipNulls();
  public static final Joiner PARAMS_OR_JOINER = Joiner.on(PARAM_OR_OP).skipNulls();
  public static final Joiner PARAMS_AND_JOINER = Joiner.on(PARAM_AND_OP).skipNulls();
  public static final Joiner HTTP_PARAMS_JOINER = Joiner.on(HTTP_AND_OP).skipNulls();

  public static final String ISO8601_FMT = "yyyy-MM-dd'T'00:00:00'Z'";

  private static final String REGEX_MULTIPLE_BLANKS = "\\s(\\s)+";

  private static final String SINGLE_BLANK = "\\ ";

  // Pattern for setting the facet method on single field
  private static final String FACET_METHOD_FMT = "f.%s" + FacetParams.FACET_METHOD;


  private static final ImmutableMap<FacetField.Method, String> FACET_METHOD_MAP =
    new ImmutableMap.Builder<FacetField.Method, String>()
      .put(FacetField.Method.ENUM, FacetParams.FACET_METHOD_enum)
      .put(FacetField.Method.FIELD_CACHE, FacetParams.FACET_METHOD_fc)
      .put(FacetField.Method.FIELD_CACHE_SEGMENT, FacetParams.FACET_METHOD_fcs)
      .build();

  /**
   * Default private/hidden constructor.
   */
  private QueryUtils() {
    // empty block
  }

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
    return query.replaceAll(REGEX_MULTIPLE_BLANKS, SINGLE_BLANK);
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
   * Utility method that return the exact matching field to use.
   * If field.exactMatchField is null or empty, return field.field() else return field.exactMatchField().
   */
  public static String getExactMatchField(FullTextSearchField field) {
    return Strings.isNullOrEmpty(field.exactMatchField()) ? field.field() : field.exactMatchField();
  }

  /**
   * According to the FullTextSearchField.partialMatching() returns a search pattern that could contains wildcards.
   *
   * @param field the Annotation to be analyzed
   * @param query the input search pattern
   */
  public static String getMatchPatternAndScore(FullTextSearchField field, String query) {
    if (field.partialMatching() == WildcardPadding.BOTH) {
      return DEFAULT_FILTER_QUERY + query + DEFAULT_FILTER_QUERY + SCORE_OP + field.partialMatchScore();
    } else if (field.partialMatching() == WildcardPadding.LEFT) {
      return DEFAULT_FILTER_QUERY + query + SCORE_OP + field.partialMatchScore();
    } else if (field.partialMatching() == WildcardPadding.RIGHT) {
      return query + DEFAULT_FILTER_QUERY + SCORE_OP + field.partialMatchScore();
    } else { // NONE
      return query;
    }
  }


  /**
   * Determines if the value is negated with the {@link SearchConstants#NOT_OP} operator.
   */
  public static boolean isNegated(String value) {
    return value.startsWith(SearchConstants.NOT_OP);
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
        qValue = ClientUtils.escapeQueryChars(qValue);
      }
      // make it a phrase query if it contains blanks
      if (qValue.contains(BLANK)) {
        qValue = toPhraseQuery(qValue);
      }
    }
    return qValue;
  }


  /**
   * If the value parameter starts with {@link SearchConstants#NOT_OP} it is removed.
   */
  public static String removeNegation(String value) {
    if (value.startsWith(SearchConstants.NOT_OP)) {
      return value.replaceFirst(SearchConstants.NOT_OP, "");
    }
    return value;
  }


  /**
   * Helper method that sets the parameters for a paginated query.
   *
   * @param pageable the Pageable used to extract the parameters
   * @param solrQuery this object is modified adding the pagination parameters
   */
  public static void setQueryPaging(final Pageable pageable, SolrQuery solrQuery) {
    final Integer pageSize = pageable.getLimit();
    final Long offset = pageable.getOffset();
    solrQuery.setRows(pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE));
    solrQuery.setStart(offset == null ? 0 : offset.intValue());
  }

  /**
   * Helper method that sets the parameters for a paginated query.
   *
   * @param pageable the Pageable used to extract the parameters
   * @param solrQuery this object is modified adding the pagination parameters
   * @param maxPageSize the maximum page size allowed
   */
  public static void setQueryPaging(final Pageable pageable, SolrQuery solrQuery, final Integer maxPageSize) {
    final Integer pageSize = pageable.getLimit();
    final Long offset = pageable.getOffset();
    solrQuery.setRows(pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(pageSize, maxPageSize));
    solrQuery.setStart(offset == null ? 0 : offset.intValue());
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
      return toParenthesesQuery(query + SCORE_OP + boostValue.toString());
    }
    return query + SCORE_OP + boostValue.toString();
  }

  /**
   * Transforms a Date instance into a String using the default date parser.
   */
  public static String toDateQueryFormat(Date value) {
    return new SimpleDateFormat(ISO8601_FMT).format(value);
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

  /**
   * Sets the Solr facet.method for the field parameter accoding to the method parameter.
   */
  public static void setFacetMethod(SolrQuery solrQuery, String field, FacetField.Method facetFieldMethod) {
    solrQuery.setParam(String.format(FACET_METHOD_FMT, field), FACET_METHOD_MAP.get(facetFieldMethod));
  }
}
