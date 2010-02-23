package ch.vd.uniregctb.role;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import ch.vd.registre.base.date.RegDate;
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
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
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

		++ rapport.ctbsTraites;

		if (tiersService.isSourcierGris(ctb, RegDate.get(anneePeriode, 12, 31))) {
			rapport.addCtbIgnoreSourcierGris(ctb);
		}
		else if (ctb.validate().hasErrors()) {
			rapport.addErrorCtbInvalide(ctb);
		}
		else {

			// ajoute les assujettissements du contribuable de l'année spécifiée dans le rapprot
			try {
				processAssujettissements(anneePeriode, ctb, rapport);
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
	 * @param anneePeriode la période fiscale dont on veut déterminer l'assujettissement
	 * @param contribuable le contribuable en question
	 * @param rapport le rapport à compléter
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processAssujettissements(int anneePeriode, Contribuable contribuable, ProduireRolesResults rapport) throws TraitementException {

		final AssujettissementContainer assujettissementContainer = new AssujettissementContainer(contribuable, anneePeriode, rapport);

		// traite les assujettissements
		final List<Assujettissement> assujettissements = assujettissementContainer.getAssujettissementAnneePeriode();
		if (assujettissements != null) {
			for (Assujettissement a : assujettissements) {
				processAssujettissement(a, assujettissementContainer);
			}
		}
		else {

			final DecompositionForsAnneeComplete fors = assujettissementContainer.getForsAnneePeriode();
			if (!fors.isFullyEmpty()) {
				// pas d'assujettissement, mais des fors quand-même... A priori ils se terminent dans la période...
				processNonAssujettissement(fors, assujettissementContainer);
			}
		}
	}

	/**
	 * Ajoute les détails de l'assujettissements du contribuable spécifié au rapport.
	 *
	 * @param assujet l'assujettissement à traiter
	 * @param assujettissementContainer cache pour les assujettissements calculés sur la période courante et la précédente
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processAssujettissement(Assujettissement assujet, AssujettissementContainer assujettissementContainer) throws TraitementException {

		final Contribuable contribuable = assujettissementContainer.ctb;
		final ProduireRolesResults rapport = assujettissementContainer.rapport;
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
				processForFiscal(assujet, f, typeCtb, assujettissementContainer);
			}
		}
		for (ForFiscalRevenuFortune f : fors.secondairesDansLaPeriode) {
			processForFiscal(assujet, f, typeCtb, assujettissementContainer);
		}
	}

	/**
	 * Ajoute les détails du non-assujettissement au rapport (= cas du contribuable ayant au moins un for dans le canton mais pas du tout
	 * assujetti dans l'année)
	 *
	 * @param fors la décomposition des fors du contribuable pour l'année de la période courante
	 * @param assujettissementContainer cache pour les assujettissements calculés sur la période courante et la précédente
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processNonAssujettissement(DecompositionForsAnneeComplete fors, AssujettissementContainer assujettissementContainer) throws TraitementException {

		// calcul de l'assujettissement dans la période précédente
		final List<Assujettissement> assujettissementsAnneePrecedente = assujettissementContainer.getAssujettissementPeriodePrecedente();

		// prenons ensuite les communes des fors de la décomposition
		final Set<Integer> ofsCommunes = new HashSet<Integer>(fors.principauxDansLaPeriode.size() + fors.secondairesDansLaPeriode.size());
		for (ForFiscalRevenuFortune ff : fors.principauxDansLaPeriode) {
			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				ofsCommunes.add(ff.getNumeroOfsAutoriteFiscale());
			}
		}
		for (ForFiscalRevenuFortune ff : fors.secondairesDansLaPeriode) {
			ofsCommunes.add(ff.getNumeroOfsAutoriteFiscale());
		}

		// sur chacune de ces communes, il faut ensuite indiquer que le contribuable n'est pas assujetti
		for (Integer noOfsCommune : ofsCommunes) {
			traiteNonAssujettiAvecPrecedentAssujettissement(assujettissementContainer, noOfsCommune);
		}
	}

	private static final class AssujettissementContainer {

		private final int anneePeriode;
		private final Contribuable ctb;
		private final ProduireRolesResults rapport;

		private final DecompositionForsAnneeComplete forsAnneePeriode;
		private final List<Assujettissement> assujettissementAnneePeriode;
		private List<Assujettissement> assujettissementAnneePrecedente;

		private AssujettissementContainer(Contribuable ctb, int anneePeriode, ProduireRolesResults rapport) throws TraitementException {
			this.anneePeriode = anneePeriode;
			this.ctb = ctb;
			this.rapport = rapport;
			this.forsAnneePeriode = new DecompositionForsAnneeComplete(ctb, anneePeriode);
			this.assujettissementAnneePeriode = determineAssujettissement(forsAnneePeriode, ctb, rapport);
		}

		public List<Assujettissement> getAssujettissementPeriodePrecedente() throws TraitementException {
			if (assujettissementAnneePrecedente == null) {
				final DecompositionForsAnneeComplete fors = new DecompositionForsAnneeComplete(ctb, anneePeriode - 1);
				assujettissementAnneePrecedente = determineAssujettissement(fors, ctb, rapport);
				if (assujettissementAnneePrecedente == null) {
					// histoire qu'on ne le fasse pas plusieurs fois...
					assujettissementAnneePrecedente = Collections.emptyList();
				}
			}
			return assujettissementAnneePrecedente.size() == 0 ? null : assujettissementAnneePrecedente;
		}

		public List<Assujettissement> getAssujettissementAnneePeriode() {
			return assujettissementAnneePeriode;
		}

		public DecompositionForsAnneeComplete getForsAnneePeriode() {
			return forsAnneePeriode;
		}

		private static List<Assujettissement> determineAssujettissement(DecompositionForsAnneeComplete fors, Contribuable ctb, ProduireRolesResults rapport) throws TraitementException {
			try {
				return Assujettissement.determine(fors);
			}
			catch (AssujettissementException e) {
				rapport.addErrorErreurAssujettissement(ctb, e.getMessage());
				throw new TraitementException();
			}
		}
	}

	/**
	 * Ajoute au rapport les détails de l'assujettissement pour le for fiscal spécifié.
	 *
	 * @param assujettissement
	 * @param forFiscal le for fiscal déterminant de l'assujettissement
	 * @param typeCtb le type de contribuable, ou <b>null</b> si le contribuable n'est pas assujetti
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processForFiscal(Assujettissement assujettissement, ForFiscalRevenuFortune forFiscal, TypeContribuable typeCtb, AssujettissementContainer assujettissementContainer) throws TraitementException {

		final ProduireRolesResults rapport = assujettissementContainer.rapport;
		final Contribuable contribuable = assujettissementContainer.ctb;
		final int anneePeriode = assujettissementContainer.anneePeriode;

		if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD != forFiscal.getTypeAutoriteFiscale()) {
			final String details = String.format("L'autorité fiscale du for [%s] n'est pas dans le canton.", forFiscal);
			rapport.addCtbIgnoreDonneesIncoherentes(contribuable, details);
			throw new TraitementException();
		}

		final Integer ofsCommune = forFiscal.getNumeroOfsAutoriteFiscale();
		final TypeAssujettissement typeAssujettissement = getTypeAssujettissementPourCommune(ofsCommune, assujettissement);
		if (typeAssujettissement == TypeAssujettissement.NON_ASSUJETTI) {

			// pas d'assujettissement sur cette commune dans l'annee de période
			// -> s'il y en avait un avant, il faut l'indiquer dans le rapport
			// (sauf si le contribuable était alors sourcier gris, auquel cas il ne doit pas apparaître)
			if (!tiersService.isSourcierGris(contribuable, RegDate.get(anneePeriode - 1, 12, 31))) {
				traiteNonAssujettiAvecPrecedentAssujettissement(assujettissementContainer, ofsCommune);
			}
		}
		else {
			final DebutFinFor debutFin = getInformationDebutFin(forFiscal, anneePeriode);
			final InfoCommune infoCommune = rapport.getOrCreateInfoPourCommune(ofsCommune);
			final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(contribuable, anneePeriode, adresseService, tiersService);
			final InfoFor infoFor = new InfoFor(typeCtb, debutFin.dateOuverture, debutFin.motifOuverture, debutFin.dateFermeture, debutFin.motifFermeture, typeAssujettissement, forFiscal.isPrincipal(), forFiscal.getMotifRattachement());
			infoCtb.addFor(infoFor);
		}
	}

	private void traiteNonAssujettiAvecPrecedentAssujettissement(AssujettissementContainer assujettissementContainer, Integer ofsCommune) throws TraitementException {

		final List<Assujettissement> assujettissementsAnneePrecedente = assujettissementContainer.getAssujettissementPeriodePrecedente();
		if (assujettissementsAnneePrecedente != null) {

			final ProduireRolesResults rapport = assujettissementContainer.rapport;
			final Contribuable contribuable = assujettissementContainer.ctb;
			final int anneePeriode = assujettissementContainer.anneePeriode;

			for (Assujettissement assAnneePrecedente : assujettissementsAnneePrecedente) {
				final TypeContribuable typeCtbAnneePrecedente = getTypeContribuable(assAnneePrecedente);
				final DecompositionFors forsAnneePrecedente = assAnneePrecedente.getFors();
				for (ForFiscalRevenuFortune ff : forsAnneePrecedente.principauxDansLaPeriode) {
					if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ff.getNumeroOfsAutoriteFiscale() == ofsCommune.intValue()) {
						traiteNonAssujettiAvecPrecedentAssujettissement(rapport, contribuable, anneePeriode, ofsCommune, typeCtbAnneePrecedente, ff);
					}
				}
				for (ForFiscalRevenuFortune ff : forsAnneePrecedente.secondairesDansLaPeriode) {
					if (ff.getNumeroOfsAutoriteFiscale() == ofsCommune.intValue()) {
						traiteNonAssujettiAvecPrecedentAssujettissement(rapport, contribuable, anneePeriode, ofsCommune, typeCtbAnneePrecedente, ff);
					}
				}
			}
		}
	}

	/**
	 * Traitement, pour une commune particulière, d'un contribuable qui n'est pas assujetti sur la période courante mais qui l'était
	 * sur la période précédente
	 * @param rapport rapport à compléter
	 * @param contribuable contribuable en cours de traitement
	 * @param anneePeriode année de la période courante
	 * @param ofsCommune numéro OFS étendu de la commune considérée
	 * @param typeCtbAnneePrecedente type de contribuable sur la commune à l'époque de la période précédente
	 * @param ff for fiscal sur la commune qui était ouvert dans la période précédente
	 *            le rapport auquel on veut ajouter les deltas d'assujettissements
	 */
	private void traiteNonAssujettiAvecPrecedentAssujettissement(ProduireRolesResults rapport, Contribuable contribuable, int anneePeriode, Integer ofsCommune,
	                                                             TypeContribuable typeCtbAnneePrecedente, ForFiscalRevenuFortune ff) {
		final DebutFinFor debutFin = getInformationDebutFin(ff, anneePeriode);
		final InfoCommune infoCommune = rapport.getOrCreateInfoPourCommune(ofsCommune);
		final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(contribuable, anneePeriode, adresseService, tiersService);
		final InfoFor infoFor = new InfoFor(debutFin.dateOuverture, debutFin.motifOuverture, debutFin.dateFermeture, debutFin.motifFermeture, typeCtbAnneePrecedente, ff.isPrincipal(), ff.getMotifRattachement());
		infoCtb.addFor(infoFor);
	}

	private static class DebutFinFor {
		public final RegDate dateOuverture;
		public final MotifFor motifOuverture;
		public final RegDate dateFermeture;
		public final MotifFor motifFermeture;

		private DebutFinFor(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {
			this.dateOuverture = dateOuverture;
			this.motifOuverture = motifOuverture;
			this.dateFermeture = dateFermeture;
			this.motifFermeture = motifFermeture;
		}
	}

	private static DebutFinFor getInformationDebutFin(ForFiscalRevenuFortune forFiscal, int anneePeriode) {

		final ForFiscalRevenuFortune forFiscalPourOuverture;
		if (forFiscal.getDateDebut().getOneDayBefore().year() != forFiscal.getDateDebut().year()) {
			// debut au premier janvier...
			final Tiers tiers = forFiscal.getTiers();
			ForFiscalRevenuFortune candidatRetenu = forFiscal;
			while (true) {
				final List<ForFiscal> fors = tiers.getForsFiscauxValidAt(candidatRetenu.getDateDebut().getOneDayBefore());
				ForFiscalRevenuFortune nouveauCandidat = null;
				for (ForFiscal candidat : fors) {
					if (candidat instanceof ForFiscalRevenuFortune &&
							candidat.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
							candidat.getNumeroOfsAutoriteFiscale().equals(forFiscal.getNumeroOfsAutoriteFiscale()) &&
							((ForFiscalRevenuFortune) candidat).getMotifRattachement() == forFiscal.getMotifRattachement()) {
						if (nouveauCandidat == null || nouveauCandidat.getDateDebut().isAfter(candidat.getDateDebut())) {
							nouveauCandidat = (ForFiscalRevenuFortune) candidat;
						}
					}
				}
				if (nouveauCandidat == null) {
					break;
				}
				else {
					candidatRetenu = nouveauCandidat;
				}
			}
			forFiscalPourOuverture = candidatRetenu;
		}
		else {
			forFiscalPourOuverture = forFiscal;
		}

		final ForFiscalRevenuFortune forFiscalPourFermeture;
		if (forFiscal.getDateFin() != null && forFiscal.getDateFin().getOneDayAfter().year() != forFiscal.getDateFin().year()) {
			// fin au 31 décembre...
			final Tiers tiers = forFiscal.getTiers();
			ForFiscalRevenuFortune candidatRetenu = forFiscal;
			while (true) {
				final List<ForFiscal> fors = tiers.getForsFiscauxValidAt(candidatRetenu.getDateFin().getOneDayAfter());
				ForFiscalRevenuFortune nouveauCandidat = null;
				for (ForFiscal candidat : fors) {
					if (candidat instanceof ForFiscalRevenuFortune &&
							candidat.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
							candidat.getNumeroOfsAutoriteFiscale().equals(forFiscal.getNumeroOfsAutoriteFiscale()) &&
							((ForFiscalRevenuFortune) candidat).getMotifRattachement() == forFiscal.getMotifRattachement()) {
						if (nouveauCandidat == null || nouveauCandidat.getDateFin().isBefore(candidat.getDateFin())) {
							nouveauCandidat = (ForFiscalRevenuFortune) candidat;
						}
					}
				}
				if (nouveauCandidat == null) {
					break;
				}
				else {
					candidatRetenu = nouveauCandidat;
				}
			}
			forFiscalPourFermeture = candidatRetenu;
		}
		else {
			forFiscalPourFermeture = forFiscal;
		}

		final RegDate dateFermeture;
		final MotifFor motifFermeture;
		if (forFiscalPourFermeture.getDateFin() != null && forFiscalPourFermeture.getDateFin().year() <= anneePeriode) {
			dateFermeture = forFiscalPourFermeture.getDateFin();
			motifFermeture = forFiscalPourFermeture.getMotifFermeture();
		}
		else {
			dateFermeture = null;
			motifFermeture = null;
		}

		final MotifFor motifOuverture = forFiscalPourOuverture.getMotifOuverture();
		final RegDate dateOuverture = forFiscalPourOuverture.getDateDebut();
		return new DebutFinFor(dateOuverture, motifOuverture, dateFermeture, motifFermeture);
	}

	private static TypeAssujettissement getTypeAssujettissementPourCommune(int noOfsCommune, Assujettissement assujettissement) {
		final boolean communeActive = assujettissement != null && assujettissement.isActifSurCommune(noOfsCommune);
		final TypeAssujettissement typeAssujettissement;
		if (communeActive) {
			if (assujettissement.getMotifFractFin() != null) {
				typeAssujettissement = TypeAssujettissement.TERMINE_DANS_PF;
			}
			else {
				typeAssujettissement = TypeAssujettissement.POURSUIVI_APRES_PF;
			}
		}
		else {
			typeAssujettissement = TypeAssujettissement.NON_ASSUJETTI;
		}
		return typeAssujettissement;
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

				final RegDate debutPeriode = RegDate.get(annee, 1, 1);
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

				final RegDate debutPeriode = RegDate.get(annee, 1, 1);
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
