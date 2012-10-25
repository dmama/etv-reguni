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
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class JdbcPeriodiciteDaoImpl implements JdbcPeriodiciteDao {

	private static final PeriodiciteMapper ROW_MAPPER = new PeriodiciteMapper();

	@Override
	@SuppressWarnings({"unchecked"})
	public Periodicite get(long forId, JdbcTemplate template) {
		final Pair<Long, Periodicite> pair = (Pair<Long, Periodicite>) DataAccessUtils.uniqueResult(template.query(PeriodiciteMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<Periodicite> getForTiers(long tiersId, JdbcTemplate template) {
		final List<Pair<Long, Periodicite>> list = template.query(PeriodiciteMapper.selectByTiersId(), new Object[]{tiersId}, ROW_MAPPER);
		final HashSet<Periodicite> set = new HashSet<Periodicite>(list.size());
		for (Pair<Long, Periodicite> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<Periodicite>> getForTiers(Collection<Long> tiersId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, Periodicite>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, Periodicite>>() {
			@Override
			public List<Pair<Long, Periodicite>> process(List<Long> ids) {
				return template.query(PeriodiciteMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<Periodicite>> map = new HashMap<Long, Set<Periodicite>>();
		for (Pair<Long, Periodicite> pair : list) {
			Set<Periodicite> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<Periodicite>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class PeriodiciteMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ID, " + // 1
				"ANNULATION_DATE, " + // 2
				"ANNULATION_USER, " + // 3
				"DATE_DEBUT, " + // 4
				"DATE_FIN, " + // 5
				"DEBITEUR_ID, " + // 6
				"LOG_CDATE, " + // 7
				"LOG_CUSER, " + // 8
				"LOG_MDATE, " + // 9
				"LOG_MUSER, " + // 10
				"PERIODE_DECOMPTE, " + // 11
				"PERIODICITE_TYPE " + // 12
				"from PERIODICITE";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where DEBITEUR_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where DEBITEUR_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final long temp6 = rs.getLong(6);
			final Long debiteurId = (rs.wasNull() ? null : temp6);
			final Periodicite res;
			
			
			final long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final Date annulationDate = rs.getTimestamp(2);
			final String annulationUser = rs.getString(3);
			final int temp4 = rs.getInt(4);
			final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp4, false));
			final int temp5 = rs.getInt(5);
			final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
			final Date logCdate = rs.getTimestamp(7);
			final String logCuser = rs.getString(8);
			final Timestamp logMdate = rs.getTimestamp(9);
			final String logMuser = rs.getString(10);
			final String temp11 = rs.getString(11);
			final PeriodeDecompte periodeDecompte = (rs.wasNull() ? null : Enum.valueOf(PeriodeDecompte.class, temp11));
			final String temp12 = rs.getString(12);
			final PeriodiciteDecompte periodiciteType = (rs.wasNull() ? null : Enum.valueOf(PeriodiciteDecompte.class, temp12));
			
			Periodicite o = new Periodicite();
			o.setId(id);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setDateDebut(dateDebut);
			o.setDateFin(dateFin);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			o.setPeriodeDecompte(periodeDecompte);
			o.setPeriodiciteDecompte(periodiciteType);
			res = o;
			
			final Pair<Long, Periodicite> pair = new Pair<Long, Periodicite>();
			pair.setFirst(debiteurId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
