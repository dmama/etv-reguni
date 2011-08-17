package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeDocument;

public class EnvoiAnnexeImmeubleEnMasseProcessor {

	final Logger LOGGER = Logger.getLogger(EnvoiAnnexeImmeubleEnMasseProcessor.class);


	private final TiersService tiersService;

	private final HibernateTemplate hibernateTemplate;

	private final PeriodeFiscaleDAO periodeDAO;

	private final ModeleDocumentDAO modeleDAO;

	private final DeclarationImpotService diService;

	private final PlatformTransactionManager transactionManager;


	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	private final int tailleLot;


	private HashMap<Long, ContribuableAvecImmeuble> mapCtb;


	private static class Cache {
		public final CollectiviteAdministrative cedi;
		public final CollectiviteAdministrative aci;
		public final PeriodeFiscale periode;
		public final ModeleDocument modele;

		public Cache(CollectiviteAdministrative cedi, CollectiviteAdministrative aci, ModeleDocument modele, PeriodeFiscale periode) {
			this.cedi = cedi;
			this.modele = modele;
			this.periode = periode;
			this.aci = aci;
		}
	}

	private Cache cache;
	private EnvoiAnnexeImmeubleResults rapport;

	public EnvoiAnnexeImmeubleEnMasseProcessor(TiersService tiersService, HibernateTemplate hibernateTemplate, ModeleDocumentDAO modeleDAO,
	                                           PeriodeFiscaleDAO periodeDAO, DeclarationImpotService diService, int tailleLot,
	                                           PlatformTransactionManager transactionManager,
	                                           ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.tiersService = tiersService;
		this.hibernateTemplate = hibernateTemplate;
		this.modeleDAO = modeleDAO;
		this.periodeDAO = periodeDAO;
		this.tailleLot = tailleLot;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		Assert.isTrue(tailleLot > 0);
	}

	public EnvoiAnnexeImmeubleResults run(final int anneePeriode, final List<ContribuableAvecImmeuble> listCtbImmo, final int nbAnnexeMax,
	                                      final RegDate dateTraitement, StatusManager s) throws DeclarationException {
		final int nbMax = 0;
		Assert.isTrue(rapport == null); // Un rapport non null signifirait que l'appel a été fait par le batch des DI non émises

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final EnvoiAnnexeImmeubleResults rapportFinal = new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbAnnexeMax);
		mapCtb = getMapCtbImmeubleId(listCtbImmo);

		final List<Long> ctbImmeubleIds = new ArrayList<Long>();
		ctbImmeubleIds.addAll(mapCtb.keySet());
		Collections.sort(ctbImmeubleIds);

		// certains contribuables ne recoivent pas de DI du canton (par exemple les diplomates suisses)

		status.setMessage("Récupération des contribuables à traiter...");

		// Traite les contribuables par lots
		final BatchTransactionTemplate<Long, EnvoiAnnexeImmeubleResults> template =
				new BatchTransactionTemplate<Long, EnvoiAnnexeImmeubleResults>(ctbImmeubleIds, tailleLot, Behavior.REPRISE_AUTOMATIQUE,
						transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, EnvoiAnnexeImmeubleResults>() {

			@Override
			public EnvoiAnnexeImmeubleResults createSubRapport() {
				return new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbAnnexeMax);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, EnvoiAnnexeImmeubleResults r) throws Exception {
				rapport = r;

				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				if (nbMax > 0 && rapportFinal.nbCtbsTotal + batch.size() >= nbMax) {
					// limite le nombre de contribuable pour ne pas dépasser le nombre max
					int reducedSize = nbMax - rapportFinal.nbCtbsTotal;
					batch = batch.subList(0, reducedSize);
				}

				if (batch.size() > 0) {
					traiterBatch(batch, anneePeriode, dateTraitement, nbAnnexeMax);
				}

				return !rapportFinal.interrompu && (nbMax <= 0 || rapportFinal.nbCtbsTotal + batch.size() < nbMax);
			}
		});

		if (status.interrupted()) {
			status.setMessage("L'envoi en masse du formulaire immeuble a été interrompue."
					+ " Nombre de formulaire immeuble envoyées au moment de l'interruption = " + rapportFinal.ctbsTraites.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'envoi en masse des formulaire immeuble est terminée. Nombre de formulaire immeuble envoyées = "
					+ rapportFinal.ctbsTraites.size() + ". Nombre d'erreurs = " + rapportFinal.ctbsEnErrors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Pour le testing uniquement !
	 */
	protected void setRapport(EnvoiAnnexeImmeubleResults rapport) {
		this.rapport = rapport;
	}

	/**
	 * Traite tout le batch des contribuables, un par un.
	 *
	 * @param ids            les ids des contribuables à traiter
	 * @param anneePeriode   l'année fiscale considérée
	 * @param dateTraitement la date de traitement
	 * @param nbAnnexeMax
	 * @throws ch.vd.uniregctb.declaration.DeclarationException
	 *          en cas d'erreur dans le traitement d'un contribuable.
	 */
	protected void traiterBatch(List<Long> ids, int anneePeriode, RegDate dateTraitement, int nbAnnexeMax)
			throws DeclarationException {
		// pré-chauffage du cache des individus du civil

		if (serviceCivilCacheWarmer != null) {
			serviceCivilCacheWarmer.warmIndividusPourTiers(ids, null, AttributeIndividu.ADRESSES);
		}
		rapport.nbCtbsTotal += ids.size();

		initCache(anneePeriode);

		for (Long id : ids) {
			ContribuableAvecImmeuble ctbImmeuble = mapCtb.get(id);
			Contribuable ctb = (Contribuable) tiersService.getTiers(id);

			if (!isAssujettiEnFinDePeriode(ctb, anneePeriode)) {
				rapport.addIgnoreCtbNonAssujetti(ctb, anneePeriode);
			}

			int nombreAnnexe = getNombreAnnexeAEnvoyer(ctbImmeuble.getNombreImmeuble());
			if (nombreAnnexe > nbAnnexeMax) {
				nombreAnnexe = nbAnnexeMax;
			}
			InformationsDocumentAdapter infoFormulaireImmeuble = new InformationsDocumentAdapter();
			infoFormulaireImmeuble.setAnnee(anneePeriode);
			infoFormulaireImmeuble.setNbAnnexes(nbAnnexeMax);
			infoFormulaireImmeuble.setTiers(ctb);
			infoFormulaireImmeuble.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
			infoFormulaireImmeuble.setCollId(cache.cedi.getId());
			final RegDate dateReference = RegDate.get(anneePeriode, 12, 31);
			infoFormulaireImmeuble.setDateReference(dateReference);
			infoFormulaireImmeuble.setDeclarationImpotOrdinaire(false);
			infoFormulaireImmeuble.setDelaiRetourImprime(dateReference);
			infoFormulaireImmeuble.setDelaiAccorde(dateReference);
			infoFormulaireImmeuble.setModifUser(EnvoiAnnexeImmeubleJob.NAME);
			infoFormulaireImmeuble.setQualification(Qualification.MANUEL);
			infoFormulaireImmeuble.setIdDocument(ctb.getId().intValue());
			ForGestion forGestion = tiersService.getForGestionActif(ctb, dateReference);
			if (forGestion != null) {
				infoFormulaireImmeuble.setNoOfsCommune(forGestion.getNoOfsCommune());
			}

			imprimerAnnexeImmeuble(infoFormulaireImmeuble, cache.modele.getModelesFeuilleDocument(), dateTraitement);
			rapport.addCtbTraites(ctb.getId());

		}


	}


	private void imprimerAnnexeImmeuble(InformationsDocumentAdapter infosDocuments, Set<ModeleFeuilleDocument> listeModele, RegDate dateTraitement) throws DeclarationException {
		diService.envoiAnnexeImmeubleForBatch(infosDocuments, listeModele, dateTraitement);
	}

	protected boolean isAssujettiEnFinDePeriode(Contribuable ctb, int periode) {
		List<Assujettissement> list = null;
		try {
			list = Assujettissement.determine(ctb, periode);
		}
		catch (AssujettissementException e) {
			//ajouter erreur e calcul d'assujetissement
		}
		RegDate date = RegDate.get(periode, 12, 31);
		if (list != null && !list.isEmpty()) {
			for (Assujettissement a : list) {
				if (a.isValidAt(date)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determine le nombre d'annexe à envoyer selon le nombre d'immeuble
	 *
	 * @param nombreImmeuble
	 * @return le nombre d'immeuble divisé par 2 arondi à l'entier supérieur
	 */
	protected int getNombreAnnexeAEnvoyer(int nombreImmeuble) {
		return Double.valueOf(Math.ceil(nombreImmeuble / 2)).intValue();

	}

	/**
	 * Initialise les données pré-cachées pour éviter de les recharger plusieurs fois de la base de données.
	 *
	 * @param anneePeriode l'année fiscale considérée
	 * @param categorie    la catégorie de contribuable considérée
	 * @throws ch.vd.uniregctb.declaration.DeclarationException
	 *          en cas d'erreur dans le traitement d'un contribuable.
	 */
	protected void initCache(int anneePeriode) throws DeclarationException {

		// Récupère le CEDI
		CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		if (cedi == null) {
			throw new DeclarationException("Impossible de charger le centre d'enregistrement des déclarations d'impôt (CEDI).");
		}

		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);
		if (aci == null) {
			throw new DeclarationException("Impossible de charger la collectivité administrative de l'administration cantonale des impôts (ACI).");
		}

		// Récupère la période fiscale
		PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriode);
		if (periode == null) {
			throw new DeclarationException("La période fiscale [" + anneePeriode + "] n'existe pas dans la base de données.");
		}

		// Récupère le modèle de document
		ModeleDocument modele = modeleDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		if (modele == null) {
			throw new DeclarationException("Impossible de trouver le modèle de document pour une déclaration d'impôt pour la période ["
					+ periode.getAnnee() + "] et le type de document [" + TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH.name() + "].");
		}

		cache = new Cache(cedi, aci, modele, periode);
	}


	private HashMap<Long, ContribuableAvecImmeuble> getMapCtbImmeubleId(List<ContribuableAvecImmeuble> listeCtbImmeuble) {

		HashMap<Long, ContribuableAvecImmeuble> mapCtb = new HashMap<Long, ContribuableAvecImmeuble>(listeCtbImmeuble.size());

		for (ContribuableAvecImmeuble ctbImmeuble : listeCtbImmeuble) {

			mapCtb.put(ctbImmeuble.getNumeroContribuable(), ctbImmeuble);
		}
		return mapCtb;

	}


}
