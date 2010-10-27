package ch.vd.uniregctb.situationfamille;

import java.util.ArrayList;
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
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamilleDAO;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.TiersService;

public class ComparerSituationFamilleProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ComparerSituationFamilleProcessor.class);
	private ServiceCivilService serviceCivil;
	private final PlatformTransactionManager transactionManager;
	private SituationFamilleDAO situationFamilleDAO;
	private TiersService tiersService;
	private int batchSize = BATCH_SIZE;
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
        final List<SituationFamilleMenageCommun> list = (List<SituationFamilleMenageCommun>) situationFamilleDAO.getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(SituationFamilleMenageCommun.class);
                crit.add(Restrictions.in("id", batch));
                crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                return crit.list();
            }
        });

		for (SituationFamilleMenageCommun situation : list) {
			rapport.get().nbSituationTotal++;
			PersonnePhysique personne = (PersonnePhysique)tiersService.getTiers(situation.getContribuablePrincipalId());
			final Long numeroIndividu = personne.getNumeroIndividu();
			if(numeroIndividu!=null){
				EtatCivil etatCivil =  serviceCivil.getEtatCivilActif(numeroIndividu,null);
				if(!situation.getEtatCivil().equals(ch.vd.uniregctb.type.EtatCivil.from(etatCivil.getTypeEtatCivil()))){
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

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<Long> idsMessage = (List<Long>) situationFamilleDAO.getHibernateTemplate().executeWithNewSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						Query queryObject = session.createQuery(queryMessage);
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
