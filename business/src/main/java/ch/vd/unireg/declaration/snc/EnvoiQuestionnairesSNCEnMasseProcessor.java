package ch.vd.unireg.declaration.snc;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.common.TicketTimeoutException;
import ch.vd.unireg.declaration.DeclarationAvecNumeroSequence;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationGenerationOperation;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDelaiDeclaration;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

/**
 * Processeur pour l'implémentation du job d'envoi en masse des questionnaires SNC d'une période fiscale
 */
public class EnvoiQuestionnairesSNCEnMasseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiQuestionnairesSNCEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final TacheDAO tacheDAO;
	private final QuestionnaireSNCService questionnaireService;
	private final PeriodeFiscaleDAO periodeFiscaleDAO;
	private final TicketService ticketService;

	public EnvoiQuestionnairesSNCEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService, TacheDAO tacheDAO,
	                                              QuestionnaireSNCService questionnaireService, PeriodeFiscaleDAO periodeFiscaleDAO, TicketService ticketService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.tacheDAO = tacheDAO;
		this.questionnaireService = questionnaireService;
		this.periodeFiscaleDAO = periodeFiscaleDAO;
		this.ticketService = ticketService;
	}

	public EnvoiQuestionnairesSNCEnMasseResults run(final int periodeFiscale, final RegDate dateTraitement, @Nullable final Integer nbMaxEnvois, @Nullable StatusManager s) {

		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);

		final EnvoiQuestionnairesSNCEnMasseResults rapportFinal = new EnvoiQuestionnairesSNCEnMasseResults(periodeFiscale, dateTraitement, nbMaxEnvois);
		if (nbMaxEnvois != null && nbMaxEnvois <= 0) {
			// pas grand'chose à faire....
			status.setMessage("Nombre maximal d'envois spécifié négatif ou nul : aucun traitement lancé.");
			return rapportFinal;
		}

		// un peu de tracing
		status.setMessage("Identification des entreprises concernées...");

		// récupération des contribuables concernés par une tâche d'envoi
		final List<Long> idsContribuables = getIdsContribuablesAvecTachesEnvoi(periodeFiscale);

		// traitement de ces contribuables par lot
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, EnvoiQuestionnairesSNCEnMasseResults> template = new BatchTransactionTemplateWithResults<>(idsContribuables, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, EnvoiQuestionnairesSNCEnMasseResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, EnvoiQuestionnairesSNCEnMasseResults rapport) throws Exception {
				status.setMessage(String.format("Traitement des contribuables [%s -> %s]", batch.get(0), batch.get(batch.size() - 1)), progressMonitor.getProgressInPercent());
				final PeriodeFiscale pf = periodeFiscaleDAO.getPeriodeFiscaleByYear(periodeFiscale);
				final int nbEnvoyesAvant = rapportFinal.getNombreEnvoyes();
				if (nbMaxEnvois == null || nbMaxEnvois > nbEnvoyesAvant) {
					for (Long idContribuable : batch) {
						final Tiers tiers = tiersService.getTiers(idContribuable);
						if (!(tiers instanceof Entreprise)) {
							rapport.addErrorWrongPartyType(idContribuable);
							continue;
						}
						final DeclarationGenerationOperation operation = new DeclarationGenerationOperation(tiers.getNumero());
						try {
							final TicketService.Ticket ticket = ticketService.getTicket(operation, Duration.ofMillis(500));
							try {
								traiterContribuable((Entreprise) tiers, pf, dateTraitement, rapport);
							}
							finally {
								ticket.release();
							}
						}
						catch (TicketTimeoutException e) {
							throw new DeclarationException("Un questionnaire SNC est déjà en cours de génération sur cette entreprise.");
						}
						if ((nbMaxEnvois != null && nbMaxEnvois <= nbEnvoyesAvant + rapport.getNombreEnvoyes()) || status.isInterrupted()) {
							return false;
						}
					}
				}
				return !status.isInterrupted();
			}

			@Override
			public EnvoiQuestionnairesSNCEnMasseResults createSubRapport() {
				return new EnvoiQuestionnairesSNCEnMasseResults(periodeFiscale, dateTraitement, nbMaxEnvois);
			}

		}, progressMonitor);

		// et au final...
		status.setMessage("Traitement terminé.");

		// fin
		rapportFinal.setInterrupted(status.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterContribuable(Entreprise entreprise, PeriodeFiscale pf, RegDate dateTraitement, EnvoiQuestionnairesSNCEnMasseResults rapport) throws DeclarationException {
		final List<Tache> tachesATraiter = getTachesEnvoiATraiter(entreprise, pf.getAnnee());
		if (tachesATraiter == null || tachesATraiter.isEmpty()) {
			// pas de tâche à traiter... on dirait bien que quelqu'un nous est passé sous le nez
			rapport.addIgnoreTacheDisparue(entreprise.getNumero());
		}
		else {
			// un questionnaire existe-t-il déjà pour cette période fiscale
			final List<QuestionnaireSNC> questionnairesExistants = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf.getAnnee(), false);
			if (!questionnairesExistants.isEmpty()) {
				// on annule tout, il n'y a rien à faire !
				for (Tache tache : tachesATraiter) {
					tache.setAnnule(true);
				}
				rapport.addIgnoreQuestionnaireExistant(entreprise.getNumero());
			}
			else {
				// seule la première est marquée comme traitée, les éventuelles autres seront annulées par le processus de recalcul des tâches
				final TacheEnvoiQuestionnaireSNC tacheATraiter = (TacheEnvoiQuestionnaireSNC) tachesATraiter.get(0);
				tacheATraiter.setEtat(TypeEtatTache.TRAITE);
				tacheATraiter.setDateDebut(RegDate.get(pf.getAnnee(), 1, 1));       // éventuel réalignement de la tâche
				tacheATraiter.setDateFin(RegDate.get(pf.getAnnee(), 12, 31));

				final QuestionnaireSNC questionnaire = new QuestionnaireSNC();
				questionnaire.setDateDebut(tacheATraiter.getDateDebut());
				questionnaire.setDateFin(tacheATraiter.getDateFin());
				questionnaire.setPeriode(pf);
				questionnaire.setModeleDocument(pf.get(TypeDocument.QUESTIONNAIRE_SNC));
				questionnaire.setNumero(getNewSequenceNumber(entreprise, pf.getAnnee()));
				questionnaire.setTiers(entreprise);
				questionnaire.addEtat(new EtatDeclarationEmise(dateTraitement));
				addDelaiRetourInitial(questionnaire, dateTraitement, pf.getParametrePeriodeFiscaleSNC());
				questionnaire.setCodeSegment(QuestionnaireSNCService.codeSegment);
				final QuestionnaireSNC saved = hibernateTemplate.merge(questionnaire);
				entreprise.addDeclaration(saved);
				questionnaireService.envoiQuestionnaireSNCForBatch(saved, dateTraitement);
				rapport.addQuestionnaireEnvoye(saved);
			}
		}
	}

	private static int getNewSequenceNumber(Entreprise entreprise, int pf) {
		final List<DeclarationAvecNumeroSequence> declarations = entreprise.getDeclarationsDansPeriode(DeclarationAvecNumeroSequence.class, pf, true);
		int max = 0;
		for (DeclarationAvecNumeroSequence declaration : declarations) {
			max = Math.max(max, declaration.getNumero());
		}
		return max + 1;
	}

	private void addDelaiRetourInitial(QuestionnaireSNC questionnaire, RegDate dateTraitement, ParametrePeriodeFiscaleSNC parametres) {
		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(dateTraitement);
		delai.setDateTraitement(dateTraitement);
		delai.setDelaiAccordeAu(parametres.getTermeGeneralRappelEffectif());
		delai.setTypeDelai(TypeDelaiDeclaration.IMPLICITE); // [FISCPROJ-873] Par définition, les délais des envois en masse sont implicites
		questionnaire.setDelaiRetourImprime(parametres.getTermeGeneralRappelImprime());
		questionnaire.addDelai(delai);
	}

	@Nullable
	private List<Tache> getTachesEnvoiATraiter(Entreprise entreprise, int periodeFiscale) {
		final TacheCriteria tacheCriterion = new TacheCriteria();
		tacheCriterion.setAnnee(periodeFiscale);
		tacheCriterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
		tacheCriterion.setInclureTachesAnnulees(false);
		tacheCriterion.setNumeroCTB(entreprise.getNumero());
		tacheCriterion.setTypeTache(TypeTache.TacheEnvoiQuestionnaireSNC);
		final List<Tache> tachesATraiter = tacheDAO.find(tacheCriterion);
		return tachesATraiter == null || tachesATraiter.isEmpty() ? null : tachesATraiter;
	}

	private List<Long> getIdsContribuablesAvecTachesEnvoi(final int periodeFiscale) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final String hql = "SELECT DISTINCT tache.contribuable.id FROM TacheEnvoiQuestionnaireSNC AS tache WHERE tache.annulationDate IS NULL AND tache.dateFin BETWEEN :debut AND :fin AND tache.etat = :etat ORDER BY tache.contribuable.id ASC";
				return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery(hql);
						query.setParameter("debut", RegDate.get(periodeFiscale, 1, 1));
						query.setParameter("fin", RegDate.get(periodeFiscale, 12, 31));
						query.setParameter("etat", TypeEtatTache.EN_INSTANCE);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}
}
