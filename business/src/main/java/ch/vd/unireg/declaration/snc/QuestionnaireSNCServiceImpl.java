package ch.vd.unireg.declaration.snc;

import javax.jms.JMSException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.DelaiDeclarationDAO;
import ch.vd.unireg.declaration.DelaiDeclarationDAOImpl;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEchue;
import ch.vd.unireg.declaration.EtatDeclarationRappelee;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.QuestionnaireSNCDAO;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationPMSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitation;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService.PeriodeContext;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.validation.ValidationService;

public class QuestionnaireSNCServiceImpl implements QuestionnaireSNCService {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionnaireSNCServiceImpl.class);

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
	private EvenementDeclarationPMSender evenementDeclarationPMSender;
	private Set<String> sourcesMonoQuittancement;
	private DelaiDeclarationDAO delaiDeclarationDAO;

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

	public void setEvenementDeclarationPMSender(EvenementDeclarationPMSender evenementDeclarationPMSender) {
		this.evenementDeclarationPMSender = evenementDeclarationPMSender;
	}

	public void setSourcesMonoQuittancement(Set<String> sourcesMonoQuittancement) {
		this.sourcesMonoQuittancement = sourcesMonoQuittancement;
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
		final List<PeriodeExploitation> periodeExploitations = periodeExploitationService.determinePeriodesExploitation(entreprise, pourEmissionAutoSeulement ? PeriodeContext.ENVOI_AUTO : PeriodeContext.THEORIQUE);
		final List<DateRange> periodes = periodeExploitations.stream().map(PeriodeExploitation::getDateRange).collect(Collectors.toList());
		return periodes
				.stream()
				.map(DateRange::getDateDebut)
				.map(RegDate::year)
				.collect(Collectors.toSet());
	}

	@Override
	public EditiqueResultat envoiQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateEvenement) throws DeclarationException {
		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimeQuestionnaireSNCOnline(questionnaire);
			evenementFiscalService.publierEvenementFiscalEmissionQuestionnaireSNC(questionnaire, dateEvenement);
			final ContribuableImpositionPersonnesMorales ctb = questionnaire.getTiers();
			// envoi du NIP à qui de droit
			if (StringUtils.isNotBlank(questionnaire.getCodeControle())) {
				final String codeSegment = questionnaire.getCodeSegment() != null ? Integer.toString(questionnaire.getCodeSegment()) : null;

				evenementDeclarationPMSender.sendEmissionQSNCEvent(ctb.getNumero(), questionnaire.getPeriode().getAnnee(), questionnaire.getNumero(), questionnaire.getCodeControle(), codeSegment);
			}

			return resultat;
		}
		catch (EditiqueException | EvenementDeclarationException | JMSException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public void envoiQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateEvenement) throws DeclarationException {
		try {
			final ContribuableImpositionPersonnesMorales snc = questionnaire.getTiers();
			editiqueCompositionService.imprimerQuestionnaireSNCForBatch(questionnaire);
			evenementFiscalService.publierEvenementFiscalEmissionQuestionnaireSNC(questionnaire, dateEvenement);
			if (StringUtils.isNotBlank(questionnaire.getCodeControle())) {
				final String codeSegment = questionnaire.getCodeSegment() != null ? Integer.toString(questionnaire.getCodeSegment()) : null;

				evenementDeclarationPMSender.sendEmissionQSNCEvent(snc.getNumero(), questionnaire.getPeriode().getAnnee(), questionnaire.getNumero(), questionnaire.getCodeControle(), codeSegment);
			}
		}
		catch (EditiqueException | EvenementDeclarationException e) {
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
		//FISCPROJ-364 s'assurer qu'une source qui ne supporte que le mono quittancement soit bien prise en compte
		if (sourcesMonoQuittancement.contains(source)) {
			for (EtatDeclaration etat : questionnaire.getEtatsDeclaration()) {
				if (!etat.isAnnule() && etat instanceof EtatDeclarationRetournee && source.equals(((EtatDeclarationRetournee) etat).getSource())) {
					etat.setAnnule(true);
				}
			}
		}

		final EtatDeclarationRetournee retour = new EtatDeclarationRetournee(dateRetour, source);
		questionnaire.addEtat(retour);

		evenementFiscalService.publierEvenementFiscalQuittancementQuestionnaireSNC(questionnaire, dateRetour);
	}

	@Override
	public void annulerQuestionnaire(QuestionnaireSNC questionnaire) throws DeclarationException {

		try {
			questionnaire.setAnnule(true);
			evenementFiscalService.publierEvenementFiscalAnnulationQuestionnaireSNC(questionnaire);
			if (StringUtils.isNotBlank(questionnaire.getCodeControle())) {
				final String codeRoutage = questionnaire.getCodeSegment() != null ? Integer.toString(questionnaire.getCodeSegment()) : Integer.toString(QuestionnaireSNCService.codeSegment);
				final Integer pf = questionnaire.getPeriode().getAnnee();

				evenementDeclarationPMSender.sendAnnulationQSNCEvent(questionnaire.getTiers().getNumero(), pf, questionnaire.getNumero(), questionnaire.getCodeControle(), codeRoutage);

			}
		}
		catch (EvenementDeclarationException e) {
			throw new DeclarationException(e);
		}
	}

	@Override
	public Long ajouterDelai(long questionnaireId, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal etatDelai) {

		final QuestionnaireSNC qsnc = questionnaireSNCDAO.get(questionnaireId);
		if (qsnc == null) {
			throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + questionnaireId);
		}

		// on ajoute le délai
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());
		delai.setDateDemande(dateDemande);
		delai.setEtat(etatDelai);
		delai.setDelaiAccordeAu(delaiAccordeAu);

		delai = AddAndSaveHelper.addAndSave(qsnc, delai, questionnaireSNCDAO::save, new DelaiDocumentFiscalAddAndSaveAccessor<>());
		return delai.getId();
	}

	@Override
	public void echoirQuestionnaire(@NotNull QuestionnaireSNC qsnc, @NotNull RegDate dateObtention) {
		final EtatDeclarationEchue etat = new EtatDeclarationEchue();
		etat.setDateObtention(dateObtention);
		qsnc.addEtat(etat);
		evenementFiscalService.publierEvenementFiscalEcheanceQuestionnaireSNC(qsnc, dateObtention);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoiDemandeDelaiQuestionnaireSNCOnline(Long idDelai, RegDate dateTraitement) throws EditiqueException {

		try {
			final DelaiDeclaration delai = delaiDeclarationDAO.get(idDelai);
			final QuestionnaireSNC qsnc = (QuestionnaireSNC) delai.getDeclaration();
			final Pair<EditiqueResultat, String> resultat;
			switch (delai.getEtat()) {
			case ACCORDE:
			case REFUSE:
				resultat = editiqueCompositionService.imprimeLettreDecisionDelaiQSNCOnline(delai);
				break;
			default:
				final String message = String.format("Type de lettre non-supporté, etat du délai  : %s", delai.getEtat());
				LOGGER.error(message);
				throw new IllegalArgumentException(message);
			}
			delai.setCleArchivageCourrier(resultat.getRight());
			return resultat.getLeft();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new EditiqueException(e);
		}

	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String envoiDemandeDelaiQuestionnaireSNCBatch(Long idDelai, RegDate dateTraitement) throws EditiqueException {

		try {

			final DelaiDeclaration delai = delaiDeclarationDAO.get(idDelai);
			final QuestionnaireSNC qsnc = (QuestionnaireSNC) delai.getDeclaration();
			final String cleArchivageDocument;
			switch (delai.getEtat()) {
			case ACCORDE:
			case REFUSE:
				cleArchivageDocument = editiqueCompositionService.imprimeLettreDecisionDelaiQSNCBatch(delai);
				break;
			default:
				final String message = String.format("Type de lettre non-supporté, etat du délai  : %s", delai.getEtat());
				LOGGER.error(message);
				throw new IllegalArgumentException(message);
			}
			delai.setCleArchivageCourrier(cleArchivageDocument);
			return cleArchivageDocument;
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new EditiqueException(e);
		}

	}


	public void setDelaiDeclarationDAO(DelaiDeclarationDAOImpl delaiDeclarationDAO) {
		this.delaiDeclarationDAO = delaiDeclarationDAO;
	}
}
