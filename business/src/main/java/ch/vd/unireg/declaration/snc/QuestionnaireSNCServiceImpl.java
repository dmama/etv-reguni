package ch.vd.unireg.declaration.snc;

import javax.jms.JMSException;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.EtatDeclarationRappelee;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.QuestionnaireSNCDAO;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService.PeriodeContext;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.validation.ValidationService;

public class QuestionnaireSNCServiceImpl implements QuestionnaireSNCService {

	private ParametreAppService parametreAppService;
	private PlatformTransactionManager transactionManager;
	private PeriodeFiscaleDAO periodeDAO;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ValidationService validationService;
	private TacheDAO tacheDAO;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private TicketService ticketService;
	private QuestionnaireSNCDAO questionnaireSNCDAO;
	private DelaisService delaisService;
	private EditiqueCompositionService editiqueCompositionService;
	private EditiqueService editiqueService;
	private ImpressionRappelQuestionnaireSNCHelper impressionRappelHelper;
	private EvenementFiscalService evenementFiscalService;
	private PeriodeExploitationService periodeExploitationService;

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setPeriodeDAO(PeriodeFiscaleDAO periodeDAO) {
		this.periodeDAO = periodeDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	public void setQuestionnaireSNCDAO(QuestionnaireSNCDAO questionnaireSNCDAO) {
		this.questionnaireSNCDAO = questionnaireSNCDAO;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setImpressionRappelHelper(ImpressionRappelQuestionnaireSNCHelper impressionRappelHelper) {
		this.impressionRappelHelper = impressionRappelHelper;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setPeriodeExploitationService(PeriodeExploitationService periodeExploitationService) {
		this.periodeExploitationService = periodeExploitationService;
	}

	@Override
	public DeterminationQuestionnairesSNCResults determineQuestionnairesAEmettre(int periodeFiscale, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException {
		final DeterminationQuestionnairesSNCAEmettreProcessor processor = new DeterminationQuestionnairesSNCAEmettreProcessor(parametreAppService, transactionManager, periodeDAO, hibernateTemplate, tiersService,
		                                                                                                                      adresseService, validationService, tacheDAO, this);
		return processor.run(periodeFiscale, dateTraitement, nbThreads, statusManager);
	}

	@Override
	public EnvoiQuestionnairesSNCEnMasseResults envoiQuestionnairesSNCEnMasse(int periodeFiscale, RegDate dateTraitement, @Nullable Integer nbMaxEnvois, StatusManager statusManager) {
		final EnvoiQuestionnairesSNCEnMasseProcessor processor = new EnvoiQuestionnairesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, tacheDAO, this, periodeFiscaleDAO, ticketService);
		return processor.run(periodeFiscale, dateTraitement, nbMaxEnvois, statusManager);
	}

	@Override
	public EnvoiRappelsQuestionnairesSNCResults envoiRappelsQuestionnairesSNCEnMasse(RegDate dateTraitement, @Nullable Integer periodeFiscale, @Nullable Integer nbMaxEnvois, StatusManager statusManager) {
		final EnvoiRappelsQuestionnairesSNCProcessor processor = new EnvoiRappelsQuestionnairesSNCProcessor(transactionManager, hibernateTemplate, questionnaireSNCDAO, delaisService, this);
		return processor.run(dateTraitement, periodeFiscale, nbMaxEnvois, statusManager);
	}

	@NotNull
	@Override
	public Set<Integer> getPeriodesFiscalesTheoriquementCouvertes(Entreprise entreprise, boolean pourEmissionAutoSeulement) {
		return periodeExploitationService.determinePeriodesExploitation(entreprise, pourEmissionAutoSeulement ? PeriodeContext.ENVOI_AUTO : PeriodeContext.THEORIQUE);
	}

	@Override
	public EditiqueResultat envoiQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateEvenement) throws DeclarationException {
		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeQuestionnaireSNCOnline(questionnaire);
			evenementFiscalService.publierEvenementFiscalEmissionQuestionnaireSNC(questionnaire, dateEvenement);
			return resultat;
		}
		catch (EditiqueException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public void envoiQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateEvenement) throws DeclarationException {
		try {
			editiqueCompositionService.imprimerQuestionnaireSNCForBatch(questionnaire);
			evenementFiscalService.publierEvenementFiscalEmissionQuestionnaireSNC(questionnaire, dateEvenement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EditiqueResultat envoiDuplicataQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws DeclarationException {
		try {
			return editiqueCompositionService.imprimeDuplicataQuestionnaireSNCOnline(questionnaire);
		}
		catch (EditiqueException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EditiqueResultat envoiRappelQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateTraitement) throws DeclarationException {
		final EtatDeclarationRappelee etat = new EtatDeclarationRappelee(dateTraitement, dateTraitement);
		AddAndSaveHelper.addAndSave(questionnaire, etat, questionnaireSNCDAO::save, new EtatDocumentFiscalAddAndSaveAccessor<>());

		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeRappelQuestionnaireSNCOnline(questionnaire, dateTraitement);
			evenementFiscalService.publierEvenementFiscalRappelQuestionnaireSNC(questionnaire, dateTraitement);
			return resultat;
		}
		catch (EditiqueException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public void envoiRappelQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateTraitement, RegDate dateExpedition) throws DeclarationException {
		final EtatDeclarationRappelee etat = new EtatDeclarationRappelee(dateTraitement, dateExpedition);
		AddAndSaveHelper.addAndSave(questionnaire, etat, questionnaireSNCDAO::save, new EtatDocumentFiscalAddAndSaveAccessor<>());

		try {
			editiqueCompositionService.imprimeRappelQuestionnaireSNCForBatch(questionnaire, dateTraitement, dateExpedition);
			evenementFiscalService.publierEvenementFiscalRappelQuestionnaireSNC(questionnaire, dateTraitement);
		}
		catch (EditiqueException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public EditiqueResultat getCopieConformeRappelQuestionnaireSNC(QuestionnaireSNC questionnaire) throws EditiqueException {
		final String cleArchivage = impressionRappelHelper.construitCleArchivageDocument(questionnaire);
		return editiqueService.getPDFDeDocumentDepuisArchive(questionnaire.getTiers().getNumero(), impressionRappelHelper.getTypeDocumentEditique(questionnaire), cleArchivage);
	}

	@Override
	public void quittancerQuestionnaire(QuestionnaireSNC questionnaire, RegDate dateRetour, String source) {
		final EtatDeclarationRetournee retour = new EtatDeclarationRetournee(dateRetour, source);
		questionnaire.addEtat(retour);

		evenementFiscalService.publierEvenementFiscalQuittancementQuestionnaireSNC(questionnaire, dateRetour);
	}

	@Override
	public void annulerQuestionnaire(QuestionnaireSNC questionnaire) {
		questionnaire.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationQuestionnaireSNC(questionnaire);
	}
}
