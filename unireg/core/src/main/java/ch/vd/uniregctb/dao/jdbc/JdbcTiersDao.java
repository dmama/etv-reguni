package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public interface JdbcTiersDao {

	/**
	 * Charge un tiers à partir de son id et renseigne les collections spécifiées dans le paramètre <i>parts</i>.
	 *
	 * @param tiersId l'id du tiers à charger
	 * @param parts   les collections devant être renseignées sur le tiers
	 * @return un tiers ou <b>null</b> si le tiers n'existe pas dans la base de données
	 */
	Tiers get(long tiersId, Set<TiersDAO.Parts> parts);

	/**
	 * Charge un seul lot les tiers dont les ids sont spécifiées en paramètre.
	 * <p/>
	 * <b>Attention !</b> Les tiers chargés de cette manière ne sont pas connus de Hibernate et ne peuvent pas être sauvés en base.
	 *
	 * @param ids   les ids des tiers à charger. Au maximum 500 à la fois.
	 * @param parts les collections associées aux tiers à précharger.
	 * @return une liste de tiers
	 */
	List<Tiers> getBatch(Collection<Long> ids, Set<TiersDAO.Parts> parts);

	Tiers get(long tiersId, JdbcTemplate template);

	List<Tiers> getList(Collection<Long> tiersId, JdbcTemplate template);
}

