package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

public class DonneesRC {
	private final List<DateRanged<StatusRC>> status;
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<StatusInscriptionRC>> statusInscription;
	private final List<DateRanged<Capital>> capital;
	private final List<DateRanged<Adresse>> adresseLegale;

	public DonneesRC(List<DateRanged<Adresse>> adresseLegale, List<DateRanged<StatusRC>> status, List<DateRanged<String>> nom,
	                 List<DateRanged<StatusInscriptionRC>> statusInscription, List<DateRanged<Capital>> capital) {
		this.adresseLegale = adresseLegale;
		this.status = status;
		this.nom = nom;
		this.statusInscription = statusInscription;
		this.capital = capital;
	}

	public List<DateRanged<Adresse>> getAdresseLegale() {
		return adresseLegale;
	}

	public List<DateRanged<Capital>> getCapital() {
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
}
