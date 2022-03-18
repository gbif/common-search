package org.gbif.common.search.test;

import org.gbif.api.vocabulary.Country;

import java.util.Date;

import lombok.Builder;

@lombok.Data
@Builder
public class Data {

  private Integer key;
  private Date created;
  private String title;
  private Country country;

}
