package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
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
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.TiersService;

public class IdentifierContribuableProcessor {

	private static final int BATCH_SIZE = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentifierContribuableProcessor.class);

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
		final ParallelBatchTransactionTemplateWithResults<Long, IdentifierContribuableResults>
				template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
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

		if (status.isInterrupted()) {
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
							final Set<IdentificationContribuable.Etat> etats = EnumSet.of(IdentificationContribuable.Etat.A_EXPERTISER,
							                                                              IdentificationContribuable.Etat.A_EXPERTISER_SUSPENDU,
							                                                              IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT,
							                                                              IdentificationContribuable.Etat.A_TRAITER_MAN_SUSPENDU,
							                                                              IdentificationContribuable.Etat.EXCEPTION);
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
