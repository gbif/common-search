package org.gbif.common.search.builder;

import org.gbif.api.model.common.search.SpellCheckResponse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Translates a Solr spell check response into a GBIF spell check response.
 */
public class SpellCheckResponseBuilder {

  private static final Joiner COLLATIONS_JOINER = Joiner.on(' ').skipNulls();

  /**
   * Utility classes hide constructors.
   */
  private SpellCheckResponseBuilder(){
    //nothing to do
  }

  public static SpellCheckResponse build(org.apache.solr.client.solrj.response.SpellCheckResponse solrSpellCheckResponse) {

    SpellCheckResponse spellCheckResponse = new SpellCheckResponse();
    spellCheckResponse.setCorrectlySpelled(solrSpellCheckResponse.isCorrectlySpelled());
    Map<String,SpellCheckResponse.Suggestion> suggestionMap = Maps.newHashMap();
    //If the response contains collations, are use to build the response since those are guarantee to get results
    if (solrSpellCheckResponse.getCollatedResults() != null && !solrSpellCheckResponse.getCollatedResults().isEmpty()) {
      for (org.apache.solr.client.solrj.response.SpellCheckResponse.Collation collation : solrSpellCheckResponse.getCollatedResults()) {
        StringBuilder tokenBuilder = new StringBuilder();
        StringBuilder spellCorrection = new StringBuilder();
        for (Iterator<org.apache.solr.client.solrj.response.SpellCheckResponse.Correction>  itCorrections = collation.getMisspellingsAndCorrections().iterator();
            itCorrections.hasNext();) {
          org.apache.solr.client.solrj.response.SpellCheckResponse.Correction correction = itCorrections.next();
          tokenBuilder.append(correction.getOriginal());
          spellCorrection.append(correction.getCorrection());
          if (itCorrections.hasNext()) {
            tokenBuilder.append(' ');
            spellCorrection.append(' ');
          }
        }
        SpellCheckResponse.Suggestion suggestion = new SpellCheckResponse.Suggestion();
        String token  = tokenBuilder.toString();
        if (suggestionMap.containsKey(token)) {
          List<String> alternatives = suggestionMap.get(token).getAlternatives();
          alternatives.add(spellCorrection.toString());
          suggestion.setAlternatives(alternatives);
          suggestion.setNumFound(Math.max(suggestion.getNumFound(),new Long(collation.getNumberOfHits()).intValue()));
        } else {
          suggestion.setAlternatives(Lists.newArrayList(spellCorrection.toString()));
          suggestionMap.put(token.toString(), suggestion);
          suggestion.setNumFound(new Long(collation.getNumberOfHits()).intValue());
        }

      }
    } else if (solrSpellCheckResponse.getSuggestionMap() != null && !solrSpellCheckResponse.getSuggestionMap().isEmpty()) {
      for (org.apache.solr.client.solrj.response.SpellCheckResponse.Suggestion solrSuggestion : solrSpellCheckResponse.getSuggestionMap()
        .values()) {
        SpellCheckResponse.Suggestion suggestion = new SpellCheckResponse.Suggestion();
        suggestion.setNumFound(solrSuggestion.getNumFound());
        suggestion.setAlternatives(solrSuggestion.getAlternatives());
        suggestionMap.put(solrSuggestion.getToken(), suggestion);
      }
    }
    spellCheckResponse.setSuggestions(suggestionMap);
    return spellCheckResponse;
  }
}
