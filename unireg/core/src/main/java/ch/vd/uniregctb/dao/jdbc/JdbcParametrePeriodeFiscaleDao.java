package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;

public interface JdbcParametrePeriodeFiscaleDao {

	ParametrePeriodeFiscale get(long forId, JdbcTemplate template);

	Set<ParametrePeriodeFiscale> getForPeriode(long periodeId, JdbcTemplate template);

	Map<Long, Set<ParametrePeriodeFiscale>> getForPeriode(Collection<Long> periodeIds, JdbcTemplate template);
}