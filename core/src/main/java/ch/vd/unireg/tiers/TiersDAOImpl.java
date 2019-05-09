package ch.vd.unireg.tiers;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.SessionImpl;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.foncier.AllegementFoncier;

public class TiersDAOImpl extends BaseDAOImpl<Tiers, Long> implements TiersDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersDAOImpl.class);
	private static final int MAX_IN_SIZE = 500;
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
		final FlushModeType mode = (doNotAutoFlush ? FlushModeType.COMMIT : null);
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
			final Query query = session.createQuery("from Tiers t where type(t) = " + clazz.getSimpleName());
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

	@Override
	public @NotNull List<Heritage> getLiensHeritage(@NotNull Collection<Long> tiersIds) {
		if (tiersIds.isEmpty()) {
			return Collections.emptyList();
		}
		final Query query = getCurrentSession().createQuery("from Heritage where objetId in (:ids) and annulationDate is null");
		query.setParameterList("ids", tiersIds);
		//noinspection unchecked
		return query.list();
	}

	@SuppressWarnings({"unchecked"})
	private Set<Long> getFirstLevelOfRelatedIds(final Set<Long> input) {

		if (input == null || input.isEmpty()) {
			return Collections.emptySet();
		}

		final Session session = getCurrentSession();
		final List<Object[]> list;
		final FlushModeType mode = session.getFlushMode();
		session.setFlushMode(FlushModeType.COMMIT);
		try {
			final String hql = "select r.objetId, r.sujetId from RapportEntreTiers r where type(r) != RapportPrestationImposable and (r.objetId in (:ids) OR r.sujetId in (:ids))";
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
		Long getTiersId(T entity);
	}

	private interface EntitySetSetter<T extends HibernateEntity> {
		void setEntitySet(Tiers tiers, Set<T> set);
	}

	private <T extends HibernateEntity> Map<Long, Set<T>> groupByTiersId(Session session, List<T> entities, TiersIdGetter<T> getter) {

		final Map<Long, Set<T>> map = new HashMap<>();

		for (T e : entities) {
			session.setReadOnly(e, true);
			final Long tiersId = getter.getTiersId(e);
			final Set<T> set = map.computeIfAbsent(tiersId, k -> new HashSet<>());
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
			hqlSujets.append(" and type(r) != ContactImpotSource");
		}
		hqlSujets.append(" and r.objetId in (:ids)");
		return hqlSujets.toString();
	}

	private static String buildHqlForObjets(boolean excludeContactsImpotSource) {
		final StringBuilder hqlSujets = new StringBuilder();
		hqlSujets.append("select r.objetId from RapportEntreTiers r where r.annulationDate is null");
		if (excludeContactsImpotSource) {
			hqlSujets.append(" and type(r) != ContactImpotSource");
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

		final FlushModeType mode = session.getFlushMode();
		if (mode != FlushModeType.COMMIT) {
			LOGGER.warn("Le 'flushMode' de la session hibernate est forcé en MANUAL.");
			session.setFlushMode(FlushModeType.COMMIT); // pour éviter qu'Hibernate essaie de mettre-à-jour les collections des associations one-to-many avec des cascades delete-orphan.
		}
		try {
			return getBatch(ids instanceof Set ? (Set) ids : new HashSet<>(ids), parts, session);
		}
		finally {
			if (mode != FlushModeType.COMMIT) {
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
			
			final TiersIdGetter<IdentificationPersonne> getter = entity -> entity.getPersonnePhysique().getId();
			final EntitySetSetter<IdentificationPersonne> setter = (t, set) -> {
				if (t instanceof PersonnePhysique) {
					((PersonnePhysique) t).setIdentificationsPersonnesForGetBatch(set);
				}
			};

			// on associe les identifications de personnes avec les tiers à la main
			associate(session, identifications, tiers, getter, setter);
		}

		{
			// on charge les identifications d'entreprises en vrac
			final List<IdentificationEntreprise> identifications = queryObjectsByIds("from IdentificationEntreprise as a where a.ctb.id in (:ids)", ids, session);

			final TiersIdGetter<IdentificationEntreprise> getter = entity -> entity.getCtb().getId();
			final EntitySetSetter<IdentificationEntreprise> setter = (t, set) -> {
				if (t instanceof Contribuable) {
					((Contribuable) t).setIdentificationsEntrepriseForGetBatch(set);
				}
			};

			// on associe les identifications d'entreprises avec les tiers à la main
			associate(session, identifications, tiers, getter, setter);
		}

		{
			// on charge les coordonnées financièes en vrac
			final List<CoordonneesFinancieres> coordonnees = queryObjectsByIds("from CoordonneesFinancieres as a where a.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<CoordonneesFinancieres> getter = entity -> entity.getTiers().getId();
			final EntitySetSetter<CoordonneesFinancieres> setter = Tiers::setCoordonneesFinancieres;

			// on associe les identifications d'entreprises avec les tiers à la main
			associate(session, coordonnees, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.ADRESSES)) {

			// on charge toutes les adresses en vrac
			final List<AdresseTiers> adresses = queryObjectsByIds("from AdresseTiers as a where a.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<AdresseTiers> getter = entity -> entity.getTiers().getId();
			final EntitySetSetter<AdresseTiers> setter = Tiers::setAdressesTiers;

			// on associe les adresses avec les tiers à la main
			associate(session, adresses, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.DECLARATIONS)) {

			// on précharge toutes les périodes fiscales pour éviter de les charger une à une depuis les déclarations d'impôt
			final Query periodes = session.createQuery("from PeriodeFiscale");
			periodes.list();

			// on charge toutes les declarations en vrac
			final List<DocumentFiscal> declarations = queryObjectsByIds("from Declaration as d where d.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<DocumentFiscal> getter = entity -> entity.getTiers().getId();
			final EntitySetSetter<DocumentFiscal> setter = Tiers::setDocumentsFiscaux;

			// on associe les déclarations avec les tiers à la main
			associate(session, declarations, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.FORS_FISCAUX)) {
			// on charge tous les fors fiscaux en vrac
			final List<ForFiscal> fors = queryObjectsByIds("from ForFiscal as f where f.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<ForFiscal> getter = entity -> entity.getTiers().getId();
			final EntitySetSetter<ForFiscal> setter = Tiers::setForsFiscaux;

			// on associe les fors fiscaux avec les tiers à la main
			associate(session, fors, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.RAPPORTS_ENTRE_TIERS)) {
			// on charge tous les rapports entre tiers en vrac
			{
				final List<RapportEntreTiers> rapports = queryObjectsByIds("from RapportEntreTiers as r where r.sujetId in (:ids)", ids, session);

				final TiersIdGetter<RapportEntreTiers> getter = RapportEntreTiers::getSujetId;
				final EntitySetSetter<RapportEntreTiers> setter = Tiers::setRapportsSujet;

				// on associe les rapports avec les tiers à la main
				associate(session, rapports, tiers, getter, setter);
			}
			{
				final List<RapportEntreTiers> rapports = queryObjectsByIds("from RapportEntreTiers as r where r.objetId in (:ids)", ids, session);

				final TiersIdGetter<RapportEntreTiers> getter = RapportEntreTiers::getObjetId;
				final EntitySetSetter<RapportEntreTiers> setter = Tiers::setRapportsObjet;

				// on associe les rapports avec les tiers à la main
				associate(session, rapports, tiers, getter, setter);
			}
		}

		if (parts != null && parts.contains(Parts.SITUATIONS_FAMILLE)) {

			// on charge toutes les situations de famille en vrac
			final List<SituationFamille> situations = queryObjectsByIds("from SituationFamille as r where r.contribuable.id in (:ids)", ids, session);

			final TiersIdGetter<SituationFamille> getter = entity -> entity.getContribuable().getId();
			final EntitySetSetter<SituationFamille> setter = (t, set) -> {
				if (t instanceof ContribuableImpositionPersonnesPhysiques) {
					((ContribuableImpositionPersonnesPhysiques) t).setSituationsFamille(set);
				}
			};

			// on associe les situations de famille avec les tiers à la main
			associate(session, situations, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.PERIODICITES)) {

			// on charge toutes les périodicités en vrac
			final List<Periodicite> periodicites = queryObjectsByIds("from Periodicite as p where p.debiteur.id in (:ids)", ids, session);

			final TiersIdGetter<Periodicite> getter = entity -> entity.getDebiteur().getId();
			final EntitySetSetter<Periodicite> setter = (t, set) -> {
				if (t instanceof DebiteurPrestationImposable) {
					((DebiteurPrestationImposable) t).setPeriodicites(set);
				}
			};

			// on associe les périodicités avec les tiers à la main
			associate(session, periodicites, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.ALLEGEMENTS_FISCAUX)) {
			// on charge les allègements fiscaux en vrac
			final List<AllegementFiscal> allegements = queryObjectsByIds("from AllegementFiscal as af where af.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<AllegementFiscal> getter = entity -> entity.getEntreprise().getId();
			final EntitySetSetter<AllegementFiscal> setter = (t, set) -> {
				if (t instanceof Entreprise) {
					((Entreprise) t).setAllegementsFiscaux(set);
				}
			};

			// associations manuelles
			associate(session, allegements, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.ETATS_FISCAUX)) {
			// on charge les états fiscaux en vrac
			final List<EtatEntreprise> etats = queryObjectsByIds("from EtatEntreprise as ee where ee.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<EtatEntreprise> getter = entity -> entity.getEntreprise().getId();
			final EntitySetSetter<EtatEntreprise> setter = (t, set) -> {
				if (t instanceof Entreprise) {
					((Entreprise) t).setEtats(set);
				}
			};

			// associations manuelles
			associate(session, etats, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.REGIMES_FISCAUX)) {
			// on charge les régimes fiscaux en vrac
			final List<RegimeFiscal> regimes = queryObjectsByIds("from RegimeFiscal as rf where rf.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<RegimeFiscal> getter = entity -> entity.getEntreprise().getId();
			final EntitySetSetter<RegimeFiscal> setter = (t, set) -> {
				if (t instanceof Entreprise) {
					((Entreprise) t).setRegimesFiscaux(set);
				}
			};

			// associations manuelles
			associate(session, regimes, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.DONNEES_CIVILES)) {
			// on charge les données civiles en vrac
			final List<DonneeCivileEntreprise> donnees = queryObjectsByIds("from DonneeCivileEntreprise as dce where dce.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<DonneeCivileEntreprise> getter = entity -> entity.getEntreprise().getId();
			final EntitySetSetter<DonneeCivileEntreprise> setter = (t, set) -> {
				if (t instanceof Entreprise) {
					((Entreprise) t).setDonneesCiviles(set);
				}
			};

			// associations manuelles
			associate(session, donnees, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.BOUCLEMENTS)) {
			// on charge les bouclements en vrac
			final List<Bouclement> bouclements = queryObjectsByIds("from Bouclement as b where b.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<Bouclement> getter = entity -> entity.getEntreprise().getId();
			final EntitySetSetter<Bouclement> setter = (t, set) -> {
				if (t instanceof Entreprise) {
					((Entreprise) t).setBouclements(set);
				}
			};

			// associations manuelles
			associate(session, bouclements, tiers, getter, setter);
		}

		if (parts != null && parts.contains(Parts.FLAGS)) {
			// on charge les flags en vrac
			final List<FlagEntreprise> flags = queryObjectsByIds("from FlagEntreprise as fe where fe.entreprise.id in (:ids)", ids, session);

			final TiersIdGetter<FlagEntreprise> getter = entity -> entity.getEntreprise().getId();
			final EntitySetSetter<FlagEntreprise> setter = (t, set) -> {
				if (t instanceof Entreprise) {
					((Entreprise) t).setFlags(set);
				}
			};

			// associations manuelles
			associate(session, flags, tiers, getter, setter);
		}

		// les adresses mandataires
		if (parts != null && parts.contains(Parts.ADRESSES_MANDATAIRES)) {
			// on charge les adresses mandataires en vrac
			final List<AdresseMandataire> adresses = queryObjectsByIds("from AdresseMandataire as am where am.mandant.id in (:ids)", ids, session);

			final TiersIdGetter<AdresseMandataire> getter = entity -> entity.getMandant().getId();
			final EntitySetSetter<AdresseMandataire> setter = (t, set) -> {
				if (t instanceof Contribuable) {
					((Contribuable) t).setAdressesMandataires(set);
				}
			};

			// associations manuelles
			associate(session, adresses, tiers, getter, setter);
		}

		// les étiquettes
		if (parts != null && parts.contains(Parts.ETIQUETTES)) {
			// on charge les étiquettes tiers en vrac
			final List<EtiquetteTiers> etiquettes = queryObjectsByIds("from EtiquetteTiers as etiq where etiq.tiers.id in (:ids)", ids, session);

			final TiersIdGetter<EtiquetteTiers> getter = entity -> entity.getTiers().getId();
			final EntitySetSetter<EtiquetteTiers> setter = Tiers::setEtiquettes;

			// associations manuelles
			associate(session, etiquettes, tiers, getter, setter);
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
		return getAllIdsFor(includeCancelled, types == null ? null : Arrays.asList(types));
	}

	@Override
	public List<Long> getAllIdsFor(boolean includeCancelled, @Nullable Collection<TypeTiers> types) {
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append("where 1=1");

		// condition sur l'état annulé
		if (!includeCancelled) {
			whereClause.append(" and tiers.annulationDate is null");
		}

		// conditions sur les types
		if (types != null && !types.isEmpty()) {
			whereClause.append(" and type(tiers) in (");
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
		final NativeQuery query = session.createNativeQuery(SQL_QUERY_INDIVIDUS_LIES_PARENTE);
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
		return (RapportEntreTiers) saveObjectWithMerge(object);
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

		FlushModeType flushMode = null;
		if (doNotAutoFlush) {
			flushMode = session.getFlushMode();
			session.setFlushMode(FlushModeType.COMMIT);
		}
		else {
			// la requête ci-dessus n'est pas une requête HQL, donc hibernate ne fera pas les
			// flush potentiellement nécessaires... Idéalement, bien-sûr, il faudrait écrire la requête en HQL, mais
			// je n'y arrive pas...
			session.flush();
		}

		final List<Long> list;
		try {
			final NativeQuery query = session.createNativeQuery(sql);
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
	 * Recherche de tiers Entreprise ou Etablissement non annulés, non désactivés.
	 *
	 * @param parametreTiers La classe qui apporte les éléments spécifiques au type de tiers recherché
	 * @param numeroCantonal le numéro cantonal associé au tiers
	 * @return le numéro du tiers
	 */
	@SuppressWarnings({"unchecked"})
	private Long getNumeroTiersByNumeroCantonal(ParametreTiers parametreTiers, final long numeroCantonal) {

		/**
		 * Clone de la méthode ci-dessus pour les tiers PP. A la différence qu'on ne supporte pas la désactivation du flush.
		 */

		final StringBuilder b = new StringBuilder();
		b.append("SELECT T.NUMERO, MAX(FF_A.DATE_FERMETURE) AS DATE_DESACTIVATION, MAX(FF_R.DATE_OUVERTURE) AS DATE_REACTIVATION");
		b.append(" FROM TIERS T");
		b.append(" LEFT OUTER JOIN FOR_FISCAL FF_A ON FF_A.TIERS_ID=T.NUMERO AND FF_A.ANNULATION_DATE IS NULL AND FF_A.MOTIF_FERMETURE='ANNULATION'");
		b.append(" LEFT OUTER JOIN FOR_FISCAL FF_R ON FF_R.TIERS_ID=T.NUMERO AND FF_R.ANNULATION_DATE IS NULL AND FF_R.MOTIF_OUVERTURE='REACTIVATION'");
		b.append(" WHERE T.TIERS_TYPE='");
		b.append(parametreTiers.getTiersType());
		b.append("'");
		b.append(" AND T.");
		b.append(parametreTiers.getIdProperty());
		b.append("=:noCantonal AND T.ANNULATION_DATE IS NULL");
		b.append(" GROUP BY T.NUMERO");
		b.append(" ORDER BY T.NUMERO ASC");
		final String sql = b.toString();

		final Session session = getCurrentSession();

		session.flush();

		final List<Long> list;
		final NativeQuery query = session.createNativeQuery(sql);
		query.setParameter("noCantonal", numeroCantonal);

		// tous les candidats sortent : il faut ensuite filtrer par rapport aux dates d'annulation et de réactivation...
		final List<Object[]> rows = query.list();
		if (rows != null && !rows.isEmpty()) {
			final List<Long> res = new ArrayList<>(rows.size());
			for (Object[] row : rows) {
				final Number enId = (Number) row[0];
				final Number indexDesactivation = (Number) row[1];
				final Number indexReactivation = (Number) row[2];
				if (indexDesactivation == null) {
					res.add(enId.longValue());
				}
				else if (indexReactivation != null && indexReactivation.intValue() > indexDesactivation.intValue()) {
					res.add(enId.longValue());
				}
			}
			list = res;
		}
		else {
			list = null;
		}

		if (list == null || list.isEmpty()) {
			return null;
		}

		if (list.size() > 1) {
			final long[] ids = new long[list.size()];
			for (int i = 0; i < list.size(); ++i) {
				ids[i] = list.get(i);
			}
			parametreTiers.throwMultipleTiersException(numeroCantonal, ids);
		}

		return list.get(0);
	}

	private interface ParametreTiers {
		String getTiersType();
		String getIdProperty();
		void throwMultipleTiersException(long numeroEntrepriseCivile, long[] noEntreprises);
	}

	private static final ParametreTiers PARAMETRE_ENTREPRISE = new ParametreTiers() {
		@Override
		public String getTiersType() {
			return "Entreprise";
		}

		@Override
		public String getIdProperty() {
			return "NUMERO_ENTREPRISE";
		}

		@Override
		public void throwMultipleTiersException(long numeroEntrepriseCivile, long[] noEntreprises) {
			throw new PlusieursEntreprisesAvecMemeNumeroCivilException(numeroEntrepriseCivile, noEntreprises);
		}
	};

	/**
	 * <p>
	 *     Recherche l'Entreprise par son numéro d'entreprise au registre des entreprises qui tient compte des annulations, désactivations et réactivations.
	 * </p>
	 * <p>
	 *     Ne retourne que les tiers Entreprise, et dans le cas où un tiers d'un autre type existerait, null est retourné quand même.
	 * </p>
	 *
	 * @param numeroEntrepriseCivile Le numéro RCEnt
	 * @return L'entreprise correspondant au numéro, ou null si aucune Entreprise n'est trouvée pour ce numéro.
	 */
	public Entreprise getEntrepriseByNoEntrepriseCivile(long numeroEntrepriseCivile) {

		// on passe par le numéro de tiers pour pouvoir factoriser l'algorithme dans la recherche du tiers, en espérant que les performances n'en seront pas trop affectées

		final Long id = getNumeroTiersByNumeroCantonal(PARAMETRE_ENTREPRISE, numeroEntrepriseCivile);
		final Entreprise entreprise;
		if (id != null) {
			entreprise = (Entreprise) get(id); // Par la magie d'Hibernate, si on a un numéro, c'est qu'on a une Entreprise.
		}
		else {
			entreprise = null;
		}
		return entreprise;
	}

	private static final ParametreTiers PARAMETRE_ETABLISSEMENT = new ParametreTiers() {
		@Override
		public String getTiersType() {
			return "Etablissement";
		}

		@Override
		public String getIdProperty() {
			return "NUMERO_ETABLISSEMENT";
		}

		@Override
		public void throwMultipleTiersException(long numeroEntrepriseCivile, long[] noEtablissement) {
			throw new PlusieursEtablissementsAvecMemesNumeroCivilsException(numeroEntrepriseCivile, noEtablissement);
		}
	};

	/**
	 * <p>
	 *     Recherche l'Etablissement par son numéro d'entreprise au registre des entreprises qui tient compte des annulations, désactivations et réactivations.
	 * </p>
	 * <p>
	 *     Ne retourne que les tiers Etablissement, et dans le cas où un tiers d'un autre type existerait, null est retourné quand même.
	 * </p>
	 *
	 * @param numeroEtablissementCivil Le numéro RCEnt
	 * @return L'établissement correspondant au numéro, ou null si aucun Etablissement n'est trouvé pour ce numéro.
	 */
	@Override
	public Etablissement getEtablissementByNumeroEtablissementCivil(long numeroEtablissementCivil) {
		// on passe par le numéro de tiers pour pouvoir factoriser l'algorithme dans la recherche du tiers, en espérant que les performances n'en seront pas trop affectées

		final Long id = getNumeroTiersByNumeroCantonal(PARAMETRE_ETABLISSEMENT, numeroEtablissementCivil);
		final Etablissement etablissement;
		if (id != null) {
			etablissement = (Etablissement) get(id); // Par la magie d'Hibernate, si on a un numéro, c'est qu'on a un Etablissement.
		}
		else {
			etablissement = null;
		}
		return etablissement;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getEntreprisesSansRegimeFiscal() {
		final Session session = getCurrentSession();
		final String q =
				"select e.numero " +
				"from tiers e " +
				"where e.numero not in " +
				"  (" +
				"    select distinct regime.entreprise_id " +
				"    from regime_fiscal regime  " +
				"    where regime.annulation_date is null " +
				"  ) " +
				"and e.tiers_type = 'Entreprise' " +
				"and e.annulation_date is null " +
				"order by e.numero";

		final NativeQuery query = session.createNativeQuery(q);
		//noinspection unchecked
		final List<? extends Number> list = query.list();
		return list.stream()
				.map(Number::longValue)
				.collect(Collectors.toList());
	}

	@Override
	public List<Long> getEntreprisesAvecRegimeFiscalAt(@NotNull String code, @NotNull RegDate date) {
		final Query query = getCurrentSession().createQuery("select distinct entreprise.id from RegimeFiscal " +
				                                                    "where annulationDate is null " +
				                                                    "and code = :code " +
				                                                    "and (dateDebut is null or dateDebut <= :date) " +
				                                                    "and (dateFin is null or dateFin >= :date) " +
				                                                    "order by entreprise.id");
		query.setParameter("code", code);
		query.setParameter("date", date);

		//noinspection unchecked
		final List<? extends Number> list = query.list();
		return list.stream()
				.map(Number::longValue)
				.collect(Collectors.toList());
	}

	@Override
	public void updateOids(final Map<Long, Integer> tiersOidsMapping) {

		if (tiersOidsMapping == null || tiersOidsMapping.isEmpty()) {
			return;
		}

		// [UNIREG-1024] On met-à-jour les tâches encore ouvertes, à l'exception des tâches de contrôle de dossier
		final Session session = getCurrentSession();

		final FlushModeType mode = session.getFlushMode();
		session.setFlushMode(FlushModeType.COMMIT);
		try {
			// met-à-jour les tiers concernés
			final Query update = session.createNativeQuery("update TIERS set OID = :oid where NUMERO = :id");
			final Query updateForNullValue = session.createNativeQuery("update TIERS set OID = null where NUMERO = :id");
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
		final FlushModeType flushMode = doNotAutoFlush ? FlushModeType.COMMIT : null;
		final List<CollectiviteAdministrative> list = find(query, buildNamedParameters(Pair.of("noDistrict", numeroDistrict)), flushMode);
		if (list == null || list.isEmpty()) {
			return null;
		}
		if (list.size() != 1) { // une seule collectivité administrative de regroupement par district
			throw new IllegalArgumentException();
		}
		return list.get(0);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(int numeroRegion) {
		final String query = "from CollectiviteAdministrative col where col.identifiantRegionFiscale = :noRegion";
		final List<CollectiviteAdministrative> list = find(query, buildNamedParameters(Pair.of("noRegion", numeroRegion)), null);
		if (list == null || list.isEmpty()) {
			return null;
		}
		if (list.size() != 1) { // une seule collectivité administrative de regroupement par Region
			throw new IllegalArgumentException();
		}
		return list.get(0);
	}

	@Override
	@SuppressWarnings({"UnnecessaryBoxing"})
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {

		final String query = "from CollectiviteAdministrative col where col.numeroCollectiviteAdministrative = :noTechnique";
		final FlushModeType mode = (doNotAutoFlush ? FlushModeType.COMMIT : null);
		final List<CollectiviteAdministrative> list = find(query, buildNamedParameters(Pair.of("noTechnique", numeroTechnique)), mode);
		if (list == null || list.isEmpty()) {
			return null;
		}
		if (list.size() != 1) {     // le numéro de collectivité administrative est défini comme 'unique' sur la base
			throw new IllegalArgumentException();
		}
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
	 * @see ch.vd.unireg.tiers.TiersDAO#getDebiteurPrestationImposableByNumero(java.lang.Long)
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
		final FlushModeType mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushModeType.COMMIT);
			list = crit.list();
		}
		finally {
			session.setFlushMode(mode);
		}

		if (list.isEmpty()) {
			return null;
		}
		else {
			if (list.size() != 1) {
				throw new IllegalArgumentException();
			}
			return list.get(0);
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Contribuable getContribuable(final DebiteurPrestationImposable debiteur) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("select t from ContactImpotSource r, Tiers t where r.objetId = :dpiId and r.sujetId = t.id and r.annulationDate is null");
		query.setParameter("dpiId", debiteur.getId());

		final FlushModeType mode = session.getFlushMode();
		session.setFlushMode(FlushModeType.COMMIT);
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
	 * Juste une façade devant la méthode {@link BaseDAOImpl#save(Object)} pour le typage en T
	 * @param tiers tiers à sauvegarder
	 * @param <T> type du tiers
	 * @return le tiers après sauvegarde
	 */
	private <T extends Tiers> T saveTiers(T tiers) {
		//noinspection unchecked
		return (T) save(tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		return AddAndSaveHelper.addAndSave(tiers, forFiscal, this::saveTiers, new ForFiscalAccessor<>());
	}

	@Override
	public DecisionAci addAndSave(Contribuable tiers, DecisionAci decisionAci) {
		return AddAndSaveHelper.addAndSave(tiers, decisionAci, this::saveTiers, DECISION_ACI_ACCESSOR);
	}

	private static final class DeclarationAccessor<T extends Declaration> implements AddAndSaveHelper.EntityAccessor<Tiers, T> {
		@Override
		public Collection<Declaration> getEntities(Tiers tiers) {
			return tiers.getDeclarations();
		}

		@Override
		public void addEntity(Tiers tiers, T d) {
			tiers.addDeclaration(d);
		}

		@Override
		public void assertEquals(T d1, T d2) {
			if (d1.getDateDebut() != d2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (d1.getDateFin() != d2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public <T extends Declaration> T addAndSave(Tiers tiers, T declaration) {
		return AddAndSaveHelper.addAndSave(tiers, declaration, this::saveTiers, new DeclarationAccessor<>());
	}

	private static final AddAndSaveHelper.EntityAccessor<DebiteurPrestationImposable, Periodicite> PERIODICITE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<DebiteurPrestationImposable, Periodicite>() {
		@Override
		public Collection<Periodicite> getEntities(DebiteurPrestationImposable dpi) {
			return dpi.getPeriodicites();
		}

		@Override
		public void addEntity(DebiteurPrestationImposable dpi, Periodicite p) {
			dpi.addPeriodicite(p);
		}

		@Override
		public void assertEquals(Periodicite p1, Periodicite p2) {
			if (p1.getDateDebut() != p2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (p1.getDateFin() != p2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite) {
		return AddAndSaveHelper.addAndSave(debiteur, periodicite, this::saveTiers, PERIODICITE_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<ContribuableImpositionPersonnesPhysiques, SituationFamille> SITUATION_FAMILLE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<ContribuableImpositionPersonnesPhysiques, SituationFamille>() {
		@Override
		public Collection<SituationFamille> getEntities(ContribuableImpositionPersonnesPhysiques ctb) {
			return ctb.getSituationsFamille();
		}

		@Override
		public void addEntity(ContribuableImpositionPersonnesPhysiques ctb, SituationFamille entity) {
			ctb.addSituationFamille(entity);
		}

		@Override
		public void assertEquals(SituationFamille entity1, SituationFamille entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SituationFamille addAndSave(ContribuableImpositionPersonnesPhysiques contribuable, SituationFamille situation) {
		return AddAndSaveHelper.addAndSave(contribuable, situation, this::saveTiers, SITUATION_FAMILLE_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Tiers, AdresseTiers> ADRESSE_TIERS_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Tiers, AdresseTiers>() {
		@Override
		public Collection<AdresseTiers> getEntities(Tiers tiers) {
			return tiers.getAdressesTiers();
		}

		@Override
		public void addEntity(Tiers tiers, AdresseTiers entity) {
			tiers.addAdresseTiers(entity);
		}

		@Override
		public void assertEquals(AdresseTiers entity1, AdresseTiers entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse) {
		return AddAndSaveHelper.addAndSave(tiers, adresse, this::saveTiers, ADRESSE_TIERS_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Contribuable, AdresseMandataire> ADRESSE_MANDATAIRE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Contribuable, AdresseMandataire>() {
		@Override
		public Collection<? extends HibernateEntity> getEntities(Contribuable ctb) {
			return ctb.getAdressesMandataires();
		}

		@Override
		public void addEntity(Contribuable ctb, AdresseMandataire entity) {
			ctb.addAdresseMandataire(entity);
		}

		@Override
		public void assertEquals(AdresseMandataire entity1, AdresseMandataire entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public AdresseMandataire addAndSave(Contribuable contribuable, AdresseMandataire adresse) {
		return AddAndSaveHelper.addAndSave(contribuable, adresse, this::saveTiers, ADRESSE_MANDATAIRE_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<PersonnePhysique, IdentificationPersonne> IDENTIFICATION_PERSONNE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<PersonnePhysique, IdentificationPersonne>() {
		@Override
		public Collection<IdentificationPersonne> getEntities(PersonnePhysique pp) {
			return pp.getIdentificationsPersonnes();
		}

		@Override
		public void addEntity(PersonnePhysique pp, IdentificationPersonne entity) {
			pp.addIdentificationPersonne(entity);
		}

		@Override
		public void assertEquals(IdentificationPersonne entity1, IdentificationPersonne entity2) {
			if (entity1.getCategorieIdentifiant() != entity2.getCategorieIdentifiant()) {
				throw new IllegalArgumentException();
			}
			if (!Objects.equals(entity1.getIdentifiant(), entity2.getIdentifiant())) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident) {
		return AddAndSaveHelper.addAndSave(pp, ident, this::saveTiers, IDENTIFICATION_PERSONNE_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Contribuable, IdentificationEntreprise> IDENTIFICATION_ENTREPRISE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Contribuable, IdentificationEntreprise>() {
		@Override
		public Collection<IdentificationEntreprise> getEntities(Contribuable ctb) {
			return ctb.getIdentificationsEntreprise();
		}

		@Override
		public void addEntity(Contribuable ctb, IdentificationEntreprise entity) {
			ctb.addIdentificationEntreprise(entity);
		}

		@Override
		public void assertEquals(IdentificationEntreprise entity1, IdentificationEntreprise entity2) {
			if (!Objects.equals(entity1.getNumeroIde(), entity2.getNumeroIde())) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public IdentificationEntreprise addAndSave(Contribuable ctb, IdentificationEntreprise ident) {
		return AddAndSaveHelper.addAndSave(ctb, ident, this::saveTiers, IDENTIFICATION_ENTREPRISE_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Etablissement, DomicileEtablissement> DOMICILE_ETABLISSEMENT_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Etablissement, DomicileEtablissement>() {
		@Override
		public Collection<DomicileEtablissement> getEntities(Etablissement tiers) {
			return tiers.getDomiciles();
		}

		@Override
		public void addEntity(Etablissement etablissement, DomicileEtablissement domicile) {
			etablissement.addDomicile(domicile);
		}

		@Override
		public void assertEquals(DomicileEtablissement entity1, DomicileEtablissement entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getTypeAutoriteFiscale() != entity2.getTypeAutoriteFiscale()) {
				throw new IllegalArgumentException();
			}
			if (!Objects.equals(entity1.getNumeroOfsAutoriteFiscale(), entity2.getNumeroOfsAutoriteFiscale())) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public DomicileEtablissement addAndSave(Etablissement etb, DomicileEtablissement domicile) {
		return AddAndSaveHelper.addAndSave(etb, domicile, this::saveTiers, DOMICILE_ETABLISSEMENT_ACCESSOR);
	}

	private static final class AllegementFiscalAccessor<T extends AllegementFiscal> implements AddAndSaveHelper.EntityAccessor<Entreprise, T> {
		@Override
		public Collection<T> getEntities(Entreprise tiers) {
			//noinspection unchecked
			return (Collection<T>) tiers.getAllegementsFiscaux();
		}

		@Override
		public void addEntity(Entreprise tiers, T entity) {
			tiers.addAllegementFiscal(entity);
		}

		@Override
		public void assertEquals(T entity1, T entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getTypeCollectivite() != entity2.getTypeCollectivite()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getTypeImpot() != entity2.getTypeImpot()) {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public <T extends AllegementFiscal> T addAndSave(Entreprise entreprise, T allegement) {
		return AddAndSaveHelper.addAndSave(entreprise, allegement, this::saveTiers, new AllegementFiscalAccessor<>());
	}

	private static final AddAndSaveHelper.EntityAccessor<Entreprise, DonneeCivileEntreprise> DONNEE_CIVILE_FISCAL_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Entreprise, DonneeCivileEntreprise>() {
		@Override
		public Collection<DonneeCivileEntreprise> getEntities(Entreprise tiers) {
			return tiers.getDonneesCiviles();
		}

		@Override
		public void addEntity(Entreprise tiers, DonneeCivileEntreprise entity) {
			tiers.addDonneeCivile(entity);
		}

		@Override
		public void assertEquals(DonneeCivileEntreprise entity1, DonneeCivileEntreprise entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
			if (RaisonSocialeFiscaleEntreprise.class.isAssignableFrom(entity1.getClass())) {
				RaisonSocialeFiscaleEntreprise raison1 = (RaisonSocialeFiscaleEntreprise) entity1;
				RaisonSocialeFiscaleEntreprise raison2 = (RaisonSocialeFiscaleEntreprise) entity2;
				if (!Objects.equals(raison1.getRaisonSociale(), raison2.getRaisonSociale())) {
					throw new IllegalArgumentException();
				}
			}
			else if (FormeJuridiqueFiscaleEntreprise.class.isAssignableFrom(entity1.getClass())) {
				FormeJuridiqueFiscaleEntreprise jur1 = (FormeJuridiqueFiscaleEntreprise) entity1;
				FormeJuridiqueFiscaleEntreprise jur2 = (FormeJuridiqueFiscaleEntreprise) entity2;
				if (jur1.getFormeJuridique() != jur2.getFormeJuridique()) {
					throw new IllegalArgumentException();
				}
			}
			else if (CapitalFiscalEntreprise.class.isAssignableFrom(entity1.getClass())) {
				CapitalFiscalEntreprise cap1 = (CapitalFiscalEntreprise) entity1;
				CapitalFiscalEntreprise cap2 = (CapitalFiscalEntreprise) entity2;
				if (!Objects.equals(cap1.getMontant().getMontant(), cap2.getMontant().getMontant())) {
					throw new IllegalArgumentException();
				}
				if (!Objects.equals(cap1.getMontant().getMonnaie(), cap2.getMontant().getMonnaie())) {
					throw new IllegalArgumentException();
				}
			}
		}
	};

	@Override
	public DonneeCivileEntreprise addAndSave(Entreprise entreprise, DonneeCivileEntreprise donneeCivile) {
		return AddAndSaveHelper.addAndSave(entreprise, donneeCivile, this::saveTiers, DONNEE_CIVILE_FISCAL_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Entreprise, Bouclement> BOUCLEMENT_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Entreprise, Bouclement>() {
		@Override
		public Collection<Bouclement> getEntities(Entreprise tiers) {
			return tiers.getBouclements();
		}

		@Override
		public void addEntity(Entreprise tiers, Bouclement entity) {
			tiers.addBouclement(entity);
		}

		@Override
		public void assertEquals(Bouclement entity1, Bouclement entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getAncrage() != entity2.getAncrage()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getPeriodeMois() != entity2.getPeriodeMois()) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public Bouclement addAndSave(Entreprise entreprise, Bouclement bouclement) {
		return AddAndSaveHelper.addAndSave(entreprise, bouclement, this::saveTiers, BOUCLEMENT_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Entreprise, RegimeFiscal> REGIME_FISCAL_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Entreprise, RegimeFiscal>() {
		@Override
		public Collection<RegimeFiscal> getEntities(Entreprise tiers) {
			return tiers.getRegimesFiscaux();
		}

		@Override
		public void addEntity(Entreprise tiers, RegimeFiscal entity) {
			tiers.addRegimeFiscal(entity);
		}

		@Override
		public void assertEquals(RegimeFiscal entity1, RegimeFiscal entity2) {
			if (entity1.getPortee() != entity2.getPortee()) {
				throw new IllegalArgumentException();
			}
			if (!entity1.getCode().equals(entity2.getCode())) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public RegimeFiscal addAndSave(Entreprise entreprise, RegimeFiscal regime) {
		return AddAndSaveHelper.addAndSave(entreprise, regime, this::saveTiers, REGIME_FISCAL_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Entreprise, EtatEntreprise> ETAT_ENTREPRISE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Entreprise, EtatEntreprise>() {
		@Override
		public Collection<EtatEntreprise> getEntities(Entreprise tiers) {
			return tiers.getEtats();
		}

		@Override
		public void addEntity(Entreprise tiers, EtatEntreprise entity) {
			tiers.addEtat(entity);
		}

		@Override
		public void assertEquals(EtatEntreprise entity1, EtatEntreprise entity2) {
			if (entity1.getDateObtention() != entity2.getDateObtention()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getType() != entity2.getType()) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public EtatEntreprise addAndSave(Entreprise entreprise, EtatEntreprise etat) {
		return AddAndSaveHelper.addAndSave(entreprise, etat, this::saveTiers, ETAT_ENTREPRISE_ACCESSOR);
	}

	private static final AddAndSaveHelper.EntityAccessor<Entreprise, FlagEntreprise> FLAG_ENTREPRISE_ACCESSOR = new AddAndSaveHelper.EntityAccessor<Entreprise, FlagEntreprise>() {
		@Override
		public Collection<FlagEntreprise> getEntities(Entreprise tiers) {
			return tiers.getFlags();
		}

		@Override
		public void addEntity(Entreprise tiers, FlagEntreprise entity) {
			tiers.addFlag(entity);
		}

		@Override
		public void assertEquals(FlagEntreprise entity1, FlagEntreprise entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getType() != entity2.getType()) {
				throw new IllegalArgumentException();
			}
		}
	};

	@Override
	public FlagEntreprise addAndSave(Entreprise entreprise, FlagEntreprise flag) {
		return AddAndSaveHelper.addAndSave(entreprise, flag, this::saveTiers, FLAG_ENTREPRISE_ACCESSOR);
	}

	private static final class AutreDocumentFiscalAccessor<T extends AutreDocumentFiscal> implements AddAndSaveHelper.EntityAccessor<Entreprise, T> {
		@Override
		public Collection<AutreDocumentFiscal> getEntities(Entreprise tiers) {
			return tiers.getAutresDocumentsFiscaux();
		}

		@Override
		public void addEntity(Entreprise tiers, T entity) {
			tiers.addAutreDocumentFiscal(entity);
		}

		@Override
		public void assertEquals(T entity1, T entity2) {
			if (entity1.getDateEnvoi() != entity2.getDateEnvoi()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getEtat() != entity2.getEtat()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getClass() != entity2.getClass()) {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public <T extends AutreDocumentFiscal> T addAndSave(Entreprise entreprise, T document) {
		return AddAndSaveHelper.addAndSave(entreprise, document, this::saveTiers, new AutreDocumentFiscalAccessor<>());
	}

	private static final class AllegementFoncierAccessor<T extends AllegementFoncier> implements AddAndSaveHelper.EntityAccessor<ContribuableImpositionPersonnesMorales, T> {
		@Override
		public Collection<? extends HibernateEntity> getEntities(ContribuableImpositionPersonnesMorales tiers) {
			return tiers.getAllegementsFonciers();
		}

		@Override
		public void addEntity(ContribuableImpositionPersonnesMorales tiers, T entity) {
			tiers.addAllegementFoncier(entity);
		}

		@Override
		public void assertEquals(T entity1, T entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getImmeuble() != entity2.getImmeuble()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getClass() != entity2.getClass()) {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public <T extends AllegementFoncier> T addAndSave(ContribuableImpositionPersonnesMorales contribuable, T allegementFoncier) {
		return AddAndSaveHelper.addAndSave(contribuable, allegementFoncier, this::saveTiers, new AllegementFoncierAccessor<>());
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
						"FROM DOCUMENT_FISCAL DI                                                      " +
						"JOIN ETAT_DOCUMENT_FISCAL ED ON ED.DOCUMENT_FISCAL_ID = DI.ID                    " +
						"JOIN FOR_FISCAL FF ON FF.TIERS_ID=DI.TIERS_ID                            " +
						"AND FF.FOR_TYPE != 'ForDebiteurPrestationImposable'                      " +
						"AND ED.LOG_MDATE >= :debut                                               " +
						"AND ED.LOG_MDATE <= :fin                                                 " +
						"AND ED.TYPE IN ('EMIS', 'ECHU')                                        " +
						"AND DI.DOCUMENT_TYPE IN ('DI', 'DIPM', 'QSNC', 'LR')                     " +
						"ORDER BY CTB_ID                                                          ";

		final Session session = getCurrentSession();
		final NativeQuery queryObject = session.createNativeQuery(RequeteContribuablesModifies);
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
		final FlushModeType mode = session.getFlushMode();
		session.setFlushMode(FlushModeType.COMMIT);
		try {
			final String sql = "UPDATE TIERS SET BLOC_REMB_AUTO=:newFlag, LOG_MDATE=:now, LOG_MUSER=:user WHERE NUMERO=:numero AND (BLOC_REMB_AUTO IS NULL OR BLOC_REMB_AUTO != :newFlag)";
			final NativeQuery query = getCurrentSession().createNativeQuery(sql);
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

	@Override
	public void setDirtyFlag(@Nullable Collection<Long> ids, boolean flag, @NotNull Session session) {

		if (ids == null || ids.isEmpty()) {
			return;
		}

		final NativeQuery query = session.createNativeQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(flag) + " where NUMERO in (:ids)");
		query.setParameterList("ids", ids);
		query.executeUpdate();

		if (!flag) {
			// [UNIREG-1979] On remet aussi à zéro tous les tiers dont la date 'reindex_on' est atteinte aujourd'hui
			final NativeQuery q = session.createNativeQuery("update TIERS set REINDEX_ON = null where REINDEX_ON is not null and REINDEX_ON <= :today and NUMERO in (:ids)");
			q.setParameter("today", RegDate.get().index());
			q.setParameterList("ids", ids);
			q.executeUpdate();
		}
	}

	private static class ForFiscalAccessor<T extends ForFiscal> implements AddAndSaveHelper.EntityAccessor<Tiers, T> {
		@Override
		public Collection<ForFiscal> getEntities(Tiers tiers) {
			return tiers.getForsFiscaux();
		}

		@Override
		public void addEntity(Tiers tiers, T entity) {
			tiers.addForFiscal(entity);
		}

		@Override
		public void assertEquals(T entity1, T entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	}

	private static class DecisionAciAccessor implements AddAndSaveHelper.EntityAccessor<Contribuable, DecisionAci> {
		@Override
		public Collection<DecisionAci> getEntities(Contribuable ctb) {
			return ctb.getDecisionsAci();
		}

		@Override
		public void addEntity(Contribuable ctb, DecisionAci decisionAci) {
			ctb.addDecisionAci(decisionAci);
		}

		@Override
		public void assertEquals(DecisionAci entity1, DecisionAci entity2) {
			if (!Objects.equals(entity1.getNumeroOfsAutoriteFiscale(), entity2.getNumeroOfsAutoriteFiscale())) {
				throw new IllegalArgumentException();
			}
			if (entity1.getTypeAutoriteFiscale() != entity2.getTypeAutoriteFiscale()) {
				throw new IllegalArgumentException();
			}
			if (!Objects.equals(entity1.getContribuable().getNumero(), entity2.getContribuable().getNumero())) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateDebut() != entity2.getDateDebut()) {
				throw new IllegalArgumentException();
			}
			if (entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	}
}
