#common-search
This library contains common classes used by the GBIF search services backed by Solr. In particular the classes exposed
byt this library can be categorized into the following types:
  
  * Annotations: to be used un Java classes to specify how facets, Solr fields and full text fields are mapped to Java
  fields.
  * Builders: classes to build Solr queries from Java annotated classes and to transform Solr responses into 
  org.gbif.api.model.common.search.SearchResponse. The builder classes transform annotated classes into Solr requests
  for full text, faceted and highlighting search requests.
  * Configuration/Injections: Guice modules and Builder classes to build instances of SolrClient classes for Cloud, Http and 
  Embedded Solr servers.
  * Utility classes: set of classes to process GBIF search annotation, manipulates search parameters and store Solr constants.
  
## How to build the project
Execute the Maven command

```
  mvn clean package install
```

## Notes

  * This project has been updated to use Solr 5.3.X which deprecated the usage of SolrServer in favor of using the SolrClient class.
  * The usage of Solr embedded servers is not recommended anymore, it should be used for testing purposes only.
