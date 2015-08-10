package ch.vd.uniregctb.migration.pm.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesCiviles;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAllegementFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeCollectivite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDemandeDelaiSommation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

public class EntrepriseMigrator extends AbstractEntityMigrator<RegpmEntreprise> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseMigrator.class);

	/**
	 * La valeur à mettre dans le champ "source" d'un état de DI retournée lors de la migration
	 */
	private static final String SOURCE_RETOUR_DI_MIGREE = "SDI";

	private final BouclementService bouclementService;
	private final AssujettissementService assujettissementService;
	private final RCEntAdapter rcEntAdapter;
	private final AdresseHelper adresseHelper;

	public EntrepriseMigrator(UniregStore uniregStore,
	                          ActivityManager activityManager,
	                          BouclementService bouclementService,
	                          AssujettissementService assujettissementService,
	                          RCEntAdapter rcEntAdapter,
	                          AdresseHelper adresseHelper) {
		super(uniregStore, activityManager);
		this.bouclementService = bouclementService;
		this.assujettissementService = assujettissementService;
		this.rcEntAdapter = rcEntAdapter;
		this.adresseHelper = adresseHelper;
	}

	private static Entreprise createEntreprise(RegpmEntreprise regpm) {
		final Entreprise unireg = new Entreprise(regpm.getId());
		copyCreationMutation(regpm, unireg);
		return unireg;
	}

	private static class ControleForsSecondairesData {
		private final Set<RegpmForSecondaire> regpm;
		private final KeyedSupplier<Entreprise> entrepriseSupplier;

		public ControleForsSecondairesData(Set<RegpmForSecondaire> regpm, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.regpm = regpm;
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static class CouvertureForsData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public CouvertureForsData(KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static class ComparaisonAssujettissementsData {
		private final SortedSet<RegpmAssujettissement> regpmAssujettissements;
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public ComparaisonAssujettissementsData(SortedSet<RegpmAssujettissement> regpmAssujettissements, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.regpmAssujettissements = regpmAssujettissements;
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static class ForsPrincipauxData {
		@NotNull
		private final List<RegpmForPrincipal> liste;
		public ForsPrincipauxData(@Nullable List<RegpmForPrincipal> liste) {
			this.liste = liste == null ? Collections.emptyList() : liste;
		}
	}

	private static class DossiersFiscauxData {
		@NotNull
		private final List<RegpmDossierFiscal> liste;
		public DossiersFiscauxData(@Nullable List<RegpmDossierFiscal> liste) {
			this.liste = liste == null ? Collections.emptyList() : liste;
		}
	}

	@Override
	public void initMigrationResult(MigrationResultInitialization mr) {
		super.initMigrationResult(mr);
		
		//
		// callbacks avant la fin des transactions
		//

		// consolidation pour la constitution des fors "immeuble"
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.Immeuble.class,
		                                        MigrationConstants.PHASE_FORS_IMMEUBLES,
		                                        k -> k.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData.Immeuble(d1.entiteJuridiqueSupplier, DATE_RANGE_MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondairesImmeuble(d, mr));

		// callback pour le contrôle des fors secondaires après création
		mr.registerPreTransactionCommitCallback(ControleForsSecondairesData.class,
		                                        MigrationConstants.PHASE_CONTROLE_FORS_SECONDAIRES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleForsSecondaires(d, mr));

		// callback pour le contrôle (et la correction) de la couverture des fors secondaires par des fors principaux
		mr.registerPreTransactionCommitCallback(CouvertureForsData.class,
		                                        MigrationConstants.PHASE_COUVERTURE_FORS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleCouvertureFors(d, mr));

		// callback pour le contrôle des données d'assujettissement
		mr.registerPreTransactionCommitCallback(ComparaisonAssujettissementsData.class,
		                                        MigrationConstants.PHASE_COMPARAISON_ASSUJETTISSEMENTS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> comparaisonAssujettissements(d, mr));
		
		//
		// données "cachées" sur les entreprises
		//
		
		// les fors principaux non-ignorés
		mr.registerDataExtractor(ForsPrincipauxData.class,
		                         e -> extractForsPrincipaux(e, mr),
		                         null,
		                         null);

		// les données qui viennent du civil
		mr.registerDataExtractor(DonneesCiviles.class,
		                         e -> extractDonneesCiviles(e, mr),
		                         null,
		                         null);

		// les dossiers fiscaux non-ignorés
		mr.registerDataExtractor(DossiersFiscauxData.class,
		                         e -> extractDossiersFiscaux(e, mr),
		                         null,
		                         null);
	}

	/**
	 * Extraction des dossiers fiscaux valides d'une entreprise
	 * @param e entreprise de RegPM
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @return les dossiers fiscaux de l'entreprise
	 */
	@NotNull
	private DossiersFiscauxData extractDossiersFiscaux(RegpmEntreprise e, MigrationResultContextManipulation mr) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		return doInLogContext(entrepriseKey, mr, () -> {

			final List<RegpmDossierFiscal> liste = e.getDossiersFiscaux().stream()
					.filter(df -> {
						if (df.getModeImposition() != RegpmModeImposition.POST) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
							              String.format("Le dossier fiscal de la PF %d en mode %s est ignoré dans la migration.", df.getPf(), df.getModeImposition()));
							return false;
						}
						return true;
					})
					.collect(Collectors.toList());

			return new DossiersFiscauxData(liste);
		});
	}

	/**
	 * Appel de RCEnt pour les données de l'entreprise
	 * @param e entreprise de RegPM
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @return les données civiles collectées (peut être <code>null</code> si l'entreprise n'a pas de pendant civil)
	 */
	@Nullable
	private DonneesCiviles extractDonneesCiviles(RegpmEntreprise e, MigrationResultContextManipulation mr) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		return doInLogContext(entrepriseKey, mr, () -> {

			final Long idCantonal = e.getNumeroCantonal();
			if (idCantonal == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, "Pas de numéro cantonal assigné, pas de lien vers le civil.");
				return null;
			}

			try {
				final Organisation org = rcEntAdapter.getOrganisation(idCantonal);
				if (org != null) {
					return new DonneesCiviles(org);
				}

				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Aucune donnée renvoyée par RCEnt pour cette entreprise.");
			}
			catch (Exception ex) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Erreur rencontrée lors de l'interrogation de RCEnt pour l'entreprise.");
				LOGGER.error("Exception lancée lors de l'interrogation de RCEnt pour l'entreprise dont l'ID cantonal est " + idCantonal, ex);
			}

			// rien trouvé -> on ignore les erreurs RCEnt
			return null;
		});
	}

	/**
	 * Extraction des fors principaux valides d'une entreprise de RegPM (en particulier, on blinde le
	 * cas de fors multiples à la même date ete des fors sans date de début)
	 * @param regpm l'entreprise cible
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @return un container des fors principaux valides de l'entreprise
	 */
	@NotNull
	private ForsPrincipauxData extractForsPrincipaux(RegpmEntreprise regpm, MigrationResultContextManipulation mr) {
		final EntityKey entrepriseKey = buildEntrepriseKey(regpm);
		return doInLogContext(entrepriseKey, mr, () -> {

			final Map<RegDate, List<RegpmForPrincipal>> forsParDate = regpm.getForsPrincipaux().stream()
					.collect(Collectors.toMap(RegpmForPrincipal::getDateValidite,
					                          Collections::singletonList,
					                          (f1, f2) -> Stream.concat(f1.stream(), f2.stream()).collect(Collectors.toList())));

			final List<RegpmForPrincipal> liste = forsParDate.entrySet().stream()
					.filter(entry -> !entry.getValue().isEmpty())
					.map(entry -> {
						final List<RegpmForPrincipal> fors = entry.getValue();
						if (fors.size() > 1) {

							// si les fors représentent le même pays ou la même commune, on prend le premier
							final Map<CommuneOuPays, List<RegpmForPrincipal>> parLocalisation = fors.stream()
									.collect(Collectors.toMap(CommuneOuPays::new,
									                          Collections::singletonList,
									                          (f1, f2) -> Stream.concat(f1.stream(), f2.stream()).collect(Collectors.toList()),
									                          LinkedHashMap::new));     // on veut conserver l'ordre des localisation pour les tests
							if (parLocalisation.size() == 1) {
								mr.addMessage(LogCategory.FORS, LogLevel.INFO,
								              String.format("Plusieurs (%d) fors principaux sur la même autorité fiscale (%s) ont une date de début identique au %s : seul le premier sera pris en compte.",
								                            fors.size(),
								                            parLocalisation.keySet().iterator().next(),
								                            StringRenderers.DATE_RENDERER.toString(entry.getKey())));

								return fors.get(0);
							}

							// si les fors sont de même type, on a effectivement une erreur (et on ne garde que le dernier pour ce run de migration)
							// sinon, on ne considère que ceux qui sont en administration effective (s'il y en a plusieurs, c'est aussi une erreur
							// et on ne garde que le dernier d'entre eux dans ce run de migration)
							final Map<RegpmTypeForPrincipal, List<RegpmForPrincipal>> parType = fors.stream()
									.collect(Collectors.toMap(RegpmForPrincipal::getType,
									                          Collections::singletonList,
									                          (f1, f2) -> Stream.concat(f1.stream(), f2.stream()).collect(Collectors.toList()),
									                          () -> new EnumMap<>(RegpmTypeForPrincipal.class)));

							// tous de même type = taille de la map == 1
							if (parType.size() == 1) {
								mr.addMessage(LogCategory.FORS, LogLevel.ERROR,
								              String.format("Plusieurs (%d) fors principaux de même type (%s) mais sur des autorités fiscales différentes (%s) ont une date de début identique au %s : seul le dernier sera pris en compte.",
								                            fors.size(),
								                            parType.keySet().iterator().next(),
								                            toDisplayString(parLocalisation.keySet(), Object::toString),
								                            StringRenderers.DATE_RENDERER.toString(entry.getKey())));
							}
							else {

								// les deux types sont donc représentés, c'est l'administration effective qui gagne

								parType.entrySet().stream()
										.filter(parTypeEntry -> parTypeEntry.getKey() != RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE)
										.map(Map.Entry::getValue)
										.flatMap(List::stream)
										.map(ff -> Pair.of(ff.getId().getSeqNo(), new CommuneOuPays(ff)))
										.forEach(pair -> mr.addMessage(LogCategory.FORS, LogLevel.WARN,
										                               String.format(
												                               "For fiscal principal %d %s ignoré en raison de la présence à la même date (%s) d'un for fiscal principal différent de type %s.",
												                               pair.getLeft(),
												                               pair.getRight(),
												                               StringRenderers.DATE_RENDERER.toString(entry.getKey()),
												                               RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE)));

								final List<RegpmForPrincipal> forsAdministrationEffective = parType.get(RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE);
								if (forsAdministrationEffective.size() > 1) {

									final Map<CommuneOuPays, List<RegpmForPrincipal>> admEffectiveParLocalisation = forsAdministrationEffective.stream()
											.collect(Collectors.toMap(CommuneOuPays::new,
											                          Collections::singletonList,
											                          (f1, f2) -> Stream.concat(f1.stream(), f2.stream()).collect(Collectors.toList()),
											                          LinkedHashMap::new));         // on veut conserver l'ordre des localisation pour les tests

									// tous au même endroit ou pas ?
									if (admEffectiveParLocalisation.size() == 1) {

										// tous au même endroit -> on prend le premier
										mr.addMessage(LogCategory.FORS, LogLevel.INFO,
										              String.format("Plusieurs (%d) fors principaux de type %s et sur la même autorité fiscale (%s) ont une date de début identique au %s : seul le premier sera pris en compte.",
										                            fors.size(),
										                            RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE,
										                            admEffectiveParLocalisation.keySet().iterator().next(),
										                            StringRenderers.DATE_RENDERER.toString(entry.getKey())));

										return forsAdministrationEffective.get(0);
									}

									mr.addMessage(LogCategory.FORS, LogLevel.ERROR,
									              String.format("Plusieurs (%d) fors principaux de type %s sur des autorités fiscales différentes (%s) ont une date de début identique au %s : seul le dernier sera pris en compte.",
									                            forsAdministrationEffective.size(),
									                            RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE,
									                            toDisplayString(admEffectiveParLocalisation.keySet(), Object::toString),
									                            StringRenderers.DATE_RENDERER.toString(entry.getKey())));
								}

								// on ne considère que le dernier, comme indiqué dans le message, et comme
								// dans le set d'entrée, les fors sont triés... (à date égale, par numéro de séquence)
								// -> le dernier de la liste ici est celui que l'on veut conserver
								return forsAdministrationEffective.get(forsAdministrationEffective.size() - 1);
							}
						}

						// dans le set d'entrée, les fors sont triés... (à date égale, par numéro de séquence)
						// -> le dernier de la liste ici est celui que l'on veut conserver
						return fors.get(fors.size() - 1);
					})
					.filter(ff -> {
						if (ff.getDateValidite() == null) {
							mr.addMessage(LogCategory.FORS, LogLevel.WARN,
							              String.format("Le for principal %d est ignoré car il a une date de début nulle.", ff.getId().getSeqNo()));
							return false;
						}
						return true;
					})
					.collect(Collectors.toList());

			return new ForsPrincipauxData(liste);
		});
	}

	/**
	 * @param data données d'identification de l'entreprise dont on veut contrôler l'assujettissement
	 * @param mr collecteur de message de suivi et manipulateur de contexte de log
	 */
	private void comparaisonAssujettissements(ComparaisonAssujettissementsData data, MigrationResultContextManipulation mr) {
		final EntityKey keyEntreprise = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntreprise, mr, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();
			try {
				// assujettissements ICC dans RegPM
				final List<DateRange> lilic = data.regpmAssujettissements.stream()
						.filter(a -> a.getType() == RegpmTypeAssujettissement.LILIC)
						.map(a -> new DateRangeHelper.Range(a.getDateDebut(), a.getDateFin()))
						.collect(Collectors.toList());
				final List<DateRange> lilicIntersectant = new ArrayList<>(lilic.size());

				// assujettissements calculés par Unireg
				final List<Assujettissement> calcules = Optional.ofNullable(assujettissementService.determine(entreprise)).orElse(Collections.emptyList());
				final List<Assujettissement> calculesIntersectant = new ArrayList<>(calcules.size());

				// assujettissements complètement apparus
				for (Assujettissement apparu : calcules) {
					if (DateRangeHelper.intersect(apparu, lilic)) {
						calculesIntersectant.add(apparu);
					}
					else {
						mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.WARN,
						              String.format("Nouvelle période d'assujettissement apparue : %s.", StringRenderers.DATE_RANGE_RENDERER.toString(apparu)));
					}
				}

				// assujettissements complètement disparus
				for (DateRange disparu : lilic) {
					if (DateRangeHelper.intersect(disparu, calcules)) {
						lilicIntersectant.add(disparu);
					}
					else {
						mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.WARN,
						              String.format("Ancienne période d'assujettissement disparue : %s.", StringRenderers.DATE_RANGE_RENDERER.toString(disparu)));
					}
				}

				// les cas qui s'intersectent
				if (!lilicIntersectant.isEmpty() || !calculesIntersectant.isEmpty()) {
					if (!sameDateRanges(lilicIntersectant, calculesIntersectant)) {
						mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.WARN,
						              String.format("Période(s) d'assujettissement modifiée(s) : avant (%s) et après (%s).",
						                            toDisplayString(lilicIntersectant),
						                            toDisplayString(calculesIntersectant)));
					}
				}
			}
			catch (AssujettissementException e) {
				mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.ERROR, "Erreur rencontrée lors du calcul de l'assujettissement de l'entreprise.");
				LOGGER.error("Exception lancée lors du calcul de l'assujettissement de l'entreprise " + entreprise.getNumero(), e);
			}
		});
	}

	/**
	 * @param data donnée d'identification de l'entreprise dont la couverture des fors est à contrôler
	 * @param mr collecteur de message de suivi et manipulateur de contexte de log
	 */
	private void controleCouvertureFors(CouvertureForsData data, MigrationResultContextManipulation mr) {
		final EntityKey keyEntreprise = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntreprise, mr, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();
			final ForsParType fpt = entreprise.getForsParType(true);
			if (!fpt.secondaires.isEmpty()) {

				// récupération des périodes couvertes par des fors secondaires
				final List<DateRange> fs = DateRangeHelper.merge(fpt.secondaires);

				// récupération des périodes couvertes par les fors principaux
				final List<? extends DateRange> fp = entreprise.getForsFiscauxPrincipauxActifsSorted();

				// périodes des fors secondaires non-couvertes par les fors principaux ?
				final List<DateRange> rangesNonCouverts = DateRangeHelper.subtract(fs, fp, new DateRangeAdapterCallback());
				if (rangesNonCouverts != null && !rangesNonCouverts.isEmpty()) {

					// la règle dit d'étendre le prochain for principal trouvé (= celui juste après le trou)
					// afin qu'il couvre le trou (s'il n'y en a pas après, on prendra celui d'avant)
					// (dans le cas où il n'y a ni for avant, ni for après le trou, c'est qu'il n'y a pas de for principal
					// du tout, et on fait un for pays inconnu...)

					// y a-t-il des fors principaux ?
					if (fp == null || fp.isEmpty()) {
						// non -> aucun, on va créer des fors "pays inconnu"
						rangesNonCouverts.stream()
								.map(range -> new ForFiscalPrincipalPM(range.getDateDebut(),
								                                       MotifFor.INDETERMINE,
								                                       range.getDateFin(),
								                                       range.getDateFin() != null ? MotifFor.INDETERMINE : null,
								                                       ServiceInfrastructureService.noPaysInconnu,
								                                       TypeAutoriteFiscale.PAYS_HS,
								                                       MotifRattachement.DOMICILE))
								.peek(ff -> ff.setGenreImpot(GenreImpot.BENEFICE_CAPITAL))
								.peek(ff -> mr.addMessage(LogCategory.FORS, LogLevel.WARN,
								                          String.format("Création d'un for principal 'bouche-trou' %s pour couvrir les fors secondaires.",
								                                        StringRenderers.DATE_RANGE_RENDERER.toString(ff))))
								.forEach(entreprise::addForFiscal);
					}
					else {
						// il y a des fors principaux, donc il y a toujours au moins un for principal
						// avant ou après chaque trou... (pas forcément juste avant ou juste après car la période des ranges non-couverts
						// ne concerne que les fors secondaires...)

						final NavigableMap<RegDate, ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted()
								.stream()
								.collect(Collectors.toMap(ForFiscalPrincipalPM::getDateDebut,
								                          Function.identity(),
								                          (f1, f2) -> { throw new IllegalArgumentException("Plusieurs fors principaux commençant le même jour ???"); },
								                          TreeMap::new));

						for (DateRange nonCouvert : rangesNonCouverts) {

							final RegDate dateFinTrou = nonCouvert.getDateFin();
							final ForFiscalPrincipalPM forApresTrou = dateFinTrou == null ? null : forsPrincipaux.higherEntry(dateFinTrou).getValue();
							if (forApresTrou != null) {
								// on change la date de début pour couvrir le trou
								mr.addMessage(LogCategory.FORS, LogLevel.WARN,
								              String.format("La date de début du for fiscal principal %s est adaptée (-> %s) pour couvrir les fors secondaires.",
								                            StringRenderers.DATE_RANGE_RENDERER.toString(forApresTrou),
								                            StringRenderers.DATE_RENDERER.toString(nonCouvert.getDateDebut())));

								// il ne suffit pas de le dire, il faut le faire...
								forApresTrou.setDateDebut(nonCouvert.getDateDebut());
							}
							else {
								// TODO il y a un souci, non ?
								// s'il n'y a pas de for principal après, c'est que le dernier for principal est fermé
								// mais s'il est fermé, c'est que l'entreprise est dissoute, non ?
								// comment, dans ce cas, peut-il encore y avoir des fors secondaires actifs après la dissolution ?

								// il n'y a pas de for après, on regarde avant...
								final RegDate dateDebutTrou = nonCouvert.getDateDebut();
								final ForFiscalPrincipalPM forAvantTrou = dateDebutTrou == null ? null : forsPrincipaux.lowerEntry(dateDebutTrou).getValue();
								if (forAvantTrou != null) {
									// on change la date de fin pour couvrir le trou
									mr.addMessage(LogCategory.FORS, LogLevel.WARN,
									              String.format("La date de fin du for fiscal principal %s est adaptée (-> %s) pour couvrir les fors secondaires.",
									                            StringRenderers.DATE_RANGE_RENDERER.toString(forAvantTrou),
									                            StringRenderers.DATE_RENDERER.toString(nonCouvert.getDateFin())));

									// là non plus, il ne suffit pas de le dire...
									forAvantTrou.setDateFin(nonCouvert.getDateFin());
								}
							}
						}
					}

					// on va forcer le re-calcul des motifs
					calculeMotifsOuvertureFermeture(entreprise.getForsFiscauxPrincipauxActifsSorted());
				}
			}
		});
	}

	/**
	 * Consolidation de toutes les migrations de PM par rapport au contrôle final des fors secondaires
	 * @param data données de l'entreprise
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 */
	private void controleForsSecondaires(ControleForsSecondairesData data, MigrationResultContextManipulation mr) {
		final EntityKey keyEntiteJuridique = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntiteJuridique, mr, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();

			// on va construire des périodes par commune (no OFS), et vérifier qu'on a bien les mêmes des deux côtés
			final Map<Integer, List<DateRange>> avantMigration = data.regpm.stream()
					.collect(Collectors.toMap(fs -> NO_OFS_COMMUNE_EXTRACTOR.apply(fs.getCommune()),
					                          Collections::singletonList,
					                          DATE_RANGE_LIST_MERGER,
					                          TreeMap::new));
			final Set<ForFiscal> forsFiscaux = Optional.ofNullable(entreprise.getForsFiscaux()).orElse(Collections.emptySet());    // en cas de nouvelle entreprise, la collection est nulle
			final Map<Integer, List<DateRange>> apresMigration = forsFiscaux.stream()
					.filter(f -> f instanceof ForFiscalSecondaire)
					.collect(Collectors.toMap(ForFiscal::getNumeroOfsAutoriteFiscale, Collections::singletonList, DATE_RANGE_LIST_MERGER, TreeMap::new));

			// rien avant, rien après, pas la peine de continuer...
			if (avantMigration.isEmpty() && apresMigration.isEmpty()) {
				return;
			}

			// des communes présentes d'un côté et pas du tout de l'autre ?
			final Set<Integer> ofsSeulementAvant = avantMigration.keySet().stream().filter(ofs -> !apresMigration.containsKey(ofs)).collect(Collectors.toCollection(LinkedHashSet::new));
			final Set<Integer> ofsSeulementApres = apresMigration.keySet().stream().filter(ofs -> !avantMigration.containsKey(ofs)).collect(Collectors.toCollection(LinkedHashSet::new));
			if (!ofsSeulementAvant.isEmpty()) {
				for (Integer ofs : ofsSeulementAvant) {
					mr.addMessage(LogCategory.FORS, LogLevel.WARN,
					              String.format("Il n'y a plus de fors secondaires sur la commune OFS %d (avant : %s).", ofs, toDisplayString(avantMigration.get(ofs))));
				}
			}
			if (!ofsSeulementApres.isEmpty()) {
				for (Integer ofs : ofsSeulementApres) {
					mr.addMessage(LogCategory.FORS, LogLevel.WARN,
					              String.format("Il n'y avait pas de fors secondaires sur la commune OFS %d (maintenant : %s).", ofs, toDisplayString(apresMigration.get(ofs))));
				}
			}

			// et sur les communes effectivement en commun, il faut comparer les périodes
			avantMigration.keySet().stream()
					.filter(apresMigration::containsKey)
					.forEach(ofs -> {
						final List<DateRange> rangesAvant = avantMigration.get(ofs);
						final List<DateRange> rangesApres = apresMigration.get(ofs);
						if (!sameDateRanges(rangesAvant, rangesApres)) {
							mr.addMessage(LogCategory.FORS, LogLevel.WARN,
							              String.format("Sur la commune OFS %d, la couverture des fors secondaires n'est plus la même : avant (%s) et après (%s).",
							                            ofs, toDisplayString(rangesAvant), toDisplayString(rangesApres)));
						}
					});
		});
	}

	/**
	 * @param list une liste de ranges
	 * @return une représentation String de cette liste
	 */
	private static String toDisplayString(List<? extends DateRange> list) {
		return toDisplayString(list, StringRenderers.DATE_RANGE_RENDERER);
	}

	/**
	 * @param list une collection d'élément
	 * @param renderer un renderer capable de fournir une chaîne de caractère pour chaque élément de la liste
	 * @param <T> le type des éléments dans la liste
	 * @return une représentation String de cette liste
	 */
	private static <T> String toDisplayString(Collection<T> list, StringRenderer<? super T> renderer) {
		return list.stream().map(renderer::toString).collect(Collectors.joining(", "));
	}

	/**
	 * @param regpm une PM
	 * @return la date de début de l'activité de la PM en question
	 */
	private static RegDate getDateDebutActivite(RegpmEntreprise regpm, MigrationResultProduction mr) {
		// TODO faut-il bien prendre la date de début du premier for principal ???
		return mr.getExtractedData(ForsPrincipauxData.class, buildEntrepriseKey(regpm)).liste.stream()
				.map(RegpmForPrincipal::getDateValidite)
				.min(NullDateBehavior.LATEST::compare)
				.orElse(null);
	}

	/**
	 * @param regpm une PM
	 * @return la date de fin de l'activité de la PM en question (<code>null</code> si la PM est toujours active...)
	 */
	private static RegDate getDateFinActivite(RegpmEntreprise regpm) {
		// TODO est-ce vraiment la date de fin d'activité de la PM (opionnelle, évidemment) ?
		return regpm.getDateFinFiscale();
	}

	/**
	 * Les listes de ranges en entrée sont supposés triés
	 * @param l1 une liste de ranges
	 * @param l2 une autre liste de ranges
	 * @return <code>true</code> si les listes contiennent les mêmes plages de dates
	 */
	private static boolean sameDateRanges(List<? extends DateRange> l1, List<? extends DateRange> l2) {
		boolean same = l1.size() == l2.size();
		if (same) {
			for (Iterator<? extends DateRange> i1 = l1.iterator(), i2 = l2.iterator(); i1.hasNext() && i2.hasNext() && same; ) {
				final DateRange r1 = i1.next();
				final DateRange r2 = i2.next();
				same = DateRangeHelper.equals(r1, r2);
			}
		}
		return same;
	}

	/**
	 * Consolidation de toutes les demandes de créations de fors secondaires "immeuble" pour une PM
	 * @param data les données consolidées des communes/dates sur lesquels les fors doivent être créés
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 */
	private void createForsSecondairesImmeuble(ForsSecondairesData.Immeuble data, MigrationResultContextManipulation mr) {
		final EntityKey keyEntiteJuridique = data.entiteJuridiqueSupplier.getKey();
		doInLogContext(keyEntiteJuridique, mr, () -> {
			final Tiers entiteJuridique = data.entiteJuridiqueSupplier.get();
			for (Map.Entry<RegpmCommune, List<DateRange>> communeData : data.communes.entrySet()) {

				final RegpmCommune commune = communeData.getKey();
				if (commune.getCanton() != RegpmCanton.VD) {
					mr.addMessage(LogCategory.FORS, LogLevel.WARN,
					              String.format("Immeuble(s) sur la commune de %s (%d) sise dans le canton %s -> pas de for secondaire créé.",
					                            commune.getNom(),
					                            NO_OFS_COMMUNE_EXTRACTOR.apply(commune),
					                            commune.getCanton()));
				}
				else {
					final Integer noOfsCommune = NO_OFS_COMMUNE_EXTRACTOR.apply(commune);
					for (DateRange dates : communeData.getValue()) {
						final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
						ffs.setDateDebut(dates.getDateDebut());
						ffs.setDateFin(dates.getDateFin());
						ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
						ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
						ffs.setNumeroOfsAutoriteFiscale(noOfsCommune);
						ffs.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
						ffs.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
						ffs.setMotifFermeture(dates.getDateFin() != null ? MotifFor.VENTE_IMMOBILIER : null);
						ffs.setTiers(entiteJuridique);
						entiteJuridique.addForFiscal(ffs);

						mr.addMessage(LogCategory.FORS, LogLevel.INFO, String.format("For secondaire 'immeuble' %s ajouté sur la commune %d.",
						                                                             StringRenderers.DATE_RANGE_RENDERER.toString(dates),
						                                                             noOfsCommune));
					}
				}
			}
		});
	}

	@NotNull
	@Override
	protected EntityKey buildEntityKey(RegpmEntreprise entity) {
		return buildEntrepriseKey(entity);
	}

	@Override
	protected void doMigrate(RegpmEntreprise regpm, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {

		if (idMapper.hasMappingForEntreprise(regpm.getId())) {
			// l'entreprise a déjà été migrée... pas la peine d'aller plus loin, ou bien ? <- Genevois
			return;
		}

		// Les entreprises conservent leur numéro comme numéro de contribuable
		Entreprise unireg = uniregStore.getEntityFromDb(Entreprise.class, regpm.getId());
		if (unireg == null) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.");
			unireg = uniregStore.saveEntityToDb(createEntreprise(regpm));
		}
		idMapper.addEntreprise(regpm, unireg);

		final KeyedSupplier<Entreprise> moi = new KeyedSupplier<>(buildEntrepriseKey(regpm), getEntrepriseByUniregIdSupplier(unireg.getId()));

		// Récupération des données civiles si elles existent
		Organisation rcent = null;
		// TODO à réactiver quand on se branchera vraiment sur RCEnt
		if (false) {
			final DonneesCiviles donneesCiviles = mr.getExtractedData(DonneesCiviles.class, moi.getKey());
			if (donneesCiviles != null) {
				rcent = donneesCiviles.getOrganisation();
			}
		}

		// enregistrement de cette entreprise pour un contrôle final des fors secondaires (une fois que tous les immeubles et établissements ont été visés)
		mr.addPreTransactionCommitData(new ControleForsSecondairesData(regpm.getForsSecondaires(), moi));

		// enregistrement de cette entreprise pour un contrôle final de la couverture des fors secondaires par les fors principaux
		mr.addPreTransactionCommitData(new CouvertureForsData(moi));

		// enregistrement de cette entreprise pour la comparaison des assujettissements avant/après
		mr.addPreTransactionCommitData(new ComparaisonAssujettissementsData(regpm.getAssujettissements(), moi));

		// TODO migrer les adresses, les documents...

		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, unireg, mr);

		migrateAllegementsFiscaux(regpm, unireg, mr);
		migrateRegimesFiscaux(regpm, unireg, mr);
		migrateExercicesCommerciaux(regpm, unireg, mr);
		migrateDeclarations(regpm, unireg, mr);
		migrateForsPrincipaux(regpm, unireg, mr);
		migrateImmeubles(regpm, unireg, mr);
		generateEtablissementPrincipal(regpm, unireg, linkCollector, idMapper, mr);

		migrateMandataires(regpm, mr, linkCollector, idMapper);
		migrateFusionsApres(regpm, linkCollector, idMapper);

		// log de suivi à la fin des opérations pour cette entreprise
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Entreprise migrée : %s.", FormatNumeroHelper.numeroCTBToDisplay(unireg.getNumero())));
	}

	/**
	 * Classe interne qui contient la donnée d'une commune Suisse ou d'un pays, de manière exclusive
	 */
	private static final class CommuneOuPays {

		private final RegpmCommune commune;
		private final Integer noOfsPays;

		public CommuneOuPays(@NotNull RegpmCommune commune) {
			this.commune = commune;
			this.noOfsPays = null;
		}

		public CommuneOuPays(@NotNull RegpmForPrincipal ffp) {
			if (ffp.getCommune() != null) {
				this.commune = ffp.getCommune();
				this.noOfsPays = null;
			}
			else {
				this.commune = null;
				this.noOfsPays = ffp.getOfsPays();
			}
		}

		/**
		 * @return le type d'autorité fiscale représentée par l'entité
		 */
		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			if (commune != null) {
				return commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
			}
			else {
				return TypeAutoriteFiscale.PAYS_HS;
			}
		}

		/**
		 * @return le numéro OFS de la commune ou du pays (voir {@link #getTypeAutoriteFiscale()})
		 */
		public Integer getNumeroOfsAutoriteFiscale() {
			return commune != null ? NO_OFS_COMMUNE_EXTRACTOR.apply(commune) : noOfsPays;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final CommuneOuPays that = (CommuneOuPays) o;

			if (commune != null ? !commune.equals(that.commune) : that.commune != null) return false;
			return !(noOfsPays != null ? !noOfsPays.equals(that.noOfsPays) : that.noOfsPays != null);
		}

		@Override
		public int hashCode() {
			int result = commune != null ? commune.hashCode() : 0;
			result = 31 * result + (noOfsPays != null ? noOfsPays.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return String.format("%s/%d", getTypeAutoriteFiscale(), getNumeroOfsAutoriteFiscale());
		}
	}

	/**
	 * Génération de l'établissement principal dont le domicile est placé sur la commune de l'entreprise (ou sur les fors fiscaux principaux si la commune n'est pas indiquée)
	 * @param regpm l'entreprise de RegPM
	 * @param unireg l'entreprise dans Unireg
	 * @param linkCollector le collecteur de liens à créer entre les entités
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param mr collecteur de messages de migration
	 */
	private void generateEtablissementPrincipal(RegpmEntreprise regpm, Entreprise unireg, EntityLinkCollector linkCollector, IdMapping idMapper, MigrationResultProduction mr) {

		// TODO peut-être pourra-t-on faire mieux avec les données de RCEnt, mais pour le moment, on suppose l'existence d'UN SEUL établissement principal avec, au besoin, plusieurs domiciles successifs

		final SortedMap<RegDate, CommuneOuPays> localisations;
		final RegpmCommune commune = regpm.getCommune();
		if (commune != null) {
			localisations = new TreeMap<>(Collections.singletonMap(getDateDebutActivite(regpm, mr), new CommuneOuPays(commune)));
		}
		else {
			// pas de commune (cas le plus fréquent...) on va donc regarder les fors principaux...
			final List<RegpmForPrincipal> forsPrincipaux = mr.getExtractedData(ForsPrincipauxData.class, buildEntrepriseKey(regpm)).liste;
			if (!forsPrincipaux.isEmpty()) {

				// pas besoin de faire de merge (voir l'exception lancée plus bas) car la liste extraite est, par construction, fiable
				// (en tout cas au niveau des dates...)
				localisations = forsPrincipaux.stream()
						.collect(Collectors.toMap(RegpmForPrincipal::getDateValidite,
						                          CommuneOuPays::new,
						                          (cop1, cop2) -> { throw new IllegalStateException("Map construite pour n'avoir qu'un seul for par date... pas besoin d'appeler le merger..."); },
						                          TreeMap::new));
			}
			else {
				// pas de commune, pas de for principal... -> pas d'établissement principal
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Pas de commune ni de for principal associé, pas d'établissement principal créé.");
				return;
			}
		}

		final Etablissement etbPrincipal = uniregStore.saveEntityToDb(new Etablissement());
		final Supplier<Etablissement> etbPrincipalSupplier = getEtablissementByUniregIdSupplier(etbPrincipal.getNumero());
		etbPrincipal.setEnseigne(regpm.getEnseigne());
		etbPrincipal.setPrincipal(true);

		// un peu de log pour indiquer la création de l'établissement principal
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, "Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()) + ".");

		// lien entre l'établissement principal et son entreprise
		final Supplier<Entreprise> entrepriseSupplier = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());
		linkCollector.addLink(new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(etbPrincipalSupplier, entrepriseSupplier, localisations.firstKey(), getDateFinActivite(regpm)));

		// domiciles selon les localisations trouvées plus haut (pour l'instant, sans date de fin... qui seront assignées juste après...)
		final List<DomicileEtablissement> domiciles = localisations.entrySet().stream()
				.map(entry -> {
					final CommuneOuPays cop = entry.getValue();
					return new DomicileEtablissement(entry.getKey(), null, cop.getTypeAutoriteFiscale(), cop.getNumeroOfsAutoriteFiscale(), null);
				})
				.collect(Collectors.toList());

		// assignation des dates de fin
		assigneDatesFin(getDateFinActivite(regpm), domiciles);

		// liaison des domiciles à l'établissement
		domiciles.stream()
				.peek(domicile -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Domicile de l'établissement principal %s : %s sur %s/%d.",
				                                                                                FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
				                                                                                StringRenderers.DATE_RANGE_RENDERER.toString(domicile),
				                                                                                domicile.getTypeAutoriteFiscale(),
				                                                                                domicile.getNumeroOfsAutoriteFiscale())))
				.forEach(etbPrincipal::addDomicile);

   		// TODO adresse ?
	}

	/**
	 * Migration des liens de fusions (ceux qui clôturent l'entreprise en question, les autres étant traités au moment de la migration des entreprises précédentes)
	 * @param regpm l'entreprise qui va disparaître dans la fusion
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateFusionsApres(RegpmEntreprise regpm, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// migration des fusions (cette entreprise étant la source)
		regpm.getFusionsApres().stream()
				.filter(fusion -> !fusion.isRectifiee())
				.forEach(apres -> {
					// TODO et les autres informations de la fusion (forme, date d'inscription, date de contrat, date de bilan... ?)
					final Supplier<Entreprise> apresFusion = getEntrepriseByRegpmIdSupplier(idMapper, apres.getEntrepriseApres().getId());
					linkCollector.addLink(new EntityLinkCollector.FusionEntreprisesLink(moi, apresFusion, apres.getDateBilan().getOneDayAfter(), null));
				});
	}

	/**
	 * Migration des mandataires d'une entreprise
	 * @param regpm entreprise à migrer
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateMandataires(RegpmEntreprise regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// migration des mandataires -> liens à créer par la suite
		regpm.getMandataires().forEach(mandat -> {

			// récupération du mandataire qui peut être une autre entreprise, un établissement ou un individu
			final Supplier<? extends Contribuable> mandataire = getPolymorphicSupplier(idMapper, mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
			if (mandataire == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Le mandat " + mandat.getId() + " n'a pas de mandataire.");
				return;
			}

			// TODO ne faut-il vraiment migrer que les mandats généraux ?

			// on ne migre que les mandats généraux pour le moment
			if (mandat.getType() != RegpmTypeMandat.GENERAL) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Le mandat " + mandat.getId() + " de type " + mandat.getType() + " est ignoré dans la migration.");
				return;
			}

			final TypeMandat typeMandat = TypeMandat.GENERAL;
			final String bicSwift = mandat.getBicSwift();
			String iban;
			try {
				iban = IbanExtractor.extractIban(mandat, mr);
			}
			catch (IbanExtractor.IbanExtratorException e) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Impossible d'extraire un IBAN du mandat " + mandat.getId() + " (" + e.getMessage() + ")");
				iban = null;
			}

			// une date de début nulle pose un grave problème (c'est peut-être une date trop lointaine dans le passé, i.e. avant 1291... -> vraissemblablement une erreur de saisie)
			if (mandat.getDateAttribution() == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
				              "Le mandat " + mandat.getId() + " n'a pas de date d'attribution (ou cette date est très loin dans le passé), il sera donc ignoré dans la migration.");
				return;
			}

			// une date de début dans le futur fait que le mandat est ignoré (Unireg n'aime pas ça...)
			if (isFutureDate(mandat.getDateAttribution())) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, String.format("La date d'attribution du mandat %s est dans le futur (%s), le mandat sera donc ignoré dans la migration.",
				                                                              mandat.getId(), StringRenderers.DATE_RENDERER.toString(mandat.getDateAttribution())));
				return;
			}

			// date de fin dans le futur -> on ignore la date de fin
			final RegDate dateFin;
			if (isFutureDate(mandat.getDateResiliation())) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, String.format("La date de résiliation du mandat %s est dans le futur (%s), le mandat sera donc laissé ouvert dans la migration.",
				                                                              mandat.getId(), StringRenderers.DATE_RENDERER.toString(mandat.getDateResiliation())));
				dateFin = null;
			}
			else {
				dateFin = mandat.getDateResiliation();
			}

			// ajout du lien entre l'entreprise et son mandataire
			linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi, mandataire, mandat.getDateAttribution(), dateFin, typeMandat, iban, bicSwift));
		});
	}

	private void migrateExercicesCommerciaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final RegDate dateBouclementFutur = regpm.getDateBouclementFutur();
		final List<RegDate> datesBouclements;
		final SortedSet<RegpmExerciceCommercial> exercicesCommerciaux = regpm.getExercicesCommerciaux();
		if (exercicesCommerciaux != null && !exercicesCommerciaux.isEmpty()) {

			final Stream.Builder<RegDate> additionalDatesStreamBuilder = Stream.builder();

			// dans RegPM, les exercices commerciaux ne sont instanciés que quand une DI est retournée, ce qui a pour conséquence immédiate qu'en l'absence
			// d'assujettissement, aucune DI n'est bien-sûr envoyée, encore moins retournée, et donc aucun exercice commercial n'est créé en base...
			// donc, si on trouve des trous dans les exercices commerciaux en base, cela correspond à une interruption de l'assujettissement, et il faut le combler
			// (on va supposer des exercices commerciaux annuels, faute de mieux, dans la période non-mappée)

			// période totale maximale couverte (potentiellement partiellement, c'est justement ce qui nous intéresse ici...) par des exercices commerciaux de regpm
			final DateRange lifespan = new DateRangeHelper.Range(exercicesCommerciaux.first().getDateDebut(), exercicesCommerciaux.last().getDateFin());
			final List<DateRange> mapped = exercicesCommerciaux.stream().map(ex -> new DateRangeHelper.Range(ex.getDateDebut(), ex.getDateFin())).collect(Collectors.toList());
			final List<DateRange> notMapped = DateRangeHelper.subtract(lifespan, mapped);
			if (!notMapped.isEmpty()) {

				// il y a bien au moins une (peut-être plusieurs...) période qui n'est pas mappée...
				// pour chacune d'entre elles, on va supposer des exercices commerciaux annuels (à la date d'ancrage du dernier exercice avant le trou) pour combler
				for (DateRange range : notMapped) {
					final DayMonth previousDayMonth = DayMonth.get(range.getDateDebut().getOneDayBefore());
					final DayMonth currentDayMonth = DayMonth.get(range.getDateFin());

					// s'il y a un nombre entier d'années entre les deux, on va juste boucler sur les années
					final RegDate startingDate;
					if (previousDayMonth.isEndOfMonth() && currentDayMonth.isEndOfMonth() && previousDayMonth.month() == currentDayMonth.month()) {
						startingDate = range.getDateDebut().getOneDayBefore().addYears(1);
					}
					// sinon, on se cale sur la fin et on boucle annuellement
					else {
						startingDate = currentDayMonth.nextAfter(range.getDateDebut());
					}

					// la boucle elle-même...
					for (RegDate dateBouclement = startingDate ; dateBouclement.isBeforeOrEqual(range.getDateFin()) ; dateBouclement = dateBouclement.addYears(1)) {
						additionalDatesStreamBuilder.accept(dateBouclement);

						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Ajout d'une date de bouclement estimée au %s pour combler l'absence d'exercice commercial dans RegPM sur la période %s.",
						                            StringRenderers.DATE_RENDERER.toString(dateBouclement),
						                            StringRenderers.DATE_RANGE_RENDERER.toString(range)));
					}
				}
			}

			final RegpmExerciceCommercial dernierConnu = exercicesCommerciaux.last();
			final RegDate dateFinDernierExercice = dernierConnu.getDateFin();

			// c'est apparemment le cas qui apparaît tout le temps :
			// la déclaration (= dossier fiscal) de la PF précédente a déjà été envoyée, mais aucun exercice commercial
			// n'a encore été généré, et la date de bouclement futur correspond déjà à la fin de la PF courante)
			// -> je dois bien créer un exercice commercial dans Unireg entre les deux (= pour la DI envoyée)

			// TODO que faire si la date de bouclement futur est nulle ?

			if (dateBouclementFutur != null) {
				if (dateFinDernierExercice.addYears(1).compareTo(dateBouclementFutur) < 0) {
					final RegDate dateEstimee = dateBouclementFutur.addYears(-1);
					mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
					              String.format("Prise en compte d'une date de bouclement estimée au %s (un an avant la date de bouclement futur).", RegDateHelper.dateToDisplayString(dateEstimee)));
					additionalDatesStreamBuilder.accept(dateEstimee);
				}
				additionalDatesStreamBuilder.accept(dateBouclementFutur);
			}
			final Stream<RegDate> additionalDatesStream = additionalDatesStreamBuilder.build();

			// la liste des dates à prendre en compte pour le calcul des bouclements à la sauce Unireg
			datesBouclements = Stream.concat(exercicesCommerciaux.stream().map(RegpmExerciceCommercial::getDateFin), additionalDatesStream)
					.collect(Collectors.toList());
		}
		else if (dateBouclementFutur != null) {
			// aucun exercice commercial, mais une date de bouclement futur -> ce sera le premier bouclement connu
			datesBouclements = Collections.singletonList(dateBouclementFutur);
		}
		else {
			// entreprise sans exercice commercial ni bouclement futur -> ça se log...
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Entreprise sans exercice commercial ni date de bouclement futur.");
			datesBouclements = Collections.emptyList();
		}

		// calcul des périodicités...
		final List<Bouclement> bouclements = bouclementService.extractBouclementsDepuisDates(datesBouclements, 12);
		bouclements.stream()
				.peek(b -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				                         String.format("Cycle de bouclements créé, applicable dès le %s : tous les %d mois, à partir du premier %s.",
				                                       StringRenderers.DATE_RENDERER.toString(b.getDateDebut()),
				                                       b.getPeriodeMois(),
				                                       StringRenderers.DAYMONTH_RENDERER.toString(b.getAncrage()))))
				.forEach(unireg::addBouclement);
	}

	@NotNull
	private PeriodeFiscale getPeriodeFiscaleByYear(int year) {
		// critère sur l'année
		final Map<String, Object> params = Collections.singletonMap("annee", year);

		// récupération des données
		final List<PeriodeFiscale> pfs = Optional.ofNullable(uniregStore.getEntitiesFromDb(PeriodeFiscale.class, params)).orElse(Collections.emptyList());
		if (pfs.isEmpty()) {
			throw new IllegalStateException("Aucune période fiscale trouvée dans Unireg pour l'année " + year);
		}
		else if (pfs.size() > 1) {
			throw new IllegalStateException("Plusieurs périodes fiscales trouvées dans Unireg pour l'année " + year);
		}
		else {
			return pfs.get(0);
		}
	}

	private Declaration migrateDeclaration(RegpmDossierFiscal dossier, RegDate dateDebut, RegDate dateFin, MigrationResultProduction mr) {
		final PeriodeFiscale pf = getPeriodeFiscaleByYear(dossier.getPf());

		final DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();
		copyCreationMutation(dossier, di);
		di.setDateDebut(dateDebut);
		di.setDateFin(dateFin);
		di.setDelais(migrateDelaisDeclaration(dossier, di, mr));
		di.setEtats(migrateEtatsDeclaration(dossier, di, mr));
		di.setNumero(dossier.getNoParAnnee());
		di.setPeriode(pf);

		if (dossier.getEtat() == RegpmTypeEtatDossierFiscal.ANNULE) {
			di.setAnnulationUser(Optional.ofNullable(dossier.getLastMutationOperator()).orElse(AuthenticationHelper.getCurrentPrincipal()));
			di.setAnnulationDate(Optional.ofNullable((Date) dossier.getLastMutationTimestamp()).orElseGet(DateHelper::getCurrentDate));
		}
		return di;
	}

	/**
	 * Migration des déclarations d'impôts, de leurs états, délais...
	 */
	private void migrateDeclarations(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final List<RegpmDossierFiscal> dossiers = mr.getExtractedData(DossiersFiscauxData.class, buildEntrepriseKey(regpm)).liste;
		final Set<RegpmDossierFiscal> dossiersFiscauxAttribuesAuxExercicesCommerciaux = new HashSet<>(dossiers.size());

		// boucle sur chacun des exercices commerciaux
		regpm.getExercicesCommerciaux().forEach(exercice -> {

			final RegpmDossierFiscal dossier = exercice.getDossierFiscal();
			if (dossier != null && dossier.getModeImposition() == RegpmModeImposition.POST) {

				// on collecte les dossiers fiscaux attachés aux exercices commerciaux
				// pour trouver au final ceux qui ne le sont pas (= les déclarations envoyées mais pas encore traitées ???)
				dossiersFiscauxAttribuesAuxExercicesCommerciaux.add(dossier);

				// un peu de log pour le suivi
				mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
				              String.format("Génération d'une déclaration sur la PF %d à partir des dates %s de l'exercice commercial %d et du dossier fiscal correspondant.",
				                            dossier.getPf(),
				                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(exercice.getDateDebut(), exercice.getDateFin())),
				                            exercice.getId().getSeqNo()));

				// un petit warning sur des cas bizarres...
				if (dossier.getPf() != exercice.getDateFin().year()) {
					mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
					              String.format("Dossier fiscal sur la PF %d alors que la fin de l'exercice commercial est en %d... N'est-ce pas étrange ?",
					                            dossier.getPf(),
					                            exercice.getDateFin().year()));
				}

				final Declaration di = migrateDeclaration(dossier, exercice.getDateDebut(), exercice.getDateFin(), mr);
				unireg.addDeclaration(di);
			}
		});

		// ensuite, il faut éventuellement trouver une déclaration envoyée mais pour laquelle je n'ai pas encore
		// d'entrée dans la table des exercices commerciaux
		dossiers.stream()
				.filter(dossier -> !dossiersFiscauxAttribuesAuxExercicesCommerciaux.contains(dossier))
				.forEach(dossier -> {

					// si la PF est celle de la prochaine date de bouclement, alors on peut la créer...
					if (regpm.getDateBouclementFutur() != null && regpm.getDateBouclementFutur().year() == dossier.getPf()) {
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Dossier fiscal %d/%d sans exercice commercial associé sur la PF du bouclement futur -> quelle est la date de début de la déclaration ?",
						                            dossier.getPf(), dossier.getNoParAnnee()));

						// TODO comment évaluer la date de début ?
//						final RegDate dateDebut = null;
//						final Declaration di = migrateDeclaration(dossier, dateDebut, regpm.getDateBouclementFutur());
//						unireg.addDeclaration(di);
					}
					else {
						// TODO que faire avec ces dossiers ? Ils correspondent pourtant à une déclaration envoyée, mais pourquoi n'y a-t-il pas d'exercice commercial associé ?
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Dossier fiscal %d/%d passé sans exercice commercial associé.", dossier.getPf(), dossier.getNoParAnnee()));
					}
				});
	}

	/**
	 * Génération des délais de dépôt
	 */
	private static Set<DelaiDeclaration> migrateDelaisDeclaration(RegpmDossierFiscal dossier, Declaration di, MigrationResultProduction mr) {

		final Set<DelaiDeclaration> delais = new LinkedHashSet<>();

		// délai initial
		if (dossier.getDelaiRetour() != null) {
			final DelaiDeclaration delai = new DelaiDeclaration();
			copyCreationMutation(dossier, delai);
			delai.setConfirmationEcrite(false);
			delai.setDateDemande(dossier.getDateEnvoi());           // TODO le délai initial est "demandé" à la date d'envoi, non ?
			delai.setDateTraitement(dossier.getDateEnvoi());
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(dossier.getDelaiRetour());
			delais.add(delai);

			// un peu de traçabilité
			mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
			              String.format("Délai initial de retour fixé au %s.", StringRenderers.DATE_RENDERER.toString(dossier.getDelaiRetour())));
		}

		// fonction de conversion
		final Function<RegpmDemandeDelaiSommation, DelaiDeclaration> mapper = regpm -> {
			final DelaiDeclaration delai = new DelaiDeclaration();
			copyCreationMutation(regpm, delai);
			delai.setConfirmationEcrite(false);                             // les documents ne doivent pas être retrouvés dans Unireg, mais par le DPerm s'il le faut
			delai.setDateDemande(regpm.getDateDemande());
			delai.setDateTraitement(regpm.getDateReception());              // TODO on est sûr ce de mapping ?
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(regpm.getDelaiAccorde());
			return delai;
		};

		// TODO que fait-on avec les demandes de délai en cours d'analyse (etat = DEMANDEE) ?
		// TODO que fait-on avec les demandes de délai refusées (etat = REFUSEE) ?
		// TODO que fait-on avec les demandes de délai après sommation (type = APRES_SOMMATION) ?

		// demandes ultérieures
		dossier.getDemandesDelai().stream()
				.filter(demande -> {
					if (demande.getType() != RegpmTypeDemandeDelai.AVANT_SOMMATION) {
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Demande de délai 'après sommation' du %s ignorée.", StringRenderers.DATE_RENDERER.toString(demande.getDateDemande())));
						return false;
					}
					return true;
				})
				.filter(demande -> {
					if (demande.getEtat() != RegpmTypeEtatDemandeDelai.ACCORDEE) {
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Demande de délai non-accordée (%s) du %s ignorée.", demande.getEtat(), StringRenderers.DATE_RENDERER.toString(demande.getDateDemande())));
						return false;
					}
					return true;
				})
				.map(mapper)
				.peek(delai -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO, String.format("Nouveau délai généré au %s.", StringRenderers.DATE_RENDERER.toString(delai.getDelaiAccordeAu()))))
				.forEach(delais::add);
		return delais;
	}

	/**
	 * Génération des états d'une déclaration
	 */
	private static Set<EtatDeclaration> migrateEtatsDeclaration(RegpmDossierFiscal dossier, Declaration di, MigrationResultProduction mr) {

		final Set<EtatDeclaration> etats = new LinkedHashSet<>();

		// envoi
		if (dossier.getDateEnvoi() != null) {
			etats.add(new EtatDeclarationEmise(dossier.getDateEnvoi()));
		}

		// sommation
		if (dossier.getDateEnvoiSommation() != null) {
			etats.add(new EtatDeclarationSommee(dossier.getDateEnvoiSommation(), dossier.getDateEnvoiSommation()));
		}

		// échéance, est dans les données de taxation (= taxation d'office)
		dossier.getEnvironnementsTaxation().stream()
				.map(RegpmEnvironnementTaxation::getDecisionsTaxation)
				.flatMap(Set::stream)
				.filter(dt -> dt.getNatureDecision().isTaxationOffice())
				.filter(dt -> dt.getEtatCourant() != RegpmTypeEtatDecisionTaxation.ANNULEE)
				.findAny()
				.ifPresent(dt -> etats.add(new EtatDeclarationEchue(RegDateHelper.get(dt.getLastMutationTimestamp()))));

		// retour
		if (dossier.getDateRetour() != null) {
			etats.add(new EtatDeclarationRetournee(dossier.getDateRetour(), SOURCE_RETOUR_DI_MIGREE));
		}

		// un peu de traçabilité sur le travail accompli ici
		etats.forEach(etat -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO, String.format("Etat '%s' migré au %s.", etat.getEtat(), StringRenderers.DATE_RENDERER.toString(etat.getDateObtention()))));

		return etats;
	}

	private void migrateForsPrincipaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final Function<RegpmForPrincipal, Optional<Pair<Boolean, ForFiscalPrincipalPM>>> mapper = f -> {
			final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
			copyCreationMutation(f, ffp);
			ffp.setDateDebut(f.getDateValidite());
			ffp.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
			ffp.setMotifRattachement(MotifRattachement.DOMICILE);
			if (f.getCommune() != null) {
				final RegpmCommune commune = f.getCommune();
				ffp.setNumeroOfsAutoriteFiscale(NO_OFS_COMMUNE_EXTRACTOR.apply(commune));
				ffp.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
			}
			else if (f.getOfsPays() != null) {
				if (f.getOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
					mr.addMessage(LogCategory.FORS, LogLevel.ERROR, String.format("For principal %s sans commune mais sur Suisse", f.getId()));
					return Optional.empty();
				}
				ffp.setNumeroOfsAutoriteFiscale(f.getOfsPays());
				ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			}
			else {
				mr.addMessage(LogCategory.FORS, LogLevel.ERROR, String.format("For principal %s sans autorité fiscale", f.getId()));
				return Optional.empty();
			}
			ffp.setTiers(unireg);
			return Optional.of(Pair.of(f.getType() == RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, ffp));
		};

		// récupération des fors principaux valides
		final List<RegpmForPrincipal> forsRegpm = mr.getExtractedData(ForsPrincipauxData.class, buildEntrepriseKey(regpm)).liste;

		// puis la migration, justement

		// ici, on va collecter les fors qui sont issus d'une décision ACI (= administration effective)
		// afin de générer les entités Unireg 'DecisionACI'
		final List<ForFiscalPrincipalPM> listeIssueDeDecisionAci = new ArrayList<>(forsRegpm.size());
		final List<ForFiscalPrincipalPM> liste = forsRegpm.stream()
				.map(mapper)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.peek(pair -> {
					if (pair.getLeft()) {
						listeIssueDeDecisionAci.add(pair.getRight());
					}
				})
				.map(Pair::getRight)
				.sorted(Comparator.comparing(ForFiscalPrincipal::getDateDebut))
				.collect(Collectors.toList());

		// assignation des dates de fin
		assigneDatesFin(getDateFinActivite(regpm), liste);

		// assignation des motifs
		calculeMotifsOuvertureFermeture(liste);

		// on les ajoute au tiers
		liste.forEach(ff -> {
			// un peu de traçabilité.
			mr.addMessage(LogCategory.FORS, LogLevel.INFO,
			              String.format("For principal %s/%d %s généré.", ff.getTypeAutoriteFiscale(), ff.getNumeroOfsAutoriteFiscale(), StringRenderers.DATE_RANGE_RENDERER.toString(ff)));

			// ajout au tiers
			unireg.addForFiscal(ff);
		});

		// ... et finalement on crée également les éventuelles décisions ACI
		listeIssueDeDecisionAci.stream()
				.map(ff -> {
					final DecisionAci decision = new DecisionAci();
					decision.setDateDebut(ff.getDateDebut());
					decision.setDateFin(ff.getDateFin());
					decision.setNumeroOfsAutoriteFiscale(ff.getNumeroOfsAutoriteFiscale());
					decision.setTypeAutoriteFiscale(ff.getTypeAutoriteFiscale());
					decision.setRemarque(String.format("Selon décision OIPM du %s par %s.", StringRenderers.DATE_RENDERER.toString(RegDateHelper.get(ff.getLogCreationDate())), ff.getLogCreationUser()));
					return decision;
				})
				.peek(decision -> mr.addMessage(LogCategory.FORS, LogLevel.INFO,
				                                String.format("Décision ACI %s/%d %s générée.",
				                                              decision.getTypeAutoriteFiscale(),
				                                              decision.getNumeroOfsAutoriteFiscale(),
				                                              StringRenderers.DATE_RANGE_RENDERER.toString(decision))))
				.forEach(unireg::addDecisionAci);
	}

	/**
	 * Calcul des motifs d'ouverture/fermeture des fors fiscaux principaux passés en paramètre
	 * @param fors liste des fors principaux (supposés triés dans l'ordre chronologique)
	 */
	private static void calculeMotifsOuvertureFermeture(List<ForFiscalPrincipalPM> fors) {
		final MovingWindow<ForFiscalPrincipalPM> wnd = new MovingWindow<>(fors);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipalPM> snap = wnd.next();
			final ForFiscalPrincipal current = snap.getCurrent();
			final ForFiscalPrincipal previous = snap.getPrevious();
			final ForFiscalPrincipal next = snap.getNext();

			// TODO les motifs doivent peut-être venir des inscriptions/radiations au RC

			// le tout premier for a un motif d'ouverture indéterminé
			if (previous == null) {
				current.setMotifOuverture(MotifFor.INDETERMINE);
			}

			// le tout dernier for a un motif de fermeture indéterminé si la date de fermeture est non-nulle
			if (next == null && current.getDateFin() != null) {
				current.setMotifFermeture(MotifFor.INDETERMINE);
			}

			// comparaison des types d'autorité fiscales pour les mutations
			if (next != null) {
				final TypeAutoriteFiscale currentTAF = current.getTypeAutoriteFiscale();
				final TypeAutoriteFiscale nextTAF = next.getTypeAutoriteFiscale();
				final MotifFor motif;
				if (currentTAF == nextTAF) {
					// TODO il y a sans doute d'autres possibilités, comme une fusion de communes...
					motif = MotifFor.DEMENAGEMENT_VD;
				}
				else if (nextTAF == TypeAutoriteFiscale.PAYS_HS) {
					motif = MotifFor.DEPART_HS;
				}
				else if (currentTAF == TypeAutoriteFiscale.PAYS_HS) {
					motif = MotifFor.ARRIVEE_HS;
				}
				else if (nextTAF == TypeAutoriteFiscale.COMMUNE_HC) {
					motif = MotifFor.DEPART_HC;
				}
				else {
					motif = MotifFor.ARRIVEE_HC;
				}

				current.setMotifFermeture(motif);
				next.setMotifOuverture(motif);
			}
		}
	}

	private void migrateImmeubles(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		// les fors secondaires devront être créés sur l'entreprise migrée
		final KeyedSupplier<Entreprise> moi = new KeyedSupplier<>(buildEntrepriseKey(regpm), getEntrepriseByUniregIdSupplier(unireg.getId()));

		// les immeubles en possession directe
		final Map<RegpmCommune, List<DateRange>> mapDirecte = couvertureDepuisRattachementsProprietaires(regpm.getRattachementsProprietaires());
	    if (!mapDirecte.isEmpty()) {
		    mr.addPreTransactionCommitData(new ForsSecondairesData.Immeuble(moi, mapDirecte));
	    }

		// les immeubles en possession via un groupe
		final Map<RegpmCommune, List<DateRange>> mapViaGroupe = couvertureDepuisAppartenancesGroupeProprietaire(regpm.getAppartenancesGroupeProprietaire());
		if (!mapViaGroupe.isEmpty()) {
			mr.addPreTransactionCommitData(new ForsSecondairesData.Immeuble(moi, mapViaGroupe));
		}
	}

	/**
	 * Attribution des dates de fin en suivant les principes que
	 * <ul>
	 *     <li>la liste des éléments est triée dans l'ordre chronologique des dates de début</li>
	 *     <li>les éléments ne se chevauchent pas</li>
	 * </ul>
	 * @param derniereDateFin date de fin à appliquer au dernier éléments de la liste
	 * @param listeTriee liste dont les éléments doivent se voir assigner une date de fin
	 * @param <T> type des éléments de la liste
	 */
	private static <T extends HibernateDateRangeEntity> void assigneDatesFin(@Nullable RegDate derniereDateFin, List<T> listeTriee) {
		RegDate dateFinCourante = derniereDateFin;
		for (T ffp : CollectionsUtils.revertedOrder(listeTriee)) {
			ffp.setDateFin(dateFinCourante);
			dateFinCourante = ffp.getDateDebut().getOneDayBefore();
		}
	}

	private static TypeRegimeFiscal mapTypeRegimeFiscal(RegpmTypeRegimeFiscal type) {
		// TODO il va falloir trouver un mapping un peu plus touffu...
		return TypeRegimeFiscal.ORDINAIRE;
	}

	private static RegimeFiscal mapRegimeFiscal(RegimeFiscal.Portee portee, RegpmRegimeFiscal rf) {
		final RegimeFiscal unireg = new RegimeFiscal();
		unireg.setDateDebut(rf.getDateDebut());
		unireg.setDateFin(null);
		unireg.setPortee(portee);
		unireg.setType(mapTypeRegimeFiscal(rf.getType()));
		return unireg;
	}

	private static <T extends RegpmRegimeFiscal> List<RegimeFiscal> mapRegimesFiscaux(RegimeFiscal.Portee portee,
	                                                                                  SortedSet<T> regimesRegpm,
	                                                                                  @Nullable RegDate dateFinRegimes,
	                                                                                  MigrationResultProduction mr) {
		// collecte des régimes fiscaux CH sans date de fin d'abord...
		final List<RegimeFiscal> liste = regimesRegpm.stream()
				.filter(r -> r.getDateAnnulation() == null)         // on ne migre pas les régimes fiscaux annulés
				.filter(rf -> {
					if (rf.getDateDebut() == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début nulle.",
						                            portee,
						                            rf.getType()));
						return false;
					}
					return true;
				})
				.map(r -> mapRegimeFiscal(portee, r))
				.collect(Collectors.toList());

		// ... puis attribution des dates de fin
		assigneDatesFin(dateFinRegimes, liste);
		return liste;
	}

	private void migrateRegimesFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// collecte des régimes fiscaux CH...
		final List<RegimeFiscal> listeCH = mapRegimesFiscaux(RegimeFiscal.Portee.CH,
		                                                     regpm.getRegimesFiscauxCH(),
		                                                     getDateFinActivite(regpm),
		                                                     mr);

		// ... puis des règimes fiscaux VD
		final List<RegimeFiscal> listeVD = mapRegimesFiscaux(RegimeFiscal.Portee.VD,
		                                                     regpm.getRegimesFiscauxVD(),
		                                                     getDateFinActivite(regpm),
		                                                     mr);

		// et finalement on ajoute tout ça dans l'entreprise
		Stream.concat(listeCH.stream(), listeVD.stream()).forEach(unireg::addRegimeFiscal);
	}

	private static AllegementFiscal.TypeImpot toTypeImpot(@NotNull RegpmCodeContribution codeContribution) {
		switch (codeContribution) {
		case BENEFICE:
			return AllegementFiscal.TypeImpot.BENEFICE;
		case CAPITAL:
			return AllegementFiscal.TypeImpot.CAPITAL;
		default:
			throw new IllegalArgumentException("Seuls BENEFICE et CAPITAL sont supportés ici.");
		}
	}

	private static AllegementFiscal.TypeCollectivite toTypeCollectivite(@NotNull RegpmCodeCollectivite codeCollectivite) {
		switch (codeCollectivite) {
		case CANTON:
			return AllegementFiscal.TypeCollectivite.CANTON;
		case COMMUNE:
			return AllegementFiscal.TypeCollectivite.COMMUNE;
		case CONFEDERATION:
			return AllegementFiscal.TypeCollectivite.CONFEDERATION;
		default:
			throw new IllegalArgumentException("Code collectivité inconnu : " + codeCollectivite);
		}
	}

	private static AllegementFiscal.TypeCollectivite toTypeCollectivite(@NotNull RegpmObjectImpot objectImpot) {
		switch (objectImpot) {
		case CANTONAL:
			return AllegementFiscal.TypeCollectivite.CANTON;
		case COMMUNAL:
			return AllegementFiscal.TypeCollectivite.COMMUNE;
		case FEDERAL:
			return AllegementFiscal.TypeCollectivite.CONFEDERATION;
		default:
			throw new IllegalArgumentException("Code object impôt inconnu : " + objectImpot);
		}
	}

	private static AllegementFiscal buildAllegementFiscal(RegpmAllegementFiscal regpm, AllegementFiscal.TypeImpot typeImpot, AllegementFiscal.TypeCollectivite typeCollectivite, @Nullable Integer noOfsCommune) {
		final AllegementFiscal unireg = new AllegementFiscal();
		copyCreationMutation(regpm, unireg);
		unireg.setDateDebut(regpm.getDateDebut());
		unireg.setDateFin(regpm.getDateFin());
		unireg.setPourcentageAllegement(regpm.getPourcentage());
		unireg.setTypeCollectivite(typeCollectivite);
		unireg.setTypeImpot(typeImpot);
		unireg.setNoOfsCommune(noOfsCommune);
		return unireg;
	}

	private static Stream<AllegementFiscal> mapAllegementFiscal(RegpmAllegementFiscal a, MigrationResultProduction mr) {

		final Stream.Builder<AllegementFiscal> builder = Stream.builder();

		if (a.getTypeContribution() != null) {
			final RegpmTypeContribution typeContribution = a.getTypeContribution();

			// on ne s'intéresse ici qu'aux allégements "bénéfice" et "capital"
			final RegpmCodeContribution codeContribution = typeContribution.getCodeContribution();
			if (codeContribution != RegpmCodeContribution.BENEFICE && codeContribution != RegpmCodeContribution.CAPITAL) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				              String.format("Allègement fiscal %d avec un code de contribution %s -> ignoré.", a.getId().getSeqNo(), codeContribution));
			}
			else {
				final RegpmCodeCollectivite codeCollectivite = typeContribution.getCodeCollectivite();
				final AllegementFiscal.TypeImpot typeImpot = toTypeImpot(codeContribution);
				final AllegementFiscal.TypeCollectivite typeCollectivite = toTypeCollectivite(codeCollectivite);
				final Integer noOfsCommune = typeCollectivite == AllegementFiscal.TypeCollectivite.COMMUNE && a.getCommune() != null ? NO_OFS_COMMUNE_EXTRACTOR.apply(a.getCommune()) : null;
				builder.accept(buildAllegementFiscal(a, typeImpot, typeCollectivite, noOfsCommune));
			}
		}
		else if (a.getObjectImpot() != null) {
			final AllegementFiscal.TypeCollectivite typeCollectivite = toTypeCollectivite(a.getObjectImpot());
			builder.accept(buildAllegementFiscal(a, AllegementFiscal.TypeImpot.BENEFICE, typeCollectivite, null));
			builder.accept(buildAllegementFiscal(a, AllegementFiscal.TypeImpot.CAPITAL, typeCollectivite, null));
		}
		else if (a.getId().getSeqNo() == 998 || a.getId().getSeqNo() == 999) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
			              String.format("Allègement fiscal %d avec un numéro de séquence 998/999 -> ignoré.", a.getId().getSeqNo()));
		}
		else {
			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
			              String.format("Allègement fiscal %d sans type de contribution ni object impôt -> ignoré.", a.getId().getSeqNo()));
		}

		return builder.build();
	}

	private static String buildCollectiviteString(AllegementFiscal a) {
		if (a.getTypeCollectivite() == AllegementFiscal.TypeCollectivite.COMMUNE && a.getNoOfsCommune() != null) {
			return String.format("%s (%d)", a.getTypeCollectivite(), a.getNoOfsCommune());
		}
		return a.getTypeCollectivite().name();
	}

	private void migrateAllegementsFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		regpm.getAllegementsFiscaux().stream()
				.filter(a -> a.getDateAnnulation() == null)                 // on ne prend pas en compte les allègements annulés
				.sorted(Comparator.comparing(a -> a.getId().getSeqNo()))    // tri pour les tests en particulier, pour toujours traiter les allègements dans le même ordre
				.filter(a -> {
					if (a.getCommune() != null && a.getCommune().getCanton() != RegpmCanton.VD) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Allègement fiscal %d sur une commune hors-canton (%s/%d/%s) -> ignoré.",
						                            a.getId().getSeqNo(),
						                            a.getCommune().getNom(),
						                            NO_OFS_COMMUNE_EXTRACTOR.apply(a.getCommune()),
						                            a.getCommune().getCanton()));
						return false;
					}
					return true;
				})
				.map(a -> mapAllegementFiscal(a, mr))
				.flatMap(Function.identity())
				.peek(a -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				                         String.format("Allègement fiscal généré %s, collectivité %s, type %s : %s%%.",
				                                       StringRenderers.DATE_RANGE_RENDERER.toString(a),
				                                       buildCollectiviteString(a),
				                                       a.getTypeImpot(),
				                                       a.getPourcentageAllegement())))
				.forEach(unireg::addAllegementFiscal);
	}
}
