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
package org.gbif.common.search;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.model.common.search.SearchResponse;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import static org.gbif.common.search.es.indexing.EsQueryUtils.extractFacetLimit;
import static org.gbif.common.search.es.indexing.EsQueryUtils.extractFacetOffset;

public class EsResponseParser<T, P extends SearchParameter> {

  private final EsFieldMapper<P> fieldParameterMapper;

  private final SearchResultConverter<T> searchResultConverter;

  /** Private constructor. */
  public EsResponseParser(
      SearchResultConverter<T> searchResultConverter, EsFieldMapper<P> fieldParameterMapper) {
    this.searchResultConverter = searchResultConverter;
    this.fieldParameterMapper = fieldParameterMapper;
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<T, P> buildSearchResponse(
      org.elasticsearch.action.search.SearchResponse esResponse, SearchRequest<P> request) {
    return buildSearchResponse(esResponse, request, searchResultConverter::toSearchResult);
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public <R> SearchResponse<R, P> buildSearchResponse(
      org.elasticsearch.action.search.SearchResponse esResponse,
      SearchRequest<P> request,
      Function<SearchHit, R> mapper) {

    SearchResponse<R, P> response = new SearchResponse<>(request);
    response.setCount(esResponse.getHits().getTotalHits().value);
    parseHits(esResponse, mapper).ifPresent(response::setResults);
    if (request instanceof FacetedSearchRequest) {
      parseFacets(esResponse, (FacetedSearchRequest<P>) request).ifPresent(response::setFacets);
    }

    return response;
  }


  /** Extract the buckets of an {@link Aggregation}. */
  private List<? extends Terms.Bucket> getBuckets(Aggregation aggregation) {
    if (aggregation instanceof Terms) {
      return ((Terms) aggregation).getBuckets();
    } else if (aggregation instanceof Filter) {
      return ((Filter) aggregation)
          .getAggregations().asList().stream()
              .flatMap(agg -> ((Terms) agg).getBuckets().stream())
              .collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException(aggregation.getClass() + " aggregation not supported");
    }
  }

  private Optional<List<Facet<P>>> parseFacets(
      org.elasticsearch.action.search.SearchResponse esResponse, FacetedSearchRequest<P> request) {
    return Optional.ofNullable(esResponse.getAggregations())
        .map(
            aggregations ->
                aggregations.asList().stream()
                    .map(
                        aggs -> {
                          // get buckets
                          List<? extends Terms.Bucket> buckets = getBuckets(aggs);

                          // get facet of the agg
                          P facet = fieldParameterMapper.get(aggs.getName());

                          // check for paging in facets
                          long facetOffset = extractFacetOffset(request, facet);
                          long facetLimit = extractFacetLimit(request, facet);

                          List<Facet.Count> counts =
                              buckets.stream()
                                  .skip(facetOffset)
                                  .limit(facetOffset + facetLimit)
                                  .map(b -> new Facet.Count(fieldParameterMapper.parseIndexedValue(b.getKeyAsString(), facet), b.getDocCount()))
                                  .collect(Collectors.toList());

                          return new Facet<>(facet, counts);
                        })
                    .collect(Collectors.toList()));
  }

  private <R> Optional<List<R>> parseHits(
      org.elasticsearch.action.search.SearchResponse esResponse, Function<SearchHit, R> mapper) {
    if (esResponse.getHits() == null
        || esResponse.getHits().getHits() == null
        || esResponse.getHits().getHits().length == 0) {
      return Optional.empty();
    }

    return Optional.of(
        Stream.of(esResponse.getHits().getHits()).map(mapper).collect(Collectors.toList()));
  }

}
