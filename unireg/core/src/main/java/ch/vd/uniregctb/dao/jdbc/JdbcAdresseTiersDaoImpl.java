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

import ch.vd.common.model.EnumTypeAdresse;
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
import ch.vd.uniregctb.type.TypeAdressePM;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class JdbcAdresseTiersDaoImpl implements JdbcAdresseTiersDao {

	private static final AdresseTiersMapper ROW_MAPPER = new AdresseTiersMapper();

	@SuppressWarnings({"unchecked"})
	public AdresseTiers get(long forId, JdbcTemplate template) {
		final Pair<Long, AdresseTiers> pair = (Pair<Long, AdresseTiers>) DataAccessUtils.uniqueResult(template.query(AdresseTiersMapper.selectById(), new Object[]{forId}, ROW_MAPPER));
		return pair.getSecond();
	}

	@SuppressWarnings({"unchecked"})
	public Set<AdresseTiers> getForTiers(long tiersId, JdbcTemplate template) {
		final List<Pair<Long, AdresseTiers>> list = template.query(AdresseTiersMapper.selectByTiersId(), new Object[]{tiersId}, ROW_MAPPER);
		final HashSet<AdresseTiers> set = new HashSet<AdresseTiers>(list.size());
		for (Pair<Long, AdresseTiers> pair : list) {
			set.add(pair.getSecond());
		}
		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<AdresseTiers>> getForTiers(Collection<Long> tiersId, final JdbcTemplate template) {
		
		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, AdresseTiers>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, AdresseTiers>>() {
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
				"COMPLEMENT, " + // 6
				"COMPLEMENT_LOCALITE, " + // 7
				"DATE_DEBUT, " + // 8
				"DATE_FIN, " + // 9
				"LOG_CDATE, " + // 10
				"LOG_CUSER, " + // 11
				"LOG_MDATE, " + // 12
				"LOG_MUSER, " + // 13
				"NUMERO_APPARTEMENT, " + // 14
				"NUMERO_CASE_POSTALE, " + // 15
				"NUMERO_MAISON, " + // 16
				"NUMERO_OFS_PAYS, " + // 17
				"NUMERO_ORDRE_POSTE, " + // 18
				"NUMERO_POSTAL_LOCALITE, " + // 19
				"NUMERO_RUE, " + // 20
				"PERMANENTE, " + // 21
				"RUE, " + // 22
				"STYPE, " + // 23
				"TEXTE_CASE_POSTALE, " + // 24
				"TIERS_ID, " + // 25
				"TYPE, " + // 26
				"USAGE_TYPE " + // 27
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

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			final String temp1 = rs.getString(1);
			final String adrType = (rs.wasNull() ? null : temp1);
			final Long temp2 = rs.getLong(2);
			final Long id = (rs.wasNull() ? null : temp2);
			final Timestamp temp3 = rs.getTimestamp(3);
			final Date annulationDate = (rs.wasNull() ? null : temp3);
			final String temp4 = rs.getString(4);
			final String annulationUser = (rs.wasNull() ? null : temp4);
			final Long temp5 = rs.getLong(5);
			final Long autreTiersId = (rs.wasNull() ? null : temp5);
			final String temp6 = rs.getString(6);
			final String complement = (rs.wasNull() ? null : temp6);
			final String temp7 = rs.getString(7);
			final String complementLocalite = (rs.wasNull() ? null : temp7);
			final Integer temp8 = rs.getInt(8);
			final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp8, false));
			final Integer temp9 = rs.getInt(9);
			final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp9, false));
			final Timestamp temp10 = rs.getTimestamp(10);
			final Date logCdate = (rs.wasNull() ? null : temp10);
			final String temp11 = rs.getString(11);
			final String logCuser = (rs.wasNull() ? null : temp11);
			final Timestamp temp12 = rs.getTimestamp(12);
			final Timestamp logMdate = (rs.wasNull() ? null : temp12);
			final String temp13 = rs.getString(13);
			final String logMuser = (rs.wasNull() ? null : temp13);
			final String temp14 = rs.getString(14);
			final String numeroAppartement = (rs.wasNull() ? null : temp14);
			final Integer temp15 = rs.getInt(15);
			final Integer numeroCasePostale = (rs.wasNull() ? null : temp15);
			final String temp16 = rs.getString(16);
			final String numeroMaison = (rs.wasNull() ? null : temp16);
			final Integer temp17 = rs.getInt(17);
			final Integer numeroOfsPays = (rs.wasNull() ? null : temp17);
			final Integer temp18 = rs.getInt(18);
			final Integer numeroOrdrePoste = (rs.wasNull() ? null : temp18);
			final String temp19 = rs.getString(19);
			final String numeroPostalLocalite = (rs.wasNull() ? null : temp19);
			final Integer temp20 = rs.getInt(20);
			final Integer numeroRue = (rs.wasNull() ? null : temp20);
			final Boolean temp21 = rs.getBoolean(21);
			final Boolean permanente = (rs.wasNull() ? null : temp21);
			final String temp22 = rs.getString(22);
			final String rue = (rs.wasNull() ? null : temp22);
			final String temp23 = rs.getString(23);
			final EnumTypeAdresse stype = (rs.wasNull() ? null : EnumTypeAdresse.getEnum(temp23));
			final String temp24 = rs.getString(24);
			final TexteCasePostale texteCasePostale = (rs.wasNull() ? null : Enum.valueOf(TexteCasePostale.class, temp24));
			final Long temp25 = rs.getLong(25);
			final Long tiersId = (rs.wasNull() ? null : temp25);
			final String temp26 = rs.getString(26);
			final TypeAdressePM type = (rs.wasNull() ? null : Enum.valueOf(TypeAdressePM.class, temp26));
			final String temp27 = rs.getString(27);
			final TypeAdresseTiers usageType = (rs.wasNull() ? null : Enum.valueOf(TypeAdresseTiers.class, temp27));
			
			final AdresseTiers res;
			
			if (adrType.equals("AdresseSuisse")) {
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
				o.setType(type);
				o.setUsage(usageType);
				res = o;
			}
			else if (adrType.equals("AdresseAutreTiers")) {
				AdresseAutreTiers o = new AdresseAutreTiers();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setAutreTiersId(autreTiersId);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setType(TypeAdresseTiers.valueOf(temp26)); // modifié à la main parce que la colonne TYPE est partagée entre AdresseAutreTiers et AdressePM pour des types différents...
				o.setUsage(usageType);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + adrType + "]");
			}
			
			final Pair<Long, AdresseTiers> pair = new Pair<Long, AdresseTiers>();
			pair.setFirst(tiersId);
			pair.setSecond(res);
			
			return pair;
		}
	}
}
