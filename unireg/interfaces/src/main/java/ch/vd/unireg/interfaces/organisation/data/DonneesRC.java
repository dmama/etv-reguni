package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.List;

public class DonneesRC implements Serializable {

	private static final long serialVersionUID = 1633547309615186996L;

	private final List<DateRanged<StatusRC>> status;
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<StatusInscriptionRC>> statusInscription;
	private final List<Capital> capital;
	private final List<AdresseRCEnt> adresseLegale;

	public DonneesRC(List<AdresseRCEnt> adresseLegale, List<DateRanged<StatusRC>> status, List<DateRanged<String>> nom,
	                 List<DateRanged<StatusInscriptionRC>> statusInscription, List<Capital> capital) {
		this.adresseLegale = adresseLegale;
		this.status = status;
		this.nom = nom;
		this.statusInscription = statusInscription;
		this.capital = capital;
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
}
