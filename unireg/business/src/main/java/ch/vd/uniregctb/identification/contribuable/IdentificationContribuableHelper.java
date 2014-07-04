package ch.vd.uniregctb.identification.contribuable;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.tiers.TiersCriteria;

public abstract class IdentificationContribuableHelper {

	public static void updateCriteriaStandard(CriteresPersonne criteres, final TiersCriteria criteria) {
		setUpCriteria(criteria);
		// [UNIREG-1630] dans tous les cas, on doit tenir compte des autres critères (autres que le numéro AVS, donc)
		criteria.setNomRaison(concatCriteres(criteres.getPrenoms(), criteres.getNom()));
	}

	public static void updateCriteriaSansDernierNom(CriteresPersonne criteres, final TiersCriteria criteria) throws NotEnoughWordsException {
		setUpCriteria(criteria);
		//Suppression du dernier Nom
		final String critereNom = sansDernierMot(criteres.getNom(), true);
		if (critereNom == null) {
			throw new NotEnoughWordsException("un seul nom présent");
		}
		criteria.setNomRaison(concatCriteres(criteres.getPrenoms(), critereNom));
	}

	private static String rebuildString(String[] array, int len) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0 ; i < len ; ++ i) {
			b.append(array[i]);
			if (i < len - 1) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	protected static String sansDernierMot(String mots, boolean separerSurTiret) {
		final String[] decomposition = StringUtils.split(mots);
		if (decomposition == null || decomposition.length < 1) {
			return null;
		}
		final String truncated;
		if (decomposition.length == 1) {
			if (separerSurTiret) {
				final String[] decompositionTiret = StringUtils.split(decomposition[0], '-');
				if (decompositionTiret == null || decompositionTiret.length < 2) {
					truncated = null;
				}
				else {
					truncated = rebuildString(decompositionTiret, decompositionTiret.length - 1);
				}
			}
			else {
				truncated = null;
			}
		}
		else {
			truncated = rebuildString(decomposition, decomposition.length - 1);
		}
		return StringUtils.trimToNull(truncated);
	}

	protected static String getMotSansE(String mot) {
		if (mot == null) {
			return null;
		}
		return mot.toLowerCase().replaceAll("([auo])e", "$1");
	}

	public static void updateCriteriaSansDernierPrenom(CriteresPersonne criteres, final TiersCriteria criteria) throws NotEnoughWordsException {
		setUpCriteria(criteria);
		//Suppression du dernier prenom
		final String criterePrenom = sansDernierMot(criteres.getPrenoms(), false);
		if (criterePrenom == null) {
			throw new NotEnoughWordsException("un seul prénom présent");
		}
		criteria.setNomRaison(concatCriteres(criterePrenom, criteres.getNom()));

	}

	public static void updateCriteriaStandardSansE(CriteresPersonne criteres, final TiersCriteria criteria) throws NoEsToRemoveException {
		setUpCriteria(criteria);
		final String criteresPrenom = getMotSansE(criteres.getPrenoms());
		final String criteresNom = getMotSansE(criteres.getNom());
		if (StringUtils.equalsIgnoreCase(criteresNom, criteres.getNom()) && StringUtils.equalsIgnoreCase(criteresPrenom, criteres.getPrenoms())) {
			throw new NoEsToRemoveException("Pas de ae, oe, ue dans les noms et prénoms");
		}
		criteria.setNomRaison(concatCriteres(criteresPrenom, criteresNom));

	}

	public static void updateCriteriaSansDernierNomSansE(CriteresPersonne criteres, final TiersCriteria criteria) throws NoEsToRemoveException, NotEnoughWordsException {
		setUpCriteria(criteria);
		final String criteresPrenom = getMotSansE(criteres.getPrenoms());
		final String criteresNomSansE = getMotSansE(criteres.getNom());
		if (StringUtils.equalsIgnoreCase(criteresNomSansE, criteres.getNom()) && StringUtils.equalsIgnoreCase(criteresPrenom, criteres.getPrenoms())) {
			throw new NoEsToRemoveException("Pas de ae, oe, ue dans les noms et prénoms");
		}
		final String criteresNom = sansDernierMot(criteresNomSansE, true);
		if (criteresNom == null) {
			throw new NotEnoughWordsException("un seul nom présent");
		}
		criteria.setNomRaison(concatCriteres(criteresPrenom, criteresNom));
	}

	public static void updateCriteriaSansDernierPrenomSansE(CriteresPersonne criteres, final TiersCriteria criteria) throws NoEsToRemoveException, NotEnoughWordsException {
		setUpCriteria(criteria);
		final String criteresPrenomSansE = getMotSansE(criteres.getPrenoms());
		final String criteresNom = getMotSansE(criteres.getNom());
		if (StringUtils.equalsIgnoreCase(criteresNom, criteres.getNom()) && StringUtils.equalsIgnoreCase(criteresPrenomSansE, criteres.getPrenoms())) {
			throw new NoEsToRemoveException("Pas de ae, oe, ue dans les noms et prénoms");
		}
		final String criteresPrenom = sansDernierMot(criteresPrenomSansE, false);
		if (criteresPrenom == null) {
			throw new NotEnoughWordsException("un seul prénom présent");
		}
		criteria.setNomRaison(concatCriteres(criteresPrenom, criteresNom));
	}

	public static void setUpCriteria(TiersCriteria criteria) {
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);

		// critères statiques
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
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
