package ch.vd.uniregctb.migration.pm.engine;

import java.math.BigDecimal;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.common.StringRenderer;
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
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.ConsolidationPhase;
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
import ch.vd.uniregctb.migration.pm.engine.helpers.DoublonProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.OrganisationServiceAccessor;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.log.DifferencesDonneesCivilesLoggedElement;
import ch.vd.uniregctb.migration.pm.log.ForFiscalIgnoreAbsenceAssujettissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.ForPrincipalOuvertApresFinAssujettissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.ContactEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.InscriptionRC;
import ch.vd.uniregctb.migration.pm.regpm.RadiationRC;
import ch.vd.uniregctb.migration.pm.regpm.RaisonSociale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdresseEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAllegementFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCategoriePersonneMorale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeCollectivite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCritereSegmentation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDemandeDelaiSommation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFusion;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;
import ch.vd.uniregctb.migration.pm.regpm.RegpmPrononceFaillite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmSiegeEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeCritereSegmentation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.EntityWrapper;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.migration.pm.utils.OrganisationDataHelper;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CapitalEntreprise;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
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
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.type.TypeMandat;

public class EntrepriseMigrator extends AbstractEntityMigrator<RegpmEntreprise> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseMigrator.class);

	private final BouclementService bouclementService;
	private final AssujettissementService assujettissementService;
	private final PeriodeImpositionService periodeImpositionService;
	private final ParametreAppService parametreAppService;
	private final OrganisationServiceAccessor organisationService;
	private final DoublonProvider doublonProvider;

	public EntrepriseMigrator(UniregStore uniregStore,
	                          ActivityManager activityManager,
	                          ServiceInfrastructureService infraService,
	                          BouclementService bouclementService,
	                          AssujettissementService assujettissementService,
	                          OrganisationServiceAccessor organisationService,
	                          AdresseHelper adresseHelper,
	                          FusionCommunesProvider fusionCommunesProvider,
	                          FractionsCommuneProvider fractionsCommuneProvider,
	                          DatesParticulieres datesParticulieres,
	                          PeriodeImpositionService periodeImpositionService,
	                          ParametreAppService parametreAppService,
	                          DoublonProvider doublonProvider) {
		super(uniregStore, activityManager, infraService, fusionCommunesProvider, fractionsCommuneProvider, datesParticulieres, adresseHelper);
		this.bouclementService = bouclementService;
		this.assujettissementService = assujettissementService;
		this.organisationService = organisationService;
		this.periodeImpositionService = periodeImpositionService;
		this.parametreAppService = parametreAppService;
		this.doublonProvider = doublonProvider;
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

	private static final class RecalculMotifsForsIndeterminesData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		private final RegpmEntreprise regpm;
		public RecalculMotifsForsIndeterminesData(RegpmEntreprise regpm, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.entrepriseSupplier = entrepriseSupplier;
			this.regpm = regpm;
		}
	}

	private static final class ComparaisonAssujettissementsData {
		private final RegpmEntreprise regpm;
		private final boolean active;
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public ComparaisonAssujettissementsData(RegpmEntreprise regpm, boolean active, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.regpm = regpm;
			this.active = active;
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static final class AnnulationDonneesFiscalesPourInactifsData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public AnnulationDonneesFiscalesPourInactifsData(KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	private static final class DeclarationsSansExerciceCommercialRegpmData {
		private final RegpmEntreprise regpm;
		private final List<RegpmDossierFiscal> dossiersFiscauxNonAssignes;
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public DeclarationsSansExerciceCommercialRegpmData(RegpmEntreprise regpm, List<RegpmDossierFiscal> dossiersFiscauxNonAssignes, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.regpm = regpm;
			this.dossiersFiscauxNonAssignes = dossiersFiscauxNonAssignes;
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

	private static final class AssujettissementData {
		@NotNull
		private final List<DateRange> ranges;

		public AssujettissementData(@Nullable Collection<RegpmAssujettissement> liste) {
			if (liste == null || liste.isEmpty()) {
				this.ranges = Collections.emptyList();
			}
			else {
				this.ranges = neverNull(DateRangeHelper.merge(liste.stream()
						                                              .filter(a -> a.getType() == RegpmTypeAssujettissement.LILIC)
						                                              .map(a -> new DateRangeHelper.Range(a.getDateDebut(), a.getDateFin()))
						                                              .sorted(DateRangeComparator::compareRanges)
						                                              .collect(Collectors.toList())));
			}
		}

		/**
		 * @return <code>true</code> si la collection des assujettissements ICC est non-vide
		 */
		public boolean hasSome() {
			return !ranges.isEmpty();
		}

		/**
		 * @return la date de fin de l'assujettissement (<code>null</code> si pas d'assujettissement du tout ou si assujettissement encore ouvert)
		 * @see #hasSome() pour faire la distinction entre "pas d'assujettissement" et "assujettissement ouvert" en cas de <code>null</code>
		 */
		@Nullable
		public RegDate getDateFin() {
			final DateRange last = getLast();
			return last == null ? null : last.getDateFin();
		}

		/**
		 * @return la dernière période d'assujettissement, ou <code>null</code> s'il n'y en a pas
		 */
		@Nullable
		public DateRange getLast() {
			return hasSome() ? ranges.get(ranges.size() - 1) : null;
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

	private static final class RaisonSocialeHistoData {
		private final NavigableMap<RegDate, String> histo;
		public RaisonSocialeHistoData(NavigableMap<RegDate, String> histo) {
			this.histo = histo == null ? Collections.emptyNavigableMap() : histo;
		}
	}

	private static final class CapitalHistoData {
		private final NavigableMap<RegDate, BigDecimal> histo;
		public CapitalHistoData(NavigableMap<RegDate, BigDecimal> histo) {
			this.histo = histo == null ? Collections.emptyNavigableMap() : histo;
		}
	}

	private static final class FormeJuridiqueHistoData {
		private final NavigableMap<RegDate, RegpmTypeFormeJuridique> histo;
		public FormeJuridiqueHistoData(NavigableMap<RegDate, RegpmTypeFormeJuridique> histo) {
			this.histo = histo == null ? Collections.emptyNavigableMap() : histo;
		}
	}

	private static final class SiegesHistoData {
		private final NavigableMap<RegDate, RegpmSiegeEntreprise> histo;
		public SiegesHistoData(NavigableMap<RegDate, RegpmSiegeEntreprise> histo) {
			this.histo = histo == null ? Collections.emptyNavigableMap() : histo;
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
		                                        ConsolidationPhase.FORS_IMMEUBLES,
		                                        k -> k.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData.Immeuble(d1.entiteJuridiqueSupplier, DATE_RANGE_MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondairesImmeuble(d, mr, idMapper));

		// callback pour le contrôle des fors secondaires après création
		mr.registerPreTransactionCommitCallback(ControleForsSecondairesData.class,
		                                        ConsolidationPhase.CONTROLE_FORS_SECONDAIRES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleForsSecondaires(d, mr, idMapper));

		// callback pour le contrôle (et la correction) de la couverture des fors secondaires par des fors principaux
		mr.registerPreTransactionCommitCallback(CouvertureForsData.class,
		                                        ConsolidationPhase.COUVERTURE_FORS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleCouvertureFors(d, mr, idMapper));

		// callback pour la destruction des fors annulés créés
		mr.registerPreTransactionCommitCallback(EffacementForsAnnulesData.class,
		                                        ConsolidationPhase.EFFACEMENT_FORS_ANNULES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        this::effacementForsAnnules);

		// callback pour le recalcul des motifs d'ouverture/fermeture des fors principaux qui seraient encore indéterminés
		mr.registerPreTransactionCommitCallback(RecalculMotifsForsIndeterminesData.class,
		                                        ConsolidationPhase.RECALCUL_MOTIFS_INDETERMINES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        EntrepriseMigrator::recalculMotifsForsIndetermines);

		// callback pour la migration des déclarations d'impôt (= dossiers fiscaux) non-liées à un exercice commercial
		// (en gros, les déclarations encore à l'état "EMISE")
		mr.registerPreTransactionCommitCallback(DeclarationsSansExerciceCommercialRegpmData.class,
		                                        ConsolidationPhase.DECLARATIONS_SANS_EXERCICE_COMMERCIAL_REGPM,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> migrateDossiersFiscauxNonAttribues(d, mr, idMapper));

		// callback pour l'annulation des données fiscales (fors, dis...) des contribuables inactifs/annulés (doublons)
		mr.registerPreTransactionCommitCallback(AnnulationDonneesFiscalesPourInactifsData.class,
		                                        ConsolidationPhase.ANNULATION_DONNEES_CONTRIBUABLES_INACTIFS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> annulationDonneesFiscalesPourInactifs(d, mr, idMapper));

		// callback pour le contrôle des données d'assujettissement
		mr.registerPreTransactionCommitCallback(ComparaisonAssujettissementsData.class,
		                                        ConsolidationPhase.COMPARAISON_ASSUJETTISSEMENTS,
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

		// les sièges pris en compte
		mr.registerDataExtractor(SiegesHistoData.class,
		                         e -> extractSieges(e, mr, idMapper),
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
		
		// données de l'historique des raisons sociales de l'entreprise
		mr.registerDataExtractor(RaisonSocialeHistoData.class,
		                         e -> extractRaisonsSociales(e, mr, idMapper),
		                         null,
		                         null);

		// données de l'historique des capitaux de l'entreprise
		mr.registerDataExtractor(CapitalHistoData.class,
		                         e -> extractCapitaux(e, mr, idMapper),
		                         null,
		                         null);

		// données de l'historique des formes juridiques de l'entreprise
		mr.registerDataExtractor(FormeJuridiqueHistoData.class,
		                         e -> extractFormesJuridiques(e, mr, idMapper),
		                         null,
		                         null);

		// données des mandats
		mr.registerDataExtractor(DonneesMandats.class,
		                         e -> extractDonneesMandats(e, mr, idMapper),
		                         null,
		                         null);

		// périodes d'assujettissement ICC dans RegPM
		mr.registerDataExtractor(AssujettissementData.class,
		                         this::extractDonneesAssujettissement,
		                         null,
		                         null);
	}

	@NotNull
	private AssujettissementData extractDonneesAssujettissement(RegpmEntreprise entreprise) {
		return new AssujettissementData(entreprise.getAssujettissements());
	}

	@NotNull
	private DonneesMandats extractDonneesMandats(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		return extractDonneesMandats(buildEntrepriseKey(e), e.getMandants(), e.getMandataires(), mr, LogCategory.SUIVI, idMapper);
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

			// [SIFISC-16744] nouvelle règle pour l'ordre des dates à prendre en compte
			// - date de réquisition de radiation
			// - date de bilan de fusion
			// - date de prononcé de faillite
			// - date de dissolution

			final RegDate dateBilanFusion = e.getFusionsApres().stream()
					.filter(fusion -> !fusion.isRectifiee())
					.map(EntrepriseMigrator::extractDateFermetureForAvantFusion)
					.sorted(Comparator.reverseOrder())
					.findFirst()
					.orElse(null);

			final RegDate datePrononceFaillite = e.getEtatsEntreprise().stream()
					.filter(etat -> !etat.isRectifie())
					.map(RegpmEtatEntreprise::getPrononcesFaillite)
					.flatMap(Set::stream)
					.map(RegpmPrononceFaillite::getDatePrononceFaillite)
					.sorted(Comparator.reverseOrder())
					.findFirst()
					.orElse(null);

			// récupération de toutes les fins d'activité canditates (il faut logguer si on en a plusieurs)
			final List<Pair<RegDate, String>> finsActivite = Stream.of(Pair.of(e.getDateRequisitionRadiation(), "réquisition de radiation"),
			                                                           Pair.of(dateBilanFusion, "bilan de fusion"),
			                                                           Pair.of(datePrononceFaillite, "prononcé de faillite"),
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
					.collect(Collectors.toList());

			// aurions-nous plusieurs candidats ?
			if (finsActivite.size() > 1) {
				// on va logguer ça...
				final String msg = finsActivite.stream()
						.map(pair -> String.format("date de %s (%s)", pair.getRight(), StringRenderers.DATE_RENDERER.toString(pair.getLeft())))
						.collect(Collectors.joining(", ", "Plusieurs dates de fin d'activité en concurrence : ", "."));
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, msg);
			}

			// on ne prend de toute façon que la première candidate sérieuse
			final Pair<RegDate, String> finActivite = finsActivite.isEmpty() ? null : finsActivite.get(0);

			// on a quelque chose ?
			if (finActivite != null) {

				// log informatif sur la date choisie
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				              String.format("Date de fin d'activité proposée (date de %s) : %s.",
				                            finActivite.getRight(),
				                            StringRenderers.DATE_RENDERER.toString(finActivite.getLeft())));

				// log en cas de valeur un peu louche
				checkDateLouche(finActivite.getLeft(),
				                () -> String.format("La date de fin d'activité (date de %s)", finActivite.getRight()),
				                LogCategory.SUIVI,
				                mr);

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
	 * @param src une map source (n'est pas modifiée par le traitement)
	 * @param equalator un prédicat qui détermine si deux valeurs sont égales
	 * @param <T> le type des valeurs
	 * @return une nouvelle map qui ne contient pas les associations qui n'apportent rien (= dont la valeur est la même, au sens de l'équalator, que la valeur associée à la clé précédente)
	 */
	private static <T> NavigableMap<RegDate, T> purgeNoOpTransitions(NavigableMap<RegDate, T> src, BiPredicate<T, T> equalator) {
		if (src == null) {
			return null;
		}
		if (src.size() < 2) {
			return src;
		}
		final NavigableMap<RegDate, T> dest = new TreeMap<>(src);
		final Iterator<Map.Entry<RegDate, T>> cursor = dest.entrySet().iterator();
		while (cursor.hasNext()) {
			final Map.Entry<RegDate, T> currentEntry = cursor.next();
			final Map.Entry<RegDate, T> previousEntry = dest.lowerEntry(currentEntry.getKey());
			if (previousEntry != null && equalator.test(previousEntry.getValue(), currentEntry.getValue())) {
				cursor.remove();
			}
		}
		return dest;
	}

	/**
	 * Retrouve les données valides de la raison sociale d'une entreprise
	 * @param entreprise entreprise dont on veut extraire la raison sociale courante
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un structure (qui peut être vide) contenant les données de la raison sociale courante de l'entreprise
	 */
	@NotNull
	private RaisonSocialeHistoData extractRaisonsSociales(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			// récupération de toutes les données qui ont un sens
			final NavigableMap<RegDate, List<String>> strict = entreprise.getRaisonsSociales().stream()
					.filter(rs -> !rs.getRectifiee())
					.filter(rs -> {
						if (rs.getDateValidite() == null) {
							mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							              String.format("Raison sociale %d (%s) ignorée car sa date de début de validité est nulle (ou antérieure au 01.08.1291).",
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
						final String texteRaisonSociale = extractRaisonSociale(rs);

						// date louche quand-même ?
						checkDateLouche(rs.getDateValidite(),
						                () -> String.format("Raison sociale %d (%s) avec une date de validité",
						                                    rs.getId(),
						                                    texteRaisonSociale),
						                LogCategory.DONNEES_CIVILES_REGPM,
						                mr);
					})
					.map(rs -> Pair.of(rs.getDateValidite(), extractRaisonSociale(rs)))
					.collect(Collectors.toMap(Pair::getLeft,
					                          pair -> Collections.singletonList(pair.getRight()),
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
					                          TreeMap::new));

			// pour chaque date, on ne récupère que la dernière (elles sont ordonnées, à date égale, par numéro de séquence...)
			final NavigableMap<RegDate, String> reduced = strict.entrySet().stream()
					.map(entry -> {
						final List<String> values = entry.getValue();
						if (values.size() > 1) {
							values.subList(0, values.size() - 1).forEach(s -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							                                                                String.format("Raison sociale '%s' du %s ignorée car remplacée par une autre valeur à la même date.",
							                                                                              s, StringRenderers.DATE_RENDERER.toString(entry.getKey()))));
						}
						return Pair.of(entry.getKey(), values.get(values.size() - 1));
					})
					.collect(Collectors.toMap(Pair::getLeft,
					                          Pair::getRight,
					                          (s1, s2) -> { throw new IllegalArgumentException("Erreur dans l'algorithme, il ne devrait pas y avoir de conflit de date ici..."); },
					                          TreeMap::new));

			// si on a quelque chose, on s'arrête là
			if (!reduced.isEmpty()) {
				return new RaisonSocialeHistoData(purgeNoOpTransitions(reduced, Objects::equals));
			}

			// si on n'a pas trouvé de raison sociale mais qu'il y avait un ou des cas avec date de début nulle, on va quand-même essayer d'en prendre une
			final String derniereAvecDateNulle = entreprise.getRaisonsSociales().stream()
					.filter(rs -> !rs.getRectifiee())
					.filter(rs -> rs.getDateValidite() == null)
					.max(Comparator.comparingLong(RaisonSociale::getId))
					.map(EntrepriseMigrator::extractRaisonSociale)
					.orElse(null);
			if (derniereAvecDateNulle != null) {
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.WARN,
				              String.format("En l'absence de donnée valide pour la raison sociale, repêchage de '%s'.", derniereAvecDateNulle));
				final NavigableMap<RegDate, String> map = new TreeMap<>(NullDateBehavior.EARLIEST::compare);
				map.put(null, derniereAvecDateNulle);
				return new RaisonSocialeHistoData(map);
			}

			// c'est la fin, on ne sait plus trop quoi faire...
			return new RaisonSocialeHistoData(null);
		});
	}

	/**
	 * Retrouve les dernières données valides du capital d'une entreprise
	 * @param entreprise entreprise dont on veut extraire le capital courant
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un structure (qui peut être vide) contenant les données du capital courant de l'entreprise
	 */
	@NotNull
	private CapitalHistoData extractCapitaux(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			// d'abord les données qui ont un sens
			final NavigableMap<RegDate, List<BigDecimal>> strict = entreprise.getCapitaux().stream()
					.filter(c -> !c.isRectifiee())
					.filter(c -> {
						if (c.getDateEvolutionCapital() == null) {
							mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							              String.format("Capital %d (%s) ignoré car sa date de début de validité est nulle (ou antérieure au 01.08.1291).",
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
					.peek(c -> checkDateLouche(c.getDateEvolutionCapital(),
					                           () -> String.format("Capital %d (%s) avec une date de début de validité",
					                                               c.getId().getSeqNo(),
					                                               c.getCapitalLibere()),
					                           LogCategory.DONNEES_CIVILES_REGPM,
					                           mr))
					.map(c -> Pair.of(c.getDateEvolutionCapital(), c.getCapitalLibere()))
					.collect(Collectors.toMap(Pair::getLeft,
					                          pair -> Collections.singletonList(pair.getRight()),
					                          (c1, c2) -> Stream.concat(c1.stream(), c2.stream()).collect(Collectors.toList()),
					                          TreeMap::new));

			// pour chaque date, on ne prend que le dernier capital (ils sont triés par numéro de séquence)
			final NavigableMap<RegDate, BigDecimal> reduced = strict.entrySet().stream()
					.map(entry -> {
						final List<BigDecimal> values = entry.getValue();
						if (values.size() > 1) {
							values.subList(0, values.size() - 1).forEach(capital -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							                                                                      String.format("Capital %s du %s ignoré car remplacé par une autre valeur à la même date.",
							                                                                                    capital,
							                                                                                    StringRenderers.DATE_RENDERER.toString(entry.getKey()))));
						}
						return Pair.of(entry.getKey(), values.get(values.size() - 1));
					})
					.collect(Collectors.toMap(Pair::getLeft,
					                          Pair::getRight,
					                          (c1, c2) -> { throw new IllegalArgumentException("Erreur dans l'algorithme, il ne devrait pas y avoir de conflit de date ici."); },
					                          TreeMap::new));

			// encapsulation dans la structure officielle
			return new CapitalHistoData(purgeNoOpTransitions(reduced, Objects::equals));
		});
	}

	/**
	 * Retrouve un historique complet des formes juridiques d'une entreprise au cours du temps
	 * @param entreprise entreprise dont on veut extraire les formes juridiques historiques
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return une structure (qui peut être vide) contenant les données des formes juridiques historiques de l'entreprise
	 */
	@NotNull
	private FormeJuridiqueHistoData extractFormesJuridiques(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			// on calcule d'abord un historique complet strict
			final Map<RegDate, List<RegpmTypeFormeJuridique>> strict = entreprise.getFormesJuridiques().stream()
					.filter(fj -> !fj.isRectifiee())
					.filter(fj -> {
						if (fj.getDateValidite() == null) {
							mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							              String.format("Forme juridique %d (%s) ignorée car sa date de début de validité est nulle (ou antérieure au 01.08.1291).",
							                            fj.getPk().getSeqNo(),
							                            fj.getType().getCode()));
							return false;
						}
						return true;
					})
					.filter(fj -> {
						if (isFutureDate(fj.getDateValidite())) {
							mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							              String.format("Forme juridique %d (%s) ignorée car sa date de début de validité est dans le futur (%s).",
							                            fj.getPk().getSeqNo(),
							                            fj.getType().getCode(),
							                            StringRenderers.DATE_RENDERER.toString(fj.getDateValidite())));
							return false;
						}
						return true;
					})
					.peek(fj -> checkDateLouche(fj.getDateValidite(),
					                            () -> String.format("Forme juridique %d (%s) avec date de début de validité",
					                                                fj.getPk().getSeqNo(),
					                                                fj.getType().getCode()),
					                            LogCategory.DONNEES_CIVILES_REGPM,
					                            mr))
					.collect(Collectors.toMap(RegpmFormeJuridique::getDateValidite,
					                          fj -> Collections.<RegpmTypeFormeJuridique>singletonList(fj.getType()),
					                          (set1, set2) -> Stream.concat(set1.stream(), set2.stream()).collect(Collectors.toList())));

			// si plusieurs données existent à la même date, on prend le dernier en loggant les autres
			final NavigableMap<RegDate, RegpmTypeFormeJuridique> reduced = strict.entrySet().stream()
					.map(entry -> {
						final List<RegpmTypeFormeJuridique> values = entry.getValue();
						if (values.size() > 1) {
							values.subList(0, values.size() - 1).forEach(type -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							                                                                   String.format("Forme juridique '%s' du %s ignorée car remplacée par une autre à la même date.",
							                                                                                 type.getCode(),
							                                                                                 StringRenderers.DATE_RENDERER.toString(entry.getKey()))));
						}
						return Pair.of(entry.getKey(), values.get(values.size() - 1));
					})
					.collect(Collectors.toMap(Pair::getKey,
					                          Pair::getValue,
					                          (t1, t2) -> { throw new IllegalArgumentException("Erreur dans l'algorithme, on ne devrait pas pouvoir de collision de clé ici..."); },
					                          TreeMap::new));

			// si on a des informations, pas de probléme, on renvoie ça
			if (!reduced.isEmpty()) {
				return new FormeJuridiqueHistoData(purgeNoOpTransitions(reduced, (o1, o2) -> Objects.equals(o1.getCode(), o2.getCode())));
			}

			// si on n'a pas trouvé de raison sociale mais qu'il y avait un ou des cas avec date de début nulle, on va quand-même essayer d'en prendre une
			final RegpmTypeFormeJuridique derniereAvecDateNulle = entreprise.getFormesJuridiques().stream()
					.filter(fj -> !fj.isRectifiee())
					.filter(fj -> fj.getDateValidite() == null)
					.max(Comparator.comparingInt(fj -> fj.getPk().getSeqNo()))
					.map(RegpmFormeJuridique::getType)
					.orElse(null);
			if (derniereAvecDateNulle != null) {
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.WARN,
				              String.format("En l'absence de donnée valide pour la forme juridique, repêchage de '%s'.", derniereAvecDateNulle.getCode()));
				final NavigableMap<RegDate, RegpmTypeFormeJuridique> map = new TreeMap<>(NullDateBehavior.EARLIEST::compare);
				map.put(null, derniereAvecDateNulle);
				return new FormeJuridiqueHistoData(map);
			}

			// là, c'est la fin, on n'a vraiment rien à proposer...
			return new FormeJuridiqueHistoData(null);
		});
	}

	/**
	 * Extraction officielle de l'historique des sièges de l'entreprise, tels que connus dans RegPM
	 * @param entreprise entreprise de RegPM
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return une structure (qui peut être vide) contenant les données historiques retenues pour les sièges de l'entreprise
	 */
	private SiegesHistoData extractSieges(RegpmEntreprise entreprise, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(entreprise);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			final RegDate datefinActivite = mr.getExtractedData(DateFinActiviteData.class, entrepriseKey).date;

			// on retrie les sièges par date de validité (le tri naturel est fait par numéro de séquence) en ignorant au passage les sièges annulés ou dont la date de début est dans le futur
			final NavigableMap<RegDate, List<RegpmSiegeEntreprise>> strict = entreprise.getSieges().stream()
					.filter(s -> !s.isRectifiee())
					.filter(s -> {
						if (s.getDateValidite() == null) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
							              String.format("Le siège %d est ignoré car il a une date de début de validité nulle (ou antérieure au 01.08.1291).", s.getId().getSeqNo()));
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
					.filter(s -> {
						if (datefinActivite != null && RegDateHelper.isAfter(s.getDateValidite(), datefinActivite, NullDateBehavior.LATEST)) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
							              String.format("Le siège %d est ignoré car sa date de début de validité (%s) est postérieure à la date de fin d'activité de l'entreprise (%s).",
							                            s.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(s.getDateValidite()),
							                            StringRenderers.DATE_RENDERER.toString(datefinActivite)));
							return false;
						}
						return true;
					})
					.peek(s -> checkDateLouche(s.getDateValidite(),
					                           () -> String.format("Le siège %d a une date de validité", s.getId().getSeqNo()),
					                           LogCategory.SUIVI,
					                           mr))
					.collect(Collectors.toMap(RegpmSiegeEntreprise::getDateValidite,
					                          Collections::singletonList,
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
					                          TreeMap::new));

			// maintenant, on ne garde, à chaque date, que le dernier siège (trié par numéro de séquence...)
			final NavigableMap<RegDate, RegpmSiegeEntreprise> reduced = strict.entrySet().stream()
					.map(entry -> {
						final List<RegpmSiegeEntreprise> values = entry.getValue();
						if (values.size() > 1) {
							values.subList(0, values.size() - 1).stream()
									.forEach(siege -> mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, String.format("Le siège %d est ignoré car il est suivi d'un autre à la même date.", siege.getId().getSeqNo())));
						}
						return Pair.of(entry.getKey(), values.get(values.size() - 1));
					})
					.collect(Collectors.toMap(Pair::getKey,
					                          Pair::getValue,
					                          (t1, t2) -> {
						                          throw new IllegalArgumentException("Erreur dans l'algorithme, on ne devrait pas pouvoir de collision de clé ici...");
					                          },
					                          TreeMap::new));

			return new SiegesHistoData(reduced);
		});
	}

	/**
	 * Passe en revue les fors fiscaux générés pour l'entreprise et efface ceux qui sont déjà annulés
	 * @param data la donnée de l'entreprise à traiter
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
	 * @param fusion indication de fusion
	 * @return date à prendre en compte pour l'ouverture du for principal de l'entreprise après fusion
	 */
	private static RegDate extractDateOuvertureForApresFusion(RegpmFusion fusion) {
		return fusion.getDateBilan().getOneDayAfter();
	}

	/**
	 * @param fusion indication de fusion
	 * @return date à prendre en compte pour la fermeture du for principal d'une entreprise qui disparaît dans une fusion
	 */
	private static RegDate extractDateFermetureForAvantFusion(RegpmFusion fusion) {
		return fusion.getDateBilan();
	}

	/**
	 * Repasse en revue les fors principaux générés pour l'entreprise et ré-évalue les motifs d'ouverture
	 * et de fermeture qui sont à la valeur {@link MotifFor#INDETERMINE}
	 * @param data la donnée de l'entreprise à traiter
	 */
	private static void recalculMotifsForsIndetermines(RecalculMotifsForsIndeterminesData data) {
		final Entreprise unireg = data.entrepriseSupplier.get();
		final List<ForFiscalPrincipalPM> ffps = unireg.getForsFiscauxPrincipauxActifsSorted();
		if (ffps != null && !ffps.isEmpty()) {

			// les dates assimilables à une inscription au RC / une constitution d'entreprise
			final Set<RegDate> datesInscription = Stream.concat(Stream.of(data.regpm.getDateInscriptionRC(), data.regpm.getDateConstitution()),
			                                                    data.regpm.getInscriptionsRC().stream()
					                                                    .filter(inscription -> !inscription.isRectifiee())
					                                                    .map(InscriptionRC::getDateInscription))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			// les dates assimilables à une radiation du RC / dissolution de l'entreprise / fusion (fermante)
			final Set<RegDate> dateRadiationFusionFermante = Stream.concat(Stream.of(data.regpm.getDateRadiationRC(), data.regpm.getDateRequisitionRadiation(), data.regpm.getDateDissolution()),
			                                                               Stream.concat(data.regpm.getRadiationsRC().stream()
					                                                                             .filter(radiation -> !radiation.isRectifiee())
					                                                                             .map(RadiationRC::getDateRadiation),
			                                                                             data.regpm.getFusionsApres().stream()
					                                                                             .filter(fusion -> !fusion.isRectifiee())
					                                                                             .map(EntrepriseMigrator::extractDateFermetureForAvantFusion)))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			// les dates assimilables à une fusion qui s'ouvre
			final Set<RegDate> datesFusionOuvrante = data.regpm.getFusionsAvant().stream()
					.filter(fusion -> !fusion.isRectifiee())
					.map(fusion -> Stream.of(extractDateFermetureForAvantFusion(fusion), extractDateOuvertureForApresFusion(fusion)))        // TODO régler la problématique de la date de bilan par rapport à la date d'ouverture du for sur l'entreprise après fusion
					.flatMap(Function.identity())
					.filter(Objects::nonNull)       // utile ?
					.collect(Collectors.toSet());

			boolean premierFor = true;
			for (ForFiscalPrincipalPM ffp : ffps) {
				// motif d'ouverture
				if (ffp.getMotifOuverture() == MotifFor.INDETERMINE) {

					// contexte : en général, c'est le premier for de l'entreprise...

					// si c'est le cas, et que ce for est HC/HS, alors on peut laisser le motif "vide"
					if (premierFor && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						ffp.setMotifOuverture(null);
					}
					else if (datesInscription.contains(ffp.getDateDebut())) {
						ffp.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
					}
					else if (datesFusionOuvrante.contains(ffp.getDateDebut())) {
						ffp.setMotifOuverture(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE);
					}
					else {
						// tant pis, on laisse INDETERMINE
					}
				}

				// motif de fermeture
				if (ffp.getMotifFermeture() == MotifFor.INDETERMINE) {

					// contexte : en général, c'est le dernier for de l'entreprise

					// radiation ou fusion fermante ?
					if (dateRadiationFusionFermante.contains(ffp.getDateFin())) {
						ffp.setMotifFermeture(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE);
					}
					else {
						// tant pis, on laisse INDETERMINE
					}
				}

				// le prochain ne sera de toute façon pas le premier for...
				premierFor = false;
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
				mr.addMessage(LogCategory.SUIVI, logLevel, "Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.");
				return null;
			}

			try {
				final Organisation org = organisationService.getOrganisation(idCantonal, mr);
				if (org != null) {
					return new DonneesCiviles(org);
				}

				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Aucune donnée renvoyée par RCEnt pour cette entreprise.");
			}
			catch (Exception ex) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
				              String.format("Erreur rencontrée lors de l'interrogation de RCEnt pour l'entreprise (%s).", ex.getMessage()));
				LOGGER.error("Exception lancée lors de l'interrogation de RCEnt pour l'entreprise dont l'ID cantonal est " + idCantonal, ex);
			}

			// rien trouvé -> on ignore les erreurs RCEnt
			return null;
		});
	}

	/**
	 * Extraction des fors principaux valides d'une entreprise de RegPM (en particulier, on blinde le
	 * cas de fors multiples à la même date et des fors sans date de début)
	 * @param regpm l'entreprise cible
	 * @param mr le collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 * @return un container des fors principaux valides de l'entreprise
	 */
	@NotNull
	private ForsPrincipauxData extractForsPrincipaux(RegpmEntreprise regpm, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(regpm);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			final AssujettissementData assujettissementData = mr.getExtractedData(AssujettissementData.class, entrepriseKey);
			final Map<RegDate, List<RegpmForPrincipal>> forsParDate = regpm.getForsPrincipaux().stream()
					.filter(ff -> {
						if (ff.getDateValidite() != null
								&& assujettissementData.hasSome()
								&& ff.getCommune() != null
								&& ff.getCommune().getCanton() == RegpmCanton.VD
								&& RegDateHelper.isAfter(ff.getDateValidite(), assujettissementData.getDateFin(), NullDateBehavior.LATEST)) {

							mr.addMessage(LogCategory.FORS, LogLevel.WARN,
							              String.format("For fiscal principal vaudois %d ignoré car sa date de début de validité (%s) est postérieure à la date de fin d'assujettissement ICC de l'entreprise (%s).",
							                            ff.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(ff.getDateValidite()),
							                            StringRenderers.DATE_RENDERER.toString(assujettissementData.getDateFin())));

							// [SIFISC-17110] On veut une liste...
							mr.pushContextValue(ForPrincipalOuvertApresFinAssujettissementLoggedElement.class, new ForPrincipalOuvertApresFinAssujettissementLoggedElement(regpm, ff, assujettissementData.getDateFin()));
							try {
								mr.addMessage(LogCategory.FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT, LogLevel.INFO, StringUtils.EMPTY);
							}
							finally {
								mr.popContexteValue(ForPrincipalOuvertApresFinAssujettissementLoggedElement.class);
							}

							return false;
						}
						return true;
					})
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
									                          LinkedHashMap::new));     // on veut conserver l'ordre des localisations pour les tests
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
							              String.format("Le for principal %d est ignoré car il a une date de début nulle (ou antérieure au 01.08.1291).", ff.getId().getSeqNo()));
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
					.collect(Collectors.toList());

			// [SIFISC-16860] on veut une adaptation de la date du premier for, si celui-ci est HS et qu'il débute au 01.01.1900
			if (!liste.isEmpty()) {
				final RegpmForPrincipal premierForPrincipal = liste.get(0);
				if (premierForPrincipal.getOfsPays() != null && premierForPrincipal.getDateValidite() == RegDate.get(1900, 1, 1)) {
					// s'il existe des fors secondaires, il faut plutôt prendre la date de début du premier for secondaire...
					final RegpmForSecondaire premierForSecondaire = regpm.getForsSecondaires().stream()
							.min(Comparator.comparing(RegpmForSecondaire::getDateDebut))
							.orElse(null);
					if (premierForSecondaire != null) {
						final RegDate dateDebutPremierForSecondaire = premierForSecondaire.getDateDebut();
						final RegDate nouvelleDateDebut;
						if (premierForPrincipal.getDateValidite().isBefore(dateDebutPremierForSecondaire)) {
							// attention, il peut y avoir un conflit avec le for principal suivant, s'il existe
							if (liste.size() > 1 && liste.get(1).getDateValidite().isBeforeOrEqual(dateDebutPremierForSecondaire)) {
								mr.addMessage(LogCategory.FORS, LogLevel.WARN,
								              String.format("La date de début de validité du for principal %d, bien qu'au %s, ne sera pas déplacée à la date de début du premier for secondaire (%s) en raison de la présence d'un autre for principal dès le %s.",
								                            premierForPrincipal.getId().getSeqNo(),
								                            StringRenderers.DATE_RENDERER.toString(premierForPrincipal.getDateValidite()),
								                            StringRenderers.DATE_RENDERER.toString(dateDebutPremierForSecondaire),
								                            StringRenderers.DATE_RENDERER.toString(liste.get(1).getDateValidite())));
								nouvelleDateDebut = premierForPrincipal.getDateValidite();
							}
							else {
								nouvelleDateDebut = dateDebutPremierForSecondaire;
							}
						}
						else {
							nouvelleDateDebut = dateDebutPremierForSecondaire;
						}

						if (nouvelleDateDebut != premierForPrincipal.getDateValidite()) {
							mr.addMessage(LogCategory.FORS, LogLevel.WARN,
							              String.format("La date de début de validité du for principal %d est passée du %s au %s pour suivre le premier for secondaire existant.",
							                            premierForPrincipal.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(premierForPrincipal.getDateValidite()),
							                            StringRenderers.DATE_RENDERER.toString(nouvelleDateDebut)));
							premierForPrincipal.setDateValidite(nouvelleDateDebut);
						}
					}
				}
			}

			// log des dates louches
			liste.forEach(ff -> checkDateLouche(ff.getDateValidite(),
			                                    () -> String.format("Le for principal %d a une date de début", ff.getId().getSeqNo()),
			                                    LogCategory.FORS,
			                                    mr));

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
				final List<DateRange> lilic = mr.getExtractedData(AssujettissementData.class, keyEntreprise).ranges;

				// assujettissements calculés par Unireg
				final List<DateRange> calcules = neverNull(DateRangeHelper.merge(assujettissementService.determine(entreprise)));

				// [SIFISC-16333] une entreprise qui n'était pas assujettie du tout et qui l'est maintenant, ou une entreprise qui était assujettie, et qui ne l'est plus du tout,
				// doit être logguées de manière particulière (ERROR)
				if (lilic.isEmpty() || calcules.isEmpty()) {
					if (!calcules.isEmpty()) {
						// apparition totale d'assujettissement

						// [SIFISC-17114] Dans ce cas, il ne faut pas migrer les fors vers Unireg, mais les lister
						// on commence par les lister
						entreprise.getForsFiscauxNonAnnules(true).stream()
								.sorted(Comparator.comparing(ForFiscal::isPrincipal).thenComparing(DateRangeComparator::compareRanges))       // les fors secondaires d'abord, puis les dates...
								.peek(ff -> mr.addMessage(LogCategory.FORS, LogLevel.WARN,
								                          String.format("Abandon de la migration du for fiscal %s en raison de l'absence totale d'assujettissement ICC dans RegPM pour cette entreprise.",
								                                        StringRenderers.LOCALISATION_DATEE_RENDERER.toString(ff))))
								.peek(ff -> {
									mr.pushContextValue(ForFiscalIgnoreAbsenceAssujettissementLoggedElement.class, new ForFiscalIgnoreAbsenceAssujettissementLoggedElement(data.regpm, ff));
									try {
										mr.addMessage(LogCategory.FORS_IGNORES_AUCUN_ASSUJETTISSEMENT, LogLevel.INFO, StringUtils.EMPTY);
									}
									finally {
										mr.popContexteValue(ForFiscalIgnoreAbsenceAssujettissementLoggedElement.class);
									}
								})
								.forEach(ff -> ff.setAnnule(true));

						// c'est fini, on a enlevé tout l'assujettissement calculé par Unireg...
						return;
					}
					if (!lilic.isEmpty()) {
						// disparition totale d'assujettissement
						mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.ERROR,
						              String.format("Disparition totale de l'assujettissement précédent : %s.",
						                            CollectionsUtils.toString(lilic, StringRenderers.DATE_RANGE_RENDERER, ",")));
					}
				}
				else {
					final List<DateRange> lilicIntersectant = new ArrayList<>(lilic.size());
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
				}

				// si la PM est déclarée "inactive" mais qu'Unireg lui calcule un assujettissement après la date seuil du 01.01.2015,
				// c'est un problème, non ?
				final RegDate seuilActivite = datesParticulieres.getSeuilActivite();
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
		if (organisationService.isRcentEnabled()) {
			final DonneesCiviles donneesCiviles = mr.getExtractedData(DonneesCiviles.class, moi.getKey());
			if (donneesCiviles != null) {
				rcent = donneesCiviles.getOrganisation();
				unireg.setNumeroEntreprise(rcent.getNumeroOrganisation());
			}
		}

		// les entreprises qui ont un numéro IDE dans RegPM mais pas de lien vers le civil doivent être listées
		if (rcent == null && regpm.getNumeroIDE() != null) {
			if (regpm.getNumeroCantonal() != null) {
				mr.addMessage(LogCategory.IDE_SANS_NO_CANTONAL, LogLevel.WARN, "Numéro cantonal présent mais sans écho dans RCEnt.");
			}
			else {
				mr.addMessage(LogCategory.IDE_SANS_NO_CANTONAL, LogLevel.WARN, StringUtils.EMPTY);
			}
		}

		// enregistrement de cette entreprise pour un contrôle final des fors secondaires (une fois que tous les immeubles et établissements ont été visés)
		mr.addPreTransactionCommitData(new ControleForsSecondairesData(regpm.getForsSecondaires(), moi));

		// enregistrement de cette entreprise pour un contrôle final de la couverture des fors secondaires par les fors principaux
		mr.addPreTransactionCommitData(new CouvertureForsData(moi));

		// enregistrement de cette entreprise pour la suppression finale des fors créés et déjà annulés
		mr.addPreTransactionCommitData(new EffacementForsAnnulesData(moi));

		// enregistrement de cette entreprise pour le recalcul final des motifs des fors principaux
		mr.addPreTransactionCommitData(new RecalculMotifsForsIndeterminesData(regpm, moi));

		// enregistrement de cette entreprise pour l'annulation éventuelle des données fiscales (fors, dis...) pour les inactifs
		mr.addPreTransactionCommitData(new AnnulationDonneesFiscalesPourInactifsData(moi));

		// enregistrement de cette entreprise pour la comparaison des assujettissements avant/après
		mr.addPreTransactionCommitData(new ComparaisonAssujettissementsData(regpm, activityManager.isActive(regpm), moi));

		// TODO migrer les documents (questionnaires SNC...)

		final String raisonSociale = Optional.ofNullable(mr.getExtractedData(RaisonSocialeHistoData.class, moi.getKey()).histo.lastEntry()).map(Map.Entry::getValue).orElse(null);
		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, raisonSociale, unireg, mr);
		migratePersonneContact(regpm.getContact1(), unireg, mr);
		migrateNotes(regpm.getNotes(), unireg);
		migrateFlagDoublon(regpm, unireg, mr);
		migrateDonneesRegistreCommerce(regpm, rcent, unireg, mr);
		logDroitPublicAPM(regpm, mr);

		migrateAdresses(regpm, unireg, mr);
		migrateAllegementsFiscaux(regpm, unireg, mr);
		migrateRegimesFiscaux(regpm, unireg, mr);
		migrateExercicesCommerciaux(regpm, unireg, mr);
		migrateDeclarationsImpot(regpm, unireg, mr, idMapper);
		generateForsPrincipaux(regpm, unireg, mr);
		migrateImmeubles(regpm, unireg, mr);
		generateEtablissementPrincipal(regpm, rcent, linkCollector, idMapper, mr);
		migrateEtatsEntreprise(regpm, unireg, mr);

		// doit être fait après les exercices commerciaux
		migrateLIASF(regpm, unireg, mr);

		migrateMandataires(regpm, mr, linkCollector, idMapper);
		migrateFusionsApres(regpm, linkCollector, idMapper);

		// log de suivi à la fin des opérations pour cette entreprise
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Entreprise migrée : %s.", FormatNumeroHelper.numeroCTBToDisplay(unireg.getNumero())));
	}

	/**
	 * Migration des flags LIASF de l'entreprise de RegPM
	 * @param regpm l'entreprise dans RegPM
	 * @param unireg l'entreprise cible dans Unireg
	 * @param mr le collecteur de messages de suivi
	 */
	private void migrateLIASF(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		final Set<Bouclement> bouclements = unireg.getBouclements();
		regpm.getCriteresSegmentation().stream()
				.filter(critere -> critere.getType() == RegpmTypeCritereSegmentation.LIASF)
				.sorted(Comparator.comparing(RegpmCritereSegmentation::getPfDebut))
				.map(critere -> {
					final FlagEntreprise flag = new FlagEntreprise();
					copyCreationMutation(critere, flag);

					final RegDate debut = Optional.ofNullable(bouclementService.getDateDernierBouclement(bouclements, RegDate.get(critere.getPfDebut(), 1, 1), true))
							.map(RegDate::getOneDayAfter)
							.orElse(RegDate.get(critere.getPfDebut(), 1, 1));
					final RegDate fin = Optional.<Integer>ofNullable(critere.getPfFin())
							.map(annee -> bouclementService.getDateDernierBouclement(bouclements, RegDate.get(annee, 12, 31), true))
							.map(date -> {
								if (date.year() < critere.getPfFin()) {
									return RegDate.get(critere.getPfFin(), 12, 31);
								}
								else {
									return date;
								}
							})
							.orElse(null);

					flag.setDateDebut(debut);
					flag.setDateFin(fin);
					flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
					if (critere.isAnnule()) {       // on reprend même les annulés !
						flag.setAnnulationDate(flag.getLogModifDate());
						flag.setAnnulationUser(flag.getLogModifUser());
					}
					return flag;
				})
				.filter(flag -> {
					if (flag.getDateFin() != null && flag.getDateFin().isBefore(flag.getDateDebut()) && !flag.isAnnule()) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Le flag d'entreprise %s non-annulé est ignoré par la migration car sa date de début (%s) est postérieure à sa date de fin (%s).",
						                            flag.getType(),
						                            StringRenderers.DATE_RENDERER.toString(flag.getDateDebut()),
						                            StringRenderers.DATE_RENDERER.toString(flag.getDateFin())));
						return false;
					}
					return true;
				})
				.peek(flag -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				                            String.format("Génération d'un flag entreprise %s%s sur la période %s.",
				                                          flag.getType(),
				                                          flag.isAnnule() ? " (annulé)" : StringUtils.EMPTY,
				                                          StringRenderers.DATE_RANGE_RENDERER.toString(flag))))
				.forEach(unireg::addFlag);
	}

	/**
	 * @param regpm l'entreprise de RegPM
	 * @param mr le collecteur de messages de suivi
	 */
	private static void logDroitPublicAPM(RegpmEntreprise regpm, MigrationResultProduction mr) {
		final NavigableMap<RegDate, RegpmTypeFormeJuridique> formesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, buildEntrepriseKey(regpm)).histo;
		final Map.Entry<RegDate, RegpmTypeFormeJuridique> last = formesJuridiques.lastEntry();
		if (last != null && toFormeJuridique(last.getValue().getCode()) == FormeJuridiqueEntreprise.CORP_DP_ADM) {
			mr.addMessage(LogCategory.DP_APM, LogLevel.INFO,
			              String.format("Forme juridique DP/APM depuis le %s.", StringRenderers.DATE_RENDERER.toString(last.getKey())));
		}
	}

	/**
	 * Migration des adresses de l'entreprise
	 * @param regpm entreprise de RegPM
	 * @param unireg entreprise dans Unireg
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 */
	private void migrateAdresses(RegpmEntreprise regpm, Entreprise unireg, MigrationResultContextManipulation mr) {

		final Map<RegpmTypeAdresseEntreprise, RegpmAdresseEntreprise> adresses = regpm.getAdressesTypees();

		// on prend l'adresse courrier et, à défaut, l'adresse siège
		Stream.of(RegpmTypeAdresseEntreprise.COURRIER, RegpmTypeAdresseEntreprise.SIEGE)
				.map(adresses::get)
				.filter(Objects::nonNull)
				.filter(a -> {
					if (a.getDateDebut() == null) {
						mr.addMessage(LogCategory.ADRESSES, LogLevel.ERROR,
						              String.format("Adresse %s ignorée car sa date de début de validité est nulle (ou antérieure au 01.08.1291).", a.getTypeAdresse()));
						return false;
					}
					return true;
				})
				.filter(a -> {
					if (isFutureDate(a.getDateDebut())) {
						mr.addMessage(LogCategory.ADRESSES, LogLevel.ERROR,
						              String.format("Adresse %s ignorée car sa date de début de validité est dans le futur (%s).",
						                            a.getTypeAdresse(),
						                            StringRenderers.DATE_RENDERER.toString(a.getDateDebut())));
						return false;
					}
					return true;
				})
				.peek(a -> checkDateLouche(a.getDateDebut(),
				                           () -> String.format("La date de début de validité de l'adresse %s", a.getTypeAdresse()),
				                           LogCategory.ADRESSES,
				                           mr))
				.findFirst()
				.ifPresent(a -> {
					final String complement = a.getChez() == null ? regpm.getEnseigne() : a.getChez();
					final AdresseTiers adresse = adresseHelper.buildAdresse(a, mr, complement, false);
					if (adresse != null) {
						adresse.setUsage(TypeAdresseTiers.COURRIER);
						unireg.addAdresseTiers(adresse);
					}
				});
	}

	/**
	 * Un itérateur de structures découpées pour conserver des plages de valeurs constantes
	 */
	private static class DonneesRegistreCommerceGenerator implements Iterator<DonneesRegistreCommerce> {

		private final RegDate dateFinActivite;
		private final NavigableMap<RegDate, String> histoRaisonsSociales;
		private final NavigableMap<RegDate, RegpmTypeFormeJuridique> histoFormesJuridiques;
		private final MigrationResultProduction mr;

		private RegDate nextDate;
		private boolean done;

		public DonneesRegistreCommerceGenerator(RegDate firstDate, RegDate dateFinActivite, NavigableMap<RegDate, String> histoRaisonsSociales,
		                                        NavigableMap<RegDate, RegpmTypeFormeJuridique> histoFormesJuridiques, MigrationResultProduction mr) {
			this.dateFinActivite = dateFinActivite;
			this.histoRaisonsSociales = histoRaisonsSociales;
			this.histoFormesJuridiques = histoFormesJuridiques;
			this.mr = mr;
			this.nextDate = firstDate;
			this.done = histoRaisonsSociales.floorEntry(firstDate) == null || histoFormesJuridiques.floorEntry(firstDate) == null;
		}

		@Override
		public boolean hasNext() {
			return !done;
		}

		@Override
		public DonneesRegistreCommerce next() {
			if (done) {
				throw new NoSuchElementException();
			}
			final String raisonSociale = histoRaisonsSociales.floorEntry(nextDate).getValue();
			final RegpmTypeFormeJuridique formeJuridique = histoFormesJuridiques.floorEntry(nextDate).getValue();
			final RegDate debut = nextDate;

			nextDate = computeNextDate();
			final RegDate fin;
			if (nextDate == null) {
				fin = dateFinActivite;
			}
			else {
				fin = nextDate.getOneDayBefore();
			}

			return new DonneesRegistreCommerce(debut, fin, raisonSociale, toFormeJuridique(formeJuridique.getCode()));
		}

		@Nullable
		private RegDate computeNextDate() {
			final RegDate nextRaisonSocialeChangeBrutto = histoRaisonsSociales.higherKey(nextDate);
			final RegDate nextFormeJuridiqueChangeBrutto = histoFormesJuridiques.higherKey(nextDate);

			final RegDate nextRaisonSocialeChange = dateFinActivite == null || RegDateHelper.isBeforeOrEqual(nextRaisonSocialeChangeBrutto, dateFinActivite, NullDateBehavior.LATEST)
					? nextRaisonSocialeChangeBrutto
					: null;
			final RegDate nextFormeJuridiqueChange = dateFinActivite == null || RegDateHelper.isBeforeOrEqual(nextFormeJuridiqueChangeBrutto, dateFinActivite, NullDateBehavior.LATEST)
					? nextFormeJuridiqueChangeBrutto
					: null;

			done = nextRaisonSocialeChange == null && nextFormeJuridiqueChange == null;
			return Stream.of(nextRaisonSocialeChange, nextFormeJuridiqueChange)
					.filter(Objects::nonNull)
					.min(Comparator.naturalOrder())
					.orElse(null);
		}
	}

	/**
	 * @param one une instance
	 * @param two une autre instance
	 * @param equalator un prédicat de comparaison entre deux instances
	 * @param <T> le type des instances comparées
	 * @return <code>true</code> si les deux instances sont différentes (une instance <code>null</code> est toujours différente d'une instance non-<code>null</code>)
	 */
	private static <T> boolean areDifferent(@Nullable T one, @Nullable T two, BiPredicate<T, T> equalator) {
		return one != two && (one == null || two == null || !equalator.test(one, two));
	}

	/**
	 * Reconstitution des données du registre du commerce
	 * @param regpm entreprise à migrer
	 * @param rcent organisation connue de RCent
	 * @param unireg entreprise destination de la migration dans Unireg
	 * @param mr collecteur de messages de migration
	 */
	private static void migrateDonneesRegistreCommerce(RegpmEntreprise regpm, @Nullable Organisation rcent, Entreprise unireg, MigrationResultContextManipulation mr) {

		final EntityKey key = buildEntrepriseKey(regpm);
		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, key).date;

		// historique des raisons sociales
		final NavigableMap<RegDate, String> regpmRaisonsSociales = mr.getExtractedData(RaisonSocialeHistoData.class, key).histo;

		// historique des capitaux
		final NavigableMap<RegDate, BigDecimal> regpmCapitaux = mr.getExtractedData(CapitalHistoData.class, key).histo;

		// historique des formes juridiques
		final NavigableMap<RegDate, RegpmTypeFormeJuridique> regpmFormesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, key).histo;

		// y a-t-il seulement un historique au niveau des formes juridiques et des raisons sociales ?
		if (regpmRaisonsSociales.isEmpty() || regpmFormesJuridiques.isEmpty()) {
			mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
			              "Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).");
			return;
		}

		// [SIFISC-17198] comparaison des données RCEnt vs. RegPM
		if (rcent != null) {
			final String rcentFormeJuridique = Optional.ofNullable(OrganisationDataHelper.getLastValue(rcent.getFormeLegale())).map(FormeLegale::getCode).orElse(null);
			final String rcentRaisonSociale = OrganisationDataHelper.getLastValue(rcent.getNom());
			final String rcentNumeroIde = OrganisationDataHelper.getLastValue(rcent.getNumeroIDE());

			final String regpmFormeJuridique = Optional.ofNullable(toFormeJuridique(regpmFormesJuridiques.lastEntry().getValue().getCode())).map(FormeJuridiqueEntreprise::getCodeECH).orElse(null);
			final String regpmRaisonSociale = regpmRaisonsSociales.lastEntry().getValue();
			final String regpmNumeroIde = Optional.ofNullable(regpm.getNumeroIDE()).map(ide -> String.format("%s%09d", ide.getCategorie(), ide.getNumero())).orElse(null);

			final boolean differenceFormeJuridique = areDifferent(rcentFormeJuridique, regpmFormeJuridique, Objects::equals);
			final boolean differenceRaisonSociale = areDifferent(rcentRaisonSociale, regpmRaisonSociale, Objects::equals);
			final boolean differenceNumeroIde = areDifferent(rcentNumeroIde, regpmNumeroIde, Objects::equals);

			if (differenceFormeJuridique || differenceNumeroIde || differenceRaisonSociale) {
				mr.pushContextValue(DifferencesDonneesCivilesLoggedElement.class, new DifferencesDonneesCivilesLoggedElement(regpm, rcent, differenceRaisonSociale, differenceFormeJuridique, differenceNumeroIde));
				try {
					mr.addMessage(LogCategory.DIFFERENCES_DONNEES_CIVILES, LogLevel.INFO, StringUtils.EMPTY);
				}
				finally {
					mr.popContexteValue(DifferencesDonneesCivilesLoggedElement.class);
				}
			}
		}

		// l'historique commun entre les raisons sociales et les formes juridiques (seuls éléments obligatoires) commence là
		final RegDate dateDebutHistoireFiscale = RegDateHelper.maximum(regpmRaisonsSociales.firstKey(), regpmFormesJuridiques.firstKey(), NullDateBehavior.EARLIEST);

		// si on n'a aucune date de début... on peut éventuellement se rabattre sur la date de début des capitaux ou sur la date de début d'activité de l'entreprise
		final RegDate dateDebutEffective;
		if (dateDebutHistoireFiscale == null) {
			final RegDate dateDebutActivite = getDateDebutActivite(regpm, mr);
			if (dateDebutActivite != null) {
				dateDebutEffective = dateDebutActivite;
			}
			else if (regpmCapitaux.isEmpty() || regpmCapitaux.firstKey() == null) {
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
				              "Impossible de déterminer la date de début des données du registre du commerce (aucune date de début connue pour la raison sociale, la forme juridique et d'éventuels capitaux).");
				return;
			}
			else {
				// on prend ça...
				dateDebutEffective = regpmCapitaux.firstKey();
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
				              String.format("Date de début effective des données 'civiles' reprise de la première donnée de capital : %s.",
				                            StringRenderers.DATE_RENDERER.toString(dateDebutEffective)));
			}
		}
		else {
			dateDebutEffective = dateDebutHistoireFiscale;
		}

		final RegDate dateFinHistoireFiscale;
		if (rcent == null) {
			final RegDate dateDerniereRaisonSociale = regpmRaisonsSociales.isEmpty() ? null : regpmRaisonsSociales.lastKey();
			final RegDate dateDerniereFormeJuridique = regpmFormesJuridiques.isEmpty() ? null : regpmFormesJuridiques.lastKey();
			if (dateFinActivite != null && (RegDateHelper.isAfter(dateDerniereFormeJuridique, dateFinActivite, NullDateBehavior.EARLIEST) || RegDateHelper.isAfter(dateDerniereRaisonSociale, dateFinActivite, NullDateBehavior.EARLIEST))) {
				dateFinHistoireFiscale = null;
				final RegDate dateQuiDepasse = RegDateHelper.maximum(dateDerniereFormeJuridique, dateDerniereRaisonSociale, NullDateBehavior.EARLIEST);
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
				              String.format("Date de début d'une donnée de raison sociale et/ou de forme juridique (%s) postérieure à la date de fin d'activité calculée (%s), cette dernière est donc ignorée ici.",
				                            StringRenderers.DATE_RENDERER.toString(dateQuiDepasse),
				                            StringRenderers.DATE_RENDERER.toString(dateFinActivite)));
			}
			else {
				dateFinHistoireFiscale = dateFinActivite;
			}
		}
		else {
			// on s'arrête à la veille de la première date de raison sociale (= obligatoire) connue de RCEnt
			final RegDate dateDebutHistoireCivile = OrganisationDataHelper.getFirstKnownDate(rcent.getNom());
			dateFinHistoireFiscale = Optional.ofNullable(dateDebutHistoireCivile)
					.map(RegDate::getOneDayBefore)
					.orElse(dateFinActivite);

			if (dateDebutHistoireCivile != null) {
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
				              String.format("Données de forme juridique et/ou de raison sociale en provenance du registre civil dès le %s (les données ultérieures de RegPM seront ignorées).",
				                            StringRenderers.DATE_RENDERER.toString(dateDebutHistoireCivile)));
			}
		}

		// si toutes les données fiscales connues sont en fait dans la période de validité civile -> on ne migre rien de fiscal
		if (RegDateHelper.isBeforeOrEqual(dateDebutEffective, dateFinHistoireFiscale, NullDateBehavior.LATEST)) {
			// génération des données à sauvegarder
			final Iterable<DonneesRegistreCommerce> donneesRC = () -> new DonneesRegistreCommerceGenerator(dateDebutEffective, dateFinHistoireFiscale, regpmRaisonsSociales, regpmFormesJuridiques, mr);
			StreamSupport.stream(donneesRC.spliterator(), false)
					.peek(rc -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
					                          String.format("Données 'civiles' migrées : sur la période %s, raison sociale (%s) et forme juridique (%s).",
					                                        StringRenderers.DATE_RANGE_RENDERER.toString(rc),
					                                        rc.getRaisonSociale(),
					                                        rc.getFormeJuridique())))
					.forEach(unireg::addDonneesRC);
		}

		final RegDate dateFinActiviteCapitaux;
		if (rcent == null) {
			if (dateFinActivite != null && !regpmCapitaux.isEmpty() && regpmCapitaux.lastKey().isAfter(dateFinActivite)) {
				dateFinActiviteCapitaux = null;
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
				              String.format("Date de début d'une donnée de capital (%s) postérieure à la date de fin d'activité calculée (%s), cette dernière est donc ignorée ici.",
				                            StringRenderers.DATE_RENDERER.toString(regpmCapitaux.lastKey()),
				                            StringRenderers.DATE_RENDERER.toString(dateFinActivite)));
			}
			else {
				dateFinActiviteCapitaux = dateFinActivite;
			}
		}
		else {
			// on s'arrête à la veille de la première date de capital connue de RCEnt
			final RegDate dateDebutHistoireCivileCapitaux = OrganisationDataHelper.getFirstKnownDate(rcent.getCapitaux());
			dateFinActiviteCapitaux = Optional.ofNullable(dateDebutHistoireCivileCapitaux)
					.map(RegDate::getOneDayBefore)
					.orElse(dateFinActivite);

			if (dateDebutHistoireCivileCapitaux != null) {
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
				              String.format("Données de capital en provenance du registre civil dès le %s (les données ultérieures de RegPM seront ignorées).",
				                            StringRenderers.DATE_RENDERER.toString(dateDebutHistoireCivileCapitaux)));
			}
		}

		// si toutes les données de capital connues fiscalement sont en fait dans la période de validité des données connues dans RCEnt, on ne migre rien
		if (!regpmCapitaux.isEmpty() && RegDateHelper.isBeforeOrEqual(regpmCapitaux.firstKey(), dateFinActiviteCapitaux, NullDateBehavior.LATEST)) {

			// génération des données de capital (d'abord sans dates de fin...)
			final List<CapitalEntreprise> capitaux = regpmCapitaux.entrySet().stream()
					.filter(entry -> entry.getValue() != null)
					.filter(entry -> RegDateHelper.isBeforeOrEqual(entry.getKey(), dateFinActiviteCapitaux, NullDateBehavior.LATEST))
					.map(entry -> new CapitalEntreprise(entry.getKey(), null, new MontantMonetaire(entry.getValue().longValue(), MontantMonetaire.CHF)))
					.collect(Collectors.toList());

			// assignation des dates de fin en fonction des dates de début du suivant
			assigneDatesFin(dateFinActiviteCapitaux, capitaux);

			// persistence et log
			capitaux.stream()
					.peek(capital -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
					                               String.format("Donnée de capital migrée : sur la période %s, %s.",
					                                             StringRenderers.DATE_RANGE_RENDERER.toString(capital),
					                                             StringRenderers.MONTANT_MONETAIRE_RENDERER.toString(capital.getMontant()))))
					.forEach(unireg::addCapital);
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
	 * Annule l'entreprise Unireg en fonction de critères internes à la données de RegPM qui l'identifient comme un doublon
	 * @param regpm entreprise à migrer
	 * @param unireg entreprise destination de la migration dans Unireg
	 * @param mr collecteur de messages de migration
	 */
	private void migrateFlagDoublon(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		if (doublonProvider.isDoublon(regpm)) {
			unireg.setAnnule(true);
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
	 * Génération des établissements principaux liés à l'entreprise (autant que d'entrées différentes dans la map)
	 * @param regpm l'entreprise de RegPM
	 * @param validiteSitesPrincipaux map des identifiants et des périodes de 'principalité' des sites principaux civils
	 * @param linkCollector le collecteur de liens à créer entre les entités
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param mr collecteur de messages de migration
	 */
	private void generateEtablissementPrincipalSelonDonneesCiviles(RegpmEntreprise regpm, Map<Long, List<DateRanged<SiteOrganisation>>> validiteSitesPrincipaux, EntityLinkCollector linkCollector,
	                                                               IdMapping idMapper, MigrationResultProduction mr) {
		final KeyedSupplier<Entreprise> entrepriseSupplier = getEntrepriseSupplier(idMapper, regpm);
		for (Map.Entry<Long, List<DateRanged<SiteOrganisation>>> entry : validiteSitesPrincipaux.entrySet()) {

			// si l'établissement avec cet identifiant cantonal a déjà été créé en base, on le ré-utilise
			// (ça marche parce que les différents threads de migration sont synchronisés sur les identifiants cantonaux aussi)
			final Etablissement etbPrincipal;
			final List<Etablissement> etbPrincipauxExistants = uniregStore.getEntitiesFromDb(Etablissement.class, Collections.singletonMap("numeroEtablissement", entry.getKey()));
			if (etbPrincipauxExistants == null || etbPrincipauxExistants.isEmpty()) {
				etbPrincipal = uniregStore.saveEntityToDb(new Etablissement());
				etbPrincipal.setNumeroEtablissement(entry.getKey());        // lien vers le civil

				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				              String.format("Etablissement principal %s créé en liaison avec le site civil %d.",
				                            FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
				                            entry.getKey()));
			}
			else if (etbPrincipauxExistants.size() > 1) {
				throw new IllegalStateException(String.format("Plus d'un (%d) établissement dans Unireg associé au numéro cantonal %d : %s.",
				                                              etbPrincipauxExistants.size(),
				                                              entry.getKey(),
				                                              etbPrincipauxExistants.stream()
						                                              .map(Etablissement::getNumeroEtablissement)
						                                              .map(FormatNumeroHelper::numeroCTBToDisplay)
						                                              .collect(Collectors.joining(", ", "{", "}"))));
			}
			else {
				etbPrincipal = etbPrincipauxExistants.get(0);

				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				              String.format("Etablissement principal %s ré-utilisé en liaison avec le site civil %d.",
				                            FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
				                            entry.getKey()));
			}

			final Supplier<Etablissement> etbPrincipalSupplier = getEtablissementByUniregIdSupplier(etbPrincipal.getNumero());

			// ici, on ne crée pas de domiciles pour les établissements... comme ils sont connus du civils, les sièges seront à aller chercher par là-bas aussi...

			// demande de création de liens d'activité économique
			entry.getValue().stream()
					.map(range -> new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(etbPrincipalSupplier, entrepriseSupplier, range.getDateDebut(), range.getDateFin(), true))
					.forEach(linkCollector::addLink);
		}
	}

	/**
	 * Génération de l'établissement principal à partir du dernier siège de l'entreprise
	 * @param regpm l'entreprise de RegPM
	 * @param rcent l'entreprise connue dans RCEnt
	 * @param linkCollector le collecteur de liens à créer entre les entités
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param mr collecteur de messages de migration
	 */
	private void generateEtablissementPrincipal(RegpmEntreprise regpm, @Nullable Organisation rcent, EntityLinkCollector linkCollector, IdMapping idMapper, MigrationResultProduction mr) {

		// la spécification ne parle pas de l'attribut commune ni des fors principaux pour la génération de l'établissement principal
		// mais seulement de la récupération des sièges depuis la table SIEGE_ENTREPRISE

		// voyons l'historique des sièges (dans RegPM)
		final EntityKey moi = buildEntrepriseKey(regpm);
		final NavigableMap<RegDate, RegpmSiegeEntreprise> sieges = mr.getExtractedData(SiegesHistoData.class, moi).histo;

		// si une connexion avec RCEnt existe, on va essayer de lier l'établissement principal généré avec son pendant civil

		// date de fin des données fiscales (= veille de la date d'apparition civile des données, ou <code>null</code> s'il n'y a pas de données civiles)
		final RegDate dateFinEtablissementFiscal;
		if (rcent != null) {
			// récupérons d'abord les données civiles des sites principaux (s'il y en a plusieurs...)
			final List<DateRanged<SiteOrganisation>> sites = rcent.getSitePrincipaux();
			if (sites != null && !sites.isEmpty()) {

				// veille du début des données civiles
				dateFinEtablissementFiscal = sites.stream()
						.map(DateRange::getDateDebut)
						.min(Comparator.naturalOrder())
						.map(RegDate::getOneDayBefore)
						.get();

				// y a-t-il plusieurs établissements principaux distincts, d'abord ?
				final Map<Long, List<DateRanged<SiteOrganisation>>> validitesSites = sites.stream()
						.collect(Collectors.toMap(siteRange -> siteRange.getPayload().getNumeroSite(),
						                          Collections::singletonList,
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
						                          LinkedHashMap::new));     // afin de conserver l'ordre (le site le plus vieux apparaîtra en premier dans l'itérateur)

				// génération des données d'établissement depuis les données civiles
				generateEtablissementPrincipalSelonDonneesCiviles(regpm, validitesSites, linkCollector, idMapper, mr);
			}
			else {
				dateFinEtablissementFiscal = null;
			}
		}
		else {
			dateFinEtablissementFiscal = null;
		}

		// pas de donnée de siège -> on a fini (s'il y a des données civiles, tant mieux, sinon... rien)
		if (sieges.isEmpty()) {
			if (dateFinEtablissementFiscal == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Pas de siège associé dans les données fiscales, pas d'établissement principal créé.");
			}
			return;
		}
		else if (dateFinEtablissementFiscal != null) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
			              String.format("Données civiles d'établissement principal présentes dès le %s, tous les sièges ultérieurs de RegPM seront ignorés.",
			                            StringRenderers.DATE_RENDERER.toString(dateFinEtablissementFiscal.getOneDayAfter())));
		}

		// a-t-on des établissements principaux à générer depuis les données fiscales (on n'en fait qu'un au plus, en faisant bouger les domiciles,
		// mais seulement tant qu'aucune donnée civile ne vient prendre le relai)
		if (dateFinEtablissementFiscal == null || sieges.firstKey().isBeforeOrEqual(dateFinEtablissementFiscal)) {

			// récupération de la raison sociale
			final NavigableMap<RegDate, String> raisonsSociales = mr.getExtractedData(RaisonSocialeHistoData.class, moi).histo;
			final String raisonSociale = Optional.ofNullable(raisonsSociales.lastEntry()).map(Map.Entry::getValue).orElse(null);

			// date de fin d'activité de l'entreprise
			final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, moi).date;
			final RegDate dateFinDonneesFiscales = RegDateHelper.minimum(dateFinEtablissementFiscal, dateFinActivite, NullDateBehavior.LATEST);

			final Etablissement etbPrincipal = uniregStore.saveEntityToDb(new Etablissement());
			final Supplier<Etablissement> etbPrincipalSupplier = getEtablissementByUniregIdSupplier(etbPrincipal.getNumero());
			etbPrincipal.setEnseigne(regpm.getEnseigne());
			etbPrincipal.setRaisonSociale(raisonSociale);

			// un peu de log pour indiquer la création de l'établissement principal
			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Création de l'établissement principal %s.",
			                                                              FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));

			// lien entre l'établissement principal et son entreprise
			final KeyedSupplier<Entreprise> entrepriseSupplier = getEntrepriseSupplier(idMapper, regpm);
			linkCollector.addLink(new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(etbPrincipalSupplier, entrepriseSupplier, sieges.firstKey(), dateFinDonneesFiscales, true));

			// création des domiciles (pour l'instant sans date de fin, celle-ci sera ajouté ensuite)
			final List<DomicileEtablissement> domicilesBruts = sieges.entrySet().stream()
					.filter(entry -> {
						if (RegDateHelper.isAfter(entry.getKey(), dateFinEtablissementFiscal, NullDateBehavior.LATEST)) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
							              String.format("Siège %d ignoré car il ne débute qu'après la date d'apparition des données civiles.", entry.getValue().getId().getSeqNo()));
							return false;
						}
						return true;
					})
					.map(entry -> Pair.of(entry.getKey(), buildCommuneOuPays(entry.getKey(),
					                                                         entry.getValue()::getCommune,
					                                                         entry.getValue()::getNoOfsPays,
					                                                         String.format("siège %d", entry.getValue().getId().getSeqNo()),
					                                                         mr,
					                                                         LogCategory.SUIVI)))
					.map(pair -> new DomicileEtablissement(pair.getLeft(), null, pair.getRight().getTypeAutoriteFiscale(), pair.getRight().getNumeroOfsAutoriteFiscale(), null))
					.collect(Collectors.toList());

			// ajout des dates de fin
			assigneDatesFin(dateFinDonneesFiscales, domicilesBruts);

			// finalisation
			domicilesBruts.stream()
					.peek(d -> checkFractionCommuneVaudoise(d, mr, LogCategory.SUIVI))
					.map(d -> adapterAutourFusionsCommunes(d, mr, LogCategory.SUIVI, null))
					.flatMap(List::stream)
					.peek(d -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Domicile de l'établissement principal %s : %s sur %s/%d.",
					                                                                         FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
					                                                                         StringRenderers.DATE_RANGE_RENDERER.toString(d),
					                                                                         d.getTypeAutoriteFiscale(),
					                                                                         d.getNumeroOfsAutoriteFiscale())))
					.forEach(etbPrincipal::addDomicile);

			// TODO adresse ?
		}
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
					linkCollector.addLink(new EntityLinkCollector.FusionEntreprisesLink(moi, apresFusion, extractDateOuvertureForApresFusion(apres), null));
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

		// on va chercher les mandats (rôle mandant -> les mandataires) reprise
		final Collection<RegpmMandat> mandataires = mr.getExtractedData(DonneesMandats.class, moi.getKey()).getRolesMandant();

		// migration des mandataires -> liens à créer par la suite
		mandataires.stream()
				.forEach(mandat -> {

					// récupération du mandataire qui peut être une autre entreprise, un établissement ou un individu
					final KeyedSupplier<? extends Contribuable> mandataire = getPolymorphicSupplier(idMapper, mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
					if (mandataire == null) {
						// cas normalement impossible, puisque le cas a dû être écarté déjà dans l'extracteur
						throw new IllegalArgumentException("On ne devrait pas se trouver là...");
					}

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

					// ajout du lien entre l'entreprise et son mandataire
					linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi, mandataire, mandat.getDateAttribution(), mandat.getDateResiliation(), extractTypeMandat(mandat.getType()), iban, bicSwift));
				});
	}

	private static TypeMandat extractTypeMandat(RegpmTypeMandat type) {
		if (type == RegpmTypeMandat.GENERAL) {
			return TypeMandat.GENERAL;
		}

		// TODO si on doit un jour migrer d'autres types, ça va sauter (doit être cohérent avec l'extraction des mandats)
		throw new IllegalArgumentException("Type de mandat non-supporté : " + type);
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
			final List<RegpmExerciceCommercial> exercicesCommerciaux = getExercicesCommerciauxNonAnnules(regpm);
			if (!exercicesCommerciaux.isEmpty()) {

				// recherche du dernier exercice commercial (non-annulé) connu
				final RegpmExerciceCommercial dernierExcerciceConnu = exercicesCommerciaux.get(exercicesCommerciaux.size() - 1);

				if (brutto != null && dernierExcerciceConnu != null && brutto.isBefore(dernierExcerciceConnu.getDateFin())) {
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

	/**
	 * Méthode centralisée pour extraire la liste triée des exercices commerciaux non-annulés d'une entreprise dans RegPM
	 * @param regpm l'entreprise en question
	 * @return la liste triée des exercices commerciaux non-annulés de l'entreprise
	 */
	@NotNull
	private static List<RegpmExerciceCommercial> getExercicesCommerciauxNonAnnules(RegpmEntreprise regpm) {
		return regpm.getExercicesCommerciaux().stream()
				.filter(ex -> ex.getDossierFiscal() == null || ex.getDossierFiscal().getEtat() != RegpmTypeEtatDossierFiscal.ANNULE)
				.collect(Collectors.toList());
	}

	private void migrateExercicesCommerciaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final RegDate dateBouclementFutur = mr.getExtractedData(DateBouclementFuturData.class, buildEntrepriseKey(regpm)).date;
		final List<RegDate> datesBouclements = new LinkedList<>();
		final List<RegpmExerciceCommercial> exercicesCommerciaux = getExercicesCommerciauxNonAnnules(regpm);

		// on s'intéresse à une période en particulier (= ce que l'on voit par la lorgnette, d'où le nom...)
		// qui va du début de l'existence connue de l'entreprise à son activité connue la plus récente (voire même future)

		final RegDate dateDebutLorgnette = Stream.of(getDateDebutActivite(regpm, mr),
		                                             exercicesCommerciaux.isEmpty() ? null : exercicesCommerciaux.get(0).getDateDebut())
				.filter(Objects::nonNull)
				.min(Comparator.naturalOrder())
				.orElse(null);

		final RegDate dateFinLorgnette = Stream.of(dateBouclementFutur,
		                                           exercicesCommerciaux.isEmpty() ? null : exercicesCommerciaux.get(exercicesCommerciaux.size() - 1).getDateFin())
				.filter(Objects::nonNull)
				.max(Comparator.naturalOrder())
				.orElse(null);

		if (dateDebutLorgnette == null) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Entreprise sans exercice commercial ni for principal.");
		}
		if (dateFinLorgnette == null) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Entreprise sans exercice commercial ni date de bouclement futur.");
		}
		if (dateDebutLorgnette != null && dateFinLorgnette != null) {

			// dans RegPM, les exercices commerciaux ne sont instanciés que quand une DI est retournée, ce qui a pour conséquence immédiate qu'en l'absence
			// d'assujettissement, aucune DI n'est bien-sûr envoyée, encore moins retournée, et donc aucun exercice commercial n'est créé en base...
			// donc, si on trouve des trous dans les exercices commerciaux en base, cela correspond à une interruption de l'assujettissement, et il faut le combler
			// (on va supposer des exercices commerciaux annuels, faute de mieux, dans la période non-mappée)

			// période totale maximale couverte (potentiellement partiellement, c'est justement ce qui nous intéresse ici...) par des exercices commerciaux de regpm
			final DateRange lifespan = new DateRangeHelper.Range(dateDebutLorgnette, dateFinLorgnette);
			final List<DateRange> mapped = exercicesCommerciaux.stream().map(ex -> new DateRangeHelper.Range(ex.getDateDebut(), ex.getDateFin())).collect(Collectors.toList());
			final List<DateRange> notMapped = DateRangeHelper.subtract(lifespan, mapped);
			if (!notMapped.isEmpty()) {

				// il y a bien au moins une (peut-être plusieurs...) période qui n'est pas mappée...
				// pour chacune d'entre elles, on va supposer des exercices commerciaux annuels (à la date d'ancrage du premier exercice après le trou) pour combler
				for (DateRange range : notMapped) {

					// on sait que la fin du range est forcément une date de bouclement (puisqu'un exercice commercial débute au lendemain)
					// et ensuite on case autant d'années que nécessaire pour boucher le trou
					for (RegDate bouclement = range.getDateFin(); bouclement.isAfterOrEqual(range.getDateDebut()); bouclement = bouclement.addYears(-1)) {

						// la date de bouclement futur est de toute façon ajoutée par la suite, et ne mérite pas le log d'ajout d'une date "estimée"...
						if (bouclement != dateBouclementFutur) {
							datesBouclements.add(bouclement);

							mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
							              String.format("Ajout d'une date de bouclement estimée au %s pour combler l'absence d'exercice commercial dans RegPM sur la période %s.",
							                            StringRenderers.DATE_RENDERER.toString(bouclement),
							                            StringRenderers.DATE_RANGE_RENDERER.toString(range)));
						}
					}
				}
			}
		}

		// on ajoute les dates de bouclement connues (car un exercice commercial existe effectivement)
		exercicesCommerciaux.stream()
				.map(RegpmExerciceCommercial::getDateFin)
				.forEach(datesBouclements::add);

		// quand la déclaration (= dossier fiscal) de la PF précédente a déjà été envoyée (mais pas encore retournée, donc aucun exercice commercial
		// n'a encore été généré, et la date de bouclement futur correspond déjà à la fin de la PF courante)
		// -> je dois bien créer un exercice commercial dans Unireg entre les deux (= pour la DI envoyée)

		// TODO que faire si la date de bouclement futur est nulle ?
		if (dateBouclementFutur != null) {
			datesBouclements.add(dateBouclementFutur);
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
	private void migrateDeclarationsImpot(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr, IdMapping idMapper) {

		final EntityKey moi = buildEntrepriseKey(regpm);
		final List<RegpmDossierFiscal> dossiers = mr.getExtractedData(DossiersFiscauxData.class, moi).liste;
		final Set<RegpmDossierFiscal> dossiersFiscauxAttribuesAuxExercicesCommerciaux = new HashSet<>(dossiers.size());

		// boucle sur chacun des exercices commerciaux
		regpm.getExercicesCommerciaux().forEach(exercice -> {

			final RegpmDossierFiscal dossier = exercice.getDossierFiscal();
			if (dossier != null && dossier.getModeImposition() == RegpmModeImposition.POST) {

				// on collecte les dossiers fiscaux attachés aux exercices commerciaux
				// pour trouver au final ceux qui ne le sont pas (= les déclarations envoyées mais pas encore traitées ???)
				dossiersFiscauxAttribuesAuxExercicesCommerciaux.add(dossier);

				// un cas très bizarre : un dossier sur une PF qui n'existe pas parce qu'elle n'a pas encore commencé
				if (dossier.getPf() > RegDate.get().year()) {
					mr.addMessage(LogCategory.DECLARATIONS, LogLevel.ERROR,
					              String.format("Dossier fiscal sur la PF %d qui est encore dans le futur (exercice commercial %d %s) : la DI est ignorée.",
					                            dossier.getPf(),
					                            exercice.getId().getSeqNo(),
					                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(exercice.getDateDebut(), exercice.getDateFin()))));
				}
				else {

					// un peu de log pour le suivi
					mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
					              String.format("Génération d'une déclaration sur la PF %d à partir des dates %s de l'exercice commercial %d et du dossier fiscal correspondant.",
					                            dossier.getPf(),
					                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(exercice.getDateDebut(), exercice.getDateFin())),
					                            exercice.getId().getSeqNo()));

					// un petit warning sur des cas bizarres...
					if (dossier.getPf() != exercice.getDateFin().year()) {
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Dossier fiscal sur la PF %d alors que la fin de l'exercice commercial (%s) est en %d... N'est-ce pas étrange ?",
						                            dossier.getPf(),
						                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(exercice.getDateDebut(), exercice.getDateFin())),
						                            exercice.getDateFin().year()));
					}

					final Declaration di = migrateDeclaration(dossier, exercice.getDateDebut(), exercice.getDateFin(), mr);
					unireg.addDeclaration(di);
				}
			}
		});

		// ensuite, il faut éventuellement trouver une déclaration envoyée mais pour laquelle je n'ai pas encore
		// d'entrée dans la table des exercices commerciaux

		// on va essayer de reconstituer ces entrées d'après les périodes d'imposition calculées
		// (sauf que celles-ci ne sont pas disponibles avant la génération de tous les fors qui viennent potentiellement d'immeubles, d'établissements...)
		// -> il faut donc demander un calcul pour la fin de la transaction, au moment où toutes les informations seront disponibles

		final List<RegpmDossierFiscal> dossiersFiscauxNonAttribues = dossiers.stream()
				.filter(dossier -> !dossiersFiscauxAttribuesAuxExercicesCommerciaux.contains(dossier))
				.sorted(Comparator.comparingInt(RegpmDossierFiscal::getPf).thenComparingInt(RegpmDossierFiscal::getNoParAnnee))
				.collect(Collectors.toList());
		if (!dossiersFiscauxNonAttribues.isEmpty()) {
			// demande de calcul pour la fin de la transaction avec toutes les données nécessaires
			mr.addPreTransactionCommitData(new DeclarationsSansExerciceCommercialRegpmData(regpm, dossiersFiscauxNonAttribues, getEntrepriseSupplier(idMapper, regpm)));
		}
	}

	/**
	 * Méthode de callback pour s'assurer que les contribuables inactifs/annulés (doublons) n'ont pas de données fiscales (fors, dis...) non-annulées
	 * @param data données d'identification de l'entreprise à contrôler
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapping des identifiants RegPM -> Unireg
	 */
	private void annulationDonneesFiscalesPourInactifs(AnnulationDonneesFiscalesPourInactifsData data,
	                                                   MigrationResultContextManipulation mr,
	                                                   IdMapping idMapper) {
		final EntityKey key = data.entrepriseSupplier.getKey();
		doInLogContext(key, mr, idMapper, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();
			if (entreprise.isDebiteurInactif() || entreprise.isAnnule()) {
				// les éventuelles déclarations non-annulées
				neverNull(entreprise.getDeclarationsSorted()).stream()
						.filter(decl -> !decl.isAnnule())
						.peek(decl -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
						                            String.format("Déclaration %s sur la période fiscale %d annulée car l'entreprise a été identifée comme un débiteur inactif.",
						                                          StringRenderers.DATE_RANGE_RENDERER.toString(decl),
						                                          decl.getPeriode().getAnnee())))
						.forEach(decl -> decl.setAnnule(true));

				// les éventuels fors fiscaux non-annulés
				entreprise.getForsFiscauxNonAnnules(true).stream()
						.peek(ff -> mr.addMessage(LogCategory.FORS, LogLevel.INFO,
						                          String.format("For fiscal %s annulé car l'entreprise a été identifiée comme un débiteur inactif.",
						                                        StringRenderers.LOCALISATION_DATEE_RENDERER.toString(ff))))
						.forEach(ff -> ff.setAnnule(true));
			}
		});
	}

	/**
	 * Méthode de callback pour finaliser la migration des déclarations d'impôt (= celle qui ne sont associées à aucun exercice commercial dans RegPM, a priori donc
	 * celles qui ont juste été émises)
	 * @param data données collectées dans la première passe de la migration des dossiers fiscaux
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapping des identifiants RegPM -> Unireg
	 */
	private void migrateDossiersFiscauxNonAttribues(DeclarationsSansExerciceCommercialRegpmData data,
	                                                MigrationResultContextManipulation mr,
	                                                IdMapping idMapper) {
		final EntityKey key = data.entrepriseSupplier.getKey();
		doInLogContext(key, mr, idMapper, () -> {

			// de toute façon, il faut calculer les périodes d'imposition de la PM...
			final Entreprise entreprise = data.entrepriseSupplier.get();
			try {
				final List<PeriodeImposition> pis = neverNull(periodeImpositionService.determine(entreprise));
				final int premierePeriodeFiscalePersonnesMorales = parametreAppService.getPremierePeriodeFiscalePersonnesMorales();

				// par période fiscale, cherchons les périodes d'imposition non-encore couvertes
				// 1. on commence par retrouver les déclarations par période fiscale
				final Map<Integer, List<DeclarationImpotOrdinairePM>> diExistantes = neverNull(entreprise.getDeclarations()).stream()
						.map(d -> (DeclarationImpotOrdinairePM) d)
						.collect(Collectors.toMap(di -> di.getPeriode().getAnnee(),
						                          Collections::singletonList,
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(DateRangeComparator::compareRanges).collect(Collectors.toList())));

				// et 2. on regarde les périodes d'imposition pour ne garder celles qui ne matchent pas
				final Map<Integer, List<PeriodeImposition>> periodesNonCouvertes = pis.stream()
						.filter(pi -> {
							final int pf = pi.getPeriodeFiscale();
							final List<DeclarationImpotOrdinairePM> declarations = diExistantes.get(pf);
							return !DateRangeHelper.isFullyCovered(pi, declarations);           // TODO est-ce vraiment !isFullyCovered() qu'il faut utiliser ?
						})
						.collect(Collectors.toMap(PeriodeImposition::getPeriodeFiscale,
						                          pi -> new LinkedList<>(Collections.singletonList(pi)),                                                        // LinkedList pour pouvoir enlever les éléments facilement
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toCollection(LinkedList::new)),        // LinkedList pour pouvoir enlever les éléments facilement
						                          TreeMap::new));

				// indexation des exercices commerciaux connus par pf
				final NavigableMap<Integer, RegpmExerciceCommercial> mapDernierExerciceCommercial = getExercicesCommerciauxNonAnnules(data.regpm).stream()
						.collect(Collectors.toMap(ex -> ex.getDateFin().year(),
						                          Function.identity(),
						                          (ex1, ex2) -> Stream.of(ex1, ex2).max(Comparator.comparing(RegpmExerciceCommercial::getDateFin)).get(),
						                          TreeMap::new));

				// pour chaque dossier fiscal non-assigné, on va essayer de générer une déclaration
				for (RegpmDossierFiscal dossier : data.dossiersFiscauxNonAssignes) {
					// dans quelle pf, déjà ?
					final Integer pf = dossier.getPf();
					final List<PeriodeImposition> periodesNonCouvertesPourPf = periodesNonCouvertes.get(pf);
					if (periodesNonCouvertesPourPf == null || periodesNonCouvertesPourPf.isEmpty()) {
						if (pf < premierePeriodeFiscalePersonnesMorales) {
							// il est normal qu'aucune période d'imposition ne soit trouvée, puisqu'elles ne sont pas calculées avant cette année-là
							mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
							              String.format("Dossier fiscal %d/%d sans exercice commercial lié ignoré car antérieur à la PF %d.",
							                            pf, dossier.getNoParAnnee(), premierePeriodeFiscalePersonnesMorales));
						}
						else if (dossier.getEtat() == RegpmTypeEtatDossierFiscal.ANNULE) {
							// [SIFISC-16462] dans le cas d'une DI annulé, on essaie de reconstituer des dates (qui n'ont au final que peu d'importance)
							// à partir de la fin du dernier exercice commercial précédent
							final RegpmExerciceCommercial exercicePrecedent = Optional.ofNullable(mapDernierExerciceCommercial.lowerEntry(pf))
									.map(Map.Entry::getValue)
									.orElse(null);
							final RegDate dateDebut;
							final RegDate dateFin;
							if (exercicePrecedent != null) {
								// réalignement sur la PF désirée
								dateFin = exercicePrecedent.getDateFin().addYears(pf - exercicePrecedent.getDateFin().year());
								dateDebut = dateFin.addYears(-1).addDays(1);

								// un peu de log pour le suivi
								mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
								              String.format("Déclaration annulée migrée sur la PF %d en supposant des exercices de 12 mois suite au dernier exercice non-annulé (%s) : %s.",
								                            pf,
								                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(exercicePrecedent.getDateDebut(), exercicePrecedent.getDateFin())),
								                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(dateDebut, dateFin))));
							}
							else {
								// année civile
								dateDebut = RegDate.get(pf, 1, 1);
								dateFin = RegDate.get(pf, 12, 31);

								// un peu de log pour le suivi
								mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
								              String.format("Déclaration annulée migrée sur la PF %d en supposant un exercice sur l'année civile : %s.",
								                            pf,
								                            StringRenderers.DATE_RANGE_RENDERER.toString(new DateRangeHelper.Range(dateDebut, dateFin))));
							}

							final Declaration di = migrateDeclaration(dossier, dateDebut, dateFin, mr);
							entreprise.addDeclaration(di);
						}
						else {
							mr.addMessage(LogCategory.DECLARATIONS, LogLevel.ERROR,
							              String.format("Impossible de faire correspondre le dossier fiscal %d/%d (sans exercice commercial lié) avec une période d'imposition de la PF %d.",
							                            pf, dossier.getNoParAnnee(), pf));
						}
					}
					else {
						// on prend la première (les autres, s'il y en a, restent à disposition des dossier non-assignés suivants...)
						final PeriodeImposition pi = periodesNonCouvertesPourPf.remove(0);

						// un peu de log pour le suivi
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
						              String.format("Génération d'une déclaration sur la PF %d à partir des dates %s de la période d'imposition calculée et du dossier fiscal %d/%d sans exercice commercial lié.",
						                            pf, StringRenderers.DATE_RANGE_RENDERER.toString(pi), pf, dossier.getNoParAnnee()));

						final Declaration di = migrateDeclaration(dossier, pi.getDateDebut(), pi.getDateFin(), mr);
						entreprise.addDeclaration(di);
					}
				}
			}
			catch (AssujettissementException e) {
				// problème lors du calcul... on ne pourra pas rattrapper les DI...
				mr.addMessage(LogCategory.DECLARATIONS, LogLevel.ERROR, "Erreur rencontrée lors du calcul des périodes d'imposition de l'entreprise -> les DI non liées à un exercice commercial de RegPM ne sont pas migrées.");
				LOGGER.error("Exception lancée lors du calcul des périodes d'imposition de l'entreprise " + entreprise.getNumero(), e);
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
			delai.setEtat(EtatDelaiDeclaration.ACCORDE);
			delai.setSursis(false);
			delai.setCleArchivageCourrier(null);
			delai.setDateDemande(dossier.getDateEnvoi());           // TODO le délai initial est "demandé" à la date d'envoi, non ?
			delai.setDateTraitement(dossier.getDateEnvoi());
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(dossier.getDelaiRetour());
			delais.add(delai);

			// un peu de traçabilité
			mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
			              String.format("Délai initial de retour fixé au %s.", StringRenderers.DATE_RENDERER.toString(dossier.getDelaiRetour())));
		}

		final StringRenderer<DelaiDeclaration> delaiRenderer = delai -> {
			switch (delai.getEtat()) {
			case ACCORDE:
				if (delai.isSursis()) {
					return String.format("sursis au %s", StringRenderers.DATE_RENDERER.toString(delai.getDelaiAccordeAu()));
				}
				else {
					return String.format("délai accordé au %s", StringRenderers.DATE_RENDERER.toString(delai.getDelaiAccordeAu()));
				}
			case REFUSE:
				return "délai refusé";
			case DEMANDE:
				return "délai sans décision";
			default:
				throw new IllegalArgumentException("Etat non-supporté : " + delai.getEtat());
			}
		};

		// demandes ultérieures
		dossier.getDemandesDelai().stream()
				.filter(demande -> {
					if (demande.getEtat() != RegpmTypeEtatDemandeDelai.ACCORDEE && demande.getType() == RegpmTypeDemandeDelai.APRES_SOMMATION) {
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Demande de délai du %s ignorée car %s après sommation.",
						                            StringRenderers.DATE_RENDERER.toString(demande.getDateDemande()),
						                            demande.getEtat()));
						return false;
					}
					return true;
				})
				.map(regpm -> {
					final DelaiDeclaration delai = new DelaiDeclaration();
					copyCreationMutation(regpm, delai);
					delai.setEtat(migrateEtatDelaiDeclaration(regpm));
					delai.setSursis(regpm.getEtat() == RegpmTypeEtatDemandeDelai.ACCORDEE && regpm.getType() == RegpmTypeDemandeDelai.APRES_SOMMATION);
					delai.setCleArchivageCourrier(null);
					delai.setDateDemande(regpm.getDateDemande());
					delai.setDateTraitement(regpm.getDateEnvoi() != null ? regpm.getDateEnvoi() : regpm.getDateDemande());              // TODO on est sûr ce de mapping ?
					delai.setDeclaration(di);
					delai.setDelaiAccordeAu(regpm.getEtat() == RegpmTypeEtatDemandeDelai.ACCORDEE ? regpm.getDelaiAccorde() : null);
					return delai;
				})
				.peek(delai -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
				                             String.format("Génération d'un %s (demande du %s).",
				                                           delaiRenderer.toString(delai),
				                                           StringRenderers.DATE_RENDERER.toString(delai.getDateDemande()))))
				.forEach(delais::add);
		return delais;
	}

	private static EtatDelaiDeclaration migrateEtatDelaiDeclaration(RegpmDemandeDelaiSommation demande) {
		switch (demande.getEtat()) {
		case ACCORDEE:
			return EtatDelaiDeclaration.ACCORDE;
		case DEMANDEE:
			return EtatDelaiDeclaration.DEMANDE;
		case REFUSEE:
			return EtatDelaiDeclaration.REFUSE;
		default:
			throw new IllegalArgumentException("Etat de demande de délai non-supporté : " + demande.getEtat());
		}
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
			etats.add(new EtatDeclarationRetournee(dossier.getDateRetour(), MigrationConstants.SOURCE_RETOUR_DI_MIGREE));
		}

		// un peu de traçabilité sur le travail accompli ici
		etats.forEach(etat -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO, String.format("Etat '%s' migré au %s.", etat.getEtat(), StringRenderers.DATE_RENDERER.toString(etat.getDateObtention()))));

		return etats;
	}

	/**
	 * Wrapper autour d'un for fiscal principal PM pour l'implémentation
	 * de l'interface {@link CollatableDateRange}
	 */
	private final class CollatableForPrincipalPM implements CollatableDateRange, EntityWrapper<ForFiscalPrincipalPM> {

		private final ForFiscalPrincipalPM forFiscal;
		private final MigrationResultProduction mr;

		public CollatableForPrincipalPM(ForFiscalPrincipalPM forFiscal, MigrationResultProduction mr) {
			this.forFiscal = forFiscal;
			this.mr = mr;
		}

		@Override
		public RegDate getDateDebut() {
			return forFiscal.getDateDebut();
		}

		@Override
		public RegDate getDateFin() {
			return forFiscal.getDateFin();
		}

		@Override
		public boolean isValidAt(@Nullable RegDate date) {
			return forFiscal.isValidAt(date);
		}

		@Override
		public boolean isCollatable(DateRange next) {
			boolean collatable = next instanceof CollatableForPrincipalPM && DateRangeHelper.isCollatable(this, next);
			if (collatable) {
				final CollatableForPrincipalPM nextCollatable = (CollatableForPrincipalPM) next;
				final ForFiscalPrincipalPM nextForFiscal = nextCollatable.forFiscal;
				collatable = nextForFiscal.getTypeAutoriteFiscale() == forFiscal.getTypeAutoriteFiscale()
						&& nextForFiscal.getMotifRattachement() == forFiscal.getMotifRattachement()
						&& nextForFiscal.getGenreImpot() == forFiscal.getGenreImpot()
						&& nextForFiscal.getNumeroOfsAutoriteFiscale().equals(forFiscal.getNumeroOfsAutoriteFiscale());

				// toujours ok ? mais peut-être y a-t-il un souci avec une commune qui a conservé le même numéro OFS en changeant d'entité
				if (collatable && forFiscal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS) {
					final Commune commune = infraService.getCommuneByNumeroOfs(forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getDateFin());
					collatable = commune == null || RegDateHelper.isAfter(commune.getDateFin(), forFiscal.getDateFin(), NullDateBehavior.LATEST);
					if (!collatable) {
						mr.addMessage(LogCategory.FORS, LogLevel.INFO,
						              String.format("Fusion des entités %s et %s empêchée par le changement de la commune %d au %s.",
						                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(forFiscal),
						                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(nextForFiscal),
						                            forFiscal.getNumeroOfsAutoriteFiscale(),
						                            StringRenderers.DATE_RENDERER.toString(forFiscal.getDateFin())));
					}
				}
			}
			return collatable;
		}

		@Override
		public CollatableForPrincipalPM collate(DateRange next) {
			if (!isCollatable(next)) {
				throw new IllegalArgumentException("Cet appel n'a pas lieu d'être, les entités ne sont pas collatables...");
			}
			final CollatableForPrincipalPM nextCollatable = (CollatableForPrincipalPM) next;
			final ForFiscalPrincipalPM nextForFiscal = nextCollatable.forFiscal;
			final ForFiscalPrincipalPM collatedFor = new ForFiscalPrincipalPM(forFiscal.getDateDebut(),
			                                                                  forFiscal.getMotifOuverture(),
			                                                                  nextForFiscal.getDateFin(),
			                                                                  nextForFiscal.getMotifFermeture(),
			                                                                  forFiscal.getNumeroOfsAutoriteFiscale(),
			                                                                  forFiscal.getTypeAutoriteFiscale(),
			                                                                  forFiscal.getMotifRattachement());
			collatedFor.setGenreImpot(forFiscal.getGenreImpot());
			mr.addMessage(LogCategory.FORS, LogLevel.INFO, String.format("Fusion des deux entités %s et %s.",
			                                                             StringRenderers.LOCALISATION_DATEE_RENDERER.toString(forFiscal),
			                                                             StringRenderers.LOCALISATION_DATEE_RENDERER.toString(nextForFiscal)));
			return new CollatableForPrincipalPM(collatedFor, mr);
		}

		@Override
		public ForFiscalPrincipalPM getWrappedEntity() {
			return forFiscal;
		}
	}

	/**
	 * Génération des fors principaux de l'entreprise d'après les données de RegPM
	 * @param regpm entreprise dans RegPM
	 * @param unireg entreprise dans Unireg
	 * @param mr collecteur de messages de suivi
	 */
	private void generateForsPrincipaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// [SIFISC-16333] il faut faire attention à la forme juridique de l'entreprise
		final EntityKey entrepriseKey = buildEntrepriseKey(regpm);
		final NavigableMap<RegDate, RegpmTypeFormeJuridique> histoFormesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, entrepriseKey).histo;
		final boolean hasSP = histoFormesJuridiques.values().stream()
				.filter(tfj -> tfj.getCategorie() == RegpmCategoriePersonneMorale.SP)
				.findAny()
				.isPresent();
		final boolean hasPMorAPMnonDP = histoFormesJuridiques.values().stream()
				.filter(tfj -> tfj.getCategorie() == RegpmCategoriePersonneMorale.PM || tfj.getCategorie() == RegpmCategoriePersonneMorale.APM)
				.filter(tfj -> toFormeJuridique(tfj.getCode()) != FormeJuridiqueEntreprise.CORP_DP_ADM)
				.findAny()
				.isPresent();
		final boolean hasDP = histoFormesJuridiques.values().stream()
				.filter(tfj -> toFormeJuridique(tfj.getCode()) == FormeJuridiqueEntreprise.CORP_DP_ADM)
				.findAny()
				.isPresent();

		// récupération des fors principaux valides
		final List<RegpmForPrincipal> forsRegpm = mr.getExtractedData(ForsPrincipauxData.class, entrepriseKey).liste;
		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, entrepriseKey).date;

		// plusieurs catégories différentes dont SP -> migration manuelle (~10 cas...)
		if (hasSP && (hasPMorAPMnonDP || hasDP)) {
			forsRegpm.stream()
					.forEach(ff -> mr.addMessage(LogCategory.FORS, LogLevel.ERROR,
					                             String.format("For fiscal principal %d du %s non-migré car l'entreprise a été SP et PM/APM au cours de son existence.",
					                                           ff.getId().getSeqNo(),
					                                           StringRenderers.DATE_RENDERER.toString(ff.getDateValidite()))));
			return;
		}

		// [SIFISC-17112] dans le cas où une entreprise a été DP et autre au cours de sa vie, il faut prendre la décision de migration de ses fors basée sur sa catégorie courante
		final boolean isDP;
		if (hasDP && hasPMorAPMnonDP) {

			final RegpmTypeFormeJuridique derniereFormeJuridique = histoFormesJuridiques.floorEntry(RegDate.get()).getValue();
			isDP = toFormeJuridique(derniereFormeJuridique.getCode()) == FormeJuridiqueEntreprise.CORP_DP_ADM;
			if (!isDP) {
				mr.addMessage(LogCategory.FORS, LogLevel.WARN,
				              String.format("Entreprise non-DP (dernière forme juridique : '%s') ayant possédé une forme juridique DP par le passé, des fors fiscaux pourront donc être repris.",
				                            derniereFormeJuridique.getCode()));
			}
		}
		else {
			isDP = hasDP;
		}

		// [SIFISC-17097] on doit reprendre les fors des entreprises DP qui ont des immeubles
		final RegDate dateDebutPremierImmeubleDP;
		final boolean isDPavecImmeuble;
		if (isDP) {
			final Map<RegpmCommune, List<DateRange>> directs = couvertureDepuisRattachementsProprietaires(regpm.getRattachementsProprietaires());
			final Map<RegpmCommune, List<DateRange>> viaGroupe = couvertureDepuisAppartenancesGroupeProprietaire(regpm.getAppartenancesGroupeProprietaire());
			isDPavecImmeuble = (directs != null && !directs.isEmpty()) || (viaGroupe != null && !viaGroupe.isEmpty());
			if (isDPavecImmeuble) {
				mr.addMessage(LogCategory.FORS, LogLevel.INFO, "Entreprise DP avec rattachement(s) propriétaire(s), on conservera donc les fors malgré la forme juridique DP.");
			}

			// [SIFISC-17340] calcul de la date de début du premier rapport propriétaire sur une commune
			dateDebutPremierImmeubleDP = Stream.of(directs, viaGroupe)
					.filter(Objects::nonNull)
					.map(Map::values)
					.flatMap(Collection::stream)
					.flatMap(List::stream)
					.map(DateRange::getDateDebut)
					.min(Comparator.naturalOrder())
					.orElse(null);
		}
		else {
			isDPavecImmeuble = false;
			dateDebutPremierImmeubleDP = null;
		}

		// entreprises administratives de droit public (communes...) -> pas de for migré, et c'est normal
		if (isDP && !isDPavecImmeuble) {
			forsRegpm.stream()
					.forEach(ff -> mr.addMessage(LogCategory.FORS, LogLevel.INFO,
					                             String.format("For fiscal principal %d du %s non-migré (administration de droit public).",
					                                           ff.getId().getSeqNo(),
					                                           StringRenderers.DATE_RENDERER.toString(ff.getDateValidite()))));
			return;
		}

		// maintenant, on peut migrer...
		// [SIFISC-17340] attention, si on est en présence d'une société DP avec immeuble, on ne doit reprendre les fors principaux
		// qu'à partir de la date du premier immeuble...
		final List<Pair<ForFiscalPrincipalPM, Boolean>> donneesForsAGenerer;
		if (!forsRegpm.isEmpty()) {
			donneesForsAGenerer = generationDonneesMigrationForsPrincipaux(forsRegpm, hasSP, dateDebutPremierImmeubleDP, mr);
		}
		else {
			// ok, pas de fors principaux dans RegPM... mais si nous n'avons pas affaire à une administration de droit
			// public, on peut générer des fors à partir des sièges...
			donneesForsAGenerer = generationDonneesForsPrincipauxDepuisSieges(regpm, hasSP, dateDebutPremierImmeubleDP, mr);
		}

		// ici, on va collecter les fors qui sont issus d'une décision ACI (= administration effective)
		// afin de générer les entités Unireg 'DecisionACI'
		final List<ForFiscalPrincipalPM> listeIssueDeDecisionAci = new ArrayList<>(donneesForsAGenerer.size());
		final List<ForFiscalPrincipalPM> liste = donneesForsAGenerer.stream()
				.peek(pair -> {
					if (pair.getRight()) {
						listeIssueDeDecisionAci.add(pair.getLeft());
					}
				})
				.map(Pair::getLeft)
				.sorted(Comparator.comparing(ForFiscalPrincipal::getDateDebut))
				.collect(Collectors.toList());

		// assignation des dates de fin
		assigneDatesFin(dateFinActivite, liste);

		// corrections dues aux fusions de communes passées
		final List<CollatableForPrincipalPM> listeAvecTraitementFusions = liste.stream()
				.map(ff -> adapterAutourFusionsCommunes(ff, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes))
				.flatMap(List::stream)
				.map(ff -> new CollatableForPrincipalPM(ff, mr))
				.collect(Collectors.toList());

		// [SIFISC-16545] on fusionne les fors principaux consécutifs par ailleurs identiques
		final List<ForFiscalPrincipalPM> collated = DateRangeHelper.collate(listeAvecTraitementFusions).stream()
				.map(CollatableForPrincipalPM::getWrappedEntity)
				.collect(Collectors.toList());

		// assignation des motifs
		calculeMotifsOuvertureFermeture(collated);

		// on les ajoute au tiers
		collated.stream()
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
	 * Génération des données de base pour les fors principaux depuis les sièges d'une entreprise
	 * @param regpm entreprise de RegPM
	 * @param societeDePersonnes <code>vrai</code> si l'entreprise a toujours été une société de personne, <code>false</code> si elle ne l'a jamais été (le cas hybride doit être traité en amont...)
	 * @param dateDebutPremierImmeubleDP si non-<code>null</code>, nous sommes en présence d'une DP avec immeuble... il ne faut reprendre les fors principaux qu'à partir de cette date
	 * @param mr collecteur de messages de suivi
	 * @return une liste de données pour la création de fors (sans date de fin pour le moment) associés à un booléen qui indique si le for est une administration effective
	 */
	private List<Pair<ForFiscalPrincipalPM, Boolean>> generationDonneesForsPrincipauxDepuisSieges(RegpmEntreprise regpm, boolean societeDePersonnes, @Nullable RegDate dateDebutPremierImmeubleDP, MigrationResultProduction mr) {
		final EntityKey entrepriseKey = buildEntrepriseKey(regpm);
		final NavigableMap<RegDate, RegpmSiegeEntreprise> sieges = mr.getExtractedData(SiegesHistoData.class, entrepriseKey).histo;

		// pas de sièges, pas de fors principaux...
		if (sieges.isEmpty()) {
			return Collections.emptyList();
		}

		// [SIFISC-17340] s'il y a des sièges avant la première date à prendre en compte, il doivent être ignorés
		final RegDate datePremierSiegeAPrendre;
		if (dateDebutPremierImmeubleDP != null) {
			final RegDate dateForValideAuDebutPremierImmeubleDP = sieges.floorKey(dateDebutPremierImmeubleDP);
			if (dateForValideAuDebutPremierImmeubleDP == null) {
				// pas de siège avant la date du premier immeuble -> rien à faire, le processus de comblement des fors principaux fera le reste
				datePremierSiegeAPrendre = sieges.firstKey();
			}
			else {
				datePremierSiegeAPrendre = dateForValideAuDebutPremierImmeubleDP;

				// il y aura donc potentiellement des fors princinpaux ignorés... on va les logger ici
				sieges.headMap(datePremierSiegeAPrendre).values().stream()
						.forEach(siege -> mr.addMessage(LogCategory.FORS, LogLevel.WARN,
						                             String.format("Le siege %d est ignoré car antérieur à la date de début (%s) du premier immeuble associé à l'entreprise DP.",
						                                           siege.getId().getSeqNo(),
						                                           StringRenderers.DATE_RENDERER.toString(dateDebutPremierImmeubleDP))));
			}
		}
		else {
			datePremierSiegeAPrendre = sieges.firstKey();
		}

		return sieges.tailMap(datePremierSiegeAPrendre, true).entrySet().stream()
				.map(entry -> {
					final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
					final RegpmSiegeEntreprise siege = entry.getValue();
					copyCreationMutation(siege, ffp);

					// potentiel rabotage de la date de début du siège
					if (dateDebutPremierImmeubleDP != null && dateDebutPremierImmeubleDP.isAfter(siege.getDateValidite())) {
						ffp.setDateDebut(dateDebutPremierImmeubleDP);

						mr.addMessage(LogCategory.FORS, LogLevel.WARN,
						              String.format("La date de début de validité du siège %d est déplacée du %s au %s pour correspondre à la date de début du premier immeuble associé à l'entreprise DP.",
						                            siege.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(siege.getDateValidite()),
						                            StringRenderers.DATE_RENDERER.toString(dateDebutPremierImmeubleDP)));
					}
					else {
						ffp.setDateDebut(siege.getDateValidite());
					}

					ffp.setGenreImpot(societeDePersonnes ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL);
					ffp.setMotifRattachement(MotifRattachement.DOMICILE);

					final CommuneOuPays cop = buildCommuneOuPays(siege.getDateValidite(), siege::getCommune, siege::getNoOfsPays, String.format("siège %d", siege.getId().getSeqNo()), mr, LogCategory.FORS);
					ffp.setTypeAutoriteFiscale(cop.getTypeAutoriteFiscale());
					ffp.setNumeroOfsAutoriteFiscale(cop.getNumeroOfsAutoriteFiscale());

					mr.addMessage(LogCategory.FORS, LogLevel.INFO,
					              String.format("Données du siège %d utilisées pour les fors principaux : %s/%d depuis le %s.",
					                            siege.getId().getSeqNo(),
					                            cop.getTypeAutoriteFiscale(),
					                            cop.getNumeroOfsAutoriteFiscale(),
					                            StringRenderers.DATE_RENDERER.toString(siege.getDateValidite())));
					if (cop.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						mr.addMessage(LogCategory.FORS, LogLevel.WARN,
						              String.format("Utilisation d'un siège vaudois (%d sur commune %d dès le %s) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?",
						                            siege.getId().getSeqNo(),
						                            cop.getNumeroOfsAutoriteFiscale(),
						                            StringRenderers.DATE_RENDERER.toString(siege.getDateValidite())));
					}

					checkFractionCommuneVaudoise(ffp, mr, LogCategory.FORS);
					return ffp;
				})
				.map(ff -> Pair.of(ff, Boolean.FALSE))
				.collect(Collectors.toList());
	}

	/**
	 * Génération des données servant à la migration des fors principaux existants en fors principaux pour Unireg
	 * @param forsRegpm les fors principaux retenus dans RegPM
	 * @param societeDePersonnes <code>vrai</code> si l'entreprise a toujours été une société de personne, <code>false</code> si elle ne l'a jamais été (le cas hybride doit être traité en amont...)
	 * @param dateDebutPremierImmeubleDP si non-<code>null</code>, nous sommes en présence d'une DP avec immeuble... il ne faut reprendre les fors principaux qu'à partir de cette date
	 * @param mr collecteur de messages de suivi
	 * @return une liste de données pour la création de fors (sans date de fin pour le moment) associés à un booléen qui indique si le for est une administration effective
	 */
	private List<Pair<ForFiscalPrincipalPM, Boolean>> generationDonneesMigrationForsPrincipaux(List<RegpmForPrincipal> forsRegpm,
	                                                                                           boolean societeDePersonnes,
	                                                                                           @Nullable RegDate dateDebutPremierImmeubleDP,
	                                                                                           MigrationResultProduction mr) {

		// construisons d'abord une map des fors principaux pour ne reprendre que les fors à partir de la date de début
		// du premier immeuble DP (si cette date est renseignée, bien-sûr, cf SIFISC-17340)
		final NavigableMap<RegDate, RegpmForPrincipal> mapFors = forsRegpm.stream()
				.collect(Collectors.toMap(RegpmForPrincipal::getDateValidite,
				                          Function.identity(),
				                          (f1, f2) -> { throw new IllegalArgumentException("Il ne devrait plus y avoir, à ce niveau, de fors principaux à la même date..."); },
				                          TreeMap::new));

		final Function<RegpmForPrincipal, Pair<ForFiscalPrincipalPM, Boolean>> mapper =
				f -> {
					final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
					copyCreationMutation(f, ffp);

					// potentiellement, rabotage du for
					if (dateDebutPremierImmeubleDP != null && dateDebutPremierImmeubleDP.isAfter(f.getDateValidite())) {
						ffp.setDateDebut(dateDebutPremierImmeubleDP);

						mr.addMessage(LogCategory.FORS, LogLevel.WARN,
						              String.format("La date de début de validité du for principal %d est déplacée du %s au %s pour correspondre à la date de début du premier immeuble associé à l'entreprise DP.",
						                            f.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(f.getDateValidite()),
						                            StringRenderers.DATE_RENDERER.toString(dateDebutPremierImmeubleDP)));
					}
					else {
						ffp.setDateDebut(f.getDateValidite());
					}

					ffp.setGenreImpot(societeDePersonnes ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL);
					ffp.setMotifRattachement(MotifRattachement.DOMICILE);

					final CommuneOuPays cop = buildCommuneOuPays(f.getDateValidite(), f::getCommune, f::getOfsPays, String.format("for principal %d", f.getId().getSeqNo()), mr, LogCategory.FORS);
					ffp.setTypeAutoriteFiscale(cop.getTypeAutoriteFiscale());
					ffp.setNumeroOfsAutoriteFiscale(cop.getNumeroOfsAutoriteFiscale());

					checkFractionCommuneVaudoise(ffp, mr, LogCategory.FORS);
					return Pair.of(ffp, f.getType() == RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE);
				};

		// s'il y a des fors avant la première date à prendre en compte, ils doivent être ignorés
		final RegDate datePremierForAPrendre;
		if (dateDebutPremierImmeubleDP != null) {
			final RegDate dateForValideAuDebutPremierImmeubleDP = mapFors.floorKey(dateDebutPremierImmeubleDP);
			if (dateForValideAuDebutPremierImmeubleDP == null) {
				// pas de for avant la date du premier immeuble -> rien à faire, le processus de comblement des fors principaux fera le reste
				datePremierForAPrendre = mapFors.firstKey();
			}
			else {
				datePremierForAPrendre = dateForValideAuDebutPremierImmeubleDP;

				// il y aura donc potentiellement des fors princinpaux ignorés... on va les logger ici
				mapFors.headMap(datePremierForAPrendre).values().stream()
						.forEach(fp -> mr.addMessage(LogCategory.FORS, LogLevel.WARN,
						                             String.format("Le for principal %d est ignoré car antérieur à la date de début (%s) du premier immeuble associé à l'entreprise DP.",
						                                           fp.getId().getSeqNo(),
						                                           StringRenderers.DATE_RENDERER.toString(dateDebutPremierImmeubleDP))));
			}
		}
		else {
			datePremierForAPrendre = mapFors.firstKey();
		}

		return mapFors.tailMap(datePremierForAPrendre, true).values().stream()
				.map(mapper)
				.collect(Collectors.toList());
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

	private static String mapTypeRegimeFiscalVD(RegpmTypeRegimeFiscal type) {
		final RegpmTypeRegimeFiscal typeEffectif;
		switch (type) {
		case _01_ORDINAIRE:
		case _11_PARTICIPATIONS_HOLDING:
		case _12_PARTICIPATIONS_PART_IMPOSABLE:
		case _50_PLACEMENT_COLLECTIF_IMMEUBLE:
		case _60_TRANSPORTS_CONCESSIONNES:
		case _70_ORDINAIRE_ASSOCIATION_FONDATION:
		case _109_PM_AVEC_EXONERATION_ART_90G:
		case _190_PM_AVEC_EXONERATION_ART_90CEFH:
		case _709_PURE_UTILITE_PUBLIQUE:
		case _715_FONDATION_ECCLESIASTIQUE_ART_90D:
		case _719_BUTS_CULTUELS_ART_90H:
		case _729_INSTITUTIONS_DE_PREVOYANCE_ART_90I:
		case _739_CAISSES_ASSURANCES_SOCIALES_ART_90F:
		case _749_CONFEDERATION_ETAT_ETRANGER_ART_90AI:
		case _759_CANTON_ETABLISSEMENT_ART_90B:
		case _769_COMMUNE_ETABLISSEMENT_ART_90C:
		case _779_PLACEMENT_COLLECTIF_EXONERE_ART_90J:
		case _41C_SOCIETE_DE_BASE_MIXTE:
		case _42C_SOCIETE_DE_DOMICILE:
			typeEffectif = type;
			break;
		default:
			typeEffectif = RegpmTypeRegimeFiscal._01_ORDINAIRE;
			break;
		}
		return extractCode(typeEffectif);
	}

	private static String extractCode(RegpmTypeRegimeFiscal type) {
		return type.name().substring(1, type.name().indexOf('_', 1));
	}

	private static RegimeFiscal mapRegimeFiscal(RegimeFiscal.Portee portee, RegpmRegimeFiscal rf) {
		final RegimeFiscal unireg = new RegimeFiscal();
		unireg.setDateDebut(rf.getDateDebut());
		unireg.setDateFin(null);
		unireg.setPortee(portee);
		if (portee == RegimeFiscal.Portee.VD) {
			unireg.setCode(mapTypeRegimeFiscalVD(rf.getType()));
		}
		else {
			unireg.setCode(extractCode(rf.getType()));
		}
		return unireg;
	}

	private <T extends RegpmRegimeFiscal> List<RegimeFiscal> mapRegimesFiscaux(RegimeFiscal.Portee portee,
	                                                                           SortedSet<T> regimesRegpm,
	                                                                           @Nullable RegDate dateFinRegimes,
	                                                                           MigrationResultProduction mr) {
		// collecte des régimes fiscaux CH sans date de fin d'abord...
		final List<RegimeFiscal> liste = regimesRegpm.stream()
				.filter(r -> r.getDateAnnulation() == null)         // on ne migre pas les régimes fiscaux annulés
				.filter(rf -> {
					if (rf.getDateDebut() == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début nulle (ou antérieure au 01.08.1291).",
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
				.peek(rf -> checkDateLouche(rf.getDateDebut(),
				                            () -> String.format("Régime fiscal %s %s avec une date de début de validité",
				                                                portee,
				                                                rf.getType()),
				                            LogCategory.SUIVI,
				                            mr))
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
				.peek(a -> checkDateLouche(a.getDateDebut(),
				                           () -> String.format("Allègement fiscal %d avec une date de début de validité", a.getId().getSeqNo()),
				                           LogCategory.SUIVI,
				                           mr))
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

	/**
	 * Migration des états de l'entreprise
	 * @param regpm une entreprise dans RegPM
	 * @param unireg l'entreprise cible dans Unireg
	 * @param mr le collecteur de messages de suivi
	 */
	private void migrateEtatsEntreprise(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// une première structure par date de début (pour détecter les cas des états multiples à la même date
		final NavigableMap<RegDate, List<RegpmEtatEntreprise>> map = regpm.getEtatsEntreprise().stream()
				.filter(e -> !e.isRectifie())
				.filter(e -> {
					if (e.getDateValidite() == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Etat d'entreprise %d (%s) ignoré car sa date de début de validité est nulle (ou antérieure au 01.08.1291).",
						                            e.getId().getSeqNo(),
						                            e.getTypeEtat()));
						return false;
					}
					return true;
				})
				.filter(e -> {
					if (isFutureDate(e.getDateValidite())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Etat d'entreprise %d (%s) ignoré en raison de sa date de début dans le futur (%s).",
						                            e.getId().getSeqNo(),
						                            e.getTypeEtat(),
						                            StringRenderers.DATE_RENDERER.toString(e.getDateValidite())));
						return false;
					}
					return true;
				})
				.collect(Collectors.toMap(RegpmEtatEntreprise::getDateValidite,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
				                          TreeMap::new));

		// [SIFISC-17111] même si plusieurs états commencent à la même date, on les garde tous
		final List<EtatEntreprise> liste = map.values().stream()
				.flatMap(List::stream)
				.peek(e -> checkDateLouche(e.getDateValidite(),
				                           () -> String.format("Etat d'entreprise %d (%s) avec une date de début de validité", e.getId().getSeqNo(), e.getTypeEtat()),
				                           LogCategory.SUIVI,
				                           mr))
				.map(e -> {
					final EtatEntreprise etat = new EtatEntreprise();
					copyCreationMutation(e, etat);
					etat.setDateObtention(e.getDateValidite());
					etat.setType(mapTypeEtatEntreprise(e.getTypeEtat()));
					return etat;
				})
				.collect(Collectors.toList());

		// doit-on prendre en compte la date de fin d'activité de l'entreprise ?
		final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, buildEntrepriseKey(regpm)).date;
		if (dateFinActivite != null && !liste.isEmpty()) {
			final EtatEntreprise dernier = liste.get(liste.size() - 1);
			if (dateFinActivite.isBefore(dernier.getDateObtention())) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
				              String.format("Au moins un état existe après la date de fin de validité (%s) de l'entreprise.", StringRenderers.DATE_RENDERER.toString(dateFinActivite)));
			}
		}

		// fusion des états successifs identiques...
		final MovingWindow<EtatEntreprise> wnd = new MovingWindow<>(liste);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<EtatEntreprise> snap = wnd.next();
			final EtatEntreprise current = snap.getCurrent();
			final EtatEntreprise previous = snap.getPrevious();
			if (previous != null && previous.getType() == current.getType()) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				              String.format("Fusion des deux états d'entreprise '%s' successifs obtenus les %s et %s.",
				                            current.getType(),
				                            StringRenderers.DATE_RENDERER.toString(previous.getDateObtention()),
				                            StringRenderers.DATE_RENDERER.toString(current.getDateObtention())));
				wnd.remove();
			}
		}

		// on loggue les états trouvés et on ajoute dans l'entreprise cible
		liste.stream()
				.peek(etat -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
				                            String.format("Etat '%s' migré, dès le %s.",
				                                          etat.getType(),
				                                          StringRenderers.DATE_RENDERER.toString(etat.getDateObtention()))))
				.forEach(unireg::addEtat);
	}

	private static TypeEtatEntreprise mapTypeEtatEntreprise(RegpmTypeEtatEntreprise regpm) {
		if (regpm == null) {
			return null;
		}
		switch (regpm) {
		case ABSORBEE:
			return TypeEtatEntreprise.ABSORBEE;
		case DISSOUTE:
			return TypeEtatEntreprise.DISSOUTE;
		case EN_FAILLITE:
			return TypeEtatEntreprise.EN_FAILLITE;
		case EN_LIQUIDATION:
			return TypeEtatEntreprise.EN_LIQUIDATION;
		case EN_SUSPENS_FAILLITE:
			return TypeEtatEntreprise.EN_SUSPENS_FAILLITE;
		case FONDEE:
			return TypeEtatEntreprise.FONDEE;
		case INSCRITE_AU_RC:
			return TypeEtatEntreprise.INSCRITE_RC;
		case RADIEE_DU_RC:
			return TypeEtatEntreprise.RADIEE_RC;
		default:
			throw new IllegalArgumentException("Type d'état entreprise inconnu : " + regpm);
		}
	}
}
