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
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdressePM;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class JdbcAdresseTiersDaoImpl implements JdbcAdresseTiersDao {

	private static final AdresseTiersMapper ROW_MAPPER = new AdresseTiersMapper();

	@Override
	@SuppressWarnings({"unchecked"})
	public AdresseTiers get(long forId, JdbcTemplate template) {
		final Pair<Long, AdresseTiers> pair = (Pair<Long, AdresseTiers>) DataAccessUtils.uniqueResult(template.query(AdresseTiersMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<AdresseTiers> getForTiers(long tiersId, JdbcTemplate template) {
		final List<Pair<Long, AdresseTiers>> list = template.query(AdresseTiersMapper.selectByTiersId(), new Object[]{tiersId}, ROW_MAPPER);
		final HashSet<AdresseTiers> set = new HashSet<AdresseTiers>(list.size());
		for (Pair<Long, AdresseTiers> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<AdresseTiers>> getForTiers(Collection<Long> tiersId, final JdbcTemplate template) {
		
		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, AdresseTiers>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, AdresseTiers>>() {
			@Override
			public List<Pair<Long, AdresseTiers>> process(List<Long> ids) {
				return template.query(AdresseTiersMapper.selectByTiersIds(ids), ROW_MAPPER);
			}
		});

		final HashMap<Long, Set<AdresseTiers>> map = new HashMap<Long, Set<AdresseTiers>>();
		for (Pair<Long, AdresseTiers> pair : list) {
			Set<AdresseTiers> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<AdresseTiers>();
				map.put(pair.getFirst(), set);
			}
			set.add(pair.getSecond());
		}
		return map;
	}

	private static class AdresseTiersMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"ADR_TYPE, " + // 1
				"ID, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"AUTRE_TIERS_ID, " + // 5
				"AUTRE_TYPE, " + // 6
				"COMPLEMENT, " + // 7
				"COMPLEMENT_LOCALITE, " + // 8
				"DATE_DEBUT, " + // 9
				"DATE_FIN, " + // 10
				"LOG_CDATE, " + // 11
				"LOG_CUSER, " + // 12
				"LOG_MDATE, " + // 13
				"LOG_MUSER, " + // 14
				"NUMERO_APPARTEMENT, " + // 15
				"NUMERO_CASE_POSTALE, " + // 16
				"NUMERO_MAISON, " + // 17
				"NUMERO_OFS_PAYS, " + // 18
				"NUMERO_ORDRE_POSTE, " + // 19
				"NUMERO_POSTAL_LOCALITE, " + // 20
				"NUMERO_RUE, " + // 21
				"PERMANENTE, " + // 22
				"RUE, " + // 23
				"STYPE, " + // 24
				"TEXTE_CASE_POSTALE, " + // 25
				"TIERS_ID, " + // 26
				"TYPE_PM, " + // 27
				"USAGE_TYPE " + // 28
				"from ADRESSE_TIERS";

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where TIERS_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where TIERS_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String adrType = rs.getString(1);
			final long temp26 = rs.getLong(26);
			final Long tiersId = (rs.wasNull() ? null : temp26);
			final AdresseTiers res;
			
			if (adrType.equals("AdresseSuisse")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final String complement = rs.getString(7);
				final int temp9 = rs.getInt(9);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp9, false));
				final int temp10 = rs.getInt(10);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp10, false));
				final Date logCdate = rs.getTimestamp(11);
				final String logCuser = rs.getString(12);
				final Timestamp logMdate = rs.getTimestamp(13);
				final String logMuser = rs.getString(14);
				final String numeroAppartement = rs.getString(15);
				final int temp16 = rs.getInt(16);
				final Integer numeroCasePostale = (rs.wasNull() ? null : temp16);
				final String numeroMaison = rs.getString(17);
				final int temp19 = rs.getInt(19);
				final Integer numeroOrdrePoste = (rs.wasNull() ? null : temp19);
				final int temp21 = rs.getInt(21);
				final Integer numeroRue = (rs.wasNull() ? null : temp21);
				final boolean temp22 = rs.getBoolean(22);
				final Boolean permanente = (rs.wasNull() ? null : temp22);
				final String rue = rs.getString(23);
				final String temp25 = rs.getString(25);
				final TexteCasePostale texteCasePostale = (rs.wasNull() ? null : Enum.valueOf(TexteCasePostale.class, temp25));
				final String temp28 = rs.getString(28);
				final TypeAdresseTiers usageType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp28));
			
				AdresseSuisse o = new AdresseSuisse();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setComplement(complement);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroAppartement(numeroAppartement);
				o.setNumeroCasePostale(numeroCasePostale);
				o.setNumeroMaison(numeroMaison);
				o.setNumeroOrdrePoste(numeroOrdrePoste);
				o.setNumeroRue(numeroRue);
				o.setPermanente(permanente);
				o.setRue(rue);
				o.setTexteCasePostale(texteCasePostale);
				o.setUsage(usageType);
				res = o;
			}
			else if (adrType.equals("AdresseEtrangere")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final String complement = rs.getString(7);
				final String complementLocalite = rs.getString(8);
				final int temp9 = rs.getInt(9);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp9, false));
				final int temp10 = rs.getInt(10);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp10, false));
				final Date logCdate = rs.getTimestamp(11);
				final String logCuser = rs.getString(12);
				final Timestamp logMdate = rs.getTimestamp(13);
				final String logMuser = rs.getString(14);
				final String numeroAppartement = rs.getString(15);
				final int temp16 = rs.getInt(16);
				final Integer numeroCasePostale = (rs.wasNull() ? null : temp16);
				final String numeroMaison = rs.getString(17);
				final int temp18 = rs.getInt(18);
				final Integer numeroOfsPays = (rs.wasNull() ? null : temp18);
				final String numeroPostalLocalite = rs.getString(20);
				final boolean temp22 = rs.getBoolean(22);
				final Boolean permanente = (rs.wasNull() ? null : temp22);
				final String rue = rs.getString(23);
				final String temp25 = rs.getString(25);
				final TexteCasePostale texteCasePostale = (rs.wasNull() ? null : Enum.valueOf(TexteCasePostale.class, temp25));
				final String temp28 = rs.getString(28);
				final TypeAdresseTiers usageType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp28));
			
				AdresseEtrangere o = new AdresseEtrangere();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setComplement(complement);
				o.setComplementLocalite(complementLocalite);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroAppartement(numeroAppartement);
				o.setNumeroCasePostale(numeroCasePostale);
				o.setNumeroMaison(numeroMaison);
				o.setNumeroOfsPays(numeroOfsPays);
				o.setNumeroPostalLocalite(numeroPostalLocalite);
				o.setPermanente(permanente);
				o.setRue(rue);
				o.setTexteCasePostale(texteCasePostale);
				o.setUsage(usageType);
				res = o;
			}
			else if (adrType.equals("AdresseCivile")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp9 = rs.getInt(9);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp9, false));
				final int temp10 = rs.getInt(10);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp10, false));
				final Date logCdate = rs.getTimestamp(11);
				final String logCuser = rs.getString(12);
				final Timestamp logMdate = rs.getTimestamp(13);
				final String logMuser = rs.getString(14);
				final TypeAdresseCivil stype = TypeAdresseCivil.fromDbValue(rs.getString(24));
				final String temp28 = rs.getString(28);
				final TypeAdresseTiers usageType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp28));
			
				AdresseCivile o = new AdresseCivile();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setType(stype);
				o.setUsage(usageType);
				res = o;
			}
			else if (adrType.equals("AdressePM")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp9 = rs.getInt(9);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp9, false));
				final int temp10 = rs.getInt(10);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp10, false));
				final Date logCdate = rs.getTimestamp(11);
				final String logCuser = rs.getString(12);
				final Timestamp logMdate = rs.getTimestamp(13);
				final String logMuser = rs.getString(14);
				final String temp27 = rs.getString(27);
				final TypeAdressePM typePm = (rs.wasNull() ? null : Enum.valueOf(TypeAdressePM.class, temp27));
				final String temp28 = rs.getString(28);
				final TypeAdresseTiers usageType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp28));
			
				AdressePM o = new AdressePM();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setType(typePm);
				o.setUsage(usageType);
				res = o;
			}
			else if (adrType.equals("AdresseAutreTiers")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final long temp5 = rs.getLong(5);
				final Long autreTiersId = (rs.wasNull() ? null : temp5);
				final String temp6 = rs.getString(6);
				final TypeAdresseTiers autreType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp6));
				final int temp9 = rs.getInt(9);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp9, false));
				final int temp10 = rs.getInt(10);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp10, false));
				final Date logCdate = rs.getTimestamp(11);
				final String logCuser = rs.getString(12);
				final Timestamp logMdate = rs.getTimestamp(13);
				final String logMuser = rs.getString(14);
				final String temp28 = rs.getString(28);
				final TypeAdresseTiers usageType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp28));
			
				AdresseAutreTiers o = new AdresseAutreTiers();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setAutreTiersId(autreTiersId);
				o.setType(autreType);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setUsage(usageType);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + adrType + ']');
			}
			
			final Pair<Long, AdresseTiers> pair = new Pair<Long, AdresseTiers>();
			pair.setFirst(tiersId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
