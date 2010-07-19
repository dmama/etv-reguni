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
import ch.vd.uniregctb.type.TypeEtatDeclaration;

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
				"ID, " + // 1
				"ANNULATION_DATE, " + // 2
				"ANNULATION_USER, " + // 3
				"DATE_OBTENTION, " + // 4
				"DECLARATION_ID, " + // 5
				"LOG_CDATE, " + // 6
				"LOG_CUSER, " + // 7
				"LOG_MDATE, " + // 8
				"LOG_MUSER, " + // 9
				"TYPE " + // 10
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
			final Long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final Timestamp temp2 = rs.getTimestamp(2);
			final Date annulationDate = (rs.wasNull() ? null : temp2);
			final String temp3 = rs.getString(3);
			final String annulationUser = (rs.wasNull() ? null : temp3);
			final Integer temp4 = rs.getInt(4);
			final RegDate dateObtention = (rs.wasNull() ? null : RegDate.fromIndex(temp4, false));
			final Long temp5 = rs.getLong(5);
			final Long declarationId = (rs.wasNull() ? null : temp5);
			final Timestamp temp6 = rs.getTimestamp(6);
			final Date logCdate = (rs.wasNull() ? null : temp6);
			final String temp7 = rs.getString(7);
			final String logCuser = (rs.wasNull() ? null : temp7);
			final Timestamp temp8 = rs.getTimestamp(8);
			final Timestamp logMdate = (rs.wasNull() ? null : temp8);
			final String temp9 = rs.getString(9);
			final String logMuser = (rs.wasNull() ? null : temp9);
			final String temp10 = rs.getString(10);
			final TypeEtatDeclaration type = (rs.wasNull() ? null : Enum.valueOf(TypeEtatDeclaration.class, temp10));
			
			final EtatDeclaration res;
			
			EtatDeclaration o = new EtatDeclaration();
			o.setId(id);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setDateObtention(dateObtention);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			o.setEtat(type);
			res = o;
			
			final Pair<Long, EtatDeclaration> pair = new Pair<Long, EtatDeclaration>();
			pair.setFirst(declarationId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
