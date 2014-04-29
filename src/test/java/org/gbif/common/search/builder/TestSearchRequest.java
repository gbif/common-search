package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.vocabulary.Rank;

public class TestSearchRequest extends FacetedSearchRequest<TestSearchParameter> {

  public void addRankFilter(Rank rank) {
    addParameter(TestSearchParameter.RANK, rank);
  }

}
