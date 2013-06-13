package ch.vd.uniregctb.evenement.externe;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * DAO des événements externes.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementExterneDAO extends GenericDAO<EvenementExterne, Long> {

	boolean existe(String businessId);

	public Collection<EvenementExterne> getEvenementExternes(boolean ascending, EtatEvenementExterne... etatEvenementExternes);

	/**
	 * @return la liste des quittances LR dont la représentation en base doit être migrée.
	 */
	List<Long> getIdsQuittancesLRToMigrate();
}

