package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.vd.uniregctb.declaration.ModeleDocument;

public interface JdbcModeleDocumentDao {

	ModeleDocument get(long id, JdbcTemplate template);

	List<ModeleDocument> getList(Collection<Long> docIds, JdbcTemplate template);
}