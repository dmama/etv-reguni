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
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TarifImpotSource;

public class JdbcSituationFamilleDaoImpl implements JdbcSituationFamilleDao {

	private static final SituationFamilleMapper ROW_MAPPER = new SituationFamilleMapper();

	@SuppressWarnings({"unchecked"})
	public SituationFamille get(long forId, JdbcTemplate template) {
		final Pair<Long, SituationFamille> pair = (Pair<Long, SituationFamille>) DataAccessUtils.uniqueResult(template.query(SituationFamilleMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<SituationFamille> getForTiers(long tiersId, JdbcTemplate template) {
		final List<Pair<Long, SituationFamille>> list = template.query(SituationFamilleMapper.selectByTiersId(), new Object[]{tiersId}, ROW_MAPPER);
		final HashSet<SituationFamille> set = new HashSet<SituationFamille>(list.size());
		for (Pair<Long, SituationFamille> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<SituationFamille>> getForTiers(Collection<Long> tiersId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, SituationFamille>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, SituationFamille>>() {
			public List<Pair<Long, SituationFamille>> process(List<Long> ids) {
				return template.query(SituationFamilleMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<SituationFamille>> map = new HashMap<Long, Set<SituationFamille>>();
		for (Pair<Long, SituationFamille> pair : list) {
			Set<SituationFamille> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<SituationFamille>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class SituationFamilleMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"SITUATION_FAMILLE_TYPE, " + // 1
				"ID, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"CTB_ID, " + // 5
				"DATE_DEBUT, " + // 6
				"DATE_FIN, " + // 7
				"ETAT_CIVIL, " + // 8
				"LOG_CDATE, " + // 9
				"LOG_CUSER, " + // 10
				"LOG_MDATE, " + // 11
				"LOG_MUSER, " + // 12
				"NOMBRE_ENFANTS, " + // 13
				"TARIF_APPLICABLE, " + // 14
				"TIERS_PRINCIPAL_ID " + // 15
				"from SITUATION_FAMILLE";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where CTB_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where CTB_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String situationFamilleType = rs.getString(1);
			final long temp5 = rs.getLong(5);
			final Long ctbId = (rs.wasNull() ? null : temp5);
			final SituationFamille res;
			
			if (situationFamilleType.equals("SituationFamille")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp6 = rs.getInt(6);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final int temp7 = rs.getInt(7);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp7, false));
				final String temp8 = rs.getString(8);
				final EtatCivil etatCivil = (rs.wasNull() ? null : Enum.valueOf(EtatCivil.class, temp8));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final int temp13 = rs.getInt(13);
				final Integer nombreEnfants = (rs.wasNull() ? null : temp13);
			
				SituationFamillePersonnePhysique o = new SituationFamillePersonnePhysique();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setEtatCivil(etatCivil);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNombreEnfants(nombreEnfants);
				res = o;
			}
			else if (situationFamilleType.equals("SituationFamilleMenageCommun")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp6 = rs.getInt(6);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final int temp7 = rs.getInt(7);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp7, false));
				final String temp8 = rs.getString(8);
				final EtatCivil etatCivil = (rs.wasNull() ? null : Enum.valueOf(EtatCivil.class, temp8));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final int temp13 = rs.getInt(13);
				final Integer nombreEnfants = (rs.wasNull() ? null : temp13);
				final String temp14 = rs.getString(14);
				final TarifImpotSource tarifApplicable = (rs.wasNull() ? null : Enum.valueOf(TarifImpotSource.class, temp14));
				final long temp15 = rs.getLong(15);
				final Long tiersPrincipalId = (rs.wasNull() ? null : temp15);
			
				SituationFamilleMenageCommun o = new SituationFamilleMenageCommun();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setEtatCivil(etatCivil);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNombreEnfants(nombreEnfants);
				o.setTarifApplicable(tarifApplicable);
				o.setContribuablePrincipalId(tiersPrincipalId);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + situationFamilleType + "]");
			}
			
			final Pair<Long, SituationFamille> pair = new Pair<Long, SituationFamille>();
			pair.setFirst(ctbId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
