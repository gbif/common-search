package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.ThreatStatus;

import java.util.UUID;

/**
 * Example facet class.
 */
public enum TestSearchParameter implements SearchParameter {
  CHECKLIST(UUID.class),
  RANK(Rank.class),
  HIGHERTAXON(Integer.class),
  EXTINCT(Boolean.class),
  HABITAT(String.class),
  THREAT(ThreatStatus.class);

  private final Class<?> type;

  private TestSearchParameter(Class<?> type) {
    this.type = type;
  }

  @Override
  public Class<?> type() {
    return type;
  }
}
