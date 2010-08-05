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
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class JdbcForFiscalDaoImpl implements JdbcForFiscalDao {

	private static final ForFiscalMapper ROW_MAPPER = new ForFiscalMapper();

	@SuppressWarnings({"unchecked"})
	public ForFiscal get(long forId, JdbcTemplate template) {
		final Pair<Long, ForFiscal> pair = (Pair<Long, ForFiscal>) DataAccessUtils.uniqueResult(template.query(ForFiscalMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<ForFiscal> getForTiers(long tiersId, JdbcTemplate template) {
		final List<Pair<Long, ForFiscal>> list = template.query(ForFiscalMapper.selectByTiersId(), new Object[]{tiersId}, ROW_MAPPER);
		final HashSet<ForFiscal> set = new HashSet<ForFiscal>(list.size());
		for (Pair<Long, ForFiscal> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<ForFiscal>> getForTiers(Collection<Long> tiersId, final JdbcTemplate template) {

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, ForFiscal>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, ForFiscal>>() {
			public List<Pair<Long, ForFiscal>> process(List<Long> ids) {
				return template.query(ForFiscalMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<ForFiscal>> map = new HashMap<Long, Set<ForFiscal>>();
		for (Pair<Long, ForFiscal> pair : list) {
			Set<ForFiscal> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<ForFiscal>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class ForFiscalMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"FOR_TYPE, " + // 1
				"ID, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"DATE_FERMETURE, " + // 5
				"DATE_OUVERTURE, " + // 6
				"GENRE_IMPOT, " + // 7
				"LOG_CDATE, " + // 8
				"LOG_CUSER, " + // 9
				"LOG_MDATE, " + // 10
				"LOG_MUSER, " + // 11
				"MODE_IMPOSITION, " + // 12
				"MOTIF_FERMETURE, " + // 13
				"MOTIF_OUVERTURE, " + // 14
				"MOTIF_RATTACHEMENT, " + // 15
				"NUMERO_OFS, " + // 16
				"TIERS_ID, " + // 17
				"TYPE_AUT_FISC " + // 18
				"from FOR_FISCAL";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where TIERS_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where TIERS_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String forType = rs.getString(1);
			final long temp17 = rs.getLong(17);
			final Long tiersId = (rs.wasNull() ? null : temp17);
			final ForFiscal res;
			
			if (forType.equals("ForFiscalPrincipal")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateFermeture = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateOuverture = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final String temp7 = rs.getString(7);
				final GenreImpot genreImpot = (rs.wasNull() ? null : Enum.valueOf(GenreImpot.class, temp7));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
				final String temp12 = rs.getString(12);
				final ModeImposition modeImposition = (rs.wasNull() ? null : Enum.valueOf(ModeImposition.class, temp12));
				final String temp13 = rs.getString(13);
				final MotifFor motifFermeture = (rs.wasNull() ? null : Enum.valueOf(MotifFor.class, temp13));
				final String temp14 = rs.getString(14);
				final MotifFor motifOuverture = (rs.wasNull() ? null : Enum.valueOf(MotifFor.class, temp14));
				final String temp15 = rs.getString(15);
				final MotifRattachement motifRattachement = (rs.wasNull() ? null : Enum.valueOf(MotifRattachement.class, temp15));
				final int temp16 = rs.getInt(16);
				final Integer numeroOfs = (rs.wasNull() ? null : temp16);
				final String temp18 = rs.getString(18);
				final TypeAutoriteFiscale typeAutFisc = (rs.wasNull() ? null : Enum.valueOf(TypeAutoriteFiscale.class, temp18));
			
				ForFiscalPrincipal o = new ForFiscalPrincipal();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateFin(dateFermeture);
				o.setDateDebut(dateOuverture);
				o.setGenreImpot(genreImpot);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setModeImposition(modeImposition);
				o.setMotifFermeture(motifFermeture);
				o.setMotifOuverture(motifOuverture);
				o.setMotifRattachement(motifRattachement);
				o.setNumeroOfsAutoriteFiscale(numeroOfs);
				o.setTypeAutoriteFiscale(typeAutFisc);
				res = o;
			}
			else if (forType.equals("ForFiscalSecondaire")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateFermeture = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateOuverture = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final String temp7 = rs.getString(7);
				final GenreImpot genreImpot = (rs.wasNull() ? null : Enum.valueOf(GenreImpot.class, temp7));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
				final String temp13 = rs.getString(13);
				final MotifFor motifFermeture = (rs.wasNull() ? null : Enum.valueOf(MotifFor.class, temp13));
				final String temp14 = rs.getString(14);
				final MotifFor motifOuverture = (rs.wasNull() ? null : Enum.valueOf(MotifFor.class, temp14));
				final String temp15 = rs.getString(15);
				final MotifRattachement motifRattachement = (rs.wasNull() ? null : Enum.valueOf(MotifRattachement.class, temp15));
				final int temp16 = rs.getInt(16);
				final Integer numeroOfs = (rs.wasNull() ? null : temp16);
				final String temp18 = rs.getString(18);
				final TypeAutoriteFiscale typeAutFisc = (rs.wasNull() ? null : Enum.valueOf(TypeAutoriteFiscale.class, temp18));
			
				ForFiscalSecondaire o = new ForFiscalSecondaire();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateFin(dateFermeture);
				o.setDateDebut(dateOuverture);
				o.setGenreImpot(genreImpot);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setMotifFermeture(motifFermeture);
				o.setMotifOuverture(motifOuverture);
				o.setMotifRattachement(motifRattachement);
				o.setNumeroOfsAutoriteFiscale(numeroOfs);
				o.setTypeAutoriteFiscale(typeAutFisc);
				res = o;
			}
			else if (forType.equals("ForFiscalAutreElementImposable")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateFermeture = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateOuverture = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final String temp7 = rs.getString(7);
				final GenreImpot genreImpot = (rs.wasNull() ? null : Enum.valueOf(GenreImpot.class, temp7));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
				final String temp13 = rs.getString(13);
				final MotifFor motifFermeture = (rs.wasNull() ? null : Enum.valueOf(MotifFor.class, temp13));
				final String temp14 = rs.getString(14);
				final MotifFor motifOuverture = (rs.wasNull() ? null : Enum.valueOf(MotifFor.class, temp14));
				final String temp15 = rs.getString(15);
				final MotifRattachement motifRattachement = (rs.wasNull() ? null : Enum.valueOf(MotifRattachement.class, temp15));
				final int temp16 = rs.getInt(16);
				final Integer numeroOfs = (rs.wasNull() ? null : temp16);
				final String temp18 = rs.getString(18);
				final TypeAutoriteFiscale typeAutFisc = (rs.wasNull() ? null : Enum.valueOf(TypeAutoriteFiscale.class, temp18));
			
				ForFiscalAutreElementImposable o = new ForFiscalAutreElementImposable();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateFin(dateFermeture);
				o.setDateDebut(dateOuverture);
				o.setGenreImpot(genreImpot);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setMotifFermeture(motifFermeture);
				o.setMotifOuverture(motifOuverture);
				o.setMotifRattachement(motifRattachement);
				o.setNumeroOfsAutoriteFiscale(numeroOfs);
				o.setTypeAutoriteFiscale(typeAutFisc);
				res = o;
			}
			else if (forType.equals("ForFiscalAutreImpot")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateFermeture = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateOuverture = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final String temp7 = rs.getString(7);
				final GenreImpot genreImpot = (rs.wasNull() ? null : Enum.valueOf(GenreImpot.class, temp7));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
				final int temp16 = rs.getInt(16);
				final Integer numeroOfs = (rs.wasNull() ? null : temp16);
				final String temp18 = rs.getString(18);
				final TypeAutoriteFiscale typeAutFisc = (rs.wasNull() ? null : Enum.valueOf(TypeAutoriteFiscale.class, temp18));
			
				ForFiscalAutreImpot o = new ForFiscalAutreImpot();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateFin(dateFermeture);
				o.setDateDebut(dateOuverture);
				o.setGenreImpot(genreImpot);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroOfsAutoriteFiscale(numeroOfs);
				o.setTypeAutoriteFiscale(typeAutFisc);
				res = o;
			}
			else if (forType.equals("ForDebiteurPrestationImposable")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateFermeture = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateOuverture = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final String temp7 = rs.getString(7);
				final GenreImpot genreImpot = (rs.wasNull() ? null : Enum.valueOf(GenreImpot.class, temp7));
				final Date logCdate = rs.getTimestamp(8);
				final String logCuser = rs.getString(9);
				final Timestamp logMdate = rs.getTimestamp(10);
				final String logMuser = rs.getString(11);
				final int temp16 = rs.getInt(16);
				final Integer numeroOfs = (rs.wasNull() ? null : temp16);
				final String temp18 = rs.getString(18);
				final TypeAutoriteFiscale typeAutFisc = (rs.wasNull() ? null : Enum.valueOf(TypeAutoriteFiscale.class, temp18));
			
				ForDebiteurPrestationImposable o = new ForDebiteurPrestationImposable();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateFin(dateFermeture);
				o.setDateDebut(dateOuverture);
				o.setGenreImpot(genreImpot);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroOfsAutoriteFiscale(numeroOfs);
				o.setTypeAutoriteFiscale(typeAutFisc);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + forType + "]");
			}
			
			final Pair<Long, ForFiscal> pair = new Pair<Long, ForFiscal>();
			pair.setFirst(tiersId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
