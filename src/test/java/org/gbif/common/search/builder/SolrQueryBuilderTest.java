package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.vocabulary.Rank;

import java.util.Set;
import java.util.UUID;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrQueryBuilderTest {

  private SuggestQueryStringBuilder queryStringBuilder;

  @Test
  public void buildSearchRequestCombined() {
    TestSearchRequest req = new TestSearchRequest();
    req.addParameter(TestSearchParameter.HIGHERTAXON, "111");
    req.addParameter(TestSearchParameter.RANK, Rank.SPECIES);
    req.addParameter(TestSearchParameter.RANK, Rank.VARIETY);
    req.addParameter(TestSearchParameter.EXTINCT, true);
    req.addFacets(TestSearchParameter.HIGHERTAXON, TestSearchParameter.RANK);

    SolrQuery query = buildQuery(req);

    Set<String> facets = Sets.newHashSet(query.getFacetFields());
    assertEquals(2, facets.size());
    assertTrue(facets.contains("rank"));
    assertTrue(facets.contains("higher_taxon_nub_key"));
    assertFilterQueryContains(query, "{!tag=ffqextinct}extinct:true");
    assertFilterQueryContains(query, "{!tag=ffqrank}(rank:30 OR rank:26)");
    assertFilterQueryContains(query, "{!tag=ffqhigher_taxon_nub_key}higher_taxon_nub_key:111");
  }

  @Test
  public void buildSearchRequestExample() {
    TestSearchRequest searchRequest = new TestSearchRequest();
    searchRequest.addRankFilter(Rank.SUBGENUS);

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertNull(solrQuery.getFacetFields());
    assertFilterQueryContains(solrQuery, "{!tag=ffqrank}rank:20");
    assertNotNull(solrQuery.getFilterQueries());
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

    SolrQuery query = buildQuery(searchRequest);
    assertNotNull(query.getFilterQueries());
    assertNotNull(query.getFacetFields());
    assertEquals(1, query.getFilterQueries().length);
    final String x = query.getFilterQueries()[0];
    assertTrue(x.contains("checklist_key:" + uuid1.toString()));
    assertTrue(x.contains("checklist_key:" + uuid2.toString()));
    // the search example doesnt have THREAT mapped as a facet, only regular param
    assertEquals(TestSearchParameter.values().length - 1, query.getFacetFields().length);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildSearchRequestFacetedIllegal() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.setQ("puma");
    searchRequest.addFacets(TestSearchParameter.values());
    searchRequest.setMultiSelectFacets(true);
    searchRequest.addParameter(TestSearchParameter.CHECKLIST, "chk1");
    searchRequest.addParameter(TestSearchParameter.CHECKLIST, "chk2");

    buildQuery(searchRequest);
  }

  @Test
  public void buildSearchRequestFacetedNull() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.setQ("puma");
    searchRequest.addFacets(TestSearchParameter.CHECKLIST, TestSearchParameter.RANK);
    searchRequest.setMultiSelectFacets(true);

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertNull(solrQuery.getFilterQueries());
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildSearchRequestIllegalInteger() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.addParameter(TestSearchParameter.HIGHERTAXON, "1aha");
    buildQuery(searchRequest);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildSearchRequestIllegalRank() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.addParameter(TestSearchParameter.RANK, "aha");
    buildQuery(searchRequest);
  }

  @Test
  public void buildSearchRequestInteger() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.addParameter(TestSearchParameter.HIGHERTAXON, "111");
    buildQuery(searchRequest);
  }

  @Test
  public void buildSearchRequestRank() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.addParameter(TestSearchParameter.RANK, Rank.SPECIES);
    buildQuery(searchRequest);
  }

  @Test
  public void buildSimpleSearch() {
    SearchRequest<TestSearchParameter> searchRequest = new SearchRequest<TestSearchParameter>("tim");
    searchRequest.addParameter(TestSearchParameter.RANK, Rank.SUBGENUS);

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertNull(solrQuery.getFacetFields());
    assertNotNull(solrQuery.getFilterQueries());
    assertFalse(solrQuery.toString().contains("ffq"));
  }

  @Before
  public void prepareMocks() {
    queryStringBuilder = mock(SuggestQueryStringBuilder.class);
    when(queryStringBuilder.build(any(String.class))).thenReturn("*");
    when(queryStringBuilder.build(any(SearchRequest.class))).thenReturn("*");
  }


  private void assertFilterQueryContains(SolrQuery query, String filter) {
    for (String fq : query.getFilterQueries()) {
      if (filter.equals(fq)) {
        return;
      }
    }
    fail("Filter query does not contain " + filter + " : " + Joiner.on(" ").join(query.getFilterQueries()));
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
