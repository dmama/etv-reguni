package ch.vd.unireg.situationfamille;

import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.TiersService;

public class ComparerSituationFamilleProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComparerSituationFamilleProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final ServiceCivilService serviceCivil;
	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final AdresseService adresseService;
	private final TiersService tiersService;

	public ComparerSituationFamilleProcessor(ServiceCivilService serviceCivil, HibernateTemplate hibernateTemplate, TiersService tiersService, PlatformTransactionManager transactionManager,
	                                         AdresseService adresseService) {

		this.serviceCivil = serviceCivil;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.adresseService = adresseService;
	}


	public ComparerSituationFamilleResults run(final RegDate dateTraitement, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ComparerSituationFamilleResults rapportFinal = new ComparerSituationFamilleResults(dateTraitement, tiersService, adresseService);
		status.setMessage("Récupération des Situations de familles à comparer...");
		final List<Long> ids = recupererSituationFamilleAComparer();

		// Reussi les messages par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, ComparerSituationFamilleResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, ComparerSituationFamilleResults>() {

			@Override
			public ComparerSituationFamilleResults createSubRapport() {
				return new ComparerSituationFamilleResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ComparerSituationFamilleResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, r);
				return true;
			}
		}, progressMonitor);

		final int count = rapportFinal.listeSituationsDifferentes.size();

		if (status.isInterrupted()) {
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

	private void traiterBatch(final List<Long> batch, ComparerSituationFamilleResults r) throws Exception {

		// On charge tous les contribuables en vrac (avec préchargement des situations)
        final List<SituationFamilleMenageCommun> list = hibernateTemplate.execute(session -> {
	        final Criteria crit = session.createCriteria(SituationFamilleMenageCommun.class);
	        crit.add(Restrictions.in("id", batch));
	        crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	        //noinspection unchecked
	        return (List<SituationFamilleMenageCommun>) crit.list();
        });

		for (SituationFamilleMenageCommun situation : list) {
			r.nbSituationTotal++;
			PersonnePhysique personne = (PersonnePhysique)tiersService.getTiers(situation.getContribuablePrincipalId());
			final Long numeroIndividu = personne.getNumeroIndividu();
			if(numeroIndividu!=null){
				EtatCivil etatCivil =  serviceCivil.getEtatCivilActif(numeroIndividu,null);
				if(situation.getEtatCivil() != EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil())){
					r.addSituationsDifferentes(situation,etatCivil);
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

		final List<Long> ids = template.execute(status -> {
			final List<Long> idsMessage = hibernateTemplate.executeWithNewSession(session -> {
				final Query queryObject = session.createQuery(queryMessage);
				//noinspection unchecked
				return (List<Long>) queryObject.list();
			});
			Collections.sort(idsMessage);
			return idsMessage;
		});
		return ids;
	}
}
