package ch.vd.uniregctb.declaration.snc;

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
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.QuestionnaireSNCDAO;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.impl.EditiqueResultatReroutageInboxImpl;
import ch.vd.uniregctb.editique.impl.EditiqueResultatTimeoutImpl;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHisto;
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

	@Override
	public DeterminationQuestionnairesSNCResults determineQuestionnairesAEmettre(int periodeFiscale, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException {
		final DeterminationQuestionnairesSNCAEmettreProcessor processor = new DeterminationQuestionnairesSNCAEmettreProcessor(parametreAppService, transactionManager, periodeDAO, hibernateTemplate, tiersService,
		                                                                                                                      adresseService, validationService, tacheDAO);
		return processor.run(periodeFiscale, dateTraitement, nbThreads, statusManager);
	}

	@Override
	public EnvoiQuestionnairesSNCEnMasseResults envoiQuestionnairesSNCEnMasse(int periodeFiscale, RegDate dateTraitement, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws DeclarationException {
		final EnvoiQuestionnairesSNCEnMasseProcessor processor = new EnvoiQuestionnairesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, tacheDAO, this, periodeFiscaleDAO, ticketService);
		return processor.run(periodeFiscale, dateTraitement, nbMaxEnvois, statusManager);
	}

	@Override
	public EnvoiRappelsQuestionnairesSNCResults envoiRappelsQuestionnairesSNCEnMasse(RegDate dateTraitement, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws DeclarationException {
		final EnvoiRappelsQuestionnairesSNCProcessor processor = new EnvoiRappelsQuestionnairesSNCProcessor(transactionManager, hibernateTemplate, questionnaireSNCDAO, delaisService, this);
		return processor.run(dateTraitement, nbMaxEnvois, statusManager);
	}

	@NotNull
	@Override
	public Set<Integer> getPeriodesFiscalesTheoriquementCouvertes(Entreprise entreprise, boolean pourEmissionAutoSeulement) {

		// allons donc chercher les périodes de couverture des fors vaudois qui intersectent :
		// 1. les années civiles pour lesquelles Unireg doit envoyer les questionnaires SNC (entre la première PF de déclaration PM et l'année courante)
		// 2. les périodes pendant lesquelles l'entreprise a une forme juridique de type SP

		// quand a-t-on du SP ?
		final List<CategorieEntrepriseHisto> categories = tiersService.getCategoriesEntrepriseHisto(entreprise);
		final List<DateRange> rangesSP = new ArrayList<>(categories.size());
		for (CategorieEntrepriseHisto cat : categories) {
			if (cat.getCategorie() == CategorieEntreprise.SP) {
				rangesSP.add(cat);
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
		// TODO envoi à l'éditique (et inbox si pas de retour)
		// TODO envoi d'un événement fiscal ?
		return new EditiqueResultatTimeoutImpl("IDBIDON");
	}

	@Override
	public void envoiQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire) throws DeclarationException {
		// TODO envoi à l'éditique
		// TODO envoi d'un événement fiscal ?
	}

	@Override
	public EditiqueResultat envoiDuplicataQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws DeclarationException {
		// TODO envoi à l'éditique (et inbox si pas de retour)
		return new EditiqueResultatReroutageInboxImpl("IDBIDON");
	}

	@Override
	public EditiqueResultat envoiRappelQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateTraitement) throws DeclarationException {
		final EtatDeclarationRappelee rappel = new EtatDeclarationRappelee(dateTraitement, dateTraitement);
		questionnaire.addEtat(rappel);

		// TODO envoi à l'éditique (avec archivage... + envoi en inbox si pas de retour assez rapide)
		// TODO envoi d'un événement fiscal ?
		return new EditiqueResultatTimeoutImpl("IDBIDON");
	}

	@Override
	public void envoiRappelQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateTraitement, RegDate dateExpedition) throws DeclarationException {
		questionnaire.addEtat(new EtatDeclarationRappelee(dateTraitement, dateExpedition));

		// TODO envoi à l'éditique
		// TODO envoi d'un événement fiscal ?
	}

	@Override
	public EditiqueResultat getCopieConformeRappelQuestionnaireSNC(QuestionnaireSNC questionnaire) throws EditiqueException {
		// TODO demande de copie conforme à l'éditique
		return new EditiqueResultatTimeoutImpl("IDBIDON");
//		final String cleArchivage = impressionSommationDIPMHelper.construitCleArchivageDocument(di);
//		return editiqueService.getPDFDeDocumentDepuisArchive(di.getTiers().getNumero(), impressionSommationDIPMHelper.getTypeDocumentEditique(), cleArchivage);
	}

	@Override
	public void quittancerQuestionnaire(QuestionnaireSNC questionnaire, RegDate dateRetour, String source) throws DeclarationException {
		final EtatDeclarationRetournee retour = new EtatDeclarationRetournee(dateRetour, source);
		questionnaire.addEtat(retour);
	}
}