package ch.vd.uniregctb.role;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.DecompositionFors;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixte;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoCommune;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoFor;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Processeur spécialisé dans la production des rôles pour les communes vaudoises. Ce processeur doit être appelé par le service 'rôle'
 * uniquement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ProduireRolesCommuneProcessor {

	final Logger LOGGER = Logger.getLogger(ProduireRolesCommuneProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;

	private final ServiceInfrastructureService infraService;

	private final TiersDAO tiersDAO;

	private final PlatformTransactionManager transactionManager;
	
	private final AdresseService adresseService;
	
	private final TiersService tiersService;

	private final ServiceCivilService serviceCivilService;

	public ProduireRolesCommuneProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService, TiersDAO tiersDAO, PlatformTransactionManager transactionManager,
	                                     AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivilService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 * Interface implémentée par les variantes de la production des rôles (tous, pour un OID, pour une commune)
	 */
	private static interface VarianteProductionRole {
		/**
		 * Renvoie la liste des ID techniques des contribuables listés dans la variante concernée
		 */
		List<Long> getIdsContribuablesConcernes(int anneePeriode);

		/**
		 * Instancie un nouveau rapport (intermédiaire et final)
		 */
		ProduireRolesResults creerRapport(int anneePeriode, RegDate today, boolean isRapportFinal);
	}

	/**
	 * Factorisation du code de processing indépendant de la variante
	 * @param anneePeriode
	 * @param status
	 * @param variante
	 * @return
	 */
	private ProduireRolesResults doRun(final int anneePeriode, final StatusManager status, final VarianteProductionRole variante) {

		final RegDate today = RegDate.get();
		final ProduireRolesResults rapportFinal = variante.creerRapport(anneePeriode, today, true);

		// parties à aller chercher en bloc par groupe de tiers
		final Set<TiersDAO.Parts> parts = new HashSet<TiersDAO.Parts>();
		parts.add(TiersDAO.Parts.FORS_FISCAUX);
		parts.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
		parts.add(TiersDAO.Parts.ADRESSES);

		final List<Long> list = variante.getIdsContribuablesConcernes(anneePeriode);
		final BatchTransactionTemplate<Long, ProduireRolesResults> template = new BatchTransactionTemplate<Long, ProduireRolesResults>(list, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, ProduireRolesResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, ProduireRolesResults rapport) throws Exception {

				final List<Tiers> tierz = tiersDAO.getBatch(batch, parts);

				// première boucle sur les tiers pour aller chercher en un bloc les individus du civil
				// pour les habitants (nom, adresse, no-avs...)
				preloadIndividus(tierz, anneePeriode);

				// deuxième boucle pour le traitement proprement dit
				for (Tiers tiers : tierz) {

					final long ctbId = tiers.getNumero();
					final Contribuable ctb = (Contribuable) tiers;

					final String msg = String.format("Traitement du contribuable %s", FormatNumeroHelper.numeroCTBToDisplay(ctbId));
					status.setMessage(msg, rapportFinal.ctbsTraites * 100 / list.size());

					try {
						processContribuable(anneePeriode, rapport, ctb);
					}
					catch (Exception e) {
						final String msgException = String.format("Exception levée lors du traitement du contribuable %s", FormatNumeroHelper.numeroCTBToDisplay(ctbId));
						LOGGER.error(msgException, e);
						rapport.addErrorException(ctb, e);
					}

					if (status.interrupted()) {
						break;
					}
				}
				return !status.interrupted();
			}

			@Override
			public ProduireRolesResults createSubRapport() {
				return variante.creerRapport(anneePeriode, today, false);
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();

		return rapportFinal;
	}

	private void preloadIndividus(List<Tiers> tierz, int anneePeriode) {
		final Set<Long> nosIndividus = new HashSet<Long>(tierz.size() * 2);
		for (Tiers tiers : tierz) {
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.isHabitant()) {
					nosIndividus.add(pp.getNumeroIndividu());
				}
			}
			else if (tiers instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, anneePeriode);
				final PersonnePhysique principal = ensemble.getPrincipal();
				final PersonnePhysique conjoint = ensemble.getConjoint();
				if (principal != null && principal.isHabitant()) {
					nosIndividus.add(principal.getNumeroIndividu());
				}
				if (conjoint != null && conjoint.isHabitant()) {
					nosIndividus.add(conjoint.getNumeroIndividu());
				}
			}
		}
		if (nosIndividus.size() > 0) {
			// remplit le cache des individus...
			serviceCivilService.getIndividus(nosIndividus, RegDate.get(anneePeriode, 12, 31), EnumAttributeIndividu.ADRESSES);
		}
	}

	/**
	 * Produit la liste de contribuables de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	public ProduireRolesResults runPourToutesCommunes(final int anneePeriode, final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		return doRun(anneePeriode, status, new VarianteProductionRole() {
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuables(anneePeriode);
			}

			public ProduireRolesResults creerRapport(int anneePeriode, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesResults(anneePeriode, today);
			}
		});
	}

	/**
	 * Produit la liste de contribuables d'une commune (ou fraction de commune) vaudoise pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que la commune spécifiée, en fonction des déménagement des
	 * contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param noOfsCommune
	 *            le numéro Ofs étendu de la commune à traiter
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	public ProduireRolesResults runPourUneCommune(final int anneePeriode, final int noOfsCommune, final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		return doRun(anneePeriode, status, new VarianteProductionRole() {
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesSurCommunes(anneePeriode, Arrays.asList(noOfsCommune));
			}

			public ProduireRolesResults creerRapport(int anneePeriode, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesResults(anneePeriode, noOfsCommune, null, today);
			}
		});
	}

	/**
	 * Produit la liste de contribuables d'un office d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param oid
	 *            l'id de l'office d'impôt concerné
	 * @param dateTraitement
	 *            la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	public ProduireRolesResults runPourUnOfficeImpot(final int anneePeriode, final int oid, final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// récupère les numéros Ofs des communes gérées par l'office d'impôt spécifié
		final List<Integer> nosOfsCommunes = new ArrayList<Integer>();
		try {
			final List<Commune> communes = infraService.getListeCommunesByOID(oid);
			for (Commune c : communes) {
				nosOfsCommunes.add(c.getNoOFSEtendu());
			}
		}
		catch (InfrastructureException e) {
			throw new RuntimeException(e);
		}

		return doRun(anneePeriode, status, new VarianteProductionRole() {
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesSurCommunes(anneePeriode, nosOfsCommunes);
			}

			public ProduireRolesResults creerRapport(int anneePeriode, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesResults(anneePeriode, null, oid, today);
			}
		});
	}

	/**
	 * Traite le contribuable spécifié et ajoute toutes les informations nécessaires au rapport
	 */
	private void processContribuable(int anneePeriode, ProduireRolesResults rapport, Contribuable ctb) {

		rapport.ctbsTraites++;

		if (ctb.validate().hasErrors()) {
			rapport.addErrorCtbInvalide(ctb);
		}
		else {

			// ajoute les assujettissements du contribuable de l'année spécifiée dans le rapprot
			try {
				processAssujettissements(anneePeriode, ctb, rapport);

				// ajoute les deltas d'assujettissements entre l'année précédente et courante
				final ProduireRolesResults rapportAnneePrecedente = new ProduireRolesResults(anneePeriode - 1, RegDate.get());
				processAssujettissements(anneePeriode - 1, ctb, rapportAnneePrecedente);
				processDeltaAssujettissements(anneePeriode, ctb, rapportAnneePrecedente, rapport);
			}
			catch (TraitementException e) {
				// ok, rien à faire, l'erreur a déjà été renseignée dans le rapport
			}
		}
	}

	protected static class TraitementException extends Exception {

		private static final long serialVersionUID = -7831475785605819136L;

		public TraitementException() {
		}
	}

	/**
	 * Ajoute les détails du ou des assujettissements du contribuable spécifié durant la période fiscale spécifiée au rapport.
	 *
	 * @param anneePeriode
	 *            la période fiscale dont on veut déterminer l'assujettissement
	 * @param contribuable
	 *            le contribuable en question
	 * @param rapport
	 *            le rapport à compléter
	 * @throws TraitementException
	 *             en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processAssujettissements(int anneePeriode, Contribuable contribuable, ProduireRolesResults rapport) throws TraitementException {

		final DecompositionForsAnneeComplete fors = new DecompositionForsAnneeComplete(contribuable, anneePeriode);

		// détermine les assujettissements du contribuable
		try {
			final List<Assujettissement> assujettissements = Assujettissement.determine(fors);

			// traite les assujettissements
			if (assujettissements != null) {
				for (Assujettissement a : assujettissements) {
					processAssujettissement(a, contribuable, rapport, anneePeriode);
				}
			}
			else if (!fors.isFullyEmpty()) {
				processNonAssujettissement(fors, rapport, anneePeriode);
			}
		}
		catch (AssujettissementException e) {
			rapport.addErrorErreurAssujettissement(contribuable, e.getMessage());
			throw new TraitementException();
		}
	}

	/**
	 * Ajoute les détails de l'assujettissements du contribuable spécifié au rapport.
	 *
	 * @param assujet
	 *            l'assujettissement à traiter
	 * @param contribuable
	 *            le contribuable en question
	 * @param rapport
	 *            le rapport à compléter
	 * @param annee
	 * @throws TraitementException
	 *             en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processAssujettissement(Assujettissement assujet, Contribuable contribuable, ProduireRolesResults rapport, int annee) throws TraitementException {

		final TypeContribuable typeCtb = getTypeContribuable(assujet);
		if (typeCtb == null) {
			rapport.addCtbIgnoreDiplomateSuisse(contribuable);
			return;
		}

		final DecompositionFors fors = assujet.getFors();

		/*
		 * On traite tous les fors actifs durant la période d'assujettissement (à l'exception des fors principaux hors canton, évidemment)
		 * et pas seulement les fors déterminants, car toute les communes ayant un for ouvert doivent recevoir l'information
		 */
		for (ForFiscalRevenuFortune f : fors.principauxDansLaPeriode) {
			if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(f.getTypeAutoriteFiscale())) {
				processForFiscal(assujet, f, contribuable, typeCtb, rapport, annee);
			}
		}
		for (ForFiscalRevenuFortune f : fors.secondairesDansLaPeriode) {
			processForFiscal(assujet, f, contribuable, typeCtb, rapport, annee);
		}
	}

	/**
	 * Ajoute les détails du non-assujettissement au rapport (= cas du contribuable ayant au moins un for dans le canton mais pas du tout
	 * assujetti dans l'année)
	 *
	 * @param fors
	 *            la décomposition des fors du contribuable pour l'année voulue
	 * @param rapport
	 * @param annee
	 */
	private void processNonAssujettissement(DecompositionForsAnneeComplete fors, ProduireRolesResults rapport, int annee) throws TraitementException {

		/*
		 * Malgré le fait que le contribuable soit non-assujetti, il est nécessaire de traiter ses fors pour être en mesure de remplir
		 * correctement le rapport. On traite donc tous les fors actifs durant la période d'assujettissement (à l'exception des fors
		 * principaux hors canton, évidemment).
		 */
		for (ForFiscalRevenuFortune f : fors.principauxDansLaPeriode) {
			if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(f.getTypeAutoriteFiscale())) {
				processForFiscal(null, f, fors.contribuable, TypeContribuable.NON_ASSUJETTI, rapport, annee);
			}
		}
		for (ForFiscalRevenuFortune f : fors.secondairesDansLaPeriode) {
			processForFiscal(null, f, fors.contribuable, TypeContribuable.NON_ASSUJETTI, rapport, annee);
		}
	}

	/**
	 * Ajoute au rapport les détails de l'assujettissement pour le for fiscal spécifié.
	 *
	 * @param assujettissement
	 * @param forFiscal le for fiscal déterminant de l'assujettissement
	 * @param contribuable le contribuable en question
	 * @param typeCtb le type de contribuable, ou <b>null</b> si le contribuable n'est pas assujetti
	 * @param rapport le rapport à compléter
	 * @param annee
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processForFiscal(Assujettissement assujettissement, ForFiscalRevenuFortune forFiscal, Contribuable contribuable, TypeContribuable typeCtb, ProduireRolesResults rapport, int annee) throws TraitementException {

		if (!TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.equals(forFiscal.getTypeAutoriteFiscale())) {
			final String details = "L'autorité fiscale du for [" + forFiscal + "] n'est pas dans le canton.";
			rapport.addCtbIgnoreDonneesIncoherentes(contribuable, details);
			throw new TraitementException();
		}

		final RegDate dateFermeture;
		final MotifFor motifFermeture;
		if (forFiscal.getDateFin() != null && forFiscal.getDateFin().year() <= annee) {
			dateFermeture = forFiscal.getDateFin();
			motifFermeture = forFiscal.getMotifFermeture();
		}
		else {
			dateFermeture = null;
			motifFermeture = null;
		}

		final MotifFor motifOuverture = forFiscal.getMotifOuverture();
		final RegDate dateOuverture = forFiscal.getDateDebut();

		// Ajoute l'information
		final Integer ofsCommune = forFiscal.getNumeroOfsAutoriteFiscale();
		final boolean communeActive = assujettissement != null && assujettissement.isActifSurCommune(ofsCommune);
		final TypeContribuable typeCtbPourCommune = communeActive ? typeCtb : TypeContribuable.NON_ASSUJETTI;
		final InfoCommune infoCommune = rapport.getOrCreateInfoPourCommune(ofsCommune);
		final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(contribuable, annee, adresseService, tiersService);
		final ProduireRolesResults.InfoFor infoFor = new ProduireRolesResults.InfoFor(typeCtbPourCommune, dateOuverture, dateFermeture, motifOuverture, motifFermeture, communeActive);
		infoCtb.addFor(infoFor);
	}

	/**
	 * Ajoute une ligne dans le rapport au niveau de chaque commune concernée si le contribuable spécifié était assujetti l'année précédente
	 * mais ne l'est plus cette année.
	 *
	 * @param anneePeriode
	 * @param contribuable
	 *            le contribuable concerné
	 * @param rapportAnneePrecedente
     *            le rapport d'assujettissement de l'année précédente (valable
	 * @param rapport
	 *            le rapport auquel on veut ajouter les deltas d'assujettissements
	 */
	private void processDeltaAssujettissements(int anneePeriode, Contribuable contribuable, ProduireRolesResults rapportAnneePrecedente, ProduireRolesResults rapport) {

		final long noCtb = contribuable.getNumero();

		for (Integer noOfsCommune : rapportAnneePrecedente.infosCommunes.keySet()) {

			final InfoCommune infoCommuneAnneePrecedente = rapportAnneePrecedente.getInfoPourCommune(noOfsCommune);
			Assert.notNull(infoCommuneAnneePrecedente);

			final InfoContribuable infoCtbAnneePrecedente = infoCommuneAnneePrecedente.getInfoPourContribuable(noCtb);
			if (infoCtbAnneePrecedente == null) {
				// le rapport de l'année précédente ne contient aucune donnée sur le contribuable spécifié: rien à faire
				continue;
			}

			InfoCommune infoCommune = rapport.getInfoPourCommune(noOfsCommune);
			if (infoCommune != null && infoCommune.getInfoPourContribuable(noCtb) != null) {
				// le rapport de l'année courante contient déjà des données sur le contribuable spécifié: rien à faire
				continue;
			}

			/*
			 * le contribuable existait dans la commune l'année précédente et n'est plus assujetti: on l'ajoute comme tel
			 */
			Assert.notNull(infoCtbAnneePrecedente);
			if (infoCommune == null) {
				infoCommune = rapport.getOrCreateInfoPourCommune(noOfsCommune);
			}

			final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(contribuable, anneePeriode, adresseService, tiersService);

			// dates nulles -> contribuable plus assujetti
			final InfoFor infoFor = new InfoFor(TypeContribuable.NON_ASSUJETTI, null, null, null, null, false);
			infoCtb.addFor(infoFor);
		}
	}

	/**
	 * @return calcul le type de contribuable (au sens 'rôle pour les communes') à partir de l'assujettissement spécifié.
	 */
	protected static TypeContribuable getTypeContribuable(Assujettissement a) {
		final TypeContribuable typeCtb;

		if (a instanceof VaudoisOrdinaire || a instanceof Indigent) {
			typeCtb = TypeContribuable.ORDINAIRE;
		}
		else if (a instanceof VaudoisDepense) {
			typeCtb = TypeContribuable.DEPENSE;
		}
		else if (a instanceof SourcierMixte) {
			typeCtb = TypeContribuable.MIXTE;
		}
		else if (a instanceof HorsCanton) {
			typeCtb = TypeContribuable.HORS_CANTON;
		}
		else if (a instanceof HorsSuisse) {
			typeCtb = TypeContribuable.HORS_SUISSE;
		}
		else if (a instanceof SourcierPur) {
			typeCtb = TypeContribuable.SOURCE;
		}
		else {
			Assert.isTrue(a instanceof DiplomateSuisse);
			typeCtb = null; // les diplomates suisse non soumis à ICC sont ignorés
		}
		return typeCtb;
	}

	/**
	 * @return la liste des ID de tous les contribuables ayant au moins un for fiscal actif dans une commune vaudoise durant l'année spécifiée
	 *         <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement).
	 */
	@SuppressWarnings({"unchecked"})
	protected List<Long> getIdsOfAllContribuables(final int annee) {

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT cont.id FROM Contribuable AS cont INNER JOIN cont.forsFiscaux AS for");
		b.append(" WHERE cont.annulationDate IS NULL");
		b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND (for.dateDebut IS NULL OR for.dateDebut <= :finPeriode)");
		b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
		b.append(" ORDER BY cont.id ASC");
		final String hql = b.toString();

		return (List<Long>) hibernateTemplate.executeWithNativeSession(new HibernateCallback() {
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {

				final RegDate debutPeriode = RegDate.get(annee - 1, 1, 1);
				final RegDate finPeriode = RegDate.get(annee, 12, 31);

				final Query query = session.createQuery(hql);
				query.setParameter("debutPeriode", debutPeriode.index());
				query.setParameter("finPeriode", finPeriode.index());
				return query.list();
			}
		});
	}

	/**
	 * @return la liste des ID de tous les contribuables ayant au moins un for fiscal actif dans la commune vaudoise spécifiée durant l'année
	 *         spécifiée <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement).
	 */
	@SuppressWarnings({"unchecked"})
	protected List<Long> getIdsOfAllContribuablesSurCommunes(final int annee, final List<Integer> noOfsCommunes) {
		Assert.isTrue(noOfsCommunes != null && noOfsCommunes.size() > 0);

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT cont.id FROM Contribuable AS cont INNER JOIN cont.forsFiscaux AS for");
		b.append(" WHERE cont.annulationDate IS NULL");
		b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND for.numeroOfsAutoriteFiscale IN (:noOfsCommune)");
		b.append(" AND (for.dateDebut IS NULL OR for.dateDebut <= :finPeriode)");
		b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
		b.append(" ORDER BY cont.id ASC");
		final String hql = b.toString();

		return (List<Long>) hibernateTemplate.executeWithNativeSession(new HibernateCallback() {
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {

				final RegDate debutPeriode = RegDate.get(annee - 1, 1, 1);
				final RegDate finPeriode = RegDate.get(annee, 12, 31);

				final Query query = session.createQuery(hql);
				query.setParameter("debutPeriode", debutPeriode.index());
				query.setParameter("finPeriode", finPeriode.index());
				query.setParameterList("noOfsCommune", noOfsCommunes);
				return query.list();
			}
		});
	}
}
