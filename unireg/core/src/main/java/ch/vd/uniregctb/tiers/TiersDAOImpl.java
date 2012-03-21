package ch.vd.uniregctb.tiers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.EntityKey;
import org.hibernate.impl.SessionImpl;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class TiersDAOImpl extends GenericDAOImpl<Tiers, Long> implements TiersDAO {

	private static final Logger LOGGER = Logger.getLogger(TiersDAOImpl.class);
	private static final int MAX_IN_SIZE = 500;
	private static final ImmeubleAccessor IMMEUBLE_ACCESSOR = new ImmeubleAccessor();

	private Dialect dialect;

	public TiersDAOImpl() {
		super(Tiers.class);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public Tiers get(long id, boolean doNotAutoFlush) {

		Object[] criteria = {
				id
		};
		String query = "from Tiers t where t.numero = ?";

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<?> list = find(query, criteria, mode);
		if (!list.isEmpty()) {
			return (Tiers) list.get(0);
		}
		else {
			return null;
		}
	}

	private static final List<Class<? extends Tiers>> TIERS_CLASSES =
			Arrays.asList(PersonnePhysique.class, MenageCommun.class, Entreprise.class, Etablissement.class, AutreCommunaute.class, CollectiviteAdministrative.class,
					DebiteurPrestationImposable.class);

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<Class, List<Tiers>> getFirstGroupedByClass(final int count) {
		return getHibernateTemplate().execute(new HibernateCallback<Map<Class, List<Tiers>>>() {
			@Override
			public Map<Class, List<Tiers>> doInHibernate(Session session) throws HibernateException, SQLException {
				final Map<Class, List<Tiers>> map = new HashMap<Class, List<Tiers>>();
				for (Class clazz : TIERS_CLASSES) {
					final Query query = session.createQuery("from Tiers t where t.class = " + clazz.getSimpleName());
					query.setMaxResults(count);
					List<Tiers> tiers = query.list();
					if (tiers != null && !tiers.isEmpty()) {
						map.put(clazz, tiers);
					}
				}
				return map;
			}
		});
	}

	@Override
	public Set<Long> getRelatedIds(final long id, int maxDepth) {

		final Set<Long> ids = new HashSet<Long>();
		ids.add(id);

		// on démarre avec l'id passé
		final Set<Long> input = new HashSet<Long>();
		input.add(id);

		for (int i = 0; i < maxDepth; i++) {

			final Set<Long> output = getFirstLevelOfRelatedIds(input);
			ids.addAll(output);

			if (input.containsAll(output)) {
				// tous les liens ont été résolus -> plus rien à faire
				break;
			}

			// on étends la recherche aux nouveaux ids trouvés
			input.clear();
			input.addAll(output);
		}

		return ids;
	}

	@SuppressWarnings({"unchecked"})
	private Set<Long> getFirstLevelOfRelatedIds(final Set<Long> input) {

		if (input == null || input.isEmpty()) {
			return Collections.emptySet();
		}

		final List<Object[]> list = getHibernateTemplate().executeFind(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) throws HibernateException, SQLException {
				final FlushMode mode = session.getFlushMode();
				session.setFlushMode(FlushMode.MANUAL);
				try {
					final String hql = "select r.objetId, r.sujetId from RapportEntreTiers r where r.class != RapportPrestationImposable and (r.objetId in (:ids) OR r.sujetId in (:ids))";
					return queryObjectsByIds(hql, input, session);
				}
				finally {
					session.setFlushMode(mode);
				}
			}
		});

		final Set<Long> output = new HashSet<Long>();
		for (Object[] objects : list) {
			final Long objetId = (Long) objects[0];
			final Long sujetId = (Long) objects[1];
			output.add(objetId);
			output.add(sujetId);
		}

		return output;
	}

	private interface TiersIdGetter<T extends HibernateEntity> {
		public Long getTiersId(T entity);
	}

	private interface EntitySetSetter<T extends HibernateEntity> {
		public void setEntitySet(Tiers tiers, Set<T> set);
	}

	private <T extends HibernateEntity> Map<Long, Set<T>> groupByTiersId(Session session, List<T> entities, TiersIdGetter<T> getter) {

		final Map<Long, Set<T>> map = new HashMap<Long, Set<T>>();

		for (T e : entities) {
			session.setReadOnly(e, true);
			final Long tiersId = getter.getTiersId(e);
			Set<T> set = map.get(tiersId);
			if (set == null) {
				set = new HashSet<T>();
				map.put(tiersId, set);
			}
			set.add(e);
		}

		return map;
	}

	private <T extends HibernateEntity> void associateSetsWith(Map<Long, Set<T>> map, List<Tiers> tiers, EntitySetSetter<T> setter) {
		for (Tiers t : tiers) {
			Set<T> a = map.get(t.getId());
			if (a == null) {
				a = new HashSet<T>();
			}
			setter.setEntitySet(t, a);
		}
	}

	private <T extends HibernateEntity> void associate(Session session, List<T> entities, List<Tiers> tiers, TiersIdGetter<T> getter, EntitySetSetter<T> setter) {
		Map<Long, Set<T>> m = groupByTiersId(session, entities, getter);
		associateSetsWith(m, tiers, setter);
	}

	@Override
	public Set<Long> getIdsTiersLies(final Collection<Long> ids, final boolean excludeContactsImpotSource) {

		if (ids == null || ids.isEmpty()) {
			return Collections.emptySet();
		}

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Set<Long>>() {
			@SuppressWarnings({"unchecked"})
			@Override
			public Set<Long> doInHibernate(Session session) throws HibernateException, SQLException {

				// on complète la liste d'ids avec les tiers liés par rapports
				final Set<Long> idsDemandes = new HashSet<Long>(ids);
				final Set<Long> idsLies = new HashSet<Long>();

				// les tiers liés en tant que sujets
				final String hqlSujets = buildHqlForSujets(excludeContactsImpotSource);
				final List<Long> idsSujets = queryObjectsByIds(hqlSujets, idsDemandes, session);
				idsLies.addAll(idsSujets);

				// les tiers liés en tant qu'objets
				final String hqlObjets = buildHqlForObjets(excludeContactsImpotSource);
				final List<Long> idsObjets = queryObjectsByIds(hqlObjets, idsDemandes, session);
				idsLies.addAll(idsObjets);

				final Set<Long> idsFull = new HashSet<Long>(idsDemandes);
				idsFull.addAll(idsLies);
				
				return idsFull;
			}

			private String buildHqlForSujets(boolean excludeContactsImpotSource) {
				final StringBuilder hqlSujets = new StringBuilder();
				hqlSujets.append("select r.sujetId from RapportEntreTiers r where r.annulationDate is null");
				if (excludeContactsImpotSource) {
					hqlSujets.append(" and r.class != ContactImpotSource");
				}
				hqlSujets.append(" and r.objetId in (:ids)");
				return hqlSujets.toString();
			}

			private String buildHqlForObjets(boolean excludeContactsImpotSource) {
				final StringBuilder hqlSujets = new StringBuilder();
				hqlSujets.append("select r.objetId from RapportEntreTiers r where r.annulationDate is null");
				if (excludeContactsImpotSource) {
					hqlSujets.append(" and r.class != ContactImpotSource");
				}
				hqlSujets.append(" and r.sujetId in (:ids)");
				return hqlSujets.toString();
			}
		});
	}


	@Override
	@SuppressWarnings("unchecked")
	public List<Tiers> getBatch(final Collection<Long> ids, final Set<Parts> parts) {

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Tiers>>() {
			@Override
			public List<Tiers> doInHibernate(Session session) throws HibernateException, SQLException {

				if (ids == null || ids.isEmpty()) {
					return Collections.emptyList();
				}

				final FlushMode mode = session.getFlushMode();
				if (mode != FlushMode.MANUAL) {
					LOGGER.warn("Le 'flushMode' de la session hibernate est forcé en MANUAL.");
					session.setFlushMode(FlushMode.MANUAL); // pour éviter qu'Hibernate essaie de mettre-à-jour les collections des associations one-to-many avec des cascades delete-orphan.
				}

				return getBatch(new HashSet<Long>(ids), parts, session);
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	private List<Tiers> getBatch(Set<Long> ids, Set<Parts> parts, Session session) {

		// on charge les tiers en vrac
		final List<Tiers> tiers = queryObjectsByIds("from Tiers as t where t.id in (:ids)", ids, session);
		for (Tiers t : tiers) {
			session.setReadOnly(t, true);
		}

		{
			// on charge les identifications des personnes en vrac
			final List<IdentificationPersonne> identifications = queryObjectsByIds("from IdentificationPersonne as a where a.personnePhysique.id in (:ids)", ids, session);
			
			final TiersIdGetter<IdentificationPersonne> getter = new TiersIdGetter<IdentificationPersonne>() {
				@Override
				public Long getTiersId(IdentificationPersonne entity) {
					return entity.getPersonnePhysique().getId();
				}
			};
			
			final EntitySetSetter<IdentificationPersonne> setter = new EntitySetSetter<IdentificationPersonne>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<IdentificationPersonne> set) {
					if (tiers instanceof PersonnePhysique) {
						((PersonnePhysique) tiers).setIdentificationsPersonnesForGetBatch(set);
					}
				}
			}; 

			// on associe les identifications de personnes avec les tiers à la main
			associate(session, identifications, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.ADRESSES)) {

			// on charge toutes les adresses en vrac
			final List<AdresseTiers> adresses = queryObjectsByIds("from AdresseTiers as a where a.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<AdresseTiers> getter = new TiersIdGetter<AdresseTiers>() {
				@Override
				public Long getTiersId(AdresseTiers entity) {
					return entity.getTiers().getId();
				}
			};

			final EntitySetSetter<AdresseTiers> setter = new EntitySetSetter<AdresseTiers>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<AdresseTiers> set) {
					tiers.setAdressesTiers(set);
				}
			};

			// on associe les adresses avec les tiers à la main
			associate(session, adresses, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.DECLARATIONS)) {

			// on précharge toutes les périodes fiscales pour éviter de les charger une à une depuis les déclarations d'impôt
			final Query periodes = session.createQuery("from PeriodeFiscale");
			periodes.list();

			// on charge toutes les declarations en vrac
			final List<Declaration> declarations = queryObjectsByIds("from Declaration as d where d.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<Declaration> getter = new TiersIdGetter<Declaration>() {
				@Override
				public Long getTiersId(Declaration entity) {
					return entity.getTiers().getId();
				}
			};

			final EntitySetSetter<Declaration> setter = new EntitySetSetter<Declaration>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<Declaration> set) {
					tiers.setDeclarations(set);
				}
			};

			// on associe les déclarations avec les tiers à la main
			associate(session, declarations, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.FORS_FISCAUX)) {
			// on charge tous les fors fiscaux en vrac
			final List<ForFiscal> fors = queryObjectsByIds("from ForFiscal as f where f.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<ForFiscal> getter = new TiersIdGetter<ForFiscal>() {
				@Override
				public Long getTiersId(ForFiscal entity) {
					return entity.getTiers().getId();
				}
			};

			final EntitySetSetter<ForFiscal> setter = new EntitySetSetter<ForFiscal>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<ForFiscal> set) {
					tiers.setForsFiscaux(set);
				}
			};

			// on associe les fors fiscaux avec les tiers à la main
			associate(session, fors, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.RAPPORTS_ENTRE_TIERS)) {
			// on charge tous les rapports entre tiers en vrac
			{
				final List<RapportEntreTiers> rapports = queryObjectsByIds("from RapportEntreTiers as r where r.sujetId in (:ids)", ids, session);

				final TiersIdGetter<RapportEntreTiers> getter = new TiersIdGetter<RapportEntreTiers>() {
					@Override
					public Long getTiersId(RapportEntreTiers entity) {
						return entity.getSujetId();
					}
				};

				final EntitySetSetter<RapportEntreTiers> setter = new EntitySetSetter<RapportEntreTiers>() {
					@Override
					public void setEntitySet(Tiers tiers, Set<RapportEntreTiers> set) {
						tiers.setRapportsSujet(set);
					}
				};

				// on associe les rapports avec les tiers à la main
				associate(session, rapports, tiers, getter, setter);
			}
			{
				final List<RapportEntreTiers> rapports = queryObjectsByIds("from RapportEntreTiers as r where r.objetId in (:ids)", ids, session);

				final TiersIdGetter<RapportEntreTiers> getter = new TiersIdGetter<RapportEntreTiers>() {
					@Override
					public Long getTiersId(RapportEntreTiers entity) {
						return entity.getObjetId();
					}
				};

				final EntitySetSetter<RapportEntreTiers> setter = new EntitySetSetter<RapportEntreTiers>() {
					@Override
					public void setEntitySet(Tiers tiers, Set<RapportEntreTiers> set) {
						tiers.setRapportsObjet(set);
					}
				};

				// on associe les rapports avec les tiers à la main
				associate(session, rapports, tiers, getter, setter);
			}
		}

		if (parts != null && parts.contains(Parts.SITUATIONS_FAMILLE)) {

			// on charge toutes les situations de famille en vrac
			final List<SituationFamille> situations = queryObjectsByIds("from SituationFamille as r where r.contribuable.id in (:ids)", ids, session);

			final TiersIdGetter<SituationFamille> getter = new TiersIdGetter<SituationFamille>() {
				@Override
				public Long getTiersId(SituationFamille entity) {
					return entity.getContribuable().getId();
				}
			};

			final EntitySetSetter<SituationFamille> setter = new EntitySetSetter<SituationFamille>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<SituationFamille> set) {
					if (tiers instanceof Contribuable) {
						((Contribuable) tiers).setSituationsFamille(set);
					}
				}
			};

			// on associe les situations de famille avec les tiers à la main
			associate(session, situations, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.PERIODICITES)) {

			// on charge toutes les périodicités en vrac
			final List<Periodicite> periodicites = queryObjectsByIds("from Periodicite as p where p.debiteur.id in (:ids)", ids, session);

			final TiersIdGetter<Periodicite> getter = new TiersIdGetter<Periodicite>() {
				@Override
				public Long getTiersId(Periodicite entity) {
					return entity.getDebiteur().getId();
				}
			};

			final EntitySetSetter<Periodicite> setter = new EntitySetSetter<Periodicite>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<Periodicite> set) {
					if (tiers instanceof DebiteurPrestationImposable) {
						((DebiteurPrestationImposable) tiers).setPeriodicites(set);
					}
				}
			};

			// on associe les périodicités avec les tiers à la main
			associate(session, periodicites, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.IMMEUBLES)) {
			// on charge tous les immeubles en vrac
			final List<Immeuble> immeubles = queryObjectsByIds("from Immeuble as i where i.contribuable.id in (:ids)", ids, session);

			final TiersIdGetter<Immeuble> getter = new TiersIdGetter<Immeuble>() {
				@Override
				public Long getTiersId(Immeuble entity) {
					return entity.getContribuable().getId();
				}
			};

			final EntitySetSetter<Immeuble> setter = new EntitySetSetter<Immeuble>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<Immeuble> set) {
					if (tiers instanceof Contribuable) {
						((Contribuable) tiers).setImmeubles(set);
					}
				}
			};

			// on associe les immeubles avec les tiers à la main
			associate(session, immeubles, tiers, getter, setter);
		}
		return tiers;
	}

	/**
	 * Charge les objets d'un certain type en fonction des ids spécifiés.
	 * <p/>
	 * Cette méthode contourne la limitation du nombre d'éléments dans la clause in (1'000 pour Oracle, par exemple) en exécutant plusieurs requêtes si nécessaire.
	 *
	 * @param hql     la requête hql qui doit de la forme <i>from XXX as o where o.id in (:ids)</i>
	 * @param ids     les ids des tiers à charger
	 * @param session la session à utiliser
	 * @return une liste des entités trouvées
	 */
	@SuppressWarnings({"unchecked"})
	private static <T> List<T> queryObjectsByIds(String hql, Set<Long> ids, Session session) {

		final List<T> list;
		final int size = ids.size();
		final Query query = session.createQuery(hql);
		if (size <= MAX_IN_SIZE) {
			// on charge les entités en vrac
			query.setParameterList("ids", ids);
			list = query.list();
		}
		else {
			// on charge les entités par sous lots
			list = new ArrayList<T>(size);
			final List<Long> l = new ArrayList<Long>(ids);
			for (int i = 0; i < size; i += MAX_IN_SIZE) {
				final List<Long> sub = l.subList(i, Math.min(size, i + MAX_IN_SIZE));
				query.setParameterList("ids", sub);
				list.addAll(query.list());
			}
		}

		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {

		List<Long> list;
		if (ctbStart > 0 && ctbEnd > 0) {
			list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero >= ? AND tiers.numero <= ?", ctbStart, ctbEnd);
		}
		else if (ctbStart > 0) {
			list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero >= ?", ctbStart);
		}
		else if (ctbEnd > 0) {
			list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero <= ?", ctbEnd);
		}
		else {
			Assert.isTrue(ctbStart < 0 && ctbEnd < 0);
			list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers");
		}
		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllIds() {
		return (List<Long>) getHibernateTemplate().find("select tiers.numero from Tiers as tiers");
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getDirtyIds() {
		// [UNIREG-1979] ajouté les tiers devant être réindexés dans le futur (note : on les réindexe systématiquement parce que :
		//      1) en cas de deux demandes réindexations dans le futur, celle plus éloignée gagne : on compense donc 
		//         cette limitation en réindexant automatiquement les tiers flaggés comme tels
		//      2) ça ne mange pas de pain
		return (List<Long>) getHibernateTemplate().find("select tiers.numero from Tiers as tiers where tiers.indexDirty = true or tiers.reindexOn is not null");
	}

	/**
	 * ne retourne que le numero des PP de type habitant
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNumeroIndividu() {
		return (List<Long>) getHibernateTemplate().find("select habitant.numeroIndividu from PersonnePhysique as habitant where habitant.habitant = true");
	}

	private static final String QUERY_GET_NOS_IND =
			"select h.numeroIndividu " +
					"from PersonnePhysique h " +
					"where h.numeroIndividu is not null " +
					"and h.id in (:ids)";
	private static final String QUERY_GET_NOS_IND_COMPOSANTS =
			"select h.numeroIndividu " +
					"from PersonnePhysique h, AppartenanceMenage am " +
					"where am.sujetId = h.id " +
					"and am.annulationDate is null " +
					"and h.numeroIndividu is not null " +
					"and am.objetId in (:ids)";

	@Override
	public Set<Long> getNumerosIndividu(final Collection<Long> tiersIds, final boolean includesComposantsMenage) {

		if (tiersIds.size() > 1000) {
			throw new IllegalArgumentException("Il n'est pas possible de spécifier plus de 1'000 ids");
		}

		return getHibernateTemplate().executeWithNativeSession(new GetNumerosIndividusCallback(tiersIds, includesComposantsMenage));
	}

	@SuppressWarnings("unchecked")
	public static class GetNumerosIndividusCallback implements HibernateCallback<Set<Long>> {
		private final Collection<Long> tiersIds;
		private final boolean includesComposantsMenage;

		public GetNumerosIndividusCallback(Collection<Long> tiersIds, boolean includesComposantsMenage) {
			this.tiersIds = tiersIds;
			this.includesComposantsMenage = includesComposantsMenage;
		}

		@Override
		public Set<Long> doInHibernate(Session session) throws HibernateException {

			final Set<Long> numeros = new HashSet<Long>(tiersIds.size());

			final Set<Long> tiersIdSet = new HashSet<Long>(tiersIds);
			if (includesComposantsMenage) {
				// on récupère les numéros d'individu des composants des ménages
				final List<Long> nos = queryObjectsByIds(QUERY_GET_NOS_IND_COMPOSANTS, tiersIdSet, session);
				numeros.addAll(nos);
			}

			final List<Long> nos = queryObjectsByIds(QUERY_GET_NOS_IND, tiersIdSet, session);
			numeros.addAll(nos);
			return numeros;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
		// FIXME (???) la date de référence n'est pas utilisée !
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(PersonnePhysique.class);
				criteria.add(Restrictions.eq("habitant", Boolean.TRUE));
				criteria.setProjection(Projections.property("numero"));
				DetachedCriteria subCriteria = DetachedCriteria.forClass(ForFiscal.class);
				subCriteria.setProjection(Projections.id());
				subCriteria.add(Restrictions.isNull("dateFin"));
				subCriteria.add(Restrictions.eqProperty("tiers.numero", Criteria.ROOT_ALIAS + ".numero"));
				criteria.add(Subqueries.notExists(subCriteria));
				return criteria.list();
			}
		});
	}


	@Override
	public boolean exists(final Long id) {

		final String name = this.getPersistentClass().getCanonicalName();

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Boolean>() {
			@Override
			public Boolean doInHibernate(Session session) throws HibernateException, SQLException {

				// recherche dans le cache de 1er niveau dans la session si le tiers existe.
				// Hack fix, on peut resoudre le problème en utilisant la fonction session.contains(), mais pour cela
				// la fonction equals et hashcode doit être définit dans la classe tiers.
				SessionImpl s = (SessionImpl) session;
				// car il faut en prendre un. La classe EntityKey génére un hashCode sur la rootclass et non sur la classe de
				// l'instance
				// Doit être vérifier à chaque nouvelle release d'hibernate.
				Tiers tiers = new PersonnePhysique(true);
				tiers.setNumero(id);
				if (s.getPersistenceContext().containsEntity(new EntityKey(id, s.getEntityPersister(name, tiers), EntityMode.POJO)))
					return true;
				Criteria criteria = s.createCriteria(getPersistentClass());
				criteria.setProjection(Projections.rowCount());
				criteria.add(Restrictions.eq("numero", id));
				Integer count = (Integer) criteria.uniqueResult();
				return count > 0;
			}
		});
	}


	@Override
	public RapportEntreTiers save(RapportEntreTiers object) {
		TracePoint tp = TracingManager.begin();
		Object obj = super.getHibernateTemplate().merge(object);
		TracingManager.end(tp);
		return (RapportEntreTiers) obj;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
		return getHabitantByNumeroIndividu(numeroIndividu, false);
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return getPPByNumeroIndividu(numeroIndividu, true, doNotAutoFlush);
	}

	/**
	 * @see ch.vd.uniregctb.tiers.TiersDAO#getPPByNumeroIndividu(java.lang.Long, boolean)
	 */
	@Override
	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return getPPByNumeroIndividu(numeroIndividu, false, doNotAutoFlush);
	}

	@Override
	public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return getNumeroPPByNumeroIndividu(numeroIndividu, false, doNotAutoFlush);
	}

	@SuppressWarnings({"unchecked"})
	private Long getNumeroPPByNumeroIndividu(final Long numeroIndividu, boolean habitantSeulement, final boolean doNotAutoFlush) {

		/**
		 * la requête que l'on veut écrire est la suivante :
		 *      SELECT PP.NUMERO FROM TIERS PP
		 *      WHERE PP.NUMERO_INDIVIDU=? AND PP.ANNULATION_DATE IS NULL
		 *      (AND PP.PP_HABITANT = 1)
		 *      AND (SELECT NVL(MAX(FF.DATE_FERMETURE),0) FROM FOR_FISCAL FF WHERE FF.TIERS_ID=PP.NUMERO AND FF.ANNULATION_DATE IS NULL AND FF.MOTIF_FERMETURE='ANNULATION')
		 *      < (SELECT NVL(MAX(FF.DATE_OUVERTURE),1) FROM FOR_FISCAL FF WHERE FF.TIERS_ID=PP.NUMERO AND FF.ANNULATION_DATE IS NULL AND FF.MOTIF_OUVERTURE='REACTIVATION')
		 *      ORDER BY PP.NUMERO ASC;
		 *
		 * Mais voilà : NVL est une fonction spécifiquement Oracle et COALESCE, qui devrait être fonctionellement équivalente, semble souffrir d'un bug (voir UNIREG-2242)
		 * Donc on en revient aux bases : il nous faut les numéros des personnes physiques qui n'ont pas de for fermé pour motif annulation ou, si elles en ont, qui ont
		 * également un for ouvert postérieurement avec un motif "REACTIVATION"...
		 */

		final StringBuilder b = new StringBuilder();
		b.append("SELECT PP.NUMERO, MAX(FF_A.DATE_FERMETURE) AS DATE_DESACTIVATION, MAX(FF_R.DATE_OUVERTURE) AS DATE_REACTIVATION");
		b.append(" FROM TIERS PP");
		b.append(" LEFT OUTER JOIN FOR_FISCAL FF_A ON FF_A.TIERS_ID=PP.NUMERO AND FF_A.ANNULATION_DATE IS NULL AND FF_A.MOTIF_FERMETURE='ANNULATION'");
		b.append(" LEFT OUTER JOIN FOR_FISCAL FF_R ON FF_R.TIERS_ID=PP.NUMERO AND FF_R.ANNULATION_DATE IS NULL AND FF_R.MOTIF_OUVERTURE='REACTIVATION'");
		b.append(" WHERE PP.TIERS_TYPE='PersonnePhysique'");
		b.append(" AND PP.NUMERO_INDIVIDU=:noIndividu AND PP.ANNULATION_DATE IS NULL");
		if (habitantSeulement) {
			b.append(" AND PP.PP_HABITANT = ");
			b.append(dialect.toBooleanValueString(true));
		}
		b.append(" GROUP BY PP.NUMERO");
		b.append(" ORDER BY PP.NUMERO ASC");
		final String sql = b.toString();

		final List<Long> list = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {

				FlushMode flushMode = null;
				if (doNotAutoFlush) {
					flushMode = session.getFlushMode();
					session.setFlushMode(FlushMode.MANUAL);
				}
				else {
					// la requête ci-dessus n'est pas une requête HQL, donc hibernate ne fera pas les
					// flush potentiellement nécessaires... Idéalement, bien-sûr, il faudrait écrire la requête en HQL, mais
					// je n'y arrive pas...
					session.flush();
				}
				try {
					final SQLQuery query = session.createSQLQuery(sql);
					query.setParameter("noIndividu", numeroIndividu);

					// tous les candidats sortent : il faut ensuite filtrer par rapport aux dates d'annulation et de réactivation...
					final List<Object[]> rows = query.list();
					if (rows != null && !rows.isEmpty()) {
						final List<Long> res = new ArrayList<Long>(rows.size());
						for (Object[] row : rows) {
							final Number ppId = (Number) row[0];
							final Number indexDesactivation = (Number) row[1];
							final Number indexReactivation = (Number) row[2];
							if (indexDesactivation == null) {
								res.add(ppId.longValue());
							}
							else if (indexReactivation != null && indexReactivation.intValue() > indexDesactivation.intValue()) {
								res.add(ppId.longValue());
							}
						}
						return res;
					}
				}
				finally {
					if (doNotAutoFlush) {
						session.setFlushMode(flushMode);
					}
				}

				return null;
			}
		});

		if (list == null || list.isEmpty()) {
			return null;
		}

		if (list.size() > 1) {
			final long[] ids = new long[list.size()];
			for (int i = 0; i < list.size(); ++i) {
				ids[i] = list.get(i);
			}
			throw new PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException(numeroIndividu, ids);
		}

		return list.get(0);
	}

	private PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean habitantSeulement, boolean doNotAutoFlush) {

		// on passe par le numéro de tiers pour pouvoir factoriser l'algorithme dans la recherche du tiers, en espérant que les performances n'en seront pas trop affectées

		final Long id = getNumeroPPByNumeroIndividu(numeroIndividu, habitantSeulement, doNotAutoFlush);
		final PersonnePhysique pp;
		if (id != null) {
			pp = (PersonnePhysique) get(id, doNotAutoFlush);
		}
		else {
			pp = null;
		}
		return pp;
	}

	@Override
	public void updateOids(final Map<Long, Integer> tiersOidsMapping) {

		if (tiersOidsMapping == null || tiersOidsMapping.isEmpty()) {
			return;
		}

		// [UNIREG-1024] On met-à-jour les tâches encore ouvertes, à l'exception des tâches de contrôle de dossier
		getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final FlushMode mode = session.getFlushMode();
				try {
					session.setFlushMode(FlushMode.MANUAL);

					// met-à-jour les tiers concernés
					final Query update = session.createSQLQuery("update TIERS set OID = :oid where NUMERO = :id");
					final Query updateForNullValue = session.createSQLQuery("update TIERS set OID = null where NUMERO = :id");
					for (Map.Entry<Long, Integer> e : tiersOidsMapping.entrySet()) {
						if (e.getValue() != null) {
							update.setParameter("id", e.getKey());
							update.setParameter("oid", e.getValue());
							update.executeUpdate();
						}
						else {
							updateForNullValue.setParameter("id", e.getKey());
							updateForNullValue.executeUpdate();
						}

					}

					return null;
				}
				finally {
					session.setFlushMode(mode);
				}
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		return getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique, false);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(Integer numeroDistrict) {
		Object[] criteria = {
				Long.valueOf(numeroDistrict)
		};
		String query = "from CollectiviteAdministrative col where col.identifiantDistrictFiscal = ?";

		final List<?> list = find(query, criteria, null);

		if (list == null || list.isEmpty()) {
			return null;
		}
		Assert.isEqual(1, list.size()); // une seule collectivité administrative de regroupement par district
		return (CollectiviteAdministrative) list.get(0);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(Integer numeroRegion) {
		Object[] criteria = {
				Long.valueOf(numeroRegion)
		};
		String query = "from CollectiviteAdministrative col where col.identifiantRegionFiscale = ?";

		final List<?> list = find(query, criteria, null);

		if (list == null || list.isEmpty()) {
			return null;
		}
		Assert.isEqual(1, list.size()); // une seule collectivité administrative de regroupement par Region
		return (CollectiviteAdministrative) list.get(0);
	}

	@Override
	@SuppressWarnings({"UnnecessaryBoxing"})
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {

		Object[] criteria = {
				Integer.valueOf(numeroTechnique)
		};
		String query = "from CollectiviteAdministrative col where col.numeroCollectiviteAdministrative = ?";

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<?> list = find(query, criteria, mode);

		if (list == null || list.isEmpty()) {
			return null;
		}
		Assert.isEqual(1, list.size()); // le numéro de collectivité administrative est défini comme 'unique' sur la base
		return (CollectiviteAdministrative) list.get(0);
	}

	@Override
	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche du contribuable dont le numéro est:" + numeroContribuable);
		}
		return getHibernateTemplate().get(Contribuable.class, numeroContribuable);
	}

	/**
	 * @see ch.vd.uniregctb.tiers.TiersDAO#getDebiteurPrestationImposableByNumero(java.lang.Long)
	 */
	@Override
	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche du Debiteur Prestation Imposable dont le numéro est:" + numeroDPI);
		}
		return getHibernateTemplate().get(DebiteurPrestationImposable.class, numeroDPI);
	}

	/**
	 * @see ch.vd.uniregctb.tiers.TiersDAO#getPPByNumeroIndividu(java.lang.Long)
	 */
	@Override
	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
		return getPPByNumeroIndividu(numeroIndividu, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PersonnePhysique> getSourciers(int noSourcier) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche d'un sourcier dont le numéro est:" + noSourcier);
		}
		Object[] criteria = {noSourcier};
		String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier = ?";
		return (List<PersonnePhysique>) getHibernateTemplate().find(query, criteria);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PersonnePhysique> getAllMigratedSourciers() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche de tous les sourciers migrés");
		}
		String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier > 0";
		return (List<PersonnePhysique>) getHibernateTemplate().find(query);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Tiers getTiersForIndexation(final long id) {

		final List<Tiers> list = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Tiers>>() {
			@Override
			public List<Tiers> doInHibernate(Session session) throws HibernateException {
				Criteria crit = session.createCriteria(Tiers.class);
				crit.add(Restrictions.eq("numero", id));
				crit.setFetchMode("rapportsSujet", FetchMode.JOIN);
				crit.setFetchMode("forFiscaux", FetchMode.JOIN);
				// msi : hibernate ne supporte pas plus de deux JOIN dans une même requête...
				// msi : on préfère les for fiscaux aux adresses tiers qui - à cause de l'AdresseAutreTiers - impose un deuxième left outer join sur Tiers
				// crit.setFetchMode("adressesTiers", FetchMode.JOIN);
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

				final FlushMode mode = session.getFlushMode();
				try {
					session.setFlushMode(FlushMode.MANUAL);
					return crit.list();
				}
				finally {
					session.setFlushMode(mode);
				}
			}
		});

		if (list.isEmpty()) {
			return null;
		}
		else {
			Assert.isEqual(1, list.size());
			return list.get(0);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<MenageCommun> getMenagesCommuns(final List<Long> ids, Set<Parts> parts) {
		final List<Long> idsMC = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException {
				final String hql = "select mc.numero from MenageCommun mc where mc.numero in (:ids)";
				final Set<Long> set = new HashSet<Long>(ids);
				return queryObjectsByIds(hql, set, session);
			}
		});

		final List<Tiers> tiers = getBatch(idsMC, parts);
		final List<MenageCommun> menages = new ArrayList<MenageCommun>(tiers.size());
		for (Tiers t : tiers) {
			menages.add((MenageCommun) t);
		}
		return menages;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Contribuable getContribuable(final DebiteurPrestationImposable debiteur) {
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Contribuable>() {
			@Override
			public Contribuable doInHibernate(Session session) throws HibernateException {
				Query query = session.createQuery("select t from ContactImpotSource r, Tiers t where r.objetId = :dpiId and r.sujetId = t.id and r.annulationDate is null");
				query.setParameter("dpiId", debiteur.getId());
				final FlushMode mode = session.getFlushMode();
				try {
					session.setFlushMode(FlushMode.MANUAL);
					return (Contribuable) query.uniqueResult();
				}
				finally {
					session.setFlushMode(mode);
				}
			}
		});
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> getListeDebiteursSansPeriodicites() {
		return getHibernateTemplate().execute(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query q = session.createQuery("select d.numero from DebiteurPrestationImposable d where size(d.periodicites) = 0");
				return q.list();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		return addAndSave(tiers, forFiscal, new ForFiscalAccessor<T>());
	}

	@Override
	public Immeuble addAndSave(Contribuable ctb, Immeuble immeuble) {
		return addAndSave(ctb, immeuble, IMMEUBLE_ACCESSOR);
	}

	private static final EntityAccessor<Tiers, Declaration> DECLARATION_ACCESSOR = new EntityAccessor<Tiers, Declaration>() {
		@Override
		public Collection<Declaration> getEntities(Tiers tiers) {
			return tiers.getDeclarations();
		}

		@Override
		public void addEntity(Tiers tiers, Declaration d) {
			tiers.addDeclaration(d);
		}

		@Override
		public void assertSame(Declaration d1, Declaration d2) {
			Assert.isSame(d1.getDateDebut(), d2.getDateDebut());
			Assert.isSame(d1.getDateFin(), d2.getDateFin());
		}
	};

	@Override
	public Declaration addAndSave(Tiers tiers, Declaration declaration) {
		return addAndSave(tiers, declaration, DECLARATION_ACCESSOR);
	}

	private static final EntityAccessor<DebiteurPrestationImposable, Periodicite> PERIODICITE_ACCESSOR = new EntityAccessor<DebiteurPrestationImposable, Periodicite>() {
		@Override
		public Collection<Periodicite> getEntities(DebiteurPrestationImposable dpi) {
			return dpi.getPeriodicites();
		}

		@Override
		public void addEntity(DebiteurPrestationImposable dpi, Periodicite p) {
			dpi.addPeriodicite(p);
		}

		@Override
		public void assertSame(Periodicite p1, Periodicite p2) {
			Assert.isSame(p1.getDateDebut(), p2.getDateDebut());
			Assert.isSame(p1.getDateFin(), p2.getDateFin());
		}
	};

	@Override
	public Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite) {
		return addAndSave(debiteur, periodicite, PERIODICITE_ACCESSOR);
	}

	private static final EntityAccessor<Contribuable, SituationFamille> SITUATION_FAMILLE_ACCESSOR = new EntityAccessor<Contribuable, SituationFamille>() {
		@Override
		public Collection<SituationFamille> getEntities(Contribuable ctb) {
			return ctb.getSituationsFamille();
		}

		@Override
		public void addEntity(Contribuable ctb, SituationFamille entity) {
			ctb.addSituationFamille(entity);
		}

		@Override
		public void assertSame(SituationFamille entity1, SituationFamille entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SituationFamille addAndSave(Contribuable contribuable, SituationFamille situation) {
		return addAndSave(contribuable, situation, SITUATION_FAMILLE_ACCESSOR);
	}

	private static final EntityAccessor<Tiers, AdresseTiers> ADRESSE_TIERS_ACCESSOR = new EntityAccessor<Tiers, AdresseTiers>() {
		@Override
		public Collection<AdresseTiers> getEntities(Tiers tiers) {
			return tiers.getAdressesTiers();
		}

		@Override
		public void addEntity(Tiers tiers, AdresseTiers entity) {
			tiers.addAdresseTiers(entity);
		}

		@Override
		public void assertSame(AdresseTiers entity1, AdresseTiers entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse) {
		return addAndSave(tiers, adresse, ADRESSE_TIERS_ACCESSOR);
	}

	private static final EntityAccessor<PersonnePhysique, IdentificationPersonne> IDENTIFICATION_PERSONNE_ACCESSOR = new EntityAccessor<PersonnePhysique, IdentificationPersonne>() {
		@Override
		public Collection<IdentificationPersonne> getEntities(PersonnePhysique pp) {
			return pp.getIdentificationsPersonnes();
		}

		@Override
		public void addEntity(PersonnePhysique pp, IdentificationPersonne entity) {
			pp.addIdentificationPersonne(entity);
		}

		@Override
		public void assertSame(IdentificationPersonne entity1, IdentificationPersonne entity2) {
			Assert.isSame(entity1.getCategorieIdentifiant(), entity2.getCategorieIdentifiant());
			Assert.isSame(entity1.getIdentifiant(), entity2.getIdentifiant());
		}
	};

	@Override
	public IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident) {
		return addAndSave(pp, ident, IDENTIFICATION_PERSONNE_ACCESSOR);
	}

	@SuppressWarnings({"unchecked"})
	private <T extends Tiers, E extends HibernateEntity> E addAndSave(T tiers, E entity, EntityAccessor<T, E> accessor) {
		if (entity.getKey() == null) {
			// pas encore persistée

			// on mémorise les clés des entités existantes
			final Set<Object> keys;
			final Collection<E> entities = accessor.getEntities(tiers);
			if (entities == null || entities.isEmpty()) {
				keys = Collections.emptySet();
			}
			else {
				keys = new HashSet<Object>(entities.size());
				for (E d : entities) {
					final Object key = d.getKey();
					Assert.notNull(key, "Les entités existantes doivent être déjà persistées.");
					keys.add(key);
				}
			}

			// on ajoute la nouvelle entité et on sauve le tout
			accessor.addEntity(tiers, entity);
			tiers = (T) save(tiers);

			// rebelotte pour trouver la nouvelle entité
			E newEntity = null;
			for (E d : accessor.getEntities(tiers)) {
				if (!keys.contains(d.getKey())) {
					newEntity = d;
					break;
				}
			}

			Assert.notNull(newEntity);
			accessor.assertSame(entity, newEntity);
			entity = newEntity;
		}
		else {
			accessor.addEntity(tiers, entity);
		}

		Assert.notNull(entity.getKey());
		return entity;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> getListeCtbModifies(final Date dateDebutRech, final Date dateFinRech) {
		final String RequeteContribuablesModifies = //----------------------------------------------------
				"SELECT T.NUMERO AS CTB_ID                                                " +
						"FROM TIERS T                                                             " +
						"JOIN FOR_FISCAL FF ON FF.TIERS_ID=T.NUMERO                               " +
						"AND FF.FOR_TYPE != 'ForDebiteurPrestationImposable'                      " +
						"AND T.LOG_MDATE >= :debut                                                " +
						"AND T.LOG_MDATE <= :fin                                                  " +
						"                                                                         " +
						"UNION                                                                    " +
						"                                                                         " +
						"SELECT FF.TIERS_ID AS CTB_ID                                             " +
						"FROM FOR_FISCAL FF                                                       " +
						"WHERE FF.FOR_TYPE != 'ForDebiteurPrestationImposable'                    " +
						"AND FF.LOG_MDATE >= :debut                                               " +
						"AND FF.LOG_MDATE <= :fin                                                 " +
						"                                                                         " +
						"UNION                                                                    " +
						"                                                                         " +
						"SELECT DI.TIERS_ID AS CTB_ID                                             " +
						"FROM DECLARATION DI                                                      " +
						"JOIN ETAT_DECLARATION ED ON ED.DECLARATION_ID = DI.ID                    " +
						"JOIN FOR_FISCAL FF ON FF.TIERS_ID=DI.TIERS_ID                            " +
						"AND FF.FOR_TYPE != 'ForDebiteurPrestationImposable'                      " +
						"AND ED.LOG_MDATE >= :debut                                               " +
						"AND ED.LOG_MDATE <= :fin                                                 " +
						"AND ED.TYPE IN ('EMISE', 'ECHUE')                                        " +
						"ORDER BY CTB_ID                                                          ";

		final List<Long> listeCtbModifies = getHibernateTemplate().executeWithNewSession(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery queryObject = session.createSQLQuery(RequeteContribuablesModifies);

				queryObject.setTimestamp("debut", dateDebutRech);
				queryObject.setTimestamp("fin", dateFinRech);

				final List<Object> listeResultat = queryObject.list();
				final List<Long> resultat = new ArrayList<Long>(listeResultat.size());
				for (Object o : listeResultat) {
					resultat.add(((Number) o).longValue());
				}

				return resultat;
			}
		});

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Date de debut: %s ; Date de fin: %s ; Nombre de ctb modifiés: %d", dateDebutRech, dateFinRech, listeCtbModifies.size()));
		}

		return listeCtbModifies;
	}

	private static interface EntityAccessor<T extends Tiers, E extends HibernateEntity> {
		Collection<E> getEntities(T tiers);

		void addEntity(T tiers, E entity);

		void assertSame(E entity1, E entity2);
	}

	private static class ForFiscalAccessor<T extends ForFiscal> implements EntityAccessor<Tiers, T> {
		@Override
		public Collection<T> getEntities(Tiers tiers) {
			//noinspection unchecked,RedundantCast
			return (Collection<T>) tiers.getForsFiscaux();
		}

		@Override
		public void addEntity(Tiers tiers, T entity) {
			tiers.addForFiscal(entity);
		}

		@Override
		public void assertSame(T entity1, T entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
		}
	}

	private static class ImmeubleAccessor implements EntityAccessor<Contribuable, Immeuble> {
		@Override
		public Collection<Immeuble> getEntities(Contribuable ctb) {
			return ctb.getImmeubles();
		}

		@Override
		public void addEntity(Contribuable ctb, Immeuble immeuble) {
			ctb.addImmeuble(immeuble);
		}

		@Override
		public void assertSame(Immeuble entity1, Immeuble entity2) {
			Assert.isEqual(entity1.getNumero(), entity2.getNumero());
		}
	}
}
