package ch.vd.unireg.metier.common;

import ch.vd.registre.base.date.RegDate;

/**
 * Classe de méthodes utilitaires autour de décalages métier de dates, par exemple en ce qui concerne le début de l'assujettissement
 * ordinaire à l'obtention du permis C ou de la nationalité suisse
 */
public abstract class DecalageDateHelper {

	public static final int PREMIERE_PF_AVEC_REEL_DECALAGE_MOIS_SUIVANT_PERMIS_C_NATIONALITE = 2014;

	/**
	 * [SIFISC-10518] Avant 2014, une obtention au premier jour du mois ne décale pas le début de l'assujettissement ordinaire au mois suivant&nbsp;;
	 * en 2014 et après, si.
	 * @param obtention date d'obtention du permis C ou de la nationalité suisse
	 * @return date de début de l'assujettissement ordinaire correspondant
	 */
	public static RegDate getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(RegDate obtention) {
		if (obtention.year() < PREMIERE_PF_AVEC_REEL_DECALAGE_MOIS_SUIVANT_PERMIS_C_NATIONALITE) {
			// le prochain "premier du mois", y compris "aujourd'hui" si on est déjà un premier du mois
			return obtention.getOneDayBefore().getLastDayOfTheMonth().getOneDayAfter();
		}
		else {
			// le premier jour du mois suivant le mois de la date donnée
			return obtention.getLastDayOfTheMonth().getOneDayAfter();
		}
	}

	/**
	 * [SIFISC-10518] Avant 2014, il faut décaler d'un jour la date de début du for ordinaire après une obtention d'un permis C ou de la nationalité
	 * suisse (voir également SIFISC-9211), mais pas après
	 * @param obtention date d'obtention du permis C ou de la nationalité suisse
	 * @return date de début du for ordinaire correspondant
	 */
	public static RegDate getDateOuvertureForOrdinaireApresPermisCNationaliteSuisse(RegDate obtention) {
		if (obtention.year() < PREMIERE_PF_AVEC_REEL_DECALAGE_MOIS_SUIVANT_PERMIS_C_NATIONALITE) {
			return obtention.getOneDayAfter();
		}
		else {
			return obtention;
		}
	}
}
