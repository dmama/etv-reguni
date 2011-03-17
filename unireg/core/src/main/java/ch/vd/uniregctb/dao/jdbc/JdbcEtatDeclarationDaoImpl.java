package ch.vd.uniregctb.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;

public class JdbcEtatDeclarationDaoImpl implements JdbcEtatDeclarationDao {

	private static final EtatDeclarationMapper ROW_MAPPER = new EtatDeclarationMapper();

	@SuppressWarnings({"unchecked"})
	public EtatDeclaration get(long id, JdbcTemplate template) {
		final Pair<Long, EtatDeclaration> pair = (Pair<Long, EtatDeclaration>) DataAccessUtils.uniqueResult(template.query(EtatDeclarationMapper.selectById(), new Object[]{id}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<EtatDeclaration> getForDeclaration(long diId, JdbcTemplate template) {
		final List<Pair<Long, EtatDeclaration>> list = template.query(EtatDeclarationMapper.selectByDeclarationId(), new Object[]{diId}, ROW_MAPPER);
		final HashSet<EtatDeclaration> set = new HashSet<EtatDeclaration>(list.size());
		for (Pair<Long, EtatDeclaration> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<EtatDeclaration>> getForDeclarations(Collection<Long> disIds, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, EtatDeclaration>> list = CollectionsUtils.splitAndProcess(disIds, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, EtatDeclaration>>() {
			public List<Pair<Long, EtatDeclaration>> process(List<Long> ids) {
				return template.query(EtatDeclarationMapper.selectByDeclarationsIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<EtatDeclaration>> map = new HashMap<Long, Set<EtatDeclaration>>();
		for (Pair<Long, EtatDeclaration> pair : list) {
			Set<EtatDeclaration> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<EtatDeclaration>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class EtatDeclarationMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"TYPE, " + // 1
				"ID, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"DATE_ENVOI_COURRIER, " + // 5
				"DATE_OBTENTION, " + // 6
				"DECLARATION_ID, " + // 7
				"LOG_CDATE, " + // 8
				"LOG_CUSER, " + // 9
				"LOG_MDATE, " + // 10
				"LOG_MUSER " + // 11
				"from ETAT_DECLARATION";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByDeclarationId() {
			return BASE_SELECT + " where DECLARATION_ID = ?";
		}

		public static String selectByDeclarationsIds(Collection<Long> disIds) {
			return BASE_SELECT + " where DECLARATION_ID in " + JdbcDaoUtils.buildInClause(disIds);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String type = rs.getString(1);
			final long temp7 = rs.getLong(7);
			final Long declarationId = (rs.wasNull() ? null : temp7);
			final EtatDeclaration res;
			
			if (type.equals("EMISE")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp6 = rs.getInt(6);
				final RegDate dateObtention = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
			
				EtatDeclarationEmise o = new EtatDeclarationEmise();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateObtention(dateObtention);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				res = o;
			}
			else if (type.equals("SOMMEE")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateEnvoiCourrier = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateObtention = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
			
				EtatDeclarationSommee o = new EtatDeclarationSommee();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateEnvoiCourrier(dateEnvoiCourrier);
				o.setDateObtention(dateObtention);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				res = o;
			}
			else if (type.equals("RETOURNEE")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp6 = rs.getInt(6);
				final RegDate dateObtention = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
			
				EtatDeclarationRetournee o = new EtatDeclarationRetournee();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateObtention(dateObtention);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				res = o;
			}
			else if (type.equals("ECHUE")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp6 = rs.getInt(6);
				final RegDate dateObtention = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
			
				EtatDeclarationEchue o = new EtatDeclarationEchue();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateObtention(dateObtention);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + type + "]");
			}
			
			final Pair<Long, EtatDeclaration> pair = new Pair<Long, EtatDeclaration>();
			pair.setFirst(declarationId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
