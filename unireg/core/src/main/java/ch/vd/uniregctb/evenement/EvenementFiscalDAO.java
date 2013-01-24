package ch.vd.uniregctb.evenement;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.tiers.Tiers;

import java.util.Collection;

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
	Collection<EvenementFiscal> getEvenementFiscals( Tiers tiers) ;
}

