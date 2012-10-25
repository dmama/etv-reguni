package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.tiers.IdentificationPersonne;

public interface JdbcIdentificationPersonneDao {

	IdentificationPersonne get(long forId, JdbcTemplate template);

	Set<IdentificationPersonne> getForTiers(long tiersId, JdbcTemplate template);

	Map<Long, Set<IdentificationPersonne>> getForTiers(Collection<Long> tiersId, JdbcTemplate template);
}