package ch.vd.uniregctb.tiers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.EntityKey;
import org.hibernate.impl.SessionImpl;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

/**
 *
 *
 */
public class TiersDAOImpl extends GenericDAOImpl<Tiers, Long> implements TiersDAO {

    private static final Logger LOGGER = Logger.getLogger(TiersDAOImpl.class);
    private static final int MAX_IN_SIZE = 500;

	private Dialect dialect;

	public TiersDAOImpl() {
        super(Tiers.class);
    }

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
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

	private static final List<Class<? extends Tiers>> TIERS_CLASSES =
			Arrays.asList(PersonnePhysique.class, MenageCommun.class, Entreprise.class, Etablissement.class, AutreCommunaute.class, CollectiviteAdministrative.class,
					DebiteurPrestationImposable.class);

	@SuppressWarnings({"unchecked"})
	public Map<Class, List<Tiers>> getFirstGroupedByClass(final int count) {
		return (Map<Class, List<Tiers>>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
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
	private List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts, Session session) {
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
		// TODO (msi) cette étape n'est plus nécessaire depuis que les rapport-entre-tiers ne font que stocker les ids des tiers liés. A supprimer donc.
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

		if (parts != null && parts.contains(Parts.PERIODICITES)) {
			// on charge toutes les périodicités en vrac
			Query q = session.createQuery("from Periodicite as p where p.debiteur.id in (:ids)");
			q.setParameterList("ids", idsDemandes);
			List<Periodicite> periodicites = q.list();

			// on associe les périodicités avec les tiers à la main
			associate(session, periodicites, tiers, new TiersIdGetter<Periodicite>() {
				public Long getTiersId(Periodicite entity) {
					return entity.getDebiteur().getId();
				}
			}, new EntitySetSetter<Periodicite>() {
				public void setEntitySet(Tiers tiers, Set<Periodicite> set) {
					if (tiers instanceof DebiteurPrestationImposable) {
						((DebiteurPrestationImposable) tiers).setPeriodicites(set);
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
		// [UNIREG-1979] ajouté les tiers devant être réindexés dans le futur (note : on les réindexe systématiquement parce que :
		//      1) en cas de deux demandes réindexations dans le futur, celle plus éloignée gagne : on compense donc 
		//         cette limitation en réindexant automatiquement les tiers flaggés comme tels
		//      2) ça ne mange pas de pain
		return (List<Long>) getHibernateTemplate().find("select tiers.numero from Tiers as tiers where tiers.indexDirty = true or tiers.reindexOn is not null");
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
    public Set<Long> getNumerosIndividu(final Collection<Long> tiersIds, final boolean includesComposantsMenage) {

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

		final List<Long> list = (List<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {

				FlushMode flushMode = null;
				if (doNotAutoFlush) {
					flushMode = session.getFlushMode();
					session.setFlushMode(FlushMode.MANUAL);
				} else {
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
					if (rows != null && rows.size() > 0) {
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

	public List<Long> getListeDebiteursSansPeriodicites() {
		return (List<Long>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query q = session.createQuery("select d.numero from DebiteurPrestationImposable d where size(d.periodicites) = 0");
				return q.list();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {

		if (forFiscal.getId() == null) { // le for n'a jamais été persisté

			// on mémorise les ids des fors existant
			final Set<Long> ids;
			final Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
			if (forsFiscaux == null || forsFiscaux.isEmpty()) {
				ids = Collections.emptySet();
			}
			else {
				ids = new HashSet<Long>(forsFiscaux.size());
				for (ForFiscal f : forsFiscaux) {
					final Long id = f.getId();
					Assert.notNull(id, "Les fors existants doivent être persistés.");
					ids.add(id);
				}
			}

			// on ajoute le for et sauve le tout
			tiers.addForFiscal(forFiscal);
			tiers = save(tiers);

			// on recherche le for nouvellement ajouté
			ForFiscal nouveauFor = null;
			for (ForFiscal f : tiers.getForsFiscaux()) {
				if (!ids.contains(f.getId())) {
					nouveauFor = f;
					break;
				}
			}

			Assert.isSame(forFiscal.getDateDebut(), nouveauFor.getDateDebut());
			Assert.isSame(forFiscal.getDateFin(), nouveauFor.getDateFin());
			forFiscal = (T) nouveauFor;
		}
		else {
			tiers.addForFiscal(forFiscal);
		}

		Assert.notNull(forFiscal.getId());
		return forFiscal;
	}
}
