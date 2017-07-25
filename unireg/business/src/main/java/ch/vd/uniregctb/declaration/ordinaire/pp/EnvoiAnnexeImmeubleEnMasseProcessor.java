package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeleFeuille;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeDocument;

public class EnvoiAnnexeImmeubleEnMasseProcessor {

	final Logger LOGGER = LoggerFactory.getLogger(EnvoiAnnexeImmeubleEnMasseProcessor.class);

	private final TiersService tiersService;
	private final PeriodeFiscaleDAO periodeDAO;
	private final ModeleDocumentDAO modeleDAO;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final PeriodeImpositionService periodeImpositionService;
	private final AdresseService adresseService;

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

			final ModeleFeuilleDocument modeleAnnexeImmeuble = new ModeleFeuilleDocument();
			modeleAnnexeImmeuble.setModeleDocument(null);
			modeleAnnexeImmeuble.setIntituleFeuille(ModeleFeuille.ANNEXE_320.getDescription());
			modeleAnnexeImmeuble.setNoCADEV(ModeleFeuille.ANNEXE_320.getNoCADEV());
			modeleAnnexeImmeuble.setNoFormulaireACI(ModeleFeuille.ANNEXE_320.getNoFormulaireACI());
			modeleAnnexeImmeuble.setPrincipal(ModeleFeuille.ANNEXE_320.isPrincipal());

			setAnnexeImmeuble = new HashSet<>();
			setAnnexeImmeuble.add(modeleAnnexeImmeuble);
		}
	}

	private Cache cache;

	public EnvoiAnnexeImmeubleEnMasseProcessor(TiersService tiersService, ModeleDocumentDAO modeleDAO,
	                                           PeriodeFiscaleDAO periodeDAO, DeclarationImpotService diService, int tailleLot,
	                                           PlatformTransactionManager transactionManager,
	                                           ServiceCivilCacheWarmer serviceCivilCacheWarmer, PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		this.tiersService = tiersService;
		this.modeleDAO = modeleDAO;
		this.periodeDAO = periodeDAO;
		this.tailleLot = tailleLot;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
		Assert.isTrue(tailleLot > 0);
	}

	public EnvoiAnnexeImmeubleResults run(final int anneePeriode, final List<ContribuableAvecImmeuble> listCtbImmo, final int nbMax,
	                                      final RegDate dateTraitement, StatusManager s) throws DeclarationException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final EnvoiAnnexeImmeubleResults rapportFinal = new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbMax, tiersService, adresseService);

		// Traite les contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<ContribuableAvecImmeuble, EnvoiAnnexeImmeubleResults> template =
				new BatchTransactionTemplateWithResults<>(listCtbImmo, tailleLot, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<ContribuableAvecImmeuble, EnvoiAnnexeImmeubleResults>() {

			@Override
			public EnvoiAnnexeImmeubleResults createSubRapport() {
				return new EnvoiAnnexeImmeubleResults(anneePeriode, dateTraitement, "", nbMax, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<ContribuableAvecImmeuble> batch, EnvoiAnnexeImmeubleResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0).getNumeroContribuable() + "; " + batch.get(batch.size() - 1).getNumeroContribuable() + "] ...", progressMonitor.getProgressInPercent());

				if (nbMax > 0 && rapportFinal.nbCtbsTotal + batch.size() >= nbMax) {
					// limite le nombre de contribuable pour ne pas dépasser le nombre max
					int reducedSize = nbMax - rapportFinal.nbCtbsTotal;
					batch = batch.subList(0, reducedSize);
				}

				if (!batch.isEmpty()) {
					traiterBatch(batch, anneePeriode, r);
				}

				return !rapportFinal.interrompu && (nbMax <= 0 || rapportFinal.nbCtbsTotal + batch.size() < nbMax);
			}
		}, progressMonitor);

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
	 * Traite tout le batch des contribuables, un par un.
	 *
	 * @param listCtbImmeuble les listCtbImmeuble des contribuables à traiter
	 * @param anneePeriode    l'année fiscale considérée
	 * @param r
	 * @throws ch.vd.uniregctb.declaration.DeclarationException
	 *          en cas d'erreur dans le traitement d'un contribuable.
	 */
	protected void traiterBatch(List<ContribuableAvecImmeuble> listCtbImmeuble, int anneePeriode, EnvoiAnnexeImmeubleResults r) throws DeclarationException {
		// pré-chauffage du cache des individus du civil
		final List<Long> idsCtb = getIdCtb(listCtbImmeuble);
		if (serviceCivilCacheWarmer != null) {
			serviceCivilCacheWarmer.warmIndividusPourTiers(idsCtb, null, true, AttributeIndividu.ADRESSES);
		}
		r.nbCtbsTotal += listCtbImmeuble.size();

		initCache(anneePeriode);

		for (ContribuableAvecImmeuble ctbImmeuble : listCtbImmeuble) {
			final Tiers tiers = tiersService.getTiers(ctbImmeuble.getNumeroContribuable());
			if (tiers == null) {
				r.addErreurNoContribuableInvalide(ctbImmeuble, "Contribuable inexistant.");
				continue;
			}
			else if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques)) {
				r.addErreurNoContribuableInvalide(ctbImmeuble, "Tiers non soumis au régime des personnes physiques.");
				continue;
			}

			final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) tiers;
			final PeriodeImpositionPersonnesPhysiques pi = getPeriodeImpositionEnFinDePeriodeFiscale(ctb, anneePeriode, r);
			if (pi == null) {
				r.addIgnoreCtbNonAssujetti(ctb, anneePeriode);
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


				final int nombreAnnexesImprimees = imprimerAnnexeImmeuble(infoFormulaireImmeuble, cache.setAnnexeImmeuble, nombreAnnexesImmeuble);
				r.addInfoCtbTraites(ctb, nombreAnnexesImprimees);
				r.addCtbTraites(ctb.getId());
			}
		}
	}

	protected static int getNoSequenceAnnexeImmeuble(ContribuableImpositionPersonnesPhysiques ctb, DateRange pi) {
		final List<DeclarationImpotOrdinairePP> decls = ctb.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, pi.getDateFin().year(), true);
		final int noSequence;
		if (decls.isEmpty()) {
			noSequence = 1;
		}
		else {
			final DeclarationImpotOrdinairePP declFinPeriode = ctb.getDeclarationActiveAt(pi.getDateFin());
			if (declFinPeriode != null) {
				// il y a déjà une DI valide à la fin de la période considérée... on reprend donc le même numéro!
				noSequence = declFinPeriode.getNumero();
			}
			else {
				// attribution d'un nouveau numéro -> la prochaine fois que l'on générera une DI, c'est ce numéro
				// qui sera utilisé... On espère juste que c'est bien cette DI qui sera finalement envoyée au contribuable...
				noSequence = decls.size() + 1;
			}
		}
		return noSequence;
	}

	private int imprimerAnnexeImmeuble(InformationsDocumentAdapter infosDocuments, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws DeclarationException {
		return diService.envoiAnnexeImmeubleForBatch(infosDocuments, listeModele, nombreAnnexesImmeuble);
	}

	/**
	 * On ne prend que les contribuables qui ont une période d'imposition qui va jusqu'à la fin de l'année (pour éliminer les sourciers purs, pour lesquels on ne trouvera pas d'OID de gestion)
	 *
	 *
	 * @param ctb     contribuable à tester
	 * @param periode année fiscale
	 * @param r
	 * @return la période d'imposition en fin de période fiscale s'il y en a une, <code>null</code> dans le cas contraire
	 */
	protected PeriodeImpositionPersonnesPhysiques getPeriodeImpositionEnFinDePeriodeFiscale(ContribuableImpositionPersonnesPhysiques ctb, int periode, EnvoiAnnexeImmeubleResults r) {
		List<PeriodeImposition> list = null;
		try {
			list = periodeImpositionService.determine(ctb, periode);
		}
		catch (AssujettissementException e) {
			r.addErrorException(ctb, e);
		}

		final RegDate date = RegDate.get(periode, 12, 31);
		if (list != null) {
			final PeriodeImposition periodeImposition = DateRangeHelper.rangeAt(list, date);
			if (periodeImposition != null) {
				return (PeriodeImpositionPersonnesPhysiques) periodeImposition;
			}
		}

		return null;
	}

	/**
	 * Determine le nombre d'annexe à envoyer selon le nombre d'immeuble
	 *
	 * @param nombreImmeuble nombre d'immeubles pour le contribuable
	 * @return le nombre d'immeubles divisé par 2 arondi à l'entier supérieur (car on peut mettre deux immeubles par annexe)
	 */
	protected int getNombreAnnexeAEnvoyer(int nombreImmeuble) {
		// [SIFISC-2485] les annexes sont à envoyer à double.
		// [SIFISC-25564] les annexes ne doivent maintenant à nouveau plus être envoyées qu'en un seul exemplaire
		return (int) Math.ceil(nombreImmeuble / 2.0);
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
		CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		if (cedi == null) {
			throw new DeclarationException("Impossible de charger le centre d'enregistrement des déclarations d'impôt (CEDI).");
		}

		final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noACI);
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
		final List<Long> idsCtb = new ArrayList<>(listeCtbImmeuble.size());
		for (ContribuableAvecImmeuble ctbImmeuble : listeCtbImmeuble) {
			idsCtb.add(ctbImmeuble.getNumeroContribuable());
		}
		return idsCtb;
	}


}
