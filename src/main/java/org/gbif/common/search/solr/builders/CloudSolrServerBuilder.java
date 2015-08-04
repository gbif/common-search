package org.gbif.common.search.solr.builders;

import java.net.MalformedURLException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;

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
   * Builds a {@link LBHttpSolrClient} instance with a default collection name and pointing to a Zookeeper server.
   */
  public SolrClient build() {
    checkNotNull(zkHost);
    checkNotNull(defaultCollection);
    // Creates the load-balanced SolrServer.
    return buildCloudSolrServer(buildLBHttpSolrClient());
  }

  /**
   * Default Solr collection name.
   */
  public CloudSolrServerBuilder withDefaultCollection(String defaultCollection) {
    this.defaultCollection = defaultCollection;
    return this;
  }

  /**
   * List of Solr servers to be used in the {@link LBHttpSolrClient} instance creation.
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
   * Creates the {@link CloudSolrClient}. Uses the {@link LBHttpSolrClient} parameter (if not is null), the default
   * collection name and the Zookeeper server url.
   */
  private CloudSolrClient buildCloudSolrServer(LBHttpSolrClient lbHttpSolrClient) {
    CloudSolrClient cloudSolrClient;
    if (lbHttpSolrClient != null) {
      cloudSolrClient = new CloudSolrClient(zkHost, lbHttpSolrClient);
    } else {
      cloudSolrClient = new CloudSolrClient(zkHost);
    }
    cloudSolrClient.setDefaultCollection(defaultCollection);
    return cloudSolrClient;
  }

  /**
   * Builds a {@link LBHttpSolrClient} instance using the list of HTTP Solr servers.
   */
  private LBHttpSolrClient buildLBHttpSolrClient() {
    LBHttpSolrClient lbHttpSolrServer = null;
    try {
      if (httpLBServers != null && httpLBServers.length > 0) {
        return new LBHttpSolrClient(httpLBServers);
      }
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
    return lbHttpSolrServer;
  }

}
