package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.adresse.AdresseTiers;

public interface JdbcAdresseTiersDao {

	AdresseTiers get(long forId, JdbcTemplate template);

	Set<AdresseTiers> getForTiers(long tiersId, JdbcTemplate template);

	Map<Long, Set<AdresseTiers>> getForTiers(Collection<Long> tiersId, JdbcTemplate template);
}