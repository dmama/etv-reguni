package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 *   Utilisez les méthodes des helpers pour produire les données des accesseurs.
 *
 *   EntrepriseHelper fournit les méthodes nécessaires à l'accès par date:
 *   valuesForDate(), valueForDate() et dateRangeForDate(), à utiliser en priorité.
 */
public class DonneesRCRCEnt implements DonneesRC, Serializable {

	private static final long serialVersionUID = 3467985910907593777L;

	private final List<DateRanged<InscriptionRC>> inscription;
	private final List<Capital> capital;
	private final List<AdresseLegaleRCEnt> adresseLegale;
	private final List<DateRanged<String>> buts;
	private final List<DateRanged<RegDate>> dateStatuts;
	private final List<EntreeJournalRC> entreesJournal;

	public DonneesRCRCEnt(List<AdresseLegaleRCEnt> adresseLegale,
	                      List<DateRanged<InscriptionRC>> inscription,
	                      List<Capital> capital,
	                      List<DateRanged<String>> buts,
	                      List<DateRanged<RegDate>> dateStatuts,
	                      List<EntreeJournalRC> entreesJournal) {
		this.adresseLegale = adresseLegale;
		this.inscription = inscription;
		this.capital = capital;
		this.buts = buts;
		this.dateStatuts = dateStatuts;
		this.entreesJournal = entreesJournal;
	}

	@Override
	public List<AdresseLegaleRCEnt> getAdresseLegale() {
		return adresseLegale;
	}

	@Override
	public AdresseLegaleRCEnt getAdresseLegale(RegDate date) {
		return EntrepriseHelper.dateRangeForDate(adresseLegale, date);
	}

	@Override
	public List<DateRanged<InscriptionRC>> getInscription() {
		return inscription;
	}

	@Override
	public InscriptionRC getInscription(RegDate date) {
		return EntrepriseHelper.valueForDate(inscription, date);
	}

	@Override
	public List<Capital> getCapital() {
		return capital;
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
	public List<EntreeJournalRC> getEntreesJournal() {
		return entreesJournal;
	}

	@Override
	public List<EntreeJournalRC> getEntreesJournalPourDatePublication(RegDate date) {
		return EntrepriseHelper.getEntreesJournalPourDatePublication(entreesJournal, date);
	}
}
