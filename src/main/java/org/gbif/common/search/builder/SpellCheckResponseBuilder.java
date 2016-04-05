package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.SpellCheckResponse;
import com.google.common.collect.ImmutableMap;

/**
 * Translates a Solr spell check response into a GBIF spell check response.
 */
public class SpellCheckResponseBuilder {

  /**
   * Utility classes hide constructors.
   */
  private SpellCheckResponseBuilder(){
    //nothing to do
  }

  public static SpellCheckResponse build(org.apache.solr.client.solrj.response.SpellCheckResponse solrSpellCheckResponse){
    SpellCheckResponse spellCheckResponse = new SpellCheckResponse();
    spellCheckResponse.setCorrectlySpelled(solrSpellCheckResponse.isCorrectlySpelled());
    ImmutableMap.Builder<String,SpellCheckResponse.Suggestion> mapBuilder = new ImmutableMap.Builder<String,SpellCheckResponse.Suggestion>();
    if(solrSpellCheckResponse.getSuggestionMap() != null && !solrSpellCheckResponse.getSuggestionMap().isEmpty()) {
      for (org.apache.solr.client.solrj.response.SpellCheckResponse.Suggestion solrSuggestion : solrSpellCheckResponse.getSuggestionMap()
        .values()) {
        SpellCheckResponse.Suggestion suggestion = new SpellCheckResponse.Suggestion();
        suggestion.setNumFound(solrSuggestion.getNumFound());
        suggestion.setAlternatives(solrSuggestion.getAlternatives());
        mapBuilder.put(solrSuggestion.getToken(), suggestion);
      }
    }
    spellCheckResponse.setSuggestions(mapBuilder.build());
    return spellCheckResponse;
  }
}
