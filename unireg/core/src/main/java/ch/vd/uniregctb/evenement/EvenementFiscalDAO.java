package ch.vd.uniregctb.evenement;

import java.util.Collection;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * DAO des événements fiscaux.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementFiscalDAO extends GenericDAO<EvenementFiscal, Long> {

	/**
	 * Retourne la liste des événements fiscaux pour un tiers.
	 * @param tiers Tiers.
	 * @return  Retourne la liste des événements fiscaux pour un tiers.
	 */
	Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) ;
}

