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
public class DonneesRCRCEnt implements DonneesRC, Serializable {

	private static final long serialVersionUID = 8809887249010115302L;

	private final List<DateRanged<StatusInscriptionRC>> statusInscription;
	private final List<DateRanged<RaisonDeDissolutionRC>> raisonDeDissolutionVd;
	private final List<DateRanged<RegDate>> dateInscription;
	private final List<Capital> capital;
	private final List<AdresseLegaleRCEnt> adresseLegale;
	private final List<DateRanged<String>> buts;
	private final List<DateRanged<RegDate>> dateStatuts;
	private final List<DateRanged<RegDate>> dateRadiation;

	public DonneesRCRCEnt(List<AdresseLegaleRCEnt> adresseLegale,
	                      List<DateRanged<StatusInscriptionRC>> statusInscription,
	                      List<DateRanged<RaisonDeDissolutionRC>> raisonDeDissolutionVd,
	                      List<DateRanged<RegDate>> dateInscription,
	                      List<Capital> capital, List<DateRanged<String>> buts,
	                      List<DateRanged<RegDate>> dateStatuts,
	                      List<DateRanged<RegDate>> dateRadiation) {
		this.adresseLegale = adresseLegale;
		this.statusInscription = statusInscription;
		this.raisonDeDissolutionVd = raisonDeDissolutionVd;
		this.dateInscription = dateInscription;
		this.capital = capital;
		this.buts = buts;
		this.dateStatuts = dateStatuts;
		this.dateRadiation = dateRadiation;
	}

	@Override
	public List<AdresseLegaleRCEnt> getAdresseLegale() {
		return adresseLegale;
	}

	@Override
	public List<Capital> getCapital() {
		return capital;
	}

	@Override
	public List<DateRanged<StatusInscriptionRC>> getStatusInscription() {
		return statusInscription;
	}

	public List<DateRanged<RaisonDeDissolutionRC>> getRaisonDeDissolutionVd() {
		return raisonDeDissolutionVd;
	}

	@Override
	public StatusInscriptionRC getStatusInscription(RegDate date) {
		return OrganisationHelper.valueForDate(statusInscription, date);
	}

	@Override
	public RaisonDeDissolutionRC getRaisonDeDissolutionVd(RegDate date) {
		return OrganisationHelper.valueForDate(getRaisonDeDissolutionVd(), date);
	}

	@Override
	public List<DateRanged<RegDate>> getDateInscription() {
		return dateInscription;
	}

	@Override
	public RegDate getDateInscription(RegDate date) {
		return OrganisationHelper.valueForDate(dateInscription, date);
	}

	@Override
	public List<DateRanged<String>> getButs() {
		return buts;
	}

	@Override
	public List<DateRanged<RegDate>> getDateStatuts() {
		return dateStatuts;
	}

	@Override
	public List<DateRanged<RegDate>> getDateRadiation() {
		return dateRadiation;
	}

	@Override
	public RegDate getDateRadiation(RegDate date) {
		return OrganisationHelper.valueForDate(dateRadiation, date);
	}
}
