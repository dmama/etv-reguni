package ch.vd.uniregctb.declaration.ordinaire;

import javax.jms.JMSException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ListeNoteResults;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.evenement.di.EvenementDeclarationException;
import ch.vd.uniregctb.evenement.di.EvenementDeclarationSender;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.validation.ValidationService;

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

	private AdresseService adresseService;

	private ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper;

	private ImpressionSommationDIHelper impressionSommationDIHelper;

	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	private TiersService tiersService;

	private ParametreAppService parametres;

	private ValidationService validationService;

	private EvenementDeclarationSender evenementDeclarationSender;

	private int tailleLot = 100; // valeur par défaut

	private static final String CONTEXTE_COPIE_CONFORME_SOMMATION = "SommationDI";

	private static final String CONTEXTE_COPIE_CONFORME_DELAI = "ConfirmationDelai";

	private ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper;

	public DeclarationImpotServiceImpl() {
	}

	public DeclarationImpotServiceImpl(EditiqueCompositionService editiqueCompositionService, HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO,
	                                   TacheDAO tacheDAO, ModeleDocumentDAO modeleDAO, DelaisService delaisService, ServiceInfrastructureService infraService,
	                                   TiersService tiersService, ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper, PlatformTransactionManager transactionManager,
	                                   ParametreAppService parametres, ServiceCivilCacheWarmer serviceCivilCacheWarmer, ValidationService validationService,
	                                   EvenementFiscalService evenementFiscalService, EvenementDeclarationSender evenementDeclarationSender) {
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
				parametres, tiersService, transactionManager, validationService);
		return processer.run(anneePeriode, dateTraitement, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvoiDIsResults envoyerDIsEnMasse(int anneePeriode, CategorieEnvoiDI categorie, @Nullable Long noCtbMin, @Nullable Long noCtbMax, int nbMax, RegDate dateTraitement, boolean exclureDecedes,
	                                         @Nullable StatusManager status)
			throws DeclarationException {

		final EnvoiDIsEnMasseProcessor processor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO,
				delaisService, this, tailleLot, transactionManager, parametres, serviceCivilCacheWarmer);
		return processor.run(anneePeriode, categorie, noCtbMin, noCtbMax, nbMax, dateTraitement, exclureDecedes, status);
	}

	@Override
	public EnvoiAnnexeImmeubleResults envoyerAnnexeImmeubleEnMasse(int anneePeriode, RegDate dateTraitement,
	                                                               List<ContribuableAvecImmeuble> listeCtb, int nbMax, StatusManager status) throws DeclarationException {
		final EnvoiAnnexeImmeubleEnMasseProcessor processor = new EnvoiAnnexeImmeubleEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO,
				this, tailleLot, transactionManager, serviceCivilCacheWarmer);
		return processor.run(anneePeriode, listeCtb, nbMax, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatistiquesDIs produireStatsDIs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {

		final ProduireStatsDIsProcessor processor = new ProduireStatsDIsProcessor(hibernateTemplate, infraService, transactionManager, diDAO);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StatistiquesCtbs produireStatsCtbs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException {

		final ProduireStatsCtbsProcessor processor = new ProduireStatsCtbsProcessor(hibernateTemplate, infraService, tiersService, transactionManager);
		return processor.run(anneePeriode, dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListeDIsNonEmises produireListeDIsNonEmises(Integer anneePeriode, RegDate dateTraitement, StatusManager status)
			throws DeclarationException {
		final ProduireListeDIsNonEmisesProcessor processor = new ProduireListeDIsNonEmisesProcessor(hibernateTemplate, periodeDAO, modeleDAO,
				tacheDAO, tiersService, delaisService, this, transactionManager, parametres, serviceCivilCacheWarmer, validationService);
		return processor.run(anneePeriode, dateTraitement, status);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EchoirDIsResults echoirDIsHorsDelai(RegDate dateTraitement, StatusManager status) throws DeclarationException {
		final EchoirDIsProcessor processor = new EchoirDIsProcessor(hibernateTemplate, delaisService, this, transactionManager);
		return processor.run(dateTraitement, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EditiqueResultat envoiDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException {

		final Contribuable ctb = (Contribuable) declaration.getTiers();

		EditiqueResultat resultat;
		try {
			resultat = editiqueCompositionService.imprimeDIOnline(declaration, dateEvenement);
			evenementFiscalService.publierEvenementFiscalEnvoiDI(ctb, declaration, dateEvenement);
			// TODO (msi) ajouter le code de routage
			evenementDeclarationSender.sendEmissionEvent(ctb.getNumero(), declaration.getPeriode().getAnnee(), dateEvenement, declaration.getCodeControle(), "");
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		catch (JMSException e) {
			throw new DeclarationException(e);
		}
		catch (EvenementDeclarationException e) {
			throw new DeclarationException(e);
		}

		// [UNIREG-2705] il est maintenant possible de créer des déclarations déjà retournées (et pas seulement pour les indigents) 
		final EtatDeclaration etatRetour = declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
		if (etatRetour != null) {
			evenementFiscalService.publierEvenementFiscalRetourDI(ctb, declaration, etatRetour.getDateObtention());
		}
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws
			DeclarationException {
		final EditiqueResultat resultat;
		try {
			resultat = editiqueCompositionService.imprimeDuplicataDIOnline(declaration, dateEvenement, typeDocument, annexes);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		catch (JMSException e) {
			throw new DeclarationException(e);
		}
		return resultat;
	}

	@Override
	public void envoiDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException {
		try {
			final Contribuable tiers = (Contribuable) declaration.getTiers();
			editiqueCompositionService.imprimeDIForBatch(declaration, dateEvenement);
			evenementFiscalService.publierEvenementFiscalEnvoiDI(tiers, declaration, dateEvenement);
			// TODO (msi) ajouter le code de routage
			evenementDeclarationSender.sendEmissionEvent(tiers.getNumero(), declaration.getPeriode().getAnnee(), dateEvenement, declaration.getCodeControle(), "");
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		catch (EvenementDeclarationException e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void envoiSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws DeclarationException {
		try {
			editiqueCompositionService.imprimeSommationDIForBatch(declaration, miseSousPliImpossible, dateEvenement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
		evenementFiscalService.publierEvenementFiscalSommationDI((Contribuable) declaration.getTiers(), declaration, dateEvenement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void echoirDI(DeclarationImpotOrdinaire declaration, RegDate dateTraitement) {
		EtatDeclaration etat = new EtatDeclarationEchue();
		etat.setDateObtention(dateTraitement);
		declaration.addEtat(etat);
		evenementFiscalService.publierEvenementFiscalEcheanceDI((Contribuable) declaration.getTiers(), declaration, dateTraitement);
	}

	/**
	 * Retour d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	@Override
	public DeclarationImpotOrdinaire retourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, final RegDate dateEvenement) {
		if (!dateEvenement.equals(di.getDateRetour())) {
			if (di.getDateRetour() != null) {
				di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE).setAnnule(true);
			}
			final EtatDeclaration etat = new EtatDeclarationRetournee();
			etat.setDateObtention(dateEvenement);
			di.addEtat(etat);

			evenementFiscalService.publierEvenementFiscalRetourDI(contribuable, di, dateEvenement);
		}
		return di;
	}

	/**
	 * Annulation d'une DI
	 *
	 * @param contribuable  le contribuable qui possède la déclaration à annuler
	 * @param di            la déclaration qui doit être annulée
	 * @param dateEvenement la date d'annulation
	 * @return la déclaration nouvellement annulée
	 */
	@Override
	public DeclarationImpotOrdinaire annulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		di.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationDI(contribuable, di, dateEvenement);
		try {
			evenementDeclarationSender.sendAnnulationEvent(contribuable.getNumero(), di.getPeriode().getAnnee(), dateEvenement);
		}
		catch (EvenementDeclarationException e) {
			throw new RuntimeException(e);
		}
		return di;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnvoiSommationsDIsResults envoyerSommations(RegDate dateTraitement, boolean miseSousPliImpossible, int nombreMax, StatusManager statusManager) {
		final DeclarationImpotService diService = this;
		EnvoiSommationsDIsProcessor processor = new EnvoiSommationsDIsProcessor(hibernateTemplate, diDAO, delaisService, diService, tiersService, transactionManager);
		return processor.run(dateTraitement, miseSousPliImpossible, nombreMax, statusManager);
	}

	@Override
	public InputStream getCopieConformeSommationDI(DeclarationImpotOrdinaire di) throws EditiqueException {
		String nomDocument = construitIdArchivageSommationDI(di);
		InputStream pdf =
				editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument, CONTEXTE_COPIE_CONFORME_SOMMATION);
		if (pdf == null) {
			nomDocument = construitAncienIdArchivageSommationDI(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument, CONTEXTE_COPIE_CONFORME_SOMMATION);
		}
		if (pdf == null) {
			nomDocument = construitAncienIdArchivageSommationDIPourOnLine(di);
			pdf = editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), ImpressionSommationDIHelperImpl.TYPE_DOCUMENT_SOMMATION_DI, nomDocument, CONTEXTE_COPIE_CONFORME_SOMMATION);
		}
		return pdf;
	}

	/**
	 * Imprime les chemises TO pour les DIs échues pour lesquelle ces chemises n'ont pas encore été imprimées
	 */
	@Override
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
	@Override
	public DemandeDelaiCollectiveResults traiterDemandeDelaiCollective(List<Long> ids, int annee, RegDate dateDelai,
	                                                                   RegDate dateTraitement, StatusManager s) {
		DemandeDelaiCollectiveProcessor processor = new DemandeDelaiCollectiveProcessor(periodeDAO, hibernateTemplate, transactionManager);
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
	public int envoiAnnexeImmeubleForBatch(InformationsDocumentAdapter infoDocuments, Set<ModeleFeuilleDocument> listeModele, RegDate dateTraitement, int nombreAnnexesImmeuble) throws
			DeclarationException {
		try {
			return editiqueCompositionService.imprimeAnnexeImmeubleForBatch(infoDocuments, listeModele, dateTraitement, nombreAnnexesImmeuble);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
	}


	private static interface EntityAccessor<T extends DeclarationImpotOrdinaire, E extends HibernateEntity> {
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
				keys = new HashSet<Object>(entities.size());
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
	public InputStream getCopieConformeConfirmationDelai(DelaiDeclaration delai) throws EditiqueException {
		String nomDocument = construitIdArchivageConfirmationDelai(delai);
		InputStream pdf =
				editiqueService.getPDFDeDocumentDepuisArchive(delai.getDeclaration().getTiers().getNumero(),
						ImpressionConfirmationDelaiHelperImpl.TYPE_DOCUMENT_CONFIRMATION_DELAI, nomDocument, CONTEXTE_COPIE_CONFORME_DELAI);
		return pdf;
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
		final ImportCodesSegmentProcessor processor = new ImportCodesSegmentProcessor(hibernateTemplate, transactionManager, tiersService);
		return processor.run(input, s);
	}
}
