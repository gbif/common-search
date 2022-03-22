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
import org.gbif.common.search.EsSearchRequestBuilder;
import org.gbif.common.search.test.DataFieldMapper;
import org.gbif.common.search.test.DataSearchParameter;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EsSearchRequestBuilderTest {

  private final DataFieldMapper fieldMapper = new DataFieldMapper();
  private final EsSearchRequestBuilder<DataSearchParameter> requestBuilder = new EsSearchRequestBuilder<>(fieldMapper);

  @Test
  public void testBuild() {
    FacetedSearchRequest<DataSearchParameter> searchRequest = new FacetedSearchRequest<>();
    searchRequest.addParameter(DataSearchParameter.TITLE, "Animals");
    searchRequest.setFacets(Collections.singleton(DataSearchParameter.COUNTRY));

    co.elastic.clients.elasticsearch.core.SearchRequest
      esSearchRequest = requestBuilder.buildSearchRequest(searchRequest, "data");

    BoolQuery boolQueryBuilder = esSearchRequest.query().bool();
    assertEquals(1, boolQueryBuilder.filter().size());

    Query filter = boolQueryBuilder.filter().get(0);
    assertEquals(fieldMapper.get (DataSearchParameter.TITLE), filter.term().field());
    assertEquals("Animals", filter.term().value().stringValue());
  }

}
