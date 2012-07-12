package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeleFeuille;
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
	private final PeriodeImpositionService periodeImpositionService;

	private final int tailleLot;


	private static class Cache {
		public final CollectiviteAdministrative cedi;
		public final CollectiviteAdministrative aci;
		public final PeriodeFiscale periode;
		public final Set<ModeleFeuilleDocument> setAnnexeImmeuble;

		public Cache(CollectiviteAdministrative cedi, CollectiviteAdministrative aci, PeriodeFiscale periode) {
			this.cedi = cedi;
			this.periode = periode;
			this.aci = aci;

			ModeleFeuilleDocument modeleAnnexeImmeuble = new ModeleFeuilleDocument();
			modeleAnnexeImmeuble.setModeleDocument(null);
			modeleAnnexeImmeuble.setIntituleFeuille(ModeleFeuille.ANNEXE_320.getDescription());
			modeleAnnexeImmeuble.setNumeroFormulaire(ModeleFeuille.ANNEXE_320.getCode());

			setAnnexeImmeuble = new HashSet<ModeleFeuilleDocument>();
			setAnnexeImmeuble.add(modeleAnnexeImmeuble);
		}
	}

	private Cache cache;
	private EnvoiAnnexeImmeubleResults rapport;

	public EnvoiAnnexeImmeubleEnMasseProcessor(TiersService tiersService, HibernateTemplate hibernateTemplate, ModeleDocumentDAO modeleDAO,
	                                           PeriodeFiscaleDAO periodeDAO, DeclarationImpotService diService, int tailleLot,
	                                           PlatformTransactionManager transactionManager,
	                                           ServiceCivilCacheWarmer serviceCivilCacheWarmer, PeriodeImpositionService periodeImpositionService) {
		this.tiersService = tiersService;
		this.hibernateTemplate = hibernateTemplate;
		this.modeleDAO = modeleDAO;
		this.periodeDAO = periodeDAO;
		this.tailleLot = tailleLot;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.periodeImpositionService = periodeImpositionService;
		Assert.isTrue(tailleLot > 0);
	}

	public EnvoiAnnexeImmeubleResults run(final int anneePeriode, final List<ContribuableAvecImmeuble> listCtbImmo, final int nbMax,
	                                      final RegDate dateTraitement, StatusManager s) throws DeclarationException {
		Assert.isTrue(rapport == null);

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final EnvoiAnnexeImmeubleResults rapportFinal = new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbMax);

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

				if (!batch.isEmpty()) {
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
			final Contribuable ctb = (Contribuable) tiersService.getTiers(ctbImmeuble.getNumeroContribuable());

			final PeriodeImposition pi = getPeriodeImpositionEnFinDePeriodeFiscale(ctb, anneePeriode);
			if (pi == null) {
				rapport.addIgnoreCtbNonAssujetti(ctb, anneePeriode);
			}
			else {
				final int nombreAnnexesImmeuble = getNombreAnnexeAEnvoyer(ctbImmeuble.getNombreImmeubles());

				final RegDate dateReference = RegDate.get(anneePeriode, 12, 31);
				ForGestion forGestion = tiersService.getForGestionActif(ctb, dateReference);
				int noOfsCommune = 0;
				if (forGestion != null) {
					noOfsCommune = forGestion.getNoOfsCommune();
				}

				// [SIFISC-3291] Tentative d'estimation du numéro de séquence de la DI envoyée
				final int noSequence = getNoSequenceAnnexeImmeuble(ctb, pi);
				final InformationsDocumentAdapter infoFormulaireImmeuble = new InformationsDocumentAdapter(ctb, noSequence, anneePeriode, dateReference,
						dateReference, dateReference, noOfsCommune, cache.cedi.getId(), Qualification.MANUEL, pi.getCodeSegment(),
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, null);


				final int nombreAnnexesImprimees = imprimerAnnexeImmeuble(infoFormulaireImmeuble, cache.setAnnexeImmeuble, dateTraitement, nombreAnnexesImmeuble);
				rapport.addInfoCtbTraites(ctb, nombreAnnexesImprimees);
				rapport.addCtbTraites(ctb.getId());
			}


		}
	}

	protected static int getNoSequenceAnnexeImmeuble(Contribuable ctb, DateRange pi) {
		final List<Declaration> decls = ctb.getDeclarationsForPeriode(pi.getDateFin().year(), true);
		final int noSequence;
		if (decls == null || decls.isEmpty()) {
			noSequence = 1;
		}
		else {
			final Declaration declFinPeriode = ctb.getDeclarationActive(pi.getDateFin());
			if (declFinPeriode != null) {
				// il y a déjà une DI valide à la fin de la période considérée... on reprend donc le même numéro!
				noSequence = ((DeclarationImpotOrdinaire) declFinPeriode).getNumero();
			}
			else {
				// attribution d'un nouveau numéro -> la prochaine fois que l'on générera une DI, c'est ce numéro
				// qui sera utilisé... On espère juste que c'est bien cette DI qui sera finalement envoyée au contribuable...
				noSequence = decls.size() + 1;
			}
		}
		return noSequence;
	}

	private int imprimerAnnexeImmeuble(InformationsDocumentAdapter infosDocuments, Set<ModeleFeuilleDocument> listeModele, RegDate dateTraitement, int nombreAnnexesImmeuble) throws
			DeclarationException {
		return diService.envoiAnnexeImmeubleForBatch(infosDocuments, listeModele, dateTraitement, nombreAnnexesImmeuble);
	}

	/**
	 * On ne prend que les contribuables qui ont une période d'imposition qui va jusqu'à la fin de l'année (pour éliminer les sourciers purs, pour lesquels on ne trouvera pas d'OID de gestion)
	 *
	 * @param ctb     contribuable à tester
	 * @param periode année fiscale
	 * @return la période d'imposition en fin de période fiscale s'il y en a une, <code>null</code> dans le cas contraire
	 */
	protected PeriodeImposition getPeriodeImpositionEnFinDePeriodeFiscale(Contribuable ctb, int periode) {
		List<PeriodeImposition> list = null;
		try {
			list = periodeImpositionService.determine(ctb, periode);
		}
		catch (AssujettissementException e) {
			rapport.addErrorException(ctb, e);
		}
		final RegDate date = RegDate.get(periode, 12, 31);
		if (list != null) {
			final PeriodeImposition periodeImposition = DateRangeHelper.rangeAt(list, date);
			if (periodeImposition != null) {
				return periodeImposition;
			}
		}

		return null;
	}

	/**
	 * Determine le nombre d'annexe à envoyer selon le nombre d'immeuble
	 *
	 * @param nombreImmeuble
	 * @return le nombre d'immeuble divisé par 2 arondi à l'entier supérieur
	 */
	protected int getNombreAnnexeAEnvoyer(int nombreImmeuble) {
		final int nbreAnnexeCalcule = Double.valueOf(Math.ceil(nombreImmeuble / 2.0)).intValue();
		//[SIFISC-2485] les annexes sotn à envoyer à double.
		return nbreAnnexeCalcule * 2;

	}

	/**
	 * Initialise les données pré-cachées pour éviter de les recharger plusieurs fois de la base de données.
	 *
	 * @param anneePeriode l'année fiscale considérée
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

		cache = new Cache(cedi, aci, periode);
	}


	private List<Long> getIdCtb(List<ContribuableAvecImmeuble> listeCtbImmeuble) {

		final List<Long> idsCtb = new ArrayList<Long>();

		for (ContribuableAvecImmeuble ctbImmeuble : listeCtbImmeuble) {

			idsCtb.add(ctbImmeuble.getNumeroContribuable());
		}
		return idsCtb;

	}


}
