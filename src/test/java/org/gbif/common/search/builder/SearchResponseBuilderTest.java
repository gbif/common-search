package org.gbif.common.search.builder;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.search.SearchResponse;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResponseBuilderTest {

  /**
   * Tests that returned ordinal facet values are converted to actual enum names.
   */
  @Test
  public void testBuild() throws Exception {
    final SearchResponseBuilder builder =
      SearchResponseBuilder.create(TestSearchResult.class, TestSearchResult.class, TestSearchParameter.class);

    PagingRequest req = new PagingRequest(0,5);
    QueryResponse queryResponse = mock(QueryResponse.class);
    SolrDocumentList solrList = new SolrDocumentList();
    when(queryResponse.getResults()).thenReturn(solrList);
    List<FacetField> facets = Lists.newArrayList();
    FacetField rankFacet = new FacetField("rank");
    rankFacet.add("19", 87); // genus
    rankFacet.add("14", 14); // family
    rankFacet.add("1", 8);  // kingdom
    facets.add(rankFacet);
    when(queryResponse.getFacetFields()).thenReturn(facets);

    SearchResponse<TestSearchResult, TestSearchParameter> resp = builder.build(req, queryResponse);

    assertEquals(1, resp.getFacets().size());
    assertEquals(TestSearchParameter.RANK, resp.getFacets().get(0).getField());
    assertEquals(3, resp.getFacets().get(0).getCounts().size());
    assertEquals("GENUS", resp.getFacets().get(0).getCounts().get(0).getName() );
    assertEquals(87l, resp.getFacets().get(0).getCounts().get(0).getCount().longValue());
    assertEquals("FAMILY", resp.getFacets().get(0).getCounts().get(1).getName() );
    assertEquals(14l, resp.getFacets().get(0).getCounts().get(1).getCount().longValue());
    assertEquals("KINGDOM", resp.getFacets().get(0).getCounts().get(2).getName() );
    assertEquals(8l, resp.getFacets().get(0).getCounts().get(2).getCount().longValue());
  }
}
