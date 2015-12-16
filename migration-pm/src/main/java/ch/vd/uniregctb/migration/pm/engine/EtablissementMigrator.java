package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.migration.pm.ConsolidationPhase;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesCiviles;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesMandats;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDomicileEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EtablissementMigrator extends AbstractEntityMigrator<RegpmEtablissement> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtablissementMigrator.class);

	private final ServiceOrganisationService organisationService;
	private final boolean rcentEnabled;

	public EtablissementMigrator(UniregStore uniregStore, ActivityManager activityManager, ServiceInfrastructureService infraService,
	                             ServiceOrganisationService organisationService, AdresseHelper adresseHelper, FusionCommunesProvider fusionCommunesProvider, FractionsCommuneProvider fractionsCommuneProvider,
	                             DatesParticulieres datesParticulieres, boolean rcentEnabled) {
		super(uniregStore, activityManager, infraService, fusionCommunesProvider, fractionsCommuneProvider, datesParticulieres, adresseHelper);
		this.organisationService = organisationService;
		this.rcentEnabled = rcentEnabled;
	}

	private static final class DatesEtablissementsStables {
		@NotNull
		final List<DateRange> liste;
		public DatesEtablissementsStables(List<DateRange> liste) {
			this.liste = Optional.ofNullable(liste).orElseGet(Collections::emptyList);
		}
	}

	private static final class DomicilesStables {
		@NotNull
		final Map<RegpmCommune, List<DateRange>> map;
		public DomicilesStables(Map<RegpmCommune, List<DateRange>> map) {
			this.map = Optional.ofNullable(map).orElseGet(Collections::emptyMap);
		}
	}

	@Override
	public void initMigrationResult(MigrationResultInitialization mr, IdMapping idMapper) {
		super.initMigrationResult(mr, idMapper);

		// on va regrouper les données (communes et dates) par entité juridique afin de créer,
		// pour chacune d'entre elles, les fors secondaires "activité" qui vont bien
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.Activite.class,
		                                        ConsolidationPhase.FORS_ACTIVITE,
		                                        d -> d.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData.Activite(d1.entiteJuridiqueSupplier, DATE_RANGE_MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondairesEtablissement(d, mr, idMapper));

		//
		// données "cachées" sur les établissements
		//

		// les données qui viennent du civil
		mr.registerDataExtractor(DonneesCiviles.class,
		                         null,
		                         e -> extractDonneesCiviles(e, mr, idMapper),
		                         null);

		// les données des dates des établissements stables
		mr.registerDataExtractor(DatesEtablissementsStables.class,
		                         null,
		                         e -> extractDatesEtablissementsStables(e, mr, idMapper),
		                         null);

		// les données des dates de domiciles stables (= plus ou moins l'intersection entre les domiciles et les établissements stables)
		mr.registerDataExtractor(DomicilesStables.class,
		                         null,
		                         e -> extractDomicilesStables(e, mr, idMapper),
		                         null);

		// données des mandats
		mr.registerDataExtractor(DonneesMandats.class,
		                         null,
		                         e -> extractDonneesMandats(e, mr, idMapper),
		                         null);
	}

	@NotNull
	private DomicilesStables extractDomicilesStables(RegpmEtablissement e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey key = buildEtablissementKey(e);
		return doInLogContext(key, mr, idMapper, () -> {

			// on va déjà chercher les domiciles valides
			final NavigableMap<RegDate, RegpmDomicileEtablissement> mapDomiciles;
			final SortedSet<RegpmDomicileEtablissement> domiciles = e.getDomicilesEtablissements();
			if (domiciles != null && !domiciles.isEmpty()) {
				// on trie les entités avant de les collecter en map afin de s'assurer que, à dates égales,
				// c'est le dernier qui aura raison...
				mapDomiciles = domiciles.stream()
						.filter(de -> !de.isRectifiee())
						.sorted()
						.collect(Collectors.toMap(RegpmDomicileEtablissement::getDateValidite, Function.identity(), (u, v) -> v, TreeMap::new));
			}
			else {
				mapDomiciles = Collections.emptyNavigableMap();
			}

			// on va également chercher les ranges d'établissements stables
			final List<DateRange> rangesStables = mr.getExtractedData(DatesEtablissementsStables.class, key).liste;

			// ... et finalement on mélange tout ça
			final Map<RegpmCommune, List<DateRange>> couverture = rangesStables.stream()
					.map(rangeStabilite -> {
						final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebut = mapDomiciles.floorEntry(rangeStabilite.getDateDebut());
						final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileFin = rangeStabilite.getDateFin() != null ? mapDomiciles.floorEntry(rangeStabilite.getDateFin()) : mapDomiciles.lastEntry();

						// si l'une ou l'autre des entrées est nulle, c'est que le range demandé est plus grand que le range couvert par les domiciles...
						if (domicileFin == null) {
							// fin == null -> il n'y a absolument rien qui couvre le range demandé
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR, String.format("L'établissement stable %s n'intersecte aucun domicile.", StringRenderers.DATE_RANGE_RENDERER.toString(rangeStabilite)));
							return Collections.<Pair<RegpmCommune, DateRange>>emptyList();
						}

						// début == null mais fin != null -> on a une intersection, mais pas complète ([SIFISC-16148] dans ce cas, on étend la validité du domicile pour qu'il couvre l'établissement stable complètement)
						final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebutEffectif;
						if (domicileDebut == null) {
							domicileDebutEffectif = mapDomiciles.ceilingEntry(rangeStabilite.getDateDebut());        // il y en a forcément un, puisque domicileFin != null
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
							              String.format("L'établissement stable %s n'est couvert par les domiciles qu'à partir du %s (on supposera donc le premier domicile déjà valide à la date de début de l'établissement stable).",
							                            StringRenderers.DATE_RANGE_RENDERER.toString(rangeStabilite), StringRenderers.DATE_RENDERER.toString(domicileDebutEffectif.getKey())));
						}
						else {
							domicileDebutEffectif = domicileDebut;
						}

						// s'il n'y a pas eu de changemenent de commune entre les deux dates, ces entrées sont normalement les mêmes
						// (comme je ne sais pas si les Map.Entry sont des constructions pour l'extérieur ou des externalisations de données internes, je préfère juste comparer la clé)
						if (domicileDebutEffectif.getKey() == domicileFin.getKey()) {
							return Collections.singletonList(Pair.<RegpmCommune, CollatableDateRange>of(domicileDebutEffectif.getValue().getCommune(), new DateRangeHelper.Range(rangeStabilite)));
						}
						else {
							// il y a eu changement de communes... il faut donc préparer plusieurs cas
							final List<Pair<RegpmCommune, DateRange>> liste = new LinkedList<>();
							RegDate cursor = rangeStabilite.getDateFin();
							for (Map.Entry<RegDate, RegpmDomicileEtablissement> step : mapDomiciles.subMap(domicileDebutEffectif.getKey(), true, domicileFin.getKey(), true).descendingMap().entrySet()) {
								final RegDate dateDebut;
								if (step.getKey() == domicileDebutEffectif.getKey()) {
									dateDebut = rangeStabilite.getDateDebut();
								}
								else {
									dateDebut = step.getKey();
								}
								liste.add(0, Pair.of(step.getValue().getCommune(), new DateRangeHelper.Range(dateDebut, cursor)));
								cursor = dateDebut.getOneDayBefore();
							}
							return liste;
						}
					})
					.flatMap(List::stream)
					.collect(Collectors.toMap(Pair::getLeft,
					                          pair -> Collections.singletonList(pair.getRight()),
					                          DATE_RANGE_LIST_MERGER));

			return new DomicilesStables(couverture);
		});
	}

	@NotNull
	private DonneesMandats extractDonneesMandats(RegpmEtablissement e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		return extractDonneesMandats(buildEtablissementKey(e), e.getMandants(), null, mr, LogCategory.ETABLISSEMENTS, idMapper);
	}

	/**
	 * Extraction de ce qui tiendra lieu de raison sociale pour l'établissement (= la concaténation des trois champs disponibles dans l'environnement pour ça)
	 * @param regpm établissement dont on veut trouver la raison sociale
	 * @return la raison sociale concaténée
	 */
	@Nullable
	private static String extractRaisonSociale(RegpmEtablissement regpm) {
		return extractRaisonSociale(regpm.getRaisonSociale1(), regpm.getRaisonSociale2(), regpm.getRaisonSociale3());
	}

	/**
	 * @param regpm un établissement
	 * @return le nom d'enseigne s'il existe, ou la raison sociale
	 */
	@Nullable
	private static String extractEnseigneOuRaisonSociale(RegpmEtablissement regpm) {
		if (StringUtils.isNotBlank(regpm.getEnseigne())) {
			return regpm.getEnseigne();
		}
		return extractRaisonSociale(regpm);
	}

	/**
	 * Calcul des dates des établissements stables en fusionnant les éventuels ranges qui se chevauchent
	 * @param e établissement de RegPM
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return la structure contenant les ranges des dates des établissements stables
	 */
	@NotNull
	private DatesEtablissementsStables extractDatesEtablissementsStables(RegpmEtablissement e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey key = buildEtablissementKey(e);
		return doInLogContext(key, mr, idMapper, () -> {
			final List<DateRange> dates = e.getEtablissementsStables().stream()
					.filter(etb -> {
						if (etb.getDateDebut() == null) {
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR,
							              String.format("Période d'établissement stable %d ignorée car sa date de début de validité est nulle (ou antérieure au 01.08.1291).", etb.getId().getSeqNo()));
							return false;
						}
						return true;
					})
					.filter(etb -> {
						if (isFutureDate(etb.getDateDebut())) {
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR,
							              String.format("Période d'établissement stable %d ignorée car sa date de début de validité est dans le futur (%s).",
							                            etb.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(etb.getDateDebut())));
							return false;
						}
						return true;
					})
					.map(etb -> {
						if (isFutureDate(etb.getDateFin())) {
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
							              String.format("Période d'établissement stable %d avec date de fin dans le futur %s : la migration ignore cette date.",
							                            etb.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(etb.getDateFin())));
							return new DateRangeHelper.Range(etb.getDateDebut(), null);
						}
						else {
							return new DateRangeHelper.Range(etb);
						}
					})
					.sorted(DateRangeComparator::compareRanges)
					.collect(Collectors.toList());
			return new DatesEtablissementsStables(DateRangeHelper.merge(dates));
		});
	}

	/**
	 * Retrouve l'établissement (= site) dont l'ID cantonal est donné dans l'organisation fournie
	 * @param org organisation complète
	 * @param cantonalId identifiant cantonal de l'établissement (= site) recherché
	 * @return l'établissement recherché, s'il existe
	 */
	@Nullable
	private static SiteOrganisation extractLocationInOrganisation(Organisation org, long cantonalId) {
		return org.getDonneesSites().stream()
				.filter(site -> site.getNumeroSite() == cantonalId)
				.findAny()
				.orElse(null);
	}

	/**
	 * @param etablissement un établissement de RegPM en cours de migration
	 * @return la clé vers l'entité juridique de l'établissement
	 */
	@Nullable
	private EntityKey getEntiteJuridiqueKey(RegpmEtablissement etablissement) {
		return getPolymorphicKey(etablissement::getEntreprise, null, etablissement::getIndividu);
	}

	/**
	 * Le contexte d'un établissement est aussi celui de son entité juridique...
	 * @param etablissement établissement en cours de migration
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @param action action à effectuer dans le contexte de log de l'établissement
	 * @param <T> type de la donnée renvoyée par l'action
	 * @return données renvoyée par l'action
	 */
	private <T> T doInCompleteLogContext(RegpmEtablissement etablissement, MigrationResultContextManipulation mr, IdMapping idMapper, Supplier<T> action) {
		final EntityKey etbKey = buildEtablissementKey(etablissement);
		final EntityKey entiteJuridiqueKey = getEntiteJuridiqueKey(etablissement);
		return doInLogContext(etbKey, mr, idMapper, () -> doInLogContext(entiteJuridiqueKey, mr, idMapper, action));
	}

	/**
	 * Appel de RCEnt pour les données de l'établissement
	 * @param etablissement établissement de RegPM
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @return les données civiles collectées (peut être <code>null</code> si ni l'établissement ni son entreprise n'a pas de pendant civil)
	 */
	private DonneesCiviles extractDonneesCiviles(RegpmEtablissement etablissement, MigrationResultContextManipulation mr, IdMapping idMapper) {
		return doInCompleteLogContext(etablissement, mr, idMapper, () -> {

			final Long idCantonal = etablissement.getNumeroCantonal();
			if (idCantonal == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, "Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.");
			}

			// on recherche également par l'entreprise, si jamais...
			final RegpmEntreprise entreprise = etablissement.getEntreprise();
			Organisation donneesEntreprise = null;
			if (entreprise != null) {
				final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
				final DonneesCiviles dce = mr.getExtractedData(DonneesCiviles.class, entrepriseKey);
				donneesEntreprise = dce != null ? dce.getOrganisation() : null;
			}

			// si j'ai un identifiant cantonal, je peux demander l'info à RCEnt
			SiteOrganisation donneesEtablissement = null;
			if (idCantonal != null) {

				// si j'ai déjà les données de l'entreprise, je vais directement chercher les données là...
				if (donneesEntreprise != null) {
					donneesEtablissement = extractLocationInOrganisation(donneesEntreprise, idCantonal);
				}
				else {
					// pas de données d'entreprise, donc il faut demander à RCEnt les données de l'entreprise en passant par l'établissement
					// (ici, on récupère sciemment une organisation partielle depuis le numéro d'établissement... et on rappelle ensuite pour
					// obtenir l'entreprise complète)
					try {
						final Long partielleCantonalId = organisationService.getOrganisationPourSite(idCantonal);
						if (partielleCantonalId == null) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Aucune donnée renvoyée par RCEnt pour cet établissement.");
						}
						else {
							mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Etablissement lié à l'organisation RCEnt %d.", partielleCantonalId));

							// on a une organisation partielle -> il faut rappeler RCEnt pour avoir une vue complète
							try {
								final Organisation complete = organisationService.getOrganisationHistory(partielleCantonalId);
								if (complete == null) {
									mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, String.format("Aucune donnée renvoyée par RCEnt pour l'organisation %d.", partielleCantonalId));
								}
								else {
									donneesEntreprise = complete;
									donneesEtablissement = extractLocationInOrganisation(complete, idCantonal);
								}
							}
							catch (Exception e) {
								mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, String.format("Erreur rencontrée lors de l'interrogation de RCEnt pour l'organisation %d.", partielleCantonalId));
								LOGGER.error("Exception lancée lors de l'interrogation de RCEnt pour l'organisation dont l'ID cantonal est " + partielleCantonalId, e);
							}
						}
					}
					catch (Exception e) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Erreur rencontrée lors de l'interrogation de RCEnt pour l'établissement.");
						LOGGER.error("Exception lancée lors de l'interrogation de RCEnt pour l'établissement dont l'ID cantonal est " + idCantonal, e);
					}
				}
			}

			// si on a des données, on les renvoie
			// on ne peut avoir des données d'établissement que si on a des données d'entreprise
			// il suffit donc de tester les données d'entreprise pour savoir si on a des données tout court
			return donneesEntreprise != null ? new DonneesCiviles(donneesEntreprise, donneesEtablissement) : null;
		});
	}

	/**
	 * Appelé pour la consolidation des données de fors secondaires par entité juridique
	 * @param data les données collectées pour une entité juridique
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	private void createForsSecondairesEtablissement(ForsSecondairesData.Activite data, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey keyEntiteJuridique = data.entiteJuridiqueSupplier.getKey();
		doInLogContext(keyEntiteJuridique, mr, idMapper, () -> {
			final Tiers entiteJuridique = data.entiteJuridiqueSupplier.get();
			for (Map.Entry<RegpmCommune, List<DateRange>> communeData : data.communes.entrySet()) {

				// TODO attention, dans le cas d'un individu, les fors secondaires peuvent devoir être créés sur un couple !!

				final RegpmCommune commune = communeData.getKey();
				if (commune.getCanton() != RegpmCanton.VD) {
					mr.addMessage(LogCategory.FORS, LogLevel.WARN,
					              String.format("Etablissement(s) sur la commune de %s (%d) sise dans le canton %s -> pas de for secondaire créé.",
					                            commune.getNom(),
					                            NO_OFS_COMMUNE_EXTRACTOR.apply(commune),
					                            commune.getCanton()));
				}
				else {
					final int noOfsCommune = NO_OFS_COMMUNE_EXTRACTOR.apply(commune);
					communeData.getValue().stream()
							.map(range -> {
								final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
								ffs.setDateDebut(range.getDateDebut());
								ffs.setDateFin(range.getDateFin());
								ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
								ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
								ffs.setNumeroOfsAutoriteFiscale(noOfsCommune);
								ffs.setMotifRattachement(MotifRattachement.ETABLISSEMENT_STABLE);
								ffs.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
								ffs.setMotifFermeture(range.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null);
								checkFractionCommuneVaudoise(ffs, mr, LogCategory.FORS);
								return ffs;
							})
							.map(ffs -> adapterAutourFusionsCommunes(ffs, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes))
							.flatMap(List::stream)
							.peek(ffs -> mr.addMessage(LogCategory.FORS, LogLevel.INFO,
							                           String.format("For secondaire 'activité' %s ajouté sur la commune %d.",
							                                         StringRenderers.DATE_RANGE_RENDERER.toString(ffs),
							                                         noOfsCommune)))
							.forEach(entiteJuridique::addForFiscal);
				}
			}
		});
	}

	private static Etablissement createEtablissement(RegpmEtablissement regpm) {
		final Etablissement unireg = new Etablissement();
		copyCreationMutation(regpm, unireg);
		return unireg;
	}

	@NotNull
	@Override
	protected EntityKey buildEntityKey(RegpmEtablissement entity) {
		return buildEtablissementKey(entity);
	}

	/**
	 * Point d'entrée de la migration à ce niveau (utile pour compléter le contexte de log avec les données de l'entité juridique parente également)
	 * @param regpm établissement de RegPM à migrer
	 * @param mr collecteur des messages de suivi et manipulateur de contexte de log
	 * @param linkCollector collecteur de liens entre entités
	 * @param idMapper mapper des identifiants regpm -> unireg
	 */
	@Override
	protected void doMigrate(RegpmEtablissement regpm, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		doInLogContext(getEntiteJuridiqueKey(regpm), mr, idMapper, () -> doMigrateEtablissement(regpm, mr, linkCollector, idMapper));
	}

	/**
	 * Implémentation effective de la migration
	 * @param regpm établissement de RegPM à migrer
	 * @param mr collecteur des messages de suivi et manipulateur de contexte de log
	 * @param linkCollector collecteur de liens entre entités
	 * @param idMapper mapper des identifiants regpm -> unireg
	 */
	private void doMigrateEtablissement(RegpmEtablissement regpm, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {

		// Attention, il y a des cas où on ne doit pas aveuglément créer un établissement
		// (quand l'établissement apparaît comme mandataire de deux entreprises présentes dans deux graphes distincts, par exemple,
		// ou suite à une reprise sur incident...)
		if (idMapper.hasMappingForEtablissement(regpm.getId())) {
			// l'établissement a déjà été migré rien à faire...
			return;
		}

		// on crée les liens vers l'entreprise ou l'individu avec les dates d'établissements stables
		final KeyedSupplier<? extends Contribuable> entiteJuridique = getPolymorphicSupplier(idMapper, regpm::getEntreprise, null, regpm::getIndividu);
		if (entiteJuridique == null) {
			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR, "Etablissement sans lien vers une entreprise ou un individu.");
		}
		else {
			// les éventuels immeubles (à transférer sur l'entité juridique parente)
			migrateImmeubles(regpm, entiteJuridique, mr);
		}

		// [SIFISC-16155] Un établissement sans dates de stabilité et qui n'est pas non plus mandataire n'est pas migré
		final KeyedSupplier<Etablissement> moi = getEtablissementSupplier(idMapper, regpm);
		final List<DateRange> datesEtablissementsStables = mr.getExtractedData(DatesEtablissementsStables.class, moi.getKey()).liste;
		final DonneesMandats donneesMandats = mr.getExtractedData(DonneesMandats.class, moi.getKey());
		final boolean isMandataire = donneesMandats.isMandataire();
		if (datesEtablissementsStables.isEmpty() && !isMandataire) {
			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, "Etablissement ignoré car sans établissement stable ni rôle de mandataire.");
			return;
		}

		// on crée forcément un nouvel établissement
		final Etablissement unireg = uniregStore.saveEntityToDb(createEtablissement(regpm));
		idMapper.addEtablissement(regpm, unireg);

		// récupération des données dans RCEnt
		SiteOrganisation rcent = null;
		if (rcentEnabled) {
			final DonneesCiviles donneesCiviles = mr.getExtractedData(DonneesCiviles.class, moi.getKey());
			if (donneesCiviles != null) {
				rcent = donneesCiviles.getSite();
			}
		}

		// les liens vers les individus (= activités indépendantes) doivent bien être pris en compte pour les mandataires, par exemple.
		// en revanche, cela ne signifie pas que l'on doivent aller remplir les graphes de départ avec les établissements d'individus
		// pour eux-mêmes (-> on ne traite les activités indépendantes "PP" que dans le cas où elles sont mandatrices de quelque chose...)

		// on crée les liens vers l'entreprise seulement avec les dates d'établissements stables si on en a, sinon avec les dates des rôles mandataires
		if (entiteJuridique != null) {

			// on ne crée un lien que vers les entreprises...
			if (entiteJuridique.getKey().getType() == EntityKey.Type.ENTREPRISE) {

				if (!datesEtablissementsStables.isEmpty()) {
					// création des liens (= rapports entre tiers)
					datesEtablissementsStables.stream()
							.map(range -> new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(moi, entiteJuridique, range.getDateDebut(), range.getDateFin(), false))
							.forEach(linkCollector::addLink);

					// génération de l'information pour la création des fors secondaires associés à ces établissements stables
					final Map<RegpmCommune, List<DateRange>> domicilesStables = mr.getExtractedData(DomicilesStables.class, moi.getKey()).map;
					enregistrerDemandesForsSecondaires(entiteJuridique, domicilesStables, mr);
				}
				else {
					mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, "Etablissement sans aucune période de validité d'un établissement stable (lien créé selon rôles de mandataire).");

					// un seul lien, avec les dates min et max des mandats (avec le rôle mandataire) repris (mais pas de for secondaire, du coup...)

					// il y a forcément des données (voir booléen isMandataire plus haut, qui est forcément "vrai" puisque si
					// nous sommes ici, c'est qu'il n'y a pas d'établissements stables...)
					final DateRange rangeMandats = extractMinMaxMandats(donneesMandats);
					if (rangeMandats == null) {
						throw new RuntimeException("Pourquoi n'y a-t-il pas de range ?");
					}

					// et le lien, pour finir
					linkCollector.addLink(new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(moi, entiteJuridique, rangeMandats.getDateDebut(), rangeMandats.getDateFin(), false));
				}
			}
			else {
				// l'entité juridique n'est pas une entreprise, on ne fait pas de lien (mais on le dit...)
				mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
				              String.format("Etablissement lié à l'entité %d de type %s : pas de lien créé.",
				                            entiteJuridique.getKey().getId(),
				                            entiteJuridique.getKey().getType()));
			}
		}

		// on ne fait rien des "succursales" d'un établissement, car il n'y en a aucune dans le modèle RegPM

		// adresse
		migrateAdresse(regpm, unireg, datesEtablissementsStables, donneesMandats, mr);

		// coordonnées financières
		final String titulaireCompte = extractEnseigneOuRaisonSociale(regpm);
		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, titulaireCompte, unireg, mr);
		migrateNotes(regpm.getNotes(), unireg);

		// données de base : enseigne, flag "principal" (aucun de ceux qui viennent de RegPM ne le sont, normalement)
		unireg.setEnseigne(regpm.getEnseigne());
		unireg.setRaisonSociale(extractRaisonSociale(regpm));
		unireg.setNumeroEtablissement(rcent != null ? rcent.getNumeroSite() : null);

		// domiciles de l'établissement
		migrateDomiciles(regpm, rcent, unireg, mr);

		// log de suivi à la fin des opérations pour cet établissement
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Etablissement migré : %s.", FormatNumeroHelper.numeroCTBToDisplay(unireg.getNumero())));
	}

	@Nullable
	private static DateRange extractMinMaxMandats(DonneesMandats donneesMandats) {

		// pour les dates min et max, il y a forcément des données (voir booléen isMandataire plus haut, qui est forcément "vrai" puisque si
		// nous sommes ici, c'est qu'il n'y a pas d'établissements stables...)
		// (la date min ne peut pas être nulle car de tels mandats sont ignorés -> un null signifie donc ici 'y en a pas!')
		final RegDate minMandat = donneesMandats.getRolesMandataire().stream()
				.map(RegpmMandat::getDateAttribution)
				.min(Comparator.naturalOrder())
				.orElse(null);

		// la date max peut être nulle, elle, en revanche, pour les mandats encore ouverts
		final RegDate maxMandat = donneesMandats.getRolesMandataire().stream()
				.max((mandat1, mandat2) -> NullDateBehavior.LATEST.compare(mandat1.getDateResiliation(), mandat2.getDateResiliation()))
				.get()
				.getDateResiliation();

		return minMandat == null ? null : new DateRangeHelper.Range(minMandat, maxMandat);
	}

	/**
	 * Migration de l'adresse de l'établissement
	 * @param regpm établissement de RegPM
	 * @param unireg établissement dans Unireg
	 * @param datesEtablissementsStables dates des établissements stables de l'établissement de RegPM
	 * @param donneesMandats données des mandats repris autour de l'établissement
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 */
	private void migrateAdresse(RegpmEtablissement regpm, Etablissement unireg,
	                            @NotNull List<DateRange> datesEtablissementsStables,
	                            @NotNull DonneesMandats donneesMandats,
	                            MigrationResultContextManipulation mr) {

		// d'abord, on détermine les dates auxquelles l'adresse est valide
		// 1. s'il y a des dates d'établissement stable, on prend le min et le max
		// 2. sinon, on prend les dates des mandats (min et max, pareil), rôle mandataire, s'ils existent
		// 3. sinon, pas d'adresse
		final DateRange rangeAdresse;

		if (datesEtablissementsStables.isEmpty()) {
			// extraction des dates depuis les mandats
			rangeAdresse = extractMinMaxMandats(donneesMandats);
		}
		else {
			// extraction des dates min et max
			final RegDate min = datesEtablissementsStables.get(0).getDateDebut();
			final RegDate max = datesEtablissementsStables.get(datesEtablissementsStables.size() - 1).getDateFin();
			rangeAdresse = new DateRangeHelper.Range(min, max);
		}

		// TODO usage de l'adresse = COURRIER ou plutôt DOMICILE ?
		final AdresseTiers adresse = adresseHelper.buildAdresse(regpm.getAdresse(rangeAdresse), mr, regpm.getChez(), false);
		if (adresse != null) {
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			unireg.addAdresseTiers(adresse);
		}
	}

	/**
	 * Les rattachements propriétaires (directs ou via groupe) des établissements (qui sont des erreurs de saisie dans RegPM, car les établissements n'ont
	 * pas la personalité juridique) doivent être loggués et transférés sur l'entreprise parente
	 * @param regpm l'établissement en cours de migration
	 * @param entiteJuridique l'entité juridique parente
	 * @param mr collecteur des messages de suivi et manipulateur de contexte de log
	 */
	private void migrateImmeubles(RegpmEtablissement regpm, KeyedSupplier<? extends Contribuable> entiteJuridique, MigrationResultProduction mr) {

		final Map<RegpmCommune, List<DateRange>> immeublesDirects = couvertureDepuisRattachementsProprietaires(regpm.getRattachementsProprietaires());
		final Map<RegpmCommune, List<DateRange>> immeublesGroupes = couvertureDepuisAppartenancesGroupeProprietaire(regpm.getAppartenancesGroupeProprietaire());

		// un peu de log d'abord et puis enregistrement dans les données de l'entité juridique
		if (!immeublesDirects.isEmpty()) {
			// log
			immeublesDirects.keySet().stream()
					.map(c -> String.format("Etablissement avec rattachement propriétaire direct sur la commune %s/%d.", c.getNom(), NO_OFS_COMMUNE_EXTRACTOR.apply(c)))
					.forEach(msg -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, msg));

			// enregistrement
			mr.addPreTransactionCommitData(new ForsSecondairesData.Immeuble(entiteJuridique, immeublesDirects));
		}
		if (!immeublesGroupes.isEmpty()) {
			// log
			immeublesGroupes.keySet().stream()
					.map(c -> String.format("Etablissement avec rattachement propriétaire (via groupe) sur la commune %s/%d.", c.getNom(), NO_OFS_COMMUNE_EXTRACTOR.apply(c)))
					.forEach(msg -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, msg));

			// enregistrement
			mr.addPreTransactionCommitData(new ForsSecondairesData.Immeuble(entiteJuridique, immeublesGroupes));
		}
	}

	/**
	 * Migration des domiciles de l'établissement
	 * @param regpm établissement RegPM
	 * @param rcent site correspondant connu dans RCEnt
	 * @param unireg établissement Unireg
	 * @param mr collecteur de messages de suivi
	 */
	private void migrateDomiciles(RegpmEtablissement regpm, SiteOrganisation rcent, Etablissement unireg, MigrationResultProduction mr) {

		// si le site est connu dans RCEnt, il faut reprendre les domiciles fiscaux seulement jusqu'à la date de la première donnée dans RCEnt
		final RegDate dateFinValiditeDonneesFiscales;
		if (rcent != null && rcent.getSieges() != null && !rcent.getSieges().isEmpty()) {
			dateFinValiditeDonneesFiscales = rcent.getSieges().stream()
					.map(Siege::getDateDebut)
					.min(Comparator.naturalOrder())
					.map(RegDate::getOneDayBefore)
					.get();

			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.INFO,
			              String.format("Présence de données civiles dès le %s, tous les domiciles stables ultérieurs de RegPM seront ignorés.",
			                            StringRenderers.DATE_RENDERER.toString(dateFinValiditeDonneesFiscales.getOneDayAfter())));
		}
		else {
			dateFinValiditeDonneesFiscales = null;
		}

		// communes de l'établissement (dans la base du mainframe au 12.05.2015, aucun établissement n'a plus d'un domicile non-rectifié)
		// -> en fait, il n'y a toujours qu'au plus une seule commune...

		final Map<RegpmCommune, List<DateRange>> domicilesStables = mr.getExtractedData(DomicilesStables.class, buildEtablissementKey(regpm)).map;
		final List<DomicileEtablissement> mappes = domicilesStables.entrySet().stream()
				.map(entry -> entry.getValue().stream().map(range -> Pair.of(entry.getKey(), range)))
				.flatMap(Function.identity())
				.filter(pair -> RegDateHelper.isAfterOrEqual(dateFinValiditeDonneesFiscales, pair.getRight().getDateDebut(), NullDateBehavior.LATEST))
				.map(pair -> {
					final RegpmCommune commune = pair.getLeft();
					final DateRange range = pair.getRight();

					final DomicileEtablissement domicile = new DomicileEtablissement();
					domicile.setDateDebut(range.getDateDebut());
					domicile.setDateFin(RegDateHelper.minimum(range.getDateFin(), dateFinValiditeDonneesFiscales, NullDateBehavior.LATEST));

					domicile.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
					domicile.setNumeroOfsAutoriteFiscale(NO_OFS_COMMUNE_EXTRACTOR.apply(commune));
					checkFractionCommuneVaudoise(domicile, mr, LogCategory.ETABLISSEMENTS);
					return domicile;
				})
				.sorted(Comparator.comparing(DomicileEtablissement::getDateDebut))
				.map(dom -> adapterAutourFusionsCommunes(dom, mr, LogCategory.ETABLISSEMENTS, null))
				.flatMap(List::stream)
				.collect(Collectors.toList());

		// log ou ajout des domiciles dans l'établissement...
		if (mappes.isEmpty() && dateFinValiditeDonneesFiscales == null) {
			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR, "Etablissement sans domicile stable.");
		}
		else {
			mappes.stream()
					.peek(domicile -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.INFO, String.format("Domicile : %s sur %s/%d.",
					                                                                                         StringRenderers.DATE_RANGE_RENDERER.toString(domicile),
					                                                                                         domicile.getTypeAutoriteFiscale(),
					                                                                                         domicile.getNumeroOfsAutoriteFiscale())))
					.forEach(unireg::addDomicile);
		}
	}

	private void enregistrerDemandesForsSecondaires(KeyedSupplier<? extends Tiers> entiteJuridique,
	                                                Map<RegpmCommune, List<DateRange>> domicilesStables,
	                                                MigrationResultProduction mr) {

		// s'il y a des données relatives à des fors secondaires, on les envoie...
		if (!domicilesStables.isEmpty()) {
			mr.addPreTransactionCommitData(new ForsSecondairesData.Activite(entiteJuridique, domicilesStables));
		}
	}
}
