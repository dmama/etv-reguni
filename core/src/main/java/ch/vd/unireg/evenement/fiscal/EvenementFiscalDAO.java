package ch.vd.unireg.evenement.fiscal;

import java.util.Collection;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.tiers.Tiers;

public interface EvenementFiscalDAO extends GenericDAO<EvenementFiscal, Long> {

	/**
	 * @param tiers un tiers
	 * @return une collection des événements fiscaux attachés au tiers donné (en tant que tiers principal seulement...)
	 */
	Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers);
}
