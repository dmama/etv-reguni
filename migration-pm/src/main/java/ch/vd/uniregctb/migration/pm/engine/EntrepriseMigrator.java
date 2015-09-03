package ch.vd.uniregctb.migration.pm.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesCiviles;
import ch.vd.uniregctb.migration.pm.engine.data.RaisonSocialeData;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.ContactEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RaisonSociale;
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
import ch.vd.uniregctb.migration.pm.regpm.RegpmFusion;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmSiegeEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.EntityWrapper;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.tiers.LocalizedDateRange;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
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
	private final RegDate seuilActivite;

	public EntrepriseMigrator(UniregStore uniregStore,
	                          ActivityManager activityManager,
	                          ServiceInfrastructureService infraService,
	                          BouclementService bouclementService,
	                          AssujettissementService assujettissementService,
	                          RegDate seuilActivite,
	                          RCEntAdapter rcEntAdapter,
	                          AdresseHelper adresseHelper,
	                          FusionCommunesProvider fusionCommunesProvider,
	                          FractionsCommuneProvider fractionsCommuneProvider) {
		super(uniregStore, activityManager, infraService, fusionCommunesProvider, fractionsCommuneProvider);
		this.bouclementService = bouclementService;
		this.assujettissementService = assujettissementService;
		this.seuilActivite = seuilActivite;
		this.rcEntAdapter = rcEntAdapter;
		this.adresseHelper = adresseHelper;
	}

	private static Entreprise createEntreprise(RegpmEntreprise regpm) {
		final Entreprise unireg = new Entreprise(regpm.getId());
		copyCreationMutation(regpm, unireg);
		return unireg;
	}

	private static final class ControleForsSecondairesData {
		private final Set<RegpmForSecondaire> regpm;
		private final KeyedSupplier<Entreprise> entrepriseSupplier;

		public ControleForsSecondairesData(Set<RegpmForSecondaire> regpm, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.regpm = regpm;
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static final class CouvertureForsData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public CouvertureForsData(KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static final class EffacementForsAnnulesData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public EffacementForsAnnulesData(KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static final class ComparaisonAssujettissementsData {
		private final boolean active;
		private final SortedSet<RegpmAssujettissement> regpmAssujettissements;
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public ComparaisonAssujettissementsData(boolean active, SortedSet<RegpmAssujettissement> regpmAssujettissements, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.active = active;
			this.regpmAssujettissements = regpmAssujettissements;
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static final class ForsPrincipauxData {
		@NotNull
		private final List<RegpmForPrincipal> liste;
		public ForsPrincipauxData(@Nullable List<RegpmForPrincipal> liste) {
			this.liste = liste == null ? Collections.emptyList() : liste;
		}
	}

	private static final class DossiersFiscauxData {
		@NotNull
		private final List<RegpmDossierFiscal> liste;
		public DossiersFiscauxData(@Nullable List<RegpmDossierFiscal> liste) {
			this.liste = liste == null ? Collections.emptyList() : liste;
		}
	}

	private static final class DateBouclementFuturData {
		@Nullable
		private final RegDate date;
		public DateBouclementFuturData(@Nullable RegDate date) {
			this.date = date;
		}
	}

	private static final class DateFinActiviteData {
		@Nullable
		private final RegDate date;
		public DateFinActiviteData(@Nullable RegDate date) {
			this.date = date;
		}
	}

	private static final class CapitalData {
		private final BigDecimal capital;
		private final RegDate dateValidite;

		public CapitalData(BigDecimal capital, RegDate dateValidite) {
			this.capital = capital;
			this.dateValidite = dateValidite;
		}
	}

	private static final class FormeJuridiqueData {
		private final RegpmTypeFormeJuridique type;
		private final RegDate dateValidite;

		public FormeJuridiqueData(RegpmTypeFormeJuridique type, RegDate dateValidite) {
			this.type = type;
			this.dateValidite = dateValidite;
		}
	}

	@Override
	public void initMigrationResult(MigrationResultInitialization mr, IdMapping idMapper) {
		super.initMigrationResult(mr, idMapper);
		
		//
		// callbacks avant la fin des transactions
		//

		// consolidation pour la constitution des fors "immeuble"
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.Immeuble.class,
		                                        MigrationConstants.PHASE_FORS_IMMEUBLES,
		                                        k -> k.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData.Immeuble(d1.entiteJuridiqueSupplier, DATE_RANGE_MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondairesImmeuble(d, mr, idMapper));

		// callback pour le contrôle des fors secondaires après création
		mr.registerPreTransactionCommitCallback(ControleForsSecondairesData.class,
		                                        MigrationConstants.PHASE_CONTROLE_FORS_SECONDAIRES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleForsSecondaires(d, mr, idMapper));

		// callback pour le contrôle (et la correction) de la couverture des fors secondaires par des fors principaux
		mr.registerPreTransactionCommitCallback(CouvertureForsData.class,
		                                        MigrationConstants.PHASE_COUVERTURE_FORS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleCouvertureFors(d, mr, idMapper));

		// callback pour la destruction des fors annulés créés
		mr.registerPreTransactionCommitCallback(EffacementForsAnnulesData.class,
		                                        MigrationConstants.PHASE_EFFACEMENT_FORS_ANNULES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        this::effacementForsAnnules);

		// callback pour le contrôle des données d'assujettissement
		mr.registerPreTransactionCommitCallback(ComparaisonAssujettissementsData.class,
		                                        MigrationConstants.PHASE_COMPARAISON_ASSUJETTISSEMENTS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> comparaisonAssujettissements(d, mr, idMapper));
		
		//
		// données "cachées" sur les entreprises
		//
		
		// les fors principaux non-ignorés
		mr.registerDataExtractor(ForsPrincipauxData.class,
		                         e -> extractForsPrincipaux(e, mr, idMapper),
		                         null,
		                         null);

		// les données qui viennent du civil
		mr.registerDataExtractor(DonneesCiviles.class,
		                         e -> extractDonneesCiviles(e, mr, idMapper),
		                         null,
		                         null);

		// les dossiers fiscaux non-ignorés
		mr.registerDataExtractor(DossiersFiscauxData.class,
		                         e -> extractDossiersFiscaux(e, mr, idMapper),
		                         null,
		                         null);

		// la date de bouclement futur
		mr.registerDataExtractor(DateBouclementFuturData.class,
		                         e -> extractDateBouclementFutur(e, mr, idMapper),
		                         null,
		                         null);

		// date de fin d'activité
		mr.registerDataExtractor(DateFinActiviteData.class,
		                         e -> extractDateFinActivite(e, mr, idMapper),
		                         null,
		                         null);
		
		// données de la dernière raison sociale
		mr.registerDataExtractor(RaisonSocialeData.class,
		                         e -> extractRaisonSociale(e, mr, idMapper),
		                         null,
		                         null);

		// données de la dernière modification de capital
		mr.registerDataExtractor(CapitalData.class,
		                         e -> extractCapital(e, mr, idMapper),
		                         null,
		                         null);

		// données de la dernière forme juridique de l'entreprise
		mr.registerDataExtractor(FormeJuridiqueData.class,
		                         e -> extractFormeJuridique(e, mr, idMapper),
		                         null,
		                         null);
	}

	/**
	 * Détermination de la date de fin d'activité d'une entreprise
	 * @param e l'entreprise de RegPM
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return la date de fin d'activité prise en compte
	 */
	private DateFinActiviteData extractDateFinActivite(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			// [SIFISC-16176] l'ordre des dates à prendre en compte est le suivant :
			// - date de bilan de fusion (si elle existe, évidemment)
			// - date de radiation
			// - date de dissolution

			final RegDate dateBilanFusion = e.getFusionsApres().stream()
					.filter(fusion -> !fusion.isRectifiee())
					.map(RegpmFusion::getDateBilan)
					.sorted(Comparator.reverseOrder())
					.findFirst()
					.orElse(null);

			final Pair<RegDate, String> finActivite = Stream.of(Pair.of(dateBilanFusion, "bilan de fusion"),
			                                                    Pair.of(e.getDateRadiationRC(), "radiation au RC"),
			                                                    Pair.of(e.getDateDissolution(), "dissolution"))
					.filter(pair -> pair.getLeft() != null)
					.filter(pair -> {
						if (isFutureDate(pair.getLeft())) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
							              String.format("La date de %s est ignorée elle est située dans le futur (%s).",
							                            pair.getRight(),
							                            StringRenderers.DATE_RENDERER.toString(pair.getLeft())));
							return false;
						}
						return true;
					})
					.findFirst()
					.orElse(null);

			// on a quelque chose ?
			if (finActivite != null) {

				// log informatif sur la date choisie
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				              String.format("Date de fin d'activité proposée (date de %s) : %s.",
				                            finActivite.getRight(),
				                            StringRenderers.DATE_RENDERER.toString(finActivite.getLeft())));

				// log en cas de valeur un peu louche
				if (isDateLouche(finActivite.getLeft())) {
					mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
					              String.format("La date de fin d'activité (date de %s) est antérieure au %s (%s).",
					                            finActivite.getRight(),
					                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
					                            StringRenderers.DATE_RENDERER.toString(finActivite.getLeft())));
				}

				// si la date est fixée, on ne la prend en compte que si elle
				// est postérieure à la date de début du dernier for fiscal principal existant
				final List<RegpmForPrincipal> forsPrincipaux = mr.getExtractedData(ForsPrincipauxData.class, buildEntrepriseKey(e)).liste;
				final RegpmForPrincipal apresFin = forsPrincipaux.stream()
						.filter(fp -> RegDateHelper.isAfter(fp.getDateValidite(), finActivite.getLeft(), NullDateBehavior.EARLIEST))
						.findFirst()
						.orElse(null);
				if (apresFin != null) {
					// il existe au moins un for principal qui commence après la date de fin calculée -> on ignore donc cette date de fin...
					mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
					              String.format("La date de fin d'activité proposée (%s) est antérieure à la date de début du for principal %d (%s), cette date de fin d'activité sera donc ignorée.",
					                            StringRenderers.DATE_RENDERER.toString(finActivite.getLeft()),
					                            apresFin.getId().getSeqNo(),
					                            StringRenderers.DATE_RENDERER.toString(apresFin.getDateValidite())));
					return new DateFinActiviteData(null);
				}
			}

			// ça y est, on s'est tous mis d'accord sur une date
			return new DateFinActiviteData(finActivite != null ? finActivite.getLeft() : null);
		});
	}

	/**
	 * Concatène les trois champs de la raison sociale de l'entreprise en une seule
	 * @param raisonSociale entreprise de RegPM
	 * @return la chaîne de caractères (<code>null</code> si vide) représentant la raison sociale de l'entreprise
	 */
	static String extractRaisonSociale(RaisonSociale raisonSociale) {
		return extractRaisonSociale(raisonSociale.getLigne1(), raisonSociale.getLigne2(), raisonSociale.getLigne3());
	}

	/**
	 * Retrouve les données valides de la raison sociale d'une entreprise
	 * @param entreprise entreprise dont on veut extraire la raison sociale courante
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un structure (qui peut être vide) contenant les données de la raison sociale courante de l'entreprise
	 */
	@NotNull
	private RaisonSocialeData extractRaisonSociale(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> entreprise.getRaisonsSociales().stream()
				.filter(rs -> !rs.getRectifiee())
				.filter(rs -> {
					if (rs.getDateValidite() == null) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
						              String.format("Raison sociale %d (%s) ignorée car sa date de début de validité est nulle.",
						                            rs.getId(),
						                            extractRaisonSociale(rs)));
						return false;
					}
					return true;
				})
				.filter(rs -> {
					if (isFutureDate(rs.getDateValidite())) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
						              String.format("Raison sociale %d (%s) ignorée car sa date de début de validité est dans le futur (%s).",
						                            rs.getId(),
						                            extractRaisonSociale(rs),
						                            StringRenderers.DATE_RENDERER.toString(rs.getDateValidite())));
						return false;
					}
					return true;
				})
				.peek(rs -> {
					if (isDateLouche(rs.getDateValidite())) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.WARN,
						              String.format("Raison sociale %d (%s) avec une date de validité antérieure au %s (%s).",
						                            rs.getId(),
						                            extractRaisonSociale(rs),
						                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                            StringRenderers.DATE_RENDERER.toString(rs.getDateValidite())));
					}
				})
				.max(Comparator.naturalOrder())
				.map(rs -> new RaisonSocialeData(extractRaisonSociale(rs), rs.getDateValidite()))
				.orElseGet(() -> new RaisonSocialeData(null, null)));
	}

	/**
	 * Retrouve les dernières données valides du capital d'une entreprise
	 * @param entreprise entreprise dont on veut extraire le capital courant
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un structure (qui peut être vide) contenant les données du capital courant de l'entreprise
	 */
	@NotNull
	private CapitalData extractCapital(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> entreprise.getCapitaux().stream()
				.filter(c -> !c.isRectifiee())
				.filter(c -> {
					if (c.getDateEvolutionCapital() == null) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
						              String.format("Capital %d (%s) ignoré car sa date de début de validité est nulle.",
						                            c.getId().getSeqNo(),
						                            c.getCapitalLibere()));
						return false;
					}
					return true;
				})
				.filter(c -> {
					if (isFutureDate(c.getDateEvolutionCapital())) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
						              String.format("Capital %d (%s) ignoré car sa date de début de validité est dans le futur (%s).",
						                            c.getId().getSeqNo(),
						                            c.getCapitalLibere(),
						                            StringRenderers.DATE_RENDERER.toString(c.getDateEvolutionCapital())));
						return false;
					}
					return true;
				})
				.peek(c -> {
					if (isDateLouche(c.getDateEvolutionCapital())) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.WARN,
						              String.format("Capital %d (%s) avec une date de début de validité antérieure au %s (%s).",
						                            c.getId().getSeqNo(),
						                            c.getCapitalLibere(),
						                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                            StringRenderers.DATE_RENDERER.toString(c.getDateEvolutionCapital())));
					}
				})
				.max(Comparator.naturalOrder())
				.map(c -> new CapitalData(c.getCapitalLibere(), c.getDateEvolutionCapital()))
				.orElseGet(() -> new CapitalData(null, null)));
	}

	/**
	 * Retrouve les données valides de la forme juridique d'une entreprise
	 * @param entreprise entreprise dont on veut extraire forme juridique courante
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un structure (qui peut être vide) contenant les données de la forme juridique courante de l'entreprise
	 */
	@NotNull
	private FormeJuridiqueData extractFormeJuridique(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> entreprise.getFormesJuridiques().stream()
				.filter(fj -> !fj.isRectifiee())
				.filter(fj -> {
					if (fj.getDateValidite() == null) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
						              String.format("Forme juridique %d (%s) ignorée car sa date de début de validité est nulle.",
						                            fj.getPk().getSeqNo(),
						                            fj.getType()));
						return false;
					}
					return true;
				})
				.filter(fj -> {
					if (isFutureDate(fj.getDateValidite())) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
						              String.format("Forme juridique %d (%s) ignorée car sa date de début de validité est dans le futur (%s).",
						                            fj.getPk().getSeqNo(),
						                            fj.getType(),
						                            StringRenderers.DATE_RENDERER.toString(fj.getDateValidite())));
						return false;
					}
					return true;
				})
				.peek(fj -> {
					if (isDateLouche(fj.getDateValidite())) {
						mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.WARN,
						              String.format("Forme juridique %d (%s) avec date de début de validité antérieure au %s (%s).",
						                            fj.getPk().getSeqNo(),
						                            fj.getType(),
						                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                            StringRenderers.DATE_RENDERER.toString(fj.getDateValidite())));
					}
				})
				.max(Comparator.naturalOrder())
				.map(c -> new FormeJuridiqueData(c.getType(), c.getDateValidite()))
				.orElseGet(() -> new FormeJuridiqueData(null, null)));
	}

	/**
	 * Passe en revue les fors fiscaux générés pour l'entreprise et efface ceux qui sont déjà annulés
	 * @param data la données de l'entreprise à traiter
	 */
	private void effacementForsAnnules(EffacementForsAnnulesData data) {
		final Entreprise e = data.entrepriseSupplier.get();
		final Set<ForFiscal> forsFiscaux = e.getForsFiscaux();
		if (forsFiscaux != null) {
			final Set<ForFiscal> sansAnnules = forsFiscaux.stream()
					.filter(ff -> {
						if (ff.isAnnule()) {
							uniregStore.removeEntityFromDb(ff);
							return false;
						}
						return true;
					})
					.collect(Collectors.toSet());
			if (sansAnnules.size() != forsFiscaux.size()) {
				e.setForsFiscaux(sansAnnules);
			}
		}
	}

	/**
	 * Extraction des dossiers fiscaux valides d'une entreprise
	 * @param e entreprise de RegPM
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return les dossiers fiscaux de l'entreprise
	 */
	@NotNull
	private DossiersFiscauxData extractDossiersFiscaux(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

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
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return les données civiles collectées (peut être <code>null</code> si l'entreprise n'a pas de pendant civil)
	 */
	@Nullable
	private DonneesCiviles extractDonneesCiviles(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			final Long idCantonal = e.getNumeroCantonal();
			if (idCantonal == null) {
				final LogLevel logLevel = activityManager.isActive(e) ? LogLevel.ERROR : LogLevel.INFO;
				mr.addMessage(LogCategory.SUIVI, logLevel, "Pas de numéro cantonal assigné, pas de lien vers le civil.");
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
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un container des fors principaux valides de l'entreprise
	 */
	@NotNull
	private ForsPrincipauxData extractForsPrincipaux(RegpmEntreprise regpm, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(regpm);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

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
										                               String.format("For fiscal principal %d %s ignoré en raison de la présence à la même date (%s) d'un for fiscal principal différent de type %s.",
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
							mr.addMessage(LogCategory.FORS, LogLevel.ERROR,
							              String.format("Le for principal %d est ignoré car il a une date de début nulle.", ff.getId().getSeqNo()));
							return false;
						}
						return true;
					})
					.filter(ff -> {
						if (isFutureDate(ff.getDateValidite())) {
							mr.addMessage(LogCategory.FORS, LogLevel.ERROR,
							              String.format("Le for principal %d est ignoré car il a une date de début dans le futur (%s).",
							                            ff.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(ff.getDateValidite())));
							return false;
						}
						return true;
					})
					.peek(ff -> {
						if (isDateLouche(ff.getDateValidite())) {
							mr.addMessage(LogCategory.FORS, LogLevel.WARN,
							              String.format("Le for principal %d a une date de début antérieure au %s (%s).",
							                            ff.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
							                            StringRenderers.DATE_RENDERER.toString(ff.getDateValidite())));
						}
					})
					.collect(Collectors.toList());

			return new ForsPrincipauxData(liste);
		});
	}

	@NotNull
	private static <T> List<T> neverNull(@Nullable List<T> source) {
		return Optional.ofNullable(source).orElse(Collections.emptyList());
	}

	@NotNull
	private static <T> Set<T> neverNull(@Nullable Set<T> source) {
		return Optional.ofNullable(source).orElse(Collections.emptySet());
	}

	/**
	 * @param data données d'identification de l'entreprise dont on veut contrôler l'assujettissement
	 * @param mr collecteur de message de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	private void comparaisonAssujettissements(ComparaisonAssujettissementsData data, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey keyEntreprise = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntreprise, mr, idMapper, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();
			try {
				// assujettissements ICC dans RegPM
				final List<DateRange> lilic = neverNull(DateRangeHelper.merge(data.regpmAssujettissements.stream()
						                                                              .filter(a -> a.getType() == RegpmTypeAssujettissement.LILIC)
						                                                              .map(a -> new DateRangeHelper.Range(a.getDateDebut(), a.getDateFin()))
						                                                              .collect(Collectors.toList())));
				final List<DateRange> lilicIntersectant = new ArrayList<>(lilic.size());

				// assujettissements calculés par Unireg
				final List<DateRange> calcules = neverNull(DateRangeHelper.merge(assujettissementService.determine(entreprise)));
				final List<DateRange> calculesIntersectant = new ArrayList<>(calcules.size());

				// assujettissements complètement apparus
				for (DateRange apparu : calcules) {
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

				// si la PM est déclarée "inactive" mais qu'Unireg lui calcule un assujettissement après la date seuil du 01.01.2015,
				// c'est un problème, non ?
				if (!data.active && DateRangeHelper.intersect(new DateRangeHelper.Range(seuilActivite, null), calcules)) {
					mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.ERROR,
					              String.format("Assujettissement calculé après le %s sur une entreprise considérée comme inactive.", StringRenderers.DATE_RENDERER.toString(seuilActivite)));
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
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	private void controleCouvertureFors(CouvertureForsData data, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey keyEntreprise = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntreprise, mr, idMapper, () -> {
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
							final ForFiscalPrincipalPM forApresTrou = Optional.ofNullable(dateFinTrou)
									.map(forsPrincipaux::higherEntry)
									.map(Map.Entry::getValue)
									.orElse(null);
							if (forApresTrou != null) {
								// on change la date de début pour couvrir le trou
								mr.addMessage(LogCategory.FORS, LogLevel.WARN,
								              String.format("La date de début du for fiscal principal %s est adaptée (-> %s) pour couvrir les fors secondaires.",
								                            StringRenderers.DATE_RANGE_RENDERER.toString(forApresTrou),
								                            StringRenderers.DATE_RENDERER.toString(nonCouvert.getDateDebut())));

								// on va maintenant travailler sur une copie du for
								final ForFiscalPrincipalPM duplicate = duplicate(forApresTrou);

								// on annule le for trop court et on l'enlève de la map (on remettra ensuite ce qu'il faut)
								forApresTrou.setAnnule(true);
								forsPrincipaux.remove(forApresTrou.getDateDebut());

								// il ne suffit pas de le dire, il faut le faire...
								duplicate.setDateDebut(nonCouvert.getDateDebut());

								// on remet le ou les remplaçant(s) dans la liste des fors
								adapterAutourFusionsCommunes(duplicate, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes).stream()
										.peek(ff -> forsPrincipaux.put(ff.getDateDebut(), ff))
										.forEach(entreprise::addForFiscal);
							}
							else {
								// TODO il y a un souci, non ?
								// s'il n'y a pas de for principal après, c'est que le dernier for principal est fermé
								// mais s'il est fermé, c'est que l'entreprise est dissoute, non ?
								// comment, dans ce cas, peut-il encore y avoir des fors secondaires actifs après la dissolution ?

								// il n'y a pas de for après, on regarde avant...
								final RegDate dateDebutTrou = nonCouvert.getDateDebut();
								final ForFiscalPrincipalPM forAvantTrou = Optional.ofNullable(dateDebutTrou)
										.map(forsPrincipaux::lowerEntry)
										.map(Map.Entry::getValue)
										.orElse(null);
								if (forAvantTrou != null) {
									// on change la date de fin pour couvrir le trou
									mr.addMessage(LogCategory.FORS, LogLevel.WARN,
									              String.format("La date de fin du for fiscal principal %s est adaptée (-> %s) pour couvrir les fors secondaires.",
									                            StringRenderers.DATE_RANGE_RENDERER.toString(forAvantTrou),
									                            StringRenderers.DATE_RENDERER.toString(nonCouvert.getDateFin())));

									// on va maintenant travailler sur une copie du for
									final ForFiscalPrincipalPM duplicate = duplicate(forAvantTrou);

									// on annule le for trop court et on l'enlève de la map (on remettra ensuite ce qu'il faut)
									forAvantTrou.setAnnule(true);
									forsPrincipaux.remove(forAvantTrou.getDateDebut());

									// là non plus, il ne suffit pas de le dire...
									duplicate.setDateFin(nonCouvert.getDateFin());
									if (nonCouvert.getDateFin() == null) {
										duplicate.setMotifFermeture(null);
									}

									// on remet le ou les remplaçant(s) dans la liste des fors
									adapterAutourFusionsCommunes(duplicate, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes).stream()
											.peek(ff -> forsPrincipaux.put(ff.getDateDebut(), ff))
											.forEach(entreprise::addForFiscal);
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
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	private void controleForsSecondaires(ControleForsSecondairesData data, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey keyEntiteJuridique = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntiteJuridique, mr, idMapper, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();

			// on va construire des périodes par commune (no OFS), et vérifier qu'on a bien les mêmes des deux côtés
			final Map<Integer, List<DateRange>> avantMigration = data.regpm.stream()
					.collect(Collectors.toMap(fs -> NO_OFS_COMMUNE_EXTRACTOR.apply(fs.getCommune()),
					                          Collections::singletonList,
					                          DATE_RANGE_LIST_MERGER,
					                          TreeMap::new));
			final Set<ForFiscal> forsFiscaux = neverNull(entreprise.getForsFiscaux());      // en cas de nouvelle entreprise, la collection est nulle
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
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	private void createForsSecondairesImmeuble(ForsSecondairesData.Immeuble data, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey keyEntiteJuridique = data.entiteJuridiqueSupplier.getKey();
		doInLogContext(keyEntiteJuridique, mr, idMapper, () -> {
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
					communeData.getValue().stream()
							.map(range -> {
								final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
								ffs.setDateDebut(range.getDateDebut());
								ffs.setDateFin(range.getDateFin());
								ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
								ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
								ffs.setNumeroOfsAutoriteFiscale(noOfsCommune);
								ffs.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
								ffs.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
								ffs.setMotifFermeture(range.getDateFin() != null ? MotifFor.VENTE_IMMOBILIER : null);
								checkFractionCommuneVaudoise(ffs, mr, LogCategory.FORS);
								return ffs;
							})
							.map(ffs -> adapterAutourFusionsCommunes(ffs, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes))
							.flatMap(List::stream)
							.peek(ffs -> mr.addMessage(LogCategory.FORS, LogLevel.INFO,
							                           String.format("For secondaire 'immeuble' %s ajouté sur la commune %d.",
							                                         StringRenderers.DATE_RANGE_RENDERER.toString(ffs),
							                                         noOfsCommune)))
							.forEach(entiteJuridique::addForFiscal);
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

		// enregistrement de cette entreprise pour la suppression finale des fors créés et déjà annulés
		mr.addPreTransactionCommitData(new EffacementForsAnnulesData(moi));

		// enregistrement de cette entreprise pour la comparaison des assujettissements avant/après
		mr.addPreTransactionCommitData(new ComparaisonAssujettissementsData(activityManager.isActive(regpm), regpm.getAssujettissements(), moi));

		// TODO migrer les adresses, les documents...

		final String raisonSociale = mr.getExtractedData(RaisonSocialeData.class, moi.getKey()).getRaisonSociale();
		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, raisonSociale, unireg, mr);
		migratePersonneContact(regpm.getContact1(), unireg, mr);
		migrateFlagDebiteurInactif(regpm, unireg, mr);
		migrateDonneesRegistreCommerce(regpm, unireg, mr);

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
	 * Reconstitution des données du registre du commerce
	 * @param regpm entreprise à migrer
	 * @param unireg entreprise destination de la migration dans Unireg
	 * @param mr collecteur de messages de migration
	 */
	private static void migrateDonneesRegistreCommerce(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final EntityKey key = buildEntrepriseKey(regpm);
		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, key).date;

		// raison sociale (= la dernière)
		final RaisonSocialeData rsData = mr.getExtractedData(RaisonSocialeData.class, key);

		// capital (= le dernier en date)
		final CapitalData capitalData = mr.getExtractedData(CapitalData.class, key);

		// forme juridique (= la dernière en date)
		final FormeJuridiqueData formeJuridiqueData = mr.getExtractedData(FormeJuridiqueData.class, key);

		// on cherche ensuite la dernière date à partir de laquelle les éléments ci-dessus avaient leur dernière valeur
		final RegDate dateDebut = Stream.of(rsData.getDateValidite(), capitalData.dateValidite, formeJuridiqueData.dateValidite)
				.filter(date -> date != null)
				.max(Comparator.naturalOrder())
				.orElse(getDateDebutActivite(regpm, mr));

		final RegDate dateFin;
		if (dateDebut == null) {
			mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
			              "Impossible de déterminer la date de début des données du registre du commerce (dernières dates de raison sociale, de capitaux et de forme juridiques inexistantes).");
			return;
		}
		else if (RegDateHelper.isAfter(dateDebut, dateFinActivite, NullDateBehavior.LATEST)) {
			mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
			              String.format("Date de début des données du registre du commerce (%s) postérieure à la date de fin d'activité calculée (%s), cette dernière est donc ignorée ici.",
			                            StringRenderers.DATE_RENDERER.toString(dateDebut),
			                            StringRenderers.DATE_RENDERER.toString(dateFinActivite)));
			dateFin = null;
		}
		else {
			dateFin = dateFinActivite;
		}

		// capital à prendre en compte
		final MontantMonetaire capital;
		if (capitalData.capital != null) {
			capital = new MontantMonetaire(capitalData.capital.longValue(), MontantMonetaire.CHF);     // tous les montants dans RegPM sont en CHF
		}
		else {
			capital = null;
		}

		// forme juridique
		final FormeJuridiqueEntreprise formeJuridique;
		if (formeJuridiqueData.type != null) {
			formeJuridique = toFormeJuridique(formeJuridiqueData.type.getCode());
		}
		else {
			formeJuridique = null;
		}

		// si on n'a rien, pas la peine de générer une donnée
		if (StringUtils.isNotBlank(rsData.getRaisonSociale()) || capital != null || formeJuridique != null) {
			final DonneesRegistreCommerce rc = new DonneesRegistreCommerce(dateDebut, dateFin,
			                                                               rsData.getRaisonSociale(),
			                                                               capital,
			                                                               formeJuridique);
			unireg.addDonneesRC(rc);

			mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
			              String.format("Données 'civiles' migrées : sur la période %s, raison sociale (%s), capital (%s) et forme juridique (%s).",
			                            StringRenderers.DATE_RANGE_RENDERER.toString(rc),
			                            rc.getRaisonSociale(),
			                            rc.getCapital() != null ? StringRenderers.MONTANT_MONETAIRE_RENDERER.toString(rc.getCapital()) : StringUtils.EMPTY,
			                            rc.getFormeJuridique()));
		}
	}

	private static FormeJuridiqueEntreprise toFormeJuridique(String codeRegpm) {

		// TODO mapping à valider...

		if (StringUtils.isBlank(codeRegpm)) {
			return null;
		}

		switch (codeRegpm) {
		case "ASS":
			return FormeJuridiqueEntreprise.ASSOCIATION;
		case "DP":
			return FormeJuridiqueEntreprise.CORP_DP_ADM;
		case "DP/PM":
			return FormeJuridiqueEntreprise.CORP_DP_ENT;
		case "FDS. PLAC.":
			return FormeJuridiqueEntreprise.SCPC;
		case "FOND":
			return FormeJuridiqueEntreprise.FONDATION;
		case "S. COMM.":
			return FormeJuridiqueEntreprise.SC;
		case "S. COOP.":
			return FormeJuridiqueEntreprise.SCOOP;
		case "S.A.":
			return FormeJuridiqueEntreprise.SA;
		case "S.A.R.L.":
			return FormeJuridiqueEntreprise.SARL;
		case "S.COMM.ACT":
			return FormeJuridiqueEntreprise.SCA;
		case "S.N.C.":
			return FormeJuridiqueEntreprise.SNC;
		default:
			throw new IllegalArgumentException("Code de forme juridique non-supporté : " + codeRegpm);
		}
	}

	/**
	 * Assigne le flag "débiteur inactif" sur l'entreprise Unireg en fonction de critères internes à la données de RegPM
	 * @param regpm entreprise à migrer
	 * @param unireg entreprise destination de la migration dans Unireg
	 * @param mr collecteur de messages de migration
	 */
	private static void migrateFlagDebiteurInactif(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		// la règle dit : la ligne 1 de la raison sociale commence par une étoile...
		if (StringUtils.isNotBlank(regpm.getRaisonSociale1()) && regpm.getRaisonSociale1().startsWith("*")) {
			unireg.setDebiteurInactif(true);
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Entreprise identifiée comme un doublon.");
		}
	}

	/**
	 * Migration de la personne de contact d'une entreprise
	 * @param regpm l'entreprise RegPM
	 * @param unireg l'entreprise Unireg
	 * @param mr collecteur de messages de migration
	 */
	private static void migratePersonneContact(ContactEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		if (regpm != null) {
			final String noTelephone = canonizeTelephoneNumber(regpm.getNoTelephone(), "téléphone", mr);
			final String noFax = canonizeTelephoneNumber(regpm.getNoFax(), "fax", mr);
			unireg.setNumeroTelephoneProfessionnel(noTelephone);
			unireg.setNumeroTelecopie(noFax);
			unireg.setPersonneContact(StringUtils.trimToNull(new NomPrenom(regpm.getNom(), regpm.getPrenom()).getNomPrenom()));
		}
	}

	@Nullable
	private static String canonizeTelephoneNumber(String noTelephone, String type, MigrationResultProduction mr) {
		if (StringUtils.isBlank(noTelephone)) {
			return null;
		}

		// on enlève tous les non-chiffres
		final String canonical = noTelephone.replaceAll("[^0-9]+", "");
		if (StringUtils.isBlank(canonical)) {
			// il n'y a plus rien ???
			return null;
		}

		// si le numéro ne comporte pas exactement 10 chiffres, il faut le lister
		if (canonical.length() != 10) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
			              String.format("Numéro de %s '%s' ne comportant pas exactement 10 chiffres (%d)...", type, noTelephone, canonical.length()));
		}
		return canonical;
	}

	/**
	 * Classe interne qui contient la donnée d'une commune Suisse ou d'un pays, de manière exclusive
	 */
	private static final class CommuneOuPays {

		private final RegpmCommune commune;
		private final Integer noOfsPays;

		public CommuneOuPays(int noOfsPays) {
			this.commune = null;
			this.noOfsPays = noOfsPays;
		}

		public CommuneOuPays(@NotNull Supplier<RegpmCommune> communeSupplier, @NotNull Supplier<Integer> noOfsPaysSupplier) {
			final RegpmCommune commune = communeSupplier.get();
			if (commune != null) {
				this.commune = commune;
				this.noOfsPays = null;
			}
			else {
				this.commune = null;
				this.noOfsPays = noOfsPaysSupplier.get();
			}
		}

		public CommuneOuPays(@NotNull RegpmForPrincipal ffp) {
			this(ffp::getCommune, ffp::getOfsPays);
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
	 * Construit une instance de {@link CommuneOuPays} en remplaçant éventuellement les territoires par l'état souverain correspond
	 * @param dateReference date de référence pour les données du pays éventuel
	 * @param communeSupplier accesseur à une commune (qui peut être absente)
	 * @param noOfsPaysSupplier accesseur à un numéro OFS de pays (qui peut être vide)
	 * @param mr collecteur de messages de suivi
	 * @return l'instance de {@link CommuneOuPays} à utiliser
	 */
	@NotNull
	private CommuneOuPays buildCommuneOuPays(@NotNull RegDate dateReference,
	                                         @NotNull Supplier<RegpmCommune> communeSupplier,
	                                         @NotNull Supplier<Integer> noOfsPaysSupplier,
	                                         @Nullable String contexte,
	                                         MigrationResultProduction mr,
	                                         LogCategory logCategory) {
		final CommuneOuPays cop = new CommuneOuPays(communeSupplier, noOfsPaysSupplier);
		if (cop.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			// Dans le mainframe, il y avait un pays (8997 nommé 'Ex Gibraltar (voir 8213)') qui n'a pas été repris dans FiDoR...
			// Ici, on va faire comme si le pays vu était Gibraltar (8213)
			final int noOfsPays;
			final int noOfsPaysCorrigeGibraltar = cop.noOfsPays == 8997 ? 8213 : cop.noOfsPays;
			final Pays pays = infraService.getPays(noOfsPaysCorrigeGibraltar, dateReference);
			if (pays != null && !pays.isEtatSouverain()) {
				mr.addMessage(logCategory, LogLevel.WARN,
				              String.format("Le pays %d%s n'est pas un état souverain, remplacé par l'état %d.",
				                            cop.noOfsPays,
				                            StringUtils.isBlank(contexte) ? StringUtils.EMPTY : String.format(" (%s)", contexte),
				                            pays.getNoOfsEtatSouverain()));
				noOfsPays = pays.getNoOfsEtatSouverain();
			}
			else {
				noOfsPays = noOfsPaysCorrigeGibraltar;
			}

			if (noOfsPays != cop.noOfsPays) {
				return new CommuneOuPays(noOfsPays);
			}
		}
		return cop;
	}

	/**
	 * Génération de l'établissement principal à partir du dernier siège de l'entreprise
	 * @param regpm l'entreprise de RegPM
	 * @param unireg l'entreprise dans Unireg
	 * @param linkCollector le collecteur de liens à créer entre les entités
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param mr collecteur de messages de migration
	 */
	private void generateEtablissementPrincipal(RegpmEntreprise regpm, Entreprise unireg, EntityLinkCollector linkCollector, IdMapping idMapper, MigrationResultProduction mr) {

		// TODO peut-être pourra-t-on faire mieux avec les données de RCEnt, mais pour le moment, on suppose l'existence d'UN SEUL établissement principal avec, au besoin, plusieurs domiciles successifs

		// la spécification ne parle pas de l'attribut commune ni des fors principaux pour la génération de l'établissement principal
		// mais seulement de la récupération du dernier siège depuis la table SIEGE_ENTREPRISE

		// on retrie les sièges par date de validité (le tri naturel est fait par numéro de séquence) en ignorant au passage les sièges annulés ou dont la date de début est dans le futur
		final NavigableMap<RegDate, List<RegpmSiegeEntreprise>> siegesEffectifs = regpm.getSieges().stream()
				.filter(s -> !s.isRectifiee())
				.filter(s -> {
					if (s.getDateValidite() == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Le siège %d est ignoré car il a une date de début de validité nulle (ou avant 1291).", s.getId().getSeqNo()));
						return false;
					}
					return true;
				})
				.filter(s -> {
					if (isFutureDate(s.getDateValidite())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Le siège %d est ignoré car il a une date de début de validité dans le futur (%s).",
						                            s.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(s.getDateValidite())));
						return false;
					}
					return true;
				})
				.peek(s -> {
					if (isDateLouche(s.getDateValidite())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Le siège %d a une date de validité antérieure au %s (%s).",
						                            s.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                            StringRenderers.DATE_RENDERER.toString(s.getDateValidite())));
					}
				})
				.collect(Collectors.toMap(RegpmSiegeEntreprise::getDateValidite,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
				                          TreeMap::new));

		// pas de donnée de siège -> pas d'établissement principal créé
		if (siegesEffectifs.isEmpty()) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Pas de siège associé, pas d'établissement principal créé.");
			return;
		}

		// on ne prend en compte que le dernier (on loggue les autres...)
		final Map.Entry<RegDate, List<RegpmSiegeEntreprise>> donneesDateReference = siegesEffectifs.lastEntry();
		siegesEffectifs.headMap(donneesDateReference.getKey(), false).values().stream()
				.flatMap(List::stream)
				.forEach(siege -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				                                String.format("Siège %d non-migré car on ne prend en compte que le dernier.", siege.getId().getSeqNo())));

		// si la dernière date fait référence à plusieurs sièges, on prend le dernier (= en fonction de son numéro de séquence, cette fois)
		final NavigableMap<Integer, RegpmSiegeEntreprise> siegesDateReference = donneesDateReference.getValue().stream()
				.collect(Collectors.toMap(s -> s.getId().getSeqNo(),
				                          Function.identity(),
				                          (s1, s2) -> { throw new IllegalArgumentException("Plusieurs sièges avec le même numéro de séquence " + s1.getId().getSeqNo() + " sur l'entreprise " + regpm.getId()); },
				                          TreeMap::new));

		final Map.Entry<Integer, RegpmSiegeEntreprise> donneesSiegeReference = siegesDateReference.lastEntry();
		siegesDateReference.headMap(donneesSiegeReference.getKey(), false).values().stream()
				.forEach(siege -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				                                String.format("Siège %d non-migré car on ne prend en compte que le dernier.", siege.getId().getSeqNo())));

		final Etablissement etbPrincipal = uniregStore.saveEntityToDb(new Etablissement());
		final Supplier<Etablissement> etbPrincipalSupplier = getEtablissementByUniregIdSupplier(etbPrincipal.getNumero());
		etbPrincipal.setEnseigne(regpm.getEnseigne());
		etbPrincipal.setRaisonSociale(mr.getExtractedData(RaisonSocialeData.class, buildEntrepriseKey(regpm)).getRaisonSociale());
		etbPrincipal.setPrincipal(true);

		// un peu de log pour indiquer la création de l'établissement principal
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Création de l'établissement principal %s d'après le siège %d.",
		                                                              FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
		                                                              donneesSiegeReference.getKey()));

		// lien entre l'établissement principal et son entreprise
		final KeyedSupplier<Entreprise> entrepriseSupplier = getEntrepriseSupplier(idMapper, regpm);
		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, buildEntrepriseKey(regpm)).date;
		linkCollector.addLink(new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(etbPrincipalSupplier, entrepriseSupplier, donneesDateReference.getKey(), dateFinActivite));

		// domiciles selon les localisations trouvées plus haut (pour l'instant, sans date de fin... qui seront assignées juste après...)
		final RegpmSiegeEntreprise siege = donneesSiegeReference.getValue();
		final CommuneOuPays cop = buildCommuneOuPays(siege.getDateValidite(), siege::getCommune, siege::getNoOfsPays, "siège", mr, LogCategory.SUIVI);
		final DomicileEtablissement domicile = new DomicileEtablissement(siege.getDateValidite(), dateFinActivite, cop.getTypeAutoriteFiscale(), cop.getNumeroOfsAutoriteFiscale(), null);
		checkFractionCommuneVaudoise(domicile, mr, LogCategory.SUIVI);

		// liaison des domiciles à l'établissement
		adapterAutourFusionsCommunes(domicile, mr, LogCategory.SUIVI, null).stream()
				.peek(d -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Domicile de l'établissement principal %s : %s sur %s/%d.",
				                                                                         FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
				                                                                         StringRenderers.DATE_RANGE_RENDERER.toString(d),
				                                                                         d.getTypeAutoriteFiscale(),
				                                                                         d.getNumeroOfsAutoriteFiscale())))
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
		final KeyedSupplier<Entreprise> moi = getEntrepriseSupplier(idMapper, regpm);

		// migration des fusions (cette entreprise étant la source)
		regpm.getFusionsApres().stream()
				.filter(fusion -> !fusion.isRectifiee())
				.forEach(apres -> {
					// TODO et les autres informations de la fusion (forme, date d'inscription, date de contrat, date de bilan... ?)
					final KeyedSupplier<Entreprise> apresFusion = getEntrepriseSupplier(idMapper, apres.getEntrepriseApres());
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
		final KeyedSupplier<Entreprise> moi = getEntrepriseSupplier(idMapper, regpm);

		// migration des mandataires -> liens à créer par la suite (on les trie pour la reproductibilité en test)
		regpm.getMandataires().stream()
				.sorted(Comparator.comparing(mandat -> mandat.getId().getNoSequence()))
				.forEach(mandat -> {

					// récupération du mandataire qui peut être une autre entreprise, un établissement ou un individu
					final KeyedSupplier<? extends Contribuable> mandataire = getPolymorphicSupplier(idMapper, mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
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

						// TODO ne manque-t-il pas le titulaire du compte pour les coordonnées financières ?
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
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, String.format("La date d'attribution du mandat %s est dans le futur (%s), le mandat sera donc ignoré dans la migration.",
						                                                               mandat.getId(), StringRenderers.DATE_RENDERER.toString(mandat.getDateAttribution())));
						return;
					}

					if (isDateLouche(mandat.getDateAttribution())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, String.format("La date d'attribution du mandat %s est antérieure au %s (%s).",
						                                                              mandat.getId(),
						                                                              StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                                                              StringRenderers.DATE_RENDERER.toString(mandat.getDateAttribution())));
					}

					// date de fin dans le futur -> on ignore la date de fin
					final RegDate dateFin;
					if (isFutureDate(mandat.getDateResiliation())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, String.format("La date de résiliation du mandat %s est dans le futur (%s), le mandat sera donc laissé ouvert dans la migration.",
						                                                               mandat.getId(), StringRenderers.DATE_RENDERER.toString(mandat.getDateResiliation())));
						dateFin = null;
					}
					else {
						dateFin = mandat.getDateResiliation();
					}

					if (dateFin != null && isDateLouche(dateFin)) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, String.format("La date de résiliation du mandat %s est antérieure au %s (%s).",
						                                                              mandat.getId(),
						                                                              StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                                                              StringRenderers.DATE_RENDERER.toString(mandat.getDateResiliation())));
					}

					// ajout du lien entre l'entreprise et son mandataire
					linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi, mandataire, mandat.getDateAttribution(), dateFin, typeMandat, iban, bicSwift));
				});
	}

	/**
	 * Extraction de la date de bouclement futur, qui peut être ignorée si elle est avant la fin du dernier exercice commercial connu
	 * @param regpm l'entreprise dans RegPM
	 * @param mr le collecteur de messages de suivi
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return une structure de données (non-nulle) qui contient la date de bouclement futur retenue (potentiellement nulle)
	 */
	@NotNull
	private DateBouclementFuturData extractDateBouclementFutur(RegpmEntreprise regpm, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey key = buildEntityKey(regpm);
		return doInLogContext(key, mr, idMapper, () -> {
			final RegDate brutto = regpm.getDateBouclementFutur();
			final SortedSet<RegpmExerciceCommercial> exercicesCommerciaux = regpm.getExercicesCommerciaux();
			if (exercicesCommerciaux != null && !exercicesCommerciaux.isEmpty()) {
				final RegpmExerciceCommercial dernierExcerciceConnu = exercicesCommerciaux.last();
				if (brutto != null && brutto.isBefore(dernierExcerciceConnu.getDateFin())) {
					mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
					              String.format("Date de bouclement futur (%s) ignorée car antérieure à la date de fin du dernier exercice commercial connu (%s).",
					                            StringRenderers.DATE_RENDERER.toString(brutto),
					                            StringRenderers.DATE_RENDERER.toString(dernierExcerciceConnu.getDateFin())));
					return new DateBouclementFuturData(null);
				}
			}
			return new DateBouclementFuturData(brutto);
		});
	}

	private void migrateExercicesCommerciaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final RegDate dateBouclementFutur = mr.getExtractedData(DateBouclementFuturData.class, buildEntrepriseKey(regpm)).date;
		final List<RegDate> datesBouclements;
		final SortedSet<RegpmExerciceCommercial> exercicesCommerciaux = regpm.getExercicesCommerciaux();
		if (exercicesCommerciaux != null && !exercicesCommerciaux.isEmpty()) {

			final Stream.Builder<RegDate> additionalDatesStreamBuilder = Stream.builder();

			// dans RegPM, les exercices commerciaux ne sont instanciés que quand une DI est retournée, ce qui a pour conséquence immédiate qu'en l'absence
			// d'assujettissement, aucune DI n'est bien-sûr envoyée, encore moins retournée, et donc aucun exercice commercial n'est créé en base...
			// donc, si on trouve des trous dans les exercices commerciaux en base, cela correspond à une interruption de l'assujettissement, et il faut le combler
			// (on va supposer des exercices commerciaux annuels, faute de mieux, dans la période non-mappée)

			final RegpmExerciceCommercial dernierConnu = exercicesCommerciaux.last();
			final RegDate dateFinDernierExercice = dernierConnu.getDateFin();

			// période totale maximale couverte (potentiellement partiellement, c'est justement ce qui nous intéresse ici...) par des exercices commerciaux de regpm
			final DateRange lifespan = new DateRangeHelper.Range(exercicesCommerciaux.first().getDateDebut(), dateFinDernierExercice);
			final List<DateRange> mapped = exercicesCommerciaux.stream().map(ex -> new DateRangeHelper.Range(ex.getDateDebut(), ex.getDateFin())).collect(Collectors.toList());
			final List<DateRange> notMapped = DateRangeHelper.subtract(lifespan, mapped);
			if (!notMapped.isEmpty()) {

				// il y a bien au moins une (peut-être plusieurs...) période qui n'est pas mappée...
				// pour chacune d'entre elles, on va supposer des exercices commerciaux annuels (à la date d'ancrage du dernier exercice avant le trou) pour combler
				for (DateRange range : notMapped) {

					// on sait que la fin du range est forcément une date de bouclement (puisqu'un exercice commercial débute au lendemain)
					// et ensuite on case autant d'années que nécessaire pour boucher le trou
					for (RegDate bouclement = range.getDateFin(); bouclement.isAfterOrEqual(range.getDateDebut()); bouclement = bouclement.addYears(-1)) {
						additionalDatesStreamBuilder.accept(bouclement);

						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Ajout d'une date de bouclement estimée au %s pour combler l'absence d'exercice commercial dans RegPM sur la période %s.",
						                            StringRenderers.DATE_RENDERER.toString(bouclement),
						                            StringRenderers.DATE_RANGE_RENDERER.toString(range)));
					}
				}
			}

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
		final List<PeriodeFiscale> pfs = neverNull(uniregStore.getEntitiesFromDb(PeriodeFiscale.class, params));
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

		final DeclarationImpotOrdinairePM di = new DeclarationImpotOrdinairePM();
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

		final EntityKey moi = buildEntrepriseKey(regpm);
		final List<RegpmDossierFiscal> dossiers = mr.getExtractedData(DossiersFiscauxData.class, moi).liste;
		final Set<RegpmDossierFiscal> dossiersFiscauxAttribuesAuxExercicesCommerciaux = new HashSet<>(dossiers.size());
		final RegDate dateBouclementFutur = mr.getExtractedData(DateBouclementFuturData.class, moi).date;

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
					if (dateBouclementFutur != null && dateBouclementFutur.year() == dossier.getPf()) {
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

			final CommuneOuPays cop = buildCommuneOuPays(f.getDateValidite(), f::getCommune, f::getOfsPays, String.format("for principal %d", f.getId().getSeqNo()), mr, LogCategory.FORS);
			ffp.setTypeAutoriteFiscale(cop.getTypeAutoriteFiscale());
			ffp.setNumeroOfsAutoriteFiscale(cop.getNumeroOfsAutoriteFiscale());

			checkFractionCommuneVaudoise(ffp, mr, LogCategory.FORS);
			return Optional.of(Pair.of(f.getType() == RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, ffp));
		};

		// récupération des fors principaux valides
		final List<RegpmForPrincipal> forsRegpm = mr.getExtractedData(ForsPrincipauxData.class, buildEntrepriseKey(regpm)).liste;
		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, buildEntrepriseKey(regpm)).date;

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
		assigneDatesFin(dateFinActivite, liste);

		// corrections dues aux fusions de communes passées
		final List<ForFiscalPrincipalPM> listeAvecTraitementFusions = liste.stream()
				.map(ff -> adapterAutourFusionsCommunes(ff, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes))
				.flatMap(List::stream)
				.collect(Collectors.toList());

		// assignation des motifs
		calculeMotifsOuvertureFermeture(listeAvecTraitementFusions);

		// on les ajoute au tiers
		listeAvecTraitementFusions.stream()
				.peek(ff -> mr.addMessage(LogCategory.FORS, LogLevel.INFO,
				                          String.format("For principal %s/%d %s généré.",
				                                        ff.getTypeAutoriteFiscale(),
				                                        ff.getNumeroOfsAutoriteFiscale(),
				                                        StringRenderers.DATE_RANGE_RENDERER.toString(ff))))
				.forEach(unireg::addForFiscal);

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
				.map(decision -> adapterAutourFusionsCommunes(decision, mr, LogCategory.FORS, null))
				.flatMap(List::stream)
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

				// on ne veut pas surcharger un motif "FUSION_COMMUNES" placé là par ailleurs
				if (current.getMotifFermeture() != MotifFor.FUSION_COMMUNES) {
					current.setMotifFermeture(motif);
				}
				if (next.getMotifOuverture() != MotifFor.FUSION_COMMUNES) {
					next.setMotifOuverture(motif);
				}
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
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début nulle.",
						                            portee,
						                            rf.getType()));
						return false;
					}
					return true;
				})
				.filter(rf -> {
					if (isFutureDate(rf.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début dans le futur (%s).",
						                            portee,
						                            rf.getType(),
						                            StringRenderers.DATE_RENDERER.toString(rf.getDateDebut())));
						return false;
					}
					return true;
				})
				.filter(rf -> {
					if (dateFinRegimes != null && dateFinRegimes.isBefore(rf.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début (%s) postérieure à la date de fin d'activité de l'entreprise (%s).",
						                            portee,
						                            rf.getType(),
						                            StringRenderers.DATE_RENDERER.toString(rf.getDateDebut()),
						                            StringRenderers.DATE_RENDERER.toString(dateFinRegimes)));
						return false;
					}
					return true;
				})
				.peek(rf -> {
					if (isDateLouche(rf.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Régime fiscal %s %s avec une date de début de validité antérieure au %s (%s).",
						                            portee,
						                            rf.getType(),
						                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                            StringRenderers.DATE_RENDERER.toString(rf.getDateDebut())));
					}
				})
				.map(r -> mapRegimeFiscal(portee, r))
				.collect(Collectors.toList());

		// ... puis attribution des dates de fin
		assigneDatesFin(dateFinRegimes, liste);
		return liste;
	}

	private void migrateRegimesFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, buildEntrepriseKey(regpm)).date;

		// collecte des régimes fiscaux CH...
		final List<RegimeFiscal> listeCH = mapRegimesFiscaux(RegimeFiscal.Portee.CH,
		                                                     regpm.getRegimesFiscauxCH(),
		                                                     dateFinActivite,
		                                                     mr);

		// ... puis des règimes fiscaux VD
		final List<RegimeFiscal> listeVD = mapRegimesFiscaux(RegimeFiscal.Portee.VD,
		                                                     regpm.getRegimesFiscauxVD(),
		                                                     dateFinActivite,
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

	private static final class LocalisationDateeFacade<T> extends LocalisationDatee implements Duplicable<LocalisationDateeFacade<T>>, EntityWrapper<T> {

		private final T payload;

		public LocalisationDateeFacade(LocalizedDateRange source, T payload) {
			super(source.getDateDebut(), source.getDateFin(), source.getTypeAutoriteFiscale(), source.getNumeroOfsAutoriteFiscale());
			this.payload = payload;
		}

		private LocalisationDateeFacade(LocalisationDateeFacade<T> source) {
			this(source, source.payload);
		}

		@Override
		public LocalisationDateeFacade<T> duplicate() {
			return new LocalisationDateeFacade<>(this);
		}

		@Override
		public Object getKey() {
			// c'est bidon, de toute façon, car cela ne devrait jamais être inséré dans une session hibernate
			return null;
		}

		@Override
		public T getWrappedEntity() {
			return payload;
		}
	}

	/**
	 * Classe interne qui permet de faire passer une instance de {@link AllegementFiscal} pour quelque
	 * chose qui implémente l'interface {@link LocalizedDateRange} (les allègements fiscaux considérés
	 * ici sont forcément sur des communes vaudoises...)
	 */
	private static final class LocalizedAllegementFiscal implements LocalizedDateRange {

		private final AllegementFiscal allegement;

		public LocalizedAllegementFiscal(AllegementFiscal allegement) {
			this.allegement = allegement;
			if (allegement.getTypeCollectivite() != AllegementFiscal.TypeCollectivite.COMMUNE || allegement.getNoOfsCommune() == null) {
				throw new IllegalArgumentException("L'utilisation de cette classe est réservée aux allègements fiscaux communaux spécifiques...");
			}
		}

		@Override
		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		}

		@Override
		public Integer getNumeroOfsAutoriteFiscale() {
			return allegement.getNoOfsCommune();
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return allegement.isValidAt(date);
		}

		@Override
		public RegDate getDateDebut() {
			return allegement.getDateDebut();
		}

		@Override
		public RegDate getDateFin() {
			return allegement.getDateFin();
		}
	}

	@NotNull
	private Stream<AllegementFiscal> adapterAllegementFiscalPourFusionsCommunes(final AllegementFiscal allegement, MigrationResultProduction mr, LogCategory logCategory) {
		if (allegement.getTypeCollectivite() == AllegementFiscal.TypeCollectivite.COMMUNE & allegement.getNoOfsCommune() != null) {
			// calcul de la nouvelle répartition sur des communes en prenant en compte les fusions en passant par une structure
			// temporaire de "localisation datée"
			final LocalizedDateRange localizedRange = new LocalizedAllegementFiscal(allegement);
			final LocalisationDateeFacade<AllegementFiscal> facade = new LocalisationDateeFacade<>(localizedRange, allegement);
			return adapterAutourFusionsCommunes(facade, mr, logCategory, null).stream()
					.map(f -> new AllegementFiscal(f.getDateDebut(),
					                               f.getDateFin(),
					                               allegement.getPourcentageAllegement(),
					                               allegement.getTypeImpot(),
					                               allegement.getTypeCollectivite(),
					                               f.getNumeroOfsAutoriteFiscale()));
		}
		else {
			// pas de changement nécessaire pour les fusions de communes (on n'est même pas sur une commune précise !)
			return Stream.of(allegement);
		}
	}

	private void migrateAllegementsFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		regpm.getAllegementsFiscaux().stream()
				.filter(a -> a.getDateAnnulation() == null)                 // on ne prend pas en compte les allègements annulés
				.sorted(Comparator.comparing(a -> a.getId().getSeqNo()))    // tri pour les tests en particulier, pour toujours traiter les allègements dans le même ordre
				.filter(a -> {
					if (isFutureDate(a.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Allègement fiscal %d ignoré en raison de sa date de début dans le futur (%s).",
						                            a.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(a.getDateDebut())));
						return false;
					}
					return true;
				})
				.map(a -> {
					if (isFutureDate(a.getDateFin())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Date de fin (%s) de l'allègement fiscal %d ignorée (date future).",
						                            StringRenderers.DATE_RENDERER.toString(a.getDateFin()),
						                            a.getId().getSeqNo()));
						final RegpmAllegementFiscal raf = new RegpmAllegementFiscal();
						raf.setCommune(a.getCommune());
						raf.setDateDebut(a.getDateDebut());
						raf.setDateFin(null);       // date de fin ignorée
						raf.setId(a.getId());
						raf.setLastMutationOperator(a.getLastMutationOperator());
						raf.setLastMutationTimestamp(a.getLastMutationTimestamp());
						raf.setObjectImpot(a.getObjectImpot());
						raf.setPourcentage(a.getPourcentage());
						raf.setTypeContribution(a.getTypeContribution());
						return raf;
					}
					return a;
				})
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
				.peek(a -> {
					if (isDateLouche(a.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
						              String.format("Allègement fiscal %d avec une date de début de validité antérieure au %s (%s).",
						                            a.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(DATE_LOUCHE),
						                            StringRenderers.DATE_RENDERER.toString(a.getDateDebut())));
					}
				})
				.map(a -> mapAllegementFiscal(a, mr))
				.flatMap(Function.identity())
				.map(a -> adapterAllegementFiscalPourFusionsCommunes(a, mr, LogCategory.SUIVI))
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
