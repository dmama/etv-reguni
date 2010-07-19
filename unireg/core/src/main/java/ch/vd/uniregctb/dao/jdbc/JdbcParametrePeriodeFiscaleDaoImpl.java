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
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.type.TypeContribuable;

public class JdbcParametrePeriodeFiscaleDaoImpl implements JdbcParametrePeriodeFiscaleDao {

	private static final ParametrePeriodeFiscaleMapper ROW_MAPPER = new ParametrePeriodeFiscaleMapper();

	@SuppressWarnings({"unchecked"})
	public ParametrePeriodeFiscale get(long forId, JdbcTemplate template) {
		final Pair<Long, ParametrePeriodeFiscale> pair = (Pair<Long, ParametrePeriodeFiscale>) DataAccessUtils.uniqueResult(template.query(ParametrePeriodeFiscaleMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<ParametrePeriodeFiscale> getForPeriode(long periodeId, JdbcTemplate template) {
		final List<Pair<Long, ParametrePeriodeFiscale>> list = template.query(ParametrePeriodeFiscaleMapper.selectByTiersId(), new Object[]{periodeId}, ROW_MAPPER);
		final HashSet<ParametrePeriodeFiscale> set = new HashSet<ParametrePeriodeFiscale>(list.size());
		for (Pair<Long, ParametrePeriodeFiscale> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<ParametrePeriodeFiscale>> getForPeriode(Collection<Long> periodeIds, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, ParametrePeriodeFiscale>> list = CollectionsUtils.splitAndProcess(periodeIds, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, ParametrePeriodeFiscale>>() {
			public List<Pair<Long, ParametrePeriodeFiscale>> process(List<Long> ids) {
				return template.query(ParametrePeriodeFiscaleMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<ParametrePeriodeFiscale>> map = new HashMap<Long, Set<ParametrePeriodeFiscale>>();
		for (Pair<Long, ParametrePeriodeFiscale> pair : list) {
			Set<ParametrePeriodeFiscale> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<ParametrePeriodeFiscale>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class ParametrePeriodeFiscaleMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ID, " + // 1
				"ANNULATION_DATE, " + // 2
				"ANNULATION_USER, " + // 3
				"DATE_FIN_ENVOI_MASSE, " + // 4
				"LOG_CDATE, " + // 5
				"LOG_CUSER, " + // 6
				"LOG_MDATE, " + // 7
				"LOG_MUSER, " + // 8
				"PERIODE_ID, " + // 9
				"TERME_GEN_SOMM_EFFECT, " + // 10
				"TERME_GEN_SOMM_REGL, " + // 11
				"TYPE_CTB " + // 12
				"from PARAMETRE_PERIODE_FISCALE";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where PERIODE_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> periodeIds) {
			return BASE_SELECT + " where PERIODE_ID in " + JdbcDaoUtils.buildInClause(periodeIds);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			final Long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final Timestamp temp2 = rs.getTimestamp(2);
			final Date annulationDate = (rs.wasNull() ? null : temp2);
			final String temp3 = rs.getString(3);
			final String annulationUser = (rs.wasNull() ? null : temp3);
			final Integer temp4 = rs.getInt(4);
			final RegDate dateFinEnvoiMasse = (rs.wasNull() ? null : RegDate.fromIndex(temp4, false));
			final Timestamp temp5 = rs.getTimestamp(5);
			final Date logCdate = (rs.wasNull() ? null : temp5);
			final String temp6 = rs.getString(6);
			final String logCuser = (rs.wasNull() ? null : temp6);
			final Timestamp temp7 = rs.getTimestamp(7);
			final Timestamp logMdate = (rs.wasNull() ? null : temp7);
			final String temp8 = rs.getString(8);
			final String logMuser = (rs.wasNull() ? null : temp8);
			final Long temp9 = rs.getLong(9);
			final Long periodeId = (rs.wasNull() ? null : temp9);
			final Integer temp10 = rs.getInt(10);
			final RegDate termeGenSommEffect = (rs.wasNull() ? null : RegDate.fromIndex(temp10, false));
			final Integer temp11 = rs.getInt(11);
			final RegDate termeGenSommRegl = (rs.wasNull() ? null : RegDate.fromIndex(temp11, false));
			final String temp12 = rs.getString(12);
			final TypeContribuable typeCtb = (rs.wasNull() ? null : Enum.valueOf(TypeContribuable.class, temp12));
			
			final ParametrePeriodeFiscale res;
			
			ParametrePeriodeFiscale o = new ParametrePeriodeFiscale();
			o.setId(id);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setDateFinEnvoiMasseDI(dateFinEnvoiMasse);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			o.setTermeGeneralSommationEffectif(termeGenSommEffect);
			o.setTermeGeneralSommationReglementaire(termeGenSommRegl);
			o.setTypeContribuable(typeCtb);
			res = o;
			
			final Pair<Long, ParametrePeriodeFiscale> pair = new Pair<Long, ParametrePeriodeFiscale>();
			pair.setFirst(periodeId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
