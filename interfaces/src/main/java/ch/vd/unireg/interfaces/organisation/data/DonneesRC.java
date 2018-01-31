package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2015-11-04
 */
public interface DonneesRC {

	List<AdresseLegaleRCEnt> getAdresseLegale();

	AdresseLegaleRCEnt getAdresseLegale(RegDate date);

	List<Capital> getCapital();

	List<DateRanged<InscriptionRC>> getInscription();

	InscriptionRC getInscription(RegDate date);

	List<DateRanged<String>> getButs();

	List<DateRanged<RegDate>> getDateStatuts();

	List<EntreeJournalRC> getEntreesJournal();

	/**
	 * Retourne la liste des entrées de journal au RC (englobant la publication FOSC correspondante)
	 * correspondant à la date de publication FOSC passée en paramètre.
	 * @param date date de publication FOSC
	 * @return la liste des entrées de journal pour la date de publication
	 */
	List<EntreeJournalRC> getEntreesJournalPourDatePublication(RegDate date);
}
