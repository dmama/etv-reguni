package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.Declaration;

public interface JdbcDeclarationDao {

	Declaration get(long forId, boolean withEtats, JdbcTemplate template);

	Set<Declaration> getForTiers(long tiersId, boolean withEtats, JdbcTemplate template);

	Map<Long, Set<Declaration>> getForTiers(Collection<Long> tiersId, boolean withEtats, JdbcTemplate template);
}