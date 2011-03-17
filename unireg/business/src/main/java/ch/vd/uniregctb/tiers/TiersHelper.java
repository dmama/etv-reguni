package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.adresse.AdressesTiers;
import ch.vd.uniregctb.adresse.AdressesTiersHisto;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class TiersHelper {

	/**
	 * Extrait les adresses surchargées au niveau fiscal (= adresse tiers) actives à la date donnée et pour le tiers spécifiés.
	 *
	 * @param tiers
	 *            le tiers dont on veut rechercher les adresses.
	 * @param date
	 *            la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @return les adresses trouvées, ou null autrement.
	 */
	public static AdressesTiers getAdressesTiers(Tiers tiers, RegDate date) throws AdressesResolutionException {

		if (tiers == null) {
			return null;
		}

		AdressesTiers adressesAt = null;

		final Set<AdresseTiers> adresses = tiers.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers adresse : adresses) {

				if (adresse.isValidAt(date)) {

					if (adressesAt == null) { // création à la demande
						adressesAt = new AdressesTiers();
					}
					adressesAt.set(adresse);
				}
			}
		}

		return adressesAt;
	}

	/**
	 * Extrait l'adresse surchargée au niveau fiscal (= adresse tiers) active à la date donnée, pour le type d'adresse et pour le tiers
	 * spécifiés.
	 *
	 * @param tiers
	 *            le tiers dont on veut extraire une adresse.
	 * @param type
	 *            le type d'adresse recherché
	 * @param date
	 *            la date de référence, ou null pour obtenir l'adresse courante.
	 * @return une adresse tiers, ou null autrement.
	 */
	public static AdresseTiers getAdresseTiers(Tiers tiers, TypeAdresseTiers type, RegDate date) {

		if (tiers == null) {
			return null;
		}

		final Set<AdresseTiers> adresses = tiers.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers adresse : adresses) {
				if (adresse.getUsage() == type && adresse.isValidAt(date)) {
					return adresse;
				}
			}
		}

		return null;
	}

	/**
	 * Extrait l'historique de toutes les adresses surchargées au niveau fiscal pour le tiers spécifié.
	 *
	 * @return les adresses trouvées, ou null autrement.
	 */
	public static AdressesTiersHisto getAdressesTiersHisto(Tiers tiers) {

		if (tiers == null) {
			return null;
		}

		AdressesTiersHisto resultat = new AdressesTiersHisto();

		final List<AdresseTiers> adresses = tiers.getAdressesTiersSorted();
		if (adresses != null) {
			for (AdresseTiers adresse : adresses) {
				resultat.add(adresse);
			}
		}

		return resultat;
	}

	/**
	 * Retourne le rapport-entre-tiers du type et à la date spécifiés, et dont le tiers est le sujet.
	 *
	 * @param tiers
	 *            le tiers dont on veut chercher un rapport.
	 * @param type
	 *            le type de rapport voulu.
	 * @param date
	 *            la date de référence, ou <b>null</b> pour obtenir le rapport-entre-tiers actif.
	 * @return un rapport-entre-tiers, ou <b>null</b> si aucun rapport ne correspond aux critères.
	 */
	public static RapportEntreTiers getRapportSujetOfType(Tiers tiers, TypeRapportEntreTiers type, RegDate date) {

		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (type == rapport.getType() && rapport.isValidAt(date)) {
					return rapport;
				}
			}
		}
		return null;
	}

	/**
	 * @param tiers un tiers dont on veut tester la mise sous tutelle, ou non.
	 * @param date  la date à laquelle on veut faire le test
	 * @return <b>vrai</b> si le tiers est sous tutelle à la date donnée; <b>faux</b> autrement.
	 */
	public static boolean estSousTutelle(Tiers tiers, RegDate date) {
		return getRapportSujetOfType(tiers, TypeRapportEntreTiers.TUTELLE, date) != null;
	}

	/**
	 * @param tiers un tiers dont on veut tester la mise sous curatelle, ou non.
	 * @param date  la date à laquelle on veut faire le test
	 * @return <b>vrai</b> si le tiers est sous curatelle à la date donnée; <b>faux</b> autrement.
	 */
	public static boolean estSousCuratelle(Tiers tiers, RegDate date) {
		return getRapportSujetOfType(tiers, TypeRapportEntreTiers.CURATELLE, date) != null;
	}

	/**
	 * @param tiers un tiers dont on veut tester la mise sous curatelle/tutelle, ou non.
	 * @param date  la date à laquelle on veut faire le test
	 * @return <b>vrai</b> si le tiers est sous curatelle ou tutelle à la date donnée; <b>faux</b> autrement.
	 */
	public static boolean estSousCuratelleOuTutelle(Tiers tiers, RegDate date) {
		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (!rapport.isValidAt(date)) {
					continue;
				}
				if (rapport.getType() == TypeRapportEntreTiers.CURATELLE || rapport.getType() == TypeRapportEntreTiers.TUTELLE) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param tiers un tiers dont on veut tester l'existence d'un représentant avec exécution forcée
	 * @param date  la date à laquelle on veut faire le test
	 * @return <b>vrai</b> si le tiers possède un représentant avec exécution forcée à la date donnée; <b>faux</b> autrement.
	 */
	public static boolean possedeRepresentantAvecExecutionForcee(Tiers tiers, RegDate date) {
		final RepresentationConventionnelle representation = (RepresentationConventionnelle) getRapportSujetOfType(tiers, TypeRapportEntreTiers.REPRESENTATION, date);
		return representation != null && representation.getExtensionExecutionForcee() == Boolean.TRUE;
	}

	/**
	 * Retourne l'historique de tous les rapport-entre-tiers du type spécifiés, et dont le tiers est le sujet.
	 *
	 * @param tiers
	 *            le tiers dont on veut l'historique des rapports.
	 * @param type
	 *            le type de rapports voulu
	 * @return une list des rapport-entre-tiers, ou <b>null</b> si aucun rapport n'existe du type spécifié.
	 */
	public static List<RapportEntreTiers> getRapportSujetHistoOfType(Tiers tiers, TypeRapportEntreTiers type) {

		List<RapportEntreTiers> results = null;

		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (type == rapport.getType()) {
					if (results == null) {
						results = new ArrayList<RapportEntreTiers>(); // création à la demande
					}
					results.add(rapport);
				}
			}
		}

		return results;
	}

	/**
	 * Retourne l'historique de tous les rapport-entre-tiers du type spécifiés, et dont le tiers est le sujet.
	 *
	 * @param tiers le tiers dont on veut l'historique des rapports.
	 * @param types les types de rapports voulus
	 * @return une list des rapport-entre-tiers, ou <b>null</b> si aucun rapport n'existe du type spécifié.
	 */
	public static List<RapportEntreTiers> getRapportSujetHistoOfType(Tiers tiers, TypeRapportEntreTiers... types) {

		List<RapportEntreTiers> results = null;

		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				for (TypeRapportEntreTiers type : types) {
					if (type == rapport.getType()) {
						if (results == null) {
							results = new ArrayList<RapportEntreTiers>(); // création à la demande
						}
						results.add(rapport);
					}
				}
			}
		}

		return results;
	}

	/**
	 * Retourne l'historique de tous les rapport-entre-tiers du type spécifiés, et dont le tiers est l'objet.
	 *
	 * @param tiers
	 *            le tiers dont on veut l'historique des rapports.
	 * @param type
	 *            le type de rapports voulu
	 * @return une list des rapport-entre-tiers, ou <b>null</b> si aucun rapport n'existe du type spécifié.
	 */
	public static List<RapportEntreTiers> getRapportObjetHistoOfType(Tiers tiers, TypeRapportEntreTiers type) {

		List<RapportEntreTiers> results = null;

		final Set<RapportEntreTiers> rapports = tiers.getRapportsObjet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (type == rapport.getType()) {
					if (results == null) {
						results = new ArrayList<RapportEntreTiers>(); // création à la demande
					}
					results.add(rapport);
				}
			}
		}

		return results;
	}
}
