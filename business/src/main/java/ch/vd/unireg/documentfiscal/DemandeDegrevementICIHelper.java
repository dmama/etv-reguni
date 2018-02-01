package ch.vd.unireg.documentfiscal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleHelper;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;

/**
 * Rassemblement de méthodes utilitaires autour des demandes de dégrèvement ICI
 */
public abstract class DemandeDegrevementICIHelper {

	/**
	 * @param formulaire le formulaire
	 * @param rfService le service du registre foncier
	 * @return la commune de l'immeuble lié au formulaire de demande de dégrèvement
	 */
	@Nullable
	public static Commune getCommune(DemandeDegrevementICI formulaire, RegistreFoncierService rfService) {
		final RegDate dateReference = getDateReference(formulaire);
		return getCommune(formulaire.getImmeuble(), dateReference, rfService);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param rfService le service du registre foncier
	 * @return la commune de l'immeuble, à la date de référence
	 */
	@Nullable
	static Commune getCommune(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService rfService) {
		return rfService.getCommune(immeuble, dateReference);
	}

	/**
	 * @param formulaire le formulaire de demande de dégrèvement
	 * @param rfService le service du registre foncier
	 * @return l'estimation fiscale de l'immeuble lié au formulaire de demande de dégrèvement
	 */
	@Nullable
	public static EstimationRF getEstimationFiscale(DemandeDegrevementICI formulaire, RegistreFoncierService rfService) {
		final RegDate dateReference = getDateReference(formulaire);
		return getEstimationFiscale(formulaire.getImmeuble(), dateReference, rfService);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param rfService le service du registre foncier
	 * @return l'estimation fiscale de l'immeuble, à la date de référence
	 */
	@Nullable
	static EstimationRF getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService rfService) {
		return rfService.getEstimationFiscale(immeuble, dateReference);
	}

	/**
	 * @param formulaire le formulaire de demande de dégrèvement
	 * @param rfService le service du registre foncier
	 * @return le numéro de parcelle complet (avec tirets si nécessaire) de l'immeuble lié au formulaire de demande de dégrèvement
	 */
	@Nullable
	public static String getNumeroParcelleComplet(DemandeDegrevementICI formulaire, RegistreFoncierService rfService) {
		final RegDate dateReference = getDateReference(formulaire);
		return getNumeroParcelleComplet(formulaire.getImmeuble(), dateReference, rfService);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param rfService le service du registre foncier
	 * @return le numéro de parcelle complet de l'immeuble, à la date de référence
	 */
	@Nullable
	static String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService rfService) {
		return rfService.getNumeroParcelleComplet(immeuble, dateReference);
	}

	/**
	 * @param formulaire le formulaire de demande de dégrèvement
	 * @param maxLength la longueur maximale acceptable pour la description de la nature de l'immeuble lié au formulaire de demande de dégrèvement
	 * @return une chaîne de caractères qui décrit la nature de l'immeuble
	 */
	@Nullable
	public static String getNatureImmeuble(DemandeDegrevementICI formulaire, int maxLength) {
		final RegDate dateReference = getDateReference(formulaire);
		return ImmeubleHelper.getNatureImmeuble(formulaire.getImmeuble(), dateReference, maxLength);
	}

	/**
	 * Méthode centralisée pour la détermination de la date de référence
	 * @param formulaire formulaire de demande de dégrèvement
	 * @return la date de référence pour les données à extraire
	 */
	private static RegDate getDateReference(DemandeDegrevementICI formulaire) {
		return RegDate.get(formulaire.getPeriodeFiscale(), 1, 1);
	}
}
