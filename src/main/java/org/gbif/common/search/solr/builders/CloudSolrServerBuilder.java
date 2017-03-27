package org.gbif.common.search.solr.builders;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder class for {@link org.apache.solr.client.solrj.SolrClient} instances. The server instances requires, mandatory, a valid url to a
 * Zookeeper server and the default collection name. Additionally a list of SolrHttpServers can be specified to create a
 * pool of load balanced servers.
 */
public class CloudSolrServerBuilder {

  // Zookeeper server url
  private String zkHost;

  // Default collection name
  private String defaultCollection;

  // Solr Id field, used to route documents in shards
  private String idField;

  /**
   * Builds a {@link LBHttpSolrClient} instance with a default collection name and pointing to a Zookeeper server.
   */
  public SolrClient build() {
    checkNotNull(zkHost);
    checkNotNull(defaultCollection);
    // Creates the load-balanced SolrServer.
    CloudSolrClient cloudSolrClient = new CloudSolrClient(zkHost);
    cloudSolrClient.setDefaultCollection(defaultCollection);
    if (idField != null) {
      cloudSolrClient.setIdField(idField);
    }
    return cloudSolrClient;
  }

  /**
   * Default Solr collection name.
   */
  public CloudSolrServerBuilder withDefaultCollection(String defaultCollection) {
    this.defaultCollection = defaultCollection;
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
   * Zookeeper sever url.
   */
  public CloudSolrServerBuilder withIdField(String idField) {
    this.idField = idField;
    return this;
  }

}
