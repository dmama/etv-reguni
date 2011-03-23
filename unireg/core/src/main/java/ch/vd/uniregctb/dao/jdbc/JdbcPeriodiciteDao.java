package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.Periodicite;

public interface JdbcPeriodiciteDao {

	Periodicite get(long forId, JdbcTemplate template);

	Set<Periodicite> getForTiers(long tiersId, JdbcTemplate template);

	Map<Long, Set<Periodicite>> getForTiers(Collection<Long> tiersId, JdbcTemplate template);
}