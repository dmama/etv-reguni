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
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.type.CategorieIdentifiant;

public class JdbcIdentificationPersonneDaoImpl implements JdbcIdentificationPersonneDao {

	private static final IdentificationPersonneMapper ROW_MAPPER = new IdentificationPersonneMapper();

	@SuppressWarnings({"unchecked"})
	public IdentificationPersonne get(long forId, JdbcTemplate template) {
		final Pair<Long, IdentificationPersonne> pair = (Pair<Long, IdentificationPersonne>) DataAccessUtils.uniqueResult(template.query(IdentificationPersonneMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<IdentificationPersonne> getForTiers(long tiersId, JdbcTemplate template) {
		final List<Pair<Long, IdentificationPersonne>> list = template.query(IdentificationPersonneMapper.selectByTiersId(), new Object[]{tiersId}, ROW_MAPPER);
		final HashSet<IdentificationPersonne> set = new HashSet<IdentificationPersonne>(list.size());
		for (Pair<Long, IdentificationPersonne> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<IdentificationPersonne>> getForTiers(Collection<Long> tiersId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, IdentificationPersonne>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, IdentificationPersonne>>() {
			public List<Pair<Long, IdentificationPersonne>> process(List<Long> ids) {
				return template.query(IdentificationPersonneMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<IdentificationPersonne>> map = new HashMap<Long, Set<IdentificationPersonne>>();
		for (Pair<Long, IdentificationPersonne> pair : list) {
			Set<IdentificationPersonne> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<IdentificationPersonne>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class IdentificationPersonneMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ID, " + // 1
				"ANNULATION_DATE, " + // 2
				"ANNULATION_USER, " + // 3
				"CATEGORIE, " + // 4
				"IDENTIFIANT, " + // 5
				"LOG_CDATE, " + // 6
				"LOG_CUSER, " + // 7
				"LOG_MDATE, " + // 8
				"LOG_MUSER, " + // 9
				"NON_HABITANT_ID " + // 10
				"from IDENTIFICATION_PERSONNE";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where NON_HABITANT_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where NON_HABITANT_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			final Long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final Timestamp temp2 = rs.getTimestamp(2);
			final Date annulationDate = (rs.wasNull() ? null : temp2);
			final String temp3 = rs.getString(3);
			final String annulationUser = (rs.wasNull() ? null : temp3);
			final String temp4 = rs.getString(4);
			final CategorieIdentifiant categorie = (rs.wasNull() ? null : Enum.valueOf(CategorieIdentifiant.class, temp4));
			final String temp5 = rs.getString(5);
			final String identifiant = (rs.wasNull() ? null : temp5);
			final Timestamp temp6 = rs.getTimestamp(6);
			final Date logCdate = (rs.wasNull() ? null : temp6);
			final String temp7 = rs.getString(7);
			final String logCuser = (rs.wasNull() ? null : temp7);
			final Timestamp temp8 = rs.getTimestamp(8);
			final Timestamp logMdate = (rs.wasNull() ? null : temp8);
			final String temp9 = rs.getString(9);
			final String logMuser = (rs.wasNull() ? null : temp9);
			final Long temp10 = rs.getLong(10);
			final Long nonHabitantId = (rs.wasNull() ? null : temp10);
			
			final IdentificationPersonne res;
			
			IdentificationPersonne o = new IdentificationPersonne();
			o.setId(id);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setCategorieIdentifiant(categorie);
			o.setIdentifiant(identifiant);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			res = o;
			
			final Pair<Long, IdentificationPersonne> pair = new Pair<Long, IdentificationPersonne>();
			pair.setFirst(nonHabitantId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
