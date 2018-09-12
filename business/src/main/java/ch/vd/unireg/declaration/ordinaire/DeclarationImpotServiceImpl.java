package ch.vd.unireg.declaration.ordinaire;

import javax.jms.JMSException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEchue;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.ordinaire.common.DemandeDelaiCollectiveProcessor;
import ch.vd.unireg.declaration.ordinaire.common.DemandeDelaiCollectiveResults;
import ch.vd.unireg.declaration.ordinaire.pm.DeterminationDIsPMAEmettreProcessor;
import ch.vd.unireg.declaration.ordinaire.pm.DeterminationDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMProcessor;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiDeclarationsPMProcessor;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiSommationsDIsPMProcessor;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiSommationsDIsPMResults;
import ch.vd.unireg.declaration.ordinaire.pm.ImpressionSommationDeclarationImpotPersonnesMoralesHelper;
import ch.vd.unireg.declaration.ordinaire.pp.ContribuableAvecCodeSegment;
import ch.vd.unireg.declaration.ordinaire.pp.ContribuableAvecImmeuble;
import ch.vd.unireg.declaration.ordinaire.pp.DeterminationDIsPPAEmettreProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.DeterminationDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EchoirDIsPPProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.EchoirDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiAnnexeImmeubleEnMasseProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiDIsPPEnMasseProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiSommationsDIsPPProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.ImportCodesSegmentProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.ImportCodesSegmentResults;
import ch.vd.unireg.declaration.ordinaire.pp.ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.unireg.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.unireg.declaration.ordinaire.pp.ListeDIsPPNonEmises;
import ch.vd.unireg.declaration.ordinaire.pp.ListeNoteProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.unireg.declaration.ordinaire.pp.ProduireListeDIsNonEmisesProcessor;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationPMSender;
import ch.vd.unireg.evenement.di.EvenementDeclarationPPSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPM;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.ModeExonerationHisto;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.validation.ValidationService;

import static ch.vd.unireg.declaration.ordinaire.EnumCodeRoutageDI.APM_EXONEREE;
import static ch.vd.unireg.declaration.ordinaire.EnumCodeRoutageDI.APM_NON_EXONEREE;
import static ch.vd.unireg.declaration.ordinaire.EnumCodeRoutageDI.PM_HOLDING;
import static ch.vd.unireg.declaration.ordinaire.EnumCodeRoutageDI.PM_HORS_CANTON;
import static ch.vd.unireg.declaration.ordinaire.EnumCodeRoutageDI.PM_HORS_SUISSE;
import static ch.vd.unireg.declaration.ordinaire.EnumCodeRoutageDI.PM_SNC;

public class DeclarationImpotServiceImpl implements DeclarationImpotService {

	/**
	 * Ce sont les codes des régimes fiscaux vaudois spécifiques aux holdings (11 et 12) et sociétés de base (41C et 42C)
	 */
	private static final Set<String> CODES_REGIMES_FISCAUX_SOCIETE_BASE_HOLDING = new HashSet<>(Arrays.asList("11", "12", "41C", "42C"));
	/**
	 * Code de regime fiscal pour les sociétés de type SNC
	 */
	private static final String CODE_REGIME_FISCAL_SNC = "80";

	// private final Logger LOGGER = LoggerFactory.getLogger(DeclarationImpotServiceImpl.class);

	private EvenementFiscalService evenementFiscalService;
	private EditiqueCompositionService editiqueCompositionService;
	private EditiqueService editiqueService;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private PeriodeFiscaleDAO periodeDAO;
	private TacheDAO tacheDAO;
	private ModeleDocumentDAO modeleDAO;
	private DeclarationImpotOrdinaireDAO diDAO;
	private DelaisService delaisService;
	private ServiceInfrastructureService infraService;
	private AdresseService adresseService;
	private ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper impressionSommationDIPPHelper;
	private ImpressionSommationDeclarationImpotPersonnesMoralesHelper impressionSommationDIPMHelper;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private TiersService tiersService;
	private ParametreAppService parametres;
	private ValidationService validationService;
	private EvenementDeclarationPPSender evenementDeclarationPPSender;
	private EvenementDeclarationPMSender evenementDeclarationPMSender;
	private PeriodeImpositionService periodeImpositionService;
	private AssujettissementService assujettissementService;
	private TicketService ticketService;
	private PeriodeImpositionImpotSourceService piisService;
	private RegimeFiscalService regimeFiscalService;

	private Set<String> sourcesMonoQuittancement;

	private int tailleLot = 100; // valeur par défaut

	public DeclarationImpotServiceImpl() {
	}

	public DeclarationImpotServiceImpl(EditiqueCompositionService editiqueCompositionService, HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO,
	                                   TacheDAO tacheDAO, ModeleDocumentDAO modeleDAO, DelaisService delaisService, ServiceInfrastructureService infraService,
	                                   TiersService tiersService, PlatformTransactionManager transactionManager,
	                                   ParametreAppService parametres, ServiceCivilCacheWarmer serviceCivilCacheWarmer, ValidationService validationService,
	                                   EvenementFiscalService evenementFiscalService, EvenementDeclarationPPSender evenementDeclarationPPSender, PeriodeImpositionService periodeImpositionService,
	                                   AssujettissementService assujettissementService, TicketService ticketService,RegimeFiscalService regimeFiscalService) {
		this.editiqueCompositionService = editiqueCompositionService;
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.tacheDAO = tacheDAO;
		this.modeleDAO = modeleDAO;
		this.delaisService = delaisService;
		this.infraService = infraService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.parametres = parametres;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.validationService = validationService;
		this.evenementFiscalService = evenementFiscalService;
		this.evenementDeclarationPPSender = evenementDeclarationPPSender;
		this.periodeImpositionService = periodeImpositionService;
		this.assujettissementService = assujettissementService;
		this.ticketService = ticketService;
		this.regimeFiscalService = regimeFiscalService;
		this.sourcesMonoQuittancement = Collections.emptySet();
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setPeriodeDAO(PeriodeFiscaleDAO periodeDAO) {
		this.periodeDAO = periodeDAO;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setModeleDAO(ModeleDocumentDAO modeleDAO) {
		this.modeleDAO = modeleDAO;
	}

	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setImpressionSommationDIPPHelper(ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper impressionSommationDIPPHelper) {
		this.impressionSommationDIPPHelper = impressionSommationDIPPHelper;
	}

	public void setImpressionSommationDIPMHelper(ImpressionSommationDeclarationImpotPersonnesMoralesHelper impressionSommationDIPMHelper) {
		this.impressionSommationDIPMHelper = impressionSommationDIPMHelper;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setEvenementDeclarationPPSender(EvenementDeclarationPPSender evenementDeclarationPPSender) {
		this.evenementDeclarationPPSender = evenementDeclarationPPSender;
	}

	public void setEvenementDeclarationPMSender(EvenementDeclarationPMSender evenementDeclarationPMSender) {
		this.evenementDeclarationPMSender = evenementDeclarationPMSender;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	public void setSourcesMonoQuittancement(Set<String> sources) {
		this.sourcesMonoQuittancement = sources;
	}

	public void setPiisService(PeriodeImpositionImpotSourceService piisService) {
		this.piisService = piisService;
	}

	/**
	 * Pour le testing uniquement
	 */
	protected void setTailleLot(int tailleLot) {
		this.tailleLot = tailleLot;
	}

	@Override
	public DeterminationDIsPPResults determineDIsPPAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, @Nullable StatusManager status) throws DeclarationException {
		final DeterminationDIsPPAEmettreProcessor processer = new DeterminationDIsPPAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO,
		                                                                                              parametres, tiersService, transactionManager, validationService,
		                                                                                              periodeImpositionService, adresseService);
		return processer.run(anneePeriode, dateTraitement, nbThreads, status);
	}

	@Override
	public DeterminationDIsPMResults determineDIsPMAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, StatusManager status) throws DeclarationException {
		final DeterminationDIsPMAEmettreProcessor processer = new DeterminationDIsPMAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO,
		                                                                                              parametres, tiersService, transactionManager, validationService,
		                                                                                              periodeImpositionService, adresseService);
		return processer.run(anneePeriode, dateTraitement, nbThreads, status);
	}

	@Override
	public EnvoiDIsPPResults envoyerDIsPPEnMasse(int anneePeriode, CategorieEnvoiDIPP categorie, @Nullable Long noCtbMin, @Nullable Long noCtbMax, int nbMax, RegDate dateTraitement, boolean exclureDecedes,
	                                             int nbThreads, @Nullable StatusManager status) throws DeclarationException {

		final EnvoiDIsPPEnMasseProcessor processor = new EnvoiDIsPPEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO,
		                                                                            delaisService, this, tailleLot, transactionManager, parametres, serviceCivilCacheWarmer, adresseService, ticketService);
		return processor.run(anneePeriode, categorie, noCtbMin, noCtbMax, nbMax, dateTraitement, exclureDecedes, nbThreads, status);
	}

	@Override
	public EnvoiAnnexeImmeubleResults envoyerAnnexeImmeubleEnMasse(int anneePeriode, RegDate dateTraitement,
	                                                               List<ContribuableAvecImmeuble> listeCtb, int nbMax, StatusManager status) throws DeclarationException {
		final EnvoiAnnexeImmeubleEnMasseProcessor processor = new EnvoiAnnexeImmeubleEnMasseProcessor(tiersService, modeleDAO, periodeDAO,
		                                                                                              this, tailleLot, transactionManager, serviceCivilCacheWarmer, periodeImpositionService, adresseService);
		return processor.run(anneePeriode, listeCtb, nbMax, dateTraitement, status);
	}

	@Override
	public StatistiquesDIs produireStatsDIsPP(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireStatsDIsProcessor processor = new ProduireStatsDIsProcessor(hibernateTemplate, infraService, transactionManager, diDAO, assujettissementService, tiersService, adresseService);
		return processor.runPP(anneePeriode, dateTraitement, status);
	}

	@Override
	public StatistiquesDIs produireStatsDIsPM(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireStatsDIsProcessor processor = new ProduireStatsDIsProcessor(hibernateTemplate, infraService, transactionManager, diDAO, assujettissementService, tiersService, adresseService);
		return processor.runPM(anneePeriode, dateTraitement, status);
	}

	@Override
	public StatistiquesCtbs produireStatsCtbsPP(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireStatsCtbsProcessor processor = new ProduireStatsCtbsProcessor(hibernateTemplate, infraService, tiersService, transactionManager, assujettissementService, periodeImpositionService, adresseService,
		                                                                            piisService);
		return processor.runPP(anneePeriode, dateTraitement, status);
	}

	@Override
	public StatistiquesCtbs produireStatsCtbsPM(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireStatsCtbsProcessor processor = new ProduireStatsCtbsProcessor(hibernateTemplate, infraService, tiersService, transactionManager, assujettissementService, periodeImpositionService, adresseService,
		                                                                            piisService);
		return processor.runPM(anneePeriode, dateTraitement, status);
	}

	@Override
	public ListeDIsPPNonEmises produireListeDIsNonEmises(Integer anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireListeDIsNonEmisesProcessor processor = new ProduireListeDIsNonEmisesProcessor(hibernateTemplate, periodeDAO, modeleDAO,
		                                                                                            tacheDAO, tiersService, delaisService, this, transactionManager, parametres,
		                                                                                            serviceCivilCacheWarmer, validationService, periodeImpositionService,
		                                                                                            adresseService, ticketService);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	@Override
	public EchoirDIsPPResults echoirDIsPPHorsDelai(RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final EchoirDIsPPProcessor processor = new EchoirDIsPPProcessor(hibernateTemplate, delaisService, this, transactionManager, tiersService, adresseService);
		return processor.run(dateTraitement, status);
	}

	@Override
	public EchoirDIsPMResults echoirDIsPMHorsDelai(RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final EchoirDIsPMProcessor processor = new EchoirDIsPMProcessor(hibernateTemplate, delaisService, this, transactionManager, tiersService, adresseService);
		return processor.run(dateTraitement, status);
	}

	@Override
	public EditiqueResultat envoiDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws DeclarationException {

		final ContribuableImpositionPersonnesPhysiques ctb = declaration.getTiers();

		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeDIOnline(declaration);
			evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(declaration, dateEvenement);

			// [SIFISC-3103] Pour les périodes fiscales avant 2011, on n'envoie aucun événement de création de DI (pour le moment, il ne s'agit que d'ADDI)
			final int pf = declaration.getPeriode().getAnnee();
			if (pf >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
				final String codeSegmentString = Integer.toString(declaration.getCodeSegment() != null ? declaration.getCodeSegment() : VALEUR_DEFAUT_CODE_SEGMENT);
				evenementDeclarationPPSender.sendEmissionEvent(ctb.getNumero(), pf, dateEvenement, declaration.getCodeControle(), codeSegmentString);
			}

			// [UNIREG-2705] il est maintenant possible de créer des déclarations déjà retournées (et pas seulement pour les indigents)
			final EtatDeclaration etatRetour = declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE);
			if (etatRetour != null) {
				evenementFiscalService.publierEvenementFiscalQuittancementDeclarationImpot(declaration, etatRetour.getDateObtention());
			}

			return resultat;
		}
		catch (EditiqueException | EvenementDeclarationException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EditiqueResultat envoiDIOnline(DeclarationImpotOrdinairePM declaration, RegDate dateEvenement) throws DeclarationException {

		final ContribuableImpositionPersonnesMorales ctb = declaration.getTiers();

		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeDIOnline(declaration);
			evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(declaration, dateEvenement);

			// envoi du NIP à qui de droit
			if (StringUtils.isNotBlank(declaration.getCodeControle())) {
				final String codeSegment = declaration.getCodeSegment() != null ? Integer.toString(declaration.getCodeSegment()) : null;
				evenementDeclarationPMSender.sendEmissionDIEvent(ctb.getNumero(), declaration.getPeriode().getAnnee(), declaration.getNumero(), declaration.getCodeControle(), codeSegment);
			}

			// [UNIREG-2705] il est maintenant possible de créer des déclarations déjà retournées (et pas seulement pour les indigents)
			final EtatDeclaration etatRetour = declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE);
			if (etatRetour != null) {
				evenementFiscalService.publierEvenementFiscalQuittancementDeclarationImpot(declaration, etatRetour.getDateObtention());
			}

			return resultat;
		}
		catch (EditiqueException | EvenementDeclarationException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException {
		try {
			return editiqueCompositionService.imprimeDuplicataDIOnline(declaration, typeDocument, annexes);
		}
		catch (EditiqueException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException {
		try {
			return editiqueCompositionService.imprimeDuplicataDIOnline(declaration, annexes);
		}
		catch (EditiqueException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public void envoiDIForBatch(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws DeclarationException {
		try {
			final Contribuable tiers = declaration.getTiers();
			editiqueCompositionService.imprimeDIForBatch(declaration);
			evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(declaration, dateEvenement);

			final String codeSegmentString = Integer.toString(declaration.getCodeSegment() != null ? declaration.getCodeSegment() : VALEUR_DEFAUT_CODE_SEGMENT);
			evenementDeclarationPPSender.sendEmissionEvent(tiers.getNumero(), declaration.getPeriode().getAnnee(), dateEvenement, declaration.getCodeControle(), codeSegmentString);
		}
		catch (EditiqueException | EvenementDeclarationException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public void envoiDIForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateEvenement) throws DeclarationException {
		try {
			final ContribuableImpositionPersonnesMorales ctb = declaration.getTiers();
			editiqueCompositionService.imprimeDIForBatch(declaration);
			evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(declaration, dateEvenement);
			if (StringUtils.isNotBlank(declaration.getCodeControle())) {
				final String codeSegment = declaration.getCodeSegment() != null ? Integer.toString(declaration.getCodeSegment()) : null;
				evenementDeclarationPMSender.sendEmissionDIEvent(ctb.getNumero(), declaration.getPeriode().getAnnee(), declaration.getNumero(), declaration.getCodeControle(), codeSegment);
			}
		}
		catch (EditiqueException | EvenementDeclarationException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public void envoiSommationDIPPForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement, @Nullable Integer emolument) throws DeclarationException {
		try {
			editiqueCompositionService.imprimeSommationDIForBatch(declaration, miseSousPliImpossible, dateEvenement, emolument);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalSommationDeclarationImpot(declaration, dateEvenement);
	}

	@Override
	public void envoiSommationDIPMForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateExpedition) throws DeclarationException {
		try {
			editiqueCompositionService.imprimeSommationDIForBatch(declaration, dateTraitement, dateExpedition);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalSommationDeclarationImpot(declaration, dateTraitement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void echoirDI(DeclarationImpotOrdinaire declaration, RegDate dateTraitement) {
		final EtatDeclaration etat = new EtatDeclarationEchue();
		etat.setDateObtention(dateTraitement);
		declaration.addEtat(etat);
		evenementFiscalService.publierEvenementFiscalEcheanceDeclarationImpot(declaration, dateTraitement);
	}

	@Override
	public void quittancementDI(Contribuable contribuable, DeclarationImpotOrdinaire di, final RegDate dateEvenement, String source, boolean evtFiscal) {
		// [SIFISC-5208] Dorénavant, on stocke scrupuleusement tous les états de quittancement de type 'retournés', *sans* annuler les états précédents.
		// [SIFISC-8436] certaines sources ne supportent pas le multi-quittancement
		if (sourcesMonoQuittancement.contains(source)) {
			for (EtatDeclaration etat : di.getEtatsDeclaration()) {
				if (!etat.isAnnule() && etat instanceof EtatDeclarationRetournee && source.equals(((EtatDeclarationRetournee) etat).getSource())) {
					etat.setAnnule(true);
				}
			}
		}
		final EtatDeclaration etat = new EtatDeclarationRetournee(dateEvenement, source);
		di.addEtat(etat);

		if (evtFiscal) {
			evenementFiscalService.publierEvenementFiscalQuittancementDeclarationImpot(di, dateEvenement);
		}
	}

	/**
	 * Annulation d'une DI
	 *
	 * @param contribuable  le contribuable qui possède la déclaration à annuler
	 * @param di            la déclaration qui doit être annulée
	 * @param tacheId       Non-<code>null</code> si l'annulation de la DI est l'objet du traitement d'une tâche, auquel cas c'est l'ID de cette tâche
	 * @param dateEvenement la date d'annulation
	 * @return la déclaration nouvellement annulée
	 */
	@Override
	public void annulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, @Nullable Long tacheId, RegDate dateEvenement) {
		di.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationDeclarationImpot(di);

		// traitement de la tâche...
		if (tacheId != null) {
			final Tache tache = tacheDAO.get(tacheId);
			if (tache != null && !tache.isAnnule() && tache.getEtat() != TypeEtatTache.TRAITE) {
				tache.setEtat(TypeEtatTache.TRAITE);
			}
		}

		try {
			// [SIFISC-3103] Pour les périodes fiscales avant 2011, on n'envoie aucun événement d'annulation de DI (pour le moment, il ne s'agit que d'ADDI)
			final int pf = di.getPeriode().getAnnee();
			if (di instanceof DeclarationImpotOrdinairePP && pf >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
				evenementDeclarationPPSender.sendAnnulationEvent(contribuable.getNumero(), pf, dateEvenement);
			}
			if (di instanceof DeclarationImpotOrdinairePM && StringUtils.isNotBlank(di.getCodeControle())) {
				final DeclarationImpotOrdinairePM dipm = (DeclarationImpotOrdinairePM) di;
				final String codeRoutage = dipm.getCodeSegment() != null ? Integer.toString(dipm.getCodeSegment()) : null;
				evenementDeclarationPMSender.sendAnnulationDIEvent(contribuable.getNumero(), pf, di.getNumero(), di.getCodeControle(), codeRoutage);
			}
		}
		catch (EvenementDeclarationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void desannulationDI(Contribuable ctb, DeclarationImpotOrdinaire di, RegDate dateEvenement) {

		if (!di.isAnnule()) {
			throw new IllegalArgumentException("Impossible de désannuler la déclaration n°" + di.getId() + " car elle n'est pas annulée !");
		}

		di.setAnnule(false);
		evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(di, dateEvenement);
		try {
			// [SIFISC-3103] Pour les périodes fiscales avant 2011, on n'envoie aucun événement de désannulation de DI (pour le moment, il ne s'agit que d'ADDI)
			// [SIFISC-8598] Les DI de la PF 2011 qui ont été émises avant l'envoi de masse de début 2012 n'ont pas forcément de code de contrôle, il est donc inutile de les signaler à ADDI
			final int pf = di.getPeriode().getAnnee();
			if (di instanceof DeclarationImpotOrdinairePP) {
				final DeclarationImpotOrdinairePP dipp = (DeclarationImpotOrdinairePP) di;
				if (pf > DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE || (pf == DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE && dipp.getCodeControle() != null)) {
					final String codeSegmentString = Integer.toString(dipp.getCodeSegment() != null ? dipp.getCodeSegment() : VALEUR_DEFAUT_CODE_SEGMENT);
					evenementDeclarationPPSender.sendEmissionEvent(ctb.getNumero(), pf, dateEvenement, dipp.getCodeControle(), codeSegmentString);
				}
			}
			else if (di instanceof DeclarationImpotOrdinairePM) {
				final DeclarationImpotOrdinairePM dipm = (DeclarationImpotOrdinairePM) di;
				if (StringUtils.isNotBlank(dipm.getCodeControle())) {
					final String codeRoutage = dipm.getCodeSegment() != null ? Integer.toString(dipm.getCodeSegment()) : null;
					evenementDeclarationPMSender.sendEmissionDIEvent(ctb.getNumero(), pf, dipm.getNumero(), dipm.getCodeControle(), codeRoutage);
				}
			}
		}
		catch (EvenementDeclarationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvoiSommationsDIsPPResults envoyerSommationsPP(RegDate dateTraitement, boolean miseSousPliImpossible, int nombreMax, StatusManager statusManager) {
		final DeclarationImpotService diService = this;
		EnvoiSommationsDIsPPProcessor processor = new EnvoiSommationsDIsPPProcessor(hibernateTemplate, diDAO, delaisService, diService, tiersService, transactionManager, assujettissementService,
		                                                                            periodeImpositionService, adresseService);
		return processor.run(dateTraitement, miseSousPliImpossible, nombreMax, statusManager);
	}

	@Override
	public EditiqueResultat getCopieConformeSommationDI(DeclarationImpotOrdinairePP di) throws EditiqueException {
		String nomDocument = construitIdArchivageSommationDI(di);
		EditiqueResultat pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), TypeDocumentEditique.SOMMATION_DI, nomDocument);
		if (pdf == null) {
			nomDocument = construitAncienIdArchivageSommationDI(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), TypeDocumentEditique.SOMMATION_DI, nomDocument);
		}
		if (pdf == null) {
			nomDocument = construitAncienIdArchivageSommationDIPourOnLine(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), TypeDocumentEditique.SOMMATION_DI, nomDocument);
		}
		return pdf;
	}

	@Override
	public EditiqueResultat getCopieConformeSommationDI(DeclarationImpotOrdinairePM di) throws EditiqueException {
		final String cleArchivage = impressionSommationDIPMHelper.construitCleArchivageDocument(di);
		return editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), impressionSommationDIPMHelper.getTypeDocumentEditique(), cleArchivage);
	}

	/**
	 * Construit l'ID du document pour l'archivage
	 *
	 * @param declaration
	 * @return
	 */
	private String construitIdArchivageSommationDI(DeclarationImpotOrdinaire declaration) {
		return impressionSommationDIPPHelper.construitIdArchivageDocument(declaration);
	}

	/**
	 * Construit l'ID du document pour l'archivage (avant octobre 2009)
	 *
	 * @param declaration
	 * @return
	 */
	private String construitAncienIdArchivageSommationDI(DeclarationImpotOrdinaire declaration) {
		return impressionSommationDIPPHelper.construitAncienIdArchivageDocument(declaration);
	}


	/**
	 * Construit l'ID du document pour l'archivage pour les on-line (avant octobre 2009)
	 *
	 * @param declaration
	 * @return
	 */
	private String construitAncienIdArchivageSommationDIPourOnLine(DeclarationImpotOrdinaire declaration) {
		return impressionSommationDIPPHelper.construitAncienIdArchivageDocumentPourOnLine(declaration);
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DemandeDelaiCollectiveResults traiterDemandeDelaiCollective(List<Long> ids, int pf, RegDate dateDelai,
	                                                                   RegDate dateTraitement, StatusManager s) {
		final DemandeDelaiCollectiveProcessor processor = new DemandeDelaiCollectiveProcessor(periodeDAO, hibernateTemplate, transactionManager, tiersService, adresseService);
		return processor.run(ids, pf, dateDelai, dateTraitement, s);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListeNoteResults produireListeNote(RegDate dateTraitement, int nbThreads, Integer annee, StatusManager statusManager) {
		final ListeNoteProcessor processor = new ListeNoteProcessor(hibernateTemplate, transactionManager, tiersService, adresseService, infraService);
		return processor.run(dateTraitement, annee, nbThreads, statusManager);
	}

	@Override
	public int envoiAnnexeImmeubleForBatch(InformationsDocumentAdapter infoDocuments, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws DeclarationException {
		try {
			return editiqueCompositionService.imprimeAnnexeImmeubleForBatch(infoDocuments, listeModele, nombreAnnexesImmeuble);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public DelaiDeclaration addAndSave(DeclarationImpotOrdinaire declaration, DelaiDeclaration delai) {
		return AddAndSaveHelper.addAndSave(declaration, delai, hibernateTemplate::merge, new DelaiDocumentFiscalAddAndSaveAccessor<>());
	}

	@Override
	public <T extends EtatDeclaration> T addAndSave(DeclarationImpotOrdinaire declaration, T etat) {
		return AddAndSaveHelper.addAndSave(declaration, etat, hibernateTemplate::merge, new EtatDocumentFiscalAddAndSaveAccessor<>());
	}

	@Override
	public EditiqueResultat getCopieConformeConfirmationDelai(DelaiDeclaration delai) throws EditiqueException {
		return editiqueService.getPDFDeDocumentDepuisArchive(delai.getDeclaration().getTiers().getNumero(),
		                                                     getTypeDocumentEditique(delai),
		                                                     delai.getCleArchivageCourrier());
	}

	private TypeDocumentEditique getTypeDocumentEditique(DelaiDeclaration delai) {
		final Declaration declaration = delai.getDeclaration();
		if (declaration instanceof DeclarationImpotOrdinairePP || declaration instanceof DeclarationImpotSource) {
			return TypeDocumentEditique.CONFIRMATION_DELAI;
		}

		if (declaration instanceof QuestionnaireSNC) {
			return delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE ? TypeDocumentEditique.ACCORD_DELAI_QSNC : TypeDocumentEditique.REFUS_DELAI_QSNC;
		}

		if (!(declaration instanceof DeclarationImpotOrdinairePM)) {
			throw new IllegalArgumentException("Délai " + delai.getId() + " sur une déclaration non-supportée : " + declaration.getClass().getName());
		}

		if (delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
			if (delai.isSursis()) {
				return TypeDocumentEditique.SURSIS;
			}
			else {
				return TypeDocumentEditique.ACCORD_DELAI_PM;
			}
		}
		else if (delai.getEtat() == EtatDelaiDocumentFiscal.REFUSE) {
			return TypeDocumentEditique.REFUS_DELAI_PM;
		}
		else {
			throw new IllegalArgumentException("Délai " + delai.getId() + " sans document (etat " + delai.getEtat() + ")");
		}
	}

	@Override
	public ImportCodesSegmentResults importerCodesSegment(List<ContribuableAvecCodeSegment> input, StatusManager s) {
		final ImportCodesSegmentProcessor processor = new ImportCodesSegmentProcessor(hibernateTemplate, transactionManager, tiersService, adresseService);
		return processor.run(input, s);
	}

	@Override
	public EnvoiDIsPMResults envoyerDIsPMEnMasse(int periodeFiscale,
	                                             CategorieEnvoiDIPM categorieEnvoi,
	                                             RegDate dateLimiteBouclements,
	                                             @Nullable Integer nbMaxEnvois,
	                                             RegDate dateTraitement,
	                                             int nbThreads,
	                                             StatusManager statusManager) throws DeclarationException {
		final EnvoiDeclarationsPMProcessor processor = new EnvoiDeclarationsPMProcessor(hibernateTemplate, periodeDAO,
		                                                                                this, assujettissementService,
		                                                                                tailleLot, transactionManager, parametres, ticketService);
		return processor.run(periodeFiscale, categorieEnvoi, dateLimiteBouclements, nbMaxEnvois, dateTraitement, nbThreads, statusManager);
	}

	@Override
	public EnvoiSommationsDIsPMResults envoyerSommationsPM(RegDate dateTraitement, Integer nombreMax, StatusManager statusManager) {
		final EnvoiSommationsDIsPMProcessor processor = new EnvoiSommationsDIsPMProcessor(hibernateTemplate, diDAO, delaisService, this, tiersService, transactionManager,
		                                                                                  periodeImpositionService, adresseService);

		return processor.run(dateTraitement, nombreMax, statusManager);
	}

	/**
	 * Code de routage pour la personne morale :<BR />
	 * <ul>
	 * <li>1 : PM Vaudoise</li>
	 * <li>2 : APM non Exonéré à IBC</li>
	 * <li>3 : PM HS (Hors Suisse)</li>
	 * <li>4 : PM Holding</li>
	 * <li>5 : PM HC (Hors Canton)</li>
	 * <li>6 : SNC (société en nom collectif)</li>
	 * <li>7 : APM Exonéré à IBC</li>
	 * </ul>
	 *
	 * @param entreprise    entreprise considérée
	 * @param dateReference date de fin de la période d'imposition
	 * @param typeDocument  type de document considéré
	 * @return
	 * @throws DeclarationException
	 */
	@Override
	public EnumCodeRoutageDI computeCodeRoutage(Entreprise entreprise, RegDate dateReference, TypeDocument typeDocument) throws DeclarationException {

		// dépendant du régime fiscal vaudois à la date
		final RegimeFiscal vd = DateRangeHelper.rangeAt(entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD), dateReference);
		if (vd == null) {
			// pas de régime fiscal... on fait comment ?
			throw new DeclarationException("Pas de régime fiscal vaudois à la date de référence.");
		}

		final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipalAvant(dateReference);
		if (ffp == null) {
			throw new DeclarationException("Pas de for fiscal principal avant la date de référence.");
		}

		// APM -> "2" ou "7"
		if (typeDocument == TypeDocument.DECLARATION_IMPOT_APM_BATCH || typeDocument == TypeDocument.DECLARATION_IMPOT_APM_LOCAL) {
			final List<ModeExonerationHisto> exonerations = regimeFiscalService.getExonerations(entreprise, GenreImpotExoneration.IBC);
			final ModeExonerationHisto exonerationValide = DateRangeHelper.rangeAt(exonerations, dateReference);
			return exonerationValide != null && Arrays.asList(ModeExoneration.TOTALE, ModeExoneration.DE_FAIT).contains(exonerationValide.getModeExoneration()) ? APM_EXONEREE : APM_NON_EXONEREE;
		}

		// autre chose que "PM" -> boum !
		if (typeDocument != TypeDocument.DECLARATION_IMPOT_PM_BATCH && typeDocument != TypeDocument.DECLARATION_IMPOT_PM_LOCAL) {
			throw new IllegalArgumentException("Type de document absent ou non-supporté dans l'envoi des déclarations d'impôt PM : " + typeDocument);
		}

		// PM société de base ou holding -> "4"
		if (CODES_REGIMES_FISCAUX_SOCIETE_BASE_HOLDING.contains(vd.getCode())) {
			return PM_HOLDING;
		}

		//PM société en nom Collectif SNC -> "6"
		if (CODE_REGIME_FISCAL_SNC.equals(vd.getCode())) {
			return PM_SNC;
		}

		// PM vaudoise -> "1"
		if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return EnumCodeRoutageDI.PM_VAUDOISE;
		}

		// PM HC (Hors Canton) -> "5"
		if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
			return PM_HORS_CANTON;
		}

		// PM HS (Hors Suisse) -> "3"
		return PM_HORS_SUISSE;
	}
}
