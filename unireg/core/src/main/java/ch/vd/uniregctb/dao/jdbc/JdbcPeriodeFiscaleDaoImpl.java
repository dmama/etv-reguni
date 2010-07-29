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
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;

public class JdbcPeriodeFiscaleDaoImpl implements JdbcPeriodeFiscaleDao {

	private static final PeriodeFiscaleMapper ROW_MAPPER = new PeriodeFiscaleMapper();

	private JdbcParametrePeriodeFiscaleDao ppfDao = new JdbcParametrePeriodeFiscaleDaoImpl();

	public PeriodeFiscale get(long periodeId, JdbcTemplate template) {

		final PeriodeFiscale periode = (PeriodeFiscale) DataAccessUtils.uniqueResult(template.query(PeriodeFiscaleMapper.selectById(), new Object[]{periodeId}, ROW_MAPPER));
		if (periode == null) {
			return null;
		}

		final Set<ParametrePeriodeFiscale> parametres = ppfDao.getForPeriode(periodeId, template);
		for (ParametrePeriodeFiscale p : parametres) {
			p.setPeriodefiscale(periode);
		}
		periode.setParametrePeriodeFiscale(parametres);

		return periode;
	}

	@SuppressWarnings({"unchecked"})
	public List<PeriodeFiscale> getList(final Collection<Long> periodesId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<PeriodeFiscale> list = CollectionsUtils.splitAndProcess(periodesId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, PeriodeFiscale>() {
			public List<PeriodeFiscale> process(List<Long> ids) {
				return template.query(PeriodeFiscaleMapper.selectByIds(ids), ROW_MAPPER);
			}
		});

		final Map<Long, Set<ParametrePeriodeFiscale>> map = ppfDao.getForPeriode(periodesId, template);
		for (PeriodeFiscale periode : list) {
			Set<ParametrePeriodeFiscale> parametres = map.get(periode.getId());
			if (parametres == null) {
				parametres = Collections.emptySet();
			}
			else {
				for (ParametrePeriodeFiscale p : parametres) {
					p.setPeriodefiscale(periode);
				}
			}
			periode.setParametrePeriodeFiscale(parametres);
		}

		return list;
	}
	
	private static class PeriodeFiscaleMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ID, " + // 1
				"ANNEE, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"LOG_CDATE, " + // 5
				"LOG_CUSER, " + // 6
				"LOG_MDATE, " + // 7
				"LOG_MUSER " + // 8
				"from PERIODE_FISCALE";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where NUMERO in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final PeriodeFiscale res;
			
			
			final long temp1 = rs.getLong(1);
			final Long id = (rs.wasNull() ? null : temp1);
			final int temp2 = rs.getInt(2);
			final Integer annee = (rs.wasNull() ? null : temp2);
			final Date annulationDate = rs.getTimestamp(3);
			final String annulationUser = rs.getString(4);
			final Date logCdate = rs.getTimestamp(5);
			final String logCuser = rs.getString(6);
			final Timestamp logMdate = rs.getTimestamp(7);
			final String logMuser = rs.getString(8);
			
			PeriodeFiscale o = new PeriodeFiscale();
			o.setId(id);
			o.setAnnee(annee);
			o.setAnnulationDate(annulationDate);
			o.setAnnulationUser(annulationUser);
			o.setLogCreationDate(logCdate);
			o.setLogCreationUser(logCuser);
			o.setLogModifDate(logMdate);
			o.setLogModifUser(logMuser);
			res = o;
			
			return res;
		}
	}
}
