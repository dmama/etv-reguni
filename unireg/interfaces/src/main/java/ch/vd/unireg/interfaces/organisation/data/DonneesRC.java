package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

public class DonneesRC implements Serializable {

	private static final long serialVersionUID = -3503458098121748151L;

	private final List<DateRanged<StatusRC>> status;
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<StatusInscriptionRC>> statusInscription;
	private final List<Capital> capital;
	private final List<AdresseRCEnt> adresseLegale;
	private final List<DateRanged<String>> buts;
	private final List<DateRanged<RegDate>> dateStatus;

	public DonneesRC(List<AdresseRCEnt> adresseLegale, List<DateRanged<StatusRC>> status, List<DateRanged<String>> nom,
	                 List<DateRanged<StatusInscriptionRC>> statusInscription, List<Capital> capital, List<DateRanged<String>> buts, List<DateRanged<RegDate>> dateStatus) {
		this.adresseLegale = adresseLegale;
		this.status = status;
		this.nom = nom;
		this.statusInscription = statusInscription;
		this.capital = capital;
		this.buts = buts;
		this.dateStatus = dateStatus;
	}

	public List<AdresseRCEnt> getAdresseLegale() {
		return adresseLegale;
	}

	public List<Capital> getCapital() {
		return capital;
	}

	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<StatusRC>> getStatus() {
		return status;
	}

	public List<DateRanged<StatusInscriptionRC>> getStatusInscription() {
		return statusInscription;
	}

	public List<DateRanged<String>> getButs() {
		return buts;
	}

	public List<DateRanged<RegDate>> getDateStatus() {
		return dateStatus;
	}
}
