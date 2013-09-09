package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResult;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class IdentifierContribuableProcessor {

	private static final int BATCH_SIZE = 100;

	private static final Logger LOGGER = Logger.getLogger(IdentifierContribuableProcessor.class);

	private final IdentificationContribuableService identService;
	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public IdentifierContribuableProcessor(IdentificationContribuableService identService, HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager,
	                                       TiersService tiersService,
	                                       AdresseService adresseService) {
		this.identService = identService;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}


	public IdentifierContribuableResults run(final RegDate dateTraitement, int nbThreads, final StatusManager s, Long idMessage) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final IdentifierContribuableResults rapportFinal = new IdentifierContribuableResults(dateTraitement, tiersService, adresseService);
		status.setMessage("Récupération des messages de demande d'identification à traiter...");

		final List<Long> ids = recupererMessageATraiter(idMessage);

		// Reussi les messages par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResult<Long, IdentifierContribuableResults>
				template = new ParallelBatchTransactionTemplateWithResult<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, IdentifierContribuableResults>() {

			@Override
			public IdentifierContribuableResults createSubRapport() {
				return new IdentifierContribuableResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, IdentifierContribuableResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, r);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}, progressMonitor);

		final int count = rapportFinal.identifies.size();

		if (status.interrupted()) {
			status.setMessage("la relance de l'identification des messages a été interrompue."
					                  + " Nombre de messages identifés au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("la relance de l'identification des messages est terminée."
					                  + "Nombre de messages traités = " + rapportFinal.nbMessagesTotal + ". Nombre de massages identifiés = " + count + ". Nombre d'erreurs = " +
					                  rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;

	}

	private void traiterBatch(final List<Long> batch, IdentifierContribuableResults rapport) throws Exception {
		//Chargement des messages d'identification
		// On charge tous les contribuables en vrac (avec préchargement des déclarations)
		final List<IdentificationContribuable> list = hibernateTemplate.execute(new HibernateCallback<List<IdentificationContribuable>>() {
			@Override
			public List<IdentificationContribuable> doInHibernate(Session session) throws HibernateException {
				Criteria crit = session.createCriteria(IdentificationContribuable.class);
				crit.add(Restrictions.in("id", batch));
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return crit.list();
			}
		});
		for (IdentificationContribuable identificationContribuable : list) {
			++rapport.nbMessagesTotal;
			boolean isIdentifie = identService.tenterIdentificationAutomatiqueContribuable(identificationContribuable);
			if (isIdentifie) {
				rapport.addIdentifies(identificationContribuable);
			}
			else {
				rapport.addNonIdentifies(identificationContribuable);
			}
		}
	}

	private List<Long> recupererMessageATraiter(Long idMessage) {
		if (idMessage == 0L) {
			final String queryMessage =//----------------------------------
					"select distinct identificationContribuable.id                        " +
							"from IdentificationContribuable identificationContribuable   " +
							" where identificationContribuable.etat in(:etats)            ";


			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
				@Override
				public List<Long> doInTransaction(TransactionStatus status) {

					final List<Long> idsMessage = hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
						@Override
						public List<Long> doInHibernate(Session session) throws HibernateException {
							final Query queryObject = session.createQuery(queryMessage);
							final List<String> etats = new ArrayList<>();
							etats.add(IdentificationContribuable.Etat.A_EXPERTISER.name());
							etats.add(IdentificationContribuable.Etat.A_EXPERTISER_SUSPENDU.name());
							etats.add(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT.name());
							etats.add(IdentificationContribuable.Etat.A_TRAITER_MAN_SUSPENDU.name());
							etats.add(IdentificationContribuable.Etat.EXCEPTION.name());
							queryObject.setParameterList("etats", etats);
							//noinspection unchecked
							return queryObject.list();
						}
					});
					Collections.sort(idsMessage);
					return idsMessage;
				}

			});
			return ids;
		}
		else {
			final List<Long> ids = new ArrayList<>();
			ids.add(idMessage);
			return ids;

		}
	}
}
