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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.log.EntrepriseLoggedElement;
import ch.vd.uniregctb.migration.pm.log.EtablissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.IndividuLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCoordonneesFinancieres;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public abstract class AbstractEntityMigrator<T extends RegpmEntity> implements EntityMigrator<T> {

	protected final UniregStore uniregStore;
	protected final ActivityManager activityManager;

	public AbstractEntityMigrator(UniregStore uniregStore, ActivityManager activityManager) {
		this.uniregStore = uniregStore;
		this.activityManager = activityManager;
	}

	protected static final BinaryOperator<List<DateRange>> DATE_RANGE_LIST_MERGER =
			(l1, l2) -> {
				final List<DateRange> liste = Stream.concat(l1.stream(), l2.stream())
						.sorted(DateRangeComparator::compareRanges)
						.collect(Collectors.toList());
				return DateRangeHelper.merge(liste);
			};

	protected static final BinaryOperator<Map<RegpmCommune, List<DateRange>>> DATE_RANGE_MAP_MERGER =
			(m1, m2) -> Stream.concat(m1.entrySet().stream(), m2.entrySet().stream())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, DATE_RANGE_LIST_MERGER));

	protected static final StringRenderer<DateRange> DATE_RANGE_RENDERER =
			range -> String.format("[%s -> %s]",
			                       StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(range.getDateDebut()), "?"),
			                       StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(range.getDateFin()), "?"));

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

	/**
	 * Permet d'initialiser des structures de données dans le résultat
	 * @param mr structure à initialiser
	 */
	@Override
	public void initMigrationResult(MigrationResultInitialization mr) {
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByUniregIdSupplier(long id) {
		return () -> uniregStore.getEntityFromDb(Entreprise.class, id);
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByRegpmIdSupplier(IdMapping idMapper, long id) {
		return () -> uniregStore.getEntityFromDb(Entreprise.class, idMapper.getIdUniregEntreprise(id));
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByUniregIdSupplier(long id) {
		return () -> uniregStore.getEntityFromDb(Etablissement.class, id);
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByRegpmIdSupplier(IdMapping idMapper, long id) {
		return () -> uniregStore.getEntityFromDb(Etablissement.class, idMapper.getIdUniregEtablissement(id));
	}

	@NotNull
	protected final Supplier<PersonnePhysique> getIndividuByRegpmIdSupplier(IdMapping idMapper, long id) {
		return () -> uniregStore.getEntityFromDb(PersonnePhysique.class, idMapper.getIdUniregIndividu(id));
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
	protected KeyedSupplier<? extends Contribuable> getPolymorphicSupplier(IdMapping idMapper,
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
	 * Méthode utilitaire de migration des coordonnées financières pour une entité
	 * @param getter l'accesseur à la structure des coordonnées financières
	 * @param unireg la destination des données
	 * @param mr le collecteur de messages de suivi
	 */
	protected static void migrateCoordonneesFinancieres(Supplier<RegpmCoordonneesFinancieres> getter, Tiers unireg, MigrationResultProduction mr) {
		final RegpmCoordonneesFinancieres cf = getter.get();
		if (cf != null) {
			try {
				final String iban = IbanExtractor.extractIban(cf, mr);
				final String bicSwift = cf.getBicSwift();
				if (iban != null || bicSwift != null) {
					unireg.setCoordonneesFinancieres(new CoordonneesFinancieres(iban, bicSwift));
				}

				// TODO le titulaire du compte ??
				// TODO faut-il également introduire le POFICHBEXXX (= BIC de postfinance) ?
			}
			catch (IbanExtractor.IbanExtratorException e) {
				mr.addMessage(LogCategory.COORDONNEES_FINANCIERES, LogLevel.ERROR, e.getMessage());
			}
		}
	}

	protected void doInContext(EntityKey contextEntityKey, MigrationResultContextManipulation mr, Runnable action) {
		addEntityToContext(contextEntityKey, mr);
		try {
			action.run();
		}
		finally {
			resetEntityContext(contextEntityKey, mr);
		}
	}

	private void addEntityToContext(EntityKey key, MigrationResultContextManipulation mr) {
		final Graphe graphe = mr.getCurrentGraphe();

		switch (key.getType()) {
		case ENTREPRISE: {
			final RegpmEntreprise entreprise = graphe.getEntreprises().get(key.getId());
			mr.setContextValue(EntrepriseLoggedElement.class, new EntrepriseLoggedElement(entreprise, activityManager));
			break;
		}
		case ETABLISSEMENT: {
			final RegpmEtablissement etablissement = graphe.getEtablissements().get(key.getId());
			mr.setContextValue(EtablissementLoggedElement.class, new EtablissementLoggedElement(etablissement));
			break;
		}
		case INDIVIDU: {
			final RegpmIndividu individu = graphe.getIndividus().get(key.getId());
			mr.setContextValue(IndividuLoggedElement.class, new IndividuLoggedElement(individu));
			break;
		}
		default:
			throw new IllegalArgumentException("Type de clé : " + key.getType() + " non supporté!");
		}
	}

	private void resetEntityContext(EntityKey key, MigrationResultContextManipulation mr) {
		switch (key.getType()) {
		case ENTREPRISE:
			mr.resetContextValue(EntrepriseLoggedElement.class);
			break;
		case ETABLISSEMENT:
			mr.resetContextValue(EtablissementLoggedElement.class);
			break;
		case INDIVIDU:
			mr.resetContextValue(IndividuLoggedElement.class);
			break;
		default:
			throw new IllegalArgumentException("Type de clé : " + key.getType() + " non supporté!");
		}
	}
}
