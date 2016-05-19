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

import org.gbif.api.model.common.search.SearchRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.common.search.util.QueryUtils.PARAMS_OR_JOINER;
import static org.gbif.common.search.util.QueryUtils.parseQueryValue;
import static org.gbif.common.search.util.SolrConstants.BLANK;
import static org.gbif.common.search.util.SolrConstants.DEFAULT_FILTER_QUERY;
import static org.gbif.common.search.util.SolrConstants.DEFAULT_QUERY;


/**
 * Builder class that helps in the creation process of query patterns for classes annotated with
 * {@link org.gbif.common.search.model.FullTextSearchField}.
 */
public abstract class QueryStringBuilderBase {


  protected static final Logger LOG = LoggerFactory.getLogger(QueryStringBuilderBase.class);

  // Solr parameter place holder
  protected static final String QUERY_PLACE_HOLDER = "$q";

  private static final Pattern QUERY_PLACE_HOLDER_PTRN = Pattern.compile(QUERY_PLACE_HOLDER,Pattern.LITERAL);

  // query template patterns to build queries by replacing the above placeholder
  protected String queryTemplate = QUERY_PLACE_HOLDER;
  protected String phraseQueryTemplate = QUERY_PLACE_HOLDER;

  protected final List<String> highlightedFields = Lists.newArrayList();

  protected QueryStringBuilderBase(Class<?> annotatedClass) {
    initTemplates(annotatedClass);
    LOG.info("Query patterns generated for simple / phrase searches : {} / {}", queryTemplate, phraseQueryTemplate);
  }

  /**
   * Creates a string containing the full text query expression.
   *
   * @param searchRequest used to extract parameters information.
   */
  public String build(final SearchRequest<?> searchRequest) {
    return build(searchRequest.getQ());
  }

  /**
   * Creates a string containing the full text query expression.
   */
  public String build(String q) {
    String parsedQ = parseQueryValue(q);
    String generatedQuery = QUERY_PLACE_HOLDER_PTRN.matcher(getSearchPattern(parsedQ))
                            .replaceAll(Matcher.quoteReplacement(parsedQ));
    LOG.debug("Solr query generated for fulltext search: {}", generatedQuery);
    return generatedQuery;
  }

  /**
   * Gets list of the fields for highlighting.
   */
  public List<String> getHighlightedFields() {
    return highlightedFields;
  }

  /**
   * Builds a string literal surround by parenthesis (or empty) of the query pattern.
   *
   * @param queryComponets list of query components in the form field:pattern.
   *
   * @return a string with all the query components.
   */
  protected static String buildQueryExpression(List<String> queryComponets) {
    if (!queryComponets.isEmpty()) {
      return '(' + PARAMS_OR_JOINER.join(queryComponets) + ')';
    }
    return "";
  }

  /**
   * Helper method to determine if the query pattern to be used is the full text or the phrase query pattern.
   */
  protected String getSearchPattern(String qValue) {
    if (qValue.contains(BLANK)) { // is a phrase query
      return phraseQueryTemplate;
    } else if (qValue.equals(DEFAULT_FILTER_QUERY)) {
      return DEFAULT_QUERY;
    }
    return queryTemplate;
  }

  /**
   * Helper method that sets the parameters for full text search param "q".
   * It will also add the optional query function.
   */
  protected abstract void initTemplates(Class<?> annotatedClass);

}
