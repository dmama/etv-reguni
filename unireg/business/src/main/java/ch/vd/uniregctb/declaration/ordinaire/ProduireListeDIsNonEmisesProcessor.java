package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsAEmettreProcessor.ExistenceResults;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsEnMasseProcessor.DeclarationsCache;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.ValidationService;

public class ProduireListeDIsNonEmisesProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = LoggerFactory.getLogger(ProduireListeDIsNonEmisesProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	private final PeriodeFiscaleDAO periodeDAO;
	private final ModeleDocumentDAO modeleDocumentDAO;
	private final TacheDAO tacheDAO;

	private final TiersService tiersService;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final ParametreAppService parametres;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final ValidationService validationService;
	private final PeriodeImpositionService periodeImpositionService;
	private final AdresseService adresseService;
	private final TicketService ticketService;

	private DeterminationDIsAEmettreProcessor determinationDIsAEmettreProcessor;
	private EnvoiDIsEnMasseProcessor envoiDIsEnMasseProcessor;

	public ProduireListeDIsNonEmisesProcessor(HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO,
	                                          ModeleDocumentDAO modeleDocumentDAO, TacheDAO tacheDAO, TiersService tiersService, DelaisService delaisService,
	                                          DeclarationImpotService diService, PlatformTransactionManager transactionManager, ParametreAppService parametres,
	                                          ServiceCivilCacheWarmer serviceCivilCacheWarmer, ValidationService validationService, PeriodeImpositionService periodeImpositionService,
	                                          AdresseService adresseService, TicketService ticketService) {
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.modeleDocumentDAO = modeleDocumentDAO;
		this.tacheDAO = tacheDAO;
		this.tiersService = tiersService;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.parametres = parametres;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.validationService = validationService;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
		this.ticketService = ticketService;
	}

	public ListeDIsNonEmises run(final int anneePeriode, final RegDate dateTraitement, @Nullable StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		this.envoiDIsEnMasseProcessor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDocumentDAO, periodeDAO, delaisService, diService, 1, transactionManager, parametres,
		                                                             serviceCivilCacheWarmer, adresseService, ticketService);
		this.determinationDIsAEmettreProcessor = new DeterminationDIsAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO, parametres, tiersService, transactionManager, validationService,
		                                                                               periodeImpositionService, adresseService);

		final ListeDIsNonEmises rapportFinal = new ListeDIsNonEmises(anneePeriode, dateTraitement, tiersService, adresseService);

		status.setMessage("Récupération des contribuables à vérifier...");

		final List<Long> ids = determinationDIsAEmettreProcessor.createListeIdsContribuables(anneePeriode);

		// Traite les contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, ListeDIsNonEmises>
				template = new BatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, ListeDIsNonEmises>() {

			@Override
			public ListeDIsNonEmises createSubRapport() {
				return new ListeDIsNonEmises(anneePeriode, dateTraitement, tiersService, adresseService);
			}

			@Override
			public void afterTransactionStart(TransactionStatus status) {
				super.afterTransactionStart(status);
				status.setRollbackOnly();       // pour être vraiment sûr !
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ListeDIsNonEmises r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, anneePeriode, dateTraitement, r);
				return true;
			}
		}, progressMonitor);

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	protected void traiterBatch(List<Long> batch, int anneePeriode, RegDate dateTraitement, ListeDIsNonEmises r) throws DeclarationException, AssujettissementException {

		final EnvoiDIsEnMasseProcessor.Cache cache = this.envoiDIsEnMasseProcessor.initCache(anneePeriode, CategorieEnvoiDI.VAUDOIS_COMPLETE);

		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
		if (periode == null) {
			throw new DeclarationException("La période fiscale " + anneePeriode + " n'existe pas dans la base de données.");
		}

		// Traite tous les contribuables
		for (Long id : batch) {
			traiterContribuable(id, periode, dateTraitement, cache, r);
		}

	}

	private void traiterContribuable(Long id, PeriodeFiscale periode, RegDate dateTraitement, EnvoiDIsEnMasseProcessor.Cache cache, ListeDIsNonEmises r) throws DeclarationException, AssujettissementException {

		r.nbCtbsTotal++;

		final Contribuable contribuable = hibernateTemplate.get(Contribuable.class, id);
		if (validationService.validate(contribuable).hasErrors()) {
			LOGGER.info("Le ctb n'a pas pu recevoir de DI car il est invalide");
			return;
		}

		final List<PeriodeImposition> details = determinationDIsAEmettreProcessor.determineDetailsEnvoi(contribuable, periode.getAnnee(), null);
		if (details == null) {
			return;
		}

		for (PeriodeImposition d : details) {
			traiterDetails(d, contribuable, periode, dateTraitement, cache, r);
		}
	}

	private void traiterDetails(PeriodeImposition details, Contribuable contribuable, PeriodeFiscale periode, RegDate dateTraitement, EnvoiDIsEnMasseProcessor.Cache cache, ListeDIsNonEmises r) throws DeclarationException {
		
		final RegDate datePeriode = RegDate.get(periode.getAnnee());

		// Le contribuable est sensé avoir été assujetti.
		final Declaration declarationActive = contribuable.getDeclarationActive(datePeriode);
		if (declarationActive != null && !declarationActive.isAnnule()) {
			LOGGER.info("DI ok pour " + contribuable.toString());
			return;
		}

		TacheEnvoiDeclarationImpot tache = determinationDIsAEmettreProcessor.traiterPeriodeImposition(contribuable, periode, details, null);
		if (tache == null) {
			final ExistenceResults<TacheEnvoiDeclarationImpot> res = determinationDIsAEmettreProcessor.checkExistenceTache(contribuable, details);
			if (res == null) {
				r.addNonEmisePourRaisonInconnue(contribuable.getId(), null, null);
				return;
			}

			tache = res.getObject();
			switch (tache.getEtat()) {
			case EN_COURS:
				// Reellement ce cas ne devrait pas se produire ...
				r.addEntrainDEtreEmise(contribuable.getId());
				return;
			case EN_INSTANCE:
				r.addTacheNonTraitee(contribuable.getId());
				return;
			case TRAITE:
				// Cas ou la tache à était traité et la DI non émises, il va falloir simuler !
				tache.setEtat(TypeEtatTache.EN_INSTANCE);
				break;
			}
		}

		final List<Long> ids = new ArrayList<>();
		ids.add(contribuable.getId());
		final DeclarationsCache dcache = envoiDIsEnMasseProcessor.new DeclarationsCache(periode.getAnnee(), ids);

		final boolean tacheTraitee = envoiDIsEnMasseProcessor.traiterTache(tache, dateTraitement, r, cache, dcache, true);
		if (tacheTraitee) {
			// Incoherence !
			LOGGER.info("Impossible de determiner pourquoi la DI n'est pas émise pour " + contribuable.toString());
			r.addNonEmisePourRaisonInconnue(contribuable.getId(), tache.getDateDebut(), tache.getDateFin());
		}
		else {
			LOGGER.info("DI non émise pour " + contribuable.toString());
			// pas de log, celui-ci a était fait dans la simulation de l'envoi de la DI
		}
	}
}
