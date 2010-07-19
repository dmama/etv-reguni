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

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;

public class JdbcModeleFeuilleDocumentDaoImpl implements JdbcModeleFeuilleDocumentDao {

	private static final ModeleFeuilleDocumentMapper ROW_MAPPER = new ModeleFeuilleDocumentMapper();

	@SuppressWarnings({"unchecked"})
	public ModeleFeuilleDocument get(long forId, JdbcTemplate template) {
		final Pair<Long, ModeleFeuilleDocument> pair = (Pair<Long, ModeleFeuilleDocument>) DataAccessUtils.uniqueResult(template.query(ModeleFeuilleDocumentMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<ModeleFeuilleDocument> getForPeriode(long periodeId, JdbcTemplate template) {
		final List<Pair<Long, ModeleFeuilleDocument>> list = template.query(ModeleFeuilleDocumentMapper.selectByTiersId(), new Object[]{periodeId}, ROW_MAPPER);
		final HashSet<ModeleFeuilleDocument> set = new HashSet<ModeleFeuilleDocument>(list.size());
		for (Pair<Long, ModeleFeuilleDocument> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<ModeleFeuilleDocument>> getForPeriode(Collection<Long> periodeIds, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, ModeleFeuilleDocument>> list = CollectionsUtils.splitAndProcess(periodeIds, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, ModeleFeuilleDocument>>() {
			public List<Pair<Long, ModeleFeuilleDocument>> process(List<Long> ids) {
				return template.query(ModeleFeuilleDocumentMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<ModeleFeuilleDocument>> map = new HashMap<Long, Set<ModeleFeuilleDocument>>();
		for (Pair<Long, ModeleFeuilleDocument> pair : list) {
			Set<ModeleFeuilleDocument> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<ModeleFeuilleDocument>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class ModeleFeuilleDocumentMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ID, " + // 1
				"ANNULATION_DATE, " + // 2
				"ANNULATION_USER, " + // 3
				"INTITULE_FEUILLE, " + // 4
				"LOG_CDATE, " + // 5
				"LOG_CUSER, " + // 6
				"LOG_MDATE, " + // 7
				"LOG_MUSER, " + // 8
				"MODELE_ID, " + // 9
				"NO_FORMULAIRE " + // 10
				"from MODELE_FEUILLE_DOC";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where MODELE_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> periodeIds) {
			return BASE_SELECT + " where MODELE_ID in " + JdbcDaoUtils.buildInClause(periodeIds);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			final Long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final Timestamp temp2 = rs.getTimestamp(2);
			final Date annulationDate = (rs.wasNull() ? null : temp2);
			final String temp3 = rs.getString(3);
			final String annulationUser = (rs.wasNull() ? null : temp3);
			final String temp4 = rs.getString(4);
			final String intituleFeuille = (rs.wasNull() ? null : temp4);
			final Timestamp temp5 = rs.getTimestamp(5);
			final Date logCdate = (rs.wasNull() ? null : temp5);
			final String temp6 = rs.getString(6);
			final String logCuser = (rs.wasNull() ? null : temp6);
			final Timestamp temp7 = rs.getTimestamp(7);
			final Timestamp logMdate = (rs.wasNull() ? null : temp7);
			final String temp8 = rs.getString(8);
			final String logMuser = (rs.wasNull() ? null : temp8);
			final Long temp9 = rs.getLong(9);
			final Long modeleId = (rs.wasNull() ? null : temp9);
			final String temp10 = rs.getString(10);
			final String noFormulaire = (rs.wasNull() ? null : temp10);
			
			final ModeleFeuilleDocument res;
			
			ModeleFeuilleDocument o = new ModeleFeuilleDocument();
			o.setId(id);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setIntituleFeuille(intituleFeuille);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			o.setNumeroFormulaire(noFormulaire);
			res = o;
			
			final Pair<Long, ModeleFeuilleDocument> pair = new Pair<Long, ModeleFeuilleDocument>();
			pair.setFirst(modeleId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
