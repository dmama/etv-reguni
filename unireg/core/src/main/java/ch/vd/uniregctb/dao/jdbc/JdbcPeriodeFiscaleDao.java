package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.PeriodeFiscale;

public interface JdbcPeriodeFiscaleDao {

	/**
	 * Charge une période à partir de son id et renseigne toutes les collections.
	 *
	 * @param id       l'id de la période à charger
	 * @param template le jdbc template à utiliser
	 * @return une période fiscale ou <b>null</b> si la période n'existe pas dans la base de données
	 */
	PeriodeFiscale get(long id, JdbcTemplate template);

	List<PeriodeFiscale> getList(Collection<Long> periodeIds, JdbcTemplate template);
}