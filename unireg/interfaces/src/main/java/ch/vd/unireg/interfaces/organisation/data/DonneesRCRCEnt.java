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

	private static final long serialVersionUID = -3503458098121748151L;

	private final List<DateRanged<StatusRC>> status;
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<StatusInscriptionRC>> statusInscription;
	private final List<Capital> capital;
	private final List<AdresseRCEnt> adresseLegale;
	private final List<DateRanged<String>> buts;
	private final List<DateRanged<RegDate>> dateStatuts;

	public DonneesRCRCEnt(List<AdresseRCEnt> adresseLegale, List<DateRanged<StatusRC>> status, List<DateRanged<String>> nom,
	                 List<DateRanged<StatusInscriptionRC>> statusInscription, List<Capital> capital, List<DateRanged<String>> buts, List<DateRanged<RegDate>> dateStatuts) {
		this.adresseLegale = adresseLegale;
		this.status = status;
		this.nom = nom;
		this.statusInscription = statusInscription;
		this.capital = capital;
		this.buts = buts;
		this.dateStatuts = dateStatuts;
	}

	@Override
	public List<AdresseRCEnt> getAdresseLegale() {
		return adresseLegale;
	}

	@Override
	public List<Capital> getCapital() {
		return capital;
	}


	@Override
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	@Override
	public List<DateRanged<StatusRC>> getStatus() {
		return status;
	}

	@Override
	public List<DateRanged<StatusInscriptionRC>> getStatusInscription() {
		return statusInscription;
	}

	@Override
	public List<DateRanged<String>> getButs() {
		return buts;
	}

	@Override
	public List<DateRanged<RegDate>> getDateStatuts() {
		return dateStatuts;
	}
}
