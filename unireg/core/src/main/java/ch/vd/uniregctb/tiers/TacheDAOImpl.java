package ch.vd.uniregctb.tiers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.EntityKey;
import org.hibernate.impl.SessionImpl;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

public class TacheDAOImpl extends GenericDAOImpl<Tache, Long> implements TacheDAO {

	//private static final Logger LOGGER = Logger.getLogger(TacheDAOImpl.class);

	public TacheDAOImpl() {
		super(Tache.class);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Tache> find(TacheCriteria criterion) {
		return find(criterion, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<Tache> find(TacheCriteria criterion, boolean doNotAutoFlush) {

		List<Object> params = new ArrayList<Object>();
		final String query = "select tache " + buildFromWhereClause(criterion, params) + " order by tache.id asc";

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<Tache> list = (List<Tache>) find(query, params.toArray(), mode);
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<Tache> find(long noContribuable) {
		final String query = "select t from Tache t where t.contribuable.id=" + noContribuable + " order by t.id asc";
		return getHibernateTemplate().find(query);
	}

	/**
	 * Recherche d'un range de toutes les tâches correspondant au critère de sélection
	 *
	 * @param criterion
	 * @param page
	 * @param pageSize
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Tache> find(final TacheCriteria criterion, final ParamPagination paramPagination) {

		return (List<Tache>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				List<Object> params = new ArrayList<Object>();
				String query = "select tache " + buildFromWhereClause(criterion, params);

				query = query + buildOrderClause(paramPagination);

				Query queryObject = session.createQuery(query);
				Object[] values = params.toArray();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
				int maxResult = paramPagination.getTaillePage();
				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				return queryObject.list();
			}
		});
	}

	/**
	 * Construit la clause order
	 *
	 * @param paramPagination
	 * @return
	 */
	private String buildOrderClause(ParamPagination paramPagination) {
		String clauseOrder = "";
		if (paramPagination.getChamp() != null) {
			if (paramPagination.getChamp().equals("type")) {
				clauseOrder = " order by tache.class";
			}
			else {
				clauseOrder = " order by tache." + paramPagination.getChamp();
			}

			if (paramPagination.isSensAscending()) {
				clauseOrder = clauseOrder + " asc";
			}
			else {
				clauseOrder = clauseOrder + " desc";
			}
		}
		else {
			clauseOrder = " order by tache.id asc";

		}
		return clauseOrder;
	}

	/**
	 * {@inheritDoc}
	 */
	public int count(TacheCriteria criterion) {
		return count(criterion, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public int count(long noContribuable) {
		final String query = "select count(*) from Tache t where t.contribuable.id=" + noContribuable;
		return DataAccessUtils.intResult(getHibernateTemplate().find(query));
	}

	/**
	 * {@inheritDoc}
	 */
	public int count(TacheCriteria criterion, boolean doNotAutoFlush) {
		List<Object> params = new ArrayList<Object>();
		final String query = "select count(*) " + buildFromWhereClause(criterion, params);

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		int count = DataAccessUtils.intResult(find(query, params.toArray(), mode));
		return count;
	}

	/**
	 * Construit les clauses 'from' et 'where' de la requête de recherche des tâches.
	 */
	private String buildFromWhereClause(TacheCriteria criterion, List<Object> params) {

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
			clause += " and tache.class != " + typeTache.name() + " ";
		}

		// Contribuable
		final Contribuable contribuable = criterion.getContribuable();
		if (contribuable != null) {
			clause += " and tache.contribuable = ? ";
			params.add(contribuable);
		}

		// Numéro de contribuable
		if (criterion.getNumeroCTB() != null) {
			clause += " and tache.contribuable.numero = ? ";
			params.add(criterion.getNumeroCTB());
		}

		// Etat tache
		final TypeEtatTache etatTache = criterion.getEtatTache();
		if (etatTache != null) {
			clause += " and tache.etat = ? ";
			params.add(etatTache.name());
		}

		// Année de la période d'imposition
		final Integer annee = criterion.getAnnee();
		if (annee != null) {
			RegDate dateDebutPeriode = RegDate.get(annee.intValue(), 1, 1);
			RegDate dateFinPeriode = RegDate.get(annee.intValue(), 12, 31);

			if (TypeTache.TacheEnvoiDeclarationImpot.equals(typeTache)) {
				clause += " and tache.dateDebut >= ? ";
				clause += " and tache.dateFin <= ? ";
				params.add(new Integer(dateDebutPeriode.index()));
				params.add(new Integer(dateFinPeriode.index()));
			}
			else if (TypeTache.TacheAnnulationDeclarationImpot.equals(typeTache)) {
				clause += " and tache.declarationImpotOrdinaire.dateDebut >= ? ";
				clause += " and tache.declarationImpotOrdinaire.dateFin <= ? ";
				params.add(new Integer(dateDebutPeriode.index()));
				params.add(new Integer(dateFinPeriode.index()));
			}
		}

		// Office d'impôt
		final Integer oid = criterion.getOid();
		if (oid != null) {
			// [UNIREG-1850]
			clause += " and tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative = ? ";
			params.add(oid);
		}

		// Office d'impot de l'utilisateur
		final Integer[] oidUser = criterion.getOidUser();
		if (oidUser != null && oidUser.length > 0) {
			clause += " and (tache.collectiviteAdministrativeAssignee is null or " +
					"tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative in (?";
			params.add(oidUser[0]);
			for (int i = 1; i < oidUser.length; i++) {
				clause += ",?";
				params.add(oidUser[i]);
			}
			clause += "))";
		}

		final Date dateCreationDepuis = criterion.getDateCreationDepuis();
		if (dateCreationDepuis != null) {
			clause += " and tache.logCreationDate >= ? ";
			params.add(dateCreationDepuis);
		}

		final Date dateCreationJusqua = criterion.getDateCreationJusqua();
		if (dateCreationJusqua != null) {
			clause += " and tache.logCreationDate <= ? ";
			params.add(dateCreationJusqua);
		}

		final RegDate dateEcheanceJusqua = criterion.getDateEcheanceJusqua();
		if (dateEcheanceJusqua != null) {
			clause += " and tache.dateEcheance <= ? ";
			params.add(dateEcheanceJusqua.index());
		}

		final boolean inclureTachesAnnulees = criterion.isInclureTachesAnnulees();
		if (!inclureTachesAnnulees) {
			clause += " and tache.annulationDate is null";
		}

		final DeclarationImpotOrdinaire declarationAnnulee = criterion.getDeclarationAnnulee();
		if (declarationAnnulee != null && typeTache == TypeTache.TacheAnnulationDeclarationImpot) {
			clause += " and tache.declarationImpotOrdinaire = ?";
			params.add(declarationAnnulee);
		}

		return clause;
	}

	@SuppressWarnings("unchecked")
	public boolean existsTacheEnInstanceOuEnCours(final long noCtb, final TypeTache type) {

		return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				// Recherche dans le cache de la session

				SessionImpl s = (SessionImpl) session;
				final Map<EntityKey, Object> entities = s.getPersistenceContext().getEntitiesByKey();
				for (Object entity : entities.values()) {
					if (entity instanceof Tache) {
						final Tache t = (Tache) entity;
						if (t.getContribuable().getNumero().equals(noCtb) && type.equals(t.getTypeTache()) && (isTacheOuverte(t))) {
							return true;
						}
					}
				}

				// Recherche dans la base de données

				Object[] params = {
						noCtb
				};
				final String query = "from "
						+ type.name()
						+ " tache where tache.contribuable = ? and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
				final List<Tache> list = (List<Tache>) find(query, params, FlushMode.MANUAL);
				return !list.isEmpty();
			}
		});
	}


	@SuppressWarnings("unchecked")
	public boolean existsTacheEnvoiEnInstanceOuEnCours(final long noCtb, final RegDate dateDebut, final RegDate dateFin) {

		return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				// Recherche dans le cache de la session
				SessionImpl s = (SessionImpl) session;
				final Map<EntityKey, Object> entities = s.getPersistenceContext().getEntitiesByKey();
				for (Object entity : entities.values()) {
					if (entity instanceof Tache) {
						final Tache t = (Tache) entity;
						if (t.getContribuable().getNumero().equals(noCtb) && t instanceof TacheEnvoiDeclarationImpot && (isTacheOuverte(t))) {
							final TacheEnvoiDeclarationImpot envoi = (TacheEnvoiDeclarationImpot) t;
							if (dateDebut.equals(envoi.getDateDebut()) && dateFin.equals(envoi.getDateFin())) {
								return true;
							}
						}
					}
				}

				// Recherche dans la base de données
				Object[] params = {
						noCtb, dateDebut.index(), dateFin.index()
				};
				final String query = "from TacheEnvoiDeclarationImpot tache where tache.contribuable = ? and tache.dateDebut = ?  and tache.dateFin = ? and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
				final List<Tache> list = (List<Tache>) find(query, params, FlushMode.MANUAL);
				return !list.isEmpty();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public boolean existsTacheAnnulationEnInstanceOuEnCours(final long noCtb, final long noDi) {

		// Recherche dans le cache de la session

		return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				SessionImpl s = (SessionImpl) session;
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

				Object[] params = {
						noCtb, noDi
				};
				final String query = "from TacheAnnulationDeclarationImpot tache where tache.contribuable = ? and "
						+ "tache.declarationImpotOrdinaire.id = ? and tache.annulationDate is null and (tache.etat = 'EN_INSTANCE' or tache.etat = 'EN_COURS')";
				final List<Tache> list = (List<Tache>) find(query, params, FlushMode.MANUAL);
				return !list.isEmpty();
			}
		});
	}

	private static boolean isTacheOuverte(final Tache t) {
		return TypeEtatTache.EN_INSTANCE.equals(t.getEtat()) || TypeEtatTache.EN_COURS.equals(t.getEtat());
	}

	@SuppressWarnings("unchecked")
	public <T extends Tache> List<T> listTaches(long noCtb, TypeTache type) {
		Object[] params = {
				noCtb
		};
		final String query = "from " + type.name() + " tache where tache.contribuable = ?";
		final List<T> list = (List<T>) find(query, params, FlushMode.MANUAL);
		return list;
	}

	private static final String updateCollAdm =
			"update TACHE set CA_ID = (select ca.NUMERO from TIERS ca where ca.NUMERO_CA = :oid) where " +
					"ETAT = 'EN_INSTANCE' and " + // il ne faut pas modifier les tâches déjà traitées
					"TACHE_TYPE != 'CTRL_DOSSIER' and " + // [UNIREG-1024] les contrôles de dossiers doivent rester à l'ancien OID
					"CA_ID != (select aci.NUMERO from TIERS aci where aci.NUMERO_CA = 22) and " + // [UNIREG-2104] les tâches pour les décédés sont assignées à l'ACI et doivent le rester
					"CTB_ID = :ctbId and " +
					"ANNULATION_DATE is null"; // inutiles de modifier les tâches annulées pour rien

	public void updateCollAdmAssignee(final Map<Long, Integer> tiersOidsMapping) {

		if (tiersOidsMapping == null || tiersOidsMapping.isEmpty()) {
			return;
		}
		
		// [UNIREG-1024] On met-à-jour les tâches encore ouvertes, à l'exception des tâches de contrôle de dossier
		getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final FlushMode mode = session.getFlushMode();
				try {
					session.setFlushMode(FlushMode.MANUAL);

					// met-à-jour les tâches concernées
					final Query update = session.createSQLQuery(updateCollAdm);
					for (Map.Entry<Long, Integer> e : tiersOidsMapping.entrySet()) {
						update.setParameter("ctbId", e.getKey());
						update.setParameter("oid", e.getValue());
						update.executeUpdate();
					}

					return null;
				}
				finally {
					session.setFlushMode(mode);
				}
			}
		});
	}

	final static String queryTaches =
			"select " +
					"tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative, count(*) " +
					"from Tache tache " +
					"where tache.class != TacheNouveauDossier and tache.etat = 'EN_INSTANCE' and tache.dateEcheance <= :dateEcheance and tache.annulationDate is null " +
					"group by tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative";

	final static String queryDossiers =
			"select " +
					"tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative, count(*) " +
					"from TacheNouveauDossier tache " +
					"where tache.etat = 'EN_INSTANCE' and tache.dateEcheance <= :dateEcheance and tache.annulationDate is null " +
					"group by tache.collectiviteAdministrativeAssignee.numeroCollectiviteAdministrative";
	
	public Map<Integer, TacheStats> getTacheStats() {
		
		final Map<Integer, TacheStats> stats = new HashMap<Integer, TacheStats>();

		// récupère les stats des tâches en instance
		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final Query query = session.createQuery(queryTaches);
				query.setParameter("dateEcheance", RegDate.get().index());

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
				return null;
			}
		});

		// récupère les stats des dossiers en instance
		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final Query query = session.createQuery(queryDossiers);
				query.setParameter("dateEcheance", RegDate.get().index());

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
				return null;
			}
		});

		// pas de besoin de synchronisation parce que l'assignement est atomique en java
		return stats;
	}
}
