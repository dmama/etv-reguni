package ch.vd.unireg.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.adresse.AdressesTiers;
import ch.vd.unireg.adresse.AdressesTiersHisto;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public abstract class TiersHelper {

	/**
	 * Extrait les adresses surchargées au niveau fiscal (= adresse tiers) actives à la date donnée et pour le tiers spécifiés.
	 *
	 * @param tiers le tiers dont on veut rechercher les adresses.
	 * @param date  la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
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
	 * Extrait l'adresse surchargée au niveau fiscal (= adresse tiers) active à la date donnée, pour le type d'adresse et pour le tiers spécifiés.
	 *
	 * @param tiers le tiers dont on veut extraire une adresse.
	 * @param type  le type d'adresse recherché
	 * @param date  la date de référence, ou null pour obtenir l'adresse courante.
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
	 * @param tiers le tiers dont on veut chercher un rapport.
	 * @param type  le type de rapport voulu.
	 * @param date  la date de référence, ou <b>null</b> pour obtenir le rapport-entre-tiers actif.
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
	 * @param tiers le tiers dont on veut l'historique des rapports.
	 * @param type  le type de rapports voulu
	 * @return une list des rapport-entre-tiers, ou <b>null</b> si aucun rapport n'existe du type spécifié.
	 */
	public static List<RapportEntreTiers> getRapportSujetHistoOfType(Tiers tiers, TypeRapportEntreTiers type) {

		List<RapportEntreTiers> results = null;

		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (type == rapport.getType()) {
					if (results == null) {
						results = new ArrayList<>(); // création à la demande
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
							results = new ArrayList<>(); // création à la demande
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
	 * @param tiers le tiers dont on veut l'historique des rapports.
	 * @param type  le type de rapports voulu
	 * @return une list des rapport-entre-tiers, ou <b>null</b> si aucun rapport n'existe du type spécifié.
	 */
	public static List<RapportEntreTiers> getRapportObjetHistoOfType(Tiers tiers, TypeRapportEntreTiers type) {

		List<RapportEntreTiers> results = null;

		final Set<RapportEntreTiers> rapports = tiers.getRapportsObjet();
		if (rapports != null) {
			for (RapportEntreTiers rapport : rapports) {
				if (type == rapport.getType()) {
					if (results == null) {
						results = new ArrayList<>(); // création à la demande
					}
					results.add(rapport);
				}
			}
		}

		return results;
	}

	/**Cette fonction permet de  trouver l'autre parent d'un enfant passé en paramètre
	 *
	 * @param enfant    dont on recherche l'autre parent
	 * @param parent     Premier parent connu
	 * @param dateValidite   date de validité du lien parental
	 * @param tiersService
	 * @return l'autre parent de l'enfant
	 */
	public static PersonnePhysique getAutreParent(PersonnePhysique enfant, PersonnePhysique parent, RegDate dateValidite, TiersService tiersService) {
		final List<PersonnePhysique> lesParents = tiersService.getParents(enfant, dateValidite);
		if (lesParents.size() > 1) {
			return !parent.getNumero().equals(lesParents.get(0).getNumero()) ? lesParents.get(0) : lesParents.get(1);
		}
		return null;

	}

	/** Determine si les deux parents d'un contribuable enfant ont un domicile (EGID/EWID) différent
	 *
	 * @param enfant pour qui on compare l'egid des parents
	 * @param parent   connu de l'enfant
	 * @param adresseParent adresse du parent connu
	 * @param finPeriodeImposition   date de validitéde l'adresse du second parent
	 * @param adresseService       service des adresses
	 * @param tiersService service des tiers
	 * @return True si les 2 parents ont un egid/ewid différent, False sinon
	 * @throws AdresseException
	 */
	public static boolean hasParentsAvecEgidEwidDifferents(PersonnePhysique enfant, PersonnePhysique parent, AdresseGenerique adresseParent, RegDate finPeriodeImposition,
	                                                       AdresseService adresseService, TiersService tiersService) throws AdresseException {
		final PersonnePhysique autreParent = TiersHelper.getAutreParent(enfant, parent, finPeriodeImposition, tiersService);
		if (autreParent != null) {
			final AdresseGenerique adresseAutreParent = adresseService.getAdresseFiscale(autreParent, TypeAdresseFiscale.DOMICILE, finPeriodeImposition, false);
			return !isSameEgidEwid(adresseParent, adresseAutreParent);
		}
		else {
			return true;
		}
	}

	/**
	 * @param o1 premier objet à comparer
	 * @param o2 deuxième objet à comparer
	 * @param <T> type des objets (pour comparer des choses comparables)
	 * @return <code>false</code> si l'un au moins des objets est <code>null</code> ou si leurs valeurs sont différentes, <code>true</code> si les deux objets ne sont pas <code>null</code> et que les valeurs sont identiques
	 */
	private static <T> boolean areSame(T o1, T o2) {
		return o1 != null && o2 != null && o1.equals(o2);
	}

	public static boolean isSameEgidEwid(AdresseGenerique a1, AdresseGenerique a2) {
		return a1 != null && a2 != null && areSame(a1.getEgid(), a2.getEgid()) && areSame(a1.getEwid(), a2.getEwid());
	}
}
