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
package org.gbif.common;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.common.search.EsResponseParser;
import org.gbif.common.search.test.Data;
import org.gbif.common.search.test.DataFieldMapper;
import org.gbif.common.search.test.DataSearchParameter;
import org.gbif.common.search.test.DataSearchResultConverter;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;

public class EsResponseParserTest {

  private final EsResponseParser<Data, Data, DataSearchParameter> responseParser = new EsResponseParser<>(new DataSearchResultConverter(), new DataFieldMapper());

  @Test
  public void buildTest() {
    //Result test data
    Data hitSource = Data.builder().country(Country.AFGHANISTAN)
      .created(new Date())
      .key(1)
      .title("D1")
      .build();

    //Elasticsearch response
    SearchResponse<Data> searchResponse = new SearchResponse.Builder<Data>()
      .took(1)
      .timedOut(false)
      .shards(s -> s.successful(1).failed(0).total(0))
      .hits(hs -> hs.total(t -> t.value(1)
                                 .relation(TotalHitsRelation.Eq))
                                 .hits(Collections.singletonList(new Hit.Builder<Data>().id("1")
                                                                                        .index("data")
                                                                                        .source(hitSource) //test data
                                          .build())))
      .build();

    //Mock request
    FacetedSearchRequest<DataSearchParameter> request = new FacetedSearchRequest<>();
    request.setFacets(Collections.singleton(DataSearchParameter.COUNTRY));

    //Tests
    org.gbif.api.model.common.search.SearchResponse<Data, DataSearchParameter> response = responseParser.buildSearchResponse(searchResponse, request);

    Assertions.assertNotNull(response);
    Assertions.assertEquals(hitSource, response.getResults().get(0));
  }
}
