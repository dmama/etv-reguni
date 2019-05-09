package ch.vd.unireg.declaration.snc;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * Processor qui fait passer à l'état <i>échu</i> les questionnaires SNC à l'état <i>rappelé</i> et dont le délai de retour est dépassé.
 */
public class EchoirQuestionnairesSNCProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EchoirQuestionnairesSNCProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DelaisService delaisService;
	private final QuestionnaireSNCService questionnaireSNCService;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public EchoirQuestionnairesSNCProcessor(HibernateTemplate hibernateTemplate, DelaisService delaisService, QuestionnaireSNCService questionnaireSNCService, PlatformTransactionManager transactionManager, TiersService tiersService,
	                                        AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.delaisService = delaisService;
		this.questionnaireSNCService = questionnaireSNCService;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public EchoirQuestionnairesSNCResults run(@NotNull RegDate dateTraitement, @Nullable StatusManager s) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Récupération des questionnaires SNC...");

		final EchoirQuestionnairesSNCResults rapportFinal = new EchoirQuestionnairesSNCResults(dateTraitement, tiersService, adresseService);
		final List<IdentifiantDeclaration> identifiants = determineQuestionnairesATraiter(dateTraitement);

		status.setMessage("Analyse des questionnaires SNC...");

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<IdentifiantDeclaration, EchoirQuestionnairesSNCResults>
				template = new BatchTransactionTemplateWithResults<>(identifiants, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<IdentifiantDeclaration, EchoirQuestionnairesSNCResults>() {

			@Override
			public EchoirQuestionnairesSNCResults createSubRapport() {
				return new EchoirQuestionnairesSNCResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EchoirQuestionnairesSNCResults r) {
				status.setMessage(String.format("Questionnaires SNC analysés : %d/%d", rapportFinal.getTotal(), identifiants.size()), progressMonitor.getProgressInPercent());
				traiterQuestionnaires(batch, r, status);
				return !status.isInterrupted();
			}
		}, progressMonitor);

		rapportFinal.setInterrompu(status.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterQuestionnaires(@NotNull List<IdentifiantDeclaration> list, @NotNull EchoirQuestionnairesSNCResults results, @NotNull StatusManager status) {
		for (IdentifiantDeclaration ident : list) {
			traiterQuestionnaire(ident, results);
			if (status.isInterrupted()) {
				break;
			}
		}
	}

	/**
	 * Traite un questionnaire SNC. C'est-à-dire vérifier qu'il est dans l'état rappelé et que le délai de retour est dépassé;
	 * puis si c'est bien le cas, la faire passer à l'état échu.
	 *
	 * @param ident l'id du questionnaire à traiter
	 * @param results les résultats de processing à compléter
	 */
	private void traiterQuestionnaire(@NotNull IdentifiantDeclaration ident, @NotNull EchoirQuestionnairesSNCResults results) {

		final QuestionnaireSNC qsnc = hibernateTemplate.get(QuestionnaireSNC.class, ident.getIdDeclaration());
		if (qsnc == null) {
			throw new ObjectNotFoundException("Le questionnaire SNC n'existe pas.");
		}

		final EtatDeclaration dernierEtat = qsnc.getDernierEtatDeclaration();
		if (dernierEtat == null) {
			throw new IllegalArgumentException("Le questionnaire SNC ne possède pas d'état.");
		}

		// Vérifie l'état du questionnaire (blindage)
		if (dernierEtat.getEtat() != TypeEtatDocumentFiscal.RAPPELE) {
			results.addErrorEtatIncoherent(qsnc, String.format("Etat attendu=%s, état constaté=%s. Erreur dans la requête SQL ?", TypeEtatDocumentFiscal.RAPPELE, dernierEtat.getEtat()));
			return;
		}

		// Vérifie que l'échéance est bien atteinte (blindage)
		final RegDate echeance = getEcheanceRappel(dernierEtat.getDateObtention());
		if (results.getDateTraitement().isBeforeOrEqual(echeance)) {
			results.addErrorRappelNonEchu(qsnc, "Echéance = " + RegDateHelper.dateToDisplayString(echeance) + ", date de traitement = " + RegDateHelper.dateToDisplayString(results.getDateTraitement()) + ". Erreur dans la requête SQL ?");
		}

		// On fait passer le questionnaire SNC à l'état échu
		questionnaireSNCService.echoirQuestionnaire(qsnc, results.getDateTraitement());
		results.addQuestionnaireTraite(qsnc);

		// un peu de paranoïa ne fait pas de mal
		if (qsnc.getDernierEtatDeclaration().getEtat() != TypeEtatDocumentFiscal.ECHU) {
			throw new IllegalArgumentException("L'état après traitement n'est pas ECHU.");
		}
	}

	/*
	 * Une ligne des résultats de la requête SQL ci-dessous
	 */
	private class Row {
		final long sncId;
		final RegDate dateRappel;
		final long tiersId;

		public Row(@NotNull Object[] row) {
			this.sncId = ((Number) row[0]).longValue();
			this.dateRappel = RegDate.fromIndex(((Number) row[1]).intValue(), false);
			this.tiersId = ((Number) row[2]).longValue();
		}

		public boolean isEcheanceDepassee(@NotNull RegDate dateTraitement) {
			final RegDate echeance = getEcheanceRappel(dateRappel);
			return dateTraitement.isAfter(echeance);
		}

		@NotNull
		public IdentifiantDeclaration toIdentifiant() {
			return new IdentifiantDeclaration(sncId, tiersId);
		}
	}

	/**
	 * @param dateTraitement la date de référence pour déterminer si le délai est échu
	 * @return une liste des questionnaires <i>candidats</i> à être échus.
	 */
	private List<IdentifiantDeclaration> determineQuestionnairesATraiter(@NotNull RegDate dateTraitement) {

		// les questionnaires SNC rappelés, mais pas échus/retournés/suspendus
		final String sql = "SELECT QSNC.ID, ES.DATE_OBTENTION, QSNC.TIERS_ID FROM DOCUMENT_FISCAL QSNC" +
				" JOIN ETAT_DOCUMENT_FISCAL ES ON ES.DOCUMENT_FISCAL_ID = QSNC.ID AND ES.ANNULATION_DATE IS NULL AND ES.TYPE='RAPPELE'" +
				" WHERE QSNC.DOCUMENT_TYPE='QSNC' AND QSNC.ANNULATION_DATE IS NULL" +
				" AND NOT EXISTS (SELECT 1 FROM ETAT_DOCUMENT_FISCAL ED WHERE ED.DOCUMENT_FISCAL_ID = QSNC.ID AND ED.ANNULATION_DATE IS NULL AND ED.TYPE IN ('RETOURNE', 'ECHU', 'SUSPENDU'))" +
				" ORDER BY QSNC.TIERS_ID";

		// on recherche les questionnaires à traiter
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query = session.createSQLQuery(sql);
			//noinspection unchecked
			final List<Object[]> rows = query.list();
			return rows.stream()
					.map(Row::new)
					.filter(r -> r.isEcheanceDepassee(dateTraitement))
					.map(Row::toIdentifiant)
					.collect(Collectors.toList());
		}));
	}

	/**
	 * @param dateRappel une date de rappel
	 * @return la date d'échéance du rappel (= date à partir de laquelle le questionnaire est échu)
	 */
	@NotNull
	private RegDate getEcheanceRappel(@NotNull RegDate dateRappel) {
		// L'échéance de sommation = date sommation + 30 jours (délai normal) + 15 jours (délai administratif)
		final RegDate delaiTemp = delaisService.getDateFinDelaiEcheanceRappelQSNC(dateRappel); // 30 jours
		return delaisService.getDateFinDelaiEnvoiRappelQuestionnaireSNC(delaiTemp); // 15 jours
	}
}
