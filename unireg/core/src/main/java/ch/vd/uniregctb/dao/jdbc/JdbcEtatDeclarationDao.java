package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.EtatDeclaration;

public interface JdbcEtatDeclarationDao {

	EtatDeclaration get(long id, JdbcTemplate template);

	Set<EtatDeclaration> getForDeclaration(long diId, JdbcTemplate template);

	Map<Long, Set<EtatDeclaration>> getForDeclarations(Collection<Long> disIds, JdbcTemplate template);
}