package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchRequest;

import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SuggestQueryStringBuilderTest {

  private final SuggestQueryStringBuilder queryStringBuilder = SuggestQueryStringBuilder.create(TestSearchResult.class);

  @Test
  public void buildEmpty() {
    assertEquals("(canonical_name_auto:*^1000) OR (canonical_name:*^300)", queryStringBuilder.build(""));
  }

  @Test
  public void buildPhraseQuery() {
    assertEquals(
      "(canonical_name_auto:\"puma con\"^1000 OR canonical_name_auto:puma^1000) OR (canonical_name:puma^300 OR canonical_name:con^200)",
      queryStringBuilder.build("puma con"));
    assertEquals(
      "(canonical_name_auto:\"Puma concolor,\"^1000 OR canonical_name_auto:Puma^1000) OR (canonical_name:Puma^300 OR canonical_name:concolor,^200)",
      queryStringBuilder.build("Puma concolor,"));
  }

  @Test
  public void buildSearchRequest() {
    SolrQuery solrQuery = buildQuery(new SearchRequest<TestSearchParameter>("puma"));
    assertEquals("(canonical_name_auto:puma^1000) OR (canonical_name:puma^300)", solrQuery.getQuery());
  }

  @Test
  public void buildSearchRequestFaceted() {
    final UUID uuid1 = UUID.randomUUID();
    final UUID uuid2 = UUID.randomUUID();

    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.setQ("puma");
    searchRequest.addFacets(TestSearchParameter.values());
    searchRequest.setMultiSelectFacets(true);
    searchRequest.addParameter(TestSearchParameter.CHECKLIST, uuid1.toString());
    searchRequest.addParameter(TestSearchParameter.CHECKLIST, uuid2.toString());

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertEquals("(canonical_name_auto:puma^1000) OR (canonical_name:puma^300)", solrQuery.getQuery());
  }

  @Test
  public void buildSimple() {
    assertEquals("(canonical_name_auto:puma^1000) OR (canonical_name:puma^300)", queryStringBuilder.build("puma"));
  }

  private SolrQuery buildQuery(FacetedSearchRequest<TestSearchParameter> searchRequest) {
    SolrQueryBuilder<TestSearchResult, TestSearchParameter> requestBuilder =
      SolrQueryBuilder.create(TestSearchParameter.class, TestSearchResult.class, false);
    requestBuilder.withQueryBuilder(queryStringBuilder);
    return requestBuilder.build(searchRequest);
  }

  private SolrQuery buildQuery(SearchRequest<TestSearchParameter> searchRequest) {
    SolrQueryBuilder<TestSearchResult, TestSearchParameter> requestBuilder =
      SolrQueryBuilder.create(TestSearchParameter.class, TestSearchResult.class, false);
    requestBuilder.withQueryBuilder(queryStringBuilder);
    return requestBuilder.build(searchRequest);
  }

}
