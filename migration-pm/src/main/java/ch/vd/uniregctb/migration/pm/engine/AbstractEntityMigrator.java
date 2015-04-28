package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.migration.pm.MigrationResult;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.adresse.StreetData;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.AdresseAvecRue;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractEntityMigrator<T extends RegpmEntity> implements EntityMigrator<T> {

	protected SessionFactory uniregSessionFactory;
	protected StreetDataMigrator streetDataMigrator;
	protected TiersDAO tiersDAO;

	protected static final BinaryOperator<List<DateRange>> DATE_RANGE_LIST_MERGER = (l1, l2) -> {
		final List<DateRange> liste = Stream.concat(l1.stream(), l2.stream())
				.sorted(new DateRangeComparator<>())
				.collect(Collectors.toList());
		return DateRangeHelper.merge(liste);
	};

	protected static final BinaryOperator<Map<RegpmCommune, List<DateRange>>> DATE_RANGE_MAP_MERGER =
			(m1, m2) -> Stream.concat(m1.entrySet().stream(), m2.entrySet().stream())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, DATE_RANGE_LIST_MERGER));

	/**
	 * Remplit l'ensemble donné en paramètre avec les communes concernées par l'immeuble, en évitant la récursivité infinie
	 * @param immeuble immeuble à inspecter
	 * @param idsImmeublesDejaVus ensemble des identifiants des immeubles déjà rencontrés (pour bloquer la récursivité infinie)
	 */
	@NotNull
	private static Set<RegpmCommune> findCommunes(RegpmImmeuble immeuble, Set<Long> idsImmeublesDejaVus) {
		// on est déjà passé ici ?
		if (immeuble == null || idsImmeublesDejaVus.contains(immeuble.getId())) {
			return Collections.emptySet();
		}

		// comme ça on ne repassera plus sur cet immeuble...
		idsImmeublesDejaVus.add(immeuble.getId());

		final Set<RegpmCommune> communes = new HashSet<>();
		if (immeuble.getCommune() != null) {
			communes.add(immeuble.getCommune());
		}
		communes.addAll(findCommunes(immeuble.getParcelle(), idsImmeublesDejaVus));
		return communes;
	}

	@NotNull
	protected static Set<RegpmCommune> findCommunes(RegpmImmeuble immeuble) {
		return findCommunes(immeuble, new HashSet<>());
	}

	/**
	 * Méthode utilitaire d'agrégation de listes de couples (commune / plage de dates) en une map
	 * @param elements éléments dont on extrait, individuellement, les couples (commune / plage de dates)
	 * @param couvertureIndividuelle la fonction de conversion entre un élément et une liste de couples (commune / plage de dates)
	 * @param <T> le type des éléments source
	 * @return une map, indexée par commune, des plages de dates couvertes (ces plages sont fusionnées et ordonnées)
	 */
	private static <T> Map<RegpmCommune, List<DateRange>> couverture(Collection<T> elements, Function<T, Stream<Pair<RegpmCommune, DateRange>>> couvertureIndividuelle) {
		return elements.stream()
				.map(couvertureIndividuelle)
				.flatMap(Function.identity())
				.collect(Collectors.toMap(Pair::getKey,
				                          pair -> Collections.singletonList(pair.getValue()),
				                          DATE_RANGE_LIST_MERGER));
	}

	/**
	 * Extraction des plages de dates de couverture de communes depuis un rattachement propriétaire
	 * @param rattachement le rattachement propriétaire
	 * @return stream des communes couvertes avec les dates de couverture
	 */
	protected static Stream<Pair<RegpmCommune, DateRange>> couvertureDepuisRattachementProprietaire(RegpmRattachementProprietaire rattachement) {
		return findCommunes(rattachement.getImmeuble()).stream().map(commune -> Pair.of(commune, rattachement));
	}

	/**
	 * Extraction des plages de dates de couverture de communes depuis un ensemble de rattachements propriétaire
	 * @param rattachements les rattachements propriétaire
	 * @return une map, indexée par commune, des plages de dates couvertes (ces plages sont fusionnées et ordonnées)
	 */
	protected static Map<RegpmCommune, List<DateRange>> couvertureDepuisRattachementsProprietaires(Collection<RegpmRattachementProprietaire> rattachements) {
		return couverture(rattachements, AbstractEntityMigrator::couvertureDepuisRattachementProprietaire);
	}

	/**
	 * Extraction des plages de dates de couverture de commune depuis une appartenance à un groupe propriétaire (le problème
	 * est de tenir compte à la fois des dates d'appartenance au groupe et des dates des rattachements propriétaire du groupe)
	 * @param appartenance l'appartenance à un groupe propriétaire
	 * @return stream des communes couvertes avec les dates de couverture
	 */
	protected static Stream<Pair<RegpmCommune, DateRange>> couvertureDepuisAppartenanceGroupeProprietaire(RegpmAppartenanceGroupeProprietaire appartenance) {
		final DateRange rangeAppartenance = appartenance;
		return appartenance.getGroupeProprietaire().getRattachementsProprietaires().stream()
				.map(AbstractEntityMigrator::couvertureDepuisRattachementProprietaire)
				.flatMap(Function.identity())
				.filter(pair -> DateRangeHelper.intersect(rangeAppartenance, pair.getValue()))
				.map(pair -> Pair.of(pair.getKey(), DateRangeHelper.intersection(rangeAppartenance, pair.getValue())));
	}

	/**
	 * Extraction des plages de dates de couverture de commune depuis une collection d'appartenances à des groupes propriétaire
	 * @param appartenances les appartenances aux groupes
	 * @return une map, indexée par commune, des plages de dates couvertes (ces plages sont fusionnées et ordonnées)
	 */
	protected static Map<RegpmCommune, List<DateRange>> couvertureDepuisAppartenancesGroupeProprietaire(Collection<RegpmAppartenanceGroupeProprietaire> appartenances) {
		return couverture(appartenances, AbstractEntityMigrator::couvertureDepuisAppartenanceGroupeProprietaire);
	}

	public void setUniregSessionFactory(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	public void setStreetDataMigrator(StreetDataMigrator streetDataMigrator) {
		this.streetDataMigrator = streetDataMigrator;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	/**
	 * @param entity entité à migrer
	 * @param mr récipiendaire de messages à logguer
	 * @param linkCollector collecteur de liens entre entités (seront résolus à la fin de la migration)
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	@Override
	public final void migrate(T entity, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		final MigrationResultProduction localMr = mr.withMessagePrefix(getMessagePrefix(entity));
		doMigrate(entity, localMr, linkCollector, idMapper);
	}

	/**
	 * Permet d'initialiser des structures de données dans le résultat
	 * @param mr structure à initialiser
	 */
	public void initMigrationResult(MigrationResult mr) {
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByUniregIdSupplier(long id) {
		return () -> getEntityFromDb(Entreprise.class, id);
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByRegpmIdSupplier(IdMapper idMapper, long id) {
		return () -> getEntityFromDb(Entreprise.class, idMapper.getIdUniregEntreprise(id));
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByUniregIdSupplier(long id) {
		return () -> getEntityFromDb(Etablissement.class, id);
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByRegpmIdSupplier(IdMapper idMapper, long id) {
		return () -> getEntityFromDb(Etablissement.class, idMapper.getIdUniregEtablissement(id));
	}

	@NotNull
	protected final Supplier<PersonnePhysique> getIndividuByRegpmIdSupplier(IdMapper idMapper, long id) {
		return () -> getEntityFromDb(PersonnePhysique.class, idMapper.getIdUniregIndividu(id));
	}

	/**
	 * Le côté polymorphique d'une relation est en général exprimé dans RegPM par plusieurs liens distincts, dont un seul est non-vide. Dans Unireg,
	 * il n'y a en général qu'un seul lien vers une entité à caractère polymorphique... Cette méthode permet donc de transcrire une façon de faire
	 * dans l'autre.
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param entrepriseSupplier accès au lien vers une entreprise
	 * @param etablissementSupplier accès au lien vers un établissement
	 * @param individuSupplier accès au lien vers un individu
	 * @return un accès vers l'entité Unireg correspondant à l'entité (entreprise, établissement ou individu) de RegPM liée&nbsp;; cet accès n'est pas résolvable immédiatement
	 * mais est plutôt destiné à être résolu une fois toutes les entités d'un graphe migrées&nbsp;; la valeur retournée est nulle dans le cas où aucun des accès en entrée n'a
	 * fourni une entité.
	 */
	@Nullable
	protected KeyedSupplier<? extends Contribuable> getPolymorphicSupplier(IdMapper idMapper,
	                                                                @Nullable Supplier<RegpmEntreprise> entrepriseSupplier,
	                                                                @Nullable Supplier<RegpmEtablissement> etablissementSupplier,
	                                                                @Nullable Supplier<RegpmIndividu> individuSupplier) {

		final RegpmEntreprise entreprise = entrepriseSupplier != null ? entrepriseSupplier.get() : null;
		if (entreprise != null) {
			return new KeyedSupplier<>(EntityKey.of(entreprise), getEntrepriseByRegpmIdSupplier(idMapper, entreprise.getId()));
		}

		final RegpmEtablissement etablissement = etablissementSupplier != null ? etablissementSupplier.get() : null;
		if (etablissement != null) {
			return new KeyedSupplier<>(EntityKey.of(etablissement), getEtablissementByRegpmIdSupplier(idMapper, etablissement.getId()));
		}

		final RegpmIndividu individu = individuSupplier != null ? individuSupplier.get() : null;
		if (individu != null) {
			return new KeyedSupplier<>(EntityKey.of(individu), getIndividuByRegpmIdSupplier(idMapper, individu.getId()));
		}

		return null;
	}

	/**
	 * Appelé par qui le veut bien à la création d'une nouvelle entité migrée, afin d'initialiser les valeurs des colonnes LOG_CDATE, LOG_CUSER, LOG_MDATE et LOG_MUSER
	 * @param src entité de RegPM qui fournit ces informations
	 * @param dest entité d'Unireg qui les utilise
	 */
	protected static void copyCreationMutation(RegpmEntity src, HibernateEntity dest) {
		dest.setLogCreationUser(src.getLastMutationOperator());
		dest.setLogCreationDate(src.getLastMutationTimestamp());
		dest.setLogModifUser(src.getLastMutationOperator());
		dest.setLogModifDate(src.getLastMutationTimestamp());
	}

	/**
	 * @param clazz la classe de l'entité à récupérer depuis la base de données Unireg
	 * @param id identifiant de l'entité à récupérer
	 * @param <E> type de l'entité récupérée
	 * @return l'entité avec l'identifiant donné
	 */
	@Nullable
	protected final <E extends HibernateEntity> E getEntityFromDb(Class<E> clazz, long id) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().get(clazz, id);
	}

	/**
	 * Méthode qui permet d'aller chercher des entités dans la base de données Unireg sans forcément connaître leur identifiant
	 * @param clazz classe des entités visées
	 * @param criteria critères (attribut / valeur)
	 * @param <E> type des entitées visées
	 * @return la liste des entités trouvées
	 */
	protected final <E extends HibernateEntity> List<E> getEntitiesFromDb(Class<E> clazz, Map<String, ?> criteria) {
		final Criteria c = uniregSessionFactory.getCurrentSession().createCriteria(clazz);
		if (criteria != null) {
			criteria.entrySet().stream().forEach(entry -> c.add(Restrictions.eq(entry.getKey(), entry.getValue())));
		}
		//noinspection unchecked
		return c.list();
	}

	/**
	 * @param entity l'entité à sauvegarder en base de données Unireg
	 * @param <E> type de l'entité
	 * @return l'entité après sauvegarde
	 */
	protected final <E extends HibernateEntity> E saveEntityToDb(E entity) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().merge(entity);
	}

	/**
	 * Réel job de migration
	 * @param entity entité à migrer
	 * @param mr récipiendaire de messages à logguer
	 * @param linkCollector collecteur de liens entre entités (seront résolus à la fin de la migration)
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	protected abstract void doMigrate(T entity, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapper idMapper);

	/**
	 * Renvoie l'éventuel préfixe à utiliser pour tous les messages enregistrés dans les MigrationResults
	 * @param entity entité en cours de migration
	 * @return le préfixe pour cette entité
	 */
	@Nullable
	protected abstract String getMessagePrefix(T entity);

	/**
	 * Supplier qui connaît aussi le type d'objet
	 * @param <T> type d'objet fourni
	 */
	public static class KeyedSupplier<T> implements Supplier<T> {

		private final EntityKey key;
		private final Supplier<T> target;

		public KeyedSupplier(@NotNull EntityKey key, @NotNull Supplier<T> target) {
			this.key = key;
			this.target = target;
		}

		@Override
		public T get() {
			return target.get();
		}

		public EntityKey getKey() {
			return key;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyedSupplier<?> that = (KeyedSupplier<?>) o;
			return key.equals(that.key);
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}
	}

	/**
	 * Construit une adresse tiers (= surcharge d'adresse civile) à partir des données fournie.<br/>
	 * Les champs {@link AdresseTiers#usage}, {@link AdresseTiers#tiers} et {@link AdresseTiers#id} ne sont pas remplis.<br/>
	 * L'entité retournée n'est rattachée à aucune session Hibernate.
	 * @param source source des données de l'adresse
	 * @param mr collecteurs de messages de suivi
	 * @param complement supplier pour la valeur du "complément"
	 * @param permanente <code>true</code> si l'adresse doit être flaggée comme "permanente"
	 * @return une adresse presque prête à persister
	 */
	protected AdresseTiers buildAdresse(AdresseAvecRue source, MigrationResultProduction mr, @Nullable Supplier<String> complement, boolean permanente) {
		final StreetData streetData = streetDataMigrator.migrate(source, mr);
		final AdresseSupplementaire dest;
		if (streetData != null) {
			// on ne migre pas une adresse qui ne contient ni rue ni localité postale...
			if (streetData instanceof StreetData.AucuneNomenclatureTrouvee) {
				return null;
			}

			// adresse suisse
			dest = buildAdresseSuisse(streetData);
		}
		else {
			// adresse étrangère
			dest = buildAdresseEtrangere(source);
		}
		dest.setDateDebut(source.getDateDebut());
		dest.setDateFin(source.getDateFin());
		dest.setPermanente(permanente);
		dest.setComplement(complement != null ? complement.get() : null);
		return dest;
	}

	private static AdresseSuisse buildAdresseSuisse(StreetData streetData) {
		final AdresseSuisse a = new AdresseSuisse();
		a.setNpaCasePostale(null);
		a.setNumeroAppartement(null);
		a.setNumeroCasePostale(null);
		a.setNumeroMaison(streetData.getNoPolice());
		a.setNumeroOrdrePoste(streetData.getNoOrdreP());
		a.setNumeroRue(streetData.getEstrid());
		a.setRue(streetData.getNomRue());
		a.setTexteCasePostale(null);
		return a;
	}

	private static AdresseEtrangere buildAdresseEtrangere(AdresseAvecRue source) {
		final AdresseEtrangere a = new AdresseEtrangere();
		a.setComplementLocalite(null);
		a.setNumeroAppartement(null);
		a.setNumeroCasePostale(null);
		a.setNumeroMaison(source.getNoPolice());
		a.setNumeroOfsPays(source.getOfsPays());
		a.setNumeroPostalLocalite(source.getLieu());
		a.setRue(source.getNomRue());
		a.setTexteCasePostale(null);
		return a;
	}
}
