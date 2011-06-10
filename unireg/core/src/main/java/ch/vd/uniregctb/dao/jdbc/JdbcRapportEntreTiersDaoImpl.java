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
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.TypeActivite;

public class JdbcRapportEntreTiersDaoImpl implements JdbcRapportEntreTiersDao {

	private static final RapportEntreTiersMapper ROW_MAPPER = new RapportEntreTiersMapper();

	@Override
	@SuppressWarnings({"unchecked"})
	public RapportEntreTiers get(long forId, JdbcTemplate template) {
		final Pair<Long, RapportEntreTiers> pair = (Pair<Long, RapportEntreTiers>) DataAccessUtils.uniqueResult(template.query(RapportEntreTiersMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<RapportEntreTiers> getForTiersSujet(long tiersId, JdbcTemplate template) {
		final List<RapportEntreTiers> list = template.query(RapportEntreTiersMapper.selectByTiersSujetId(), new Object[]{tiersId}, ROW_MAPPER);
		return new HashSet<RapportEntreTiers>(list);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<RapportEntreTiers> getForTiersObjet(long tiersId, JdbcTemplate template) {
		final List<RapportEntreTiers> list = template.query(RapportEntreTiersMapper.selectByTiersObjetId(), new Object[]{tiersId}, ROW_MAPPER);
		return new HashSet<RapportEntreTiers>(list);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<RapportEntreTiers>> getForTiersSujet(Collection<Long> tiersId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<RapportEntreTiers> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, RapportEntreTiers>() {
			@Override
			public List<RapportEntreTiers> process(List<Long> ids) {
				return template.query(RapportEntreTiersMapper.selectByTiersSujetIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<RapportEntreTiers>> map = new HashMap<Long, Set<RapportEntreTiers>>();
		for (RapportEntreTiers ret : list) {
			Set<RapportEntreTiers> set = map.get(ret.getSujetId());
			if (set == null) {
				set = new HashSet<RapportEntreTiers>();
				map.put(ret.getSujetId(), set);
			}
			set.add(ret);
		}
		return map;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<RapportEntreTiers>> getForTiersObjet(Collection<Long> tiersId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<RapportEntreTiers> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, RapportEntreTiers>() {
			@Override
			public List<RapportEntreTiers> process(List<Long> ids) {
				return template.query(RapportEntreTiersMapper.selectByTiersObjetIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<RapportEntreTiers>> map = new HashMap<Long, Set<RapportEntreTiers>>();
		for (RapportEntreTiers ret : list) {
			Set<RapportEntreTiers> set = map.get(ret.getObjetId());
			if (set == null) {
				set = new HashSet<RapportEntreTiers>();
				map.put(ret.getObjetId(), set);
			}
			set.add(ret);
		}
		return map;
	}

	private static class RapportEntreTiersMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"RAPPORT_ENTRE_TIERS_TYPE, " + // 1
				"ID, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"DATE_DEBUT, " + // 5
				"DATE_FIN, " + // 6
				"DATE_FIN_DER_ELE_IMP, " + // 7
				"EXTENSION_EXECUTION_FORCEE, " + // 8
				"LOG_CDATE, " + // 9
				"LOG_CUSER, " + // 10
				"LOG_MDATE, " + // 11
				"LOG_MUSER, " + // 12
				"TAUX_ACTIVITE, " + // 13
				"TIERS_OBJET_ID, " + // 14
				"TIERS_SUJET_ID, " + // 15
				"TIERS_TUTEUR_ID, " + // 16
				"TYPE_ACTIVITE " + // 17
				"from RAPPORT_ENTRE_TIERS";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersSujetId() {
			return BASE_SELECT + " where TIERS_SUJET_ID = ?";
		}

		public static String selectByTiersObjetId() {
			return BASE_SELECT + " where TIERS_OBJET_ID = ?";
		}

		public static String selectByTiersSujetIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where TIERS_SUJET_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public static String selectByTiersObjetIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where TIERS_OBJET_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String rapportEntreTiersType = rs.getString(1);
			final RapportEntreTiers res;
			
			if (rapportEntreTiersType.equals("Tutelle")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
				final long temp16 = rs.getLong(16);
				final Long tiersTuteurId = (rs.wasNull() ? null : temp16);
			
				Tutelle o = new Tutelle();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				o.setAutoriteTutelaireId(tiersTuteurId);
				res = o;
			}
			else if (rapportEntreTiersType.equals("Curatelle")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
				final long temp16 = rs.getLong(16);
				final Long tiersTuteurId = (rs.wasNull() ? null : temp16);
			
				Curatelle o = new Curatelle();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				o.setAutoriteTutelaireId(tiersTuteurId);
				res = o;
			}
			else if (rapportEntreTiersType.equals("ConseilLegal")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
				final long temp16 = rs.getLong(16);
				final Long tiersTuteurId = (rs.wasNull() ? null : temp16);
			
				ConseilLegal o = new ConseilLegal();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				o.setAutoriteTutelaireId(tiersTuteurId);
				res = o;
			}
			else if (rapportEntreTiersType.equals("AnnuleEtRemplace")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
			
				AnnuleEtRemplace o = new AnnuleEtRemplace();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				res = o;
			}
			else if (rapportEntreTiersType.equals("AppartenanceMenage")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
			
				AppartenanceMenage o = new AppartenanceMenage();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				res = o;
			}
			else if (rapportEntreTiersType.equals("RapportPrestationImposable")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final int temp7 = rs.getInt(7);
				final RegDate dateFinDerEleImp = (rs.wasNull() ? null : RegDate.fromIndex(temp7, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final int temp13 = rs.getInt(13);
				final Integer tauxActivite = (rs.wasNull() ? null : temp13);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
				final String temp17 = rs.getString(17);
				final TypeActivite typeActivite = (rs.wasNull() ? null : Enum.valueOf(TypeActivite.class, temp17));
			
				RapportPrestationImposable o = new RapportPrestationImposable();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setFinDernierElementImposable(dateFinDerEleImp);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setTauxActivite(tauxActivite);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				o.setTypeActivite(typeActivite);
				res = o;
			}
			else if (rapportEntreTiersType.equals("RepresentationConventionnelle")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final boolean temp8 = rs.getBoolean(8);
				final Boolean extensionExecutionForcee = (rs.wasNull() ? null : temp8);
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
			
				RepresentationConventionnelle o = new RepresentationConventionnelle();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setExtensionExecutionForcee(extensionExecutionForcee);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				res = o;
			}
			else if (rapportEntreTiersType.equals("ContactImpotSource")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp14 = rs.getLong(14);
				final Long tiersObjetId = (rs.wasNull() ? null : temp14);
				final long temp15 = rs.getLong(15);
				final Long tiersSujetId = (rs.wasNull() ? null : temp15);
			
				ContactImpotSource o = new ContactImpotSource();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setObjetId(tiersObjetId);
				o.setSujetId(tiersSujetId);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + rapportEntreTiersType + "]");
			}
			
			return res;
		}
	}
}
