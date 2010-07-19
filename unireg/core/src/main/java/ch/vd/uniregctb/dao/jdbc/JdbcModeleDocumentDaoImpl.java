package ch.vd.uniregctb.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.type.TypeDocument;

public class JdbcModeleDocumentDaoImpl implements JdbcModeleDocumentDao {

	private static final ModeleDocumentMapper ROW_MAPPER = new ModeleDocumentMapper();

	private JdbcModeleFeuilleDocumentDao ppfDao = new JdbcModeleFeuilleDocumentDaoImpl();

	public ModeleDocument get(long periodeId, JdbcTemplate template) {

		final ModeleDocument modele = (ModeleDocument) DataAccessUtils.uniqueResult(template.query(ModeleDocumentMapper.selectById(), new Object[]{periodeId}, ROW_MAPPER));
		if (modele == null) {
			return null;
		}

		final Set<ModeleFeuilleDocument> feuilles = ppfDao.getForPeriode(periodeId, template);
		for (ModeleFeuilleDocument f : feuilles) {
			f.setModeleDocument(modele);
		}
		modele.setModelesFeuilleDocument(feuilles);

		return modele;
	}

	@SuppressWarnings({"unchecked"})
	public List<ModeleDocument> getList(final Collection<Long> periodesId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<ModeleDocument> list = CollectionsUtils.splitAndProcess(periodesId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, ModeleDocument>() {
			public List<ModeleDocument> process(List<Long> ids) {
				return template.query(ModeleDocumentMapper.selectByIds(ids), ROW_MAPPER);
			}
		});

		final Map<Long, Set<ModeleFeuilleDocument>> map = ppfDao.getForPeriode(periodesId, template);
		for (ModeleDocument modele : list) {
			Set<ModeleFeuilleDocument> feuilles = map.get(modele.getId());
			if (feuilles == null) {
				feuilles = Collections.emptySet();
			}
			else {
				for (ModeleFeuilleDocument f : feuilles) {
					f.setModeleDocument(modele);
				}
			}
			modele.setModelesFeuilleDocument(feuilles);
		}

		return list;
	}
	
	private static class ModeleDocumentMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ID, " + // 1
				"ANNULATION_DATE, " + // 2
				"ANNULATION_USER, " + // 3
				"LOG_CDATE, " + // 4
				"LOG_CUSER, " + // 5
				"LOG_MDATE, " + // 6
				"LOG_MUSER, " + // 7
				"PERIODE_ID, " + // 8
				"TYPE_DOCUMENT " + // 9
				"from MODELE_DOCUMENT";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where NUMERO in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			final Long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final Timestamp temp2 = rs.getTimestamp(2);
			final Date annulationDate = (rs.wasNull() ? null : temp2);
			final String temp3 = rs.getString(3);
			final String annulationUser = (rs.wasNull() ? null : temp3);
			final Timestamp temp4 = rs.getTimestamp(4);
			final Date logCdate = (rs.wasNull() ? null : temp4);
			final String temp5 = rs.getString(5);
			final String logCuser = (rs.wasNull() ? null : temp5);
			final Timestamp temp6 = rs.getTimestamp(6);
			final Timestamp logMdate = (rs.wasNull() ? null : temp6);
			final String temp7 = rs.getString(7);
			final String logMuser = (rs.wasNull() ? null : temp7);
			final Long temp8 = rs.getLong(8);
			final PeriodeFiscale periodeId = (rs.wasNull() ? null : getPeriodeFiscale(temp8));
			final String temp9 = rs.getString(9);
			final TypeDocument typeDocument = (rs.wasNull() ? null : Enum.valueOf(TypeDocument.class, temp9));
			
			final ModeleDocument res;
			
			ModeleDocument o = new ModeleDocument();
			o.setId(id);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			o.setPeriodeFiscale(periodeId);
			o.setTypeDocument(typeDocument);
			res = o;
			
			return res;
		}
		
		private PeriodeFiscale getPeriodeFiscale(Long periodeId) {
			return null;  // TODO (msi)
		}
	}
}
