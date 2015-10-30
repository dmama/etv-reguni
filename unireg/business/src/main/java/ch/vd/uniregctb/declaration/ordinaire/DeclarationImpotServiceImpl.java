package ch.vd.uniregctb.declaration.ordinaire;

import javax.jms.JMSException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiDeclarationsPMProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pm.TypeDeclarationImpotPM;
import ch.vd.uniregctb.declaration.ordinaire.pp.ContribuableAvecCodeSegment;
import ch.vd.uniregctb.declaration.ordinaire.pp.ContribuableAvecImmeuble;
import ch.vd.uniregctb.declaration.ordinaire.pp.DemandeDelaiCollectiveProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.DemandeDelaiCollectiveResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.DeterminationDIsAEmettreProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.DeterminationDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EchoirDIsProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.EchoirDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiAnnexeImmeubleEnMasseProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiDIsEnMasseProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiSommationsDIsProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiSommationsDIsResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImportCodesSegmentProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImportCodesSegmentResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionConfirmationDelaiHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionConfirmationDelaiHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionDeclarationImpotOrdinaireHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionSommationDIHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeDIsPPNonEmises;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeNoteProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.declaration.ordinaire.pp.ProduireListeDIsNonEmisesProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.ProduireStatsCtbsProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.ProduireStatsDIsProcessor;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesDIs;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.evenement.di.EvenementDeclarationException;
import ch.vd.uniregctb.evenement.di.EvenementDeclarationSender;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.ValidationService;

public class DeclarationImpotServiceImpl implements DeclarationImpotService {

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
	private ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper;
	private ImpressionSommationDIHelper impressionSommationDIHelper;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private TiersService tiersService;
	private ParametreAppService parametres;
	private ValidationService validationService;
	private EvenementDeclarationSender evenementDeclarationSender;
	private ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper;
	private PeriodeImpositionService periodeImpositionService;
	private AssujettissementService assujettissementService;
	private TicketService ticketService;

	private Set<String> sourcesMonoQuittancement;

	private int tailleLot = 100; // valeur par défaut

	public DeclarationImpotServiceImpl() {
	}

	public DeclarationImpotServiceImpl(EditiqueCompositionService editiqueCompositionService, HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO,
	                                   TacheDAO tacheDAO, ModeleDocumentDAO modeleDAO, DelaisService delaisService, ServiceInfrastructureService infraService,
	                                   TiersService tiersService, ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper, PlatformTransactionManager transactionManager,
	                                   ParametreAppService parametres, ServiceCivilCacheWarmer serviceCivilCacheWarmer, ValidationService validationService,
	                                   EvenementFiscalService evenementFiscalService, EvenementDeclarationSender evenementDeclarationSender, PeriodeImpositionService periodeImpositionService,
	                                   AssujettissementService assujettissementService, TicketService ticketService) {
		this.editiqueCompositionService = editiqueCompositionService;
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.tacheDAO = tacheDAO;
		this.modeleDAO = modeleDAO;
		this.delaisService = delaisService;
		this.infraService = infraService;
		this.tiersService = tiersService;
		this.impressionDIHelper = impressionDIHelper;
		this.transactionManager = transactionManager;
		this.parametres = parametres;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.validationService = validationService;
		this.evenementFiscalService = evenementFiscalService;
		this.evenementDeclarationSender = evenementDeclarationSender;
		this.periodeImpositionService = periodeImpositionService;
		this.assujettissementService = assujettissementService;
		this.ticketService = ticketService;
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

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public ImpressionDeclarationImpotOrdinaireHelper getImpressionDIHelper() {
		return impressionDIHelper;
	}

	public void setImpressionDIHelper(ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper) {
		this.impressionDIHelper = impressionDIHelper;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setImpressionSommationDIHelper(ImpressionSommationDIHelper impressionSommationDIHelper) {
		this.impressionSommationDIHelper = impressionSommationDIHelper;
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

	public void setEvenementDeclarationSender(EvenementDeclarationSender evenementDeclarationSender) {
		this.evenementDeclarationSender = evenementDeclarationSender;
	}

	public void setImpressionConfirmationDelaiHelper(ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper) {
		this.impressionConfirmationDelaiHelper = impressionConfirmationDelaiHelper;
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

	/**
	 * Pour le testing uniquement
	 */
	protected void setTailleLot(int tailleLot) {
		this.tailleLot = tailleLot;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DeterminationDIsResults determineDIsAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, @Nullable StatusManager status)
			throws DeclarationException {

		final DeterminationDIsAEmettreProcessor processer = new DeterminationDIsAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO,
				parametres, tiersService, transactionManager, validationService, periodeImpositionService, adresseService);
		return processer.run(anneePeriode, dateTraitement, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvoiDIsPPResults envoyerDIsPPEnMasse(int anneePeriode, CategorieEnvoiDI categorie, @Nullable Long noCtbMin, @Nullable Long noCtbMax, int nbMax, RegDate dateTraitement, boolean exclureDecedes,
	                                             int nbThreads, @Nullable StatusManager status) throws DeclarationException {

		final EnvoiDIsEnMasseProcessor processor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO,
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatistiquesDIs produireStatsDIs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {

		final ProduireStatsDIsProcessor processor = new ProduireStatsDIsProcessor(hibernateTemplate, infraService, transactionManager, diDAO, assujettissementService, tiersService, adresseService);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatistiquesCtbs produireStatsCtbs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireStatsCtbsProcessor processor = new ProduireStatsCtbsProcessor(hibernateTemplate, infraService, tiersService, transactionManager, assujettissementService, adresseService);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListeDIsPPNonEmises produireListeDIsNonEmises(Integer anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final ProduireListeDIsNonEmisesProcessor processor = new ProduireListeDIsNonEmisesProcessor(hibernateTemplate, periodeDAO, modeleDAO,
		                                                                                            tacheDAO, tiersService, delaisService, this, transactionManager, parametres,
		                                                                                            serviceCivilCacheWarmer, validationService, periodeImpositionService,
		                                                                                            adresseService, ticketService);
		return processor.run(anneePeriode, dateTraitement, status);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EchoirDIsResults echoirDIsHorsDelai(RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final EchoirDIsProcessor processor = new EchoirDIsProcessor(hibernateTemplate, delaisService, this, transactionManager, tiersService, adresseService);
		return processor.run(dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EditiqueResultat envoiDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws DeclarationException {

		final Contribuable ctb = declaration.getTiers();

		EditiqueResultat resultat;
		try {
			resultat = editiqueCompositionService.imprimeDIOnline(declaration);
			evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(declaration, dateEvenement);

			// [SIFISC-3103] Pour les périodes fiscales avant 2011, on n'envoie aucun événement de création de DI (pour le moment, il ne s'agit que d'ADDI)
			final int pf = declaration.getPeriode().getAnnee();
			if (pf >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
				final String codeSegmentString = Integer.toString(declaration.getCodeSegment() != null ? declaration.getCodeSegment() : VALEUR_DEFAUT_CODE_SEGMENT);
				evenementDeclarationSender.sendEmissionEvent(ctb.getNumero(), pf, dateEvenement, declaration.getCodeControle(), codeSegmentString);
			}

			// [UNIREG-2705] il est maintenant possible de créer des déclarations déjà retournées (et pas seulement pour les indigents)
			final EtatDeclaration etatRetour = declaration.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE);
			if (etatRetour != null) {
				evenementFiscalService.publierEvenementFiscalQuittancementDeclarationImpot(declaration, etatRetour.getDateObtention());
			}
		}
		catch (EditiqueException | EvenementDeclarationException | JMSException e) {
			throw new DeclarationException(e);
		}
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException {
		final EditiqueResultat resultat;
		try {
			resultat = editiqueCompositionService.imprimeDuplicataDIOnline(declaration, typeDocument, annexes);
		}
		catch (EditiqueException | JMSException e) {
			throw new DeclarationException(e);
		}
		return resultat;
	}

	@Override
	public void envoiDIForBatch(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws DeclarationException {
		try {
			final Contribuable tiers = declaration.getTiers();
			editiqueCompositionService.imprimeDIForBatch(declaration);
			evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(declaration, dateEvenement);

			final String codeSegmentString = Integer.toString(declaration.getCodeSegment() != null ? declaration.getCodeSegment() : VALEUR_DEFAUT_CODE_SEGMENT);
			evenementDeclarationSender.sendEmissionEvent(tiers.getNumero(), declaration.getPeriode().getAnnee(), dateEvenement, declaration.getCodeControle(), codeSegmentString);
		}
		catch (EditiqueException | EvenementDeclarationException e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void envoiSommationDIForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws DeclarationException {
		try {
			editiqueCompositionService.imprimeSommationDIForBatch(declaration, miseSousPliImpossible, dateEvenement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalSommationDeclarationImpot(declaration, dateEvenement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void echoirDI(DeclarationImpotOrdinairePP declaration, RegDate dateTraitement) {
		EtatDeclaration etat = new EtatDeclarationEchue();
		etat.setDateObtention(dateTraitement);
		declaration.addEtat(etat);
		evenementFiscalService.publierEvenementFiscalEcheanceDeclarationImpot(declaration, dateTraitement);
	}

	@Override
	public DeclarationImpotOrdinaire quittancementDI(Contribuable contribuable, DeclarationImpotOrdinaire di, final RegDate dateEvenement, String source, boolean evtFiscal) {
		// [SIFISC-5208] Dorénavant, on stocke scrupuleusement tous les états de quittancement de type 'retournés', *sans* annuler les états précédents.
		// [SIFISC-8436] certaines sources ne supportent pas le multi-quittancement
		if (sourcesMonoQuittancement.contains(source)) {
			for (EtatDeclaration etat : di.getEtats()) {
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
		return di;
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
	public DeclarationImpotOrdinairePP annulationDI(ContribuableImpositionPersonnesPhysiques contribuable, DeclarationImpotOrdinairePP di, @Nullable Long tacheId, RegDate dateEvenement) {
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
			if (pf >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
				evenementDeclarationSender.sendAnnulationEvent(contribuable.getNumero(), pf, dateEvenement);
			}
		}
		catch (EvenementDeclarationException e) {
			throw new RuntimeException(e);
		}
		return di;
	}

	@Override
	public void desannulationDI(ContribuableImpositionPersonnesPhysiques ctb, DeclarationImpotOrdinairePP di, RegDate dateEvenement) {

		if (!di.isAnnule()) {
			throw new IllegalArgumentException("Impossible de désannuler la déclaration n°" + di.getId() + " car elle n'est pas annulée !");
		}

		di.setAnnule(false);
		evenementFiscalService.publierEvenementFiscalEmissionDeclarationImpot(di, dateEvenement);
		try {
			// [SIFISC-3103] Pour les périodes fiscales avant 2011, on n'envoie aucun événement de désannulation de DI (pour le moment, il ne s'agit que d'ADDI)
			// [SIFISC-8598] Les DI de la PF 2011 qui ont été émises avant l'envoi de masse de début 2012 n'ont pas forcément de code de contrôle, il est donc inutile de les signaler à ADDI
			final int pf = di.getPeriode().getAnnee();
			if (pf > DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE || (pf == DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE && di.getCodeControle() != null)) {
				final String codeSegmentString = Integer.toString(di.getCodeSegment() != null ? di.getCodeSegment() : VALEUR_DEFAUT_CODE_SEGMENT);
				evenementDeclarationSender.sendEmissionEvent(ctb.getNumero(), pf, dateEvenement, di.getCodeControle(), codeSegmentString);
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
	public EnvoiSommationsDIsResults envoyerSommations(RegDate dateTraitement, boolean miseSousPliImpossible, int nombreMax, StatusManager statusManager) {
		final DeclarationImpotService diService = this;
		EnvoiSommationsDIsProcessor processor = new EnvoiSommationsDIsProcessor(hibernateTemplate, diDAO, delaisService, diService, tiersService, transactionManager, assujettissementService,
				periodeImpositionService, adresseService);
		return processor.run(dateTraitement, miseSousPliImpossible, nombreMax, statusManager);
	}

	@Override
	public EditiqueResultat getCopieConformeSommationDI(DeclarationImpotOrdinaire di) throws EditiqueException {
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

	/**
	 * Construit l'ID du document pour l'archivage
	 *
	 * @param declaration
	 * @return
	 */
	private String construitIdArchivageSommationDI(DeclarationImpotOrdinaire declaration) {
		return impressionSommationDIHelper.construitIdArchivageDocument(declaration);
	}

	/**
	 * Construit l'ID du document pour l'archivage (avant octobre 2009)
	 *
	 * @param declaration
	 * @return
	 */
	private String construitAncienIdArchivageSommationDI(DeclarationImpotOrdinaire declaration) {
		return impressionSommationDIHelper.construitAncienIdArchivageDocument(declaration);
	}


	/**
	 * Construit l'ID du document pour l'archivage pour les on-line (avant octobre 2009)
	 *
	 * @param declaration
	 * @return
	 */
	private String construitAncienIdArchivageSommationDIPourOnLine(DeclarationImpotOrdinaire declaration) {
		return impressionSommationDIHelper.construitAncienIdArchivageDocumentPourOnLine(declaration);
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DemandeDelaiCollectiveResults traiterDemandeDelaiCollective(List<Long> ids, int annee, RegDate dateDelai,
	                                                                   RegDate dateTraitement, StatusManager s) {
		DemandeDelaiCollectiveProcessor processor = new DemandeDelaiCollectiveProcessor(periodeDAO, hibernateTemplate, transactionManager, tiersService, adresseService);
		return processor.run(ids, annee, dateDelai, dateTraitement, s);
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


	private interface EntityAccessor<T extends DeclarationImpotOrdinaire, E extends HibernateEntity> {
		Collection<E> getEntities(T declaration);

		void addEntity(T declaration, E entity);

		void assertSame(E entity1, E entity2);
	}

	@SuppressWarnings({"unchecked"})
	private <T extends DeclarationImpotOrdinaire, E extends HibernateEntity> E addAndSave(T declaration, E entity, EntityAccessor<T, E> accessor) {
		if (entity.getKey() == null) {
			// pas encore persistée

			// on mémorise les clés des entités existantes
			final Set<Object> keys;
			final Collection<E> entities = accessor.getEntities(declaration);
			if (entities == null || entities.isEmpty()) {
				keys = Collections.emptySet();
			}
			else {
				keys = new HashSet<>(entities.size());
				for (E d : entities) {
					final Object key = d.getKey();
					Assert.notNull(key, "Les entités existantes doivent être déjà persistées.");
					keys.add(key);
				}
			}

			// on ajoute la nouvelle entité et on sauve le tout
			accessor.addEntity(declaration, entity);
			declaration = (T) diDAO.save(declaration);

			// rebelotte pour trouver la nouvelle entité
			E newEntity = null;
			for (E d : accessor.getEntities(declaration)) {
				if (!keys.contains(d.getKey())) {
					newEntity = d;
					break;
				}
			}

			Assert.notNull(newEntity);
			accessor.assertSame(entity, newEntity);
			entity = newEntity;
		}
		else {
			accessor.addEntity(declaration, entity);
		}

		Assert.notNull(entity.getKey());
		return entity;
	}

	private static final EntityAccessor<DeclarationImpotOrdinaire, DelaiDeclaration> DELAI_DECLARATION_ACCESSOR = new EntityAccessor<DeclarationImpotOrdinaire, DelaiDeclaration>() {
		@Override
		public Collection<DelaiDeclaration> getEntities(DeclarationImpotOrdinaire declarationImpotOrdinaire) {
			return declarationImpotOrdinaire.getDelais();
		}

		@Override
		public void addEntity(DeclarationImpotOrdinaire declaration, DelaiDeclaration d) {
			declaration.addDelai(d);
		}

		@Override
		public void assertSame(DelaiDeclaration d1, DelaiDeclaration d2) {
			Assert.isSame(d1.getDelaiAccordeAu(), d2.getDelaiAccordeAu());
			Assert.isSame(d1.getDateDemande(), d2.getDateDemande());
			Assert.isSame(d1.getDateTraitement(), d2.getDateTraitement());
		}
	};


	@Override
	public DelaiDeclaration addAndSave(DeclarationImpotOrdinaire declaration, DelaiDeclaration delai) {
		return addAndSave(declaration, delai, DELAI_DECLARATION_ACCESSOR);
	}

	@Override
	public EditiqueResultat getCopieConformeConfirmationDelai(DelaiDeclaration delai) throws EditiqueException {
		final String nomDocument = construitIdArchivageConfirmationDelai(delai);
		return editiqueService.getPDFDeDocumentDepuisArchive(delai.getDeclaration().getTiers().getNumero(), TypeDocumentEditique.CONFIRMATION_DELAI, nomDocument);
	}

	/**
	 * Construit l'ID du document pour l'archivage
	 *
	 * @param delaiDeclaration
	 * @return
	 */
	private String construitIdArchivageConfirmationDelai(DelaiDeclaration delaiDeclaration) {
		ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(delaiDeclaration.getDelaiAccordeAu(),
				delaiDeclaration.getId(), delaiDeclaration.getLogCreationDate());
		return impressionConfirmationDelaiHelper.construitIdArchivageDocument(params);
	}
	
	@Override
	public ImportCodesSegmentResults importerCodesSegment(List<ContribuableAvecCodeSegment> input, StatusManager s) {
		final ImportCodesSegmentProcessor processor = new ImportCodesSegmentProcessor(hibernateTemplate, transactionManager, tiersService, adresseService);
		return processor.run(input, s);
	}

	@Override
	public EnvoiDIsPMResults envoyerDIsPMEnMasse(int periodeFiscale,
	                                             TypeDeclarationImpotPM typeDeclaration,
	                                             RegDate dateLimiteBouclements,
	                                             @Nullable Integer nbMaxEnvois,
	                                             RegDate dateTraitement,
	                                             int nbThreads,
	                                             StatusManager statusManager) throws DeclarationException {
		final EnvoiDeclarationsPMProcessor processor = new EnvoiDeclarationsPMProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO,
		                                                                                delaisService, this, assujettissementService, periodeImpositionService,
		                                                                                tailleLot, transactionManager, parametres, adresseService, evenementFiscalService, ticketService);
		return processor.run(periodeFiscale, typeDeclaration, dateLimiteBouclements, nbMaxEnvois, dateTraitement, nbThreads, statusManager);
	}
}
