package ch.vd.unireg.adresse;

import java.util.Collections;
import java.util.List;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.TiersService;

public class ResolutionAdresseProcessor {

	private static final int BATCH_SIZE = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(ResolutionAdresseProcessor.class);

	private final AdresseService adresseService;
	private final ServiceInfrastructureService infraService;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final HibernateTemplate hibernateTemplate;

	public ResolutionAdresseProcessor(AdresseService adresseService, ServiceInfrastructureService infraService, PlatformTransactionManager transactionManager,
	                                  TiersService tiersService, HibernateTemplate hibernateTemplate) {
		this.adresseService = adresseService;
		this.infraService = infraService;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.hibernateTemplate = hibernateTemplate;
	}

	public ResolutionAdresseResults run(final RegDate dateTraitement, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ResolutionAdresseResults rapportFinal = new ResolutionAdresseResults(dateTraitement, tiersService, adresseService);
		status.setMessage("Récupération des adresses à résoudre...");
		final List<Long> ids = recupererAdresseATraiter();

		// Reussi les messages par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, ResolutionAdresseResults>
				template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, ResolutionAdresseResults>() {

			@Override
			public ResolutionAdresseResults createSubRapport() {
				return new ResolutionAdresseResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ResolutionAdresseResults r) {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, r);
				return true;
			}
		}, progressMonitor);

		//On calcul le nombre total d'adresse traitées
		rapportFinal.nbAdresseTotal = rapportFinal.nbAdresseTotal + rapportFinal.erreurs.size();
		final int countTraites = rapportFinal.nbAdresseTotal;
		final int countResolu = rapportFinal.listeAdresseResolues.size();

		if (status.isInterrupted()) {
			status.setMessage("la résolution des adresses a été interrompue."
					+ " Nombre d'adresses traitées au moment de l'interruption = " + countTraites
					+ " Nombre d'adresses résolues au moment de l'interruption = " + countResolu);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("la la résolution des adresses est terminée."
					+ "Nombre d'adresse traitées = " + countTraites + ". Nombre d'adresses résolues = " + countResolu + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;

	}

	private void traiterBatch(final List<Long> batch, ResolutionAdresseResults r) {
		// Chargement des adresses
		final List<AdresseSuisse> list = hibernateTemplate.execute(session -> {
			final Query query = session.createQuery("from AdresseSuisse where id in (:ids)");
			query.setParameterList("ids", batch);
			//noinspection unchecked
			return (List<AdresseSuisse>) query.list();
		});
		for (AdresseSuisse adresseSuisse : list) {
			r.nbAdresseTotal++;
			ressoudreAdresse(adresseSuisse, r);
		}
	}

	private void ressoudreAdresse(AdresseSuisse adresseSuisse, ResolutionAdresseResults r) throws RuntimeException {
		Rue rue;
		final Integer numeroRueAdresse = adresseSuisse.getNumeroRue();
		try {
			rue = infraService.getRueByNumero(numeroRueAdresse);
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Tiers " + adresseSuisse.getTiers().getId() + " Impossible de trouver la rue avec le numéro  " + numeroRueAdresse + " Message d'erreur:" + e.getMessage());
		}

		if (rue == null) {
			throw new RuntimeException("Tiers " + adresseSuisse.getTiers().getId() + " La rue avec le numéro  " + numeroRueAdresse + " est inconnue !");
		}


		Integer numeroLocalite = rue.getNoLocalite();
		final Localite localite;
		try {
			localite = infraService.getLocaliteByONRP(numeroLocalite, adresseSuisse.getDateFin());
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Tiers " + adresseSuisse.getTiers().getId() + " Impossible de trouver la localite avec le numéro  " + numeroLocalite + " Message d'erreur:" + e.getMessage());
		}
		if (localite == null) {
			throw new RuntimeException("Tiers " + adresseSuisse.getTiers().getId() + " La localité avec le numéro " + numeroLocalite + " est inconnue !");
		}

		adresseSuisse.setRue(rue.getDesignationCourrier());
		adresseSuisse.setNumeroOrdrePoste(localite.getNoOrdre());
		r.addAdresseResolue(adresseSuisse, localite.getNom());

		//SUppression de la référence vers la rue
		adresseSuisse.setNumeroRue(null);

	}


	private List<Long> recupererAdresseATraiter() {

		final String queryMessage =//----------------------------------
				"select distinct adresseSuisse.id                        " +
						"from AdresseSuisse adresseSuisse                " +
						" where adresseSuisse.numeroRue is not null      ";


		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		//noinspection UnnecessaryLocalVariable
		final List<Long> ids = template.execute(status -> {
			final List<Long> idsAdresse = hibernateTemplate.execute(session -> {
				final Query queryObject = session.createQuery(queryMessage);
				//noinspection unchecked
				return (List<Long>) queryObject.list();
			});
			Collections.sort(idsAdresse);
			return idsAdresse;
		});
		return ids;
	}
}