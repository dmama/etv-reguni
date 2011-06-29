package ch.vd.uniregctb.identification.contribuable;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class IdentificationContribuableHelper {

	public void updateCriteriaStandard(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		// [UNIREG-1630] dans tous les cas, on doit tenir compte des autres critères (autres que le numéro AVS, donc)
		criteria.setNomRaison(concatCriteres(criteres.getPrenoms(), criteres.getNom()));

	}

	public void updateCriteriaSansDernierNom(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		//Suppression du dernier Nom
		final String critereNom = getPremierMot(criteres.getNom());
		criteria.setNomRaison(concatCriteres(criteres.getPrenoms(), critereNom));

	}

	public String getPremierMot(String mot) {
		if (mot == null) {
			return null;
		}
		final String motMinuscule = mot.toLowerCase();
		String[] tabMots = StringUtils.split(motMinuscule);
		if (tabMots.length == 1) {
			//on recherche d'autres type de séparation
			tabMots = StringUtils.split(mot, "-");

		}
		return tabMots[0];
	}

	public String getMotSansE(String mot) {

		if(mot ==null){
			return null;
		}
		final String motMinuscule = mot.toLowerCase();
		final String sansAe = StringUtils.replace(motMinuscule, "ae", "a");
		final String sansUe = StringUtils.replace(sansAe, "ue", "u");
		final String result = StringUtils.replace(sansUe, "oe", "o");
		return result;

	}

	public void updateCriteriaSansDernierPrenom(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		//Suppression du dernier prenom
		final String criterePrenom = getPremierMot(criteres.getPrenoms());
		criteria.setNomRaison(concatCriteres(criterePrenom, criteres.getNom()));

	}


	public void updateCriteriaStandardSansE(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		final String criteresPrenom = getMotSansE(criteres.getPrenoms());
		final String criteresNom = getMotSansE(criteres.getNom());
		criteria.setNomRaison(concatCriteres(criteresPrenom, criteresNom));

	}

	public void updateCriteriaSansDernierNomSansE(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		final String criteresPrenom = getMotSansE(criteres.getPrenoms());
		final String criteresNomSansE = getMotSansE(criteres.getNom());
		final String criteresNom = getPremierMot(criteresNomSansE);
		criteria.setNomRaison(concatCriteres(criteresPrenom, criteresNom));

	}

	public void updateCriteriaSansDernierPrenomSansE(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		final String criteresPrenomSansE = getMotSansE(criteres.getPrenoms());
		final String criteresPrenom = getPremierMot(criteresPrenomSansE);
		final String criteresNom = getMotSansE(criteres.getNom());
		criteria.setNomRaison(concatCriteres(criteresPrenom, criteresNom));

	}


	public void setUpCriteria(TiersCriteria criteria) {
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);


		// critères statiques
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		criteria.setTypeTiers(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		criteria.setTypeVisualisation(TiersCriteria.TypeVisualisation.COMPLETE);
	}


	private static String concatCriteres(final String first, final String second) {
		final String concat;
		if (first != null || second != null) {
			StringBuilder s = new StringBuilder();
			if (first != null) {
				s.append(first);
			}
			if (first != null && second != null) {
				s.append(' ');
			}
			if (second != null) {
				s.append(second);
			}
			concat = s.toString();
		}
		else {
			concat = null;
		}
		return concat;
	}
}
