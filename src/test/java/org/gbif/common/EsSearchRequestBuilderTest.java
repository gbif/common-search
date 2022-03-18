/*
package org.gbif.common;

import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.common.search.EsSearchRequestBuilder;
import org.gbif.common.search.test.DataFieldMapper;
import org.gbif.common.search.test.DataSearchParameter;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EsSearchRequestBuilderTest {

  private final DataFieldMapper fieldMapper = new DataFieldMapper();
  private final EsSearchRequestBuilder<DataSearchParameter> requestBuilder = new EsSearchRequestBuilder<>(fieldMapper);

  @Test
  public void testBuild() {
    SearchRequest<DataSearchParameter> searchRequest = new SearchRequest<>();
    searchRequest.addParameter(DataSearchParameter.TITLE, "Animals");
    org.elasticsearch.action.search.SearchRequest esSearchRequest = requestBuilder.buildSearchRequest(searchRequest, "data");

    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder)esSearchRequest.source().query();
    assertEquals(1, boolQueryBuilder.filter().size());

    TermQueryBuilder filter = (TermQueryBuilder)boolQueryBuilder.filter().get(0);
    assertEquals(fieldMapper.get (DataSearchParameter.TITLE), filter.fieldName());
    assertEquals("data", filter.value());
  }

}
*/
