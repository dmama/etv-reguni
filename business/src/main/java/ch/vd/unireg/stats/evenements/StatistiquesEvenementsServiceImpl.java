package ch.vd.unireg.stats.evenements;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.externe.EtatEvenementExterne;
import ch.vd.unireg.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.reqdes.ErreurTraitement;
import ch.vd.unireg.reqdes.EtatTraitement;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class StatistiquesEvenementsServiceImpl implements StatistiquesEvenementsService {

	private HibernateTemplate hibernateTemplate;

	private static final String VISA_HUMAIN_TEMPLATE = "zai%";

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * @param debutActivite date à partir de laquelle on liste les nouvelles modifications
	 * @return statistiques sur les événements civils e-CH
	 */
	@Override
	public StatsEvenementsCivilsPersonnesResults getStatistiquesEvenementsCivilsPersonnes(RegDate debutActivite) {
		final Map<EtatEvenementCivil, Integer> etats = getEtatsEvenementsCivilsPersonnes(null);
		final Map<EtatEvenementCivil, Integer> etatsNouveaux = getEtatsEvenementsCivilsPersonnes(debutActivite);
		final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParType = getNombreEvenementsCivilsEnErreurParTypeEch(null);
		final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParTypeNouveaux = getNombreEvenementsCivilsEnErreurParTypeEch(debutActivite);
		final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> forcesParType = getNombreEvenementsForcesParTypeEch(null);
		final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> forcesRecemmentParType = getNombreEvenementsForcesParTypeEch(debutActivite);
		final List<StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo> toutesErreurs = getToutesErreursEvenementsCivilsPersonnes();
		final List<StatsEvenementsCivilsPersonnesResults.EvenementCivilTraiteManuellementInfo> manipulationsManuelles = getManipulationsManuellesEch(debutActivite);
		final List<StatsEvenementsCivilsPersonnesResults.QueueAttenteInfo> queuesAttente = getQueuesAttente();
		return new StatsEvenementsCivilsPersonnesResults(etats, etatsNouveaux, erreursParType, erreursParTypeNouveaux, toutesErreurs,
		                                                 manipulationsManuelles, forcesParType, forcesRecemmentParType, queuesAttente);
	}
	
	private Map<EtatEvenementCivil, Integer> getEtatsEvenementsCivilsPersonnes(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<>(1);
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
			sqlParameters = new HashMap<>(1);
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
				return Pair.of(type, action);
			}

			@Override
			public Integer buildValue(Object[] row) {
				return ((Number) row[2]).intValue();
			}
		});
	}

	/**
	 * @param debutActivite le seuil relatif à la date de modification
	 * @return la répartition des événements civils forcés depuis une date donnée (ou depuis toujours si aucune date n'est donnée)
	 */
	private Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> getNombreEvenementsForcesParTypeEch(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT TYPE, ACTION_EVT, COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE ETAT='FORCE' AND LOG_MDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY TYPE, ACTION_EVT";
			sqlParameters = new HashMap<>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT TYPE, ACTION_EVT, COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE ETAT='FORCE' GROUP BY TYPE, ACTION_EVT";
			sqlParameters = null;
		}
		return buildMapFromSql(sql, sqlParameters, new MapSelectCallback<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer>() {
			@Override
			public Pair<TypeEvenementCivilEch, ActionEvenementCivilEch> buildKey(Object[] row) {
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.valueOf((String) row[0]);
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.valueOf((String) row[1]);
				return Pair.of(type, action);
			}

			@Override
			public Integer buildValue(Object[] row) {
				return ((Number) row[2]).intValue();
			}
		});
	}
	
	private List<StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo> getToutesErreursEvenementsCivilsPersonnes() {

		final String sql = "SELECT R.ID, R.DATE_EVENEMENT, R.DATE_TRAITEMENT, R.ETAT, R.NO_INDIVIDU, R.TYPE, R.ACTION_EVT, E.MESSAGE, R.COMMENTAIRE_TRAITEMENT"
				+ " FROM EVENEMENT_CIVIL_ECH R JOIN EVENEMENT_CIVIL_ECH_ERREUR E ON E.EVT_CIVIL_ID = R.ID WHERE R.ETAT NOT IN ('TRAITE','REDONDANT') ORDER BY R.ID, R.DATE_TRAITEMENT";

		return executeSelect(sql, null, new SelectCallback<StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo>() {
			@Override
			public StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo onRow(Object[] row) {

				if (row.length != 9) {
					throw new IllegalArgumentException();
				}

				final long id = ((Number) row[0]).longValue();
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[1]).intValue(), false);
				final Date dateTraitement = (Date) row[2];
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[3]);
				final Long individu = row[4] != null ? ((Number) row[4]).longValue() : null;
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.valueOf((String) row[5]);
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.valueOf((String) row[6]);
				final String message = (String) row[7];
				final String commentaireStraitement = (String) row[8];
				return new StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo(id, type, action, dateEvenement, dateTraitement, etat, individu, commentaireStraitement, message);
			}
		});
	}

	private List<StatsEvenementsCivilsPersonnesResults.EvenementCivilTraiteManuellementInfo> getManipulationsManuellesEch(RegDate debutActivite) {

		final String sql = "SELECT ID, LOG_CDATE, LOG_MDATE, LOG_MUSER, DATE_EVENEMENT, ETAT, NO_INDIVIDU, TYPE, ACTION_EVT, COMMENTAIRE_TRAITEMENT FROM EVENEMENT_CIVIL_ECH"
				+ " WHERE LOG_MUSER LIKE '" + VISA_HUMAIN_TEMPLATE + "' AND LOG_MDATE > TO_DATE('" + debutActivite.index() + "', 'YYYYMMDD') ORDER BY ID";

		return executeSelect(sql, null, new SelectCallback<StatsEvenementsCivilsPersonnesResults.EvenementCivilTraiteManuellementInfo>() {
			@Override
			public StatsEvenementsCivilsPersonnesResults.EvenementCivilTraiteManuellementInfo onRow(Object[] row) {

				if (row.length != 10) {
					throw new IllegalArgumentException();
				}

				final long id = ((Number) row[0]).longValue();
				final Date dateReception = (Date) row[1];
				final Date dateModification = (Date) row[2];
				final String visaOperateur = (String) row[3];
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[4]).intValue(), false);
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[5]);
				final Long individu = row[6] != null ? ((Number) row[6]).longValue() : null;
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.valueOf((String) row[7]);
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.valueOf((String) row[8]);
				final String commentaireTraitement = (String) row[9];
				return new StatsEvenementsCivilsPersonnesResults.EvenementCivilTraiteManuellementInfo(id, type, action, dateEvenement, etat, individu, commentaireTraitement, visaOperateur, dateReception, dateModification);
			}
		});
	}
	
	private List<StatsEvenementsCivilsPersonnesResults.QueueAttenteInfo> getQueuesAttente() {
		final String sql = "SELECT NO_INDIVIDU, MIN(DATE_EVENEMENT), MAX(DATE_EVENEMENT), COUNT(*) FROM EVENEMENT_CIVIL_ECH WHERE ETAT IN ('EN_ERREUR', 'EN_ATTENTE') GROUP BY NO_INDIVIDU ORDER BY COUNT(*) DESC, NO_INDIVIDU ASC";
		
		return executeSelect(sql, null, new SelectCallback<StatsEvenementsCivilsPersonnesResults.QueueAttenteInfo>() {
			@Override
			public StatsEvenementsCivilsPersonnesResults.QueueAttenteInfo onRow(Object[] row) {
				if (row.length != 4) {
					throw new IllegalArgumentException();
				}
				if (row[0] != null) {
					final long noIndividu = ((Number) row[0]).longValue();
					final RegDate minDate = RegDate.fromIndex(((Number) row[1]).intValue(), false);
					final RegDate maxDate = RegDate.fromIndex(((Number) row[2]).intValue(), false);
					final int count = ((Number) row[3]).intValue();
					return new StatsEvenementsCivilsPersonnesResults.QueueAttenteInfo(noIndividu, minDate, maxDate, count);
				}
				else {
					return null;
				}
			}
		});
	}

	@Override
	public StatsEvenementsCivilsEntreprisesResults getStatistiquesEvenementsCivilsEntreprises(RegDate debutActivite) {
		final Map<EtatEvenementEntreprise, Integer> etats = getEtatsEvenementsCivilsEntreprise(null);
		final Map<EtatEvenementEntreprise, Integer> etatsRecents = getEtatsEvenementsCivilsEntreprise(debutActivite);
		final Map<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer> mutationsTraitees = getStatistiquesMutationsTraitees(null);
		final Map<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer> mutationsRecentesTraitees = getStatistiquesMutationsTraitees(debutActivite);
		final List<StatsEvenementsCivilsEntreprisesResults.DetailMutationTraitee> detailsMutationsTraiteesRecentes = getMutationsTraiteesDepuis(debutActivite);
		final List<StatsEvenementsCivilsEntreprisesResults.ErreurInfo> erreurs = getToutesErreursEvenementsCivilsEntreprises();
		final List<StatsEvenementsCivilsEntreprisesResults.EvenementEnSouffranceInfo> enSouffrance = getEvenementsCivilsEntrepriseEnSouffrance(15);
		return new StatsEvenementsCivilsEntreprisesResults(etats, etatsRecents, mutationsTraitees, mutationsRecentesTraitees, detailsMutationsTraiteesRecentes, erreurs, enSouffrance);
	}

	private Map<EtatEvenementEntreprise, Integer> getEtatsEvenementsCivilsEntreprise(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_ORGANISATION WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_ORGANISATION GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(EtatEvenementEntreprise.class, sql, sqlParameters);
	}

	@Nullable
	private Map<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer> getStatistiquesMutationsTraitees(@Nullable RegDate debutActivite) {
		final Map<String, Object> sqlParameters = new HashMap<>(2);
		final String limitationDate;
		if (debutActivite == null) {
			limitationDate = StringUtils.EMPTY;
		}
		else {
			limitationDate = " AND EVT.LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD')";
			sqlParameters.put("debutActivite", debutActivite.index());
		}

		final String prefixe = EvenementEntrepriseInterne.PREFIXE_MUTATION_TRAITEE;
		final int prefixeLength = prefixe.length();
		final String sql = String.format("SELECT EVT.ETAT, SUBSTR(MSG.MESSAGE, %d), COUNT(*) FROM EVENEMENT_ORGANISATION EVT JOIN EVENEMENT_ORGANISATION_ERREUR MSG ON MSG.EVT_ORGANISATION_ID=EVT.ID WHERE SUBSTR(MSG.MESSAGE,1,%d) = :prefixe%s GROUP BY EVT.ETAT, SUBSTR(MSG.MESSAGE, %d)",
		                                 prefixeLength + 1,
		                                 prefixeLength,
		                                 limitationDate,
		                                 prefixeLength + 1);
		sqlParameters.put("prefixe", prefixe);

		// extraction des lignes bruttes du resultSet (avec 'factorisation' des instances de String pour les descriptions)
		final Map<String, String> descriptions = new TreeMap<>();
		final List<Pair<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer>> data = this.executeSelect(sql, sqlParameters, new SelectCallback<Pair<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer>>() {
			@Override
			public Pair<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer> onRow(Object[] row) {

				if (row.length != 3) {
					throw new IllegalArgumentException();
				}

				final String description = (String) row[1];
				final String uniqueDescription;
				if (descriptions.containsKey(description)) {
					uniqueDescription = descriptions.get(description);
				}
				else {
					uniqueDescription = description;
					descriptions.put(description, description);
				}
				final EtatEvenementEntreprise etat = EtatEvenementEntreprise.valueOf((String) row[0]);
				final int nombre = ((Number) row[2]).intValue();
				return Pair.of(new StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey(uniqueDescription, etat), nombre);
			}
		});

		// constitution de la Map à partir des lignes bruttes
		if (data != null) {
			final Map<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer> map = new TreeMap<>();
			for (Pair<StatsEvenementsCivilsEntreprisesResults.MutationsTraiteesStatsKey, Integer> d : data) {
				map.put(d.getLeft(), d.getRight());
			}
			return map;
		}
		else {
			return null;
		}
	}

	private List<StatsEvenementsCivilsEntreprisesResults.DetailMutationTraitee> getMutationsTraiteesDepuis(RegDate dateDebutActivite) {
		final String prefixe = EvenementEntrepriseInterne.PREFIXE_MUTATION_TRAITEE;
		final int prefixeLength = prefixe.length();
		final String sql = String.format("SELECT EVT.NO_ORGANISATION, EVT.DATE_EVENEMENT, EVT.NO_EVENEMENT, EVT.ETAT, SUBSTR(MSG.MESSAGE, %d), MSG.LOG_CDATE" +
				                                 " FROM EVENEMENT_ORGANISATION EVT JOIN EVENEMENT_ORGANISATION_ERREUR MSG ON MSG.EVT_ORGANISATION_ID=EVT.ID" +
				                                 " WHERE SUBSTR(MSG.MESSAGE,1,%d) = :prefixe AND MSG.LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD')" +
				                                 " ORDER BY EVT.NO_ORGANISATION, EVT.DATE_TRAITEMENT, EVT.ID, MSG.LIST_INDEX",
		                                 prefixeLength + 1,
		                                 prefixeLength);
		final Map<String, Object> sqlParameters = new HashMap<>(2);
		sqlParameters.put("prefixe", prefixe);
		sqlParameters.put("debutActivite", dateDebutActivite.index());

		// extraction des lignes du resultSet (avec 'factorisation' des instances de String pour les descriptions)
		final Map<String, String> descriptions = new TreeMap<>();
		return executeSelect(sql, sqlParameters, new SelectCallback<StatsEvenementsCivilsEntreprisesResults.DetailMutationTraitee>() {
			@Override
			public StatsEvenementsCivilsEntreprisesResults.DetailMutationTraitee onRow(Object[] row) {

				if (row.length != 6) {
					throw new IllegalArgumentException();
				}

				final long noEntrepriseCivile = ((Number) row[0]).longValue();
				final RegDate dateEvenement = RegDateHelper.indexStringToDate(Integer.toString(((Number) row[1]).intValue()));
				final long noEvenement = ((Number) row[2]).longValue();
				final EtatEvenementEntreprise etat = EtatEvenementEntreprise.valueOf((String) row[3]);
				final String description = (String) row[4];
				final Timestamp date = (Timestamp) row[5];

				final String uniqueDescription;
				if (descriptions.containsKey(description)) {
					uniqueDescription = descriptions.get(description);
				}
				else {
					uniqueDescription = description;
					descriptions.put(description, description);
				}

				return new StatsEvenementsCivilsEntreprisesResults.DetailMutationTraitee(noEntrepriseCivile, etat, uniqueDescription, noEvenement, dateEvenement, date);
			}
		});
	}

	private List<StatsEvenementsCivilsEntreprisesResults.ErreurInfo> getToutesErreursEvenementsCivilsEntreprises() {
		final String sql = "SELECT R.ID, R.NO_EVENEMENT, R.DATE_EVENEMENT, R.DATE_TRAITEMENT, R.ETAT, R.NO_ORGANISATION, E.MESSAGE"
				+ " FROM EVENEMENT_ORGANISATION R JOIN EVENEMENT_ORGANISATION_ERREUR E ON E.EVT_ORGANISATION_ID = R.ID WHERE R.ETAT NOT IN ('TRAITE','REDONDANT')"
				+ " AND E.TYPE='ERROR' ORDER BY R.ID, R.DATE_TRAITEMENT";

		return executeSelect(sql, null, new SelectCallback<StatsEvenementsCivilsEntreprisesResults.ErreurInfo>() {
			@Override
			public StatsEvenementsCivilsEntreprisesResults.ErreurInfo onRow(Object[] row) {

				if (row.length != 7) {
					throw new IllegalArgumentException();
				}

				final long id = ((Number) row[0]).longValue();
				final long noEvenement = ((Number) row[1]).longValue();
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[2]).intValue(), false);
				final Date dateTraitement = (Date) row[3];
				final EtatEvenementEntreprise etat = EtatEvenementEntreprise.valueOf((String) row[4]);
				final long noEntrepriseCivile = ((Number) row[5]).longValue();
				final String message = (String) row[6];
				return new StatsEvenementsCivilsEntreprisesResults.ErreurInfo(id, noEntrepriseCivile, noEvenement, dateEvenement, dateTraitement, etat, message);
			}
		});
	}

	private List<StatsEvenementsCivilsEntreprisesResults.EvenementEnSouffranceInfo> getEvenementsCivilsEntrepriseEnSouffrance(int seuilEnJours) {
		final String sql = "SELECT ID, NO_EVENEMENT, NO_ORGANISATION, DATE_EVENEMENT, LOG_CDATE, ETAT FROM EVENEMENT_ORGANISATION"
				+ " WHERE ETAT IN ('A_TRAITER', 'A_VERIFIER', 'EN_ERREUR', 'EN_ATTENTE')"
				+ " AND LOG_CDATE < CURRENT_DATE - INTERVAL '" + seuilEnJours + "' DAY ORDER BY ID ASC";

		return executeSelect(sql, null, new SelectCallback<StatsEvenementsCivilsEntreprisesResults.EvenementEnSouffranceInfo>() {
			@Override
			public StatsEvenementsCivilsEntreprisesResults.EvenementEnSouffranceInfo onRow(Object[] row) {
				if (row.length != 6) {
					throw new IllegalArgumentException();
				}

				final long id = ((Number) row[0]).longValue();
				final long noEvevement = ((Number) row[1]).longValue();
				final long noEntrepriseCivile = ((Number) row[2]).longValue();
				final RegDate dateEvenement = RegDate.fromIndex(((Number) row[3]).intValue(), false);
				final Date dateReception = (Date) row[4];
				final EtatEvenementEntreprise etat = EtatEvenementEntreprise.valueOf((String) row[5]);
				return new StatsEvenementsCivilsEntreprisesResults.EvenementEnSouffranceInfo(id, noEntrepriseCivile, noEvevement, dateEvenement, dateReception, etat);
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

		return executeSelect(sql, null, new SelectCallback<StatsEvenementsExternesResults.EvenementExterneErreur>() {
			@Override
			public StatsEvenementsExternesResults.EvenementExterneErreur onRow(Object[] row) {

				if (row.length != 2) {
					throw new IllegalArgumentException();
				}

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
			sqlParameters = new HashMap<>(1);
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

		return executeSelect(sql, null, new SelectCallback<StatsEvenementsIdentificationContribuableResults.EvenementInfo>() {
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

	@Override
	public StatsEvenementsNotairesResults getStatistiquesEvenementsNotaires(RegDate debutActivite) {
		final Map<EtatTraitement, Integer> etats = getEtatsUnitesTraitementReqDes(null);
		final Map<EtatTraitement, Integer> etatsRecents = getEtatsUnitesTraitementReqDes(debutActivite);
		final List<StatsEvenementsNotairesResults.UniteTraitementEnErreurInfo> erreurs = getErreursUnitesTraitementReqDes();
		final List<StatsEvenementsNotairesResults.UniteTraitementForceesInfo> manipulationsManuelles = getManipulationsManuellesReqDes();
		return new StatsEvenementsNotairesResults(etats, etatsRecents, erreurs, manipulationsManuelles);
	}

	private Map<EtatTraitement, Integer> getEtatsUnitesTraitementReqDes(@Nullable RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM REQDES_UNITE_TRAITEMENT WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM REQDES_UNITE_TRAITEMENT GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(EtatTraitement.class, sql, sqlParameters);
	}

	private List<StatsEvenementsNotairesResults.UniteTraitementEnErreurInfo> getErreursUnitesTraitementReqDes() {
		final StringBuilder b = new StringBuilder();
		b.append("SELECT UT.ID, EVT.VISA_NOTAIRE, EVT.NUMERO_MINUTE, EVT.DATE_ACTE, UT.DATE_TRAITEMENT, UT.ETAT, PP1.NOM AS NOM_PP1, PP1.PRENOMS AS PRENOMS_PP1, PP2.NOM AS NOM_PP2, PP2.PRENOMS AS PRENOMS_PP2, E.TYPE, E.MESSAGE");
		b.append(" FROM REQDES_ERREUR E");
		b.append(" JOIN REQDES_UNITE_TRAITEMENT UT ON E.UNITE_TRAITEMENT_ID=UT.ID");
		b.append(" JOIN EVENEMENT_REQDES EVT ON UT.EVENEMENT_ID=EVT.ID");
		b.append(" JOIN REQDES_PARTIE_PRENANTE PP1 ON PP1.UNITE_TRAITEMENT_ID=UT.ID");
		b.append(" LEFT OUTER JOIN REQDES_PARTIE_PRENANTE PP2 ON PP2.UNITE_TRAITEMENT_ID=UT.ID AND PP2.ID>PP1.ID");
		b.append(" WHERE NOT EXISTS (SELECT 1 FROM REQDES_PARTIE_PRENANTE PP WHERE PP.UNITE_TRAITEMENT_ID=UT.ID AND PP.ID<PP1.ID)");
		b.append(" AND UT.ETAT='").append(EtatTraitement.EN_ERREUR).append("'");
		final String sql = b.toString();
		return executeSelect(sql, null, new SelectCallback<StatsEvenementsNotairesResults.UniteTraitementEnErreurInfo>() {
			@Override
			public StatsEvenementsNotairesResults.UniteTraitementEnErreurInfo onRow(Object[] row) {
				final long id = ((Number) row[0]).longValue();
				final String visaNotaire = (String) row[1];
				final String numeroMinute = (String) row[2];
				final RegDate dateActe = row[3] != null ? RegDate.fromIndex(((Number) row[3]).intValue(), false) : null;
				final Date dateTraitement = (Date) row[4];
				final EtatTraitement etat = row[5] != null ? EtatTraitement.valueOf((String) row[5]) : null;
				final NomPrenom pp1 = row[6] != null ? new NomPrenom((String) row[6], (String) row[7]) : null;
				final NomPrenom pp2 = row[8] != null ? new NomPrenom((String) row[8], (String) row[9]) : null;
				final ErreurTraitement.TypeErreur typeErreur = ErreurTraitement.TypeErreur.valueOf((String) row[10]);
				final String msgErreur = (String) row[11];
				return new StatsEvenementsNotairesResults.UniteTraitementEnErreurInfo(id, etat, visaNotaire, numeroMinute, dateActe, dateTraitement, pp1, pp2, typeErreur, msgErreur);
			}
		});
	}

	private List<StatsEvenementsNotairesResults.UniteTraitementForceesInfo> getManipulationsManuellesReqDes() {
		final StringBuilder b = new StringBuilder();
		b.append("SELECT UT.ID, EVT.VISA_NOTAIRE, EVT.NUMERO_MINUTE, EVT.DATE_ACTE, UT.LOG_CDATE, UT.LOG_MDATE, UT.LOG_MUSER, UT.ETAT, PP1.NOM AS NOM_PP1, PP1.PRENOMS AS PRENOMS_PP1, PP2.NOM AS NOM_PP2, PP2.PRENOMS AS PRENOMS_PP2");
		b.append(" FROM REQDES_UNITE_TRAITEMENT UT");
		b.append(" JOIN EVENEMENT_REQDES EVT ON UT.EVENEMENT_ID=EVT.ID");
		b.append(" JOIN REQDES_PARTIE_PRENANTE PP1 ON PP1.UNITE_TRAITEMENT_ID=UT.ID");
		b.append(" LEFT OUTER JOIN REQDES_PARTIE_PRENANTE PP2 ON PP2.UNITE_TRAITEMENT_ID=UT.ID AND PP2.ID>PP1.ID");
		b.append(" WHERE NOT EXISTS (SELECT 1 FROM REQDES_PARTIE_PRENANTE PP WHERE PP.UNITE_TRAITEMENT_ID=UT.ID AND PP.ID<PP1.ID)");
		b.append(" AND UT.ETAT='").append(EtatTraitement.FORCE).append("'");
		final String sql = b.toString();
		return executeSelect(sql, null, new SelectCallback<StatsEvenementsNotairesResults.UniteTraitementForceesInfo>() {
			@Override
			public StatsEvenementsNotairesResults.UniteTraitementForceesInfo onRow(Object[] row) {
				final long id = ((Number) row[0]).longValue();
				final String visaNotaire = (String) row[1];
				final String numeroMinute = (String) row[2];
				final RegDate dateActe = row[3] != null ? RegDate.fromIndex(((Number) row[3]).intValue(), false) : null;
				final Date dateReception = (Date) row[4];
				final Date dateModification = (Date) row[5];
				final String visaForcage = (String) row[6];
				final EtatTraitement etat = row[7] != null ? EtatTraitement.valueOf((String) row[7]) : null;
				final NomPrenom pp1 = row[8] != null ? new NomPrenom((String) row[8], (String) row[9]) : null;
				final NomPrenom pp2 = row[10] != null ? new NomPrenom((String) row[10], (String) row[11]) : null;
				return new StatsEvenementsNotairesResults.UniteTraitementForceesInfo(id, etat, visaNotaire, numeroMinute, dateActe, pp1, pp2, visaForcage, dateReception, dateModification);
			}
		});
	}

	private interface SelectCallback<T> {
		T onRow(Object[] row);
	}

	@SuppressWarnings({"unchecked"})
	private <T> List<T> executeSelect(final String sql, @Nullable final Map<String, Object> sqlParameters, final SelectCallback<T> callback) {
		return hibernateTemplate.executeWithNewSession(session -> {
			final Query query = session.createSQLQuery(sql);
			setParameters(query, sqlParameters);
			final List<Object[]> results = query.list();
			if (results != null && !results.isEmpty()) {
				final List<T> liste = new ArrayList<>(results.size());
				for (Object[] row : results) {
					final T element = callback.onRow(row);
					if (element != null) {
						liste.add(element);
					}
				}
				return liste.isEmpty() ? null : liste;
			}
			else {
				return null;
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	private <T extends Enum<T>> Map<T, Integer> getNombreParModalite(final Class<T> enumClass, final String sql, @Nullable final Map<String, Object> sqlParameters) {
		return hibernateTemplate.executeWithNewSession(session -> {
			final Query query = session.createSQLQuery(sql);
			setParameters(query, sqlParameters);
			final List<Object[]> result = query.list();
			if (result != null && !result.isEmpty()) {
				final Map<T, Integer> map = new HashMap<>(result.size());
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
		});
	}
	
	private interface MapSelectCallback<K, V> {
		K buildKey(Object[] row);
		V buildValue(Object[] row);
	}

	private <K, V> Map<K, V> buildMapFromSql(final String sql, @Nullable final Map<String, Object> sqlParameters, final MapSelectCallback<K, V> callback) {
		return hibernateTemplate.executeWithNewSession(session -> {
			final Query query = session.createSQLQuery(sql);
			setParameters(query, sqlParameters);
			final List<Object[]> results = query.list();
			if (results != null && !results.isEmpty()) {
				final Map<K, V> map = new HashMap<>(results.size());
				for (Object[] row : results) {
					final K key = callback.buildKey(row);
					final V value = callback.buildValue(row);
					map.put(key, value);
				}
				return map;
			}
			return null;
		});
	}

	private static void setParameters(Query query, @Nullable final Map<String, Object> sqlParameters) {
		if (sqlParameters != null && !sqlParameters.isEmpty()) {
			for (Map.Entry<String, Object> entry : sqlParameters.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}
	}
}
