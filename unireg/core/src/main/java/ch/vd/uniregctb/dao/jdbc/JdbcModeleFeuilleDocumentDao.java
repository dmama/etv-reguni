package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;

public interface JdbcModeleFeuilleDocumentDao {

	ModeleFeuilleDocument get(long forId, JdbcTemplate template);

	Set<ModeleFeuilleDocument> getForPeriode(long periodeId, JdbcTemplate template);

	Map<Long, Set<ModeleFeuilleDocument>> getForPeriode(Collection<Long> periodeIds, JdbcTemplate template);
}