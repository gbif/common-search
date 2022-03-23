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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.search.Hit;

import static org.gbif.common.search.es.indexing.EsQueryUtils.extractFacetLimit;
import static org.gbif.common.search.es.indexing.EsQueryUtils.extractFacetOffset;

public class EsResponseParser<T,SR, P extends SearchParameter> {

  private final EsFieldMapper<P> fieldParameterMapper;

  private final Function<Hit<SR>,T> searchResultConverter;

  /** Private constructor. */
  public EsResponseParser(
    Function<Hit<SR>,T> searchResultConverter, EsFieldMapper<P> fieldParameterMapper) {
    this.searchResultConverter = searchResultConverter;
    this.fieldParameterMapper = fieldParameterMapper;
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<T, P> buildSearchResponse(
    co.elastic.clients.elasticsearch.core.SearchResponse<SR> esResponse, SearchRequest<P> request) {
    return buildSearchResponse(esResponse, request, searchResultConverter);
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<T, P> buildSearchResponse(
    co.elastic.clients.elasticsearch.core.SearchResponse<SR> esResponse,
      SearchRequest<P> request,
      Function<Hit<SR>, T> mapper) {

    SearchResponse<T,P> response = new SearchResponse<>(request);
    Optional.ofNullable(esResponse.hits().total()).ifPresent(t -> response.setCount(t.value()));
    response.setResults(parseHits(esResponse, mapper));
    if (request instanceof FacetedSearchRequest) {
      response.setFacets(parseFacets(esResponse, (FacetedSearchRequest<P>) request));
    }

    return response;
  }

  /** Extract the buckets of an {@link Aggregate}. */
  private Buckets<? extends MultiBucketBase> getBuckets(Aggregate aggregate) {
    if (aggregate.isSterms()) {
      return aggregate.sterms().buckets();
    } else if (aggregate.isLterms()) {
      return aggregate.lterms().buckets();
    } else if (aggregate.isDterms()) {
      return aggregate.dterms().buckets();
    } else if (aggregate.isFilters()) {
      return aggregate.filters().buckets();
    } else {
      throw new IllegalArgumentException(aggregate.getClass() + " aggregation not supported");
    }
  }

  private Buckets<? extends MultiBucketBase> getFilteredBuckets(String aggregateName, Aggregate aggregate) {
    return getBuckets(aggregate.filter().aggregations().get("filtered_" + aggregateName));
  }

  private List<Facet<P>> parseFacets(
    co.elastic.clients.elasticsearch.core.SearchResponse<SR> esResponse, FacetedSearchRequest<P> request) {
    return esResponse.aggregations()
          .entrySet()
          .stream()
          .map(
              agg ->
              {
                            // get buckets
                            Buckets<? extends MultiBucketBase> buckets = agg.getValue().isFilter()? getFilteredBuckets(agg.getKey(), agg.getValue()): getBuckets(agg.getValue());

                            // get facet of the agg
                            P facet = fieldParameterMapper.get(agg.getKey().replaceFirst("filtered_",""));

                            // check for paging in facets
                            long facetOffset = extractFacetOffset(request, facet);
                            long facetLimit = extractFacetLimit(request, facet);

                            List<Facet.Count> counts =
                                buckets.array().stream()
                                    .skip(facetOffset)
                                    .limit(facetOffset + facetLimit)
                                    .map(b -> new Facet.Count(fieldParameterMapper.parseIndexedValue(agg.getValue().isFilter()? getFilteredBucketKey(
                                      agg.getKey(), agg.getValue(),b) : getBucketKey(agg.getValue(), b), facet), b.docCount()))
                                    .collect(Collectors.toList());

                            return new Facet<>(facet, counts);
                          })
                      .collect(Collectors.toList());
  }

  private static String getFilteredBucketKey(String aggName, Aggregate aggregate, MultiBucketBase bucket) {
    return getBucketKey(aggregate.filter().aggregations().get("filtered_" + aggName), bucket);
  }

  private static String getBucketKey(Aggregate aggregate, MultiBucketBase bucket) {
    if (aggregate.isLterms()) {
      return Long.toString(((LongTermsBucket)bucket).key());
    }
    if (aggregate.isDterms()) {
      return Double.toString(((DoubleTermsBucket)bucket).key());
    }
    if (aggregate.isSterms()) {
      return ((StringTermsBucket)bucket).key();
    }
    throw new IllegalArgumentException(aggregate.getClass() + " aggregation not supported");
  }

  private List<T> parseHits(
    co.elastic.clients.elasticsearch.core.SearchResponse<SR> esResponse, Function<Hit<SR>, T> mapper) {
    return esResponse.hits().hits().stream().map(mapper).collect(Collectors.toList());
  }

}
