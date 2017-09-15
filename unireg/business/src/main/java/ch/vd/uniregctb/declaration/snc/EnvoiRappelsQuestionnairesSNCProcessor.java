package ch.vd.uniregctb.declaration.snc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.QuestionnaireSNCDAO;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

public class EnvoiRappelsQuestionnairesSNCProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiRappelsQuestionnairesSNCProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final QuestionnaireSNCDAO questionnaireSNCDAO;
	private final DelaisService delaisService;
	private final QuestionnaireSNCService qsncService;

	/**
	 * Pour les tests de limitations, on aime bien que les questionnaires soient toujours traités dans le même ordre,
	 * (= le même ordre que celui des données d'entrée)
	 */
	private static final Comparator<QuestionnaireSNC> SNC_COMPARATOR = Comparator.comparing(q -> new IdentifiantDeclaration(q, null),
	                                                                                        IdentifiantDeclaration.COMPARATOR_NATUREL);

	public EnvoiRappelsQuestionnairesSNCProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, QuestionnaireSNCDAO questionnaireSNCDAO, DelaisService delaisService, QuestionnaireSNCService qsncService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.questionnaireSNCDAO = questionnaireSNCDAO;
		this.delaisService = delaisService;
		this.qsncService = qsncService;
	}

	public EnvoiRappelsQuestionnairesSNCResults run(final RegDate dateTraitement, @Nullable final Integer periodeFiscale, @Nullable final Integer nbMaxEnvois, @Nullable StatusManager s) {
		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);
		final int limite = nbMaxEnvois == null || nbMaxEnvois <= 0 ? 0 : nbMaxEnvois;

		status.setMessage("Identification des questionnaires concernés...");

		// récupération des identifiants des questionnaires à rappeler
		final List<IdentifiantDeclaration> ids = getQuestionnairesARappeler(dateTraitement, periodeFiscale);

		// traitement
		final EnvoiRappelsQuestionnairesSNCResults rapportFinal = new EnvoiRappelsQuestionnairesSNCResults(dateTraitement, periodeFiscale, nbMaxEnvois);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<IdentifiantDeclaration, EnvoiRappelsQuestionnairesSNCResults> template = new BatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<IdentifiantDeclaration, EnvoiRappelsQuestionnairesSNCResults>() {
			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EnvoiRappelsQuestionnairesSNCResults rapport) throws Exception {
				final List<Long> identifiantsQuestionnaires = extractIdentifiants(batch);
				final Set<QuestionnaireSNC> questionnaires = questionnaireSNCDAO.getDeclarationsAvecDelaisEtEtats(identifiantsQuestionnaires);
				status.setMessage("Traitement des rappels...", progressMonitor.getProgressInPercent());

				// pour les tests de limitations, on aime bien que les questionnaires soient toujours traités dans le même ordre,
				// (= le même ordre que celui des données d'entrée)
				final List<QuestionnaireSNC> questionnairesTries = new ArrayList<>(questionnaires);
				questionnairesTries.sort(SNC_COMPARATOR);

				for (QuestionnaireSNC questionnaire : questionnairesTries) {
					traiterQuestionnaire(questionnaire, dateTraitement, rapport);
					if (limite > 0 && rapportFinal.getNombreRappelsEmis() + rapport.getNombreRappelsEmis() >= limite) {
						return false;
					}
				}
				return !status.isInterrupted();
			}

			@Override
			public EnvoiRappelsQuestionnairesSNCResults createSubRapport() {
				return new EnvoiRappelsQuestionnairesSNCResults(dateTraitement, periodeFiscale, nbMaxEnvois);
			}
		}, progressMonitor);

		status.setMessage("Traitement terminé.");

		rapportFinal.setInterrupted(status.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterQuestionnaire(QuestionnaireSNC questionnaire, RegDate dateTraitement, EnvoiRappelsQuestionnairesSNCResults rapport) throws DeclarationException {
		// il faut vérifier que le délai administratif est passé...

		// si le questionnaire n'est plus dans l'état "EMISE", c'est qu'il vient de bouger et ne doit pas être rappelé
		final EtatDeclaration dernierEtat = questionnaire.getDernierEtatDeclaration();
		if (dernierEtat == null || dernierEtat.getEtat() != TypeEtatDocumentFiscal.EMISE) {
			rapport.addQuestionnaireNonEmis(questionnaire, dernierEtat);
		}
		else {
			final RegDate finDelai = delaisService.getDateFinDelaiEnvoiRappelQuestionnaireSNC(questionnaire.getDelaiAccordeAu());
			if (finDelai.isBefore(dateTraitement)) {
				// pour faire le parallèle avec les DI, on ne va pas rappeler un questionnaire qui devrait être annulé...
				final Set<Integer> pfsACouvrir = qsncService.getPeriodesFiscalesTheoriquementCouvertes((Entreprise) questionnaire.getTiers(), false);
				if (!pfsACouvrir.contains(questionnaire.getPeriode().getAnnee())) {
					rapport.addRappelIgnoreCarQuestionnaireDevraitEtreAnnule(questionnaire);
				}
				else {
					// ok, il faut donc maintenant envoyer le rappel

					// passage à l'état "rappelé"
					final RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionQuestionnaireSNC(dateTraitement);

					// envoi du document éditique
					qsncService.envoiRappelQuestionnaireSNCForBatch(questionnaire, dateTraitement, dateExpedition);

					// entrée dans le rapport
					rapport.addRappelEmis(questionnaire);
				}
			}
			else {
				rapport.addRappelIgnoreDelaiAdministratifNonEchu(questionnaire);
			}
		}
	}

	private static List<Long> extractIdentifiants(List<IdentifiantDeclaration> src) {
		final List<Long> ids = new ArrayList<>(src.size());
		for (IdentifiantDeclaration id : src) {
			ids.add(id.getIdDeclaration());
		}
		return ids;
	}

	/**
	 * ATTENTION : ici, contrairement aux cas des DI/LR dans lesquels on ne teste pas l'existence d'un état "ECHUE" seul (sans état "SOMMEE"), parce
	 * que ce n'est pas un cas métier, nous sommes obligés de le faire ici car la migration des données de SIMPA a parfois ajouté de tels états "ECHUE"
	 * justement pour empêcher le rappel de vieux questionnaires...
	 */
	private List<IdentifiantDeclaration> getQuestionnairesARappeler(final RegDate dateTraitement, @Nullable final Integer periodeFiscale) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<IdentifiantDeclaration>>() {
			@Override
			public List<IdentifiantDeclaration> doInTransaction(TransactionStatus status) {
				final List<Object[]> rows = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
					@Override
					public List<Object[]> doInHibernate(Session session) throws HibernateException {
						final StringBuilder b = new StringBuilder();
						b.append("SELECT qsnc.id, qsnc.tiers.id FROM QuestionnaireSNC AS qsnc");
						b.append(" WHERE qsnc.annulationDate IS NULL");
						if (periodeFiscale != null) {
							b.append(" AND qsnc.periode.annee = :pf");
						}
						b.append(" AND EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE qsnc.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class = EtatDeclarationEmise)");
						b.append(" AND NOT EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE qsnc.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class IN (EtatDeclarationRetournee, EtatDeclarationSommee, EtatDeclarationRappelee, EtatDeclarationSuspendue, EtatDeclarationEchue))");
						b.append(" AND EXISTS (SELECT delai.declaration.id FROM DelaiDeclaration AS delai WHERE qsnc.id = delai.declaration.id AND delai.annulationDate IS NULL AND delai.delaiAccordeAu IS NOT NULL AND delai.etat = 'ACCORDE'");
						b.append(" GROUP BY delai.declaration.id HAVING MAX(delai.delaiAccordeAu) < :dateLimite)");
						b.append(" ORDER BY qsnc.tiers.id ASC, qsnc.dateDebut ASC");
						final String sql = b.toString();
						final Query query = session.createQuery(sql);
						query.setParameter("dateLimite", dateTraitement);
						if (periodeFiscale != null) {
							query.setParameter("pf", periodeFiscale);
						}
						//noinspection unchecked
						return query.list();
					}
				});
				final List<IdentifiantDeclaration> identifiants = new ArrayList<>(rows.size());
				for (Object[] objects : rows) {
					final Number idQuestionnaire = (Number) objects[0];
					final Number idTiers = (Number) objects[1];
					final IdentifiantDeclaration identifiantDeclaration = new IdentifiantDeclaration(idQuestionnaire.longValue(), idTiers.longValue());
					identifiants.add(identifiantDeclaration);
				}
				return identifiants;
			}
		});
	}
}
