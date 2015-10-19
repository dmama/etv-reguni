package ch.vd.uniregctb.tiers;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.SessionImpl;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

public class TacheDAOImpl extends BaseDAOImpl<Tache, Long> implements TacheDAO {

	//private static final Logger LOGGER = LoggerFactory.getLogger(TacheDAOImpl.class);

	public TacheDAOImpl() {
		super(Tache.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tache> find(TacheCriteria criterion) {
		return find(criterion, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Tache> find(TacheCriteria criterion, boolean doNotAutoFlush) {

		final Map<String, Object> params = new HashMap<>();
		final String query = "select tache " + buildFromWhereClause(criterion, params) + " order by tache.id asc";

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		return find(query, params, mode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Tache> find(long noContribuable) {
		final String query = "select t from Tache t where t.contribuable.id=" + noContribuable + " order by t.id asc";
		return find(query, null);
	}

	/**
	 * Recherche d'un range de toutes les tâches correspondant au critère de sélection
	 *
	 * @param criterion
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Tache> find(final TacheCriteria criterion, final ParamPagination paramPagination) {

		final Session session = getCurrentSession();
		final Map<String, Object> paramsWhere = new HashMap<>();
		final String whereClause = buildFromWhereClause(criterion, paramsWhere);
		final QueryFragment fragment = new QueryFragment("select tache " + whereClause, paramsWhere);
		fragment.add(paramPagination.buildOrderClause("tache", null, true, null));

		final Query queryObject = fragment.createQuery(session);

		int firstResult = paramPagination.getSqlFirstResult();
		int maxResult = paramPagination.getSqlMaxResults();
		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);

		return queryObject.list();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int count(TacheCriteria criterion) {
		return count(criterion, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int count(long noContribuable) {
		final String query = "select count(*) from Tache t where t.contribuable.id=" + noContribuable;
		return DataAccessUtils.intResult(find(query, null));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int count(TacheCriteria criterion, boolean doNotAutoFlush) {
		final Map<String, Object> params = new HashMap<>();
		final String query = "select count(*) " + buildFromWhereClause(criterion, params);

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		return DataAccessUtils.intResult(find(query, params, mode));
	}

	/**
	 * Construit les clauses 'from' et 'where' de la requête de recherche des tâches.
	 */
	private String buildFromWhereClause(TacheCriteria criterion, Map<String, Object> params) {

		String clause = "";

		// Type de tache
		final TypeTache typeTache = criterion.getTypeTache();
		if (typeTache == null || criterion.isInvertTypeTache()) {
			clause = "from Tache tache where 1=1 ";
		}
		else {
			clause = "from " + typeTache.name() + " tache where 1=1 ";
		}
		if (criterion.isInvertTypeTache()) {
			clause += " and tache.class != " + typeTache.name() + ' ';
		}

		// Contribuable
		final Contribuable contribuable = criterion.getContribuable();
		if (contribuable != null) {
			clause += " and tache.contribuable = :ctb";
			params.put("ctb", contribuable);
		}

		// Numéro de contribuable
		if (criterion.getNumeroCTB() != null) {
			clause += " and tache.contribuable.id = :ctbId";
			params.put("ctbId", criterion.getNumeroCTB());
		}

		// Etat tache
		final TypeEtatTache etatTache = criterion.getEtatTache();
		if (etatTache != null) {
			clause += " and tache.etat = :etat";
			params.put("etat", etatTache);
		}

		// Année de la période d'imposition
		final Integer annee = criterion.getAnnee();
		if (annee != null) {
			final RegDate dateDebutPeriode = RegDate.get(annee, 1, 1);
			final RegDate dateFinPeriode = RegDate.get(annee, 12, 31);

			if (TypeTache.TacheEnvoiDeclarationImpotPP == typeTache) {
				clause += " and tache.dateDebut >= :dateDebut";
				clause += " and tache.dateFin <= :dateFin";
				params.put("dateDebut", dateDebutPeriode);
				params.put("dateFin", dateFinPeriode);
			}
			else if (TypeTache.TacheEnvoiDeclarationImpotPM == typeTache) {
				clause += " and tache.dateFin between :dateDebut and :dateFin";
				params.put("dateDebut", dateDebutPeriode);
				params.put("dateFin", dateFinPeriode);
			}
			else if (TypeTache.TacheAnnulationDeclarationImpot == typeTache) {
				clause += " and tache.declarationImpotOrdinaire.dateDebut >= :dateDebut";
				clause += " and tache.declarationImpotOrdinaire.dateFin <= :dateFin";
				params.put("dateDebut", dateDebutPeriode);
				params.put("dateFin", dateFinPeriode);
			}
		}

		// Office d'impôt
		final Integer oid = criterion.getOid();
		if (oid != null) {
			// [UNIREG-1850]
			clause += " and tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative = :oid";
			params.put("oid", oid);
		}

		// Office d'impot de l'utilisateur
		final Integer[] oidUser = criterion.getOidUser();
		if (oidUser != null && oidUser.length > 0) {
			clause += " and (tache.collectiviteAdministrativeAssignee is null or tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative in (:oidUser))";
			params.put("oidUser", Arrays.asList(oidUser));
		}

		final Date dateCreationDepuis = criterion.getDateCreationDepuis();
		if (dateCreationDepuis != null) {
			clause += " and tache.logCreationDate >= :cdateMin";
			params.put("cdateMin", dateCreationDepuis);
		}

		final Date dateCreationJusqua = criterion.getDateCreationJusqua();
		if (dateCreationJusqua != null) {
			clause += " and tache.logCreationDate <= :cdateMax";
			params.put("cdateMax", dateCreationJusqua);
		}

		final RegDate dateEcheanceJusqua = criterion.getDateEcheanceJusqua();
		if (dateEcheanceJusqua != null) {
			clause += " and tache.dateEcheance <= :dateEcheanceMax";
			params.put("dateEcheanceMax", dateEcheanceJusqua);
		}

		final boolean inclureTachesAnnulees = criterion.isInclureTachesAnnulees();
		if (!inclureTachesAnnulees) {
			clause += " and tache.annulationDate is null";
		}

		final DeclarationImpotOrdinaire declarationAnnulee = criterion.getDeclarationAnnulee();
		if (declarationAnnulee != null && typeTache == TypeTache.TacheAnnulationDeclarationImpot) {
			clause += " and tache.declarationImpotOrdinaire = :diAnnulee";
			params.put("diAnnulee", declarationAnnulee);
		}

		return clause;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean existsTacheEnInstanceOuEnCours(final long noCtb, final TypeTache type) {

		final Session session = getCurrentSession();

		// Recherche dans le cache de la session
		final SessionImpl s = (SessionImpl) session;
		final Map<EntityKey, Object> entities = s.getPersistenceContext().getEntitiesByKey();
		for (Object entity : entities.values()) {
			if (entity instanceof Tache) {
				final Tache t = (Tache) entity;
				if (t.getContribuable().getNumero().equals(noCtb) && type == t.getTypeTache() && (isTacheOuverte(t))) {
					return true;
				}
			}
		}

		// Recherche dans la base de données
		final String query = "from "
				+ type.name()
				+ " tache where tache.contribuable.id = :noCtb and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
		final List<Tache> list = find(query, buildNamedParameters(Pair.of("noCtb", noCtb)), FlushMode.MANUAL);
		return !list.isEmpty();
	}


	@Override
	@SuppressWarnings("unchecked")
	public boolean existsTacheEnvoiDIPPEnInstanceOuEnCours(final long noCtb, final RegDate dateDebut, final RegDate dateFin) {

		final Session session = getCurrentSession();

		// Recherche dans le cache de la session
		SessionImpl s = (SessionImpl) session;
		final Map<EntityKey, Object> entities = s.getPersistenceContext().getEntitiesByKey();
		for (Object entity : entities.values()) {
			if (entity instanceof Tache) {
				final Tache t = (Tache) entity;
				if (t.getContribuable().getNumero().equals(noCtb) && t instanceof TacheEnvoiDeclarationImpotPP && (isTacheOuverte(t))) {
					final TacheEnvoiDeclarationImpotPP envoi = (TacheEnvoiDeclarationImpotPP) t;
					if (dateDebut.equals(envoi.getDateDebut()) && dateFin.equals(envoi.getDateFin())) {
						return true;
					}
				}
			}
		}

		// Recherche dans la base de données
		final String query = "from TacheEnvoiDeclarationImpotPP tache where tache.contribuable.id = :noCtb and tache.dateDebut = :debut and tache.dateFin = :fin and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
		final List<Tache> list = find(query,
		                              buildNamedParameters(Pair.<String, Object>of("noCtb", noCtb),
		                                                   Pair.<String, Object>of("debut", dateDebut),
		                                                   Pair.<String, Object>of("fin", dateFin)),
		                              FlushMode.MANUAL);
		return !list.isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean existsTacheAnnulationEnInstanceOuEnCours(final long noCtb, final long noDi) {

		final Session session = getCurrentSession();

		// Recherche dans le cache de la session
		final SessionImpl s = (SessionImpl) session;
		final Map<EntityKey, Object> entities = s.getPersistenceContext().getEntitiesByKey();
		for (Object entity : entities.values()) {
			if (entity instanceof Tache) {
				final Tache t = (Tache) entity;
				if (t.getContribuable().getNumero().equals(noCtb) && t instanceof TacheAnnulationDeclarationImpot
						&& (isTacheOuverte(t))) {
					final TacheAnnulationDeclarationImpot annul = (TacheAnnulationDeclarationImpot) t;
					if (Long.valueOf(noDi).equals(annul.getDeclarationImpotOrdinaire().getId())) {
						return true;
					}
				}
			}
		}

		// Recherche dans la base de données

		final String query = "from TacheAnnulationDeclarationImpot tache where tache.contribuable.id = :ctb and "
				+ "tache.declarationImpotOrdinaire.id = :noDi and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
		final List<Tache> list = find(query,
		                              buildNamedParameters(Pair.of("ctb", noCtb), Pair.of("noDi", noDi)),
		                              FlushMode.MANUAL);
		return !list.isEmpty();
	}

	private static boolean isTacheOuverte(final Tache t) {
		return TypeEtatTache.EN_INSTANCE == t.getEtat() || TypeEtatTache.EN_COURS == t.getEtat();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Tache> List<T> listTaches(long noCtb, TypeTache type) {
		final String query = "from " + type.name() + " tache where tache.contribuable.id = :noCtb";
		return find(query, buildNamedParameters(Pair.of("noCtb", noCtb)) , FlushMode.MANUAL);
	}

	private static final String updateCollAdm =
			"update TACHE set CA_ID = (select ca.NUMERO from TIERS ca where ca.NUMERO_CA = :oid) where " +
					"ETAT = 'EN_INSTANCE' and " + // il ne faut pas modifier les tâches déjà traitées
					"TACHE_TYPE != 'CTRL_DOSSIER' and " + // [UNIREG-1024] les contrôles de dossiers doivent rester à l'ancien OID
					"CA_ID != (select aci.NUMERO from TIERS aci where aci.NUMERO_CA = 22) and " + // [UNIREG-2104] les tâches pour les décédés sont assignées à l'ACI et doivent le rester
					"CTB_ID = :ctbId and " +
					"ANNULATION_DATE is null"; // inutiles de modifier les tâches annulées pour rien

	@Override
	public void updateCollAdmAssignee(final Map<Long, Integer> tiersOidsMapping) {

		if (tiersOidsMapping == null || tiersOidsMapping.isEmpty()) {
			return;
		}

		// [UNIREG-1024] On met-à-jour les tâches encore ouvertes, à l'exception des tâches de contrôle de dossier
		final Session session = getCurrentSession();
		final FlushMode mode = session.getFlushMode();
		session.setFlushMode(FlushMode.MANUAL);
		try {
			// met-à-jour les tâches concernées
			final Query update = session.createSQLQuery(updateCollAdm);
			for (Map.Entry<Long, Integer> e : tiersOidsMapping.entrySet()) {
				//UNIREG-1585 l'oid est mis à jour sur les tâches que s'iln'est pas null
				if (e.getValue() != null) {
					update.setParameter("ctbId", e.getKey());
					update.setParameter("oid", e.getValue());
					update.executeUpdate();
				}
			}
		}
		finally {
			session.setFlushMode(mode);
		}
	}

	static final String queryTaches =
			"select " +
					"tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative, count(*) " +
					"from Tache tache " +
					"where tache.class != TacheNouveauDossier and tache.etat = 'EN_INSTANCE' and tache.dateEcheance <= :dateEcheance and tache.annulationDate is null " +
					"group by tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative";

	static final String queryDossiers =
			"select " +
					"tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative, count(*) " +
					"from TacheNouveauDossier tache " +
					"where tache.etat = 'EN_INSTANCE' and tache.dateEcheance <= :dateEcheance and tache.annulationDate is null " +
					"group by tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative";
	
	@Override
	public Map<Integer, TacheStats> getTacheStats() {
		
		final Map<Integer, TacheStats> stats = new HashMap<>();
		final Session session = getCurrentSession();

		// récupère les stats des tâches en instance
		{
			final Query query = session.createQuery(queryTaches);
			query.setParameter("dateEcheance", RegDate.get());

			final List list = query.list();
			for (Object o : list) {
				Object tuple[] = (Object[]) o;
				final Integer oid = (Integer) tuple[0];
				final Long count = (Long) tuple[1];

				TacheStats s = stats.get(oid);
				if (s == null) {
					s = new TacheStats();
					stats.put(oid, s);
				}

				s.tachesEnInstance = count.intValue();
			}
		}

		// récupère les stats des dossiers en instance
		{
			final Query query = session.createQuery(queryDossiers);
			query.setParameter("dateEcheance", RegDate.get());

			final List list = query.list();
			for (Object o : list) {
				Object tuple[] = (Object[]) o;
				final Integer oid = (Integer) tuple[0];
				final Long count = (Long) tuple[1];

				TacheStats s = stats.get(oid);
				if (s == null) {
					s = new TacheStats();
					stats.put(oid, s);
				}

				s.dossiersEnInstance = count.intValue();
			}
		}

		// pas de besoin de synchronisation parce que l'assignement est atomique en java
		return stats;
	}
}
