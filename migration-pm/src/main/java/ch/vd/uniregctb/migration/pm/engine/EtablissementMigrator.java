package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
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
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
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

	private final RCEntAdapter rcEntAdapter;
	private final AdresseHelper adresseHelper;

	public EtablissementMigrator(UniregStore uniregStore, ActivityManager activityManager, ServiceInfrastructureService infraService,
	                             RCEntAdapter rcEntAdapter, AdresseHelper adresseHelper, FusionCommunesProvider fusionCommunesProvider, FractionsCommuneProvider fractionsCommuneProvider,
	                             DatesParticulieres datesParticulieres) {
		super(uniregStore, activityManager, infraService, fusionCommunesProvider, fractionsCommuneProvider, datesParticulieres);
		this.rcEntAdapter = rcEntAdapter;
		this.adresseHelper = adresseHelper;
	}

	private static List<Pair<RegpmCommune, CollatableDateRange>> buildPeriodesForsSecondaires(NavigableMap<RegDate, RegpmDomicileEtablissement> domicilesValides, DateRange range, MigrationResultProduction mr) {
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebut = domicilesValides.floorEntry(range.getDateDebut());
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileFin = range.getDateFin() != null ? domicilesValides.floorEntry(range.getDateFin()) : domicilesValides.lastEntry();

		// si l'une ou l'autre des entrées est nulle, c'est que le range demandé est plus grand que le range couvert par les domiciles...
		if (domicileFin == null) {
			// fin == null -> il n'y a absolument rien qui couvre le range demandé
			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR, String.format("L'établissement stable %s n'intersecte aucun domicile.", StringRenderers.DATE_RANGE_RENDERER.toString(range)));
			return Collections.emptyList();
		}

		// début == null mais fin != null -> on a une intersection, il faut donc raboter un peu
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebutEffectif;
		if (domicileDebut == null) {
			domicileDebutEffectif = domicilesValides.ceilingEntry(range.getDateDebut());        // il y en a forcément un, puisque domicileFin != null
			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, String.format("L'établissement stable %s n'est couvert par les domiciles qu'à partir du %s.",
			                                                                       StringRenderers.DATE_RANGE_RENDERER.toString(range), StringRenderers.DATE_RENDERER.toString(domicileDebutEffectif.getKey())));
		}
		else {
			domicileDebutEffectif = domicileDebut;
		}

		// s'il n'y a pas eu de changemenent de commune entre les deux dates, ces entrées sont normalement les mêmes
		// (comme je ne sais pas si les Map.Entry sont des constructions pour l'extérieur ou des externalisations de données internes, je préfère juste comparer la clé)
		if (domicileDebutEffectif.getKey() == domicileFin.getKey()) {
			final RegDate dateDebut = RegDateHelper.maximum(domicileDebutEffectif.getKey(), range.getDateDebut(), NullDateBehavior.EARLIEST);
			return Collections.singletonList(Pair.<RegpmCommune, CollatableDateRange>of(domicileDebutEffectif.getValue().getCommune(), new DateRangeHelper.Range(dateDebut, range.getDateFin())));
		}
		else {
			// il y a eu changement de communes... il faut donc préparer plusieurs cas
			final List<Pair<RegpmCommune, CollatableDateRange>> liste = new LinkedList<>();
			RegDate cursor = range.getDateFin();
			for (Map.Entry<RegDate, RegpmDomicileEtablissement> step : domicilesValides.subMap(domicileDebutEffectif.getKey(), true, domicileFin.getKey(), true).descendingMap().entrySet()) {
				final RegDate dateDebut = RegDateHelper.maximum(range.getDateDebut(), step.getKey(), NullDateBehavior.EARLIEST);
				liste.add(0, Pair.of(step.getValue().getCommune(), new DateRangeHelper.Range(dateDebut, cursor)));
				cursor = dateDebut.getOneDayBefore();
			}
			return liste;
		}
	}

	private static final class DatesEtablissementsStables {
		@NotNull
		final List<DateRange> liste;
		public DatesEtablissementsStables(List<DateRange> liste) {
			this.liste = liste == null ? Collections.emptyList() : liste;
		}
	}

	@Override
	public void initMigrationResult(MigrationResultInitialization mr, IdMapping idMapper) {
		super.initMigrationResult(mr, idMapper);

		// on va regrouper les données (communes et dates) par entité juridique afin de créer,
		// pour chacune d'entre elles, les fors secondaires "activité" qui vont bien
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.Activite.class,
		                                        MigrationConstants.PHASE_FORS_ACTIVITE,
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

		// données des mandats
		mr.registerDataExtractor(DonneesMandats.class,
		                         null,
		                         e -> extractDonneesMandats(e, mr, idMapper),
		                         null);
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
					.map(DateRangeHelper.Range::new)
					.map(range -> {
						if (isFutureDate(range.getDateFin())) {
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
							              String.format("Etablissement stable avec date de fin dans le futur %s : la migration ignore cette date.",
							                            StringRenderers.DATE_RENDERER.toString(range.getDateFin())));
							return new DateRangeHelper.Range(range.getDateDebut(), null);
						}
						else {
							return range;
						}
					})
					.sorted(DateRangeComparator::compareRanges)
					.collect(Collectors.toList());
			return new DatesEtablissementsStables(DateRangeHelper.merge(dates));
		});
	}

	/**
	 * Retrouve l'établissement (= location) dont l'ID cantonal est donné dans l'organisation fournie
	 * @param org organisation complète
	 * @param cantonalId identifiant cantonal de l'établissement (= location) recherché
	 * @return l'établissement recherché, s'il existe
	 */
	@Nullable
	private static OrganisationLocation extractLocationInOrganisation(Organisation org, long cantonalId) {
		return org.getLocationData().stream()
				.filter(ld -> ld.getCantonalId() == cantonalId)
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
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, "Pas de numéro cantonal assigné, pas de lien vers le civil.");
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
			OrganisationLocation donneesEtablissement = null;
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
						final Organisation partielle = rcEntAdapter.getLocation(idCantonal);
						if (partielle == null) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Aucune donnée renvoyée par RCEnt pour cet établissement.");
						}
						else {
							final long partielleCantonalId = partielle.getCantonalId();
							mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Etablissement lié à l'organisation RCEnt %d.", partielleCantonalId));

							// on a une organisation partielle -> il faut rappeler RCEnt pour avoir une vue complète
							try {
								final Organisation complete = rcEntAdapter.getOrganisation(partielleCantonalId);
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
				// TODO l'établissement principal doit-il générer un for secondaire ?

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
		// TODO à un moment, il faudra quand-même se demander comment cela se passe avec RCEnt, non ?

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

		// les liens vers les individus (= activités indépendantes) doivent bien être pris en compte pour les mandataires, par exemple.
		// en revanche, cela ne signifie pas que l'on doivent aller remplir les graphes de départ avec les établissements d'individus
		// pour eux-mêmes (-> on ne traite les activités indépendantes "PP" que dans le cas où elles sont mandatrices de quelque chose...)

		// on crée les liens vers l'entreprise ou l'individu avec les dates d'établissements stables
		if (entiteJuridique != null) {
			if (!datesEtablissementsStables.isEmpty()) {
				// création des liens (= rapports entre tiers)
				datesEtablissementsStables.stream()
						.map(range -> new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(moi, entiteJuridique, range.getDateDebut(), range.getDateFin()))
						.forEach(linkCollector::addLink);

				// génération de l'information pour la création des fors secondaires associés à ces établissements stables
				enregistrerDemandesForsSecondaires(entiteJuridique, regpm.getDomicilesEtablissements(), mr, datesEtablissementsStables);
			}
			else {
				mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, "Etablissement sans aucune période de validité d'un établissement stable (lien créé selon rôles de mandataire).");

				// un seul lien, avec les dates min et max des mandats (avec le rôle mandataire) repris

				// pour les dates min et max, il y a forcément des données (voir booléen isMandataire plus haut, qui est forcément "vrai" puisque si
				// nous sommes ici, c'est qu'il n'y a pas d'établissements stables...)
				// (la date min ne peut pas être nulle car de tels mandats sont ignorés)
				final RegDate minMandat = donneesMandats.getRolesMandataire().stream()
						.map(RegpmMandat::getDateAttribution)
						.min(Comparator.naturalOrder())
						.get();

				// (la date max peut être nulle, elle, en revanche, pour les mandats encore ouverts
				final RegDate maxMandat = donneesMandats.getRolesMandataire().stream()
						.max((mandat1, mandat2) -> NullDateBehavior.LATEST.compare(mandat1.getDateResiliation(), mandat2.getDateResiliation()))
						.get()
						.getDateResiliation();

				// et le lien, pour finir
				linkCollector.addLink(new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(moi, entiteJuridique, minMandat, maxMandat));
			}
		}

		// on ne fait rien des "succursales" d'un établissement, car il n'y en a aucune dans le modèle RegPM

		// adresse
		// TODO usage de l'adresse = COURRIER ou plutôt DOMICILE ?
		// TODO adresse permanente ou pas ?
		// TODO enseigne dans le complément d'adresse ?
		final AdresseTiers adresse = adresseHelper.buildAdresse(regpm.getAdresse(), mr, regpm::getEnseigne, false);
		if (adresse != null) {
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			unireg.addAdresseTiers(adresse);
		}

		// coordonnées financières
		final String titulaireCompte = extractEnseigneOuRaisonSociale(regpm);
		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, titulaireCompte, unireg, mr);

		// données de base : enseigne, flag "principal" (aucun de ceux qui viennent de RegPM ne le sont, normalement)
		unireg.setEnseigne(regpm.getEnseigne());
		unireg.setRaisonSociale(extractRaisonSociale(regpm));
		unireg.setPrincipal(false);
		unireg.setNumeroEtablissement(null);        // TODO à voir avec RCEnt

		// domiciles de l'établissement
		migrateDomiciles(regpm, unireg, mr);

		// log de suivi à la fin des opérations pour cet établissement
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Etablissement migré : %s.", FormatNumeroHelper.numeroCTBToDisplay(unireg.getNumero())));
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
	 * @param unireg établissement Unireg
	 * @param mr collecteur de messages de suivi
	 */
	private void migrateDomiciles(RegpmEtablissement regpm, Etablissement unireg, MigrationResultProduction mr) {
		// communes de l'établissement (dans la base du mainframe au 12.05.2015, aucun établissement n'a plus d'un domicile non-rectifié)
		// -> en fait, il n'y a toujours qu'au plus une seule commune...

		// création d'un domicile Unireg depuis un domicile RegPM
		final Function<RegpmDomicileEtablissement, DomicileEtablissement> mapper = d -> {
			final DomicileEtablissement domicile = new DomicileEtablissement();
			domicile.setDateDebut(d.getDateValidite());

			final RegpmCommune commune = d.getCommune();
			domicile.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
			domicile.setNumeroOfsAutoriteFiscale(NO_OFS_COMMUNE_EXTRACTOR.apply(commune));
			checkFractionCommuneVaudoise(domicile, mr, LogCategory.ETABLISSEMENTS);
			return domicile;
		};

		// liste des domiciles Unireg (sans dates de fin pour le moment, elles seront assignées juste après)
		final List<DomicileEtablissement> domiciles = regpm.getDomicilesEtablissements().stream()
				.filter(domicile -> !domicile.isRectifiee())
				.map(mapper)
				.sorted(Comparator.comparing(DomicileEtablissement::getDateDebut))
				.collect(Collectors.toList());

		// assignation des dates de fin (la dernière date de fin est par défaut à null, et sera mise à autre chose éventuellement lors de l'intersection
		// avec les dates des établissements stables plus bas)
		RegDate dateFinCourante = null;
		for (DomicileEtablissement domicile : CollectionsUtils.revertedOrder(domiciles)) {
			domicile.setDateFin(dateFinCourante);
			dateFinCourante = domicile.getDateDebut().getOneDayBefore();
		}

		// maintenant, on a des domiciles à mettre en regard des établissements stables
		// (en partie pour ajouter une date de fin au dernier domicile le cas échéant)
		final List<DateRange> etablissementsStables = mr.getExtractedData(DatesEtablissementsStables.class, buildEtablissementKey(regpm)).liste;
		final List<DomicileEtablissement> domicilesStables = domiciles.stream()
				.map(domicile -> Pair.of(domicile, DateRangeHelper.intersections(domicile, etablissementsStables)))     // intersection avec les établissements stables
				.filter(pair -> pair.getValue() != null && !pair.getValue().isEmpty())                      // filtrage des domiciles qui n'ont pas d'intersection avec les établissements stables
				.map(pair -> pair.getValue().stream().map(range -> Pair.of(pair.getKey(), range)))          // duplication en cas d'intersections disjointes
				.flatMap(Function.identity())
				.map(pair -> new DomicileEtablissement(pair.getValue().getDateDebut(),                      // ajustement des dates selon les dates d'intersection
				                                       pair.getValue().getDateFin(),
				                                       pair.getKey().getTypeAutoriteFiscale(),
				                                       pair.getKey().getNumeroOfsAutoriteFiscale(),
				                                       null))
				.map(dom -> adapterAutourFusionsCommunes(dom, mr, LogCategory.ETABLISSEMENTS, null))
				.flatMap(List::stream)
				.collect(Collectors.toList());

		// log ou ajout des domiciles dans l'établissement...
		if (domicilesStables.isEmpty()) {
			mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR, "Etablissement sans domicile.");
		}
		else {
			domicilesStables.stream()
					.peek(domicile -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.INFO, String.format("Domicile : %s sur %s/%d.",
					                                                                                         StringRenderers.DATE_RANGE_RENDERER.toString(domicile),
					                                                                                         domicile.getTypeAutoriteFiscale(),
					                                                                                         domicile.getNumeroOfsAutoriteFiscale())))
					.forEach(unireg::addDomicile);
		}
	}

	private void enregistrerDemandesForsSecondaires(KeyedSupplier<? extends Tiers> entiteJuridique,
	                                                Set<RegpmDomicileEtablissement> domiciles,
	                                                MigrationResultProduction mr,
	                                                Collection<DateRange> datesEtablissementsStables) {

		// les domiciles avec leurs dates d'établissement
		final NavigableMap<RegDate, RegpmDomicileEtablissement> mapDomiciles;
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

		// les informations, par communes, des périodes concernées
		final Map<RegpmCommune, List<DateRange>> mapFors = datesEtablissementsStables.stream()
				.map(range -> buildPeriodesForsSecondaires(mapDomiciles, range, mr))
				.flatMap(List::stream)
				.collect(Collectors.toMap(Pair::getKey,
				                          pair -> Collections.singletonList(pair.getValue()),
				                          DATE_RANGE_LIST_MERGER));

		// s'il y a des données relatives à des fors secondaires, on les envoie...
		if (!mapFors.isEmpty()) {
			mr.addPreTransactionCommitData(new ForsSecondairesData.Activite(entiteJuridique, mapFors));
		}
	}
}
