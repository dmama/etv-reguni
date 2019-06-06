package ch.vd.unireg.tiers;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.SessionImpl;
import org.hibernate.query.Query;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

public class TacheDAOImpl extends BaseDAOImpl<Tache, Long> implements TacheDAO {

	public TacheDAOImpl() {
		super(Tache.class);
	}

	@Override
	public List<Tache> find(TacheCriteria criterion) {
		return find(criterion, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Tache> find(TacheCriteria criterion, boolean doNotAutoFlush) {

		final Map<String, Object> params = new HashMap<>();
		final String query = "select tache " + buildFromWhereClause(criterion, params) + " order by tache.id asc";

		final FlushModeType mode = (doNotAutoFlush ? FlushModeType.COMMIT : null);
		return find(query, params, mode);
	}

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

	@Override
	public int count(TacheCriteria criterion) {
		return count(criterion, false);
	}

	@Override
	public int count(long noContribuable) {
		final String query = "select count(*) from Tache t where t.contribuable.id=" + noContribuable;
		return DataAccessUtils.intResult(find(query, null));
	}

	@Override
	public int count(TacheCriteria criterion, boolean doNotAutoFlush) {
		final Map<String, Object> params = new HashMap<>();
		final String query = "select count(*) " + buildFromWhereClause(criterion, params);

		final FlushModeType mode = (doNotAutoFlush ? FlushModeType.COMMIT : null);
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
			clause += " and type(tache) != " + typeTache.name() + ' ';
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
		if (annee != null && !criterion.isInvertTypeTache()) {
			final RegDate dateDebutPeriode = RegDate.get(annee, 1, 1);
			final RegDate dateFinPeriode = RegDate.get(annee, 12, 31);

			if (typeTache == TypeTache.TacheEnvoiDeclarationImpotPP || typeTache == TypeTache.TacheEnvoiDeclarationImpotPM || typeTache == TypeTache.TacheEnvoiQuestionnaireSNC) {
				clause += " and tache.dateFin between :dateDebut and :dateFin";
				params.put("dateDebut", dateDebutPeriode);
				params.put("dateFin", dateFinPeriode);
			}
			else if (typeTache == TypeTache.TacheAnnulationDeclarationImpot || typeTache == TypeTache.TacheAnnulationQuestionnaireSNC) {
				clause += " and tache.declaration.dateFin between :dateDebut and :dateFin";
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

		final Declaration declarationAnnulee = criterion.getDeclarationAnnulee();
		if (declarationAnnulee != null && (typeTache == TypeTache.TacheAnnulationDeclarationImpot || typeTache == TypeTache.TacheAnnulationQuestionnaireSNC) && !criterion.isInvertTypeTache()) {
			clause += " and tache.declaration = :diAnnulee";
			params.put("diAnnulee", declarationAnnulee);
		}

		if (StringUtils.isNotBlank(criterion.getCommentaire()) && typeTache == TypeTache.TacheControleDossier && !criterion.isInvertTypeTache()) {
			clause += " and tache.commentaire = :commentaire";
			params.put("commentaire", criterion.getCommentaire());
		}

		return clause;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean existsTacheControleDossierEnInstanceOuEnCours(final long noCtb, final String commentaire) {

		final Session session = getCurrentSession();

		// Recherche dans le cache de la session
		final SessionImpl s = (SessionImpl) session;
		final Map<EntityKey, Object> entities = s.getPersistenceContext().getEntitiesByKey();
		for (Object entity : entities.values()) {
			if (entity instanceof Tache) {
				final Tache t = (Tache) entity;
				if (t.getContribuable().getNumero().equals(noCtb) && isTacheOuverte(t) && (commentaire == null || commentaire.equals(t.getCommentaire()))) {
					return true;
				}
			}
		}

		// Recherche dans la base de données
		final String commentSql;
		final List<Pair<String, Object>> parametres = new ArrayList<>(2);
		if (commentaire == null) {
			commentSql = StringUtils.EMPTY;
		}
		else {
			commentSql = " and tache.commentaire=:commentaire";
			parametres.add(Pair.of("commentaire", commentaire));
		}
		final String query = String.format("from Tache tache where tache.contribuable.id = :noCtb and tache.annulationDate is null and tache.etat in ('EN_INSTANCE', 'EN_COURS')%s", commentSql);
		parametres.add(Pair.of("noCtb", noCtb));
		final List<Tache> list = find(query, buildNamedParameters(parametres), FlushModeType.COMMIT);
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
		                              buildNamedParameters(Pair.of("noCtb", noCtb),
		                                                   Pair.of("debut", dateDebut),
		                                                   Pair.of("fin", dateFin)),
		                              FlushModeType.COMMIT);
		return !list.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<TypeTache, List<String>> getCommentairesDistincts() {
		final String hql = "select distinct type(t), t.commentaire from Tache t where t.commentaire is not null";
		final Query query = getCurrentSession().createQuery(hql);

		final Map<TypeTache, List<String>> map = new EnumMap<>(TypeTache.class);
		final List<Object[]> commentaires = (List<Object[]>) query.list();
		for (Object[] row : commentaires) {
			final Class<? extends Tache> classTache = (Class<? extends Tache>) row[0];
			final String commentaire = (String) row[1];
			final TypeTache type = TypeTache.valueOf(classTache.getSimpleName());
			final List<String> list = map.computeIfAbsent(type, k -> new LinkedList<>());
			list.add(commentaire);
		}
		for (List<String> list : map.values()) {
			Collections.sort(list);
		}
		return map;
	}

	@Override
	public Set<Integer> getCollectivitesAvecTaches() {
		final String hql = "select distinct t.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative from Tache t";
		final Query query = getCurrentSession().createQuery(hql);
		//noinspection unchecked
		return new HashSet<>((List<Integer>) query.list());
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
					if (Long.valueOf(noDi).equals(annul.getDeclaration().getId())) {
						return true;
					}
				}
			}
		}

		// Recherche dans la base de données

		final String query = "from TacheAnnulationDeclaration tache where tache.contribuable.id = :ctb and "
				+ "tache.declaration.id = :noDi and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
		final List<Tache> list = find(query,
		                              buildNamedParameters(Pair.of("ctb", noCtb), Pair.of("noDi", noDi)),
		                              FlushModeType.COMMIT);
		return !list.isEmpty();
	}

	private static boolean isTacheOuverte(final Tache t) {
		return TypeEtatTache.EN_INSTANCE == t.getEtat() || TypeEtatTache.EN_COURS == t.getEtat();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Tache> List<T> listTaches(long noCtb, TypeTache type) {
		final String query = "from " + type.name() + " tache where tache.contribuable.id = :noCtb";
		return find(query, buildNamedParameters(Pair.of("noCtb", noCtb)) , FlushModeType.COMMIT);
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
		final FlushModeType mode = session.getFlushMode();
		session.setFlushMode(FlushModeType.COMMIT);
		try {
			// met-à-jour les tâches concernées
			final Query update = session.createNativeQuery(updateCollAdm);
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
					"where type(tache) != TacheNouveauDossier and tache.etat = 'EN_INSTANCE' and tache.dateEcheance <= :dateEcheance and tache.annulationDate is null " +
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

				final TacheStats s = stats.computeIfAbsent(oid, k -> new TacheStats());
				s.tachesEnInstance = count.intValue();
			}
		}

		// récupère les stats des dossiers en instance
		{
			final Query query = session.createQuery(queryDossiers);
			query.setParameter("dateEcheance", RegDate.get());

			final List list = query.list();
			for (Object o : list) {
				final Object tuple[] = (Object[]) o;
				final Integer oid = (Integer) tuple[0];
				final Long count = (Long) tuple[1];

				final TacheStats s = stats.computeIfAbsent(oid, k -> new TacheStats());
				s.dossiersEnInstance = count.intValue();
			}
		}

		// pas de besoin de synchronisation parce que l'assignement est atomique en java
		return stats;
	}
}
