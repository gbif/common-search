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
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.common.SearchService;
import org.gbif.common.search.builder.FullTextQueryStringBuilder;
import org.gbif.common.search.builder.SearchResponseBuilder;
import org.gbif.common.search.builder.SolrQueryBuilder;
import org.gbif.common.search.exception.SearchException;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generic {@link SearchService} that encapsulates the communication with a {@link SolrServer}.
 * The basic implementation of the service is a search operation that holds all the valid parameters.
 * The response returned is basically information taken from the Solr {@link QueryResponse}.
 * During the initialization time the class expects a {@link SolrServer} instance which
 * depending on the configuration parameters is an remote or a embedded Solr server.
 * Additionally, this class internally implements the necessary logic for accessing the mapped fields between Solr and
 * the request object values; most of this is implemented by using annotations, enum types and java introspection.
 * All the mapping handling is done by {@link SearchResponseBuilder} and
 * {@link org.gbif.common.search.builder.SolrQueryBuilder}.
 *
 * @param <T> the type of the results that the search operation return using a {@link SearchResponse}<T>
 * @param <P> the search parameter enum also used for facets
 */
public class SolrSearchService<T, P extends Enum<?> & SearchParameter, ST extends T, R extends FacetedSearchRequest<P>>
  implements SearchService<T, P, R> {

  private static final Logger LOG = LoggerFactory.getLogger(SolrSearchService.class);

  /*
   * Solr server instance, this abstract type is used because it can hold instance of:
   * CommonsHttpSolrServer or EmbeddedSolrServer.
   */
  private final SolrServer solrServer;

  // Cached instance of response builder.
  // This instance is used for cloning subsequent usages of this object without the penalty recreate similar instances.
  private final SearchResponseBuilder<T, ST, P> responseBuilder;

  private final SolrQueryBuilder<ST, P> searchQueryBuilder;

  /**
   * Default constructor.
   *
   * @param solrServer Solr server instance, this abstract type is used because it can hold instance of
   *        CommonsHttpSolrServer or EmbeddedSolrServer
   * @param type of the results content
   * @param primarySortOrder ordered fields used for an optional sort order in every search
   */
  public SolrSearchService(SolrServer solrServer, Class<T> type, Class<ST> solrType,
    Class<P> enumSearchParamType, Map<String, SolrQuery.ORDER> primarySortOrder) {
    this.solrServer = solrServer;
    FullTextQueryStringBuilder fullTextQueryBuilder = FullTextQueryStringBuilder.create(solrType);
    responseBuilder = SearchResponseBuilder.create(type, solrType, enumSearchParamType);
    responseBuilder.withHighlightFields(fullTextQueryBuilder.getHighlightedFields());
    searchQueryBuilder = SolrQueryBuilder.create(enumSearchParamType, solrType);
    searchQueryBuilder.withQueryBuilder(fullTextQueryBuilder);
    searchQueryBuilder.withPrimarySortOrder(new LinkedHashMap<String, SolrQuery.ORDER>(primarySortOrder));
  }

  /**
   * Full constructor.
   *
   * @param solrServer Solr server instance, this abstract type is used because it can hold instance of
   *        CommonsHttpSolrServer or EmbeddedSolrServer
   * @param requestHandler specific Solr request handler to be used
   * @param type of the results content
   * @param primarySortOrder ordered fields used for an optional sort order in every search
   */
  public SolrSearchService(SolrServer solrServer, @Nullable final String requestHandler, Class<T> type,
    Class<ST> solrType,
    Class<P> enumSearchParamType, Map<String, SolrQuery.ORDER> primarySortOrder) {
    this(solrServer, type, solrType, enumSearchParamType, primarySortOrder);
    searchQueryBuilder.withRequestHandler(requestHandler);
  }


  /**
   * Issues a SolrQuery and converts the response to a SearchResponse object. Besides, the facets and paging
   * parameter and responses are handled in the request and response objects.
   *
   * @param searchRequest the searchRequest that contains the search parameters
   * @return the SearchResponse of the search operation
   */
  @Override
  public SearchResponse<T, P> search(final R searchRequest) {

    try {
      // Defensive copy: done because the build method is not thread safe.
      SolrQueryBuilder<ST, P> requestBuilder = searchQueryBuilder.getCopy();
      SolrQuery solrQuery = requestBuilder.build(searchRequest);

      // Executes the search operation in Solr
      LOG.debug("Solr query executed: {}", solrQuery);
      final QueryResponse queryResponse = solrServer.query(solrQuery);

      // Defensive copy: done because the build method is not thread safe.
      return responseBuilder.getCopy().build(searchRequest, queryResponse);
    } catch (SolrServerException e) {
      if (e.getRootCause() instanceof IllegalArgumentException) {
        LOG.error("Bad search request", e);
        throw (IllegalArgumentException) e.getRootCause();
      } else {
        LOG.error("Error executing the search operation", e);
        throw new SearchException(e);
      }
    }
  }

  /**
   * @return the responseBuilderBase
   */
  protected SearchResponseBuilder<T, ST, P> getResponseBuilder() {
    return responseBuilder;
  }


  /**
   * @return the solrServer
   */
  protected SolrServer getSolrServer() {
    return solrServer;
  }
}
