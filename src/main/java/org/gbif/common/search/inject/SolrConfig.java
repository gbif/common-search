package org.gbif.common.search.inject;

import org.gbif.common.search.solr.SolrServerType;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Configurations for solr with optional injection that allow us to provide defaults for a temporary embedded server.
 */
public class SolrConfig {

  @Inject
  @Named("solr.server.type")
  public SolrServerType serverType;

  @Inject(optional = true)
  @Named("solr.server")
  public String serverHome = null;

  @Inject(optional = true)
  @Named("solr.collection")
  public String collection = null;

  @Inject(optional = true)
  @Named("solr.server.httpLbservers")
  public String httpLBservers = null;


  @Inject(optional = true)
  @Named("solr.delete")
  public boolean deleteOnExit = false;
}
