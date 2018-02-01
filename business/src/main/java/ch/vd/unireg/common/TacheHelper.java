package ch.vd.unireg.common;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.type.TypeContribuable;

/**
 * Quelques méthodes utiles autour des tâches
 */
public abstract class TacheHelper {

	/**
	 * Pour les entreprises d'utilité publique, on demande à ce que la date d'échéance de la tâche d'envoi de DI soit
	 * au 31.01 (selon paramétrage) de l'année suivant la PF de la DI
	 *
	 * @param parametreAppService service de récupération des paramètres applicatifs
	 * @param typeContribuable type de contribuable pour lequel on veut générer une tâche d'envoi de DI entreprise
	 * @param dateReference date de référence (= date de génération de la tâche)
	 * @param dateFinPeriodeImposition date de la fin de la période d'imposition considérée pour la DI entreprise
	 * @return la date d'échéance à placer dans la tâche d'émission de DI entreprise
	 */
	public static RegDate getDateEcheanceTacheEnvoiDIPM(ParametreAppService parametreAppService, TypeContribuable typeContribuable, RegDate dateReference, RegDate dateFinPeriodeImposition) {
		final RegDate dateDefaut = Tache.getDefaultEcheance(dateReference);
		if (typeContribuable == TypeContribuable.UTILITE_PUBLIQUE) {
			final Integer[] limiteEnvoiMasseUtilitePublique = parametreAppService.getDateLimiteEnvoiMasseDeclarationsUtilitePublique();
			final RegDate prochaineDateLimiteEnvoiMasse = RegDate.get(dateFinPeriodeImposition.year() + 1, limiteEnvoiMasseUtilitePublique[1], limiteEnvoiMasseUtilitePublique[0]);
			if (prochaineDateLimiteEnvoiMasse.isAfter(dateDefaut)) {
				return prochaineDateLimiteEnvoiMasse;
			}
		}
		return dateDefaut;
	}
}
