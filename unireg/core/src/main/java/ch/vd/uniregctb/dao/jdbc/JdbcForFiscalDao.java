package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.tiers.ForFiscal;

public interface JdbcForFiscalDao {

	ForFiscal get(long forId, JdbcTemplate template);

	Set<ForFiscal> getForTiers(long tiersId, JdbcTemplate template);

	Map<Long, Set<ForFiscal>> getForTiers(Collection<Long> tiersId, JdbcTemplate template);
}
