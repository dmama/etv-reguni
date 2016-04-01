package ch.vd.uniregctb.migration.pm.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiConsumer;
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
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesMandats;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.log.EntrepriseLoggedElement;
import ch.vd.uniregctb.migration.pm.log.EtablissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.IndividuLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmBlocNotes;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCoordonneesFinancieres;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHisto;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class AbstractEntityMigrator<T extends RegpmEntity> implements EntityMigrator<T> {

	protected final MigrationContexte migrationContexte;

	public AbstractEntityMigrator(MigrationContexte migrationContexte) {
		this.migrationContexte = migrationContexte;
	}

	public static final BinaryOperator<List<DateRange>> DATE_RANGE_LIST_MERGER =
			(l1, l2) -> {
				final List<DateRange> liste = Stream.concat(l1.stream(), l2.stream())
						.sorted(DateRangeComparator::compareRanges)
						.collect(Collectors.toList());
				return DateRangeHelper.merge(liste);
			};

	public static final BinaryOperator<Map<RegpmCommune, List<DateRange>>> DATE_RANGE_MAP_MERGER =
			(m1, m2) -> Stream.concat(m1.entrySet().stream(), m2.entrySet().stream())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, DATE_RANGE_LIST_MERGER));

	/**
	 * Extracteur du numéro OFS (au sens Unireg) d'une commune en prenant en compte la spécificité des fractions de communes
	 * vaudoises (qui n'ont pas de numéro OFS officiel mais dont l'ID technique en tient lieu dans Unireg)
	 */
	public static final Function<RegpmCommune, Integer> NO_OFS_COMMUNE_EXTRACTOR =
			commune -> commune.getNoOfs() == null || commune.getNoOfs() == 0 ? commune.getId().intValue() : commune.getNoOfs();

	/**
	 * Extracteur du type d'autorité fiscale d'une commune
	 */
	public static final Function<RegpmCommune, TypeAutoriteFiscale> TAF_COMMUNE_EXTRACTOR =
			commune -> commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;

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
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	@Override
	public void initMigrationResult(MigrationResultInitialization mr, IdMapping idMapper) {
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByUniregIdSupplier(long id) {
		return () -> migrationContexte.getUniregStore().getEntityFromDb(Entreprise.class, id);
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByRegpmIdSupplier(IdMapping idMapper, long id) {
		return () -> migrationContexte.getUniregStore().getEntityFromDb(Entreprise.class, idMapper.getIdUniregEntreprise(id));
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByUniregIdSupplier(long id) {
		return () -> migrationContexte.getUniregStore().getEntityFromDb(Etablissement.class, id);
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByRegpmIdSupplier(IdMapping idMapper, long id) {
		return () -> migrationContexte.getUniregStore().getEntityFromDb(Etablissement.class, idMapper.getIdUniregEtablissement(id));
	}

	@NotNull
	protected final Supplier<PersonnePhysique> getIndividuByRegpmIdSupplier(IdMapping idMapper, long id) {
		return () -> migrationContexte.getUniregStore().getEntityFromDb(PersonnePhysique.class, idMapper.getIdUniregIndividu(id));
	}

	@NotNull
	protected final KeyedSupplier<Entreprise> getEntrepriseSupplier(IdMapping idMapper, RegpmEntreprise regpm) {
		return new KeyedSupplier<>(buildEntrepriseKey(regpm), getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId()));
	}

	@NotNull
	protected final KeyedSupplier<Etablissement> getEtablissementSupplier(IdMapping idMapper, RegpmEtablissement regpm) {
		return new KeyedSupplier<>(buildEtablissementKey(regpm), getEtablissementByRegpmIdSupplier(idMapper, regpm.getId()));
	}

	@NotNull
	protected final KeyedSupplier<PersonnePhysique> getIndividuSupplier(IdMapping idMapper, RegpmIndividu regpm) {
		return new KeyedSupplier<>(buildIndividuKey(regpm), getIndividuByRegpmIdSupplier(idMapper, regpm.getId()));
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
			return getEntrepriseSupplier(idMapper, entreprise);
		}

		final RegpmEtablissement etablissement = etablissementSupplier != null ? etablissementSupplier.get() : null;
		if (etablissement != null) {
			return getEtablissementSupplier(idMapper, etablissement);
		}

		final RegpmIndividu individu = individuSupplier != null ? individuSupplier.get() : null;
		if (individu != null) {
			return getIndividuSupplier(idMapper, individu);
		}

		return null;
	}

	/**
	 * Le côté polymorphique d'une relation est en général exprimé dans RegPM par plusieurs liens distincts, dont un seul est non-vide. Dans Unireg,
	 * il n'y a en général qu'un seul lien vers une entité à caractère polymorphique... Cette méthode permet donc de transcrire une façon de faire
	 * dans l'autre.
	 * @param entrepriseSupplier accès au lien vers une entreprise
	 * @param etablissementSupplier accès au lien vers un établissement
	 * @param individuSupplier accès au lien vers un individu
	 * @return la clé de l'entité (entreprise, établissement ou individu) de RegPM
	 */
	@Nullable
	protected static EntityKey getPolymorphicKey(@Nullable Supplier<RegpmEntreprise> entrepriseSupplier,
	                                             @Nullable Supplier<RegpmEtablissement> etablissementSupplier,
	                                             @Nullable Supplier<RegpmIndividu> individuSupplier) {

		final RegpmEntreprise entreprise = entrepriseSupplier != null ? entrepriseSupplier.get() : null;
		if (entreprise != null) {
			return buildEntrepriseKey(entreprise);
		}

		final RegpmEtablissement etablissement = etablissementSupplier != null ? etablissementSupplier.get() : null;
		if (etablissement != null) {
			return buildEtablissementKey(etablissement);
		}

		final RegpmIndividu individu = individuSupplier != null ? individuSupplier.get() : null;
		if (individu != null) {
			return buildIndividuKey(individu);
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
	 * Méthode utilitaire de migration des coordonnées financières pour une entité
	 * @param getter l'accesseur à la structure des coordonnées financières
	 * @param titulaireCompte titulaire du compte bancaire
	 * @param unireg la destination des données
	 * @param mr le collecteur de messages de suivi
	 */
	protected static void migrateCoordonneesFinancieres(Supplier<RegpmCoordonneesFinancieres> getter,
	                                                    String titulaireCompte,
	                                                    Tiers unireg,
	                                                    MigrationResultProduction mr) {
		final RegpmCoordonneesFinancieres cf = getter.get();
		if (cf != null) {
			try {
				final String iban = IbanExtractor.extractIban(cf, mr);
				final String bicSwift = cf.getBicSwift();
				if (iban != null || bicSwift != null) {
					unireg.setCoordonneesFinancieres(new CoordonneesFinancieres(iban, bicSwift));
					if (StringUtils.isNotBlank(titulaireCompte)) {
						unireg.setTitulaireCompteBancaire(titulaireCompte);
					}
				}

				// TODO faut-il également introduire le POFICHBEXXX (= BIC de postfinance) ?
			}
			catch (IbanExtractor.IbanExtractorException e) {
				mr.addMessage(LogCategory.COORDONNEES_FINANCIERES, LogLevel.ERROR, e.getMessage());
			}
		}
	}

	/**
	 * Point d'entrée de la migration d'une entité
	 * @param entity entité à migrer
	 * @param mr récipiendaire de messages à logguer
	 * @param linkCollector collecteur de liens entre entités (seront résolus à la fin de la migration)
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	@Override
	public final void migrate(T entity, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		final EntityKey entityKey = buildEntityKey(entity);
		doInLogContext(entityKey, mr, idMapper, () -> doMigrate(entity, mr, linkCollector, idMapper));
	}

	/**
	 * A implémenter dans les classes dérivées pour le réel travail de migration
	 * @param entity entité à migrer
	 * @param mr collecteur des messages de suivi et manipulateur de contexte de log
	 * @param linkCollector collecteur de liens entre entités
	 * @param idMapper mapper des identifiants regpm -> unireg
	 */
	protected abstract void doMigrate(T entity, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper);

	/**
	 * Construit une clé à partir de l'entité migrée
	 * @param entity entité à migrer
	 * @return clé pour l'entité
	 */
	@NotNull
	protected abstract EntityKey buildEntityKey(T entity);

	/**
	 * Génère une clé pour une entreprise de RegPM
	 * @param entreprise entreprise de RegPM
	 * @return la clé associée
	 */
	@NotNull
	protected static EntityKey buildEntrepriseKey(RegpmEntreprise entreprise) {
		return EntityKey.of(entreprise);
	}

	/**
	 * Génère une clé pour un établissement de RegPM
	 * @param etablissement établissement de RegPM
	 * @return la clé associée
	 */
	@NotNull
	protected static EntityKey buildEtablissementKey(RegpmEtablissement etablissement) {
		return EntityKey.of(etablissement);
	}

	/**
	 * Génère une clé pour un individu de RegPM
	 * @param individu individu de RegPM
	 * @return la clé associée
	 */
	@NotNull
	protected static EntityKey buildIndividuKey(RegpmIndividu individu) {
		return EntityKey.of(individu);
	}

	/**
	 * Exécute l'action donnée dans un contexte de log contenant les données de l'entité (entreprise, établissement, individu) fournie
	 * @param contextEntityKey entité dont les données doivent être temporairement poussées sur le contexte de log
	 * @param mr collecteur de messages de suivis et manipulateur de contextes de log
	 * @param idMapper container des mappings entres entités RegPM &lt;-&gt; Unireg
	 * @param action action à lancer
	 * @param <D> type de résultat retourné par l'action
	 * @return la donnée retournée par l'action
	 */
	protected <D> D doInLogContext(EntityKey contextEntityKey, MigrationResultContextManipulation mr, IdMapping idMapper, Supplier<D> action) {
		pushEntityToLogContext(contextEntityKey, mr, idMapper);
		try {
			return action.get();
		}
		finally {
			popEntityFromLogContext(contextEntityKey, mr);
		}
	}

	/**
	 * Exécute l'action donnée dans un contexte de log contenant les données de l'entité (entreprise, établissement, individu) fournie
	 * @param contextEntityKey entité dont les données doivent être temporairement poussées sur le contexte de log
	 * @param mr collecteur de messages de suivis et manipulateur de contextes de log
	 * @param idMapper container des mappings entres entités RegPM &lt;-&gt; Unireg
	 * @param action action à lancer
	 */
	protected void doInLogContext(EntityKey contextEntityKey, MigrationResultContextManipulation mr, IdMapping idMapper, Runnable action) {
		doInLogContext(contextEntityKey, mr, idMapper, () -> {
			action.run();
			return null;
		});
	}

	private void pushEntityToLogContext(EntityKey key, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final Graphe graphe = mr.getCurrentGraphe();

		switch (key.getType()) {
		case ENTREPRISE: {
			final RegpmEntreprise entreprise = graphe.getEntreprises().get(key.getId());
			mr.pushContextValue(EntrepriseLoggedElement.class, new EntrepriseLoggedElement(entreprise, migrationContexte.getActivityManager()));
			break;
		}
		case ETABLISSEMENT: {
			final RegpmEtablissement etablissement = graphe.getEtablissements().get(key.getId());
			mr.pushContextValue(EtablissementLoggedElement.class, new EtablissementLoggedElement(etablissement, idMapper));
			break;
		}
		case INDIVIDU: {
			final RegpmIndividu individu = graphe.getIndividus().get(key.getId());
			mr.pushContextValue(IndividuLoggedElement.class, new IndividuLoggedElement(individu, idMapper));
			break;
		}
		default:
			throw new IllegalArgumentException("Type de clé : " + key.getType() + " non supporté!");
		}
	}

	private void popEntityFromLogContext(EntityKey key, MigrationResultContextManipulation mr) {
		switch (key.getType()) {
		case ENTREPRISE:
			mr.popContexteValue(EntrepriseLoggedElement.class);
			break;
		case ETABLISSEMENT:
			mr.popContexteValue(EtablissementLoggedElement.class);
			break;
		case INDIVIDU:
			mr.popContexteValue(IndividuLoggedElement.class);
			break;
		default:
			throw new IllegalArgumentException("Type de clé : " + key.getType() + " non supporté!");
		}
	}

	/**
	 * @param list une liste de ranges
	 * @return une représentation String de cette liste
	 */
	protected static String toDisplayString(List<? extends DateRange> list) {
		return toDisplayString(list, StringRenderers.DATE_RANGE_RENDERER);
	}

	/**
	 * @param list une collection d'élément
	 * @param renderer un renderer capable de fournir une chaîne de caractères pour chaque élément de la liste
	 * @param <T> le type des éléments dans la liste
	 * @return une représentation String de cette liste
	 */
	protected static <T> String toDisplayString(Collection<T> list, StringRenderer<? super T> renderer) {
		return list.stream().map(renderer::toString).collect(Collectors.joining(", "));
	}

	/**
	 * Concatène les trois champs de la raison sociale en une seule
	 * @param ligne1 première ligne
	 * @param ligne2 deuxième ligne
	 * @param ligne3 troisième ligne
	 * @return la chaîne de caractères (<code>null</code> si vide) représentant la raison sociale de l'entreprise
	 */
	protected static String extractRaisonSociale(String ligne1, String ligne2, String ligne3) {
		final Stream.Builder<String> builder = Stream.builder();
		if (StringUtils.isNotBlank(ligne1)) {
			builder.accept(ligne1);
		}
		if (StringUtils.isNotBlank(ligne2)) {
			builder.accept(ligne2);
		}
		if (StringUtils.isNotBlank(ligne3)) {
			builder.accept(ligne3);
		}
		return StringUtils.trimToNull(builder.build().collect(Collectors.joining(" ")));
	}

	/**
	 * Adapteur des motifs d'ouverture/fermeture des fors fiscaux par rapports aux fusions de communes (peut-être utilisé dans {@link #adapterAutourFusionsCommunes(LocalisationDatee, MigrationResultProduction, LogCategory, BiConsumer)})
	 * @param origine le for fiscal original
	 * @param remplacant le for fiscal (a priori dupliqué du premier sauf peut-être pour les dates de début et de fin, ainsi que la commune visée) qui remplacera (au moins pour partie) le for original
	 * @param <T> le type de for fiscal
	 */
	protected static <T extends ForFiscalAvecMotifs> void adapteMotifsForsFusionCommunes(T origine, T remplacant) {
		if (origine.getDateDebut() != remplacant.getDateDebut()) {
			remplacant.setMotifOuverture(MotifFor.FUSION_COMMUNES);
		}
		if (origine.getDateFin() != remplacant.getDateFin() && remplacant.getDateFin() != null) {
			remplacant.setMotifFermeture(MotifFor.FUSION_COMMUNES);
		}
	}

	/**
	 * Découpe l'entité de localisation datée fournie en entrée en autant de localisations datées que
	 * nécessaire pour tenir compte des fusions de communes suisses
	 * @param source une localisation datée
	 * @param mr collecteur de messages de suivi
	 * @param logCategory catégorie à utiliser pour les messages de suivi
	 * @param adaptator (optionel) consomateur qui recevra la source en premier paramètre, et chacun des nouveaux éléments créés (dupliqués) en second paramètre
	 * @param <LD> le type de localisation datée (for fiscal, décision aci...)
	 * @return la liste (triée par date de début) des entités après découpage
	 */
	@NotNull
	protected <LD extends LocalisationDatee & Duplicable<? super LD>> List<LD> adapterAutourFusionsCommunes(LD source,
	                                                                                                        MigrationResultProduction mr,
	                                                                                                        LogCategory logCategory,
	                                                                                                        @Nullable BiConsumer<LD, LD> adaptator) {
		final Stream<LD> stream = adapterAutourFusionsCommunesStream(source, mr, logCategory, adaptator);

		// reconstitution de la liste (triée) des localisations remplaçantes
		return stream
				.sorted(DateRangeComparator::compareRanges)
				.peek(ld -> {
					// on ne mets un log que si quelque chose a changé...
					if (ld != source) {
						mr.addMessage(logCategory, LogLevel.INFO,
						              String.format("Entité %s au moins partiellement remplacée par %s pour suivre les fusions de communes.",
						                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source),
						                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(ld)));
					}
				})
				.collect(Collectors.toList());
	}

	/**
	 * @param source une localisation datée
	 * @param mr collecteur de messages de suivi
	 * @param logCategory catégorie à utiliser pour les messages de suivi
	 * @param <LD> le type de localisation datée (for fiscal, décision aci...)
	 * @return <code>null</code> s'il n'y a pas de limitation à apporter à la source, sinon la 'meilleure' couverture non-totale de la source qui correspond à une commune avec le bon numéro OFS
	 */
	@Nullable
	private <LD extends LocalisationDatee> DateRange getCouvertureLimitante(LD source, MigrationResultProduction mr, LogCategory logCategory) {

		// on ne s'intéresse qu'aux communes suisses
		if (source.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			return null;
		}

		// ok, c'est une commune... est-elle connue dans l'infrastructure ?
		final List<Commune> communes = migrationContexte.getInfraService().getCommuneHistoByNumeroOfs(source.getNumeroOfsAutoriteFiscale());
		if (communes == null || communes.isEmpty()) {
			mr.addMessage(logCategory, LogLevel.ERROR,
			              String.format("Commune %d inconnue dans l'infrastructure fiscale (cas de %s).",
			                            source.getNumeroOfsAutoriteFiscale(),
			                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source)));

			// ca va être un problème plus tard, mais c'est la validation qui chopera le souci
			return null;
		}

		// la commune est connue, est-elle valide sur la durée de la localisation ?
		// on ignore les communes dont la date de début est dans le futur, et on
		// ignore la date de fin des communes si celle-ci est dans le futur (y compris aujourd'hui)
		final DateRange intersectante = communes.stream()
				.filter(commune -> !migrationContexte.getDateHelper().isFutureDate(commune.getDateDebutValidite()))
				.map(c -> new DateRangeHelper.Range(c.getDateDebutValidite(), migrationContexte.getDateHelper().isFutureDateOrToday(c.getDateFinValidite()) ? null : c.getDateFinValidite()))
				.filter(range -> DateRangeHelper.intersect(range, source))
				.sorted(DateRangeComparator::compareRanges)
				.findFirst()
				.orElse(null);

		// c'est possible, j'ai trouvé une intersection
		if (intersectante != null) {
			if (DateRangeHelper.within(source, intersectante)) {
				// complètement incluse dans une instance de commune -> tout va bien, rien à faire
				return null;
			}

			// c'est la seule portion de la localisation couverte pour le moment
			return intersectante;
		}

		// pas d'intersection, on va prendre la première, au pif en fait...
		return new DateRangeHelper.Range(communes.get(0).getDateDebutValidite(), communes.get(0).getDateFinValidite());
	}

	/**
	 * Découpe l'entité de localisation datée fournie en entrée en autant de localisations datées que
	 * nécessaire pour tenir compte des fusions de communes suisses
	 * @param source une localisation datée
	 * @param mr collecteur de messages de suivi
	 * @param logCategory catégorie à utiliser pour les messages de suivi
	 * @param adaptator (optionel) consomateur qui recevra la source en premier paramètre, et chacun des nouveaux éléments créés (dupliqués) en second paramètre
	 * @param <LD> le type de localisation datée (for fiscal, décision aci...)
	 * @return la liste des entités après découpage
	 */
	@NotNull
	private <LD extends LocalisationDatee & Duplicable<? super LD>> Stream<LD> adapterAutourFusionsCommunesStream(LD source,
	                                                                                                              MigrationResultProduction mr,
	                                                                                                              LogCategory logCategory,
	                                                                                                              @Nullable BiConsumer<LD, LD> adaptator) {

		// récupération de la couverture
		final DateRange couverture = getCouvertureLimitante(source, mr, logCategory);
		if (couverture == null) {
			// ca joue comme ça
			return Stream.of(source);
		}

		// ok, il y a un ou des bouts qui dépassent...

		final Stream.Builder<LD> builder = Stream.builder();

		// on regarde d'abord l'éventuelle intersection (= ce qui reste de l'ancienne donnée)
		final DateRange intersection = DateRangeHelper.intersection(source, couverture);
		if (intersection != null) {
			builder.accept(duplicate(source, intersection, source.getNumeroOfsAutoriteFiscale(), adaptator));
		}

		// jusqu'à preuve que le cas contraire existe, on va supposer qu'un numéro OFS de commune est utilisée de manière continue
		// (= sans interruption suivie d'une ré-utilisation), ce qui signifie que les dépassements sont soit avant la converture, soit après,
		// mais en aucun cas dans un trou de cette couverture

		// maintenant on regarde ce qui dépasse (= ce qui n'intersecte pas)
		final List<DateRange> depassements = DateRangeHelper.subtract(source, Collections.singletonList(couverture));
		depassements.stream()
				.map(d -> traiterDepassement(source, d, couverture.getDateDebut(), couverture.getDateFin(), mr, logCategory, adaptator))
				.flatMap(Function.identity())
				.forEach(builder);

		// finalisation du stream
		return builder.build();
	}

	/**
	 * Traitement d'un dépassement de validité de la commune de la source
	 * @param source une localisation datée
	 * @param rangeNonValide range sur lequel la commune de la localisation source n'est pas valide (peut être ouvert à gauche et/ou à droite)
	 * @param debutCouverture date de début de la validité de la commune de la source
	 * @param finCouverture date de fin de la validité de la commune de la source
	 * @param mr collecteur de messages de suivi
	 * @param logCategory catégorie à utiliser pour les messages de suivi
	 * @param adaptator (optionel) consomateur qui recevra la source en premier paramètre, et chacun des nouveaux éléments créés (dupliqués) en second paramètre
	 * @param <LD> le type de localisation datée (for fiscal, décision aci...)
	 * @return un stream des entités de remplissage du dépassement
	 */
	@NotNull
	private <LD extends LocalisationDatee & Duplicable<? super LD>> Stream<LD> traiterDepassement(LD source, DateRange rangeNonValide,
	                                                                                              @Nullable RegDate debutCouverture, @Nullable RegDate finCouverture,
	                                                                                              MigrationResultProduction mr, LogCategory logCategory, @Nullable BiConsumer<LD, LD> adaptator) {

		// juste avant la couverture ?
		// ou bien avant la couverture (cas des périodes de validité (couverture vs localisation datée) complètement disjointes) ?
		if (debutCouverture != null && RegDateHelper.isBefore(rangeNonValide.getDateFin(), debutCouverture, NullDateBehavior.LATEST)) {
			final List<Integer> avant = migrationContexte.getFusionCommunesProvider().getCommunesAvant(source.getNumeroOfsAutoriteFiscale(), debutCouverture);
			if (avant.isEmpty()) {
				mr.addMessage(logCategory, LogLevel.ERROR,
				              String.format("Entité %s : aucune commune connue à l'origine de la commune %d avant le %s.",
				                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source),
				                            source.getNumeroOfsAutoriteFiscale(),
				                            StringRenderers.DATE_RENDERER.toString(debutCouverture)));
				return Stream.empty();
			}
			else {
				if (avant.size() > 1) {
					mr.addMessage(logCategory, LogLevel.WARN,
					              String.format("Entité %s : plusieurs communes connues à l'origine de la commune %d avant le %s (on prend la première) : %s.",
					                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source),
					                            source.getNumeroOfsAutoriteFiscale(),
					                            StringRenderers.DATE_RENDERER.toString(debutCouverture),
					                            toDisplayString(avant, String::valueOf)));

				}

				final LD duplicate = duplicate(source, rangeNonValide, avant.get(0), adaptator);
				return adapterAutourFusionsCommunesStream(duplicate, mr, logCategory, adaptator);        // appel récursif
			}
		}

		// juste après la couverture ?
		// ou bien après la couverture (cas des périodes de validité (couverture vs localisation datée) complètement disjointes) ?
		else if (finCouverture != null && RegDateHelper.isAfter(rangeNonValide.getDateDebut(), finCouverture, NullDateBehavior.EARLIEST)) {
			final List<Integer> apres = migrationContexte.getFusionCommunesProvider().getCommunesApres(source.getNumeroOfsAutoriteFiscale(), finCouverture);
			if (apres.isEmpty()) {
				mr.addMessage(logCategory, LogLevel.ERROR,
				              String.format("Entité %s : aucune commune connue à la suite de la commune %d après le %s.",
				                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source),
				                            source.getNumeroOfsAutoriteFiscale(),
				                            StringRenderers.DATE_RENDERER.toString(finCouverture)));
				return Stream.empty();
			}
			else {
				if (apres.size() > 1) {
					mr.addMessage(logCategory, LogLevel.WARN,
					              String.format("Entité %s : plusieurs communes connues à la suite de la commune %d après le %s (on prend la première) : %s.",
					                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source),
					                            source.getNumeroOfsAutoriteFiscale(),
					                            StringRenderers.DATE_RENDERER.toString(finCouverture),
					                            toDisplayString(apres, String::valueOf)));
				}

				final LD duplicate = duplicate(source, rangeNonValide, apres.get(0), adaptator);
				return adapterAutourFusionsCommunesStream(duplicate, mr, logCategory, adaptator);        // appel récursif
			}
		}

		// ni avant, ni après... c'est apparemment le cas où la supposition vue plus haut est invalide...
		else {
			throw new RuntimeException("Algorithme incomplet : il semble que certains numéros OFS (" + source.getNumeroOfsAutoriteFiscale() + " ?) ont des 'trous' d'utilisation...");
		}
	}

	/**
	 * Duplication d'une localisation moyennant quelques ajustements
	 * @param source une localisation datée
	 * @param range nouveau range de validité de la copie
	 * @param noOfs numéro OFS de la commune à placer sur la copie
	 * @param adaptator (optionel) consomateur qui recevra la source en premier paramètre, et chacun des nouveaux éléments créés (dupliqués) en second paramètre
	 * @param <LD> le type de localisation datée (for fiscal, décision aci...)
	 * @return une donnée équivalente à la donnée source moyennant les ajustements demandés
	 */
	@NotNull
	private static <LD extends LocalisationDatee & Duplicable<? super LD>> LD duplicate(LD source, DateRange range, int noOfs, @Nullable BiConsumer<LD, LD> adaptator) {
		final LD duplicate = duplicate(source);
		duplicate.setDateDebut(range.getDateDebut());
		duplicate.setDateFin(range.getDateFin());
		duplicate.setNumeroOfsAutoriteFiscale(noOfs);
		if (adaptator != null) {
			adaptator.accept(source, duplicate);
		}
		return duplicate;
	}

	/**
	 * Duplication d'une entité. Les données suivantes ne sont pas recopiées :
	 * <ul>
	 *     <li>identifiant</li>
	 *     <li>date et visa de dernière mutation</li>
	 *     <li>éventuels date/visa d'annulation</li>
	 * </ul>
	 * @param source source des données
	 * @param <HE> type de la donnée
	 * @return nouvelle instance générée par un appel à {@link Duplicable#duplicate} sur la source, et à la recopie des LOG_MDATE, LOG_MUSER, LOG_CDATE, LOG_CUSER
	 */
	protected static <HE extends HibernateEntity & Duplicable<? super HE>> HE duplicate(HE source) {

		// ce cast est assez moche, on est d'accord, mais c'est trop compliqué par rapport
		// au gain (= seulement cette migration) de faire en sorte que ForFiscal devienne
		// une classe générique afin de pouvoir implémenter la bonne flaveur de l'interface Duplicable...

		//noinspection unchecked
		final HE dest = (HE) source.duplicate();
		dest.setLogCreationDate(source.getLogCreationDate());
		dest.setLogCreationUser(source.getLogCreationUser());
		return dest;
	}

	/**
	 * Si la localisation datée représente une commune faîtière de factions (vaudoises), on met un warning dans le log
	 * et on place la localisation sur la première des fractions trouvées
	 * @param source une localisation datée
	 * @param mr collecteur de messages de suivi
	 * @param logCategory catégorie à utiliser pour les messages de suivi
	 * @return <code>true</code> si tout était déjà bon, <code>false</code> si une mise à jour a été nécessaire
	 */
	protected boolean checkFractionCommuneVaudoise(LocalisationDatee source, MigrationResultProduction mr, LogCategory logCategory) {

		// seules les communes vaudoises ont des fractions
		if (source.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return true;
		}

		// on va chercher la commune en question
		final List<Commune> communes = migrationContexte.getInfraService().getCommuneHistoByNumeroOfs(source.getNumeroOfsAutoriteFiscale());
		if (communes == null || communes.isEmpty()) {
			// on ne trouve pas de commune, ça râlera plus tard (mais ce n'est pas le moment, dans cette méthode sur les fractions,
			// de lever l'erreur
			return true;
		}

		// la commune du for
		final Commune candidate;

		// on trouve la première commune qui insersecte la localisation
		final Commune intersectante = communes.stream()
				.filter(commune -> !migrationContexte.getDateHelper().isFutureDate(commune.getDateDebutValidite()))
				.map(c -> Pair.of(c, new DateRangeHelper.Range(c.getDateDebutValidite(), migrationContexte.getDateHelper().isFutureDateOrToday(c.getDateFinValidite()) ? null : c.getDateFinValidite())))
				.filter(pair -> DateRangeHelper.intersect(pair.getRight(), source))
				.sorted(Comparator.comparing(Pair::getRight, DateRangeComparator::compareRanges))
				.findFirst()
				.map(Pair::getLeft)
				.orElse(null);

		// si on a une intersection, on va tester avec
		if (intersectante != null) {
			candidate = intersectante;
		}
		else if (RegDateHelper.isBefore(source.getDateFin(), communes.get(0).getDateDebutValidite(), NullDateBehavior.LATEST)) {
			candidate = communes.get(0);
		}
		else if (RegDateHelper.isAfter(source.getDateDebut(), communes.get(communes.size() - 1).getDateFinValidite(), NullDateBehavior.EARLIEST)) {
			candidate = communes.get(communes.size() - 1);
		}
		else {
			throw new IllegalArgumentException("Algorithme incomplet dans la recherche de la commune d'un for... il y aurait des discontinuités dans l'utilisation d'un numéro OFS (" + source.getNumeroOfsAutoriteFiscale() + ") ?");
		}

		// on a une commune avec laquelle tester -> allons-y !
		if (!candidate.isPrincipale()) {
			return true;
		}

		// quelles sont les fractions en questions ?
		final List<Commune> fractions = migrationContexte.getFractionsCommuneProvider().getFractions(candidate);

		// cette commune est donc principale... on change avec la première de ses fractions...
		mr.addMessage(logCategory, LogLevel.ERROR,
		              String.format("La commune de l'entité %s est une commune faîtière de fractions, elle sera déplacée sur la PREMIERE fraction correspondante : %s !",
		                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(source),
		                            toDisplayString(fractions, f -> String.valueOf(f.getNoOFS()))));

		// la bonne fraction !
		final Commune fraction = fractions.get(0);

		// on change la localisation
		source.setNumeroOfsAutoriteFiscale(fraction.getNoOFS());
		return false;
	}

	/**
	 * @param mandat mandat dans RegPM
	 * @param mr collecteur de messages de suivi (un message est loggué si le mandat n'est pas repris)
	 * @return <code>true</code> si le mandat doit être repris, <code>false</code> sinon
	 */
	private boolean isMandatRepris(RegpmMandat mandat, MigrationResultProduction mr, LogCategory logCategory) {

		// pas de mandataire identifié -> on oublie tout de suite
		final EntityKey mandataire = getPolymorphicKey(mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
		if (mandataire == null) {
			mr.addMessage(logCategory, LogLevel.ERROR,
			              String.format("Le mandat %d de l'entreprise mandante %d n'a pas de mandataire.",
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise()));
			return false;
		}

		// [SIFISC-17979] on ne reprend que les mandats généraux et les mandats tiers
		if (mandat.getType() != RegpmTypeMandat.GENERAL && mandat.getType() != RegpmTypeMandat.TIERS) {
			mr.addMessage(logCategory, LogLevel.WARN,
			              String.format("Le mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré à cause de son type (%s).",
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise(),
			                            mandataire.getId(),
			                            mandataire.getType(),
			                            mandat.getType()));
			return false;
		}

		// [SIFISC-17979] pour les mandats "tiers", on ne reprend que les mandats encore actifs
		if (mandat.getType() == RegpmTypeMandat.TIERS && mandat.getDateResiliation() != null) {
			mr.addMessage(logCategory, LogLevel.WARN,
			              String.format("Le mandat %s %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré car il est résilié.",
			                            mandat.getType(),
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise(),
			                            mandataire.getId(),
			                            mandataire.getType()));
			return false;
		}

		// [SIFISC-17979] pour les mandats "tiers", on ne reprends que les mandats qui pointent vers une entreprise
		if (mandat.getType() == RegpmTypeMandat.TIERS && mandataire.getType() != EntityKey.Type.ENTREPRISE) {
			mr.addMessage(logCategory, LogLevel.WARN,
			              String.format("Le mandat %s %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré à cause du type du mandataire.",
			                            mandat.getType(),
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise(),
			                            mandataire.getId(),
			                            mandataire.getType()));
			return false;
		}

		// [SIFISC-17979] pour les mandats, "tiers", le numéro de compte doit être interprétable
		if (mandat.getType() == RegpmTypeMandat.TIERS) {
			try {
				final String iban = IbanExtractor.extractIban(mandat, mr);
				if (iban == null) {
					mr.addMessage(logCategory, LogLevel.ERROR,
					              String.format("Le mandat %s %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré car aucune coordonnée financière n'a pu en être extraite.",
					                            mandat.getType(),
					                            mandat.getId().getNoSequence(),
					                            mandat.getId().getIdEntreprise(),
					                            mandataire.getId(),
					                            mandataire.getType()));
					return false;
				}
			}
			catch (IbanExtractor.IbanExtractorException e) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Impossible d'extraire un IBAN du mandat " + mandat.getId() + " (" + e.getMessage() + "), mandat non repris.");
				return false;
			}
		}

		// on ne reprend de toute façon que les mandats assez récents
		if (RegDateHelper.isBefore(mandat.getDateResiliation(), migrationContexte.getDatesParticulieres().getSeuilRepriseMandats(), NullDateBehavior.LATEST)) {
			mr.addMessage(logCategory, LogLevel.WARN,
			              String.format("Le mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré car sa date de résiliation est antérieure au %s (%s).",
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise(),
			                            mandataire.getId(),
			                            mandataire.getType(),
			                            StringRenderers.DATE_RENDERER.toString(migrationContexte.getDatesParticulieres().getSeuilRepriseMandats()),
			                            StringRenderers.DATE_RENDERER.toString(mandat.getDateResiliation())));
			return false;
		}

		// pas de date d'attribution (ou beaucoup trop lointaine dans le passé) -> passe ton chemin, manant !
		if (mandat.getDateAttribution() == null) {
			mr.addMessage(logCategory, LogLevel.ERROR,
			              String.format("Le mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré car sa date d'attribution est nulle (ou antérieure au 01.08.1291).",
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise(),
			                            mandataire.getId(),
			                            mandataire.getType()));
			return false;
		}

		// date d'attribution future -> ignoré
		if (migrationContexte.getDateHelper().isFutureDate(mandat.getDateAttribution())) {
			mr.addMessage(logCategory, LogLevel.ERROR,
			              String.format("Le mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignoré car sa date d'attribution est dans le futur (%s).",
			                            mandat.getId().getNoSequence(),
			                            mandat.getId().getIdEntreprise(),
			                            mandataire.getId(),
			                            mandataire.getType(),
			                            StringRenderers.DATE_RENDERER.toString(mandat.getDateAttribution())));
			return false;
		}

		// un petit log pour les dates louches
		migrationContexte.getDateHelper().checkDateLouche(mandat.getDateAttribution(),
		                                                  () -> String.format("La date d'attribution du mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s",
		                                                                      mandat.getId().getNoSequence(),
		                                                                      mandat.getId().getIdEntreprise(),
		                                                                      mandataire.getId(),
		                                                                      mandataire.getType()),
		                                                  logCategory,
		                                                  mr);
		migrationContexte.getDateHelper().checkDateLouche(mandat.getDateResiliation(),
		                                                  () -> String.format("La date de résiliation du mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s",
		                                                                      mandat.getId().getNoSequence(),
		                                                                      mandat.getId().getIdEntreprise(),
		                                                                      mandataire.getId(),
		                                                                      mandataire.getType()),
		                                                  logCategory,
		                                                  mr);

		return true;
	}

	/**
	 * @param mandat un mandat source
	 * @param mr le collecteur de messages de suivi
	 * @param logCategory la catégorie de log en cas de message généré
	 * @return le mandat source si sa date de résiliation est absente ou passée, une autre instance sans date de résiliation si elle est future
	 */
	private RegpmMandat ignoreDateResiliationMandatSiFuture(RegpmMandat mandat, MigrationResultProduction mr, LogCategory logCategory) {
		if (mandat.getDateResiliation() == null || !migrationContexte.getDateHelper().isFutureDate(mandat.getDateResiliation())) {
			return mandat;
		}

		final EntityKey mandataire = getPolymorphicKey(mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
		if (mandataire == null) {
			// on ne devrait jamais passer ici, normalement ce cas a été écarté plus tôt
			return mandat;
		}

		mr.addMessage(logCategory, LogLevel.ERROR,
		              String.format("La date de résiliation du mandat %d de l'entreprise mandante %d vers l'entité mandataire %d de type %s est ignorée (= mandat ouvert) car elle est dans le futur (%s).",
		                            mandat.getId().getNoSequence(),
		                            mandat.getId().getIdEntreprise(),
		                            mandataire.getId(),
		                            mandataire.getType(),
		                            StringRenderers.DATE_RENDERER.toString(mandat.getDateResiliation())));

		final RegpmMandat copie = new RegpmMandat();
		copie.setBicSwift(mandat.getBicSwift());
		copie.setDateAttribution(mandat.getDateAttribution());
		copie.setDateResiliation(null);         // c'est justement celle-là qu'on veut ignorer
		copie.setIban(mandat.getIban());
		copie.setId(mandat.getId());
		copie.setInstitutionFinanciere(mandat.getInstitutionFinanciere());
		copie.setLastMutationOperator(mandat.getLastMutationOperator());
		copie.setLastMutationTimestamp(mandat.getLastMutationTimestamp());
		copie.setMandataireEntreprise(mandat.getMandataireEntreprise());
		copie.setMandataireEtablissement(mandat.getMandataireEtablissement());
		copie.setMandataireIndividu(mandat.getMandataireIndividu());
		copie.setMotif(mandat.getMotif());
		copie.setNoCCP(mandat.getNoCCP());
		copie.setNoCompteBancaire(mandat.getNoCompteBancaire());
		copie.setNoFaxContact(mandat.getNoFaxContact());
		copie.setNomContact(mandat.getNomContact());
		copie.setNomInstitutionFinanciere(mandat.getNomInstitutionFinanciere());
		copie.setNoTelContact(mandat.getNoTelContact());
		copie.setPrenomContact(mandat.getPrenomContact());
		copie.setType(mandat.getType());
		return copie;
	}

	/**
	 * Extrait les données des mandats d'une entité à partir de ses collections de mandants et et mandataires
	 * @param entityKey clé associée à l'entité
	 * @param mandants la collection des mandats représentant les mandants de l'entité
	 * @param mandataires la collection des mandats représentant les mandataires de l'entité
	 * @param mr collecteur de messages de suivi et manipulateur du contexte de log
	 * @param logCategory catégorie de log pour les messages générés
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return les données extraites
	 */
	@NotNull
	protected DonneesMandats extractDonneesMandats(EntityKey entityKey,
	                                               @Nullable Collection<RegpmMandat> mandants,
	                                               @Nullable Collection<RegpmMandat> mandataires,
	                                               MigrationResultContextManipulation mr,
	                                               LogCategory logCategory,
	                                               IdMapping idMapper) {

		return doInLogContext(entityKey, mr, idMapper, () -> {

			// l'entité est mandatrice de ses mandants
			final List<RegpmMandat> rolesMandataire;
			if (mandants != null && !mandants.isEmpty()) {
				rolesMandataire = mandants.stream()
						.sorted(Comparator.<RegpmMandat>comparingLong(mandat -> mandat.getId().getIdEntreprise()).thenComparingInt(mandat -> mandat.getId().getNoSequence()))   // tri pour déterminisme
						.filter(mandat -> isMandatRepris(mandat, mr, logCategory))
						.map(mandat -> ignoreDateResiliationMandatSiFuture(mandat, mr, logCategory))
						.collect(Collectors.toList());
			}
			else {
				rolesMandataire = null;
			}

			// l'entité est mandante de ses mandataires
			final List<RegpmMandat> rolesMandant;
			if (mandataires != null && !mandataires.isEmpty()) {
				rolesMandant = mandataires.stream()
						.sorted(Comparator.comparingLong(mandat -> mandat.getId().getNoSequence()))     // tri pour que les tests soient plus reproductibles...
						.filter(mandat -> isMandatRepris(mandat, mr, logCategory))
						.map(mandat -> ignoreDateResiliationMandatSiFuture(mandat, mr, logCategory))
						.collect(Collectors.toList());
			}
			else {
				rolesMandant = null;
			}

			return new DonneesMandats(rolesMandant, rolesMandataire);
		});
	}

	/**
	 * Migration des notes associées à un tiers
	 * @param notes notes collectées dans RegPM
	 * @param unireg destination des notes
	 */
	protected void migrateNotes(SortedSet<? extends RegpmBlocNotes> notes, Tiers unireg) {
		if (notes != null && !notes.isEmpty()) {
			notes.stream()
					.filter(regpm -> StringUtils.isNotBlank(regpm.getCommentaire()))
					.map(regpm -> {
						final Remarque remarque = new Remarque();
						copyCreationMutation(regpm, remarque);
						remarque.setTexte(migrateTexteNote(regpm.getCommentaire(), regpm.getDateValidite()));
						remarque.setTiers(unireg);
						return remarque;
					})
					.filter(remarque -> StringUtils.isNotBlank(remarque.getTexte()))
					.forEach(migrationContexte.getUniregStore()::saveEntityToDb);
		}
	}

	private static String migrateTexteNote(String src, RegDate dateValidite) {
		final int ligne = 60;

		// on sait que le commentaire dans le mainframe a une longueur maximale de 800 caractères...
		// donc on n'a pas à se préoccuper d'une quelconque troncature (nous avons 2000 caractères dans Unireg)

		final List<String> lignes = new ArrayList<>(20);
		for (int i = 0 ; i < src.length() ; i += ligne) {
			final String extract = StringUtils.trimToNull(src.substring(i, Math.min(i + ligne, src.length())));
			if (extract != null) {
				lignes.add(extract);
			}
		}

		if (!lignes.isEmpty()) {
			lignes.set(0, String.format("%s - %s", RegDateHelper.dateToDisplayString(dateValidite), lignes.get(0)));
			return lignes.stream().collect(Collectors.joining(System.lineSeparator()));
		}
		else {
			return null;
		}
	}

	/**
	 * @param tiers le tiers à qui on veut ajouter un for secondaire, là, maintenant...
	 * @param rangeFor la période de validité du for secondaire prévu
	 * @return {@link GenreImpot#BENEFICE_CAPITAL} s'il s'agit d'une entreprise non-'société de personnes' dans la période du for, ou sinon {@link GenreImpot#REVENU_FORTUNE}
	 */
	@NotNull
	protected GenreImpot getGenreImpotPourForSecondaire(Tiers tiers, DateRange rangeFor) {
		if (tiers instanceof Entreprise) {
			final List<CategorieEntrepriseHisto> categories = migrationContexte.getTiersService().getCategoriesEntrepriseHisto((Entreprise) tiers);
			final List<DateRange> rangesSP = categories.stream()
					.filter(cat -> cat.getCategorie() == CategorieEntreprise.SP)
					.collect(Collectors.toList());
			if (!DateRangeHelper.isFullyCovered(rangeFor, rangesSP)) {
				return GenreImpot.BENEFICE_CAPITAL;
			}
		}
		return GenreImpot.REVENU_FORTUNE;
	}
}
