package org.gbif.common.search.solr;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.solr.client.solrj.SolrClient;


/**
 * Trivial guice module providing a remote or embedded Solr server based on the modules configuration.
 * The only class exposed is SolrClient, optionally bound to a named key.
 *
 */
public class SolrModule extends AbstractModule {

  private final Key<SolrClient> bindingKey;
  private final SolrConfig cfg;

  public SolrModule(SolrConfig cfg) {
    this(cfg, null);
  }

  public SolrModule(SolrConfig cfg, String solrBindingName) {
    this.cfg = cfg;
    if (solrBindingName != null) {
      bindingKey = Key.get(SolrClient.class, Names.named(solrBindingName));
    } else {
      bindingKey = null;
    }
  }

  public Key<SolrClient> bindingKey() {
    return bindingKey;
  }

  @Override
  protected void configure() {
    bind(SolrClient.class).toInstance(cfg.buildSolr());
    if (bindingKey != null) {
      bind(bindingKey).to(SolrClient.class);
    }
  }
}
