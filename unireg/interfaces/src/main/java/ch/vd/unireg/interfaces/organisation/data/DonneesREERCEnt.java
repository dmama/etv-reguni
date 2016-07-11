package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   OrganisationHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class DonneesREERCEnt implements DonneesREE, Serializable {

	private static final long serialVersionUID = -6332558068487594901L;

	private final List<DateRanged<StatusREE>> statusREE;
	private final List<DateRanged<RegDate>> dateInscriptionREE;

	public DonneesREERCEnt(List<DateRanged<StatusREE>> statusREE,
	                       List<DateRanged<RegDate>> dateInscriptionREE) {
		this.statusREE = statusREE;
		this.dateInscriptionREE = dateInscriptionREE;
	}

	public List<DateRanged<StatusREE>> getStatusREE() {
		return statusREE;
	}

	@Override
	public StatusREE getStatusREE(RegDate date) {
		return OrganisationHelper.valueForDate(statusREE, date);
	}


	public List<DateRanged<RegDate>> getDateInscriptionREE() {
		return dateInscriptionREE;
	}

	@Override
	public RegDate getDateInscriptionREE(RegDate date) {
		return OrganisationHelper.valueForDate(dateInscriptionREE, date);
	}
}
