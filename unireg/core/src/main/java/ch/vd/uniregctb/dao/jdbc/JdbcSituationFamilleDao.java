package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.tiers.SituationFamille;

public interface JdbcSituationFamilleDao {

	SituationFamille get(long forId, JdbcTemplate template);

	Set<SituationFamille> getForTiers(long tiersId, JdbcTemplate template);

	Map<Long, Set<SituationFamille>> getForTiers(Collection<Long> tiersId, JdbcTemplate template);
}