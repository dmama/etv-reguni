package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.tiers.RapportEntreTiers;

public interface JdbcRapportEntreTiersDao {

	RapportEntreTiers get(long forId, JdbcTemplate template);

	Set<RapportEntreTiers> getForTiersSujet(long tiersId, JdbcTemplate template);

	Set<RapportEntreTiers> getForTiersObjet(long tiersId, JdbcTemplate template);

	Map<Long, Set<RapportEntreTiers>> getForTiersSujet(Collection<Long> tiersId, JdbcTemplate template);

	Map<Long, Set<RapportEntreTiers>> getForTiersObjet(Collection<Long> tiersId, JdbcTemplate template);
}