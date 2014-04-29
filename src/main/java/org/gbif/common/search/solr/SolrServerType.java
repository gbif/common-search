package org.gbif.common.search.solr;


/**
 * Supported SolrServer types.
 */
public enum SolrServerType {
  // Embedded instance
  EMBEDDED,
  // HTTP Solr server (default)
  HTTP,
  // LoadBalanced Http Server
  LBHTTP,
  // CloudSolr server
  CLOUD;
}
