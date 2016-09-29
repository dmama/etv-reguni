package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2015-11-04
 */
public interface DonneesREE {

	List<DateRanged<InscriptionREE>> getInscriptionREE();

	InscriptionREE getInscriptionREE(RegDate date);
}
