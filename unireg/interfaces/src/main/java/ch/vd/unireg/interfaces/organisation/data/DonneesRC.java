package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2015-11-04
 */
public interface DonneesRC {
	List<AdresseRCEnt> getAdresseLegale();

	List<Capital> getCapital();

	List<DateRanged<StatusInscriptionRC>> getStatusInscription();

	StatusInscriptionRC getStatusInscription(RegDate date);

	List<DateRanged<RegDate>> getDateInscription();

	List<DateRanged<String>> getButs();

	List<DateRanged<RegDate>> getDateStatuts();

	List<DateRanged<RegDate>> getDateRadiation();

	RegDate getDateRadiation(RegDate date);
}
