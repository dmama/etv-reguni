package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRangeHelper;
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
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
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

	public EnvoiAnnexeImmeubleResults run(final int anneePeriode, final List<ContribuableAvecImmeuble> listCtbImmo, final int nbMax,
	                                      final RegDate dateTraitement, StatusManager s) throws DeclarationException {
		Assert.isTrue(rapport == null);

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final EnvoiAnnexeImmeubleResults rapportFinal = new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbMax);


		// certains contribuables ne recoivent pas de DI du canton (par exemple les diplomates suisses)

		status.setMessage("Récupération des contribuables à traiter...");

		// Traite les contribuables par lots
		final BatchTransactionTemplate<ContribuableAvecImmeuble, EnvoiAnnexeImmeubleResults> template =
				new BatchTransactionTemplate<ContribuableAvecImmeuble, EnvoiAnnexeImmeubleResults>(listCtbImmo, tailleLot, Behavior.REPRISE_AUTOMATIQUE,
						transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<ContribuableAvecImmeuble, EnvoiAnnexeImmeubleResults>() {

			@Override
			public EnvoiAnnexeImmeubleResults createSubRapport() {
				return new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbMax);
			}

			@Override
			public boolean doInTransaction(List<ContribuableAvecImmeuble> batch, EnvoiAnnexeImmeubleResults r) throws Exception {
				rapport = r;

				status.setMessage("Traitement du batch [" + batch.get(0).getNumeroContribuable() + "; " + batch.get(batch.size() - 1).getNumeroContribuable() + "] ...", percent);

				if (nbMax > 0 && rapportFinal.nbCtbsTotal + batch.size() >= nbMax) {
					// limite le nombre de contribuable pour ne pas dépasser le nombre max
					int reducedSize = nbMax - rapportFinal.nbCtbsTotal;
					batch = batch.subList(0, reducedSize);
				}

				if (batch.size() > 0) {
					traiterBatch(batch, anneePeriode, dateTraitement);
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
	 * @param listCtbImmeuble les listCtbImmeuble des contribuables à traiter
	 * @param anneePeriode    l'année fiscale considérée
	 * @param dateTraitement  la date de traitement
	 * @param nbAnnexeMax
	 * @throws ch.vd.uniregctb.declaration.DeclarationException
	 *          en cas d'erreur dans le traitement d'un contribuable.
	 */
	protected void traiterBatch(List<ContribuableAvecImmeuble> listCtbImmeuble, int anneePeriode, RegDate dateTraitement)
			throws DeclarationException {
		// pré-chauffage du cache des individus du civil
		final List<Long> idsCtb = getIdCtb(listCtbImmeuble);
		if (serviceCivilCacheWarmer != null) {
			serviceCivilCacheWarmer.warmIndividusPourTiers(idsCtb, null, AttributeIndividu.ADRESSES);
		}
		rapport.nbCtbsTotal += listCtbImmeuble.size();

		initCache(anneePeriode);

		for (ContribuableAvecImmeuble ctbImmeuble : listCtbImmeuble) {
			int nombreAnnexesImmeuble;
			nombreAnnexesImmeuble = 0;
			Contribuable ctb = (Contribuable) tiersService.getTiers(ctbImmeuble.getNumeroContribuable());

			if (!isAssujettiEnFinDePeriode(ctb, anneePeriode)) {
				rapport.addIgnoreCtbNonAssujetti(ctb, anneePeriode);
			}
			else {
				nombreAnnexesImmeuble = getNombreAnnexeAEnvoyer(ctbImmeuble.getNombreImmeubles());

				final RegDate dateReference = RegDate.get(anneePeriode, 12, 31);
				ForGestion forGestion = tiersService.getForGestionActif(ctb, dateReference);
				int noOfsCommune = 0;
				if (forGestion != null) {
					noOfsCommune = forGestion.getNoOfsCommune();
				}

				InformationsDocumentAdapter infoFormulaireImmeuble = new InformationsDocumentAdapter(ctb, ctb.getId().intValue(), anneePeriode, dateReference,
						dateReference, dateReference, noOfsCommune, cache.cedi.getId(), Qualification.MANUEL,
						EnvoiAnnexeImmeubleJob.NAME, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);

				final int nombreAnnexesImprimees = imprimerAnnexeImmeuble(infoFormulaireImmeuble, cache.modele.getModelesFeuilleDocument(), dateTraitement, nombreAnnexesImmeuble);
				rapport.addInfoCtbTraites(ctb, nombreAnnexesImprimees);
				rapport.addCtbTraites(ctb.getId());
			}


		}
	}

	private int imprimerAnnexeImmeuble(InformationsDocumentAdapter infosDocuments, Set<ModeleFeuilleDocument> listeModele, RegDate dateTraitement, int nombreAnnexesImmeuble) throws DeclarationException {
		return diService.envoiAnnexeImmeubleForBatch(infosDocuments, listeModele, dateTraitement, nombreAnnexesImmeuble);
	}

	/**
	 * On ne prend que les contribuables qui ont une période d'imposition qui va
	 * jusqu'à la fin de l'année (pour éliminer les sourciers purs, pour lesquels on ne trouvera pas d'OID de gestion)
	 * @param ctb contribuable à tester
	 * @param periode année fiscale
	 * @return <code>true</code> si le contribuable a une période d'imposition en fin d'année fiscale, <code>false</code> sinon
	 */
	protected boolean isAssujettiEnFinDePeriode(Contribuable ctb, int periode) {
		List<PeriodeImposition> list = null;
		try {
			list = PeriodeImposition.determine(ctb, periode);
		}
		catch (AssujettissementException e) {
			//ajouter erreur e calcul d'assujetissement
		}
		final RegDate date = RegDate.get(periode, 12, 31);
		if (list != null) {
			final PeriodeImposition periodeImposition = DateRangeHelper.rangeAt(list, date);
			if (periodeImposition != null) {
				return true;
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
		return Double.valueOf(Math.ceil(nombreImmeuble / 2.0)).intValue();

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


	private List<Long> getIdCtb(List<ContribuableAvecImmeuble> listeCtbImmeuble) {

		final List<Long> idsCtb = new ArrayList<Long>();

		for (ContribuableAvecImmeuble ctbImmeuble : listeCtbImmeuble) {

			idsCtb.add(ctbImmeuble.getNumeroContribuable());
		}
		return idsCtb;

	}


}
