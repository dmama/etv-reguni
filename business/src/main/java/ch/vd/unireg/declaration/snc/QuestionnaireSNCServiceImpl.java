package ch.vd.unireg.declaration.snc;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
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
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationPMSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalConsolide;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
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
	private RegimeFiscalService regimeFiscalService;
	private EvenementDeclarationPMSender evenementDeclarationPMSender;

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

	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	public void setEvenementDeclarationPMSender(EvenementDeclarationPMSender evenementDeclarationPMSender) {
		this.evenementDeclarationPMSender = evenementDeclarationPMSender;
	}

	@Override
	public DeterminationQuestionnairesSNCResults determineQuestionnairesAEmettre(int periodeFiscale, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException {
		final DeterminationQuestionnairesSNCAEmettreProcessor processor =
				new DeterminationQuestionnairesSNCAEmettreProcessor(parametreAppService, transactionManager, periodeDAO, hibernateTemplate, tiersService,
						adresseService, validationService, tacheDAO, this);
		return processor.run(periodeFiscale, dateTraitement, nbThreads, statusManager);
	}

	@Override
	public EnvoiQuestionnairesSNCEnMasseResults envoiQuestionnairesSNCEnMasse(int periodeFiscale, RegDate dateTraitement, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws
			DeclarationException {
		final EnvoiQuestionnairesSNCEnMasseProcessor processor =
				new EnvoiQuestionnairesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, tacheDAO, this, periodeFiscaleDAO, ticketService);
		return processor.run(periodeFiscale, dateTraitement, nbMaxEnvois, statusManager);
	}

	@Override
	public EnvoiRappelsQuestionnairesSNCResults envoiRappelsQuestionnairesSNCEnMasse(RegDate dateTraitement, @Nullable Integer periodeFiscale, @Nullable Integer nbMaxEnvois,
	                                                                                 StatusManager statusManager) throws DeclarationException {
		final EnvoiRappelsQuestionnairesSNCProcessor processor = new EnvoiRappelsQuestionnairesSNCProcessor(transactionManager, hibernateTemplate, questionnaireSNCDAO, delaisService, this);
		return processor.run(dateTraitement, periodeFiscale, nbMaxEnvois, statusManager);
	}

	@NotNull
	@Override
	public Set<Integer> getPeriodesFiscalesTheoriquementCouvertes(Entreprise entreprise, boolean pourEmissionAutoSeulement) {

		// allons donc chercher les périodes de couverture des fors vaudois qui intersectent :
		// 1. les années civiles pour lesquelles Unireg doit envoyer les questionnaires SNC (entre la première PF de déclaration PM et l'année courante)
		// 2. les périodes pendant lesquelles l'entreprise a une forme juridique de type SP

		// quand a-t-on du SP ?
		final List<RegimeFiscalConsolide> regimesFiscaux = regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
		final List<DateRange> rangesSP = new ArrayList<>(regimesFiscaux.size());
		for (RegimeFiscalConsolide regime : regimesFiscaux) {
			if (regime.getCategorie() == CategorieEntreprise.SP) {
				rangesSP.add(regime);
			}
		}

		// y en a-t-il ?
		if (rangesSP.isEmpty()) {
			return Collections.emptySet();
		}

		// quand génère-t-on des questionnaires SNC ?
		final int premiereAnnee = pourEmissionAutoSeulement
				? parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales()
				: parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
		final DateRange periodeGestionUnireg = new DateRangeHelper.Range(RegDate.get(premiereAnnee, 1, 1), null);

		// a-t-on des catégories d'entreprise SP dans la période de gestion par Unireg ?
		final List<DateRange> spDansPeriodeGestion = DateRangeHelper.intersections(periodeGestionUnireg, rangesSP);
		if (spDansPeriodeGestion == null || spDansPeriodeGestion.isEmpty()) {
			return Collections.emptySet();
		}

		// couverture des fors vaudois ?
		final List<ForFiscal> forsFiscaux = entreprise.getForsFiscauxNonAnnules(true);
		final List<ForFiscal> forsVaudois = new ArrayList<>(forsFiscaux.size());
		for (ForFiscal ff : forsFiscaux) {
			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ff.getGenreImpot() == GenreImpot.REVENU_FORTUNE) {
				forsVaudois.add(ff);
			}
		}
		final List<DateRange> couvertureVaudoiseFors = DateRangeHelper.merge(forsVaudois);
		if (couvertureVaudoiseFors == null || couvertureVaudoiseFors.isEmpty()) {
			return Collections.emptySet();
		}

		// couverture des fors vaudois pendant les périodes SP intéressantes ?
		final List<DateRange> spVaudois = DateRangeHelper.intersections(couvertureVaudoiseFors, spDansPeriodeGestion);
		if (spVaudois == null || spVaudois.isEmpty()) {
			return Collections.emptySet();
		}

		// découpons maintenant par année civile ce qui nous reste
		final Set<Integer> periodes = new LinkedHashSet<>();
		for (int annee = periodeGestionUnireg.getDateDebut().year(); annee <= RegDate.get().year(); ++annee) {
			final DateRange anneeCivile = new DateRangeHelper.Range(RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
			if (DateRangeHelper.intersect(anneeCivile, spVaudois)) {
				periodes.add(annee);
			}
		}
		return periodes;
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
	public void quittancerQuestionnaire(QuestionnaireSNC questionnaire, RegDate dateRetour, String source) throws DeclarationException {
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
}
