package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.engine.EntityKey;
import org.hibernate.impl.SessionImpl;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;

/**
 *
 *
 */
public class TiersDAOImpl extends GenericDAOImpl<Tiers, Long> implements TiersDAO {

    private static final Logger LOGGER = Logger.getLogger(TiersDAOImpl.class);
    private static final int MAX_IN_SIZE = 500;

	public TiersDAOImpl() {
        super(Tiers.class);
    }

    public Tiers get(long id, boolean doNotAutoFlush) {

        Object[] criteria = {
                id
        };
        String query = "from Tiers t where t.numero = ?";

        final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
        final List<?> list = find(query, criteria, mode);
        if (list.size() > 0) {
            return (Tiers) list.get(0);
        } else {
            return null;
        }
    }

	@SuppressWarnings({"unchecked"})
	public List<Tiers> getFirst(final int count) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("from Tiers");
				query.setMaxResults(count);
				return query.list();
			}
		});
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

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Tiers> getBatch(final Collection<Long> ids, final Set<Parts> parts) {

        return (List<Tiers>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {

                if (ids == null || ids.isEmpty()) {
                    return Collections.emptyList();
                }

	            final FlushMode mode = session.getFlushMode();
	            if (mode != FlushMode.MANUAL) {
		            LOGGER.warn("Le 'flushMode' de la session hibernate est forcé en MANUAL.");
		            session.setFlushMode(FlushMode.MANUAL); // pour éviter qu'Hibernate essaie de mettre-à-jour les collections des associations one-to-many avec des cascades delete-orphan.
	            }

	            return getBatch(ids, parts, session);
            }
        });
    }

	@SuppressWarnings({"unchecked"})
	private Object getBatch(Collection<Long> ids, Set<Parts> parts, Session session) {
		Assert.isTrue(ids.size() <= MAX_IN_SIZE, "Le nombre maximal d'ids est dépassé");

		// on complète la liste d'ids avec les tiers liés par rapports, si nécessaire
		final Set<Long> idsDemandes = new HashSet<Long>(ids);
		final Set<Long> idsLies = new HashSet<Long>();

		if (parts != null && parts.contains(Parts.RAPPORTS_ENTRE_TIERS)) {
			Query qsujets = session.createQuery("select r.sujetId from RapportEntreTiers r where r.objetId in (:ids)");
			qsujets.setParameterList("ids", ids);
			final List<Long> idsSujets = qsujets.list();
			idsLies.addAll(idsSujets);

			Query qobjets = session.createQuery("select r.objetId from RapportEntreTiers r where r.sujetId in (:ids)");
			qobjets.setParameterList("ids", ids);
			final List<Long> idsObjets = qobjets.list();
			idsLies.addAll(idsObjets);
		}

		final Set<Long> idsFull = new HashSet<Long>(idsDemandes);
		idsFull.addAll(idsLies);

		// on charge les tiers en vrac
		// idsFull -> les tiers liés sont nécessaires pour charger les rapport-entre-tiers sans sous-requêtes
		final List<Tiers> full = queryObjectsByIds("from Tiers as t where t.id in (:ids)", idsFull, session);
		for (Tiers t : full) {
			session.setReadOnly(t, true);
		}

		// [UNIREG-1985] extrait la liste des tiers demandés explicitement, de manière à ne initialiser les collections que sur ceux-ci.
		final List<Tiers> tiers = new ArrayList<Tiers>(idsDemandes.size());
		for (Tiers t : full) {
			if (idsDemandes.contains(t.getNumero())) {
				tiers.add(t);
			}
		}

		{
			// on charge les identifications des personnes en vrac
			Query q = session.createQuery("from IdentificationPersonne as a where a.personnePhysique.id in (:ids)");
			q.setParameterList("ids", idsDemandes);
			List<IdentificationPersonne> identifications = q.list();

			// on associe les identifications de personnes avec les tiers à la main
			associate(session, identifications, tiers, new TiersIdGetter<IdentificationPersonne>() {
				public Long getTiersId(IdentificationPersonne entity) {
					return entity.getPersonnePhysique().getId();
				}
			}, new EntitySetSetter<IdentificationPersonne>() {
				public void setEntitySet(Tiers tiers, Set<IdentificationPersonne> set) {
					if (tiers instanceof PersonnePhysique) {
						((PersonnePhysique) tiers).setIdentificationsPersonnes(set);
					}
				}
			});
		}

		if (parts != null && parts.contains(Parts.ADRESSES)) {
			// on charge toutes les adresses en vrac
			// idsFull -> les adresses des tiers liés sont nécessaires pour calculer les adresses fiscales
			List<AdresseTiers> adresses = queryObjectsByIds("from AdresseTiers as a where a.tiers.id in (:ids)", idsFull, session);

			// on associe les adresses avec les tiers à la main
			associate(session, adresses, full, new TiersIdGetter<AdresseTiers>() {
				public Long getTiersId(AdresseTiers entity) {
					return entity.getTiers().getId();
				}
			}, new EntitySetSetter<AdresseTiers>() {
				public void setEntitySet(Tiers tiers, Set<AdresseTiers> set) {
					tiers.setAdressesTiers(set);
				}
			});
		}

		if (parts != null && parts.contains(Parts.DECLARATIONS)) {

			// on précharge toutes les périodes fiscales pour éviter de les charger une à une depuis les déclarations d'impôt
			Query periodes = session.createQuery("from PeriodeFiscale");
			periodes.list();

			// on charge toutes les declarations en vrac
			Query q = session.createQuery("from Declaration as d where d.tiers.id in (:ids)");
			q.setParameterList("ids", idsDemandes);
			List<Declaration> declarations = q.list();

			// on associe les déclarations avec les tiers à la main
			associate(session, declarations, tiers, new TiersIdGetter<Declaration>() {
				public Long getTiersId(Declaration entity) {
					return entity.getTiers().getId();
				}
			}, new EntitySetSetter<Declaration>() {
				public void setEntitySet(Tiers tiers, Set<Declaration> set) {
					tiers.setDeclarations(set);
				}
			});
		}

		if (parts != null && parts.contains(Parts.FORS_FISCAUX)) {
			// on charge tous les fors fiscaux en vrac
			Query q = session.createQuery("from ForFiscal as f where f.tiers.id in (:ids)");
			q.setParameterList("ids", idsDemandes);
			List<ForFiscal> fors = q.list();

			// on associe les fors fiscaux avec les tiers à la main
			associate(session, fors, tiers, new TiersIdGetter<ForFiscal>() {
				public Long getTiersId(ForFiscal entity) {
					return entity.getTiers().getId();
				}
			}, new EntitySetSetter<ForFiscal>() {
				public void setEntitySet(Tiers tiers, Set<ForFiscal> set) {
					tiers.setForsFiscaux(set);
				}
			});
		}

		if (parts != null && parts.contains(Parts.RAPPORTS_ENTRE_TIERS)) {
			// on charge tous les rapports entre tiers en vrac
			{
				Query q = session.createQuery("from RapportEntreTiers as r where r.sujetId in (:ids)");
				q.setParameterList("ids", idsDemandes);
				List<RapportEntreTiers> rapports = q.list();

				// on associe les rapports avec les tiers à la main
				associate(session, rapports, tiers, new TiersIdGetter<RapportEntreTiers>() {
					public Long getTiersId(RapportEntreTiers entity) {
						return entity.getSujetId();
					}
				}, new EntitySetSetter<RapportEntreTiers>() {
					public void setEntitySet(Tiers tiers, Set<RapportEntreTiers> set) {
						tiers.setRapportsSujet(set);
					}
				});
			}
			{

				Query q = session.createQuery("from RapportEntreTiers as r where r.objetId in (:ids)");
				q.setParameterList("ids", idsDemandes);
				List<RapportEntreTiers> rapports = q.list();

				// on associe les rapports avec les tiers à la main
				associate(session, rapports, tiers, new TiersIdGetter<RapportEntreTiers>() {
					public Long getTiersId(RapportEntreTiers entity) {
						return entity.getObjetId();
					}
				}, new EntitySetSetter<RapportEntreTiers>() {
					public void setEntitySet(Tiers tiers, Set<RapportEntreTiers> set) {
						tiers.setRapportsObjet(set);
					}
				});
			}
		}

		if (parts != null && parts.contains(Parts.SITUATIONS_FAMILLE)) {
			// on charge toutes les situations de famille en vrac
			Query q = session.createQuery("from SituationFamille as r where r.contribuable.id in (:ids)");
			q.setParameterList("ids", idsDemandes);
			List<SituationFamille> situations = q.list();

			// on associe les situations de famille avec les tiers à la main
			associate(session, situations, tiers, new TiersIdGetter<SituationFamille>() {
				public Long getTiersId(SituationFamille entity) {
					return entity.getContribuable().getId();
				}
			}, new EntitySetSetter<SituationFamille>() {
				public void setEntitySet(Tiers tiers, Set<SituationFamille> set) {
					if (tiers instanceof Contribuable) {
						((Contribuable) tiers).setSituationsFamille(set);
					}
				}
			});
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
        } else {
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

    @SuppressWarnings("unchecked")
    public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {

        List<Long> list;
        if (ctbStart > 0 && ctbEnd > 0) {
            list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero >= ? AND tiers.numero <= ?", new Object[]{ctbStart, ctbEnd});
        } else if (ctbStart > 0) {
            list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero >= ?", ctbStart);
        } else if (ctbEnd > 0) {
            list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero <= ?", ctbEnd);
        } else {
            Assert.isTrue(ctbStart < 0 && ctbEnd < 0);
            list = getHibernateTemplate().find("SELECT tiers.numero FROM Tiers AS tiers");
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<Long> getAllIds() {
        return (List<Long>) getHibernateTemplate().find("select tiers.numero from Tiers as tiers");
    }

    @SuppressWarnings("unchecked")
    public List<Long> getDirtyIds() {
        return (List<Long>) getHibernateTemplate().find("select tiers.numero from Tiers as tiers where tiers.indexDirty = true");
    }

    /**
     * ne retourne que le numero des PP de type habitant
     */
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

    @SuppressWarnings("unchecked")
    public Set<Long> getNumerosIndividu(final Set<Long> tiersIds, final boolean includesComposantsMenage) {

	    if (tiersIds.size() > 1000) {
			throw new IllegalArgumentException("Il n'est pas possible de spécifier plus de 1'000 ids");		    
	    }

        return (Set<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {

	            final Set<Long> numeros = new HashSet<Long>(tiersIds.size());

	            if (includesComposantsMenage) {
		            // on récupère les numéros d'individu des composants des ménages
					final Query queryComposants = session.createQuery(QUERY_GET_NOS_IND_COMPOSANTS);
					queryComposants.setParameterList("ids", tiersIds);
		            numeros.addAll(queryComposants.list());
	            }

                final Query queryObject = session.createQuery(QUERY_GET_NOS_IND);
                queryObject.setParameterList("ids", tiersIds);
                numeros.addAll(queryObject.list());

	            return numeros;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<Long> getHabitantsForMajorite(RegDate dateReference) {
        // FIXME (???) la date de référence n'est pas utilisée !
        return (List<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(PersonnePhysique.class);
                criteria.add(Restrictions.eq("habitant", Boolean.TRUE));
                criteria.setProjection(Projections.property("numero"));
                DetachedCriteria subCriteria = DetachedCriteria.forClass(ForFiscal.class);
                subCriteria.setProjection(Projections.id());
                subCriteria.add(Expression.isNull("dateFin"));
                subCriteria.add(Expression.eqProperty("tiers.numero", Criteria.ROOT_ALIAS + ".numero"));
                criteria.add(Subqueries.notExists(subCriteria));
                return criteria.list();
            }
        });
    }


    @Override
    public boolean exists(final Long id) {

        final String name = this.getPersistentClass().getCanonicalName();

        return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {

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
                criteria.add(Expression.eq("numero", id));
                Integer count = (Integer) criteria.uniqueResult();
                return count > 0;
            }
        });
    }


    public RapportEntreTiers save(RapportEntreTiers object) {
        TracePoint tp = TracingManager.begin();
        Object obj = super.getHibernateTemplate().merge(object);
        TracingManager.end(tp);
        return (RapportEntreTiers) obj;
    }

    /**
     * {@inheritDoc}
     */
    public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
        return getHabitantByNumeroIndividu(numeroIndividu, false);
    }

    public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
	    return getPPByNumeroIndividu(numeroIndividu, true, doNotAutoFlush);
    }

    /**
     * @see ch.vd.uniregctb.tiers.TiersDAO#getPPByNumeroIndividu(java.lang.Long, boolean)
     */
    public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
	    return getPPByNumeroIndividu(numeroIndividu, false, doNotAutoFlush);
    }

	public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {

		final Object[] criteria = {numeroIndividu};
		final String query = "select pp.id from PersonnePhysique pp where pp.numeroIndividu = ? and pp.annulationDate is null order by pp.numero asc";

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<?> list = find(query, criteria, mode);

		if (list.isEmpty()) {
			return null;
		}

		if (list.size() > 1) {
			final long[] ids = new long[list.size()];
			for (int i = 0; i < list.size(); ++i) {
				ids[i] = (Long) list.get(i);
			}
			throw new PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException(numeroIndividu, ids);
		}

		return (Long) list.get(0);
	}

	private PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean habitantSeulement, boolean doNotAutoFlush) {

		final Object[] criteria = {numeroIndividu};
		final StringBuilder b = new StringBuilder("from PersonnePhysique pp where pp.numeroIndividu = ? and pp.annulationDate is null");
		if (habitantSeulement) {
			b.append(" and pp.habitant = true");
		}
		b.append(" order by pp.numero asc");
		final String query = b.toString();

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<?> list = find(query, criteria, mode);
		if (list.size() == 0) {
			return null;
		}
		else if (list.size() == 1) {
		    return (PersonnePhysique) list.get(0);
		}
		else {
			final long[] noPersonnesPhysiques = new long[list.size()];
			for (int i = 0 ; i < list.size() ; ++ i) {
				final PersonnePhysique pp = (PersonnePhysique) list.get(i);
				noPersonnesPhysiques[i] = pp.getNumero();
			}
		    throw new PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException(numeroIndividu, noPersonnesPhysiques);
		}
	}

    /**
     * {@inheritDoc}
     */
    public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
        return getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique, false);
    }

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

    public Contribuable getContribuableByNumero(Long numeroContribuable) {
        LOGGER.debug("Recherche du contribuable dont le numéro est:" + numeroContribuable);
        return (Contribuable) getHibernateTemplate().get(Contribuable.class, numeroContribuable);
    }

    /**
     * @see ch.vd.uniregctb.tiers.TiersDAO#getDebiteurPrestationImposableByNumero(java.lang.Long)
     */
    public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
        LOGGER.debug("Recherche du Debiteur Prestation Imposable dont le numéro est:" + numeroDPI);
        return (DebiteurPrestationImposable) getHibernateTemplate().get(DebiteurPrestationImposable.class, numeroDPI);
    }

    /**
     * @see ch.vd.uniregctb.tiers.TiersDAO#getPPByNumeroIndividu(java.lang.Long)
     */
    public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
        return getPPByNumeroIndividu(numeroIndividu, false);
    }

    @SuppressWarnings("unchecked")
    public List<PersonnePhysique> getSourciers(int noSourcier) {
        LOGGER.debug("Recherche d'un sourcier dont le numéro est:" + noSourcier);
        Object[] criteria = {noSourcier};
        String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier = ?";
        return (List<PersonnePhysique>) getHibernateTemplate().find(query, criteria);
    }

    @SuppressWarnings("unchecked")
    public List<PersonnePhysique> getAllMigratedSourciers() {
        LOGGER.debug("Recherche de tous les sourciers migrés");
        String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier > 0";
        return (List<PersonnePhysique>) getHibernateTemplate().find(query);
    }

    @SuppressWarnings("unchecked")
    public Tiers getTiersForIndexation(final long id) {

        final List<Tiers> list = (List<Tiers>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
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
        } else {
            Assert.isEqual(1, list.size());
            return list.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    public List<MenageCommun> getMenagesCommuns(final List<Long> ids, Set<Parts> parts) {
        final List<Long> idsMC = (List<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Query query = session.createQuery("select mc.numero from MenageCommun mc where mc.numero in (:ids)");
                query.setParameterList("ids", ids);
                return query.list();
            }
        });

        final List<Tiers> tiers = getBatch(idsMC, parts);
        final List<MenageCommun> menages = new ArrayList<MenageCommun>(tiers.size());
        for (Tiers t : tiers) {
            menages.add((MenageCommun) t);
        }
        return menages;
    }

	public Contribuable getContribuable(final DebiteurPrestationImposable debiteur) {
		return (Contribuable) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
		    public Object doInHibernate(Session session) throws HibernateException {
		        Query query = session.createQuery("select t from ContactImpotSource r, Tiers t where r.objetId = :dpiId and r.sujetId = t.id and r.annulationDate is null");
		        query.setParameter("dpiId", debiteur.getId());
			    final FlushMode mode = session.getFlushMode();
			    try {
			        session.setFlushMode(FlushMode.MANUAL);
				    return query.uniqueResult();
			    }
			    finally {
			        session.setFlushMode(mode);
			    }
		    }
		});
	}

	public void updateOids(final Map<Long, Integer> tiersOidsMapping) {

		if (tiersOidsMapping == null || tiersOidsMapping.isEmpty()) {
			return;
		}

		// [UNIREG-1024] On met-à-jour les tâches encore ouvertes, à l'exception des tâches de contrôle de dossier
		getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final FlushMode mode = session.getFlushMode();
				try {
					session.setFlushMode(FlushMode.MANUAL);

					// met-à-jour les tiers concernés
					final Query update = session.createSQLQuery("update TIERS set OID = :oid where NUMERO = :id");
					for (Map.Entry<Long, Integer> e : tiersOidsMapping.entrySet()) {
						update.setParameter("id", e.getKey());
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
}
