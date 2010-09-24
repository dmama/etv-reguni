package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsAEmettreProcessor.ExistenceResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsEnMasseProcessor.DeclarationsCache;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatTache;

public class ProduireListeDIsNonEmisesProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ProduireListeDIsNonEmisesProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	private final PeriodeFiscaleDAO periodeDAO;
	private final ModeleDocumentDAO modeleDocumentDAO;
	private final TacheDAO tacheDAO;

	private final TiersService tiersService;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final ParametreAppService parametres;

	private DeterminationDIsAEmettreProcessor determinationDIsAEmettreProcessor;
	private EnvoiDIsEnMasseProcessor envoiDIsEnMasseProcessor;

	private ListeDIsNonEmises rapport;

	public ProduireListeDIsNonEmisesProcessor(HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO,
			ModeleDocumentDAO modeleDocumentDAO, TacheDAO tacheDAO, TiersService tiersService, DelaisService delaisService,
			DeclarationImpotService diService, PlatformTransactionManager transactionManager, ParametreAppService parametres) {
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.modeleDocumentDAO = modeleDocumentDAO;
		this.tacheDAO = tacheDAO;
		this.tiersService = tiersService;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.parametres = parametres;
	}

	public ListeDIsNonEmises run(final int anneePeriode, final RegDate dateTraitement, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		this.envoiDIsEnMasseProcessor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDocumentDAO, periodeDAO, delaisService, diService, 1, transactionManager, parametres);
		this.determinationDIsAEmettreProcessor = new DeterminationDIsAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO, parametres, tiersService, transactionManager);

		final ListeDIsNonEmises rapportFinal = new ListeDIsNonEmises(anneePeriode, dateTraitement);

		status.setMessage("Récupération des contribuables à vérifier...");

		final List<Long> ids = determinationDIsAEmettreProcessor.createListeIdsContribuables(anneePeriode);

		// Traite les contribuables par lots
		final BatchTransactionTemplate<Long, ListeDIsNonEmises> template = new BatchTransactionTemplate<Long, ListeDIsNonEmises>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, ListeDIsNonEmises>() {

			@Override
			public ListeDIsNonEmises createSubRapport() {
				return new ListeDIsNonEmises(anneePeriode, dateTraitement);
			}

			@Override
			public void afterTransactionStart(TransactionStatus status) {
				super.afterTransactionStart(status);
				status.setRollbackOnly();       // pour être vraiment sûr !
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ListeDIsNonEmises r) throws Exception {

				rapport = r;
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch, anneePeriode, dateTraitement);
				return true;
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	protected void traiterBatch(List<Long> batch, int anneePeriode, RegDate dateTraitement) throws DeclarationException, AssujettissementException {

		this.envoiDIsEnMasseProcessor.initCache(anneePeriode, TypeContribuableDI.VAUDOIS_ORDINAIRE);

		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
		if (periode == null) {
			throw new DeclarationException("La période fiscale " + anneePeriode + " n'existe pas dans la base de données.");
		}

		// Traite tous les contribuables
		for (Long id : batch) {
			traiterContribuable(id, periode, dateTraitement);
		}

	}

	private void traiterContribuable(Long id, PeriodeFiscale periode, RegDate dateTraitement) throws DeclarationException, AssujettissementException {

		rapport.nbCtbsTotal++;

		final Contribuable contribuable = (Contribuable) hibernateTemplate.get(Contribuable.class, id);
		if (contribuable.validate().hasErrors()) {
			LOGGER.info("Le ctb n'a pas pu recevoir de DI car il est invalide");
			return;
		}

		final List<PeriodeImposition> details = determinationDIsAEmettreProcessor.determineDetailsEnvoi(contribuable, periode.getAnnee());
		if (details == null) {
			return;
		}

		for (PeriodeImposition d : details) {
			traiterDetails(d, contribuable, periode, dateTraitement);
		}
	}

	private void traiterDetails(PeriodeImposition details, Contribuable contribuable, PeriodeFiscale periode, RegDate dateTraitement) throws DeclarationException {
		
		final RegDate datePeriode = RegDate.get(periode.getAnnee());

		// Le contribuable est sensé avoir été assujetti.
		final Declaration declarationActive = contribuable.getDeclarationActive(datePeriode);
		if (declarationActive != null && !declarationActive.isAnnule()) {
			LOGGER.info("DI ok pour " + contribuable.toString());
			return;
		}

		TacheEnvoiDeclarationImpot tache = determinationDIsAEmettreProcessor.traiterPeriodeImposition(contribuable, periode, details);
		if (tache == null) {
			ExistenceResults<TacheEnvoiDeclarationImpot> res = determinationDIsAEmettreProcessor.checkExistenceTache(contribuable, details);
			if (res == null) {
				rapport.addNonEmisePourRaisonInconnue(contribuable.getId(), null, null);
			}
			else {
				tache = res.getObject();
				switch (tache.getEtat()) {
				case EN_COURS:
					// Reellement ce cas ne devrait pas se produire ...
					rapport.addEntrainDEtreEmise(contribuable.getId());
					return;
				case EN_INSTANCE:
					rapport.addTacheNonTraitee(contribuable.getId());
					return;
				case TRAITE:
					// Cas ou la tache à était traité et la DI non émises, il va falloir simuler !
					tache.setEtat(TypeEtatTache.EN_INSTANCE);
					break;
				}
			}
		}

		List<Long> ids = new ArrayList<Long>();
		ids.add(contribuable.getId());
		final DeclarationsCache cache = envoiDIsEnMasseProcessor.new DeclarationsCache(periode.getAnnee(), ids);

		envoiDIsEnMasseProcessor.setRapport(rapport);
		boolean tacheTraitee = envoiDIsEnMasseProcessor.traiterTache(tache, dateTraitement, cache, true);
		if (tacheTraitee) {
			// Incoherence !
			LOGGER.info("Impossible de determiner pourquoi la DI n'est pas émise pour " + contribuable.toString());
			rapport.addNonEmisePourRaisonInconnue(contribuable.getId(), tache.getDateDebut(), tache.getDateFin());
		}
		else {
			LOGGER.info("DI non émise pour " + contribuable.toString());
			// pas de log, celui-ci a était fait dans la simulation de l'envoi de la DI
		}
	}
}
