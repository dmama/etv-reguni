package ch.vd.uniregctb.evenement;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * DAO des �v�nements civils.
 * 
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 * 
 */
public interface EvenementCivilUnitaireDAO extends GenericDAO<EvenementCivilUnitaire, Long> {
	
	List<EvenementCivilUnitaire> findNotTreatedEvenementsOrderByDate();
}

