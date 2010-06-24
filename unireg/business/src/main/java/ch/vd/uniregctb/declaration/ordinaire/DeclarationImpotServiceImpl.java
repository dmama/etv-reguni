package ch.vd.uniregctb.declaration.ordinaire;

import javax.jms.JMSException;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationImpotServiceImpl implements DeclarationImpotService {

	// private final Logger LOGGER = Logger.getLogger(DeclarationImpotServiceImpl.class);

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

	private ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper;

	private ImpressionSommationDIHelper impressionSommationDIHelper;

	private TiersService tiersService;

	private ParametreAppService parametres;

	private int tailleLot = 100; // valeur par défaut

	public DeclarationImpotServiceImpl() {
	}

	public DeclarationImpotServiceImpl(EditiqueCompositionService editiqueCompositionService, HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO,
			TacheDAO tacheDAO, ModeleDocumentDAO modeleDAO, DelaisService delaisService, ServiceInfrastructureService infraService,
			TiersService tiersService, ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper, PlatformTransactionManager transactionManager,
			ParametreAppService parametres) {
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

	/**
	 * Pour le testing uniquement
	 */
	protected void setTailleLot(int tailleLot) {
		this.tailleLot = tailleLot;
	}

	/**
	 * {@inheritDoc}
	 */
	public DeterminationDIsResults determineDIsAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, StatusManager status)
			throws DeclarationException {

		final DeterminationDIsAEmettreProcessor processer = new DeterminationDIsAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO,
				parametres, tiersService, transactionManager);
		return processer.run(anneePeriode, dateTraitement, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public EnvoiDIsResults envoyerDIsEnMasse(int anneePeriode, TypeContribuableDI type, Long noCtbMin, Long noCtbMax, int nbMax, RegDate dateTraitement, StatusManager status)
			throws DeclarationException {

		final EnvoiDIsEnMasseProcessor processor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO,
				delaisService, this, tailleLot, transactionManager);
		return processor.run(anneePeriode, type, noCtbMin, noCtbMax, nbMax, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public StatistiquesDIs produireStatsDIs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {

		final ProduireStatsDIsProcessor processor = new ProduireStatsDIsProcessor(hibernateTemplate, infraService, transactionManager, diDAO);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public StatistiquesCtbs produireStatsCtbs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {

		final ProduireStatsCtbsProcessor processor = new ProduireStatsCtbsProcessor(hibernateTemplate, infraService, tiersService, transactionManager);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public ListeDIsNonEmises produireListeDIsNonEmises(Integer anneePeriode, RegDate dateTraitement, StatusManager status)
			throws DeclarationException {
		final ProduireListeDIsNonEmisesProcessor processor = new ProduireListeDIsNonEmisesProcessor(hibernateTemplate, periodeDAO, modeleDAO,
				tacheDAO, tiersService, delaisService, this, transactionManager, parametres);
		return processor.run(anneePeriode, dateTraitement, status);
	}


	/**
	 * {@inheritDoc}
	 */
	public EchoirDIsResults echoirDIsHorsDelai(RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final EchoirDIsProcessor processor = new EchoirDIsProcessor(hibernateTemplate, delaisService, this, transactionManager);
		return processor.run(dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public EditiqueResultat envoiDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException {
		EditiqueResultat resultat;
		try {
			resultat = editiqueCompositionService.imprimeDIOnline(declaration, dateEvenement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		catch (JMSException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalEnvoiDI((Contribuable) declaration.getTiers(), declaration, dateEvenement);
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument,
			List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException {
		EditiqueResultat resultat;
		try {
			resultat = editiqueCompositionService.imprimeDIOnline(declaration, dateEvenement, typeDocument, annexes, true);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		catch (JMSException e) {
			throw new DeclarationException(e);
		}
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 * @throws AdressesResolutionException
	 */
	public void envoiDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException {
		try {
			editiqueCompositionService.imprimeDIForBatch(declaration, dateEvenement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalEnvoiDI((Contribuable) declaration.getTiers(), declaration, dateEvenement);
	}

	/**
	 * {@inheritDoc}
	 */
	public void envoiSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws DeclarationException {
		try {
			editiqueCompositionService.imprimeSommationDIForBatch(declaration,miseSousPliImpossible, dateEvenement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalSommationDI((Contribuable) declaration.getTiers(), declaration, dateEvenement);
	}

	/**
	 * {@inheritDoc}
	 */
	public void echoirDI(DeclarationImpotOrdinaire declaration, RegDate dateTraitement) {
		EtatDeclaration etat = new EtatDeclaration();
		etat.setDateObtention(dateTraitement);
		etat.setEtat(TypeEtatDeclaration.ECHUE);
		declaration.addEtat(etat);
		evenementFiscalService.publierEvenementFiscalEcheanceDI((Contribuable)declaration.getTiers(), declaration, dateTraitement);
	}

	/**
	 * Retour d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire retourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, final RegDate dateEvenement) {
		if (!dateEvenement.equals(di.getDateRetour())) {
			if (di.getDateRetour() != null) {
				di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE).setAnnule(true);
			}
			EtatDeclaration etat = new EtatDeclaration();
			etat.setEtat(TypeEtatDeclaration.RETOURNEE);
			etat.setDateObtention(dateEvenement);
			di.addEtat(etat);

			evenementFiscalService.publierEvenementFiscalRetourDI(contribuable, di, dateEvenement);
		}
		return di;
	}

	/**
	 * Sommation d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire sommationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		DeclarationImpotOrdinaire diRtr = null;
		// TODO (fnr) Factorisation de la sommation d'une DI
		evenementFiscalService.publierEvenementFiscalSommationDI(contribuable, di, dateEvenement);
		return diRtr;
	}

	/**
	 * Taxation d'office
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire taxationOffice(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		DeclarationImpotOrdinaire diRtr = null;
		// TODO (fde) Factorisation de la taxation d'office
		evenementFiscalService.publierEvenementFiscalTaxationOffice(contribuable, diRtr, dateEvenement);
		return diRtr;
	}

	/**
	 * Annulation d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire annulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		di.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationDI(contribuable, di, dateEvenement);
		return di;
	}

	/**
	 * {@inheritDoc}
	 */
	public EnvoiSommationsDIsResults envoyerSommations(final RegDate dateTraitement, final boolean miseSousPliImpossible, final Integer nombreMax, StatusManager statusManager) {
		final DeclarationImpotService diService = this;
		EnvoiSommationsDIsProcessor processor = new EnvoiSommationsDIsProcessor(hibernateTemplate, diDAO, delaisService, diService, transactionManager);
		return processor.run(dateTraitement, miseSousPliImpossible, nombreMax, statusManager);


	}

	public byte[] getCopieConformeSommationDI(DeclarationImpotOrdinaire di) throws EditiqueException {
		String nomDocument = construitIdArchivageSommationDI(di);
		byte[] pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument);
		if (pdf == null) {
			nomDocument = construitAncienIdArchivageSommationDI(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument);
		}
		if (pdf == null) {
			nomDocument = construitAncienIdArchivageSommationDIPourOnLine(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument);
		}
		return pdf;
	}

	/**
	 * Imprime les chemises TO pour les DIs échues pour lesquelle ces chemises
	 * n'ont pas encore été imprimées
	 */
	public ImpressionChemisesTOResults envoiChemisesTaxationOffice(int nombreMax, Integer noColOid, StatusManager status) {
		final ImpressionChemisesTOProcessor processor = new ImpressionChemisesTOProcessor(hibernateTemplate, diDAO, transactionManager, editiqueCompositionService, infraService);
		return processor.run(nombreMax, noColOid, status);
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
	public DemandeDelaiCollectiveResults traiterDemandeDelaiCollective(List<Long> ids, int annee, RegDate dateDelai,
			RegDate dateTraitement, StatusManager s) {
		DemandeDelaiCollectiveProcessor processor = new DemandeDelaiCollectiveProcessor(periodeDAO, hibernateTemplate, transactionManager);
		return processor.run(ids, annee, dateDelai, dateTraitement, s);
	}
}
