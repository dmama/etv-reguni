package ch.vd.uniregctb.declaration.snc;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AddAndSaveHelper;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.EtatDeclarationAddAndSaveAccessor;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.QuestionnaireSNCDAO;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalConsolide;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

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
	private ServiceRegimeFiscal serviceRegimeFiscal;

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

	public void setServiceRegimeFiscal(ServiceRegimeFiscal serviceRegimeFiscal) {
		this.serviceRegimeFiscal = serviceRegimeFiscal;
	}

	@Override
	public DeterminationQuestionnairesSNCResults determineQuestionnairesAEmettre(int periodeFiscale, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException {
		final DeterminationQuestionnairesSNCAEmettreProcessor processor = new DeterminationQuestionnairesSNCAEmettreProcessor(parametreAppService, transactionManager, periodeDAO, hibernateTemplate, tiersService,
		                                                                                                                      adresseService, validationService, tacheDAO, this);
		return processor.run(periodeFiscale, dateTraitement, nbThreads, statusManager);
	}

	@Override
	public EnvoiQuestionnairesSNCEnMasseResults envoiQuestionnairesSNCEnMasse(int periodeFiscale, RegDate dateTraitement, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws DeclarationException {
		final EnvoiQuestionnairesSNCEnMasseProcessor processor = new EnvoiQuestionnairesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, tacheDAO, this, periodeFiscaleDAO, ticketService);
		return processor.run(periodeFiscale, dateTraitement, nbMaxEnvois, statusManager);
	}

	@Override
	public EnvoiRappelsQuestionnairesSNCResults envoiRappelsQuestionnairesSNCEnMasse(RegDate dateTraitement, @Nullable Integer periodeFiscale, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws DeclarationException {
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
		final List<RegimeFiscalConsolide> regimesFiscaux = serviceRegimeFiscal.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
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
		for (int annee = periodeGestionUnireg.getDateDebut().year() ; annee <= RegDate.get().year() ; ++ annee) {
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
		AddAndSaveHelper.addAndSave(questionnaire, etat, questionnaireSNCDAO::save, new EtatDeclarationAddAndSaveAccessor<>());

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
		AddAndSaveHelper.addAndSave(questionnaire, etat, questionnaireSNCDAO::save, new EtatDeclarationAddAndSaveAccessor<>());

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
		questionnaire.setAnnule(true);
		evenementFiscalService.publierEvenementFiscalAnnulationQuestionnaireSNC(questionnaire);
	}
}
