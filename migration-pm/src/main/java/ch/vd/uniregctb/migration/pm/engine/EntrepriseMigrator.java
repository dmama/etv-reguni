package ch.vd.uniregctb.migration.pm.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseGeneriqueAdapter;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseSupplementaireAdapter;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.migration.pm.ConsolidationPhase;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.data.CommuneOuPays;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesAdministrateurs;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesCiviles;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesCivilesAppariementEtablissements;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesMandats;
import ch.vd.uniregctb.migration.pm.engine.data.FlagFormesJuridiquesIncompatiblesData;
import ch.vd.uniregctb.migration.pm.engine.data.LocalisationFiscale;
import ch.vd.uniregctb.migration.pm.engine.data.RegimesFiscauxHistoData;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.log.AdressePermanenteLoggedElement;
import ch.vd.uniregctb.migration.pm.log.AppariementEtablissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.DifferencesDonneesCivilesLoggedElement;
import ch.vd.uniregctb.migration.pm.log.ForFiscalIgnoreAbsenceAssujettissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.ForPrincipalOuvertApresFinAssujettissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.FormesJuridiquesIncompatiblesLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.RegimeFiscalMappingLoggedElement;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.ContactEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.InscriptionRC;
import ch.vd.uniregctb.migration.pm.regpm.RaisonSociale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdministrateur;
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
import ch.vd.uniregctb.migration.pm.regpm.RegpmDomicileEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFinFaillite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFonction;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFusion;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;
import ch.vd.uniregctb.migration.pm.regpm.RegpmPrononceFaillite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmQuestionnaireSNC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalCH;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;
import ch.vd.uniregctb.migration.pm.regpm.RegpmSiegeEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmSocieteDirection;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeCritereSegmentation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatQuestionnaireSNC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.EntityWrapper;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.migration.pm.utils.OrganisationDataHelper;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCanton;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.tiers.LocalizedDateRange;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeLettreBienvenue;
import ch.vd.uniregctb.type.TypeMandat;

public class EntrepriseMigrator extends AbstractEntityMigrator<RegpmEntreprise> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseMigrator.class);

	public EntrepriseMigrator(MigrationContexte migrationContexte) {
		super(migrationContexte);
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

	private static final class CouvertureRegimesFiscauxData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		public CouvertureRegimesFiscauxData(KeyedSupplier<Entreprise> entrepriseSupplier) {
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

	private static final class AjoutRemarqueData {
		private final KeyedSupplier<Entreprise> entrepriseSupplier;
		private final List<String> remarques;

		private AjoutRemarqueData(KeyedSupplier<Entreprise> entrepriseSupplier, @Nullable List<String> remarques) {
			this.entrepriseSupplier = entrepriseSupplier;
			this.remarques = Optional.ofNullable(remarques)
					.map(List::stream)
					.map(stream -> stream.collect(Collectors.toCollection(LinkedList::new)))
					.orElseGet(LinkedList::new);
		}

		public AjoutRemarqueData(KeyedSupplier<Entreprise> entrepriseSupplier, String remarque) {
			this(entrepriseSupplier, Collections.singletonList(remarque));
		}

		public AjoutRemarqueData merge(List<String> remarques) {
			final List<String> all = new LinkedList<>(this.remarques);
			if (remarques != null) {
				all.addAll(remarques);
			}
			return new AjoutRemarqueData(entrepriseSupplier, all);
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

		// callback pour le contrôle de la couverture des régimes fiscaux
		mr.registerPreTransactionCommitCallback(CouvertureRegimesFiscauxData.class,
		                                        ConsolidationPhase.COUVERTURE_REGIMES_FISCAUX,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleCouvertureRegimesFiscaux(d, mr, idMapper));

		// callback pour le contrôle des données d'assujettissement
		mr.registerPreTransactionCommitCallback(ComparaisonAssujettissementsData.class,
		                                        ConsolidationPhase.COMPARAISON_ASSUJETTISSEMENTS,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> comparaisonAssujettissements(d, mr, idMapper));

		// callback pour l'ajout des remarques collectées au cours du traitement
		mr.registerPreTransactionCommitCallback(AjoutRemarqueData.class,
		                                        ConsolidationPhase.AJOUT_REMARQUES,
		                                        k -> k.entrepriseSupplier,
		                                        (ard1, ard2) -> ard1.merge(ard2.remarques),
		                                        this::ajoutRemarques);

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

		// les données d'appariement des établissements de l'entreprise
		mr.registerDataExtractor(DonneesCivilesAppariementEtablissements.class,
		                         e -> extractDonneesAppariementEtablissements(e, mr, idMapper),
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

		// régimes fiscaux
		mr.registerDataExtractor(RegimesFiscauxHistoData.class,
		                         e -> extractDonneesRegimesFiscaux(e, mr, idMapper),
		                         null,
		                         null);

		// données des administrateurs
		mr.registerDataExtractor(DonneesAdministrateurs.class,
		                         e -> extractDonneesAdministrateurs(e, mr, idMapper),
		                         null,
		                         null);

		// données du flag d'incompatibilité des formes juridiques civiles et fiscales
		mr.registerDataExtractor(FlagFormesJuridiquesIncompatiblesData.class,
		                         e -> extractFlagFormesJuridiquesIncompatibles(e, mr, idMapper),
		                         null,
		                         null);
	}

	@NotNull
	private DonneesAdministrateurs extractDonneesAdministrateurs(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey moi = buildEntrepriseKey(e);
		return doInLogContext(moi, mr, idMapper, () -> DonneesAdministrateurs.fromEntrepriseAdministree(e, migrationContexte, mr));
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
			// [SIFISC-18101] changement d'ordre
			// - date de bilan de fusion
			// - date de prononcé de faillite ([SIFISC-18088] à ignorer s'il existe une révocation de faillite plus récente)
			// - date de réquisition de radiation
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
					.filter(prononce -> {
						// [SIFISC-18088] ne pas prendre en compte le prononcé de faillite qui est révoqué
						if (prononce.getFinsFaillite() != null && !prononce.getFinsFaillite().isEmpty()) {
							final RegDate revocation = prononce.getFinsFaillite().stream()
									.filter(fin -> !fin.isRectifiee())
									.map(RegpmFinFaillite::getDateRevocation)
									.filter(Objects::nonNull)
									.filter(prononce.getDatePrononceFaillite()::isBeforeOrEqual)
									.findAny()
									.orElse(null);
							if (revocation != null) {
								mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
								              String.format("Prononcé de faillite au %s ignoré (dans le calcul de la date de fin d'activité) pour cause de révocation au %s.",
								                            StringRenderers.DATE_RENDERER.toString(prononce.getDatePrononceFaillite()),
								                            StringRenderers.DATE_RENDERER.toString(revocation)));
								return false;
							}
						}
						return true;
					})
					.map(RegpmPrononceFaillite::getDatePrononceFaillite)
					.sorted(Comparator.reverseOrder())
					.findFirst()
					.orElse(null);

			// récupération de toutes les fins d'activité canditates (il faut logguer si on en a plusieurs)
			final List<Pair<RegDate, String>> finsActivite = Stream.of(Pair.of(dateBilanFusion, "bilan de fusion"),
			                                                           Pair.of(datePrononceFaillite, "prononcé de faillite"),
			                                                           Pair.of(e.getDateRequisitionRadiation(), "réquisition de radiation"),
			                                                           Pair.of(e.getDateDissolution(), "dissolution"))
					.filter(pair -> pair.getLeft() != null)
					.filter(pair -> {
						if (migrationContexte.getDateHelper().isFutureDate(pair.getLeft())) {
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

			// [SIFISC-17845] si aucune date candidate jusque là, et que l'assujettissement ICC prend fin, on prend cette date
			else if (finsActivite.isEmpty()) {
				final List<DateRange> assujettissementIcc = mr.getExtractedData(AssujettissementData.class, entrepriseKey).ranges;
				if (!assujettissementIcc.isEmpty()) {
					final RegDate dateFin = assujettissementIcc.get(assujettissementIcc.size() - 1).getDateFin();
					if (dateFin != null) {
						if (migrationContexte.getDateHelper().isFutureDate(dateFin)) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
							              String.format("La date de fin d'assujettissement ICC est dans le futur (%s), elle est donc ignorée.",
							                            StringRenderers.DATE_RENDERER.toString(dateFin)));
						}
						else {
							// on insère cette date dans le flot afin qu'elle soit prise en compte (il n'y a qu'elle...)
							finsActivite.add(Pair.of(dateFin, "fin d'assujettissement ICC"));
						}
					}
				}
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
				migrationContexte.getDateHelper().checkDateLouche(finActivite.getLeft(),
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
						if (migrationContexte.getDateHelper().isFutureDate(rs.getDateValidite())) {
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
						migrationContexte.getDateHelper().checkDateLouche(rs.getDateValidite(),
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
						if (migrationContexte.getDateHelper().isFutureDate(c.getDateEvolutionCapital())) {
							mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							              String.format("Capital %d (%s) ignoré car sa date de début de validité est dans le futur (%s).",
							                            c.getId().getSeqNo(),
							                            c.getCapitalLibere(),
							                            StringRenderers.DATE_RENDERER.toString(c.getDateEvolutionCapital())));
							return false;
						}
						return true;
					})
					.peek(c -> migrationContexte.getDateHelper().checkDateLouche(c.getDateEvolutionCapital(),
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
						final List<BigDecimal> significantValues;
						if (values.size() > 1) {
							// [SIFISC-18084] s'il y a plusieurs valeurs, on ignore les "0"
							// on regarde d'abord s'il y a autre chose que des zéros
							final boolean hasNonZero = values.stream()
									.filter(capital -> capital.compareTo(BigDecimal.ZERO) != 0)
									.findAny()
									.isPresent();

							if (hasNonZero) {
								// on enlève les zéros !
								significantValues = values.stream()
										.filter(capital -> {
											if (capital.compareTo(BigDecimal.ZERO) == 0) {
												mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.WARN,
												              String.format("Capital 0 du %s ignoré car une valeur non-nulle existe à la même date.",
												                            StringRenderers.DATE_RENDERER.toString(entry.getKey())));
												return false;
											}
											return true;
										})
										.collect(Collectors.toList());
							}
							else {
								significantValues = values;
							}

							// un peu de log de toutes ces valeurs ignorées...
							significantValues.subList(0, significantValues.size() - 1).forEach(capital -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							                                                                                            String.format("Capital %s du %s ignoré car remplacé par une autre valeur à la même date.",
							                                                                                                          capital,
							                                                                                                          StringRenderers.DATE_RENDERER.toString(entry.getKey()))));
						}
						else {
							significantValues = values;
						}
						return Pair.of(entry.getKey(), significantValues.get(significantValues.size() - 1));
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
						if (migrationContexte.getDateHelper().isFutureDate(fj.getDateValidite())) {
							mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.ERROR,
							              String.format("Forme juridique %d (%s) ignorée car sa date de début de validité est dans le futur (%s).",
							                            fj.getPk().getSeqNo(),
							                            fj.getType().getCode(),
							                            StringRenderers.DATE_RENDERER.toString(fj.getDateValidite())));
							return false;
						}
						return true;
					})
					.peek(fj -> migrationContexte.getDateHelper().checkDateLouche(fj.getDateValidite(),
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
						if (migrationContexte.getDateHelper().isFutureDate(s.getDateValidite())) {
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
					.peek(s -> migrationContexte.getDateHelper().checkDateLouche(s.getDateValidite(),
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
							migrationContexte.getUniregStore().removeEntityFromDb(ff);
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

			final Map<RegDate, MotifFor> ouvertures = new HashMap<>();
			{
				// les dates assimilables à une inscription au RC / une constitution d'entreprise -> DEBUT_EXPLOITATION
				Stream.concat(Stream.of(data.regpm.getDateInscriptionRC(), data.regpm.getDateConstitution()),
				              data.regpm.getInscriptionsRC().stream()
						              .filter(inscription -> !inscription.isRectifiee())
						              .map(InscriptionRC::getDateInscription))
						.filter(Objects::nonNull)
						.forEach(date -> ouvertures.put(date, MotifFor.DEBUT_EXPLOITATION));

				// les dates assimilables à une fusion qui s'ouvre -> FUSION_ENTREPRISE
				data.regpm.getFusionsAvant().stream()
						.filter(fusion -> !fusion.isRectifiee())
						.map(fusion -> Stream.of(extractDateFermetureForAvantFusion(fusion),
						                         extractDateOuvertureForApresFusion(fusion)))        // TODO régler la problématique de la date de bilan par rapport à la date d'ouverture du for sur l'entreprise après fusion
						.flatMap(Function.identity())
						.filter(Objects::nonNull)       // utile ?
						.forEach(date -> ouvertures.put(date, MotifFor.FUSION_ENTREPRISES));
			}

			final Map<RegDate, MotifFor> fermetures = new HashMap<>();
			{
				// dates assimilables à une fusion qui débute -> FUSION_ENTREPRISE
				data.regpm.getFusionsApres().stream()
						.filter(fusion -> !fusion.isRectifiee())
						.map(EntrepriseMigrator::extractDateFermetureForAvantFusion)
						.forEach(date -> fermetures.put(date, MotifFor.FUSION_ENTREPRISES));

				// dates de prononcé de faillite
				data.regpm.getEtatsEntreprise().stream()
						.filter(etat -> !etat.isRectifie())
						.map(RegpmEtatEntreprise::getPrononcesFaillite)
						.flatMap(Set::stream)
						.map(RegpmPrononceFaillite::getDatePrononceFaillite)
						.forEach(date -> fermetures.put(date, MotifFor.FAILLITE));
			}

			boolean premierFor = true;
			for (ForFiscalPrincipalPM ffp : ffps) {
				// motif d'ouverture
				if (ffp.getMotifOuverture() == MotifFor.INDETERMINE) {

					// contexte : en général, c'est le premier for de l'entreprise...

					final MotifFor motif = ouvertures.get(ffp.getDateDebut());
					if (motif != null) {
						// motif déterminé
						ffp.setMotifOuverture(motif);
					}
					// si c'est le cas, et que ce for est HC/HS, alors on peut laisser le motif "vide"
					else if (premierFor && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						ffp.setMotifOuverture(null);
					}
					else {
						// tant pis, on laisse INDETERMINE
					}
				}

				// motif de fermeture
				if (ffp.getMotifFermeture() == MotifFor.INDETERMINE) {

					// contexte : en général, c'est le dernier for de l'entreprise

					final MotifFor motif = fermetures.get(ffp.getDateFin());
					if (motif != null) {
						// motif déterminé
						ffp.setMotifFermeture(motif);
					}
					else if (DateRangeHelper.rangeAt(ffps, ffp.getDateFin().getOneDayAfter()) == null) {
						// cessation d'activité
						ffp.setMotifFermeture(MotifFor.CESSATION_ACTIVITE);
					}
					else {
						// on ne sait pas trop, mais il y a encore quelque chose après...
						// --> on laisse INDETERMINE
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
				final LogLevel logLevel = migrationContexte.getActivityManager().isActive(e) ? LogLevel.ERROR : LogLevel.INFO;
				mr.addMessage(LogCategory.SUIVI, logLevel, "Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.");
				return null;
			}

			// [SIFISC-18274] si le même numéro cantonal a été utilisé pour plusieurs entreprises, il ne faut l'utiliser sur aucune d'entre elles
			final Set<Long> identifiantsEntrepriseLies = migrationContexte.getAppariementsMultiplesManager().getIdentifiantsEntreprisesAvecMemeAppariement(idCantonal);
			if (identifiantsEntrepriseLies.size() > 1) {
				final String ids = CollectionsUtils.toString(identifiantsEntrepriseLies, FormatNumeroHelper::numeroCTBToDisplay, ", ");
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
				              String.format("Numéro cantonal connu pour être utilisé sur plusieurs entreprises de RegPM (%s). Aucun lien vers le civil ne sera créé.", ids));

				// enregistrement d'une remarque
				final String remarque = String.format("Contribuable migré sans lien avec RCEnt malgré la présence du numéro cantonal %d, associé à plusieurs entreprises (%s).", idCantonal, ids);
				mr.addPreTransactionCommitData(new AjoutRemarqueData(new KeyedSupplier<>(entrepriseKey, getEntrepriseByRegpmIdSupplier(idMapper, e.getId())), remarque));

				// absence de lien civil
				return null;
			}

			try {
				final Organisation org = migrationContexte.getOrganisationService().getOrganisation(idCantonal, mr);
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

	@Nullable
	private DonneesCivilesAppariementEtablissements extractDonneesAppariementEtablissements(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		final DonneesCiviles donneesCiviles = mr.getExtractedData(DonneesCiviles.class, entrepriseKey);
		if (donneesCiviles == null || donneesCiviles.getOrganisation() == null) {
			// rien du tout...
			return null;
		}

		final Organisation organisation = donneesCiviles.getOrganisation();
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {

			// au travail, maintenant.. il faut apparier tous les établissements connus dans RegPM
			// (on fait une copie de la collection ici car l'algorithme d'appariement en plusieurs phases plus bas va modifier la collection,
			// ce que l'on ne veut bien-sûr pas faire sur les vraies données...)
			final List<RegpmEtablissement> etablissementsAApparier = new ArrayList<>(e.getEtablissements());
			if (etablissementsAApparier.isEmpty()) {
				// pas d'établissements connus, rien à apparier...
				return new DonneesCivilesAppariementEtablissements(organisation);
			}

			// construisons un ensemble des sites secondaires connus dans RCEnt (la clé dans cette map est l'identifiant cantonal du site)
			// (cet ensemble sera réduit au fur et à mesure que les sites seront appariés)
			final Map<Long, Pair<SiteOrganisation, List<DateRange>>> sitesDisponiblesAuCivil = organisation.getDonneesSites().stream()
					.map(site -> {
						final List<DateRange> datesSecondaires = site.getTypeDeSite().stream()
								.filter(type -> type.getPayload() == TypeDeSite.ETABLISSEMENT_SECONDAIRE)
								.map(DateRangeHelper.Range::new)
								.collect(Collectors.toList());
						if (datesSecondaires.isEmpty()) {
							return null;
						}
						return Pair.of(site, DateRangeHelper.merge(datesSecondaires));
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(pair -> pair.getLeft().getNumeroSite(),
					                          Function.identity()));

			// construisons ensuite une map des sites par numéro IDE
			final Map<String, List<SiteOrganisation>> sitesParNumeroIDE = sitesDisponiblesAuCivil.values().stream()
					.map(Pair::getLeft)
					.map(site -> Optional.ofNullable(site.getNumeroIDE()).orElseGet(Collections::emptyList).stream()
							.map(DateRanged::getPayload)
							.map(ide -> Pair.of(site, ide))
							.distinct())
					.flatMap(Function.identity())
					.collect(Collectors.toMap(Pair::getRight,
					                          pair -> new LinkedList<>(Collections.singletonList(pair.getLeft())),      // il faut avoir une collection editable ici
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toCollection(LinkedList::new))));

			// map de sortie, la clé représente ici le numéro de l'établissement dans RegPM
			final Map<Long, SiteOrganisation> appariements = new HashMap<>(etablissementsAApparier.size());

			// première phase : appariement selon le numéro IDE renseigné dans RegPM
			final Iterator<RegpmEtablissement> iteratorPhase1 = etablissementsAApparier.iterator();
			while (iteratorPhase1.hasNext()) {
				final RegpmEtablissement etablissement = iteratorPhase1.next();
				if (etablissement.getNumeroIDE() != null) {

					// mise en place du contexte de log... puisqu'on est en train de traiter un établissement particulier
					doInLogContext(buildEtablissementKey(etablissement), mr, idMapper, () -> {
						final String ide = StringRenderers.NUMERO_IDE_CANONICAL_RENDERER.toString(etablissement.getNumeroIDE());
						final List<SiteOrganisation> sitesPourIde = sitesParNumeroIDE.get(ide);
						if (sitesPourIde == null || sitesPourIde.isEmpty()) {

							// on essaie de détecter un cas beaucoup plus violent (= erreur de saisie manifeste),
							// où ce numéro serait connu de RCEnt mais pour une toute autre entreprise...

							final ServiceOrganisationRaw.Identifiers identifiants = migrationContexte.getOrganisationService().getOrganisationByNoIde(ide);
							if (identifiants == null) {
								mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN, "Numéro IDE présent dans RegPM, mais aucun site secondaire d'organisation de RCEnt n'arbore ce même numéro.");
							}
							else if (identifiants.idCantonalOrganisation != organisation.getNumeroOrganisation()) {
								mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR,
								              String.format("Numéro IDE présent dans RegPM connu dans RCEnt mais lié au site %d d'une autre organisation (%d).",
								                            identifiants.idCantonalSite,
								                            identifiants.idCantonalOrganisation));
							}
							else if (sitesDisponiblesAuCivil.containsKey(identifiants.idCantonalSite)) {
								// RCEnt m'indique un site par le numéro IDE, mais ce même site n'affiche pas le numéro IDE... bizarre !!!
								mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR,
								              String.format("Site %d de l'organisation de RCEnt pointé du doigt (à son insu) par la recherche par numéro IDE.",
								                            identifiants.idCantonalSite));
							}
							else if (organisation.getSiteForNo(identifiants.idCantonalSite) != null) {
								// RCEnt m'indique un site, mais celui-ci n'est pas un site secondaire
								mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR,
								              String.format("Site %d de l'organisation de RCEnt indiqué par la recherche par numéro IDE correspond au site principal de l'organisation.",
								                            identifiants.idCantonalSite));
							}
							else {
								// RCEnt indique un site qui n'est carrément pas renvoyé dans les données complètes de l'organisation
								mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.ERROR,
								              String.format("Site %d de l'organisation de RCEnt indiqué par la recherche par numéro IDE absent des données renvoyée pour l'organisation.",
								                            identifiants.idCantonalSite));
							}
						}
						else if (sitesPourIde.size() > 1) {
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
							              String.format("Plusieurs sites de l'organisation dans RCEnt arborent le numéro IDE présent dans RegPM (sites : %s).",
							                            sitesPourIde.stream()
									                            .mapToLong(SiteOrganisation::getNumeroSite)
									                            .sorted()
							                                    .mapToObj(String::valueOf)
									                            .collect(Collectors.joining(", "))));
						}
						else {
							// un seul site secondaire de RCEnt correspond au numéro IDE présent dans RegPM -> appariement
							final SiteOrganisation vainqueur = sitesPourIde.get(0);

							// un peu de log
							mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.INFO,
							              String.format("Etablissement apparié au site %d de RCEnt par le biais du numéro IDE.", vainqueur.getNumeroSite()));

							// on garde le mapping trouvé et on s'assure que ni l'établissement de RegPM ni le site de RCEnt ne seront
							// ré-utilisés pour un autre appariement
							appariements.put(etablissement.getId(), vainqueur);
							sitesDisponiblesAuCivil.remove(vainqueur.getNumeroSite());
							iteratorPhase1.remove();

							// nettoyage également de la structure de recherche par numéro IDE
							final Iterator<Map.Entry<String, List<SiteOrganisation>>> iteratorSitesParNumeroIde = sitesParNumeroIDE.entrySet().iterator();
							while (iteratorSitesParNumeroIde.hasNext()) {
								final Map.Entry<String, List<SiteOrganisation>> entry = iteratorSitesParNumeroIde.next();
								final Iterator<SiteOrganisation> iteratorSites = entry.getValue().iterator();
								while (iteratorSites.hasNext()) {
									final SiteOrganisation site = iteratorSites.next();
									if (site == vainqueur) {
										iteratorSites.remove();
									}
								}
								if (entry.getValue().isEmpty()) {
									iteratorSitesParNumeroIde.remove();
								}
							}
						}
					});
				}
			}

			// seconde phase : tentative d'appariement selon les communes de domicile

			// regroupement des sites civils par dernière localisation fiscale (le booléen porte l'information 'site fermé')
			final Map<LocalisationFiscale, List<Pair<SiteOrganisation, Boolean>>> sitesCivilsParLocalisation = sitesDisponiblesAuCivil.entrySet().stream()
					.map(entry -> {
						final Pair<SiteOrganisation, List<DateRange>> value = entry.getValue();
						final RegDate derniereDate = value.getRight().stream()
								.max(Comparator.comparing(DateRange::getDateFin, NullDateBehavior.LATEST::compare))
								.map(DateRange::getDateFin)
								.orElse(null);
						final SiteOrganisation site = value.getLeft();
						final LocalisationFiscale localisation = Optional.ofNullable(site.getDomicile(derniereDate))
								.map(domicile -> new LocalisationFiscale(domicile.getTypeAutoriteFiscale(), domicile.getNoOfs()))
								.orElse(null);
						if (localisation == null) {
							return null;
						}
						return Pair.of(localisation, Pair.of(site, derniereDate != null));
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::getLeft,
					                          pair -> Collections.singletonList(pair.getRight()),
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

			// regroupement des établissements de RegPM par dernière localisation fiscale
			final Map<LocalisationFiscale, List<RegpmEtablissement>> etablissementsParLocalisation = etablissementsAApparier.stream()
					.map(etb -> {
						final RegpmDomicileEtablissement dernierDomicile = etb.getDomicilesEtablissements().stream()
								.filter(domicile -> !domicile.isRectifiee())
								.max(Comparator.comparing(RegpmDomicileEtablissement::getDateValidite, NullDateBehavior.EARLIEST::compare))
								.orElse(null);
						if (dernierDomicile == null) {
							return null;
						}
						final RegpmCommune commune = dernierDomicile.getCommune();
						final LocalisationFiscale localisation = new LocalisationFiscale(TAF_COMMUNE_EXTRACTOR.apply(commune), NO_OFS_COMMUNE_EXTRACTOR.apply(commune));
						return Pair.of(localisation, etb);
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::getLeft,
					                          pair -> Collections.singletonList(pair.getRight()),
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

			// nous allons faire une boucle sur toutes les localisations pour lesquelles nous avons un domicile connu dans RegPM
			// (les autres ne feront de toute façon pas l'objet d'appariement, puisque nous ne créons pas de nouveaux établissements secondaires
			// d'après les données civiles dans la migration)
			for (Map.Entry<LocalisationFiscale, List<RegpmEtablissement>> entry : etablissementsParLocalisation.entrySet()) {
				final LocalisationFiscale localisationFiscale = entry.getKey();
				final List<Pair<SiteOrganisation, Boolean>> sitesDansLocalisation = sitesCivilsParLocalisation.get(localisationFiscale);
				if (sitesDansLocalisation != null && !sitesDansLocalisation.isEmpty()) {
					final List<RegpmEtablissement> etablissements = entry.getValue();

					// contrôle d'activité (la clé correspond à 'établissement fermé') sur les établissements
					final Map<Boolean, List<RegpmEtablissement>> etablissementsParFermeture = etablissements.stream()
							.map(etb -> {
								final boolean isOuvert = etb.getEtablissementsStables().stream()
										.filter(etbStable -> etbStable.getDateFin() == null)
										.findAny()
										.isPresent();
								return Pair.of(!isOuvert, etb);
							})
							.collect(Collectors.toMap(Pair::getLeft,
							                          pair -> Collections.singletonList(pair.getRight()),
							                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

					// contrôle d'activité (la clé correspond à 'site fermé') sur les sites civils (cette collection
					// pourra être mise-à-jour au fur et à mesure des appariements, d'où la nécessité d'avoir des collections modifiables)
					final Map<Boolean, List<SiteOrganisation>> sitesParFermeture = sitesDansLocalisation.stream()
							.collect(Collectors.toMap(Pair::getRight,
							                          pair -> new LinkedList<>(Collections.singletonList(pair.getLeft())),
							                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toCollection(LinkedList::new))));

					// calculs séparés pour les ouverts et les fermés
					for (Boolean ferme : Arrays.asList(Boolean.TRUE, Boolean.FALSE)) {
						final List<RegpmEtablissement> etablissementsCandidats = Optional.of(ferme)
								.map(etablissementsParFermeture::get)
								.filter(list -> !list.isEmpty())
								.orElse(null);
						final List<SiteOrganisation> sitesCandidats = Optional.of(ferme)
								.map(sitesParFermeture::get)
								.filter(list -> !list.isEmpty())
								.orElse(null);
						final String activityFlagDisplayString = ferme ? "inactif" : "actif";

						// rien à dire si tout est vide
						if (etablissementsCandidats != null && sitesCandidats != null) {
							if (etablissementsCandidats.size() == 1 && sitesCandidats.size() == 1) {
								// identification facile, quelles que soient les raisons sociales
								final RegpmEtablissement etb = etablissementsCandidats.get(0);
								final SiteOrganisation site = sitesCandidats.get(0);
								appariements.put(etb.getId(), site);

								doInLogContext(buildEtablissementKey(etb), mr, idMapper, () -> {
									mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.INFO,
									              String.format("Etablissement apparié au site %d %s de RCEnt par le biais de son domicile (%s).",
									                            site.getNumeroSite(),
									                            activityFlagDisplayString,
									                            localisationFiscale));
								});

								// pas la peine de faire le nettoyage dans les listes car c'était de toute façon le seul
							}
							else {
								// il faut tenter un matching sur la raison sociale...

								// faisons des groupes de raisons sociales
								final Map<String, List<RegpmEtablissement>> etablissementsParRaisonSociale = etablissementsCandidats.stream()
										.collect(Collectors.toMap(etb -> extractRaisonSociale(etb.getRaisonSociale1(), etb.getRaisonSociale2(), etb.getRaisonSociale3()),
										                          Collections::singletonList,
										                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

								final Map<String, List<SiteOrganisation>> sitesParRaisonSociale = sitesCandidats.stream()
										.map(site -> site.getNom().stream()
												.max(Comparator.comparing(DateRange::getDateDebut))
												.map(DateRanged::getPayload)
												.map(nom -> Pair.of(nom, site))
												.orElse(null))
										.filter(Objects::nonNull)
										.collect(Collectors.toMap(Pair::getLeft,
										                          pair -> Collections.singletonList(pair.getRight()),
										                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

								// une nouvelle boucle sur les raisons sociales
								for (Map.Entry<String, List<RegpmEtablissement>> entryEtablissementsPourRaisonSociale : etablissementsParRaisonSociale.entrySet()) {
									final List<RegpmEtablissement> etbsRaisonSociale = entryEtablissementsPourRaisonSociale.getValue();
									final String raisonSociale = entryEtablissementsPourRaisonSociale.getKey();
									final List<SiteOrganisation> sitesAvecRaisonSociale = sitesParRaisonSociale.get(raisonSociale);
									if (sitesAvecRaisonSociale != null) {
										if (etbsRaisonSociale.size() == 1 && sitesAvecRaisonSociale.size() == 1) {
											// même commune, même nom, même flag d'activité -> c'est le bon
											final RegpmEtablissement etb = etbsRaisonSociale.get(0);
											final SiteOrganisation siteUtilise = sitesAvecRaisonSociale.get(0);
											appariements.put(etb.getId(), siteUtilise);

											doInLogContext(buildEtablissementKey(etb), mr, idMapper,
											               () -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.INFO,
											                                   String.format("Etablissement apparié au site %d %s de RCEnt par le biais de son domicile (%s) et de sa raison sociale (%s).",
											                                                 siteUtilise.getNumeroSite(),
											                                                 activityFlagDisplayString,
											                                                 localisationFiscale,
											                                                 raisonSociale)));

											// il faut maintenant écarter le site des sites potentiels dans une prochaine boucle
											final Iterator<SiteOrganisation> iteratorSitesCandidats = sitesCandidats.iterator();
											while (iteratorSitesCandidats.hasNext()) {
												final SiteOrganisation site = iteratorSitesCandidats.next();
												if (site == siteUtilise) {
													iteratorSitesCandidats.remove();
												}
											}
										}
										else {
											// pas de match parfait...
											etbsRaisonSociale.forEach(etb -> doInLogContext(buildEtablissementKey(etb), mr, idMapper,
											                                                () -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
											                                                                    String.format("Etablissement non-apparié à un site RCEnt car il existe plusieurs sites %ss (%s) au même endroit (%s) avec la même raison sociale (%s).",
											                                                                                  activityFlagDisplayString,
											                                                                                  sitesAvecRaisonSociale.stream()
													                                                                                  .map(SiteOrganisation::getNumeroSite)
													                                                                                  .map(String::valueOf)
													                                                                                  .collect(Collectors.joining(", ")),
											                                                                                  localisationFiscale,
											                                                                                  raisonSociale))));
										}
									}
									else {
										// aucun site civil avec la même raison sociale... tant pis !
										etbsRaisonSociale.forEach(etb -> doInLogContext(buildEtablissementKey(etb), mr, idMapper,
										                                                () -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
										                                                                    String.format("Etablissement non-apparié à un site RCEnt car il n'existe aucun site %s sur %s avec la raison sociale '%s'.",
										                                                                                  activityFlagDisplayString,
										                                                                                  localisationFiscale,
										                                                                                  raisonSociale))));
									}
								}
							}
						}
						else if (etablissementsCandidats != null) {
							// aucun site candidat sur la même commune avec le même flag "fermé"
							// (si nous sommes ici, c'est qu'il y a au moins un site civil, et que tous ont le flag opposé)
							etablissementsCandidats.forEach(etb -> doInLogContext(buildEtablissementKey(etb), mr, idMapper,
							                                                      () -> mr.addMessage(LogCategory.ETABLISSEMENTS, LogLevel.WARN,
							                                                                          String.format("Etablissement non-apparié à un site RCEnt car il n'existe aucun site %s sur %s.",
							                                                                                        activityFlagDisplayString,
							                                                                                        localisationFiscale))));
						}
					}
				}
			}

			// on veut une liste des établissements appariés avec leur raison sociale
			appariements.entrySet().stream()
					.map(entry -> Pair.of(mr.getCurrentGraphe().getEtablissements().get(entry.getKey()), entry.getValue()))
					.forEach(pair -> doInLogContext(buildEtablissementKey(pair.getLeft()), mr, idMapper,
					                                () -> {
						                                final RegpmEtablissement etablissement = pair.getLeft();
						                                final SiteOrganisation site = pair.getRight();
						                                final String regpmRaisonSociale = extractRaisonSociale(etablissement.getRaisonSociale1(), etablissement.getRaisonSociale2(), etablissement.getRaisonSociale3());
						                                final String rcentRaisonSociale = site.getNom().stream()
								                                .max(Comparator.comparing(DateRange::getDateDebut))
								                                .map(DateRanged::getPayload)
								                                .orElse(null);

						                                mr.pushContextValue(AppariementEtablissementLoggedElement.class, new AppariementEtablissementLoggedElement(site.getNumeroSite(),
						                                                                                                                                           regpmRaisonSociale,
						                                                                                                                                           rcentRaisonSociale));
						                                try {
							                                final LogLevel logLevel = areEqual(regpmRaisonSociale, rcentRaisonSociale, String::equalsIgnoreCase) ? LogLevel.INFO : LogLevel.WARN;
							                                mr.addMessage(LogCategory.APPARIEMENTS_ETABLISSEMENTS_SECONDAIRES, logLevel, StringUtils.EMPTY);
						                                }
						                                finally {
							                                mr.popContexteValue(AppariementEtablissementLoggedElement.class);
						                                }
					                                })

					);

			// c'est fini, on renvoie ce qu'on a trouvé
			return new DonneesCivilesAppariementEtablissements(organisation, appariements);
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
						if (migrationContexte.getDateHelper().isFutureDate(ff.getDateValidite())) {
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
			liste.forEach(ff -> migrationContexte.getDateHelper().checkDateLouche(ff.getDateValidite(),
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
				final List<DateRange> calcules = neverNull(DateRangeHelper.merge(migrationContexte.getAssujettissementService().determine(entreprise)));

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

							// on veut faire la différence entre des assujettissements différents sur un jour ou plus
							if (onlyDifferringByOneDay(lilicIntersectant, calculesIntersectant)) {
								mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.WARN,
								              String.format("Période d'assujettissement décalée d'un jour : avant (%s) et après (%s).",
								                            toDisplayString(lilicIntersectant),
								                            toDisplayString(calculesIntersectant)));
							}
							else {
								mr.addMessage(LogCategory.ASSUJETTISSEMENTS, LogLevel.WARN,
								              String.format("Période(s) d'assujettissement modifiée(s) : avant (%s) et après (%s).",
								                            toDisplayString(lilicIntersectant),
								                            toDisplayString(calculesIntersectant)));
							}
						}
					}
				}

				// si la PM est déclarée "inactive" mais qu'Unireg lui calcule un assujettissement après la date seuil du 01.01.2015,
				// c'est un problème, non ?
				final RegDate seuilActivite = migrationContexte.getDatesParticulieres().getSeuilActivite();
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
	 * @param ranges1 des périodes
	 * @param ranges2 d'autres périodes
	 * @return <code>true</code> s'il n'y a qu'une seule période de chaque côté et que celles-ci ne diffèrent que d'un jour au début ou à la fin
	 */
	private static boolean onlyDifferringByOneDay(List<DateRange> ranges1, List<DateRange> ranges2) {
		if (ranges1.size() != 1 || ranges2.size() != 1) {
			return false;
		}

		// il n'y en a qu'un de chaque côté, donc pour que la différence ne soit que d'un jour,
		// il faut et il suffit qu'une des bornes soit identique des deux côtés et que l'autre
		// décalée d'un jour, dans un sens ou dans l'autre

		final DateRange range1 = ranges1.get(0);
		final DateRange range2 = ranges2.get(0);
		final RegDate debut1 = range1.getDateDebut();
		final RegDate debut2 = range2.getDateDebut();
		final RegDate fin1 = range1.getDateFin();
		final RegDate fin2 = range2.getDateFin();

		if (debut1 == debut2) {
			return fin1 != null && fin2 != null && (fin1.getOneDayAfter() == fin2 || fin1.getOneDayBefore() == fin2);
		}
		if (fin1 == fin2) {
			return debut1 != null && debut2 != null && (debut1.getOneDayAfter() == debut2 || debut1.getOneDayBefore() == debut2);
		}
		return false;
	}

	/**
	 * Des remarques ont été enregistrées pour une entreprise... il est maintenant temps de les ajouter..
	 * @param data données des remarques collectées
	 */
	private void ajoutRemarques(AjoutRemarqueData data) {
		final Entreprise entreprise = data.entrepriseSupplier.get();
		final RemarqueDAO remarqueDAO = migrationContexte.getRemarqueDAO();
		data.remarques.stream()
				.map(texte -> texte.length() > LengthConstants.TIERS_REMARQUE ? texte.substring(0, LengthConstants.TIERS_REMARQUE - 1) : texte)
				.map(texte -> {
					final Remarque remarque = new Remarque();
					remarque.setTexte(texte);
					remarque.setTiers(entreprise);
					return remarque;
				})
				.forEach(remarqueDAO::save);
	}

	/**
	 * @param data donnée d'identification de l'entreprise dont la couverture des régimes fiscaux est à contrôler
	 * @param mr collecteur de message de suivi et manipulateur de contexte de log
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	private void controleCouvertureRegimesFiscaux(CouvertureRegimesFiscauxData data, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey keyEntreprise = data.entrepriseSupplier.getKey();
		doInLogContext(keyEntreprise, mr, idMapper, () -> {
			final Entreprise entreprise = data.entrepriseSupplier.get();
			final List<DateRange> forsBeneficeCapital = DateRangeHelper.collateRange(DateRangeHelper.merge(entreprise.getForsFiscauxNonAnnules(true).stream()
					                                                                                               .filter(ff -> ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL)
					                                                                                               .collect(Collectors.toList())));

			// pas la peine d'aller plus loin si pas de fors non-annulé 'bénéfice capital'
			if (forsBeneficeCapital != null && !forsBeneficeCapital.isEmpty()) {
				final Map<RegimeFiscal.Portee, List<RegimeFiscal>> regimesParPortee = entreprise.getRegimesFiscauxNonAnnulesTries().stream()
						.collect(Collectors.toMap(RegimeFiscal::getPortee,
						                          Collections::singletonList,
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

				// zone à couvrir effectivement
				final DateRange aCouvrir = new DateRangeHelper.Range(RegDate.get(migrationContexte.getParametreAppService().getPremierePeriodeFiscalePersonnesMorales(), 1, 1), null);

				// zones de fors 'bénéfice/capital' non-couverte par des régimes fiscaux ?
				for (RegimeFiscal.Portee portee : RegimeFiscal.Portee.values()) {
					final List<RegimeFiscal> regimes = regimesParPortee.get(portee);
					final List<DateRange> nonCouverts = DateRangeHelper.intersections(aCouvrir, DateRangeHelper.subtract(forsBeneficeCapital, regimes, new DateRangeAdapterCallback()));
					if (nonCouverts != null && !nonCouverts.isEmpty()) {
						// nous avons des trous...
						for (DateRange trou : nonCouverts) {
							// s'il y a déjà un régime avant le trou, on va juste le prolonger
							final RegimeFiscal avantTrou = DateRangeHelper.rangeAt(regimes, trou.getDateDebut().getOneDayBefore());
							if (avantTrou != null) {
								mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
								              String.format("Régime fiscal %s %s de type '%s' prolongé jusqu'au %s pour couvrir les fors de l'entreprise.",
								                            avantTrou.getPortee(),
								                            StringRenderers.DATE_RANGE_RENDERER.toString(avantTrou),
								                            avantTrou.getCode(),
								                            StringRenderers.DATE_RENDERER.toString(trou.getDateFin())));

								avantTrou.setDateFin(trou.getDateFin());
							}
							else {
								// pas de régime avant le trou... peut-être après ?
								final RegimeFiscal apresTrou = trou.getDateFin() != null
										? DateRangeHelper.rangeAt(regimes, trou.getDateFin().getOneDayAfter())
										: null;
								if (apresTrou != null) {
									mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
									              String.format("Régime fiscal %s %s de type '%s' pris en compte dès le %s pour couvrir les fors de l'entreprise.",
									                            apresTrou.getPortee(),
									                            StringRenderers.DATE_RANGE_RENDERER.toString(apresTrou),
									                            apresTrou.getCode(),
									                            StringRenderers.DATE_RENDERER.toString(trou.getDateDebut())));

									apresTrou.setDateDebut(trou.getDateDebut());
								}
								else {
									// pas de régime ni avant, ni après le trou (= pas de régime du tout !!)
									// -> par défaut en fonction de la forme juridique de l'entreprise au début du trou
									final RegpmTypeRegimeFiscal type;
									final NavigableMap<RegDate, RegpmTypeFormeJuridique> histoFormesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, keyEntreprise).histo;
									final Map.Entry<RegDate, RegpmTypeFormeJuridique> fjAvantDebutTrou = histoFormesJuridiques.floorEntry(trou.getDateDebut());
									if (fjAvantDebutTrou != null) {
										type = getDefautTypeRegimeFiscalPourFormeJuridique(fjAvantDebutTrou.getValue());
									}
									else {
										final Map.Entry<RegDate, RegpmTypeFormeJuridique> fjApresDebutTrou = histoFormesJuridiques.higherEntry(trou.getDateDebut());
										if (fjApresDebutTrou != null) {
											type = getDefautTypeRegimeFiscalPourFormeJuridique(fjApresDebutTrou.getValue());
										}
										else {
											// aucune forme juridique connue -> on prend 01
											type = RegpmTypeRegimeFiscal._01_ORDINAIRE;
										}
									}

									final RegimeFiscal rf = new RegimeFiscal(trou.getDateDebut(), null, portee, type.getCode());
									entreprise.addRegimeFiscal(rf);

									mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
									              String.format("Ajout d'un régime fiscal %s de type '%s' sur la période %s pour couvrir les fors de l'entreprise.",
									                            portee,
									                            rf.getCode(),
									                            StringRenderers.DATE_RANGE_RENDERER.toString(rf)));

									// pas la peine de traiter les autres trous : ils sont traités par ordre chronologique, il n'y a aucun régime
									// fiscal présent et nous avons mis une date de fin 'null' à celui que nous venons de créer...
									break;
								}
							}
						}
					}
				}
			}
		});
	}

	private static RegpmTypeRegimeFiscal getDefautTypeRegimeFiscalPourFormeJuridique(RegpmTypeFormeJuridique fj) {
		switch (fj.getCategorie()) {
		case PM:
		case SP:
			return RegpmTypeRegimeFiscal._01_ORDINAIRE;
		case APM:
			return RegpmTypeRegimeFiscal._70_ORDINAIRE_ASSOCIATION_FONDATION;
		default:
			throw new IllegalArgumentException("Catégorie de forme judirique invalide : " + fj.getCategorie());
		}
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
						// [SIFISC-17164] s'il y avait des fors dans RegPM, on va essayer de les reprendre
						final List<RegpmForPrincipal> forsRegPM = mr.getExtractedData(ForsPrincipauxData.class, keyEntreprise).liste;
						if (forsRegPM.isEmpty()) {
							// vraiment rien... on va créer des fors "pays inconnu"
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
							// on fait le travail à partir des données de RegPM
							final NavigableMap<RegDate, RegpmForPrincipal> mapForsPrincipaux = forsRegPM.stream()
									.collect(Collectors.toMap(RegpmForPrincipal::getDateValidite,
									                          Function.identity(),
									                          (f1, f2) -> { throw new IllegalArgumentException("Plusieurs fors principaux qui commencent le même jour ? aurait dû être traité en amont..."); },
									                          TreeMap::new));

							// boucle sur les trous
							final List<ForFiscalPrincipalPM> forsPrincipaux = new LinkedList<>();
							for (DateRange nonCouvert : rangesNonCouverts) {
								// les fors principaux de RegPM sont valides jusqu'à preuve du contraire...
								RegDate curseur = nonCouvert.getDateDebut();
								while (curseur != null && nonCouvert.isValidAt(curseur)) {
									// puisqu'il y a au moins un for dans RegPM, on le trouve forcément en regardant derrière puis devant
									final RegDate dateDebut = curseur;
									final RegpmForPrincipal forSource = Optional.of(dateDebut)
											.map(mapForsPrincipaux::floorEntry)
											.orElseGet(() -> mapForsPrincipaux.higherEntry(dateDebut))
											.getValue();
									final RegDate dateFin = RegDateHelper.minimum(nonCouvert.getDateFin(),
									                                              Optional.ofNullable(mapForsPrincipaux.higherKey(curseur)).map(RegDate::getOneDayBefore).orElse(null),
									                                              NullDateBehavior.LATEST);

									final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
									ffp.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
									ffp.setDateDebut(dateDebut);
									ffp.setDateFin(dateFin);
									ffp.setMotifOuverture(MotifFor.INDETERMINE);
									ffp.setMotifFermeture(dateFin != null ? MotifFor.INDETERMINE : null);
									final CommuneOuPays communeOuPays = new CommuneOuPays(forSource);
									ffp.setTypeAutoriteFiscale(communeOuPays.getTypeAutoriteFiscale());
									ffp.setNumeroOfsAutoriteFiscale(communeOuPays.getNumeroOfsAutoriteFiscale());
									ffp.setMotifRattachement(MotifRattachement.DOMICILE);
									forsPrincipaux.add(ffp);

									// prochaine boucle (= changement de for possible dans le même trou)
									curseur = Optional.ofNullable(dateFin).map(RegDate::getOneDayAfter).orElse(null);
								}
							}

							// gestion des fusions de communes, si jamais...
							forsPrincipaux.stream()
									.peek(ff -> mr.addMessage(LogCategory.FORS, LogLevel.WARN,
									                          String.format("For principal %s généré (d'après les données RegPM précédemment ignorées) pour couvrir les fors secondaires.",
									                                        StringRenderers.LOCALISATION_DATEE_RENDERER.toString(ff))))
									.map(ff -> adapterAutourFusionsCommunes(ff, mr, LogCategory.FORS, AbstractEntityMigrator::adapteMotifsForsFusionCommunes))
									.flatMap(List::stream)
									.forEach(entreprise::addForFiscal);
						}
					}
					else {
						// il y a des fors principaux, donc il y a toujours au moins un for principal
						// avant ou après chaque trou... (pas forcément juste avant ou juste après car la période des ranges non-couverts
						// ne concerne que les fors secondaires...)

						final NavigableMap<RegDate, ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted().stream()
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

					// et on va recalculer la date de début du premier exercice commercial
					assigneDateDebutPremierExerciceCommercial(entreprise);
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
		Entreprise unireg = migrationContexte.getUniregStore().getEntityFromDb(Entreprise.class, regpm.getId());
		if (unireg == null) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.");
			unireg = migrationContexte.getUniregStore().saveEntityToDb(createEntreprise(regpm));
		}
		idMapper.addEntreprise(regpm, unireg);

		final KeyedSupplier<Entreprise> moi = new KeyedSupplier<>(buildEntrepriseKey(regpm), getEntrepriseByUniregIdSupplier(unireg.getId()));

		// Récupération des données civiles si elles existent
		Organisation rcent = null;
		if (migrationContexte.getOrganisationService().isRcentEnabled()) {
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

			// dans ce cas, on reprend le numéro IDE dans Unireg
			final String noIde = StringRenderers.NUMERO_IDE_CANONICAL_RENDERER.toString(regpm.getNumeroIDE());
			if (NumeroIDEHelper.isValid(noIde)) {
				final IdentificationEntreprise ide = new IdentificationEntreprise();
				ide.setNumeroIde(noIde);
				unireg.addIdentificationEntreprise(ide);
			}
			else {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
				              String.format("Le numéro IDE présent dans RegPM (catégorie = %s, numéro = %d) est invalide, il sera ignoré.",
				                            regpm.getNumeroIDE().getCategorie(),
				                            regpm.getNumeroIDE().getNumero()));
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

		// enregistrement de cette entreprise pour le contrôle de la couverture des régimes fiscaux
		mr.addPreTransactionCommitData(new CouvertureRegimesFiscauxData(moi));

		// enregistrement de cette entreprise pour la comparaison des assujettissements avant/après
		mr.addPreTransactionCommitData(new ComparaisonAssujettissementsData(regpm, migrationContexte.getActivityManager().isActive(regpm), moi));

		final String raisonSociale = Optional.ofNullable(mr.getExtractedData(RaisonSocialeHistoData.class, moi.getKey()).histo.lastEntry()).map(Map.Entry::getValue).orElse(null);
		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, raisonSociale, unireg, mr);
		migratePersonneContact(regpm.getContact1(), unireg, mr);
		migrateNotes(regpm.getNotes(), unireg);
		migrateFlagDoublon(regpm, unireg, mr);
		migrateDonneesCiviles(regpm, rcent, unireg, mr);
		logDroitPublicAPM(regpm, mr);

		migrateAdresses(regpm, rcent, unireg, mr, idMapper);
		migrateAllegementsFiscaux(regpm, unireg, mr);
		migrateRegimesFiscaux(regpm, unireg, mr);
		migrateExercicesCommerciaux(regpm, unireg, mr);
		migrateDeclarationsImpot(regpm, unireg, mr, idMapper);
		migrateQuestionnairesSNC(regpm, unireg, mr);
		migrateLettresBienvenue(regpm, unireg, mr, idMapper);
		generateForsPrincipaux(regpm, unireg, mr);
		migrateImmeubles(regpm, unireg, mr);

		// premier contrôle des régimes fiscaux par rapport aux fors, avant de toute façon celui qui sera lancé à la fin de la transaction
		// (ici car la génération de l'établissement principal peut induire un flush de la session et donc lancer les validateurs)
		controleCouvertureRegimesFiscaux(new CouvertureRegimesFiscauxData(moi), mr, idMapper);

		generateEtablissementPrincipal(regpm, rcent, linkCollector, idMapper, mr);
		migrateEtatsEntreprise(regpm, unireg, mr);

		// doit être fait après les exercices commerciaux
		migrateLIASF(regpm, unireg, mr);
		migrateFlagsDApresRegimesFiscaux(regpm, unireg, mr);

		migrateMandataires(regpm, unireg, mr, linkCollector, idMapper);
		migrateFusionsApres(regpm, linkCollector, idMapper);
		migrateSocietesDeDirection(regpm, mr, linkCollector, idMapper);
		migrateAdministrateursSocieteImmobiliere(regpm, mr, linkCollector, idMapper);

		// log de suivi à la fin des opérations pour cette entreprise
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Entreprise migrée : %s.", FormatNumeroHelper.numeroCTBToDisplay(unireg.getNumero())));
	}

	@NotNull
	private FlagFormesJuridiquesIncompatiblesData extractFlagFormesJuridiquesIncompatibles(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final boolean rcentEnabled = migrationContexte.getOrganisationService().isRcentEnabled();
		final EntityKey moi = buildEntrepriseKey(e);
		return doInLogContext(moi, mr, idMapper, () -> {
			final DonneesCiviles donneesCiviles = rcentEnabled ? mr.getExtractedData(DonneesCiviles.class, moi) : null;
			final Organisation rcent = donneesCiviles != null ? donneesCiviles.getOrganisation() : null;
			return new FlagFormesJuridiquesIncompatiblesData(hasFormesJuridiquesIncompatibles(e, rcent, mr));
		});
	}

	/**
	 * @param regpm entreprise de RegPM
	 * @param rcent organisation de RCEnt (optionnelle)
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @return <code>true</code> si les données RegPM et RCEnt contiennent des formes juridiques incompatibles (au sens du SIFISC-18378])
	 */
	private boolean hasFormesJuridiquesIncompatibles(RegpmEntreprise regpm, @Nullable Organisation rcent, MigrationResultContextManipulation mr) {
		if (rcent == null) {
			// pas de données RCEnt -> pas d'incompatibilité possible
			return false;
		}

		// récupération des formes juridiques de chaque côté
		final RegDate dateDebutPremiereFormeJuridiqueCivile = rcent.getFormeLegale().stream()
				.map(DateRanged::getDateDebut)
				.min(Comparator.naturalOrder())
				.orElse(null);
		final Set<FormeLegale> rcentFormesJuridiques = rcent.getFormeLegale().stream()
				.map(DateRanged::getPayload)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(FormeLegale.class)));

		// rien côté RCEnt, on oublie...
		if (dateDebutPremiereFormeJuridiqueCivile == null || rcentFormesJuridiques.isEmpty()) {
			return false;
		}

		final Optional<String> codeRegpmFormeJuridique = Optional.of(mr.getExtractedData(FormeJuridiqueHistoData.class, buildEntityKey(regpm)).histo)
				.map(map -> map.floorEntry(dateDebutPremiereFormeJuridiqueCivile))
				.map(Map.Entry::getValue)
				.map(RegpmTypeFormeJuridique::getCode);

		// pas de forme juridique dans RegPM antérieure aux données civiles, on oublie (pas d'incompatibilité possible)
		if (!codeRegpmFormeJuridique.isPresent()) {
			return false;
		}

		final FormeJuridiqueEntreprise regpmFormeJuridique = toFormeJuridique(codeRegpmFormeJuridique.get());
		FormeLegale rcentFormeJuridiqueGenante = null;

		// 1. "entreprise individuelle" d'un côté et autre chose de l'autre...
		final Set<FormeJuridiqueEntreprise> regpmFormesJuridiquesAccepteesPourEntrepriseIndividuelleCivile = EnumSet.of(FormeJuridiqueEntreprise.EI, FormeJuridiqueEntreprise.SC, FormeJuridiqueEntreprise.SNC);
		if (rcentFormesJuridiques.contains(FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE) && !regpmFormesJuridiquesAccepteesPourEntrepriseIndividuelleCivile.contains(regpmFormeJuridique)) {
			rcentFormeJuridiqueGenante = FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE;
		}
		else if (regpmFormeJuridique == FormeJuridiqueEntreprise.EI && containsSomethingElse(rcentFormesJuridiques, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE)) {
			rcentFormeJuridiqueGenante = rcentFormesJuridiques.stream()
					.filter(fl -> fl != FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE)
					.findAny()
					.get();
		}
		else {
			final Set<FormeLegale> rcentSocietePersonnes = EnumSet.of(FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF, FormeLegale.N_0104_SOCIETE_EN_COMMANDITE, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE);
			final Set<FormeJuridiqueEntreprise> regpmSocietePersonnes = EnumSet.of(FormeJuridiqueEntreprise.SC, FormeJuridiqueEntreprise.SNC);

			// 2. "société de personnes" d'un côté et pas de l'autre
			if (regpmSocietePersonnes.contains(regpmFormeJuridique) && containsSomethingElse(FormeLegale.class, rcentFormesJuridiques, rcentSocietePersonnes)) {
				rcentFormeJuridiqueGenante = rcentFormesJuridiques.stream()
						.filter(fl -> !rcentSocietePersonnes.contains(fl))
						.findAny()
						.get();
			}
			else if (!regpmSocietePersonnes.contains(regpmFormeJuridique) && intersect(FormeLegale.class, rcentFormesJuridiques, rcentSocietePersonnes)) {
				rcentFormeJuridiqueGenante = rcentFormesJuridiques.stream()
						.filter(rcentSocietePersonnes::contains)
						.findAny()
						.get();
			}
		}

		final boolean incompatible = rcentFormeJuridiqueGenante != null;
		if (incompatible) {
			// on doit sortir une liste...
			final String regpmRaisonSociale = mr.getExtractedData(RaisonSocialeHistoData.class, buildEntrepriseKey(regpm)).histo.lastEntry().getValue();
			final String rcentRaisonSociale = OrganisationDataHelper.getLastValue(rcent.getNom());
			mr.pushContextValue(FormesJuridiquesIncompatiblesLoggedElement.class, new FormesJuridiquesIncompatiblesLoggedElement(codeRegpmFormeJuridique.get(), rcentFormeJuridiqueGenante, regpmRaisonSociale, rcentRaisonSociale));
			try {
				mr.addMessage(LogCategory.FORMES_JURIDIQUES_INCOMPATIBLES, LogLevel.ERROR, "Entreprise migrée sans fors, déclarations, régimes fiscaux ni spécificités fiscales (l'appariement est-il correct ?).");
			}
			finally {
				mr.popContexteValue(FormesJuridiquesIncompatiblesLoggedElement.class);
			}
		}
		return incompatible;
	}

	/**
	 * @param set un ensemble de valeurs
	 * @param value une valeur particulière
	 * @param <T> le type des valeurs de l'ensemble
	 * @return <code>true</code> si l'ensemble contient au moins une valeur qui n'est pas la valeur donnée
	 */
	private static <T extends Enum<T>> boolean containsSomethingElse(@NotNull Set<T> set, @NotNull T value) {
		return !set.isEmpty() && (!set.contains(value) || set.size() > 1);
	}

	/**
	 * @param clazz la classe des éléments des ensembles
	 * @param set un ensemble de valeurs
	 * @param values un autre ensemble de valeurs
	 * @param <T> le types des valeurs
	 * @return <code>true</code> si <i>set</i> contient au moins une valeur qui n'est pas dans <i>values</i>.
	 */
	private static <T extends Enum<T>> boolean containsSomethingElse(Class<T> clazz, @NotNull Set<T> set, @NotNull Set<T> values) {
		final Set<T> reliquat = EnumSet.noneOf(clazz);
		reliquat.addAll(set);
		reliquat.removeAll(values);
		return !reliquat.isEmpty();
	}

	/**
	 * @param clazz la classe des éléments des ensembles
	 * @param set1 un ensemble de valeurs
	 * @param set2 un autre ensemble de valeurs
	 * @param <T> le types des valeurs
	 * @return <code>true</code> si l'intersection des deux ensembles n'est pas vide
	 */
	private static <T extends Enum<T>> boolean intersect(Class<T> clazz, @NotNull Set<T> set1, @NotNull Set<T> set2) {
		final Set<T> intersection = EnumSet.noneOf(clazz);
		intersection.addAll(set1);
		intersection.retainAll(set2);
		return !intersection.isEmpty();
	}

	/**
	 * Migration des flags LIASF de l'entreprise de RegPM
	 * @param regpm l'entreprise dans RegPM
	 * @param unireg l'entreprise cible dans Unireg
	 * @param mr le collecteur de messages de suivi
	 */
	private void migrateLIASF(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les spécificités (flags)
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, buildEntrepriseKey(regpm)).isIncompatible()) {
			return;
		}

		final Set<Bouclement> bouclements = unireg.getBouclements();
		regpm.getCriteresSegmentation().stream()
				.filter(critere -> critere.getType() == RegpmTypeCritereSegmentation.LIASF)
				.sorted(Comparator.comparing(RegpmCritereSegmentation::getPfDebut))
				.map(critere -> {
					final FlagEntreprise flag = new FlagEntreprise();
					copyCreationMutation(critere, flag);

					final RegDate debut = Optional.ofNullable(migrationContexte.getBouclementService().getDateDernierBouclement(bouclements, RegDate.get(critere.getPfDebut(), 1, 1), true))
							.map(RegDate::getOneDayAfter)
							.orElse(RegDate.get(critere.getPfDebut(), 1, 1));
					final RegDate fin = Optional.<Integer>ofNullable(critere.getPfFin())
							.map(annee -> migrationContexte.getBouclementService().getDateDernierBouclement(bouclements, RegDate.get(annee, 12, 31), true))
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
	 * Migration des flags LIASF de l'entreprise de RegPM
	 * @param regpm l'entreprise dans RegPM
	 * @param unireg l'entreprise cible dans Unireg
	 * @param mr le collecteur de messages de suivi
	 */
	private void migrateFlagsDApresRegimesFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		final EntityKey key = buildEntrepriseKey(regpm);

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les spécificités (flags)
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, key).isIncompatible()) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Aucune spécificité fiscale migrée en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

		final RegimesFiscauxHistoData data = mr.getExtractedData(RegimesFiscauxHistoData.class, key);

		// construction d'une map qui, pour chaque type de flag, possède une liste des ranges de date
		final Map<TypeFlagEntreprise, List<DateRange>> map = Stream.concat(buildRanges(data.getCH()).stream().map(range -> range.withPayload(range.getPayload().getType())),
		                                                                               buildRanges(data.getVD()).stream().map(range -> range.withPayload(range.getPayload().getType())))
				.map(range -> range.withPayload(migrationContexte.getRegimeFiscalHelper().getTypeFlagEntreprise(range.getPayload())))
				.filter(range -> range.getPayload() != null)
				.collect(Collectors.toMap(DateRangeHelper.Ranged::getPayload,
				                          range -> Collections.singletonList(new DateRangeHelper.Range(range)),
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
				                          () -> new EnumMap<>(TypeFlagEntreprise.class)));

		if (!map.isEmpty()) {
			for (Map.Entry<TypeFlagEntreprise, List<DateRange>> entry : map.entrySet()) {
				// trions la liste en question avant de fusionner les périodes
				final List<DateRange> periodes = DateRangeHelper.merge(entry.getValue().stream()
						                                                       .sorted(DateRangeComparator::compareRanges)
						                                                       .collect(Collectors.toList()));
				if (periodes != null && !periodes.isEmpty()) {
					periodes.stream()
							.map(range -> new FlagEntreprise(entry.getKey(), range.getDateDebut(), range.getDateFin()))
							.peek(flag -> mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
							                            String.format("Génération d'un flag entreprise %s sur la période %s.",
							                                          flag.getType(),
							                                          StringRenderers.DATE_RANGE_RENDERER.toString(flag))))
							.forEach(unireg::addFlag);
				}
			}
		}
	}

	private static <T> List<DateRangeHelper.Ranged<T>> buildRanges(NavigableMap<RegDate, T> map) {
		final List<DateRangeHelper.Ranged<T>> ranges = new ArrayList<>(map.size());
		for (Map.Entry<RegDate, T> entry : map.entrySet()) {
			final RegDate dateFin = Optional.of(entry.getKey())
					.map(map::higherKey)
					.map(RegDate::getOneDayBefore)
					.orElse(null);
			final DateRangeHelper.Ranged<T> range = new DateRangeHelper.Ranged<>(entry.getKey(), dateFin, entry.getValue());
			ranges.add(range);
		}
		return ranges;
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
	 * @param rcent organisation dans RCEnt, si applicable
	 * @param unireg entreprise dans Unireg
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 */
	private void migrateAdresses(RegpmEntreprise regpm, @Nullable Organisation rcent, Entreprise unireg, MigrationResultContextManipulation mr, IdMapping idMapper) {

		// [SIFISC-17970] l'adresse "SIEGE" de RegPM doit être prise en adresse "POURSUITE" dans Unireg sur les période où RCEnt ne fournit pas d'adresse légale
		// [SIFISC-17970] l'adresse "COURRIER" de RegPM doit être migrée en adresse "COURRIER" dans Unireg sur les périodes où
		//      1. RCEnt ne fournit pas d'adresse effective, ou
		//      2. RCEnt fournit bien une adresse effective, mais celle-ci est différente
		// [SIFISC-18360] les surcharges "COURRIER" doivent également apparaître comme surcharge "REPRESENTATION" si l'entreprise est mandataire

		final Map<RegpmTypeAdresseEntreprise, RegpmAdresseEntreprise> adresses = regpm.getAdressesTypees();

		// construction d'un référentiel des adresses fiscales candidates à la reprise
		final Map<TypeAdresseTiers, AdresseSupplementaire> candidatsSurcharge = Stream.of(RegpmTypeAdresseEntreprise.COURRIER, RegpmTypeAdresseEntreprise.SIEGE)
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
					if (migrationContexte.getDateHelper().isFutureDate(a.getDateDebut())) {
						mr.addMessage(LogCategory.ADRESSES, LogLevel.ERROR,
						              String.format("Adresse %s ignorée car sa date de début de validité est dans le futur (%s).",
						                            a.getTypeAdresse(),
						                            StringRenderers.DATE_RENDERER.toString(a.getDateDebut())));
						return false;
					}
					return true;
				})
				.peek(a -> migrationContexte.getDateHelper().checkDateLouche(a.getDateDebut(),
				                                                             () -> String.format("La date de début de validité de l'adresse %s", a.getTypeAdresse()),
				                                                             LogCategory.ADRESSES,
				                                                             mr))
				.map(a -> {
					final String complement = a.getChez() == null ? regpm.getEnseigne() : a.getChez();
					final AdresseSupplementaire adresse = migrationContexte.getAdresseHelper().buildAdresseSupplementaire(a, mr, complement, false);
					if (adresse != null) {
						final TypeAdresseTiers type = a.getTypeAdresse() == RegpmTypeAdresseEntreprise.SIEGE ? TypeAdresseTiers.POURSUITE : TypeAdresseTiers.COURRIER;
						adresse.setUsage(type);
					}
					return adresse;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(AdresseTiers::getUsage,
				                          Function.identity(),
				                          (a1, a2) -> { throw new IllegalArgumentException("Erreur d'algorithme, il ne devrait jamais avoir plusieurs usages identiques ici..."); },
				                          () -> new EnumMap<>(TypeAdresseTiers.class)));

		// même chose côté civil
		final Map<TypeAdresseCivil, List<Adresse>> adressesCiviles;
		if (rcent == null || rcent.getAdresses().isEmpty()) {
			adressesCiviles = Collections.emptyMap();
		}
		else {
			adressesCiviles = rcent.getAdresses().stream()
					.collect(Collectors.toMap(Adresse::getTypeAdresse,
					                          Collections::singletonList,
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
					                          () -> new EnumMap<>(TypeAdresseCivil.class)));
		}

		// on regarde d'abord l'adresse de siège/poursuite (RegPM) vs. légale (RCEnt)
		final AdresseSupplementaire poursuite = candidatsSurcharge.get(TypeAdresseTiers.POURSUITE);
		if (poursuite != null) {

			// y a-t-il une adresse légale dans RCEnt ?
			final List<Adresse> adressesLegales = adressesCiviles.get(TypeAdresseCivil.PRINCIPALE);
			if (adressesLegales == null || adressesLegales.isEmpty()) {
				// non, aucune... -> il faut migrer l'adresse de poursuite sur toute sa plage de validité

				// un peu de log
				mr.addMessage(LogCategory.ADRESSES, LogLevel.INFO,
				              String.format("Adresse fiscale de siège migrée (en tant qu'adresse de poursuite) sur la période %s.",
				                            StringRenderers.DATE_RANGE_RENDERER.toString(poursuite)));

				unireg.addAdresseTiers(poursuite);
			}
			else {
				// on ne migre l'adresse poursuite de RegPM que jusqu'à la veille de la première date de validité de l'adresse RCEnt
				final RegDate dateFin = adressesLegales.stream()
						.map(Adresse::getDateDebut)
						.min(Comparator.naturalOrder())
						.map(RegDate::getOneDayBefore)
						.get();

				// on vérifie, par acquis de conscience, que l'adresse de RegPM commence bien avant la date de RCEnt
				// (sinon, on l'oublie, tout simplement)
				if (dateFin.isBefore(poursuite.getDateDebut())) {
					mr.addMessage(LogCategory.ADRESSES, LogLevel.WARN,
					              String.format("L'adresse de siège de RegPM sera ignorée car sa date de début de validité (%s) est postérieure ou égale à la date de début de l'adresse légale de RCEnt (%s).",
					                            StringRenderers.DATE_RENDERER.toString(poursuite.getDateDebut()),
					                            StringRenderers.DATE_RENDERER.toString(dateFin.getOneDayAfter())));
				}
				else {
					final AdresseTiers surcharge = poursuite.duplicate();
					surcharge.setDateFin(dateFin);

					// un peu de log
					mr.addMessage(LogCategory.ADRESSES, LogLevel.INFO,
					              String.format("Adresse fiscale de siège migrée (en tant qu'adresse de poursuite) sur la période %s.",
					                            StringRenderers.DATE_RANGE_RENDERER.toString(surcharge)));

					unireg.addAdresseTiers(surcharge);
				}
			}
		}

		// on regarde ensui les adresses courrier (RegPM) vs. effectives (RCEnt)
		final AdresseSupplementaire courrier = candidatsSurcharge.get(TypeAdresseTiers.COURRIER);
		if (courrier != null) {

			// [SIFISC-18360] on a besoin de savoir si l'entreprise est mandataire
			final boolean isMandataire = mr.getExtractedData(DonneesMandats.class, buildEntrepriseKey(regpm)).isMandataire();

			// y a-t-il une adresse effective dans RCEnt ?
			final List<Adresse> adressesEffectives = adressesCiviles.get(TypeAdresseCivil.COURRIER);
			if (adressesEffectives == null || !DateRangeHelper.intersect(courrier, adressesEffectives)) {
				// pas d'adresse effective qui intersecte avec l'adresse courrier fiscale, elle est donc migrée telle-qu'elle
				unireg.addAdresseTiers(courrier);
				assigneFlagPermanentSurAdresseCourrier(courrier, poursuite, regpm, unireg, mr, idMapper);

				// [SIFISC-18360] si l'entreprise est mandataire, on pose aussi une adresse de représentation
				if (isMandataire) {
					final AdresseSupplementaire representation = recopieCourrierEnRepresentation(courrier, mr);
					unireg.addAdresseTiers(representation);
				}
			}
			else {

				// une AdresseGenerique est une représentation "canonique" d'une adresse...
				final AdresseGenerique courrierGenerique = new AdresseSupplementaireAdapter(courrier, unireg, false, migrationContexte.getInfraService());

				// on ne va garder dans ces "adresses effectives" que celles que l'on veut garder en provenance du civil
				// (= celles qui ne doivent pas être surchargées, i.e. celles qui sont finalement identiques aux données de RegPM)
				final List<AdresseGenerique> aGarder = adressesEffectives.stream()
						.map(adresse -> {
							try {
								return new AdresseCivileAdapter(adresse, unireg, false, migrationContexte.getInfraService());
							}
							catch (DonneesCivilesException e) {
								LOGGER.error(String.format("Problème à la résolution de l'adresse effective RCEnt sur la période %s : %s.",
								                           StringRenderers.DATE_RANGE_RENDERER.toString(adresse),
								                           e.getMessage()),
								             e);
								mr.addMessage(LogCategory.ADRESSES, LogLevel.ERROR,
								              String.format("Impossible de résoudre l'adresse effective civile (%s).", e.getMessage()));
								return null;
							}
						})
						.filter(Objects::nonNull)
						.map(civile -> {
							// on découpe en : partie intersectante et partie juste civile
							// (afin que, à partir d'ici, il n'y ait pas d'intersection partielle)
							final Stream.Builder<AdresseGenerique> builder = Stream.builder();

							// partie intersectante
							final DateRange intersection = DateRangeHelper.intersection(civile, courrier);
							if (intersection != null) {
								builder.accept(new AdresseGeneriqueAdapter(civile, intersection.getDateDebut(), intersection.getDateFin(), civile.getSource(), false));
							}

							// partie juste civile
							final List<DateRange> justeCiviles = DateRangeHelper.subtract(civile, Collections.singletonList(courrier));
							if (justeCiviles != null) {
								justeCiviles.stream()
										.map(range -> new AdresseGeneriqueAdapter(civile, range.getDateDebut(), range.getDateFin(), civile.getSource(), false))
										.forEach(builder);
							}
							return builder.build();
						})
						.flatMap(Function.identity())
						.filter(adresse -> !DateRangeHelper.intersect(adresse, courrier) || isMemeDestination(adresse, courrierGenerique))
						.collect(Collectors.toList());

				// on peut la migrer telle qu'elle sur les périodes où il n'y a pas d'adresse civile
				final List<DateRange> absenceAdresseEffective = DateRangeHelper.subtract(courrier, aGarder);
				if (absenceAdresseEffective != null) {
					absenceAdresseEffective.stream()
							.map(range -> {
								final AdresseSupplementaire surcharge = (AdresseSupplementaire) courrier.duplicate();
								surcharge.setDateDebut(range.getDateDebut());
								surcharge.setDateFin(range.getDateFin());
								assigneFlagPermanentSurAdresseCourrier(surcharge, poursuite, regpm, unireg, mr, idMapper);
								return surcharge;
							})
							.forEach(surchargeCourrier -> {
								// surcharge courrier
								mr.addMessage(LogCategory.ADRESSES, LogLevel.INFO,
								              String.format("Adresse fiscale de courrier migrée sur la période %s.",
								                            StringRenderers.DATE_RANGE_RENDERER.toString(surchargeCourrier)));
								unireg.addAdresseTiers(surchargeCourrier);

								// [SIFISC-18360] sur une entreprise mandataire, on pose aussi une surcharge en "représentation"
								if (isMandataire) {
									final AdresseSupplementaire representation = recopieCourrierEnRepresentation(surchargeCourrier, mr);

									mr.addMessage(LogCategory.ADRESSES, LogLevel.INFO,
									              String.format("Adresse fiscale de représentation migrée sur la période %s.",
									                            StringRenderers.DATE_RANGE_RENDERER.toString(representation)));

									unireg.addAdresseTiers(representation);
								}
							});
				}
			}
		}
	}

	private AdresseSupplementaire recopieCourrierEnRepresentation(AdresseSupplementaire courrier, MigrationResultContextManipulation mr) {
		final AdresseSupplementaire representation = (AdresseSupplementaire) courrier.duplicate();
		representation.setUsage(TypeAdresseTiers.REPRESENTATION);

		if (representation.isPermanente()) {
			final AdresseGenerique generique = new AdresseSupplementaireAdapter(representation, courrier.getTiers(), false, migrationContexte.getInfraService());
			mr.pushContextValue(AdressePermanenteLoggedElement.class, new AdressePermanenteLoggedElement(generique, TypeAdresseTiers.REPRESENTATION));
			try {
				mr.addMessage(LogCategory.ADRESSES_PERMANENTES, LogLevel.INFO, StringUtils.EMPTY);
			}
			finally {
				mr.popContexteValue(AdressePermanenteLoggedElement.class);
			}
		}

		return representation;
	}

	/**
	 * Assigne le flag permanent sur l'adresse courrier passée en paramètre.<br/>
	 * [SIFISC-17970] gestion de la permanence : le traitement détermine au jour du traitement de reprise si l'entreprise est PM ou APM<br/>
	 * <ul>
	 *     <li>PM : si l'adresse courrier possède une date de validité différente de la date de validité de l'adresse siège et que l'adresse courrier doit être reprise comme adresse surchargée, alors elle est permanente</li>
	 *     <li>APM : si l'adresse courrier est différente de l'adresse siège et que l'adresse courrier doit être reprise comme adresse surchargée, alors elle est permanente</li>
	 * </ul>
	 * Toute adresse reprise comme permanente est listée dans une liste spécifique avec le n°CTB, la raison sociale, le complément d'adresse, la rue et n° police, le NPA , la localité postale et le pays</li>
	 * @param courrier l'adresse à flagger
	 * @param siege l'adresse de siège (pour information)
	 * @param regpm entreprise dans RegPM
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void assigneFlagPermanentSurAdresseCourrier(AdresseSupplementaire courrier, @Nullable AdresseSupplementaire siege, RegpmEntreprise regpm, Entreprise unireg, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final NavigableMap<RegDate, RegpmTypeFormeJuridique> formesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, buildEntityKey(regpm)).histo;
		if (formesJuridiques != null && !formesJuridiques.isEmpty()) {

			// on recherhe la forme juridique valable à la date du jour (ou la dernière si l'entreprise n'existe plus vraiment à la date du jour)
			final RegpmTypeFormeJuridique fj = Optional.of(RegDate.get())
					.map(formesJuridiques::floorEntry)
					.map(Map.Entry::getValue)
					.orElse(null);

			final AdresseSupplementaireAdapter courrierGenerique = new AdresseSupplementaireAdapter(courrier, unireg, false, migrationContexte.getInfraService());
			final AdresseSupplementaireAdapter siegeGenerique = siege != null ? new AdresseSupplementaireAdapter(siege, unireg, false, migrationContexte.getInfraService()) : null;

			// si pas de forme juridique (ou si c'est une société de personnes), pas de flag permanent
			final boolean permanente;
			if (fj != null) {

				// la règle dépend donc de la catégorie d'entreprise
				switch (fj.getCategorie()) {
				case PM:
					permanente = siege == null || courrier.getDateDebut() != siege.getDateDebut();
					break;
				case APM:
					permanente = siege == null || !isMemeDestination(courrierGenerique, siegeGenerique);
					break;
				case SP:
				default:
					permanente = false;
					break;
				}
			}
			else {
				permanente = false;
			}

			courrier.setPermanente(permanente);
			if (permanente) {
				// il faut sortir une liste...
				mr.pushContextValue(AdressePermanenteLoggedElement.class, new AdressePermanenteLoggedElement(courrierGenerique, TypeAdresseTiers.COURRIER));
				try {
					mr.addMessage(LogCategory.ADRESSES_PERMANENTES, LogLevel.INFO, StringUtils.EMPTY);
				}
				finally {
					mr.popContexteValue(AdressePermanenteLoggedElement.class);
				}
			}
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
		return !areEqual(one, two, equalator);
	}

	/**
	 * @param one une instance
	 * @param two une autre instance
	 * @param equalator un prédicat de comparaison entre deux instances
	 * @param <T> le type des instances comparées
	 * @return <code>true</code> si les deux instances sont équivalentes (une instance <code>null</code> est toujours différente d'une instance non-<code>null</code>)
	 */
	private static <T> boolean areEqual(@Nullable T one, @Nullable T two, BiPredicate<T, T> equalator) {
		return one == two || (one != null && two != null && equalator.test(one, two));
	}

	/**
	 * @param adresse1 une adresse civile
	 * @param adresse2 une adresse fiscale
	 * @return <code>true</code> si les deux adresses pointent vers le même endroit (les dates n'ont pas d'importance ici)
	 */
	private static boolean isMemeDestination(AdresseGenerique adresse1, AdresseGenerique adresse2) {

		// [SIFISC-18273] on ne compare pas les numéros postaux complémentaires qui ne sont de toute façon pas placés sur l'enveloppe...

		return areEqual(adresse1.getLocalite(), adresse2.getLocalite(), String::equalsIgnoreCase)
				&& areEqual(adresse1.getCasePostale(), adresse2.getCasePostale(), Object::equals)
				&& areEqual(adresse1.getComplement(), adresse2.getComplement(), String::equalsIgnoreCase)
				&& areEqual(adresse1.getLocaliteComplete(), adresse2.getLocaliteComplete(), String::equalsIgnoreCase)
				&& areEqual(adresse1.getNoOfsPays(), adresse2.getNoOfsPays(), Object::equals)
				&& areEqual(adresse1.getNumero(), adresse2.getNumero(), Object::equals)
				&& areEqual(adresse1.getNumeroOrdrePostal(), adresse2.getNumeroOrdrePostal(), Object::equals)
				&& areEqual(adresse1.getNumeroPostal(), adresse2.getNumeroPostal(), Object::equals)
				&& areEqual(adresse1.getRue(), adresse2.getRue(), String::equalsIgnoreCase);
	}

	/**
	 * Reconstitution des données civiles
	 * @param regpm entreprise à migrer
	 * @param rcent organisation connue de RCent
	 * @param unireg entreprise destination de la migration dans Unireg
	 * @param mr collecteur de messages de migration
	 */
	private static void migrateDonneesCiviles(RegpmEntreprise regpm, @Nullable Organisation rcent, Entreprise unireg, MigrationResultContextManipulation mr) {

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
			final Optional<FormeLegale> rcentFormeJuridique = Optional.ofNullable(OrganisationDataHelper.getLastValue(rcent.getFormeLegale()));
			final String rcentCodeFormeJuridique = rcentFormeJuridique.map(FormeLegale::getCode).orElse(null);
			final String rcentRaisonSociale = OrganisationDataHelper.getLastValue(rcent.getNom());
			final String rcentNumeroIde = OrganisationDataHelper.getLastValue(rcent.getNumeroIDE());
			final CommuneOuPays rcentSiege = rcent.getSiegesPrincipaux().stream()
					.max(Comparator.comparing(Domicile::getDateDebut))
					.map(domicile -> new CommuneOuPays(domicile.getNoOfs(), domicile.getTypeAutoriteFiscale()))
					.orElse(null);

			final Optional<String> regpmFormeJuridique = Optional.of(regpmFormesJuridiques)
					.filter(map -> !map.isEmpty())
					.map(NavigableMap::lastEntry)
					.map(Map.Entry::getValue)
					.map(RegpmTypeFormeJuridique::getCode);
			final String regpmCodeFormeJuridique = regpmFormeJuridique.map(EntrepriseMigrator::toFormeJuridique).map(FormeJuridiqueEntreprise::getCodeECH).orElse(null);
			final String regpmRaisonSociale = regpmRaisonsSociales.lastEntry().getValue();
			final String regpmNumeroIde = Optional.ofNullable(regpm.getNumeroIDE()).map(StringRenderers.NUMERO_IDE_CANONICAL_RENDERER::toString).orElse(null);
			final CommuneOuPays regpmSiege = Optional.of(mr.getExtractedData(SiegesHistoData.class, key).histo)
					.map(NavigableMap::lastEntry)
					.map(Map.Entry::getValue)
					.map(siege -> new CommuneOuPays(siege::getCommune, siege::getNoOfsPays))
					.orElse(null);

			final boolean differenceFormeJuridique = areDifferent(rcentCodeFormeJuridique, regpmCodeFormeJuridique, Objects::equals);
			final boolean differenceRaisonSociale = areDifferent(rcentRaisonSociale, regpmRaisonSociale, Objects::equals);
			final boolean differenceNumeroIde = areDifferent(rcentNumeroIde, regpmNumeroIde, Objects::equals);
			final boolean differenceSiege = areDifferent(rcentSiege, regpmSiege, Objects::equals);

			if (differenceFormeJuridique || differenceNumeroIde || differenceRaisonSociale || differenceSiege) {
				mr.pushContextValue(DifferencesDonneesCivilesLoggedElement.class, new DifferencesDonneesCivilesLoggedElement(regpmRaisonSociale, rcentRaisonSociale, differenceRaisonSociale,
				                                                                                                             regpmFormeJuridique.orElse(null), rcentFormeJuridique.orElse(null), differenceFormeJuridique,
				                                                                                                             regpmNumeroIde, rcentNumeroIde, differenceNumeroIde,
				                                                                                                             regpmSiege, rcentSiege, differenceSiege));
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

			// d'abord les données fiscales de raison sociale (sans date de fin dans un premier temps)
			final List<RaisonSocialeFiscaleEntreprise> rss = regpmRaisonsSociales.entrySet().stream()
					.filter(entry -> entry.getValue() != null)
					.map(entry -> entry.getKey() != null ? entry : Pair.of(dateDebutEffective, entry.getValue()))
					.filter(entry -> RegDateHelper.isBeforeOrEqual(entry.getKey(), dateFinHistoireFiscale, NullDateBehavior.LATEST))
					.map(entry -> new RaisonSocialeFiscaleEntreprise(entry.getKey(), null, entry.getValue()))
					.collect(Collectors.toList());

			// assignation des dates de fin sur les raisons sociales
			assigneDatesFin(dateFinHistoireFiscale, rss);

			// persistence et log
			rss.stream()
					.peek(rs -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
					                          String.format("Donnée de raison sociale migrée : sur la période %s, '%s'.",
					                                        StringRenderers.DATE_RANGE_RENDERER.toString(rs),
					                                        rs.getRaisonSociale())))
					.forEach(unireg::addDonneeCivile);

			// ensuite, les données de forme juridique (sand date de fin dans un premier temps)
			final List<FormeJuridiqueFiscaleEntreprise> fjs = regpmFormesJuridiques.entrySet().stream()
					.filter(entry -> entry.getValue() != null)
					.map(entry -> entry.getKey() != null ? entry : Pair.of(dateDebutEffective, entry.getValue()))
					.filter(entry -> RegDateHelper.isBeforeOrEqual(entry.getKey(), dateFinHistoireFiscale, NullDateBehavior.LATEST))
					.map(entry -> new FormeJuridiqueFiscaleEntreprise(entry.getKey(), null, toFormeJuridique(entry.getValue().getCode())))
					.collect(Collectors.toList());

			// assignation des dates de fin sur les formes juridiques
			assigneDatesFin(dateFinHistoireFiscale, fjs);

			// persistence et log
			fjs.stream()
					.peek(fj -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
					                          String.format("Donnée de forme juridique migrée : sur la période %s, %s.",
					                                        StringRenderers.DATE_RANGE_RENDERER.toString(fj),
					                                        fj.getFormeJuridique())))
					.forEach(unireg::addDonneeCivile);
		}

		// on arrête également les capitaux "fiscaux" à la première date de données connues par RCEnt (= même si aucun capital n'est connu
		// à cette date dans RCEnt)... Comme le nom est une donnée obligatoire, on peut prendre la date de début de la première valeur de nom
		// comme première date de données dans RCEnt

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
			dateFinActiviteCapitaux = dateFinHistoireFiscale;
			if (dateFinActiviteCapitaux != null) {
				mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
				              String.format("Les données de capital en provenance du registre civil font foi dès le %s (les données ultérieures de RegPM seront ignorées).",
				                            StringRenderers.DATE_RENDERER.toString(dateFinActiviteCapitaux.getOneDayAfter())));
			}
		}

		// si toutes les données de capital connues fiscalement sont en fait dans la période de validité des données connues dans RCEnt, on ne migre rien
		if (!regpmCapitaux.isEmpty() && RegDateHelper.isBeforeOrEqual(regpmCapitaux.firstKey(), dateFinActiviteCapitaux, NullDateBehavior.LATEST)) {

			// génération des données de capital (d'abord sans dates de fin...)
			final List<CapitalFiscalEntreprise> capitaux = regpmCapitaux.entrySet().stream()
					.filter(entry -> entry.getValue() != null)
					.filter(entry -> RegDateHelper.isBeforeOrEqual(entry.getKey(), dateFinActiviteCapitaux, NullDateBehavior.LATEST))
					.map(entry -> new CapitalFiscalEntreprise(entry.getKey(), null, new MontantMonetaire(entry.getValue().longValue(), MontantMonetaire.CHF)))
					.collect(Collectors.toList());

			// assignation des dates de fin en fonction des dates de début du suivant
			assigneDatesFin(dateFinActiviteCapitaux, capitaux);

			// persistence et log
			capitaux.stream()
					.peek(capital -> mr.addMessage(LogCategory.DONNEES_CIVILES_REGPM, LogLevel.INFO,
					                               String.format("Donnée de capital migrée : sur la période %s, %s.",
					                                             StringRenderers.DATE_RANGE_RENDERER.toString(capital),
					                                             StringRenderers.MONTANT_MONETAIRE_RENDERER.toString(capital.getMontant()))))
					.forEach(unireg::addDonneeCivile);
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
		if (migrationContexte.getDoublonProvider().isDoublon(regpm)) {
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
			final int noOfsPaysCorrigeGibraltar = cop.getNumeroOfsAutoriteFiscale() == 8997 ? 8213 : cop.getNumeroOfsAutoriteFiscale();
			final Pays pays = migrationContexte.getInfraService().getPays(noOfsPaysCorrigeGibraltar, dateReference);
			if (pays != null && !pays.isEtatSouverain()) {
				mr.addMessage(logCategory, LogLevel.WARN,
				              String.format("Le pays %d%s n'est pas un état souverain, remplacé par l'état %d.",
				                            cop.getNumeroOfsAutoriteFiscale(),
				                            StringUtils.isBlank(contexte) ? StringUtils.EMPTY : String.format(" (%s)", contexte),
				                            pays.getNoOfsEtatSouverain()));
				noOfsPays = pays.getNoOfsEtatSouverain();
			}
			else {
				noOfsPays = noOfsPaysCorrigeGibraltar;
			}

			if (noOfsPays != cop.getNumeroOfsAutoriteFiscale()) {
				return new CommuneOuPays(noOfsPays, TypeAutoriteFiscale.PAYS_HS);
			}
		}
		return cop;
	}

	/**
	 * Génération des établissements principaux liés à l'entreprise (autant que d'entrées différentes dans la map), le premier d'entre eux devant être repris
	 * pour porter les éventuelles données fiscales antérieures au début des données civiles
	 * @param regpm l'entreprise de RegPM
	 * @param validiteSitesPrincipaux map des identifiants et des périodes de 'principalité' des sites principaux civils
	 * @param linkCollector le collecteur de liens à créer entre les entités
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param mr collecteur de messages de migration
	 * @return le tout premier établissement (historiquement) principal connu dans le registre civil (= celui auquel on rattachera l'historique issu de RegPM)
	 */
	private Etablissement generateEtablissementPrincipalSelonDonneesCiviles(RegpmEntreprise regpm, Map<Long, List<DateRanged<SiteOrganisation>>> validiteSitesPrincipaux, EntityLinkCollector linkCollector,
	                                                               IdMapping idMapper, MigrationResultProduction mr) {
		final KeyedSupplier<Entreprise> entrepriseSupplier = getEntrepriseSupplier(idMapper, regpm);

		// identifiant cantonal du tout premier établissement principal connu
		// (c'est à celui-là que l'on fera porter les données antérieures issues des données de RegPM)
		// avec la date de début associée
		final Long idCantonalPremierEtablissement = validiteSitesPrincipaux.entrySet().stream()
				.map(entry -> entry.getValue().stream().map(range -> Pair.of(entry.getKey(), range.getDateDebut())))
				.flatMap(Function.identity())
				.min(Comparator.comparing(Pair::getRight))
				.map(Pair::getLeft)
				.get();
		final Mutable<Etablissement> premierEtablissement = new MutableObject<>();

		for (Map.Entry<Long, List<DateRanged<SiteOrganisation>>> entry : validiteSitesPrincipaux.entrySet()) {

			// création d'un nouvel établissement associé au numéro cantonal donné
			final Etablissement etbPrincipal = migrationContexte.getUniregStore().saveEntityToDb(new Etablissement());
			etbPrincipal.setNumeroEtablissement(entry.getKey());        // lien vers le civil

			// [SIFISC-17744] on annule aussi l'établissement principal
			if (migrationContexte.getDoublonProvider().isDoublon(regpm)) {
				etbPrincipal.setAnnule(true);
			}

			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
			              String.format("Etablissement principal %s%s créé en liaison avec le site civil %d.",
			                            FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
			                            etbPrincipal.isAnnule() ? " (annulé)" : StringUtils.EMPTY,
			                            entry.getKey()));

			// on récupère le premier établissement
			if (entry.getKey().equals(idCantonalPremierEtablissement)) {
				premierEtablissement.setValue(etbPrincipal);
			}
			else {
				// on ne génère les liens que sur les éventuels établissements additionnels
				// (le premier établissement historique a potentiellement des données fiscales, et donc les liens seront gérés ailleurs)

				// ici, on ne crée pas de domiciles pour les établissements... comme ils sont connus du civils, les sièges seront à aller chercher par là-bas aussi...

				// demande de création de liens d'activité économique
				addLinksAciviteEconomiquePrincipale(entrepriseSupplier, etbPrincipal, entry.getValue(), linkCollector);
			}
		}

		return premierEtablissement.getValue();
	}

	/**
	 * Ajoute les liens d'activité économique principale (= entre une entreprise et un établissement principal) selon les ranges donnés
	 * @param entrepriseSupplier supplier de l'entreprise
	 * @param etablissement établissement principal
	 * @param ranges ranges de dates dans lesquels établir les liens
	 * @param linkCollector collecteur de liens
	 */
	private void addLinksAciviteEconomiquePrincipale(KeyedSupplier<Entreprise> entrepriseSupplier,
	                                                 Etablissement etablissement,
	                                                 List<? extends DateRange> ranges,
	                                                 EntityLinkCollector linkCollector) {
		final Supplier<Etablissement> etbPrincipalSupplier = getEtablissementByUniregIdSupplier(etablissement.getNumero());
		ranges.stream()
				.map(range -> new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(etbPrincipalSupplier, entrepriseSupplier, range.getDateDebut(), range.getDateFin(), true))
				.forEach(linkCollector::addLink);
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
		final KeyedSupplier<Entreprise> entrepriseSupplier = getEntrepriseSupplier(idMapper, regpm);

		// si une connexion avec RCEnt existe, on va essayer de lier l'établissement principal généré avec son pendant civil

		// date de fin des données fiscales (= veille de la date d'apparition civile des données, ou <code>null</code> s'il n'y a pas de données civiles)
		final RegDate dateFinEtablissementFiscal;
		final Pair<Etablissement, List<? extends DateRange>> premierEtablissementLieAvecCivil;
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
				final Etablissement premierEtb = generateEtablissementPrincipalSelonDonneesCiviles(regpm, validitesSites, linkCollector, idMapper, mr);
				premierEtablissementLieAvecCivil = Pair.of(premierEtb, validitesSites.get(premierEtb.getNumeroEtablissement()));
			}
			else {
				dateFinEtablissementFiscal = null;
				premierEtablissementLieAvecCivil = null;
			}
		}
		else {
			dateFinEtablissementFiscal = null;
			premierEtablissementLieAvecCivil = null;
		}

		// pas de donnée de siège -> on a fini (s'il y a des données civiles, tant mieux, sinon... rien)
		if (sieges.isEmpty()) {
			if (dateFinEtablissementFiscal == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.");
			}
			if (premierEtablissementLieAvecCivil != null) {
				// ne pas oublier de générer les liens pour le premier établissement principal généré depuis les données civiles
				addLinksAciviteEconomiquePrincipale(entrepriseSupplier, premierEtablissementLieAvecCivil.getLeft(), premierEtablissementLieAvecCivil.getRight(), linkCollector);
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

			final Etablissement etbPrincipal;
			if (premierEtablissementLieAvecCivil != null) {
				etbPrincipal = premierEtablissementLieAvecCivil.getLeft();

				// un peu de log pour indiquer la ré-utilisation de l'établissement créé pour les données civiles
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Ré-utilisation de l'établissement principal %s identifié par son numéro cantonal.",
				                                                              FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
			}
			else {
				etbPrincipal = migrationContexte.getUniregStore().saveEntityToDb(new Etablissement());
				etbPrincipal.setEnseigne(regpm.getEnseigne());
				etbPrincipal.setRaisonSociale(raisonSociale);

				// [SIFISC-17744] on annule aussi l'établissement principal
				if (migrationContexte.getDoublonProvider().isDoublon(regpm)) {
					etbPrincipal.setAnnule(true);
				}

				// un peu de log pour indiquer la création de l'établissement principal
				mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Création de l'établissement principal %s%s.",
				                                                              FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero()),
				                                                              etbPrincipal.isAnnule() ? " (annulé)" : StringUtils.EMPTY));
			}

			// lien entre l'établissement principal et son entreprise
			final DateRange rangeLien = new DateRangeHelper.Range(sieges.firstKey(), dateFinDonneesFiscales);

			// attention, s'il y avait des données civiles, il faut fusionner les deux...
			// [SIFISC-18108] si l'entreprise avait arrêté son activité avant la date de prise en charge de RCEnt, on ne prend pas en compte le lien donné par RCEnt
			// (qui n'est alors qu'une photo souvenir...)
			if (premierEtablissementLieAvecCivil != null && RegDateHelper.isAfter(dateFinActivite, dateFinEtablissementFiscal, NullDateBehavior.LATEST)) {
				final List<DateRange> ranges = DateRangeHelper.merge(Stream.concat(Stream.of(rangeLien), premierEtablissementLieAvecCivil.getRight().stream())
						                                                     .sorted(Comparator.comparing(DateRange::getDateDebut))
						                                                     .collect(Collectors.toList()));

				addLinksAciviteEconomiquePrincipale(entrepriseSupplier, etbPrincipal, ranges, linkCollector);
			}
			else {
				// [SIFISC-18108] un peu de log pour les entreprises liées à RCEnt mais fermées fiscalement bien avant
				if (premierEtablissementLieAvecCivil != null && !premierEtablissementLieAvecCivil.getRight().isEmpty()) {
					mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
					              String.format("Entreprise clôturée fiscalement avant l'avènement des données RCEnt, pas de lien vers l'établissement principal généré après le %s.",
					                            StringRenderers.DATE_RENDERER.toString(dateFinEtablissementFiscal)));
				}
				addLinksAciviteEconomiquePrincipale(entrepriseSupplier, etbPrincipal, Collections.singletonList(rangeLien), linkCollector);
			}

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
		else if (premierEtablissementLieAvecCivil != null) {
			// ne pas oublier de générer les liens vers l'établissement identifié civilement
			addLinksAciviteEconomiquePrincipale(entrepriseSupplier, premierEtablissementLieAvecCivil.getLeft(), premierEtablissementLieAvecCivil.getRight(), linkCollector);
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
	 * Migration des sociétés de direction des fonds de placement
	 * @param regpm la société (dans le rôle du fonds de placement)
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateSocietesDeDirection(RegpmEntreprise regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final KeyedSupplier<Entreprise> moi = getEntrepriseSupplier(idMapper, regpm);

		// on ne génère quelque chose que pour les entreprises qui sont des fonds de placement
		final NavigableMap<RegDate, RegpmTypeFormeJuridique> formesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, moi.getKey()).histo;
		final boolean isFondsPlacement = formesJuridiques.values().stream()
				.map(RegpmTypeFormeJuridique::getCode)
				.map(EntrepriseMigrator::toFormeJuridique)
				.anyMatch(fj -> fj == FormeJuridiqueEntreprise.SCPC);

		final SortedSet<RegpmSocieteDirection> directions = regpm.getDirections();
		if (isFondsPlacement) {

			final RegDate finActiviteFonds = mr.getExtractedData(DateFinActiviteData.class, moi.getKey()).date;

			// filtrage et génération des ranges qui vont bien (sans date de fin pour le moment...)
			final List<DateRangeHelper.Ranged<RegpmEntreprise>> ranges = directions.stream ()
					.filter(direction -> {
						if (direction.getDateValidite() == null) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
							              String.format("Direction de société %d ignorée en raison de sa date de début nulle (ou antérieure au 01.08.1291).", direction.getId().getSeqNo()));
							return false;
						}
						return true;
					})
					.filter(direction -> {
						if (migrationContexte.getDateHelper().isFutureDate(direction.getDateValidite())) {
							mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
							              String.format("Direction de société %d ignorée en raison de sa date de début dans le futur (%s).",
							                            direction.getId().getSeqNo(),
							                            StringRenderers.DATE_RENDERER.toString(direction.getDateValidite())));
							return false;
						}
						return true;
					})
					.peek(direction -> migrationContexte.getDateHelper().checkDateLouche(direction.getDateValidite(),
					                                                                     () -> String.format("La date début d'activité de la direction de société %d", direction.getId().getSeqNo()),
					                                                                     LogCategory.SUIVI,
					                                                                     mr))
					.sorted(Comparator.comparing(RegpmSocieteDirection::getDateValidite))
					.map(direction -> new DateRangeHelper.Ranged<>(direction.getDateValidite(), null, direction.getDirection()))
					.collect(Collectors.toList());

			// attribution des dates de fin
			assigneDatesFinSurRanged(finActiviteFonds, ranges);

			// génération des liens
			ranges.stream()
					.map(range -> range.withPayload(getEntrepriseSupplier(idMapper, range.getPayload())))
					.map(range -> new EntityLinkCollector.ProprietaireFondsPlacementLink(range.getPayload(), moi, range.getDateDebut(), range.getDateFin()))
					.forEach(linkCollector::addLink);
		}
		else if (!directions.isEmpty()) {
			// un petit log pour noter l'abandon de ces liens
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Lien(s) de société de direction sur une entreprise qui n'est pas un fonds de placement, ignoré(s) dans la migration.");
		}
	}

	/**
	 * Migration des liens vers les administrateurs actifs des sociétés immobilières
	 * @param regpm la société (dans le rôle de la société immobilière)
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateAdministrateursSocieteImmobiliere(RegpmEntreprise regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {

		final KeyedSupplier<Entreprise> moi = getEntrepriseSupplier(idMapper, regpm);
		final DonneesAdministrateurs donneesAdministrateurs = mr.getExtractedData(DonneesAdministrateurs.class, moi.getKey());
		final Map<Long, List<RegpmAdministrateur>> parAdministrateur = donneesAdministrateurs.getAdministrationsParAdministrateur();
		if (!parAdministrateur.isEmpty()) {
			// prenons les administrateurs un par un
			for (Map.Entry<Long, List<RegpmAdministrateur>> entry : parAdministrateur.entrySet()) {

				final List<RegpmAdministrateur> roles = entry.getValue();
				final boolean hasPresident = roles.stream().anyMatch(adm -> adm.getFonction() == RegpmFonction.PRESIDENT);
				// nous n'avons que des actifs (= sans date de fin)
				// au pire, on pourrait même en avoir plusieurs, prenons donc la première date...
				final RegDate minDate = roles.stream()
						.filter(adm -> adm.getFonction() == RegpmFonction.ADMINISTRATEUR)
						.map(RegpmAdministrateur::getDateEntreeFonction)
						.min(Comparator.naturalOrder())
						.get();

				// construction du supplier pour la personne physique administratrice
				final KeyedSupplier<PersonnePhysique> admin = getIndividuSupplier(idMapper, entry.getValue().get(0).getAdministrateur());

				// demande d'ajout de lien
				linkCollector.addLink(new EntityLinkCollector.EntrepriseAdministrateurLink(moi, admin, minDate, null, hasPresident));
			}
		}
	}

	/**
	 * Migration d'un mandat à l'aide d'un lien explicite entre tiers
	 * @param mandant l'entreprise mandante
	 * @param mandat le mandat à migrer
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateMandatAvecLien(RegpmEntreprise mandant, RegpmMandat mandat, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {

		// récupération du mandataire qui peut être une autre entreprise, un établissement ou un individu
		final KeyedSupplier<? extends Contribuable> mandataire = getPolymorphicSupplier(idMapper, mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
		if (mandataire == null) {
			// cas normalement impossible, puisque le cas a dû être écarté déjà dans l'extracteur
			throw new IllegalArgumentException("On ne devrait pas se trouver là...");
		}

		// [SIFISC-17979] on ne migre la coordonnée financière que sur les mandats "tiers"
		final String bicSwift;
		String iban;
		if (mandat.getType() == RegpmTypeMandat.TIERS) {
			bicSwift = mandat.getBicSwift();
			try {
				iban = IbanExtractor.extractIban(mandat, mr);

				// TODO ne manque-t-il pas le titulaire du compte pour les coordonnées financières ?
			}
			catch (IbanExtractor.IbanExtractorException e) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Impossible d'extraire un IBAN du mandat " + mandat.getId() + " (" + e.getMessage() + ")");
				iban = null;
			}
		}
		else {
			iban = null;
			bicSwift = null;
		}

		// [SIFISC-17979] on ne migre les coordonnées de contact que sur les mandats "généraux"
		final String nomContact;
		final String prenomContact;
		final String telContact;
		if (mandat.getType() == RegpmTypeMandat.GENERAL) {
			nomContact = mandat.getNomContact();
			prenomContact = mandat.getPrenomContact();
			telContact = canonizeTelephoneNumber(mandat.getNoTelContact(), String.format("téléphone du mandat général %d", mandat.getId().getNoSequence()), mr);
		}
		else {
			nomContact = null;
			prenomContact = null;
			telContact = null;
		}

		// un supplier qui va renvoyer l'entreprise en cours de migration
		final KeyedSupplier<Entreprise> moi = getEntrepriseSupplier(idMapper, mandant);

		// ajout du lien entre l'entreprise et son mandataire
		linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi,
		                                                                      mandataire,
		                                                                      mandat.getDateAttribution(),
		                                                                      mandat.getDateResiliation(),
		                                                                      extractTypeMandat(mandat),
		                                                                      iban,
		                                                                      bicSwift,
		                                                                      nomContact,
		                                                                      prenomContact,
		                                                                      telContact));
	}

	/**
	 * Migration des mandataires d'une entreprise
	 * @param regpm entreprise à migrer
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateMandataires(RegpmEntreprise regpm, Entreprise unireg, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final KeyedSupplier<Entreprise> moi = getEntrepriseSupplier(idMapper, regpm);

		// on va chercher les mandats (rôle mandant -> les mandataires) repris
		final Collection<RegpmMandat> mandataires = mr.getExtractedData(DonneesMandats.class, moi.getKey()).getRolesMandant();

		// migration des mandataires -> liens à créer par la suite
		mandataires.stream()
				.forEach(mandat -> migrateMandatAvecLien(regpm, mandat, mr, linkCollector, idMapper));
	}

	private static TypeMandat extractTypeMandat(RegpmMandat mandat) {
		return mapTypeMandat(mandat.getType());
	}

	private static TypeMandat mapTypeMandat(RegpmTypeMandat type) {
		switch (type) {
		case GENERAL:
			return TypeMandat.GENERAL;
		case TIERS:
			return TypeMandat.TIERS;
		default:
			// TODO si on doit un jour migrer d'autres types, ça va sauter (doit être cohérent avec l'extraction des mandats)
			throw new IllegalArgumentException("Type de mandat non-supporté : " + type);
		}
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

					// [SIFISC-17378] si la dernière DI est retournée, la date de bouclement future a été placée en connaissance de cause
					// (donc il ne doit pas y avoir de bouclement entre le lendemain de la date de fin de la PI de la DI et la date de bouclement futur)
					if (range.getDateFin() == dateBouclementFutur && !exercicesCommerciaux.isEmpty()) {
						final RegpmExerciceCommercial dernierExerciceConnu = exercicesCommerciaux.get(exercicesCommerciaux.size() - 1);
						if (dernierExerciceConnu.getDateFin().getOneDayAfter() == range.getDateDebut()) {
							// le dernier exercice commercial connu est avant le trou, vérifions maintenant
							// que cet exercice n'est pas annulé et bien retourné
							if (dernierExerciceConnu.getDossierFiscal() != null
									&& dernierExerciceConnu.getDossierFiscal().getEtat() != RegpmTypeEtatDossierFiscal.ANNULE
									&& dernierExerciceConnu.getDossierFiscal().getDateRetour() != null) {

								// vérifions ensuite que le dernier dossier fiscal connu correspond à celui que nous venons
								// de trouver (en gros, vérifions qu'il n'y a pas eu de nouvelle émission de DI ensuite...)
								final List<RegpmDossierFiscal> dossiersFiscaux = mr.getExtractedData(DossiersFiscauxData.class, buildEntrepriseKey(regpm)).liste;
								final RegpmDossierFiscal dernierDossierFiscal = dossiersFiscaux.stream()
										.filter(df -> df.getEtat() != RegpmTypeEtatDossierFiscal.ANNULE)
										.max(Comparator.comparing(RegpmDossierFiscal::getPf).thenComparing(RegpmDossierFiscal::getNoParAnnee))
										.orElse(null);

								// ce sont bien les mêmes, donc on arrête là (= on considère que la date de bouclement
								// futur est bien le prochain bouclement, car elle a été mise à jour lors du retour de la
								// dernière déclaration)
								if (dernierDossierFiscal == dernierExerciceConnu.getDossierFiscal()) {

									// comme sécurité finale, si le trou fait 24 mois ou plus, on ajoute quand-même des bouclements intermédiaires
									if (range.getDateDebut().addYears(2).getOneDayBefore().isAfter(range.getDateFin())) {

										// un petit log, ça ne fait pas de mal
										mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
										              String.format("Période %s sans exercice commercial dans RegPM reprise comme un seul exercice commercial.",
										                            StringRenderers.DATE_RANGE_RENDERER.toString(range)));

										continue;
									}
								}
							}
						}
					}

					// [SIFISC-17691] si le trou est dès le début de la lorgnette (= en début d'activité) et que sa date de début
					// est dans le second semestre de l'année, alors le premier bouclement doit être placé à la fin de l'année suivante !
					boolean nePasAjouterBouclementDansAnneeDebutTrou = false;
					if (range.getDateDebut() == dateDebutLorgnette
							&& DayMonth.get(range.getDateFin()) == DayMonth.get(12, 31)
							&& dateDebutLorgnette.month() > 6) {

						nePasAjouterBouclementDansAnneeDebutTrou = true;
					}

					// on sait que la fin du range est forcément une date de bouclement (puisqu'un exercice commercial débute au lendemain)
					// et ensuite on case autant d'années que nécessaire pour boucher le trou
					for (RegDate bouclement = range.getDateFin();
					     bouclement.isAfterOrEqual(range.getDateDebut()) && (!nePasAjouterBouclementDansAnneeDebutTrou || bouclement.year() > range.getDateDebut().year());
					     bouclement = bouclement.addYears(-1)) {

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
		final List<Bouclement> bouclements = migrationContexte.getBouclementService().extractBouclementsDepuisDates(datesBouclements, 12);
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
		final List<PeriodeFiscale> pfs = neverNull(migrationContexte.getUniregStore().getEntitiesFromDb(PeriodeFiscale.class, params, true));
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

	private DeclarationImpotOrdinairePM migrateDeclaration(RegpmDossierFiscal dossier, RegDate dateDebut, RegDate dateFin, MigrationResultProduction mr) {
		final PeriodeFiscale pf = getPeriodeFiscaleByYear(dossier.getPf());

		final DeclarationImpotOrdinairePM di = new DeclarationImpotOrdinairePM();
		copyCreationMutation(dossier, di);
		di.setDateDebut(dateDebut);
		di.setDateFin(dateFin);
		di.setDateDebutExerciceCommercial(dateDebut);
		di.setDateFinExerciceCommercial(dateFin);
		di.setDelais(migrateDelaisDeclaration(dossier, di, mr));
		di.setEtats(migrateEtatsDeclaration(dossier, mr));
		di.setNumero(dossier.getNoParAnnee());
		di.setPeriode(pf);

		if (dossier.getEtat() == RegpmTypeEtatDossierFiscal.ANNULE) {
			di.setAnnulationUser(Optional.ofNullable(dossier.getLastMutationOperator()).orElse(AuthenticationHelper.getCurrentPrincipal()));
			di.setAnnulationDate(Optional.ofNullable((Date) dossier.getLastMutationTimestamp()).orElseGet(ch.vd.registre.base.date.DateHelper::getCurrentDate));
		}
		return di;
	}

	/**
	 * Migration d'un questionnaire SNC de RegPM
	 */
	private QuestionnaireSNC migrateQuestionnaireSNC(RegpmQuestionnaireSNC regpm, MigrationResultProduction mr) {
		final int anneeFiscale = regpm.getAnneeFiscale();
		final PeriodeFiscale pf = getPeriodeFiscaleByYear(anneeFiscale);
		final QuestionnaireSNC unireg = new QuestionnaireSNC();
		copyCreationMutation(regpm, unireg);
		unireg.setDateDebut(RegDate.get(anneeFiscale, 1, 1));
		unireg.setDateFin(RegDate.get(anneeFiscale, 12, 31));
		unireg.setDelais(migrateDelaisQuestionnaireSNC(regpm, mr));
		unireg.setEtats(migrateEtatsQuestionnaireSNC(regpm, mr));
		unireg.setPeriode(pf);
		return unireg;
	}

	/**
	 * Migration des états d'un questionnaire SNC
	 */
	private static Set<EtatDeclaration> migrateEtatsQuestionnaireSNC(RegpmQuestionnaireSNC regpm, MigrationResultProduction mr) {

		final Set<EtatDeclaration> etats = new LinkedHashSet<>();

		// envoi
		if (regpm.getDateEnvoi() != null) {
			etats.add(new EtatDeclarationEmise(regpm.getDateEnvoi()));
		}

		// rappel
		if (regpm.getDateRappel() != null) {
			etats.add(new EtatDeclarationRappelee(regpm.getDateRappel(), regpm.getDateRappel()));
		}

		// retour
		if (regpm.getDateRetour() != null) {
			etats.add(new EtatDeclarationRetournee(regpm.getDateRetour(), MigrationConstants.SOURCE_RETOUR_QSNC_MIGRE));
		}

		// un peu de traçabilité sur le travail accompli ici
		etats.forEach(etat -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
		                                    String.format("Etat '%s' migré au %s.",
		                                                  etat.getEtat(),
		                                                  StringRenderers.DATE_RENDERER.toString(etat.getDateObtention()))));

		return etats;
	}

	/**
	 * Migration du délai initial d'un questionnaire SNC
	 */
	private static Set<DelaiDeclaration> migrateDelaisQuestionnaireSNC(RegpmQuestionnaireSNC regpm, MigrationResultProduction mr) {
		// un seul délai -> le délai initial, basé sur le champ délai retour du questionnaire source
		final DelaiDeclaration delaiInitial = new DelaiDeclaration();
		delaiInitial.setDateDemande(regpm.getDateEnvoi());
		delaiInitial.setDateTraitement(regpm.getDateEnvoi());
		delaiInitial.setDelaiAccordeAu(regpm.getDelaiRetour());
		delaiInitial.setEtat(EtatDelaiDeclaration.ACCORDE);

		// un peu de log
		mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
		              String.format("Délai initial fixé au %s.", StringRenderers.DATE_RENDERER.toString(regpm.getDelaiRetour())));

		return Collections.singleton(delaiInitial);
	}

	/**
	 * Migration des questionnaires SNC avec leurs délais, états...
	 * @param regpm entreprise dans RegPM
	 * @param unireg entreprise dans Unireg
	 * @param mr collecteur des messages de suivi
	 */
	private void migrateQuestionnairesSNC(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les déclarations
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, buildEntrepriseKey(regpm)).isIncompatible()) {
			mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN, "Aucun questionnaire SNC migré en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

		regpm.getQuestionnairesSNC().stream()
				.filter(q -> q.getEtat() != RegpmTypeEtatQuestionnaireSNC.ANNULE)       // on ne migre pas les questionnaires annulés
				.filter(q -> {
					if (q.getAnneeFiscale() < MigrationConstants.PREMIERE_PF) {
						mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN,
						              String.format("Questionnaire SNC non-annulé ignoré car sa période fiscale est avant %d.", MigrationConstants.PREMIERE_PF));
						return false;
					}
					return true;
				})
				.peek(q -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
				                         String.format("Génération d'un questionnaire SNC sur la période fiscale %d.", q.getAnneeFiscale())))
				.map(q -> migrateQuestionnaireSNC(q, mr))
				.forEach(unireg::addDeclaration);
	}

	/**
	 * Migration des lettres de bienvenue des entreprises de RegPM
	 * @param regpm entreprise dans RegPM
	 * @param unireg entreprise dans Unireg
	 * @param mr collecteur de messages de suivi
	 * @param idMapper mapping des identifiants RegPM -> Unireg
	 */
	private void migrateLettresBienvenue(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr, IdMapping idMapper) {
		// s'il n'y a pas d'assujettissement, il n'y a rien à migrer
		if (!regpm.getAssujettissements().isEmpty()) {

			// on ne migre que le dernier envoi
			final RegDate dateDernierEnvoi = regpm.getAssujettissements().stream()
					.map(RegpmAssujettissement::getDateEnvoiLettre)
					.filter(Objects::nonNull)
					.max(Comparator.naturalOrder())
					.orElse(null);

			// des assujettissements mais pas de lettre -> erreur
			if (dateDernierEnvoi == null) {
				mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).");
			}
			else {
				// quelle est la catégorie d'entreprise à la date d'envoi ?
				final NavigableMap<RegDate, RegpmTypeFormeJuridique> histoFormesJuridiques = mr.getExtractedData(FormeJuridiqueHistoData.class, buildEntrepriseKey(regpm)).histo;
				final RegpmTypeFormeJuridique tfj = Optional.of(dateDernierEnvoi)
						.map(histoFormesJuridiques::floorEntry)
						.map(Map.Entry::getValue)
						.orElse(null);
				if (tfj == null) {
					mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
					              String.format("Pas de forme juridique valide à la date d'envoi de la lettre de bienvenue (%s), la lettre est ignorée.",
					                            StringRenderers.DATE_RENDERER.toString(dateDernierEnvoi)));
				}
				else {
					final TypeLettreBienvenue typeLettre;
					switch (tfj.getCategorie()) {
					case APM:
						// [SIFISC-18514] Si l'APM est (ou a été) inscrite au RC, il faut en tenir compte dans le type de lettre de bienvenue migrée
						if (regpm.getDateInscriptionRC() != null) {
							typeLettre = TypeLettreBienvenue.VD_RC;
						}
						else {
							typeLettre = TypeLettreBienvenue.APM_VD_NON_RC;
						}
						break;
					case PM:
						typeLettre = TypeLettreBienvenue.VD_RC;
						break;
					default:
						typeLettre = null;
						break;
					}

					if (typeLettre == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Forme juridique de catégorie non-supportée (%s) dans la reprise de la lettre de bienvenue, la lettre est ignorée.",
						                            tfj.getCategorie()));

					}
					else {
						final LettreBienvenue lettre = new LettreBienvenue();
						lettre.setDateEnvoi(dateDernierEnvoi);
						lettre.setDelaiRetour(dateDernierEnvoi.addDays(30));
						lettre.setDateRetour(dateDernierEnvoi.addDays(20));
						lettre.setType(typeLettre);
						unireg.addAutreDocumentFiscal(lettre);
					}
				}
			}
		}
	}

	/**
	 * Migration des déclarations d'impôts, de leurs états, délais...
	 */
	private void migrateDeclarationsImpot(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr, IdMapping idMapper) {

		final EntityKey moi = buildEntrepriseKey(regpm);

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les déclarations
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, moi).isIncompatible()) {
			mr.addMessage(LogCategory.DECLARATIONS, LogLevel.WARN, "Aucune déclaration d'impôt migrée en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

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
				neverNull(entreprise.getDeclarationsTriees(Declaration.class, false)).stream()
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
				final List<PeriodeImposition> pis = neverNull(migrationContexte.getPeriodeImpositionService().determine(entreprise));
				final int premierePeriodeFiscalePersonnesMorales = migrationContexte.getParametreAppService().getPremierePeriodeFiscalePersonnesMorales();

				// par période fiscale, cherchons les périodes d'imposition non-encore couvertes
				// 1. on commence par retrouver les déclarations par période fiscale
				final Map<Integer, List<DeclarationImpotOrdinairePM>> diExistantes = entreprise.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, false).stream()
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
	private static Set<EtatDeclaration> migrateEtatsDeclaration(RegpmDossierFiscal dossier, MigrationResultProduction mr) {

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
		etats.forEach(etat -> mr.addMessage(LogCategory.DECLARATIONS, LogLevel.INFO,
		                                    String.format("Etat '%s' migré au %s.",
		                                                  etat.getEtat(),
		                                                  StringRenderers.DATE_RENDERER.toString(etat.getDateObtention()))));

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
					final Commune commune = migrationContexte.getInfraService().getCommuneByNumeroOfs(forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getDateFin());
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

		final EntityKey entrepriseKey = buildEntrepriseKey(regpm);

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les fors
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, entrepriseKey).isIncompatible()) {
			mr.addMessage(LogCategory.FORS, LogLevel.WARN, "Aucun for fiscal principal migré en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

		// [SIFISC-16333] il faut faire attention à la forme juridique de l'entreprise
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
					.filter(date -> {
						if (dateFinActivite != null && date.compareTo(dateFinActivite) > 0) {
							mr.addMessage(LogCategory.FORS, LogLevel.WARN,
							              String.format("Date de fin d'activité (%s) postérieure à la date de début du premier immeuble de l'entreprise DP (%s), la date de début des fors principaux de l'entreprise ne sera donc en aucun cas ramenée à la date d'ouverture du premier immeuble.",
							                            StringRenderers.DATE_RENDERER.toString(dateFinActivite),
							                            StringRenderers.DATE_RENDERER.toString(date)));
							return false;
						}
						return true;
					})
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
			donneesForsAGenerer = generationDonneesMigrationForsPrincipaux(regpm, forsRegpm, hasSP, dateDebutPremierImmeubleDP, mr);
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
		assigneDatesFin(hasSP ? regpm.getDateFinSocietePersonnes() : dateFinActivite, liste);

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

		// récupération et assignation de la date de début du premier exercice commercial
		assigneDateDebutPremierExerciceCommercial(unireg);

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
	 * Recherche la date de début du premier for principal non-annulé avec un genre d'impôt IBC et, si présente,
	 * assigne cette valeur au champ "dateDebutPremierExerciceCommercial" de l'entreprise
	 * @param entreprise l'entreprise
	 */
	private static void assigneDateDebutPremierExerciceCommercial(Entreprise entreprise) {
		// on trouve la date de début du premier for principal avec genre d'impôt IBC pour la mettre 'date de début du premier exercice commercial'
		final List<ForFiscalPrincipalPM> collated = neverNull(entreprise.getForsFiscauxPrincipauxActifsSorted());
		collated.stream()
				.filter(ff -> !ff.isAnnule())
				.filter(ff -> ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL)
				.map(ForFiscalPrincipalPM::getDateDebut)
				.min(Comparator.naturalOrder())
				.ifPresent(entreprise::setDateDebutPremierExerciceCommercial);
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

		final List<Pair<ForFiscalPrincipalPM, Boolean>> donneesFors = sieges.tailMap(datePremierSiegeAPrendre, true).entrySet().stream()
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

		// [SIFISC-18118] dans le cas des sociétés de personnes, il faut prendre en compte les dates ad'hoc pour les fors
		if (societeDePersonnes && regpm.getDateDebutSocietePersonnes() != null) {
			final DateRange rangeSP = new DateRangeHelper.Range(regpm.getDateDebutSocietePersonnes(), regpm.getDateFinSocietePersonnes());
			return fixForsSocietePersonnes(donneesFors, rangeSP, mr);
		}
		else {
			return donneesFors;
		}
	}

	/**
	 * Génération des données servant à la migration des fors principaux existants en fors principaux pour Unireg
	 * @param regpm entreprise de RegPM
	 * @param forsRegpm les fors principaux retenus dans RegPM
	 * @param societeDePersonnes <code>vrai</code> si l'entreprise a toujours été une société de personne, <code>false</code> si elle ne l'a jamais été (le cas hybride doit être traité en amont...)
	 * @param dateDebutPremierImmeubleDP si non-<code>null</code>, nous sommes en présence d'une DP avec immeuble... il ne faut reprendre les fors principaux qu'à partir de cette date
	 * @param mr collecteur de messages de suivi
	 * @return une liste de données pour la création de fors (sans date de fin pour le moment) associés à un booléen qui indique si le for est une administration effective
	 */
	private List<Pair<ForFiscalPrincipalPM, Boolean>> generationDonneesMigrationForsPrincipaux(RegpmEntreprise regpm,
	                                                                                           List<RegpmForPrincipal> forsRegpm,
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

		final List<Pair<ForFiscalPrincipalPM, Boolean>> donneesFors = mapFors.tailMap(datePremierForAPrendre, true).values().stream()
				.map(mapper)
				.collect(Collectors.toList());

		// [SIFISC-18118] dans le cas des sociétés de personnes, il faut prendre en compte les dates ad'hoc pour les fors
		if (societeDePersonnes && regpm.getDateDebutSocietePersonnes() != null) {
			final DateRange rangeSP = new DateRangeHelper.Range(regpm.getDateDebutSocietePersonnes(), regpm.getDateFinSocietePersonnes());
			return fixForsSocietePersonnes(donneesFors, rangeSP, mr);
		}
		else {
			return donneesFors;
		}
	}

	private static List<Pair<ForFiscalPrincipalPM, Boolean>> fixForsSocietePersonnes(List<Pair<ForFiscalPrincipalPM, Boolean>> source, DateRange rangeSP, MigrationResultProduction mr) {
		final List<Pair<ForFiscalPrincipalPM, Boolean>> donneesForsCorrigeesSP = new ArrayList<>(source.size());
		for (Pair<ForFiscalPrincipalPM, Boolean> pair : source) {
			final DateRange intersection = DateRangeHelper.intersection(rangeSP, pair.getLeft());
			if (intersection == null) {
				mr.addMessage(LogCategory.FORS, LogLevel.WARN,
				              String.format("Le for principal %s normalement généré est finalement ignoré car en dehors de la période d'exploitation de la société de personnes (%s).",
				                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(pair.getLeft()),
				                            StringRenderers.DATE_RANGE_RENDERER.toString(rangeSP)));
			}
			else if (DateRangeHelper.equals(pair.getLeft(), intersection)) {
				// complètement compris... on devra peut-être l'étendre, mais en tout cas pas le tronquer
				donneesForsCorrigeesSP.add(pair);
			}
			else {
				// intersection mais pas inclusion -> il faut tronquer...
				mr.addMessage(LogCategory.FORS, LogLevel.WARN,
				              String.format("Le for principal %s doit être tronqué à %s pour ne pas dépasser de la période d'exploitation de la société de personnes (%s).",
				                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(pair.getLeft()),
				                            StringRenderers.DATE_RANGE_RENDERER.toString(intersection),
				                            StringRenderers.DATE_RANGE_RENDERER.toString(rangeSP)));

				final ForFiscalPrincipalPM tronque = (ForFiscalPrincipalPM) pair.getLeft().duplicate();
				tronque.setDateDebut(intersection.getDateDebut());
				tronque.setDateFin(intersection.getDateFin());
				donneesForsCorrigeesSP.add(Pair.of(tronque, pair.getRight()));
			}
		}

		// doit-on étendre des fors (au début ou à la fin) ?
		if (!donneesForsCorrigeesSP.isEmpty()) {
			// début
			final Pair<ForFiscalPrincipalPM, Boolean> first = donneesForsCorrigeesSP.get(0);
			if (first.getLeft().getDateDebut() != rangeSP.getDateDebut()) {
				mr.addMessage(LogCategory.FORS, LogLevel.WARN,
				              String.format("Le date de début du for principal %s doit être avancée au %s pour couvrir la période d'exploitation de la société de personnes (%s).",
				                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(first.getLeft()),
				                            StringRenderers.DATE_RENDERER.toString(rangeSP.getDateDebut()),
				                            StringRenderers.DATE_RANGE_RENDERER.toString(rangeSP)));

				final ForFiscalPrincipalPM ralonge = (ForFiscalPrincipalPM) first.getLeft().duplicate();
				ralonge.setDateDebut(rangeSP.getDateDebut());
				donneesForsCorrigeesSP.set(0, Pair.of(ralonge, first.getRight()));
			}

			// fin
			final Pair<ForFiscalPrincipalPM, Boolean> last = donneesForsCorrigeesSP.get(donneesForsCorrigeesSP.size() - 1);
			if (last.getLeft().getDateFin() != rangeSP.getDateFin()) {
				mr.addMessage(LogCategory.FORS, LogLevel.WARN,
				              String.format("Le date de fin du for principal %s doit être repoussée au %s pour couvrir la période d'exploitation de la société de personnes (%s).",
				                            StringRenderers.LOCALISATION_DATEE_RENDERER.toString(first.getLeft()),
				                            StringRenderers.DATE_RENDERER.toString(rangeSP.getDateFin()),
				                            StringRenderers.DATE_RANGE_RENDERER.toString(rangeSP)));

				final ForFiscalPrincipalPM ralonge = (ForFiscalPrincipalPM) last.getLeft().duplicate();
				ralonge.setDateFin(rangeSP.getDateFin());
				donneesForsCorrigeesSP.set(donneesForsCorrigeesSP.size() - 1, Pair.of(ralonge, last.getRight()));
			}
		}
		return donneesForsCorrigeesSP;
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
		final EntityKey key = buildEntrepriseKey(regpm);

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les fors
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, key).isIncompatible()) {
			mr.addMessage(LogCategory.FORS, LogLevel.WARN, "Aucun for fiscal 'immeuble' migré en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

		// les fors secondaires devront être créés sur l'entreprise migrée
		final KeyedSupplier<Entreprise> moi = new KeyedSupplier<>(key, getEntrepriseByUniregIdSupplier(unireg.getId()));

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
		for (T entity : CollectionsUtils.revertedOrder(listeTriee)) {
			entity.setDateFin(dateFinCourante);
			dateFinCourante = entity.getDateDebut().getOneDayBefore();
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
	private static <T> void assigneDatesFinSurRanged(@Nullable RegDate derniereDateFin, List<DateRangeHelper.Ranged<T>> listeTriee) {
		RegDate dateFinCourante = derniereDateFin;
		final ListIterator<DateRangeHelper.Ranged<T>> iterator = listeTriee.listIterator(listeTriee.size());
		while (iterator.hasPrevious()) {
			final DateRangeHelper.Ranged<T> element = iterator.previous();
			iterator.set(element.withDateFin(dateFinCourante));
			dateFinCourante = element.getDateDebut().getOneDayBefore();
		}
	}

	private RegimeFiscal mapRegimeFiscal(RegimeFiscal.Portee portee, RegpmRegimeFiscal rf, MigrationResultContextManipulation mr) {
		final RegimeFiscal unireg = new RegimeFiscal();
		unireg.setDateDebut(rf.getDateDebut());
		unireg.setDateFin(null);
		unireg.setPortee(portee);
		if (portee == RegimeFiscal.Portee.VD) {
			unireg.setCode(migrationContexte.getRegimeFiscalHelper().mapTypeRegimeFiscalVD(rf.getType()));
		}
		else {
			unireg.setCode(migrationContexte.getRegimeFiscalHelper().mapTypeRegimeFiscalCH(rf.getType()));
		}

		// dump de la liste des mappings
		final String ancienCode = rf.getType().getCode();
		mr.pushContextValue(RegimeFiscalMappingLoggedElement.class, new RegimeFiscalMappingLoggedElement(unireg, ancienCode));
		try {
			// niveau WARN si le code change, INFO sinon
			final LogLevel level = ObjectUtils.equals(ancienCode, unireg.getCode()) ? LogLevel.INFO : LogLevel.WARN;
			mr.addMessage(LogCategory.MAPPINGS_REGIMES_FISCAUX, level, StringUtils.EMPTY);
		}
		finally {
			mr.popContexteValue(RegimeFiscalMappingLoggedElement.class);
		}

		return unireg;
	}

	/**
	 * Extraction des régimes fiscaux non-annulés valides de l'entrepride de regpm
	 * @param e l'entreprise
	 * @param mr collecteur de messages de suivi et manipulateur de contexte de log
	 * @param idMapper mapping des identifiants RegPM -> Unireg
	 * @return les données officielles pour les régimes fiscaux à migrer
	 */
	@NotNull
	private RegimesFiscauxHistoData extractDonneesRegimesFiscaux(RegpmEntreprise e, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey entrepriseKey = buildEntrepriseKey(e);
		return doInLogContext(entrepriseKey, mr, idMapper, () -> {
			final RegDate dateFinActivite = mr.getExtractedData(DateFinActiviteData.class, entrepriseKey).date;
			final NavigableMap<RegDate, RegpmRegimeFiscalCH> ch = migrationContexte.getRegimeFiscalHelper().buildMapRegimesFiscaux(e.getRegimesFiscauxCH(), dateFinActivite, RegimeFiscal.Portee.CH, mr);
			final NavigableMap<RegDate, RegpmRegimeFiscalVD> vd = migrationContexte.getRegimeFiscalHelper().buildMapRegimesFiscaux(e.getRegimesFiscauxVD(), dateFinActivite, RegimeFiscal.Portee.VD, mr);
			return new RegimesFiscauxHistoData(ch, vd);
		});
	}

	/**
	 * Classe interne pour proposer une simplification de la suite des régimes fiscaux migrés
	 * s'ils ont le même type
	 */
	private static class CollatableRegimeFiscal implements CollatableDateRange {

		private final RegimeFiscal.Portee portee;
		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final String code;

		public CollatableRegimeFiscal(RegimeFiscal rf) {
			this(rf.getPortee(), rf.getDateDebut(), rf.getDateFin(), rf.getCode());
		}

		private CollatableRegimeFiscal(RegimeFiscal.Portee portee, RegDate dateDebut, RegDate dateFin, String code) {
			this.portee = portee;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.code = code;
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}

		@Override
		public boolean isCollatable(DateRange next) {
			return next instanceof CollatableRegimeFiscal
					&& DateRangeHelper.isCollatable(this, next)
					&& this.portee == ((CollatableRegimeFiscal) next).portee
					&& Objects.equals(this.code, ((CollatableRegimeFiscal) next).code);
		}

		@Override
		public CollatableRegimeFiscal collate(DateRange next) {
			if (!isCollatable(next)) {
				throw new IllegalArgumentException("Les deux ranges ne sont pas collatable !!");
			}
			return new CollatableRegimeFiscal(portee, dateDebut, next.getDateFin(), code);
		}

		public RegimeFiscal asRegimeFiscal() {
			return new RegimeFiscal(dateDebut, dateFin, portee, code);
		}
	}

	private <T extends RegpmRegimeFiscal> List<RegimeFiscal> mapRegimesFiscaux(RegimeFiscal.Portee portee,
	                                                                           NavigableMap<RegDate, T> regimes,
	                                                                           MigrationResultContextManipulation mr) {
		// collecte des régimes fiscaux sans date de fin d'abord...
		final List<RegimeFiscal> liste = regimes.values().stream()
				.map(r -> mapRegimeFiscal(portee, r, mr))
				.collect(Collectors.toList());

		// ... puis attribution des dates de fin
		assigneDatesFin(null, liste);

		// et finalement collations...
		final List<CollatableRegimeFiscal> collated = DateRangeHelper.collate(liste.stream()
				                                                                      .map(CollatableRegimeFiscal::new)
				                                                                      .collect(Collectors.toList()));

		return collated.stream()
				.map(CollatableRegimeFiscal::asRegimeFiscal)
				.collect(Collectors.toList());
	}

	private void migrateRegimesFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultContextManipulation mr) {

		final EntityKey key = buildEntrepriseKey(regpm);

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les régimes fiscaux
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, key).isIncompatible()) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Aucun régime fiscal migré en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

		final RegimesFiscauxHistoData histoData = mr.getExtractedData(RegimesFiscauxHistoData.class, key);

		// collecte des régimes fiscaux CH...
		final List<RegimeFiscal> listeCH = mapRegimesFiscaux(RegimeFiscal.Portee.CH, histoData.getCH(), mr);

		// ... puis des règimes fiscaux VD
		final List<RegimeFiscal> listeVD = mapRegimesFiscaux(RegimeFiscal.Portee.VD, histoData.getVD(), mr);

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

	private static AllegementFiscal instanciateAllegementFiscalCommunal(AllegementFiscalCantonCommune.Type type, @Nullable Integer noOfsCommune) {
		final AllegementFiscalCommune af = new AllegementFiscalCommune();
		af.setType(type);
		af.setNoOfsCommune(noOfsCommune);
		return af;
	}

	private static AllegementFiscal instanciateAllegementFiscalCantonal(AllegementFiscalCantonCommune.Type type) {
		final AllegementFiscalCanton af = new AllegementFiscalCanton();
		af.setType(type);
		return af;
	}

	private static AllegementFiscal instanciateAllegementFiscalFederal(AllegementFiscalConfederation.Type type) {
		final AllegementFiscalConfederation af = new AllegementFiscalConfederation();
		af.setType(type);
		return af;
	}

	private static AllegementFiscal buildAllegementFiscal(RegpmAllegementFiscal regpm,
	                                                      AllegementFiscal.TypeImpot typeImpot,
	                                                      AllegementFiscal.TypeCollectivite typeCollectivite,
	                                                      AllegementFiscalCantonCommune.Type typeIcc,
	                                                      AllegementFiscalConfederation.Type typeIfd,
	                                                      @Nullable Integer noOfsCommune) {
		final AllegementFiscal unireg;
		switch (typeCollectivite) {
		case CANTON:
			unireg = instanciateAllegementFiscalCantonal(typeIcc);
			break;
		case COMMUNE:
			unireg = instanciateAllegementFiscalCommunal(typeIcc, noOfsCommune);
			break;
		case CONFEDERATION:
			unireg = instanciateAllegementFiscalFederal(typeIfd);
			break;
		default:
			throw new IllegalArgumentException("Type de collectivité non-supporté : " + typeCollectivite);
		}

		copyCreationMutation(regpm, unireg);
		unireg.setDateDebut(regpm.getDateDebut());
		unireg.setDateFin(regpm.getDateFin());
		unireg.setPourcentageAllegement(regpm.getPourcentage());
		unireg.setTypeImpot(typeImpot);
		return unireg;
	}

	private static Stream<AllegementFiscal> mapAllegementFiscal(RegpmAllegementFiscal a, AllegementFiscalCantonCommune.Type typeIcc, AllegementFiscalConfederation.Type typeIfd, MigrationResultProduction mr) {

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
				builder.accept(buildAllegementFiscal(a, typeImpot, typeCollectivite, typeIcc, typeIfd, noOfsCommune));
			}
		}
		else if (a.getObjectImpot() != null) {
			final AllegementFiscal.TypeCollectivite typeCollectivite = toTypeCollectivite(a.getObjectImpot());
			builder.accept(buildAllegementFiscal(a, AllegementFiscal.TypeImpot.BENEFICE, typeCollectivite, typeIcc, typeIfd, null));
			builder.accept(buildAllegementFiscal(a, AllegementFiscal.TypeImpot.CAPITAL, typeCollectivite, typeIcc, typeIfd, null));
		}
		else {
			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
			              String.format("Allègement fiscal %d sans type de contribution ni object impôt -> ignoré.", a.getId().getSeqNo()));
		}

		return builder.build();
	}

	private static String buildCollectiviteString(AllegementFiscal a) {
		if (a instanceof AllegementFiscalCommune && ((AllegementFiscalCommune) a).getNoOfsCommune() != null) {
			return String.format("%s (%d)", a.getTypeCollectivite(), ((AllegementFiscalCommune) a).getNoOfsCommune());
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

		private final AllegementFiscalCommune allegement;

		public LocalizedAllegementFiscal(AllegementFiscalCommune allegement) {
			this.allegement = allegement;
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
		if (allegement instanceof AllegementFiscalCommune && ((AllegementFiscalCommune) allegement).getNoOfsCommune() != null) {
			// calcul de la nouvelle répartition sur des communes en prenant en compte les fusions en passant par une structure
			// temporaire de "localisation datée"
			final AllegementFiscalCommune allegementCommunal = (AllegementFiscalCommune) allegement;
			final LocalizedDateRange localizedRange = new LocalizedAllegementFiscal(allegementCommunal);
			final LocalisationDateeFacade<AllegementFiscalCommune> facade = new LocalisationDateeFacade<>(localizedRange, allegementCommunal);
			return adapterAutourFusionsCommunes(facade, mr, logCategory, null).stream()
					.map(f -> new AllegementFiscalCommune(f.getDateDebut(),
					                                      f.getDateFin(),
					                                      allegementCommunal.getPourcentageAllegement(),
					                                      allegementCommunal.getTypeImpot(),
					                                      allegementCommunal.getType(),
					                                      f.getNumeroOfsAutoriteFiscale()));
		}
		else {
			// pas de changement nécessaire pour les fusions de communes (on n'est même pas sur une commune précise !)
			return Stream.of(allegement);
		}
	}

	private static AllegementFiscalConfederation.Type valueOfIFD(Integer value) {
		final AllegementFiscalConfederation.Type type;
		if (value == null) {
			type = null;
		}
		else {
			switch (value) {
			case 0:
				type = null;
				break;
			case 51:
				type = AllegementFiscalConfederation.Type.TEMPORAIRE_91LI;
				break;
			case 52:
				type = AllegementFiscalConfederation.Type.EXONERATION_SPECIALE;
				break;
			default:
				throw new IllegalArgumentException("Valeur invalide pour le type d'allègement 999 : " + value);
			}
		}
		return type;
	}

	private static AllegementFiscalCantonCommune.Type valueOfICC(Integer value) {
		final AllegementFiscalCantonCommune.Type type;
		if (value == null) {
			type = null;
		}
		else {
			switch (value) {
			case 0:
				type = null;
				break;
			case 1:
				type = AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI;
				break;
			case 2:
			case 3:
			case 4:
			case 6:
			case 12:
				type = AllegementFiscalCantonCommune.Type.EXONERATION_90LI;
				break;
			case 8:
				type = AllegementFiscalCantonCommune.Type.SOCIETE_SERVICE;
				break;
			case 9:
				type = AllegementFiscalCantonCommune.Type.IMMEUBLE_SI_SUBVENTIONNEE;
				break;
			case 11:
			case 17:
				type = AllegementFiscalCantonCommune.Type.EXONERATION_SPECIALE;
				break;
			default:
				throw new IllegalArgumentException("Valeur invalide pour le type d'allègement 998 : " + value);
			}
		}
		return type;
	}

	private void migrateAllegementsFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// [SIFISC-18378] si les formes juridiques entre RegPM et RCent sont incompatibles, on ne migre pas les allègements
		if (mr.getExtractedData(FlagFormesJuridiquesIncompatiblesData.class, buildEntrepriseKey(regpm)).isIncompatible()) {
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, "Aucun allègement fiscal migré en raison des formes juridiques incompatibles entre RegPM et RCEnt.");
			return;
		}

		// d'abord, il faut extraire les types d'allègements (998/999)
		// Le booléen en clé signifie isIFD (= faux -> ICC)
		final Map<Boolean, Integer> mapTypes = regpm.getAllegementsFiscaux().stream()
				.filter(af -> af.getDateAnnulation() == null)               // on ne prend pas en compte les typologies d'allègements annulées
				.filter(af -> af.getId().getSeqNo() >= 998)
				.filter(af -> af.getId().getSeqNo() < 1000)
				.collect(Collectors.toMap(af -> af.getId().getSeqNo() == 999, af -> af.getPourcentage().intValue()));
		final AllegementFiscalCantonCommune.Type typeIcc = valueOfICC(mapTypes.get(Boolean.FALSE));
		final AllegementFiscalConfederation.Type typeIfd = valueOfIFD(mapTypes.get(Boolean.TRUE));

		// ensuite, on traite les vrais allègements fiscaux accordés
		regpm.getAllegementsFiscaux().stream()
				.filter(a -> a.getDateAnnulation() == null)                 // on ne prend pas en compte les allègements annulés
				.sorted(Comparator.comparing(a -> a.getId().getSeqNo()))    // tri pour les tests en particulier, pour toujours traiter les allègements dans le même ordre
				.filter(a -> a.getId().getSeqNo() < 998)                    // on ne prend pas en compte les numéros de séquence 998 et 999 comme des allègements eux-mêmes
				.filter(a -> {
					if (migrationContexte.getDateHelper().isFutureDate(a.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Allègement fiscal %d ignoré en raison de sa date de début dans le futur (%s).",
						                            a.getId().getSeqNo(),
						                            StringRenderers.DATE_RENDERER.toString(a.getDateDebut())));
						return false;
					}
					return true;
				})
				.map(a -> {
					if (migrationContexte.getDateHelper().isFutureDate(a.getDateFin())) {
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
				.peek(a -> migrationContexte.getDateHelper().checkDateLouche(a.getDateDebut(),
				                                                             () -> String.format("Allègement fiscal %d avec une date de début de validité", a.getId().getSeqNo()),
				                                                             LogCategory.SUIVI,
				                                                             mr))
				.map(a -> mapAllegementFiscal(a, typeIcc, typeIfd, mr))
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
				.map(e -> {
					final RegDate dateValidite = Optional.ofNullable(e.getDateValidite()).orElseGet(() -> {
						// [SIFISC-18151] si l'état est "fondée" et que la date est nulle (ce qui est le cas si nous sommes ici...)
						// il faut reprendre la date du premier siège
						if (e.getTypeEtat() == RegpmTypeEtatEntreprise.FONDEE) {
							final RegDate premierSiege = Optional.of(mr.getExtractedData(SiegesHistoData.class, buildEntrepriseKey(regpm)).histo)
									.filter(sieges -> !sieges.isEmpty())
									.map(NavigableMap::firstKey)
									.orElse(null);

							if (premierSiege != null) {
								mr.addMessage(LogCategory.SUIVI, LogLevel.WARN,
								              String.format("Etat d'entreprise %d (%s) trouvé, dont la date de début de validité, nulle (ou antérieure au 01.08.1291), est ramenée à la date du premier siège (%s).",
								                            e.getId().getSeqNo(),
								                            e.getTypeEtat(),
								                            StringRenderers.DATE_RENDERER.toString(premierSiege)));
								return premierSiege;
							}
						}
						return null;
					});
					if (dateValidite == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Etat d'entreprise %d (%s) ignoré car sa date de début de validité est nulle (ou antérieure au 01.08.1291).",
						                            e.getId().getSeqNo(),
						                            e.getTypeEtat()));
						return null;
					}
					else {
						return Pair.of(dateValidite, e);
					}
				})
				.filter(Objects::nonNull)
				.filter(pair -> {
					if (migrationContexte.getDateHelper().isFutureDate(pair.getLeft())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Etat d'entreprise %d (%s) ignoré en raison de sa date de début dans le futur (%s).",
						                            pair.getRight().getId().getSeqNo(),
						                            pair.getRight().getTypeEtat(),
						                            StringRenderers.DATE_RENDERER.toString(pair.getLeft())));
						return false;
					}
					return true;
				})
				.collect(Collectors.toMap(Pair::getLeft,
				                          pair -> Collections.singletonList(pair.getRight()),
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
				                          TreeMap::new));

		// [SIFISC-17111] même si plusieurs états commencent à la même date, on les garde tous
		final List<EtatEntreprise> liste = map.entrySet().stream()
				.map(entry -> entry.getValue().stream().map(e -> Pair.of(entry.getKey(), e)))
				.flatMap(Function.identity())
				.peek(pair -> migrationContexte.getDateHelper().checkDateLouche(pair.getLeft(),
				                                                                () -> String.format("Etat d'entreprise %d (%s) avec une date de début de validité",
				                                                                                    pair.getRight().getId().getSeqNo(),
				                                                                                    pair.getRight().getTypeEtat()),
				                                                                LogCategory.SUIVI,
				                                                                mr))
				.map(pair -> {
					final EtatEntreprise etat = new EtatEntreprise();
					copyCreationMutation(pair.getRight(), etat);
					etat.setDateObtention(pair.getLeft());
					etat.setType(mapTypeEtatEntreprise(pair.getRight().getTypeEtat()));
					etat.setGeneration(TypeGenerationEtatEntreprise.AUTOMATIQUE);
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
		case EN_SUSPENS_FAILLITE:
		case EN_FAILLITE:
			return TypeEtatEntreprise.EN_FAILLITE;
		case EN_LIQUIDATION:
			return TypeEtatEntreprise.EN_LIQUIDATION;
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
