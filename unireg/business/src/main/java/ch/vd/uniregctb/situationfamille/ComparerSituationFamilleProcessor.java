package ch.vd.uniregctb.situationfamille;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamilleDAO;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.TiersService;

public class ComparerSituationFamilleProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ComparerSituationFamilleProcessor.class);
	private final ServiceCivilService serviceCivil;
	private final PlatformTransactionManager transactionManager;
	private final SituationFamilleDAO situationFamilleDAO;
	private final TiersService tiersService;
	private final int batchSize = BATCH_SIZE;
	private final ThreadLocal<ComparerSituationFamilleResults> rapport = new ThreadLocal<ComparerSituationFamilleResults>();

	public ComparerSituationFamilleProcessor(ServiceCivilService serviceCivil,SituationFamilleDAO situationFamilleDAO,TiersService tiersService, PlatformTransactionManager transactionManager) {

		this.serviceCivil = serviceCivil;
		this.transactionManager = transactionManager;
		this.situationFamilleDAO = situationFamilleDAO;
		this.tiersService = tiersService;

	}


	public ComparerSituationFamilleResults run(final RegDate dateTraitement, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ComparerSituationFamilleResults rapportFinal = new ComparerSituationFamilleResults(dateTraitement);
		status.setMessage("Récupération des Situations de familles à comparer...");
		final List<Long> ids = recupererSituationFamilleAComparer();

		// Reussi les messages par lots
		final ParallelBatchTransactionTemplate<Long, ComparerSituationFamilleResults>
				template = new ParallelBatchTransactionTemplate<Long, ComparerSituationFamilleResults>(ids, batchSize, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, situationFamilleDAO.getHibernateTemplate());
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, ComparerSituationFamilleResults>() {

			@Override
			public ComparerSituationFamilleResults createSubRapport() {
				return new ComparerSituationFamilleResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ComparerSituationFamilleResults r) throws Exception {

				rapport.set(r);
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch);
				return true;
			}
		});

		final int count = rapportFinal.listeSituationsDifferentes.size();

		if (status.interrupted()) {
			status.setMessage("La comparaison des situations de famille a été interrompue."
					+ " Nombre de situation comparées au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La comparaison des situations de famille est terminée."
					+"Nombre de situations comparés = "+rapportFinal.nbSituationTotal+". Nombre de situations différentes = " + count +  ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;

	}

	private void traiterBatch(final List<Long> batch) throws Exception {

		// On charge tous les contribuables en vrac (avec préchargement des situations)
        final List<SituationFamilleMenageCommun> list = situationFamilleDAO.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<SituationFamilleMenageCommun>>() {
            @Override
            public List<SituationFamilleMenageCommun> doInHibernate(Session session) throws HibernateException {
                final Criteria crit = session.createCriteria(SituationFamilleMenageCommun.class);
                crit.add(Restrictions.in("id", batch));
                crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	            //noinspection unchecked
	            return crit.list();
            }
        });

		for (SituationFamilleMenageCommun situation : list) {
			rapport.get().nbSituationTotal++;
			PersonnePhysique personne = (PersonnePhysique)tiersService.getTiers(situation.getContribuablePrincipalId());
			final Long numeroIndividu = personne.getNumeroIndividu();
			if(numeroIndividu!=null){
				EtatCivil etatCivil =  serviceCivil.getEtatCivilActif(numeroIndividu,null);
				if(situation.getEtatCivil() != EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil())){
					rapport.get().addSituationsDifferentes(situation,etatCivil);
				}
			}

		}

	}


	private List<Long> recupererSituationFamilleAComparer() {

		final String queryMessage =//----------------------------------
				"select distinct situationFamilleMenageCommun.id                        " +
						"from SituationFamilleMenageCommun situationFamilleMenageCommun             " +
						"where situationFamilleMenageCommun.dateFin is null            " +
						"and situationFamilleMenageCommun.annulationDate is null        ";


		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {

				final List<Long> idsMessage = situationFamilleDAO.getHibernateTemplate().executeWithNewSession(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryMessage);
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
}
