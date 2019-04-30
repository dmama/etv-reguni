package ch.vd.unireg.tiers.rattrapage.appariement;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.dialect.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.ParallelBatchTransactionTemplateWithResults;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;

/**
 * Processeur d'appariement d'établissements secondaires
 */
public class AppariementEtablissementsSecondairesProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppariementEtablissementsSecondairesProcessor.class);

	private static final int BATCH_SIZE = 10;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final AppariementService appariementService;
	private final TiersService tiersService;
	private final Dialect dbDialect;

	public AppariementEtablissementsSecondairesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                                     AppariementService appariementService, TiersService tiersService, Dialect dbDialect) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.appariementService = appariementService;
		this.tiersService = tiersService;
		this.dbDialect = dbDialect;
	}

	public AppariementEtablissementsSecondairesResults run(int nbThreads, boolean simulation, StatusManager s) {
		final StatusManager status = s == null ? new LoggingStatusManager(LOGGER) : s;
		status.setMessage("Récupération des entreprises ayant des établissements candidats à une tentative d'appariement...");
		final List<Long> ids = getIdsEntreprisesApparieesAvecEtablissementSecondaireNonApparie();
		return run(ids, nbThreads, simulation, status);
	}

	public AppariementEtablissementsSecondairesResults run(List<Long> ids, final int nbThreads, final boolean simulation, StatusManager s) {
		final StatusManager status = s == null ? new LoggingStatusManager(LOGGER) : s;
		final AppariementEtablissementsSecondairesResults rapportFinal = new AppariementEtablissementsSecondairesResults(nbThreads, simulation, ids);
		final ParallelBatchTransactionTemplateWithResults<Long, AppariementEtablissementsSecondairesResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids,
		                                                                                                                                                                  BATCH_SIZE,
		                                                                                                                                                                  nbThreads,
		                                                                                                                                                                  Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                                  transactionManager,
		                                                                                                                                                                  status,
		                                                                                                                                                                  AuthenticationInterface.INSTANCE);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, AppariementEtablissementsSecondairesResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, AppariementEtablissementsSecondairesResults rapport) throws Exception {
				status.setMessage("Tentatives d'appariement...", progressMonitor.getProgressInPercent());
				for (Long id : batch) {
					final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, id);
					if (entreprise == null) {
						throw new TiersNotFoundException(id);
					}
					else if (status.isInterrupted()) {
						break;
					}

					// tentative d'appariement des établissements secondaires non-appariés
					traitementEntreprise(entreprise, simulation, rapport);
				}
				return !status.isInterrupted();
			}

			@Override
			public AppariementEtablissementsSecondairesResults createSubRapport() {
				return new AppariementEtablissementsSecondairesResults(nbThreads, simulation, null);
			}
		}, progressMonitor);

		status.setMessage("Procédure d'appariements d'établissements secondaires terminée.");

		rapportFinal.setInterrupted(status.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void traitementEntreprise(Entreprise entreprise, boolean simulation, AppariementEtablissementsSecondairesResults rapport) {
		final List<CandidatAppariement> appariements = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
		if (!appariements.isEmpty()) {
			if (!simulation) {
				for (CandidatAppariement appariement : appariements) {
					tiersService.apparier(appariement.getEtablissement(), appariement.getEtablissementCivil());
				}
			}
			pushToRapport(entreprise, appariements, rapport);
		}
	}

	private static void pushToRapport(Entreprise entreprise, List<CandidatAppariement> appariements, AppariementEtablissementsSecondairesResults rapport) {
		for (CandidatAppariement appariement : appariements) {
			switch (appariement.getCritere()) {
			case LOCALISATION:
				rapport.addNouvelAppariementEtablissementCommune(entreprise, appariement.getEtablissement(), appariement.getEtablissementCivil(), appariement.getTypeAutoriteFiscaleSiege(), appariement.getOfsSiege());
				break;
			case IDE:
				rapport.addNouvelAppariementEtablissementIde(entreprise, appariement.getEtablissement(), appariement.getEtablissementCivil(), appariement.getTypeAutoriteFiscaleSiege(), appariement.getOfsSiege());
				break;
			default:
				throw new IllegalArgumentException("Type de critère décisif inconnu : " + appariement.getCritere());
			}
		}
	}

	private List<Long> getIdsEntreprisesApparieesAvecEtablissementSecondaireNonApparie() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> hibernateTemplate.executeWithNewSession(session -> {
			final String sql = "SELECT DISTINCT ENTR.NUMERO FROM TIERS ENTR"
					+ " JOIN RAPPORT_ENTRE_TIERS AE ON AE.TIERS_SUJET_ID=ENTR.NUMERO AND AE.ANNULATION_DATE IS NULL AND AE.RAPPORT_ENTRE_TIERS_TYPE='ActiviteEconomique' AND AE.ETB_PRINCIPAL=%s"
					+ " JOIN TIERS ETB ON AE.TIERS_OBJET_ID=ETB.NUMERO AND ETB.NUMERO_ETABLISSEMENT IS NULL"
					+ " WHERE ENTR.NUMERO_ENTREPRISE IS NOT NULL AND ENTR.ANNULATION_DATE IS NULL AND ENTR.TIERS_TYPE='Entreprise'"
					+ " ORDER BY ENTR.NUMERO";

			final Query query = session.createSQLQuery(String.format(sql, dbDialect.toBooleanValueString(false)));
			final List<Number> brutto = query.list();
			final List<Long> res = new LinkedList<>();
			for (Number id : brutto) {
				res.add(id.longValue());
			}
			return res;
		}));
	}
}
