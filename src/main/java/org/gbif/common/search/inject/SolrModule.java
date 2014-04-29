package org.gbif.common.search.inject;

import org.gbif.common.search.solr.SolrServerType;
import org.gbif.common.search.solr.builders.CloudSolrServerBuilder;
import org.gbif.common.search.solr.builders.EmbeddedServerBuilder;

import java.net.MalformedURLException;

import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Guice module providing a remote or embedded Solr server based on the modules configuration.
 * Expects 5 properties to be bound:
 * solr.server.type: must be set using literals available in {@link SolrServerType}, being SolrServerType.HTTP the
 * default.
 * solr.server: a http url to a remote Solr server, path for an embedded Solr or Zookeeper server url.
 * solr.collection: Solr collection name (required by {@link CloudSolrServerBuilder})
 * solr.server.http_lbservers: list of Solr http servers to create instance of {@link LBHttpSolrServer} (optional for
 * {@link CloudSolrServerBuilder}).
 * If empty an embedded server in /tmp will be created
 * solr.delete: if null or true deletes any embedded server artifacts after java shuts down.
 */
public class SolrModule extends PrivateModule {

  /**
   * Configurations for solr with optional injection that allow us to provide defaults for a temporary embedded server.
   */
  static class SolrConfig {

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

  private static final Logger LOG = LoggerFactory.getLogger(SolrModule.class);


  /**
   * Provides a Solr server instance based on the SolrServerType.
   */
  @Provides
  @Singleton
  @Inject
  public SolrServer providerSolr(SolrConfig cfg) {
    if (cfg.serverType == SolrServerType.EMBEDDED) {
      LOG.info("Using embedded solr server {}", cfg.serverHome);
      EmbeddedServerBuilder serverBuilder = new EmbeddedServerBuilder()
        .withServerHomeDir(cfg.serverHome).withDeleteOnExit(cfg.deleteOnExit);
      return serverBuilder.build();
    } else if (cfg.serverType == SolrServerType.LBHTTP) {
      try {
        LOG.info("Using remote load-balanced solr server {}", cfg.serverHome);
        return new LBHttpSolrServer(cfg.serverHome);
      } catch (MalformedURLException e) {
        throw new IllegalStateException(e);
      }
    } else if (cfg.serverType == SolrServerType.CLOUD) {
      CloudSolrServerBuilder serverBuilder = new CloudSolrServerBuilder()
        .withDefaultCollection(cfg.collection).withHttpLBServers(cfg.httpLBservers)
        .withZkHost(cfg.serverHome);
      return serverBuilder.build();
    } else { // cfg.serverType == SolrServerType.HTTP)
      LOG.info("Using remote solr server {}", cfg.serverHome);
      return new HttpSolrServer(cfg.serverHome);
    }
  }

  @Override
  protected void configure() {
    bind(SolrConfig.class).in(Scopes.SINGLETON);
    expose(SolrServer.class);
  }
}
