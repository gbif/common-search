package org.gbif.common.search.builder;

import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.SearchMapping;
import org.gbif.common.search.model.SuggestMapping;
import org.gbif.common.search.model.WildcardPadding;

/**
 * Annotated class for search operations.
 */
@SearchMapping(
  facets = {
    @FacetField(name = "CHECKLIST", field = "checklist_key"),
    @FacetField(name = "RANK", field = "rank"),
    @FacetField(name = "HIGHERTAXON", field = "higher_taxon_nub_key"),
    @FacetField(name = "EXTINCT", field = "extinct"),
    @FacetField(name = "HABITAT", field = "marine")},
  fulltextFields = {
    @FullTextSearchField(exactMatchScore = 100.0d, partialMatching = WildcardPadding.NONE, field = "canonical_name"),
    @FullTextSearchField(partialMatching = WildcardPadding.NONE, field = "class"),
    @FullTextSearchField(exactMatchScore = 0.3d, partialMatching = WildcardPadding.NONE, field = "description_ft"),
    @FullTextSearchField(exactMatchScore = 2.0d, partialMatching = WildcardPadding.NONE, field = "family"),
    @FullTextSearchField(exactMatchScore = 2.0d, partialMatching = WildcardPadding.NONE, field = "genus"),
    @FullTextSearchField(partialMatching = WildcardPadding.NONE, field = "kingdom"),
    @FullTextSearchField(partialMatching = WildcardPadding.NONE, field = "order"),
    @FullTextSearchField(partialMatching = WildcardPadding.NONE, field = "phylum"),
    @FullTextSearchField(partialMatching = WildcardPadding.BOTH, exactMatchScore = 10.0d, partialMatchScore = 0.2d,
      field = "scientific_name_ft"),
    @FullTextSearchField(partialMatching = WildcardPadding.NONE, exactMatchScore = 5d, field = "species"),
    @FullTextSearchField(partialMatching = WildcardPadding.NONE, exactMatchScore = 3d, field = "subgenus"),
    @FullTextSearchField(exactMatchScore = 6d, partialMatchScore = 0.2d, partialMatching = WildcardPadding.BOTH,
      field = "vernacular_name_ft")})
@SuggestMapping(field = "canonical_name", phraseQueryField = "canonical_name_auto")
public class TestSearchResult {

}
