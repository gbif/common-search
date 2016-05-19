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
package org.gbif.common.search.service;


import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.service.common.SuggestService;
import org.gbif.common.search.builder.SearchResponseBuilder;
import org.gbif.common.search.builder.SolrQueryBuilder;
import org.gbif.common.search.builder.SuggestQueryStringBuilder;
import org.gbif.common.search.exception.SearchException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extension of {@link SolrSearchService} that implements the {@link org.gbif.api.service.common.SuggestService}.
 * This class is generic implementation that can be re-used for different and more implementations.
 *
 * @param <T> Type of request object
 * @param <R> Type of response object
 * @param <P> the search parameter enum
 */
public class SolrSearchSuggestService<T, P extends Enum<?> & SearchParameter, ST extends T, R extends FacetedSearchRequest<P>, SUGT, SSUGT extends SUGT, RSUG extends SearchRequest<P>>
  extends SolrSearchService<T, P, ST, R> implements SuggestService<SUGT, P, RSUG> {

  // Logger
  private static final Logger LOG = LoggerFactory.getLogger(SolrSearchSuggestService.class);

  // Cached instance of query builder.
  private final SolrQueryBuilder<ST, P> suggestQueryBuilder;

  private static final int DEFAULT_SUGGEST_LIMIT = 10;

  private final Class<SSUGT> suggestAnnotatedClass;

  /**
   * Default constructor.
   *
   * @param solrClient Solr server instance, this abstract type is used because it can hold instance of
   *        HttpSolrClient or EmbeddedSolrServer
   * @param searchType of the results content
   * @param primarySortOrder ordered fields used for an optional sort order in every search
   */
  public SolrSearchSuggestService(SolrClient solrClient, Class<T> searchType, Class<ST> searchSolrType,
                                  Class<P> searchParameterType, Map<String, SolrQuery.ORDER> primarySortOrder,
                                  Class<SSUGT> suggestAnnotatedClass) {
    super(solrClient, searchType, searchSolrType, searchParameterType, primarySortOrder);
    this.suggestAnnotatedClass = suggestAnnotatedClass;
    suggestQueryBuilder =
      SolrQueryBuilder.create(searchParameterType, searchSolrType).withQueryBuilder(
        SuggestQueryStringBuilder.create(suggestAnnotatedClass));
  }


  /**
   * Constructor for regular search/suggest operations.
   * Doesn't contain the default order for suggest results.
   *
   * @param solrClient Solr client instance, this abstract type is used because it can hold instance of
   *        CommonsHttpSolrServer or EmbeddedSolrServer
   * @param requestHandler specific Solr request handler to be used
   * @param searchType of the results content
   * @param primarySortOrder ordered fields used for an optional sort order in every search
   */
  public SolrSearchSuggestService(SolrClient solrClient, @Nullable final String requestHandler, Class<T> searchType,
                                  Class<ST> searchSolrType, Class<P> searchParameterType,
                                  Map<String, SolrQuery.ORDER> primarySortOrder, Class<SSUGT> suggestAnnotatedClass) {
    super(solrClient, requestHandler, searchType, searchSolrType, searchParameterType, primarySortOrder);
    this.suggestAnnotatedClass = suggestAnnotatedClass;
    suggestQueryBuilder =
      SolrQueryBuilder.create(searchParameterType, searchSolrType).withRequestHandler(requestHandler)
        .withQueryBuilder(SuggestQueryStringBuilder.create(suggestAnnotatedClass));
  }


  /**
   * Full constructor.
   *
   * @param solrClient Solr client instance, this abstract type is used because it can hold instance of
   *        CommonsHttpSolrServer or EmbeddedSolrServer
   * @param requestHandler specific Solr request handler to be used
   * @param searchType of the results content
   * @param primarySortOrder ordered fields used for an optional sort order in every search
   * @param suggestSortOrder ordered fields used for an optional sort order in every suggest operation
   */
  public SolrSearchSuggestService(SolrClient solrClient, @Nullable final String requestHandler, Class<T> searchType,
                                  Class<ST> searchSolrType, Class<P> searchParameterType,
                                  Map<String, SolrQuery.ORDER> primarySortOrder,
                                  Map<String, SolrQuery.ORDER> suggestSortOrder, Class<SSUGT> suggestAnnotatedClass) {
    super(solrClient, requestHandler, searchType, searchSolrType, searchParameterType, primarySortOrder);
    this.suggestAnnotatedClass = suggestAnnotatedClass;
    suggestQueryBuilder =
      SolrQueryBuilder.create(searchParameterType, searchSolrType).withRequestHandler(requestHandler)
        .withQueryBuilder(SuggestQueryStringBuilder.create(suggestAnnotatedClass))
        .withPrimarySortOrder(suggestSortOrder);
  }

  @Override
  public List<SUGT> suggest(RSUG suggestRequest) {
    if (suggestRequest.getLimit() < 1) {
      LOG.debug("Suggest request with limit {} found. Reset to default {}", suggestRequest.getLimit(),
                DEFAULT_SUGGEST_LIMIT);
      suggestRequest.setLimit(DEFAULT_SUGGEST_LIMIT);
    }
    if (suggestRequest.getOffset() > 0) {
      LOG.debug("Suggest request with offset {} found", suggestRequest.getOffset());
    }

    try {
      QueryResponse queryResponse = getSolrClient().query(suggestQueryBuilder.build(suggestRequest));
      return (List<SUGT>) SearchResponseBuilder.buildSuggestReponse(suggestRequest, queryResponse,
                                                                    suggestAnnotatedClass).getResults();

    } catch (SolrServerException | IOException e) {
      LOG.error("Error executing/building the request", e);
      throw new SearchException(e);
    }
  }

}
