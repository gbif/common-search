package org.gbif.common.search.solr.builders;

import java.net.MalformedURLException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder class for {@link CloudSolrServer} instances. The server instances requires, mandatory, a valid url to a
 * Zookeeper server and the default collection name. Additionally a list of SolrHttpServers can be specified to create a
 * pool of load balanced servers.
 */
public class CloudSolrServerBuilder {

  // List of load-balanced Solr servers
  private String[] httpLBServers;

  // Zookeeper server url
  private String zkHost;

  // Default collection name
  private String defaultCollection;

  /**
   * Builds a {@link CloudSolrServer} instance with a default collection name and pointing to a Zookeeper server.
   */
  public SolrServer build() {
    checkNotNull(zkHost);
    checkNotNull(defaultCollection);
    // Creates the load-balanced SolrServer.
    LBHttpSolrServer lbHttpSolrServer = buildLBHttpSolrServer();
    return buildCloudSolrServer(lbHttpSolrServer);
  }

  /**
   * Default Solr collection name.
   */
  public CloudSolrServerBuilder withDefaultCollection(String defaultCollection) {
    this.defaultCollection = defaultCollection;
    return this;
  }

  /**
   * List of Solr servers to be used in the {@link LBHttpSolrServer} instance creation.
   */
  public CloudSolrServerBuilder withHttpLBServers(String... httpLBServers) {
    this.httpLBServers = httpLBServers;
    return this;
  }


  /**
   * Zookeeper sever url.
   */
  public CloudSolrServerBuilder withZkHost(String zkHost) {
    this.zkHost = zkHost;
    return this;
  }

  /**
   * Creates the {@link CloudSolrServer}. Uses the {@link LBHttpSolrServer} parameter (if not is null), the default
   * collection name and the Zookeeper server url.
   */
  private CloudSolrServer buildCloudSolrServer(LBHttpSolrServer lbHttpSolrServer) {
    CloudSolrServer cloudSolrServer;
    if (lbHttpSolrServer != null) {
      cloudSolrServer = new CloudSolrServer(zkHost, lbHttpSolrServer);
    } else {
      cloudSolrServer = new CloudSolrServer(zkHost);
    }
    cloudSolrServer.setDefaultCollection(defaultCollection);
    return cloudSolrServer;
  }

  /**
   * Builds a {@link LBHttpSolrServer} instance using the list of HTTP Solr servers.
   */
  private LBHttpSolrServer buildLBHttpSolrServer() {
    LBHttpSolrServer lbHttpSolrServer = null;
    try {
      if (httpLBServers != null && httpLBServers.length > 0) {
        return new LBHttpSolrServer(httpLBServers);
      }
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
    return lbHttpSolrServer;
  }

}
