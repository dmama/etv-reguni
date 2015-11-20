package ch.vd.uniregctb.tiers;

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

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.SessionImpl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class TiersDAOImpl extends BaseDAOImpl<Tiers, Long> implements TiersDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersDAOImpl.class);
	private static final int MAX_IN_SIZE = 500;
	private static final ImmeubleAccessor IMMEUBLE_ACCESSOR = new ImmeubleAccessor();
	private static final DecisionAciAccessor DECISION_ACI_ACCESSOR = new DecisionAciAccessor();

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

		final String query = "from Tiers t where t.numero = :id";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<Tiers> list = find(query, buildNamedParameters(Pair.of("id", id)), mode);
		if (!list.isEmpty()) {
			return list.get(0);
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
		final Session session = getCurrentSession();
		final Map<Class, List<Tiers>> map = new HashMap<>();
		for (Class clazz : TIERS_CLASSES) {
			final Query query = session.createQuery("from Tiers t where t.class = " + clazz.getSimpleName());
			query.setMaxResults(count);
			final List<Tiers> tiers = query.list();
			if (tiers != null && !tiers.isEmpty()) {
				map.put(clazz, tiers);
			}
		}
		return map;
	}

	@Override
	public Set<Long> getRelatedIds(final long id, int maxDepth) {

		final Set<Long> ids = new HashSet<>();
		ids.add(id);

		// on démarre avec l'id passé
		final Set<Long> input = new HashSet<>();
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

		final Session session = getCurrentSession();
		final List<Object[]> list;
		final FlushMode mode = session.getFlushMode();
		session.setFlushMode(FlushMode.MANUAL);
		try {
			final String hql = "select r.objetId, r.sujetId from RapportEntreTiers r where r.class != RapportPrestationImposable and (r.objetId in (:ids) OR r.sujetId in (:ids))";
			list = queryObjectsByIds(hql, input, session);
		}
		finally {
			session.setFlushMode(mode);
		}

		final Set<Long> output = new HashSet<>();
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

		final Map<Long, Set<T>> map = new HashMap<>();

		for (T e : entities) {
			session.setReadOnly(e, true);
			final Long tiersId = getter.getTiersId(e);
			Set<T> set = map.get(tiersId);
			if (set == null) {
				set = new HashSet<>();
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
				a = new HashSet<>();
			}
			setter.setEntitySet(t, a);
		}
	}

	private <T extends HibernateEntity> void associate(Session session, List<T> entities, List<Tiers> tiers, TiersIdGetter<T> getter, EntitySetSetter<T> setter) {
		Map<Long, Set<T>> m = groupByTiersId(session, entities, getter);
		associateSetsWith(m, tiers, setter);
	}

	private static String buildHqlForSujets(boolean excludeContactsImpotSource) {
		final StringBuilder hqlSujets = new StringBuilder();
		hqlSujets.append("select r.sujetId from RapportEntreTiers r where r.annulationDate is null");
		if (excludeContactsImpotSource) {
			hqlSujets.append(" and r.class != ContactImpotSource");
		}
		hqlSujets.append(" and r.objetId in (:ids)");
		return hqlSujets.toString();
	}

	private static String buildHqlForObjets(boolean excludeContactsImpotSource) {
		final StringBuilder hqlSujets = new StringBuilder();
		hqlSujets.append("select r.objetId from RapportEntreTiers r where r.annulationDate is null");
		if (excludeContactsImpotSource) {
			hqlSujets.append(" and r.class != ContactImpotSource");
		}
		hqlSujets.append(" and r.sujetId in (:ids)");
		return hqlSujets.toString();
	}

	@Override
	public Set<Long> getIdsTiersLies(final Collection<Long> ids, final boolean excludeContactsImpotSource) {

		if (ids == null || ids.isEmpty()) {
			return Collections.emptySet();
		}

		final Session session = getCurrentSession();

		// on complète la liste d'ids avec les tiers liés par rapports
		final Set<Long> idsDemandes = new HashSet<>(ids);
		final Set<Long> idsLies = new HashSet<>();

		// les tiers liés en tant que sujets
		final String hqlSujets = buildHqlForSujets(excludeContactsImpotSource);
		final List<Long> idsSujets = queryObjectsByIds(hqlSujets, idsDemandes, session);
		idsLies.addAll(idsSujets);

		// les tiers liés en tant qu'objets
		final String hqlObjets = buildHqlForObjets(excludeContactsImpotSource);
		final List<Long> idsObjets = queryObjectsByIds(hqlObjets, idsDemandes, session);
		idsLies.addAll(idsObjets);

		final Set<Long> idsFull = new HashSet<>(idsDemandes);
		idsFull.addAll(idsLies);

		return idsFull;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Tiers> getBatch(final Collection<Long> ids, final Set<Parts> parts) {

		final Session session = getCurrentSession();

		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}

		final FlushMode mode = session.getFlushMode();
		if (mode != FlushMode.MANUAL) {
			LOGGER.warn("Le 'flushMode' de la session hibernate est forcé en MANUAL.");
			session.setFlushMode(FlushMode.MANUAL); // pour éviter qu'Hibernate essaie de mettre-à-jour les collections des associations one-to-many avec des cascades delete-orphan.
		}
		try {
			return getBatch(ids instanceof Set ? (Set) ids : new HashSet<>(ids), parts, session);
		}
		finally {
			if (mode != FlushMode.MANUAL) {
				session.setFlushMode(mode);
			}
		}
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

		{
			// on charge les identifications d'entreprises en vrac
			final List<IdentificationEntreprise> identifications = queryObjectsByIds("from IdentificationEntreprise as a where a.ctb.id in (:ids)", ids, session);

			final TiersIdGetter<IdentificationEntreprise> getter = new TiersIdGetter<IdentificationEntreprise>() {
				@Override
				public Long getTiersId(IdentificationEntreprise entity) {
					return entity.getCtb().getId();
				}
			};

			final EntitySetSetter<IdentificationEntreprise> setter = new EntitySetSetter<IdentificationEntreprise>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<IdentificationEntreprise> set) {
					if (tiers instanceof Contribuable) {
						((Contribuable) tiers).setIdentificationsEntrepriseForGetBatch(set);
					}
				}
			};

			// on associe les identifications d'entreprises avec les tiers à la main
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
					if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
						((ContribuableImpositionPersonnesPhysiques) tiers).setSituationsFamille(set);
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

		if (parts != null && parts.contains(Parts.ALLEGEMENTS_FISCAUX)) {
			// on charge les allègements fiscaux en vrac
			final List<AllegementFiscal> allegements = queryObjectsByIds("from AllegementFiscal as af where af.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<AllegementFiscal> getter = new TiersIdGetter<AllegementFiscal>() {
				@Override
				public Long getTiersId(AllegementFiscal entity) {
					return entity.getEntreprise().getId();
				}
			};

			final EntitySetSetter<AllegementFiscal> setter = new EntitySetSetter<AllegementFiscal>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<AllegementFiscal> set) {
					if (tiers instanceof Entreprise) {
						((Entreprise) tiers).setAllegementsFiscaux(set);
					}
				}
			};

			// associations manuelles
			associate(session, allegements, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.ETATS_FISCAUX)) {
			// on charge les états fiscaux en vrac
			final List<EtatEntreprise> etats = queryObjectsByIds("from EtatEntreprise as ee where ee.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<EtatEntreprise> getter = new TiersIdGetter<EtatEntreprise>() {
				@Override
				public Long getTiersId(EtatEntreprise entity) {
					return entity.getEntreprise().getId();
				}
			};

			final EntitySetSetter<EtatEntreprise> setter = new EntitySetSetter<EtatEntreprise>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<EtatEntreprise> set) {
					if (tiers instanceof Entreprise) {
						((Entreprise) tiers).setEtats(set);
					}
				}
			};

			// associations manuelles
			associate(session, etats, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.REGIMES_FISCAUX)) {
			// on charge les régimes fiscaux en vrac
			final List<RegimeFiscal> regimes = queryObjectsByIds("from RegimeFiscal as rf where rf.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<RegimeFiscal> getter = new TiersIdGetter<RegimeFiscal>() {
				@Override
				public Long getTiersId(RegimeFiscal entity) {
					return entity.getEntreprise().getId();
				}
			};

			final EntitySetSetter<RegimeFiscal> setter = new EntitySetSetter<RegimeFiscal>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<RegimeFiscal> set) {
					if (tiers instanceof Entreprise) {
						((Entreprise) tiers).setRegimesFiscaux(set);
					}
				}
			};

			// associations manuelles
			associate(session, regimes, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.DONNEES_RC)) {
			// on charge les états fiscaux en vrac
			final List<DonneesRegistreCommerce> donnees = queryObjectsByIds("from DonneesRegistreCommerce as rc where rc.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<DonneesRegistreCommerce> getter = new TiersIdGetter<DonneesRegistreCommerce>() {
				@Override
				public Long getTiersId(DonneesRegistreCommerce entity) {
					return entity.getEntreprise().getId();
				}
			};

			final EntitySetSetter<DonneesRegistreCommerce> setter = new EntitySetSetter<DonneesRegistreCommerce>() {
				@Override
				public void setEntitySet(Tiers tiers, Set<DonneesRegistreCommerce> set) {
					if (tiers instanceof Entreprise) {
						((Entreprise) tiers).setDonneesRC(set);
					}
				}
			};

			// associations manuelles
			associate(session, donnees, tiers, getter, setter);
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

		// [SIFISC-7271] on doit faire en sorte que l'ensemble des IDs ne contient jamais de null (hibernate n'aime pas ça...)
		if (ids.size() > 0 && ids.contains(null)) {
			// on crée une nouvelle instance de Set pour se prémunir dans le cas où le Set en entrée serait Read-Only
			ids = new HashSet<>(ids);
			ids.remove(null);
		}

		final List<T> list;
		final int size = ids.size();
		if (size > 0) {
			final Query query = session.createQuery(hql);
			if (size <= MAX_IN_SIZE) {
				// on charge les entités en vrac
				query.setParameterList("ids", ids);
				list = query.list();
			}
			else {
				// on charge les entités par sous lots
				list = new ArrayList<>(size);
				final List<Long> l = new ArrayList<>(ids);
				for (int i = 0; i < size; i += MAX_IN_SIZE) {
					final List<Long> sub = l.subList(i, Math.min(size, i + MAX_IN_SIZE));
					query.setParameterList("ids", sub);
					list.addAll(query.list());
				}
			}
		}
		else {
			list = Collections.emptyList();
		}

		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {

		List<Long> list;
		if (ctbStart > 0 && ctbEnd > 0) {
			list = find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero >= :min AND tiers.numero <= :max",
			            buildNamedParameters(Pair.of("min", (long) ctbStart),
			                                 Pair.of("max", (long) ctbEnd)),
			            null);
		}
		else if (ctbStart > 0) {
			list = find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero >= :min",
			            buildNamedParameters(Pair.of("min", (long) ctbStart)),
			            null);
		}
		else if (ctbEnd > 0) {
			list = find("SELECT tiers.numero FROM Tiers AS tiers WHERE tiers.numero <= :max",
			            buildNamedParameters(Pair.of("max", (long) ctbEnd)),
			            null);
		}
		else {
			Assert.isTrue(ctbStart < 0 && ctbEnd < 0);
			list = find("SELECT tiers.numero FROM Tiers AS tiers", null);
		}
		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllIds() {
		return find("select tiers.numero from Tiers as tiers", null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getAllIdsFor(boolean includeCancelled, TypeTiers... types) {
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append("where 1=1");

		// condition sur l'état annulé
		if (!includeCancelled) {
			whereClause.append(" and tiers.annulationDate is null");
		}

		// conditions sur les types
		if (types != null && types.length != 0) {
			whereClause.append(" and tiers.class in (");
			boolean first = true;
			for (TypeTiers t : types) {
				if (!first) {
					whereClause.append(", ");
				}
				whereClause.append(typeToSimpleClassname(t));
				first = false;
			}
			whereClause.append(")");
		}

		return find("select tiers.numero from Tiers as tiers " + whereClause, null);
	}

	private static String typeToSimpleClassname(TypeTiers type) {
		switch (type) {
		case AUTRE_COMMUNAUTE:
			return AutreCommunaute.class.getSimpleName();
		case COLLECTIVITE_ADMINISTRATIVE:
			return CollectiviteAdministrative.class.getSimpleName();
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return DebiteurPrestationImposable.class.getSimpleName();
		case ENTREPRISE:
			return Entreprise.class.getSimpleName();
		case ETABLISSEMENT:
			return Etablissement.class.getSimpleName();
		case MENAGE_COMMUN:
			return MenageCommun.class.getSimpleName();
		case PERSONNE_PHYSIQUE:
			return PersonnePhysique.class.getSimpleName();
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getDirtyIds() {
		// [UNIREG-1979] ajouté les tiers devant être réindexés dans le futur (note : on les réindexe systématiquement parce que :
		//      1) en cas de deux demandes réindexations dans le futur, celle plus éloignée gagne : on compense donc 
		//         cette limitation en réindexant automatiquement les tiers flaggés comme tels
		//      2) ça ne mange pas de pain
		return find("select tiers.numero from Tiers as tiers where tiers.indexDirty = true or tiers.reindexOn is not null", null);
	}

	/**
	 * ne retourne que le numero des PP de type habitant
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNumeroIndividu() {
		return find("select habitant.numeroIndividu from PersonnePhysique as habitant where habitant.habitant = true", null);
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
		return getNumerosIndividu(tiersIds, includesComposantsMenage, getCurrentSession());
	}

	public static Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage, Session session) {
		final Set<Long> numeros = new HashSet<>(tiersIds.size());

		final Set<Long> tiersIdSet = new HashSet<>(tiersIds);
		if (includesComposantsMenage) {
			// on récupère les numéros d'individu des composants des ménages
			final List<Long> nos = queryObjectsByIds(QUERY_GET_NOS_IND_COMPOSANTS, tiersIdSet, session);
			numeros.addAll(nos);
		}

		final List<Long> nos = queryObjectsByIds(QUERY_GET_NOS_IND, tiersIdSet, session);
		numeros.addAll(nos);
		return numeros;
	}

	private static final String SQL_QUERY_INDIVIDUS_LIES_PARENTE =
			"SELECT PPO.NUMERO_INDIVIDU FROM TIERS PPO " +
					"JOIN RAPPORT_ENTRE_TIERS RETS ON PPO.NUMERO=RETS.TIERS_SUJET_ID AND RETS.RAPPORT_ENTRE_TIERS_TYPE='Parente' " +
					"JOIN TIERS PPS ON PPS.NUMERO=RETS.TIERS_OBJET_ID " +
					"WHERE PPS.NUMERO_INDIVIDU=:noIndividu " +
					"UNION " +
			"SELECT PPO.NUMERO_INDIVIDU FROM TIERS PPO " +
					"JOIN RAPPORT_ENTRE_TIERS RETO ON PPO.NUMERO=RETO.TIERS_OBJET_ID AND RETO.RAPPORT_ENTRE_TIERS_TYPE='Parente' " +
					"JOIN TIERS PPS ON PPS.NUMERO=RETO.TIERS_SUJET_ID " +
					"WHERE PPS.NUMERO_INDIVIDU=:noIndividu";

	@Override
	public Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource) {
		final Session session = getCurrentSession();
		final SQLQuery query = session.createSQLQuery(SQL_QUERY_INDIVIDUS_LIES_PARENTE);
		query.setLong("noIndividu", noIndividuSource);
		//noinspection unchecked
		final List<? extends Number> list = query.list();
		final Set<Long> set = new HashSet<>(list.size());
		for (Number nr : list) {
			if (nr != null) {
				set.add(nr.longValue());
			}
		}
		return set;
	}

	@Nullable
	@Override
	public List<Long> getNumerosPMs(Collection<Long> tiersIds) {

		List<Long> list = null;

		for (Long id : tiersIds) {
			if (Entreprise.FIRST_ID <= id && id <= Entreprise.LAST_ID) {
				if (list == null) {
					list = new ArrayList<>();
				}
				list.add(id);
			}
		}

		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
		// FIXME (???) la date de référence n'est pas utilisée !
		final Session session = getCurrentSession();
		final Criteria criteria = session.createCriteria(PersonnePhysique.class);
		criteria.add(Restrictions.eq("habitant", Boolean.TRUE));
		criteria.setProjection(Projections.property("numero"));
		final DetachedCriteria subCriteria = DetachedCriteria.forClass(ForFiscal.class);
		subCriteria.setProjection(Projections.id());
		subCriteria.add(Restrictions.isNull("dateFin"));
		subCriteria.add(Restrictions.eqProperty("tiers.numero", Criteria.ROOT_ALIAS + ".numero"));
		criteria.add(Subqueries.notExists(subCriteria));
		return criteria.list();
	}


	@Override
	public boolean exists(final Long id) {

		final String name = this.getPersistentClass().getCanonicalName();

		final Session session = getCurrentSession();

		// recherche dans le cache de 1er niveau dans la session si le tiers existe.
		// Hack fix, on peut resoudre le problème en utilisant la fonction session.contains(), mais pour cela
		// la fonction equals et hashcode doit être définit dans la classe tiers.
		final SessionImpl s = (SessionImpl) session;
		// car il faut en prendre un. La classe EntityKey génére un hashCode sur la rootclass et non sur la classe de
		// l'instance
		// Doit être vérifier à chaque nouvelle release d'hibernate.
		final Tiers tiers = new PersonnePhysique(true);
		tiers.setNumero(id);
		final EntityKey key = ((SessionImpl) session).generateEntityKey(id, s.getEntityPersister(name, tiers));
		if (s.getPersistenceContext().containsEntity(key)) {
			return true;
		}
		final Criteria criteria = s.createCriteria(getPersistentClass());
		criteria.setProjection(Projections.rowCount());
		criteria.add(Restrictions.eq("numero", id));
		final int count = ((Number) criteria.uniqueResult()).intValue();
		return count > 0;
	}


	@Override
	public RapportEntreTiers save(RapportEntreTiers object) {
		final TracePoint tp = TracingManager.begin();
		final RapportEntreTiers obj = (RapportEntreTiers) saveObjectWithMerge(object);
		TracingManager.end(tp);
		return obj;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu) {
		return getHabitantByNumeroIndividu(numeroIndividu, false);
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		return getPPByNumeroIndividu(numeroIndividu, true, doNotAutoFlush);
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		return getPPByNumeroIndividu(numeroIndividu, false, doNotAutoFlush);
	}

	@Override
	public Long getNumeroPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		return getNumeroPPByNumeroIndividu(numeroIndividu, false, doNotAutoFlush);
	}

	@SuppressWarnings({"unchecked"})
	private Long getNumeroPPByNumeroIndividu(final long numeroIndividu, boolean habitantSeulement, final boolean doNotAutoFlush) {

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

		final Session session = getCurrentSession();

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

		final List<Long> list;
		try {
			final SQLQuery query = session.createSQLQuery(sql);
			query.setParameter("noIndividu", numeroIndividu);

			// tous les candidats sortent : il faut ensuite filtrer par rapport aux dates d'annulation et de réactivation...
			final List<Object[]> rows = query.list();
			if (rows != null && !rows.isEmpty()) {
				final List<Long> res = new ArrayList<>(rows.size());
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
				list = res;
			}
			else {
				list = null;
			}
		}
		finally {
			if (doNotAutoFlush) {
				session.setFlushMode(flushMode);
			}
		}

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

	/**
	 * Recherche l'Entreprise par son numéro d'organisation au régistre des entreprises.
	 *
	 * @param numeroOrganisation Le numéro RCEnt
	 * @return L'entreprise correspondant au numéro, ou null si aucune n'est trouvée.
	 */
	public Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation) {
		final Criteria crit = getCurrentSession().createCriteria(Entreprise.class);
		crit.add(Restrictions.eq("numeroEntreprise", numeroOrganisation));

		return (Entreprise) crit.uniqueResult();
	}

	/**
	 * Recherche l'établissement par son numéro de site au régistre des entreprises.
	 *
	 * @param numeroSite Le numéro RCEnt
	 * @return L'établissement correspondant au numéro, ou null si aucune n'est trouvée.
	 */
	@Override
	public Etablissement getEtablissementByNumeroSite(long numeroSite) {
		final Criteria crit = getCurrentSession().createCriteria(Etablissement.class);
		crit.add(Restrictions.eq("numeroEtablissement", numeroSite));

		return (Etablissement) crit.uniqueResult();
	}

	@Override
	public void updateOids(final Map<Long, Integer> tiersOidsMapping) {

		if (tiersOidsMapping == null || tiersOidsMapping.isEmpty()) {
			return;
		}

		// [UNIREG-1024] On met-à-jour les tâches encore ouvertes, à l'exception des tâches de contrôle de dossier
		final Session session = getCurrentSession();

		final FlushMode mode = session.getFlushMode();
		session.setFlushMode(FlushMode.MANUAL);
		try {
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
		}
		finally {
			session.setFlushMode(mode);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		return getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique, false);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(int numeroDistrict, boolean doNotAutoFlush) {
		final String query = "from CollectiviteAdministrative col where col.identifiantDistrictFiscal = :noDistrict";
		final FlushMode flushMode = doNotAutoFlush ? FlushMode.MANUAL : null;
		final List<CollectiviteAdministrative> list = find(query, buildNamedParameters(Pair.of("noDistrict", numeroDistrict)), flushMode);
		if (list == null || list.isEmpty()) {
			return null;
		}
		Assert.isEqual(1, list.size()); // une seule collectivité administrative de regroupement par district
		return list.get(0);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(int numeroRegion) {
		final String query = "from CollectiviteAdministrative col where col.identifiantRegionFiscale = :noRegion";
		final List<CollectiviteAdministrative> list = find(query, buildNamedParameters(Pair.of("noRegion", numeroRegion)), null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		Assert.isEqual(1, list.size()); // une seule collectivité administrative de regroupement par Region
		return list.get(0);
	}

	@Override
	@SuppressWarnings({"UnnecessaryBoxing"})
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {

		final String query = "from CollectiviteAdministrative col where col.numeroCollectiviteAdministrative = :noTechnique";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<CollectiviteAdministrative> list = find(query, buildNamedParameters(Pair.of("noTechnique", numeroTechnique)), mode);
		if (list == null || list.isEmpty()) {
			return null;
		}
		Assert.isEqual(1, list.size()); // le numéro de collectivité administrative est défini comme 'unique' sur la base
		return list.get(0);
	}

	@Override
	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche du contribuable dont le numéro est:" + numeroContribuable);
		}
		return (Contribuable) getCurrentSession().get(Contribuable.class, numeroContribuable);
	}

	/**
	 * @see ch.vd.uniregctb.tiers.TiersDAO#getDebiteurPrestationImposableByNumero(java.lang.Long)
	 */
	@Override
	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche du Debiteur Prestation Imposable dont le numéro est:" + numeroDPI);
		}
		return (DebiteurPrestationImposable) getCurrentSession().get(DebiteurPrestationImposable.class, numeroDPI);
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu) {
		return getPPByNumeroIndividu(numeroIndividu, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PersonnePhysique> getSourciers(int noSourcier) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche d'un sourcier dont le numéro est:" + noSourcier);
		}
		final String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier = :noSourcier";
		return find(query, buildNamedParameters(Pair.of("noSourcier", noSourcier)), null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PersonnePhysique> getAllMigratedSourciers() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recherche de tous les sourciers migrés");
		}
		final String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier > 0";
		return find(query, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Tiers getTiersForIndexation(final long id) {

		final Session session = getCurrentSession();

		final Criteria crit = session.createCriteria(Tiers.class);
		crit.add(Restrictions.eq("numero", id));
		crit.setFetchMode("rapportsSujet", FetchMode.JOIN);
		crit.setFetchMode("forFiscaux", FetchMode.JOIN);
		// msi : hibernate ne supporte pas plus de deux JOIN dans une même requête...
		// msi : on préfère les for fiscaux aux adresses tiers qui - à cause de l'AdresseAutreTiers - impose un deuxième left outer join sur Tiers
		// crit.setFetchMode("adressesTiers", FetchMode.JOIN);
		crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		final List<Tiers> list;
		final FlushMode mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushMode.MANUAL);
			list = crit.list();
		}
		finally {
			session.setFlushMode(mode);
		}

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
		final Session session = getCurrentSession();
		final String hql = "select mc.numero from MenageCommun mc where mc.numero in (:ids)";
		final Set<Long> set = new HashSet<>(ids);
		final List<Long> idsMC = queryObjectsByIds(hql, set, session);

		final List<Tiers> tiers = getBatch(idsMC, parts);
		final List<MenageCommun> menages = new ArrayList<>(tiers.size());
		for (Tiers t : tiers) {
			menages.add((MenageCommun) t);
		}
		return menages;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Contribuable getContribuable(final DebiteurPrestationImposable debiteur) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("select t from ContactImpotSource r, Tiers t where r.objetId = :dpiId and r.sujetId = t.id and r.annulationDate is null");
		query.setParameter("dpiId", debiteur.getId());

		final FlushMode mode = session.getFlushMode();
		session.setFlushMode(FlushMode.MANUAL);
		try {
			return (Contribuable) query.uniqueResult();
		}
		finally {
			session.setFlushMode(mode);
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> getListeDebiteursSansPeriodicites() {
		final Session session = getCurrentSession();
		final Query q = session.createQuery("select d.numero from DebiteurPrestationImposable d where size(d.periodicites) = 0");
		return q.list();
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

	@Override
	public DecisionAci addAndSave(Contribuable tiers, DecisionAci decisionAci) {
		return addAndSave(tiers,decisionAci,DECISION_ACI_ACCESSOR);
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

	private static final EntityAccessor<ContribuableImpositionPersonnesPhysiques, SituationFamille> SITUATION_FAMILLE_ACCESSOR = new EntityAccessor<ContribuableImpositionPersonnesPhysiques, SituationFamille>() {
		@Override
		public Collection<SituationFamille> getEntities(ContribuableImpositionPersonnesPhysiques ctb) {
			return ctb.getSituationsFamille();
		}

		@Override
		public void addEntity(ContribuableImpositionPersonnesPhysiques ctb, SituationFamille entity) {
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
	public SituationFamille addAndSave(ContribuableImpositionPersonnesPhysiques contribuable, SituationFamille situation) {
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

	private static final EntityAccessor<Contribuable, IdentificationEntreprise> IDENTIFICATION_ENTREPRISE_ACCESSOR = new EntityAccessor<Contribuable, IdentificationEntreprise>() {
		@Override
		public Collection<IdentificationEntreprise> getEntities(Contribuable ctb) {
			return ctb.getIdentificationsEntreprise();
		}

		@Override
		public void addEntity(Contribuable ctb, IdentificationEntreprise entity) {
			ctb.addIdentificationEntreprise(entity);
		}

		@Override
		public void assertSame(IdentificationEntreprise entity1, IdentificationEntreprise entity2) {
			Assert.isSame(entity1.getNumeroIde(), entity2.getNumeroIde());
		}
	};

	@Override
	public IdentificationEntreprise addAndSave(Contribuable ctb, IdentificationEntreprise ident) {
		return addAndSave(ctb, ident, IDENTIFICATION_ENTREPRISE_ACCESSOR);
	}

	private static final EntityAccessor<Etablissement, DomicileEtablissement> DOMICILE_ETABLISSEMENT_ACCESSOR = new EntityAccessor<Etablissement, DomicileEtablissement>() {
		@Override
		public Collection<DomicileEtablissement> getEntities(Etablissement tiers) {
			return tiers.getDomiciles();
		}

		@Override
		public void addEntity(Etablissement etablissement, DomicileEtablissement domicile) {
			etablissement.addDomicile(domicile);
		}

		@Override
		public void assertSame(DomicileEtablissement entity1, DomicileEtablissement entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
			Assert.isSame(entity1.getTypeAutoriteFiscale(), entity2.getTypeAutoriteFiscale());
			Assert.isSame(entity1.getNumeroOfsAutoriteFiscale(), entity2.getNumeroOfsAutoriteFiscale());
		}
	};

	@Override
	public DomicileEtablissement addAndSave(Etablissement etb, DomicileEtablissement domicile) {
		return addAndSave(etb, domicile, DOMICILE_ETABLISSEMENT_ACCESSOR);
	}

	private static final EntityAccessor<Entreprise, AllegementFiscal> ALLEGEMENT_FISCAL_ACCESSOR = new EntityAccessor<Entreprise, AllegementFiscal>() {
		@Override
		public Collection<AllegementFiscal> getEntities(Entreprise tiers) {
			return tiers.getAllegementsFiscaux();
		}

		@Override
		public void addEntity(Entreprise tiers, AllegementFiscal entity) {
			tiers.addAllegementFiscal(entity);
		}

		@Override
		public void assertSame(AllegementFiscal entity1, AllegementFiscal entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
			Assert.isSame(entity1.getNoOfsCommune(), entity2.getNoOfsCommune());
			Assert.isSame(entity1.getTypeCollectivite(), entity2.getTypeCollectivite());
			Assert.isSame(entity1.getTypeImpot(), entity2.getTypeImpot());
		}
	};

	@Override
	public AllegementFiscal addAndSave(Entreprise entreprise, AllegementFiscal allegement) {
		return addAndSave(entreprise, allegement, ALLEGEMENT_FISCAL_ACCESSOR);
	}

	private static final EntityAccessor<Entreprise, Bouclement> BOUCLEMENT_ACCESSOR = new EntityAccessor<Entreprise, Bouclement>() {
		@Override
		public Collection<Bouclement> getEntities(Entreprise tiers) {
			return tiers.getBouclements();
		}

		@Override
		public void addEntity(Entreprise tiers, Bouclement entity) {
			tiers.addBouclement(entity);
		}

		@Override
		public void assertSame(Bouclement entity1, Bouclement entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getAncrage(), entity2.getAncrage());
			Assert.isSame(entity1.getPeriodeMois(), entity2.getPeriodeMois());
		}
	};

	@Override
	public Bouclement addAndSave(Entreprise entreprise, Bouclement bouclement) {
		return addAndSave(entreprise, bouclement, BOUCLEMENT_ACCESSOR);
	}

	private static final EntityAccessor<Entreprise, RegimeFiscal> REGIME_FISCAL_ACCESSOR = new EntityAccessor<Entreprise, RegimeFiscal>() {
		@Override
		public Collection<RegimeFiscal> getEntities(Entreprise tiers) {
			return tiers.getRegimesFiscaux();
		}

		@Override
		public void addEntity(Entreprise tiers, RegimeFiscal entity) {
			tiers.addRegimeFiscal(entity);
		}

		@Override
		public void assertSame(RegimeFiscal entity1, RegimeFiscal entity2) {
			Assert.isSame(entity1.getPortee(), entity2.getPortee());
			Assert.isSame(entity1.getType(), entity2.getType());
		}
	};

	@Override
	public RegimeFiscal addAndSave(Entreprise entreprise, RegimeFiscal regime) {
		return addAndSave(entreprise, regime, REGIME_FISCAL_ACCESSOR);
	}

	private static final EntityAccessor<Entreprise, EtatEntreprise> ETAT_ENTREPRISE_ACCESSOR = new EntityAccessor<Entreprise, EtatEntreprise>() {
		@Override
		public Collection<EtatEntreprise> getEntities(Entreprise tiers) {
			return tiers.getEtats();
		}

		@Override
		public void addEntity(Entreprise tiers, EtatEntreprise entity) {
			tiers.addEtat(entity);
		}

		@Override
		public void assertSame(EtatEntreprise entity1, EtatEntreprise entity2) {
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
			Assert.isSame(entity1.getType(), entity2.getType());
		}
	};

	@Override
	public EtatEntreprise addAndSave(Entreprise entreprise, EtatEntreprise etat) {
		return addAndSave(entreprise, etat, ETAT_ENTREPRISE_ACCESSOR);
	}

	private static final EntityAccessor<Entreprise, FlagEntreprise> FLAG_ENTREPRISE_ACCESSOR = new EntityAccessor<Entreprise, FlagEntreprise>() {
		@Override
		public Collection<FlagEntreprise> getEntities(Entreprise tiers) {
			return tiers.getFlags();
		}

		@Override
		public void addEntity(Entreprise tiers, FlagEntreprise entity) {
			tiers.addFlag(entity);
		}

		@Override
		public void assertSame(FlagEntreprise entity1, FlagEntreprise entity2) {
			Assert.isSame(entity1.getAnneeDebutValidite(), entity2.getAnneeDebutValidite());
			Assert.isSame(entity1.getAnneeFinValidite(), entity2.getAnneeFinValidite());
			Assert.isSame(entity1.getType(), entity2.getType());
		}
	};

	@Override
	public FlagEntreprise addAndSave(Entreprise entreprise, FlagEntreprise flag) {
		return addAndSave(entreprise, flag, FLAG_ENTREPRISE_ACCESSOR);
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
				keys = new HashSet<>(entities.size());
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

		final Session session = getCurrentSession();
		final SQLQuery queryObject = session.createSQLQuery(RequeteContribuablesModifies);
		queryObject.setTimestamp("debut", dateDebutRech);
		queryObject.setTimestamp("fin", dateFinRech);

		final List<Object> listeResultat = queryObject.list();
		final List<Long> listeCtbModifies = new ArrayList<>(listeResultat.size());
		for (Object o : listeResultat) {
			listeCtbModifies.add(((Number) o).longValue());
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Date de debut: %s ; Date de fin: %s ; Nombre de ctb modifiés: %d", dateDebutRech, dateFinRech, listeCtbModifies.size()));
		}

		// TODO les allègements fiscaux des PM doivent-ils être pris en compte ?

		return listeCtbModifies;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getIdsConnusDuCivil() {
		final String hql = "select pp.id from PersonnePhysique pp where pp.numeroIndividu is not null";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getIdsParenteDirty() {
		final String hql = "select pp.id from PersonnePhysique pp where pp.numeroIndividu is not null and pp.parenteDirty=true";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		return query.list();
	}

	@Override
	public boolean setFlagBlocageRemboursementAutomatique(long tiersId, boolean newFlag) {
		final Session session = getCurrentSession();
		final FlushMode mode = session.getFlushMode();
		session.setFlushMode(FlushMode.MANUAL);
		try {
			final String sql = "UPDATE TIERS SET BLOC_REMB_AUTO=:newFlag, LOG_MDATE=:now, LOG_MUSER=:user WHERE NUMERO=:numero AND (BLOC_REMB_AUTO IS NULL OR BLOC_REMB_AUTO != :newFlag)";
			final SQLQuery query = getCurrentSession().createSQLQuery(sql);
			query.setParameter("newFlag", newFlag);
			query.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
			query.setParameter("now", DateHelper.getCurrentDate());
			query.setParameter("numero", tiersId);
			return query.executeUpdate() > 0;
		}
		finally {
			session.setFlushMode(mode);
		}
	}

	private interface EntityAccessor<T extends Tiers, E extends HibernateEntity> {
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


	private static class DecisionAciAccessor implements EntityAccessor<Contribuable, DecisionAci> {
		@Override
		public Collection<DecisionAci> getEntities(Contribuable ctb) {
			return ctb.getDecisionsAci();
		}

		@Override
		public void addEntity(Contribuable ctb, DecisionAci decisionAci) {
			ctb.addDecisionAci(decisionAci);
		}

		@Override
		public void assertSame(DecisionAci entity1, DecisionAci entity2) {
			Assert.isSame(entity1.getNumeroOfsAutoriteFiscale(), entity2.getNumeroOfsAutoriteFiscale());
			Assert.isSame(entity1.getTypeAutoriteFiscale(), entity2.getTypeAutoriteFiscale());
			Assert.isSame(entity1.getContribuable().getNumero(), entity2.getContribuable().getNumero());
			Assert.isSame(entity1.getDateDebut(), entity2.getDateDebut());
			Assert.isSame(entity1.getDateFin(), entity2.getDateFin());
		}
	}
}
