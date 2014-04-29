package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.search.util.SolrConstants;

import java.util.regex.Matcher;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for common search operations.
 */
public class FullTextQueryStringBuilderTest {

  private final FullTextQueryStringBuilder queryStringBuilder = FullTextQueryStringBuilder
    .create(TestSearchResult.class);
  private final String EXPECTED_PUMA_QUERY =
    "(canonical_name:puma^100.0 OR class:puma^1.0 OR description_ft:puma^0.3 OR family:puma^2.0 OR genus:puma^2.0 OR kingdom:puma^1.0 OR order:puma^1.0 OR phylum:puma^1.0 OR scientific_name_ft:puma^10.0 OR scientific_name_ft:*puma*^0.2 OR species:puma^5.0 OR subgenus:puma^3.0 OR vernacular_name_ft:puma^6.0 OR vernacular_name_ft:*puma*^0.2)";

  @Test
  public void buildFullTextTest() {
    String query = queryStringBuilder.build("puma");
    assertTrue(query.contains("*"));
  }


  @Test
  public void escapeSpecialChars() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.addFacets(TestSearchParameter.values());
    searchRequest.setMultiSelectFacets(true);
    searchRequest.setQ("puma-con");
    SolrQuery solrQuery = buildQuery(searchRequest);
    assertEquals(EXPECTED_PUMA_QUERY.replaceAll("puma", "puma\\\\-con"), solrQuery.getQuery());
  }

  @Test
  public void buildPhraseQueryFullTextTest() {
    String query = queryStringBuilder.build("puma concolor");
    assertFalse(query.contains("*"));
  }

  @Test
  public void buildSearchRequestFaceted() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.setQ("puma");
    searchRequest.addFacets(TestSearchParameter.values());
    searchRequest.setMultiSelectFacets(true);

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertEquals(EXPECTED_PUMA_QUERY, solrQuery.getQuery());
  }

  @Test
  public void buildSearchRequestFacetedNull() {
    FacetedSearchRequest<TestSearchParameter> searchRequest = new FacetedSearchRequest<TestSearchParameter>();
    searchRequest.setQ("puma");
    searchRequest.addFacets(TestSearchParameter.CHECKLIST, TestSearchParameter.RANK);
    searchRequest.setMultiSelectFacets(true);

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertEquals(EXPECTED_PUMA_QUERY, solrQuery.getQuery());
  }

  @Test
  public void buildTagFacetParamTest() {
    String result = SolrConstants.FACET_FILTER_EX.replace(SolrConstants.TAG_FIELD_PARAM, "field");
    String resultWrong =
      SolrConstants.FACET_FILTER_EX.replace(SolrConstants.TAG_FIELD_PARAM, "field").replace("{", " ");
    Matcher matcherOK = SolrConstants.FACET_FILTER_RGEX.matcher(result);
    Matcher matcherError = SolrConstants.FACET_FILTER_RGEX.matcher(resultWrong);
    Matcher matcherCleaner = SolrConstants.FACET_FILTER_RGEX_CLEAN.matcher(result);
    assertTrue(matcherOK.matches());
    assertFalse(matcherError.matches());
    assertEquals("field", matcherCleaner.replaceAll(""));
    assertNotNull(result);
  }


  @Test
  public void builSearchRequestExample() {
    TestSearchRequest searchRequest = new TestSearchRequest();
    searchRequest.addRankFilter(Rank.SUBGENUS);

    SolrQuery solrQuery = buildQuery(searchRequest);
    assertEquals(SolrConstants.DEFAULT_QUERY, solrQuery.getQuery());
  }

  private SolrQuery buildQuery(FacetedSearchRequest<TestSearchParameter> searchRequest) {
    SolrQueryBuilder<TestSearchResult, TestSearchParameter> requestBuilder =
      SolrQueryBuilder.create(TestSearchParameter.class, TestSearchResult.class, false);
    requestBuilder.withQueryBuilder(queryStringBuilder);
    return requestBuilder.build(searchRequest);
  }


}
