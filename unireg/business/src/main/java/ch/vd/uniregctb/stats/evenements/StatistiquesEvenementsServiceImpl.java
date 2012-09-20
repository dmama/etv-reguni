package ch.vd.uniregctb.stats.evenements;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class StatistiquesEvenementsServiceImpl implements StatistiquesEvenementsService {

	private HibernateTemplate hibernateTemplate;

	private static final String VISA_HUMAIN_TEMPLATE = "zai%";

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Renvoie les statistiques sur les événements civils issus de RegPP (= les vieux)
	 * @param debutActivite date à partir de laquelle on liste les nouvelles modifications
	 */
	@Override
	public StatsEvenementsCivilsRegPPResults getStatistiquesEvenementsCivilsRegPP(RegDate debutActivite) {
		final Map<EtatEvenementCivil, Integer> etats = getEtatsEvenementsCivilsRegPP(null);
		final Map<TypeEvenementCivil, Integer> erreursParType = getNombreEvenementsCivilsEnErreurParTypeRegPP(null);
		final List<StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo> toutesErreurs = getToutesErreursEvenementsCivilsRegPP();
		final List<StatsEvenementsCivilsRegPPResults.EvenementCivilTraiteManuellementInfo> manipulationsManuelles = getManipulationsManuellesRegPP(debutActivite);
		return new StatsEvenementsCivilsRegPPResults(etats, erreursParType, toutesErreurs, manipulationsManuelles);
	}

	private Map<EtatEvenementCivil, Integer> getEtatsEvenementsCivilsRegPP(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(EtatEvenementCivil.class, sql, sqlParameters);
	}

	private Map<TypeEvenementCivil, Integer> getNombreEvenementsCivilsEnErreurParTypeRegPP(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT TYPE, COUNT(*) FROM EVENEMENT_CIVIL WHERE ETAT='EN_ERREUR' AND LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY TYPE";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT TYPE, COUNT(*) FROM EVENEMENT_CIVIL WHERE ETAT='EN_ERREUR' GROUP BY TYPE";
			sqlParameters = null;
		}
		return getNombreParModalite(TypeEvenementCivil.class, sql, sqlParameters);
	}

	private List<StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo> getToutesErreursEvenementsCivilsRegPP() {

		final String sql = "SELECT R.ID, R.DATE_EVENEMENT, R.DATE_TRAITEMENT, R.ETAT, R.NO_INDIVIDU_PRINCIPAL, R.NO_INDIVIDU_CONJOINT, R.TYPE, R.NUMERO_OFS_ANNONCE, E.MESSAGE"
				+ " FROM EVENEMENT_CIVIL R JOIN EVENEMENT_CIVIL_ERREUR E ON E.EVT_CIVIL_ID = R.ID WHERE R.ETAT != 'TRAITE' ORDER BY R.ID, R.DATE_TRAITEMENT";

		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo>() {
			@Override
			public StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo onRow(Object[] row) {

				Assert.isEqual(9, row.length);

				final long id = ((Number) row[0]).longValue();
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[1]).intValue(), false);
				final Date dateTraitement = (Date) row[2];
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[3]);
				final Long individuPrincipal = ((Number) row[4]).longValue();
				final Long individuConjoint = row[5] != null ? ((Number) row[5]).longValue() : null;
				final TypeEvenementCivil type = TypeEvenementCivil.valueOf((String) row[6]);
				final Integer ofsAnnonce = row[7] != null ? ((Number) row[7]).intValue() : null;
				final String message = (String) row[8];
				return new StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo(id, type, dateEvenement, dateTraitement, etat, individuPrincipal, individuConjoint, ofsAnnonce, message);
			}
		});
	}

	private List<StatsEvenementsCivilsRegPPResults.EvenementCivilTraiteManuellementInfo> getManipulationsManuellesRegPP(RegDate debutActivite) {

		final String sql = "SELECT ID, LOG_CDATE, LOG_MDATE, LOG_MUSER, DATE_EVENEMENT, ETAT, NO_INDIVIDU_PRINCIPAL, NO_INDIVIDU_CONJOINT, NUMERO_OFS_ANNONCE, TYPE FROM EVENEMENT_CIVIL"
				+ " WHERE LOG_MUSER LIKE '" + VISA_HUMAIN_TEMPLATE + "' AND LOG_MDATE > TO_DATE('" + debutActivite.index() + "', 'YYYYMMDD') ORDER BY ID";

		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsRegPPResults.EvenementCivilTraiteManuellementInfo>() {
			@Override
			public StatsEvenementsCivilsRegPPResults.EvenementCivilTraiteManuellementInfo onRow(Object[] row) {

				Assert.isEqual(10, row.length);

				final long id = ((Number) row[0]).longValue();
				final Date dateReception = (Date) row[1];
				final Date dateModification = (Date) row[2];
				final String visaOperateur = (String) row[3];
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[4]).intValue(), false);
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[5]);
				final Long individuPrincipal = ((Number) row[6]).longValue();
				final Long individuConjoint = row[7] != null ? ((Number) row[7]).longValue() : null;
				final Integer ofsAnnonce = row[8] != null ? ((Number) row[8]).intValue() : null;
				final TypeEvenementCivil type = TypeEvenementCivil.valueOf((String) row[9]);
				return new StatsEvenementsCivilsRegPPResults.EvenementCivilTraiteManuellementInfo(id, type, dateEvenement, etat, individuPrincipal, individuConjoint, ofsAnnonce, visaOperateur, dateReception, dateModification);
			}
		});
	}

	/**
	 * @param debutActivite date à partir de laquelle on liste les nouvelles modifications
	 * @return statistiques sur les événements civils e-CH
	 */
	@Override
	public StatsEvenementsCivilsEchResults getStatistiquesEvenementsCivilsEch(RegDate debutActivite) {
		final Map<EtatEvenementCivil, Integer> etats = getEtatsEvenementsCivilsEch(null);
		final Map<EtatEvenementCivil, Integer> etatsNouveaux = getEtatsEvenementsCivilsEch(debutActivite);
		final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParType = getNombreEvenementsCivilsEnErreurParTypeEch(null);
		final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParTypeNouveaux = getNombreEvenementsCivilsEnErreurParTypeEch(debutActivite);
		final List<StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo> toutesErreurs = getToutesErreursEvenementsCivilsEch();
		final List<StatsEvenementsCivilsEchResults.EvenementCivilTraiteManuellementInfo> manipulationsManuelles = getManipulationsManuellesEch(debutActivite);
		final List<StatsEvenementsCivilsEchResults.QueueAttenteInfo> queuesAttente = getQueuesAttente();
		return new StatsEvenementsCivilsEchResults(etats, etatsNouveaux, erreursParType, erreursParTypeNouveaux, toutesErreurs, manipulationsManuelles, queuesAttente);
	}
	
	private Map<EtatEvenementCivil, Integer> getEtatsEvenementsCivilsEch(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL_ECH GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(EtatEvenementCivil.class, sql, sqlParameters);
	}
	
	private Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> getNombreEvenementsCivilsEnErreurParTypeEch(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT TYPE, ACTION_EVT, COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE ETAT='EN_ERREUR' AND LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY TYPE, ACTION_EVT";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT TYPE, ACTION_EVT, COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE ETAT='EN_ERREUR' GROUP BY TYPE, ACTION_EVT";
			sqlParameters = null;
		}
		return buildMapFromSql(sql, sqlParameters, new MapSelectCallback<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer>() {
			@Override
			public Pair<TypeEvenementCivilEch, ActionEvenementCivilEch> buildKey(Object[] row) {
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.valueOf((String) row[0]);
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.valueOf((String) row[1]);
				return new Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>(type, action);
			}

			@Override
			public Integer buildValue(Object[] row) {
				return ((Number) row[2]).intValue();
			}
		});
	}
	
	private List<StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo> getToutesErreursEvenementsCivilsEch() {

		final String sql = "SELECT R.ID, R.DATE_EVENEMENT, R.DATE_TRAITEMENT, R.ETAT, R.NO_INDIVIDU, R.TYPE, R.ACTION_EVT, E.MESSAGE, R.COMMENTAIRE_TRAITEMENT"
				+ " FROM EVENEMENT_CIVIL_ECH R JOIN EVENEMENT_CIVIL_ECH_ERREUR E ON E.EVT_CIVIL_ID = R.ID WHERE R.ETAT NOT IN ('TRAITE','REDONDANT') ORDER BY R.ID, R.DATE_TRAITEMENT";

		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo>() {
			@Override
			public StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo onRow(Object[] row) {

				Assert.isEqual(9, row.length);

				final long id = ((Number) row[0]).longValue();
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[1]).intValue(), false);
				final Date dateTraitement = (Date) row[2];
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[3]);
				final Long individu = ((Number) row[4]).longValue();
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.valueOf((String) row[5]);
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.valueOf((String) row[6]);
				final String message = (String) row[7];
				final String commentaireStraitement = (String) row[8];
				return new StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo(id, type, action, dateEvenement, dateTraitement, etat, individu, commentaireStraitement, message);
			}
		});
	}

	private List<StatsEvenementsCivilsEchResults.EvenementCivilTraiteManuellementInfo> getManipulationsManuellesEch(RegDate debutActivite) {

		final String sql = "SELECT ID, LOG_CDATE, LOG_MDATE, LOG_MUSER, DATE_EVENEMENT, ETAT, NO_INDIVIDU, TYPE, ACTION_EVT, COMMENTAIRE_TRAITEMENT FROM EVENEMENT_CIVIL_ECH"
				+ " WHERE LOG_MUSER LIKE '" + VISA_HUMAIN_TEMPLATE + "' AND LOG_MDATE > TO_DATE('" + debutActivite.index() + "', 'YYYYMMDD') ORDER BY ID";

		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsEchResults.EvenementCivilTraiteManuellementInfo>() {
			@Override
			public StatsEvenementsCivilsEchResults.EvenementCivilTraiteManuellementInfo onRow(Object[] row) {

				Assert.isEqual(10, row.length);

				final long id = ((Number) row[0]).longValue();
				final Date dateReception = (Date) row[1];
				final Date dateModification = (Date) row[2];
				final String visaOperateur = (String) row[3];
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[4]).intValue(), false);
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[5]);
				final Long individu = ((Number) row[6]).longValue();
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.valueOf((String) row[7]);
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.valueOf((String) row[8]);
				final String commentaireTraitement = (String) row[9];
				return new StatsEvenementsCivilsEchResults.EvenementCivilTraiteManuellementInfo(id, type, action, dateEvenement, etat, individu, commentaireTraitement, visaOperateur, dateReception, dateModification);
			}
		});
	}
	
	private List<StatsEvenementsCivilsEchResults.QueueAttenteInfo> getQueuesAttente() {
		final String sql = "SELECT NO_INDIVIDU, MIN(DATE_EVENEMENT), MAX(DATE_EVENEMENT), COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE ETAT IN ('EN_ERREUR', 'EN_ATTENTE') GROUP BY NO_INDIVIDU ORDER BY COUNT(*) DESC, NO_INDIVIDU ASC";
		
		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsEchResults.QueueAttenteInfo>() {
			@Override
			public StatsEvenementsCivilsEchResults.QueueAttenteInfo onRow(Object[] row) {
				Assert.isEqual(4, row.length);
				final long noIndividu = ((Number) row[0]).longValue();
				final RegDate minDate = RegDate.fromIndex(((Number) row[1]).intValue(), false);
				final RegDate maxDate = RegDate.fromIndex(((Number) row[2]).intValue(), false);
				final int count = ((Number) row[3]).intValue();
				return new StatsEvenementsCivilsEchResults.QueueAttenteInfo(noIndividu, minDate, maxDate, count);
			}
		});
	}

	/**
	 * Renvoie les statistiques sur les événements externes
	 */
	@Override
	public StatsEvenementsExternesResults getStatistiquesEvenementsExternes() {
		final Map<EtatEvenementExterne, Integer> etats = getEtatsEvenementsExternes();
		final List<StatsEvenementsExternesResults.EvenementExterneErreur> erreurs = getErreursEvenementsExternes();
		return new StatsEvenementsExternesResults(etats, erreurs);
	}

	private List<StatsEvenementsExternesResults.EvenementExterneErreur> getErreursEvenementsExternes() {

		final String sql = "SELECT ID, MESSAGE FROM EVENEMENT_EXTERNE WHERE ETAT='EN_ERREUR'";

		return executeSelect(sql, new SelectCallback<StatsEvenementsExternesResults.EvenementExterneErreur>() {
			@Override
			public StatsEvenementsExternesResults.EvenementExterneErreur onRow(Object[] row) {

				Assert.isEqual(2, row.length);

				final long id = ((Number) row[0]).longValue();
				final String message = (String) row[1];
				return new StatsEvenementsExternesResults.EvenementExterneErreur(id, message);
			}
		});
	}

	private Map<EtatEvenementExterne, Integer> getEtatsEvenementsExternes() {
		final String sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_EXTERNE GROUP BY ETAT";
		return getNombreParModalite(EtatEvenementExterne.class, sql, null);
	}

	@Override
	public StatsEvenementsIdentificationContribuableResults getStatistiquesEvenementsIdentificationContribuable(RegDate debutActivite) {
		final Map<IdentificationContribuable.Etat, Integer> etats = getEtatsEvenementsIdentificationContribuable(null);
		final Map<IdentificationContribuable.Etat, Integer> etatsNouveaux = getEtatsEvenementsIdentificationContribuable(debutActivite);
		final List<StatsEvenementsIdentificationContribuableResults.EvenementInfo> aTraiter = getEvenementsIdentificationContribuableATraiter();
		return new StatsEvenementsIdentificationContribuableResults(etats, etatsNouveaux, aTraiter);
	}

	private Map<IdentificationContribuable.Etat, Integer> getEtatsEvenementsIdentificationContribuable(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_IDENTIFICATION_CTB WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_IDENTIFICATION_CTB GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(IdentificationContribuable.Etat.class, sql, sqlParameters);
	}

	private List<StatsEvenementsIdentificationContribuableResults.EvenementInfo> getEvenementsIdentificationContribuableATraiter() {
		/*
			-- Liste des messages à traiter manuellement
			SELECT DATE_DEMANDE, EMETTEUR_ID, ETAT, MESSAGE_ID, PERIODE_FISCALE, TYPE_MESSAGE, BUSINESS_ID, NB_CTB_TROUVES, NAVS11, NAVS13, ADR_CH_COMPL, ADR_CODE_PAYS, ADR_LIEU,
				   ADR_LIGNE_1, ADR_LIGNE_2, ADR_LOCALITE, ADR_NO_APPART, ADR_ORDRE_POSTE, ADR_NO_POLICE, ADR_NPA_ETRANGER, ADR_NPA_SUISSE, ADR_NO_CP, ADR_RUE, ADR_TEXT_CP,
				   ADR_TYPE, DATE_NAISSANCE, NOM, PRENOMS, SEXE
			FROM EVENEMENT_IDENTIFICATION_CTB
			WHERE ETAT IN (... états encore à traiter ...)
			ORDER BY DATE_DEMANDE;
		 */
		final StringBuilder b = new StringBuilder();
		b.append("SELECT DATE_DEMANDE, EMETTEUR_ID, ETAT, MESSAGE_ID, PERIODE_FISCALE, TYPE_MESSAGE, BUSINESS_ID, NB_CTB_TROUVES, NAVS11, NAVS13, ADR_CH_COMPL, ADR_CODE_PAYS, ADR_LIEU,");
		b.append(" ADR_LIGNE_1, ADR_LIGNE_2, ADR_LOCALITE, ADR_NO_APPART, ADR_ORDRE_POSTE, ADR_NO_POLICE, ADR_NPA_ETRANGER, ADR_NPA_SUISSE, ADR_NO_CP, ADR_RUE, ADR_TEXT_CP,");
		b.append(" ADR_TYPE, DATE_NAISSANCE, NOM, PRENOMS, SEXE");
		b.append(" FROM EVENEMENT_IDENTIFICATION_CTB WHERE ETAT IN (");
		boolean first = true;
		for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
			if (etat.isEncoreATraiter()) {
				if (!first) {
					b.append(", ");
				}
				b.append('\'').append(etat.name()).append('\'');
				first = false;
			}
		}
		b.append(") ORDER BY DATE_DEMANDE");
		final String sql = b.toString();

		return executeSelect(sql, new SelectCallback<StatsEvenementsIdentificationContribuableResults.EvenementInfo>() {
			@Override
			public StatsEvenementsIdentificationContribuableResults.EvenementInfo onRow(Object[] row) {

				final Date dateDemande = (Date) row[0];
				final String emetteurId = (String) row[1];
				final IdentificationContribuable.Etat etat = IdentificationContribuable.Etat.valueOf((String) row[2]);
				final String messageId = (String) row[3];
				final Integer pf = row[4] != null ? ((Number) row[4]).intValue() : null;
				final String typeMessage = (String) row[5];
				final String businessId = (String) row[6];
				final Integer nbCtbTrouves = row[7] != null ? ((Number) row[7]).intValue() : null;
				final String navs11 = (String) row[8];
				final String navs13 = (String) row[9];
				final String adresseChiffreComplementaire = (String) row[10];
				final String adresseCodePays = (String) row[11];
				final String adresseLieu = (String) row[12];
				final String adresseLigne1 = (String) row[13];
				final String adresseLigne2 = (String) row[14];
				final String adresseLocalite = (String) row[15];
				final String adresseNumeroAppartement = (String) row[16];
				final Integer adresseNumeroOrdrePoste = row[17] != null ? ((Number) row[17]).intValue() : null;
				final String adresseNumeroPolice = (String) row[18];
				final String adresseNpaEtranger = (String) row[19];
				final Integer adresseNpaSuisse = row[20] != null ? ((Number ) row[20]).intValue() : null;
				final Integer adresseNumeroCasePostale = row[21] != null ? ((Number) row[21]).intValue() : null;
				final String adresseRue = (String) row[22];
				final String adresseTexteCasePostale = (String) row[23];
				final CriteresAdresse.TypeAdresse adresseType = row[24] != null ? CriteresAdresse.TypeAdresse.valueOf((String) row[24]) : null;
				final RegDate dateNaissance = row[25] != null ? RegDate.fromIndex(((Number) row[25]).intValue(), true) : null;
				final String nom = (String) row[26];
				final String prenoms = (String) row[27];
				final Sexe sexe = row[28] != null ? Sexe.valueOf((String) row[28]) : null;

				return new StatsEvenementsIdentificationContribuableResults.EvenementInfo(dateDemande, emetteurId, etat, messageId, pf, typeMessage, businessId, nbCtbTrouves, navs11, navs13,
						adresseChiffreComplementaire, adresseCodePays, adresseLieu, adresseLigne1, adresseLigne2, adresseLocalite, adresseNumeroAppartement, adresseNumeroOrdrePoste, adresseNumeroPolice, adresseNpaEtranger,
						adresseNpaSuisse, adresseNumeroCasePostale, adresseRue, adresseTexteCasePostale, adresseType, dateNaissance, nom, prenoms, sexe);
			}
		});
	}

	private static interface SelectCallback<T> {
		T onRow(Object[] row);
	}

	@SuppressWarnings({"unchecked"})
	private <T> List<T> executeSelect(final String sql, final SelectCallback<T> callback) {
		return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<T>>() {

			@Override
			public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				final List<Object[]> results = query.list();
				if (results != null && !results.isEmpty()) {
					final List<T> liste = new ArrayList<T>(results.size());
					for (Object[] row : results) {
						final T element = callback.onRow(row);
						if (element != null) {
							liste.add(element);
						}
					}
					return liste;
				}
				else {
					return null;
				}
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	private <T extends Enum<T>> Map<T, Integer> getNombreParModalite(final Class<T> enumClass, final String sql, @Nullable final Map<String, Object> sqlParameters) {
		return hibernateTemplate.executeWithNewSession(new HibernateCallback<Map<T, Integer>>() {
			@Override
			public Map<T, Integer> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				if (sqlParameters != null && !sqlParameters.isEmpty()) {
					for (Map.Entry<String, Object> entry : sqlParameters.entrySet()) {
						query.setParameter(entry.getKey(), entry.getValue());
					}
				}
				final List<Object[]> result = query.list();
				if (result != null && !result.isEmpty()) {
					final Map<T, Integer> map = new HashMap<T, Integer>(result.size());
					for (Object[] row : result) {
						final T modalite = Enum.valueOf(enumClass, (String) row[0]);
						final Number nombre = (Number) row[1];
						if (nombre != null) {
							map.put(modalite, nombre.intValue());
						}
					}
					return map;
				}
				else {
					return null;
				}
			}
		});
	}
	
	private static interface MapSelectCallback<K, V> {
		K buildKey(Object[] row);
		V buildValue(Object[] row);
	}

	@SuppressWarnings({"unchecked"})
	private <K, V> Map<K, V> buildMapFromSql(final String sql, @Nullable final Map<String, Object> sqlParameters, final MapSelectCallback<K, V> callback) {
		return hibernateTemplate.executeWithNewSession(new HibernateCallback<Map<K, V>>() {
			@Override
			public Map<K, V> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				if (sqlParameters != null && !sqlParameters.isEmpty()) {
					for (Map.Entry<String, Object> entry : sqlParameters.entrySet()) {
						query.setParameter(entry.getKey(), entry.getValue());
					}
				}
				final List<Object[]> results = query.list();
				if (results != null && !results.isEmpty()) {
					final Map<K, V> map = new HashMap<K, V>(results.size());
					for (Object[] row : results) {
						final K key = callback.buildKey(row);
						final V value = callback.buildValue(row);
						map.put(key, value);
					}
					return map;
				}
				return null;
			}
		});
	}
}
