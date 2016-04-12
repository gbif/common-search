package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.SpellCheckResponse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

  public static SpellCheckResponse build(org.apache.solr.client.solrj.response.SpellCheckResponse solrSpellCheckResponse) {
    SpellCheckResponse spellCheckResponse = new SpellCheckResponse();
    spellCheckResponse.setCorrectlySpelled(solrSpellCheckResponse.isCorrectlySpelled());
    //If the response contains collations, are use to build the response since those are guarantee to get results
    if (solrSpellCheckResponse.getCollatedResults() != null && !solrSpellCheckResponse.getCollatedResults().isEmpty()) {
      spellCheckResponse.setSuggestions(fromCollatedResults(solrSpellCheckResponse));
    } else if (solrSpellCheckResponse.getSuggestionMap() != null && !solrSpellCheckResponse.getSuggestionMap().isEmpty()) {
      spellCheckResponse.setSuggestions(fromSuggestionsResults(solrSpellCheckResponse));
    }
    return spellCheckResponse;
  }

  /**
   * Creates a suggestions map from collated results.
   */
  private static Map<String,SpellCheckResponse.Suggestion> fromCollatedResults(org.apache.solr.client.solrj.response.SpellCheckResponse solrSpellCheckResponse) {
    Map<String, SpellCheckResponse.Suggestion> suggestionMap = Maps.newHashMap();
    for (org.apache.solr.client.solrj.response.SpellCheckResponse.Collation collation : solrSpellCheckResponse.getCollatedResults()) {
      StringBuilder tokenBuilder = new StringBuilder(collation.getMisspellingsAndCorrections().size());
      StringBuilder spellCorrection = new StringBuilder(collation.getMisspellingsAndCorrections().size());
      for (Iterator<org.apache.solr.client.solrj.response.SpellCheckResponse.Correction> itCorrections =
        collation.getMisspellingsAndCorrections().iterator(); itCorrections.hasNext();) {
        org.apache.solr.client.solrj.response.SpellCheckResponse.Correction correction = itCorrections.next();
        tokenBuilder.append(correction.getOriginal());
        spellCorrection.append(correction.getCorrection());
        if (itCorrections.hasNext()) {
          tokenBuilder.append(' ');
          spellCorrection.append(' ');
        }
      }
      String token = tokenBuilder.toString();
      if (suggestionMap.containsKey(token)) {
        SpellCheckResponse.Suggestion suggestion = suggestionMap.get(token);
        List<String> alternatives = suggestion.getAlternatives();
        alternatives.add(spellCorrection.toString());
        suggestion.setAlternatives(alternatives);
        suggestion.setNumFound(Math.max(suggestion.getNumFound(), Long.valueOf(collation.getNumberOfHits()).intValue()));
      } else {
        SpellCheckResponse.Suggestion suggestion = new SpellCheckResponse.Suggestion();
        suggestion.setAlternatives(Lists.newArrayList(spellCorrection.toString()));
        suggestion.setNumFound(Long.valueOf(collation.getNumberOfHits()).intValue());
        suggestionMap.put(token.toString(), suggestion);
      }
    }
    return suggestionMap;
  }

  /**
   * Creates a suggestions map from the Solr SpellCheckResponse.suggestionMap.
   */
  private static Map<String,SpellCheckResponse.Suggestion> fromSuggestionsResults(org.apache.solr.client.solrj.response.SpellCheckResponse solrSpellCheckResponse) {
    Map<String,SpellCheckResponse.Suggestion> suggestionMap = Maps.newHashMap();
    for (org.apache.solr.client.solrj.response.SpellCheckResponse.Suggestion solrSuggestion : solrSpellCheckResponse.getSuggestionMap()
      .values()) {
      SpellCheckResponse.Suggestion suggestion = new SpellCheckResponse.Suggestion();
      suggestion.setNumFound(solrSuggestion.getNumFound());
      suggestion.setAlternatives(solrSuggestion.getAlternatives());
      suggestionMap.put(solrSuggestion.getToken(), suggestion);
    }
    return suggestionMap;
  }
}
