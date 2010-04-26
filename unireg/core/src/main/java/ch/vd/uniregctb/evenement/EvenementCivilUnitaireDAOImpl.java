package ch.vd.uniregctb.evenement;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * DAO des événements civils.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class EvenementCivilUnitaireDAOImpl extends GenericDAOImpl<EvenementCivilUnitaire, Long> implements EvenementCivilUnitaireDAO {

	//private static final Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireDAOImpl.class);

	/**
	 * Constructeur par défaut.
	 */
	public EvenementCivilUnitaireDAOImpl() {
		super(EvenementCivilUnitaire.class);
	}

	@SuppressWarnings("unchecked")
	public List<EvenementCivilUnitaire> findNotTreatedEvenementsOrderByDate() {
		return getHibernateTemplate().find("from EvenementCivilUnitaire as ec where ec.etat != ? order by ec.dateEvenement", EtatEvenementCivil.TRAITE.name());
	}

}
