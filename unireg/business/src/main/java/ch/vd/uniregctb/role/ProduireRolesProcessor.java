package ch.vd.uniregctb.role;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
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
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeAssujettissement;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoFor;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Processeur spécialisé dans la production des rôles pour les communes vaudoises. Ce processeur doit être appelé par le service 'rôle'
 * uniquement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ProduireRolesProcessor {

	final Logger LOGGER = Logger.getLogger(ProduireRolesProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;

	private final ServiceInfrastructureService infraService;

	private final TiersDAO tiersDAO;

	private final PlatformTransactionManager transactionManager;
	
	private final AdresseService adresseService;
	
	private final TiersService tiersService;

	private final ServiceCivilService serviceCivilService;

	private final ValidationService validationService;

	public ProduireRolesProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService, TiersDAO tiersDAO, PlatformTransactionManager transactionManager,
	                                     AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivilService, ValidationService validationService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.serviceCivilService = serviceCivilService;
		this.validationService = validationService;
	}

	private static interface GroupementCommunes {
		boolean isDansGroupementDeCommunes(Integer ofsCandidat, Integer ofsReference);
	}

	private static class CommuneParCommune implements GroupementCommunes {
		public boolean isDansGroupementDeCommunes(Integer ofsCandidat, Integer ofsReference) {
			return ofsCandidat != null && ofsCandidat.equals(ofsReference);
		}
	}

	private static class GroupementCommunesPourOID implements GroupementCommunes {
		private final Set<Integer> ofsCommunes;

		private GroupementCommunesPourOID(Set<Integer> ofsCommunes) {
			this.ofsCommunes = ofsCommunes;
		}

		public boolean isDansGroupementDeCommunes(Integer ofsCandidat, Integer ofsReference) {
			return ofsCommunes.contains(ofsCandidat) && ofsCommunes.contains(ofsReference);
		}
	}

	/**
	 * Interface implémentée par les variantes de la production des rôles (tous, pour un OID, pour une commune)
	 */
	private static interface VarianteProductionRole<T extends ProduireRolesResults> {
		/**
		 * Renvoie la liste des ID techniques des contribuables listés dans la variante concernée
		 */
		List<Long> getIdsContribuablesConcernes(int anneePeriode);

		/**
		 * Instancie un nouveau rapport (intermédiaire et final)
		 */
		T creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal);

		/**
		 * Retourne le groupement de commune a utilisé pour la recherche de fors historiques
		 */
		GroupementCommunes getGroupementCommunes();
	}

	/**
	 * Factorisation du code de processing indépendant de la variante
	 * @param anneePeriode
	 * @param statusMessagePrefix
	 * @param status
	 * @param variante   @return
	 */
	private <T extends ProduireRolesResults> T doRun(final int anneePeriode, final int nbThreads, String statusMessagePrefix, final StatusManager status, final VarianteProductionRole<T> variante) {

		final RegDate today = RegDate.get();
		final T rapportFinal = variante.creerRapport(anneePeriode, nbThreads, today, true);

		// parties à aller chercher en bloc par groupe de tiers
		final Set<TiersDAO.Parts> parts = new HashSet<TiersDAO.Parts>();
		parts.add(TiersDAO.Parts.FORS_FISCAUX);
		parts.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
		parts.add(TiersDAO.Parts.ADRESSES);
		parts.add(TiersDAO.Parts.DECLARATIONS);

		// groupement de communes (utilisé pour calculer la date du premier for, et la date du dernier)
		final GroupementCommunes groupement = variante.getGroupementCommunes();

		// préfixe pour les différentes phases du batch (s'il y en a)
		final String prefixe;
		if (!StringUtils.isBlank(statusMessagePrefix)) {
			prefixe = String.format("%s. ", statusMessagePrefix);
		}
		else {
			prefixe = "";
		}

		final String msgRechercheContribuables = String.format("%sRecherche des contribuables.", prefixe);
		status.setMessage(msgRechercheContribuables);

		final List<Long> list = variante.getIdsContribuablesConcernes(anneePeriode);
		final ParallelBatchTransactionTemplate<Long, T> template = new ParallelBatchTransactionTemplate<Long, T>(list, BATCH_SIZE, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, T>() {

			@Override
			public boolean doInTransaction(List<Long> batch, T rapport) throws Exception {

				final List<Tiers> tierz = tiersDAO.getBatch(batch, parts);

				final String msg = String.format("%sTraitement des contribuables (%d traité(s)).", prefixe, rapportFinal.ctbsTraites);
				status.setMessage(msg, rapportFinal.ctbsTraites * 100 / list.size());

				// première boucle sur les tiers pour aller chercher en un bloc les individus du civil
				// pour les habitants (nom, adresse, no-avs...)
				preloadIndividus(tierz, anneePeriode);

				// deuxième boucle pour le traitement proprement dit
				for (Tiers tiers : tierz) {

					final long ctbId = tiers.getNumero();
					final Contribuable ctb = (Contribuable) tiers;

					try {
						processContribuable(anneePeriode, rapport, groupement, ctb);
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
			public T createSubRapport() {
				return variante.creerRapport(anneePeriode, nbThreads, today, false);
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();

		return rapportFinal;
	}

	private void preloadIndividus(List<Tiers> tierz, int anneePeriode) {
		final Map<Long, PersonnePhysique> ppByNoIndividu = new HashMap<Long, PersonnePhysique>(tierz.size() * 2);
		final RegDate date = RegDate.get(anneePeriode, 12, 31);
		for (Tiers tiers : tierz) {
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.isConnuAuCivil()) {
					final Long noIndividu = pp.getNumeroIndividu();
					ppByNoIndividu.put(noIndividu, pp);
				}
			}
			else if (tiers instanceof MenageCommun) {
				final Set<PersonnePhysique> membres = tiersService.getComposantsMenage((MenageCommun) tiers, date);
				if (membres != null) {
					for (PersonnePhysique pp : membres) {
						if (pp.isConnuAuCivil()) {
							ppByNoIndividu.put(pp.getNumeroIndividu(), pp);
						}
					}
				}
			}
		}

		if (ppByNoIndividu.size() > 0) {
			// remplit le cache des individus...
			final List<Individu> individus = serviceCivilService.getIndividus(ppByNoIndividu.keySet(), date, AttributeIndividu.ADRESSES);

			// et on remplit aussi le cache individu sur les personnes physiques... (utilisé pour l'accès à la date de décès et au sexe)
			for (Individu individu : individus) {
				final PersonnePhysique pp = ppByNoIndividu.get(individu.getNoTechnique());
				pp.setIndividuCache(individu);
			}
		}
	}

	/**
	 * Produit la liste de contribuables de toutes les communes (et fractions de commune) vaudoise pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @return un rapport (technique) sur les rôles par commune et contribuables.
	 */
	public ProduireRolesCommunesResults runPourToutesCommunes(final int anneePeriode, final int nbThreads, final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final GroupementCommunes communeParCommune = new CommuneParCommune();

		return doRun(anneePeriode, nbThreads, null, status, new VarianteProductionRole<ProduireRolesCommunesResults>() {
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuables(anneePeriode);
			}

			public ProduireRolesCommunesResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesCommunesResults(anneePeriode, nbThreads, today);
			}

			public GroupementCommunes getGroupementCommunes() {
				return communeParCommune;
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
	public ProduireRolesCommunesResults runPourUneCommune(final int anneePeriode, final int noOfsCommune, final int nbThreads, final StatusManager s) throws ServiceException {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final GroupementCommunes communeParCommune = new CommuneParCommune();

		return doRun(anneePeriode, nbThreads, null, status, new VarianteProductionRole<ProduireRolesCommunesResults>() {
			public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
				return getIdsOfAllContribuablesSurCommunes(anneePeriode, Arrays.asList(noOfsCommune));
			}

			public ProduireRolesCommunesResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
				return new ProduireRolesCommunesResults(anneePeriode, noOfsCommune, nbThreads, today);
			}

			public GroupementCommunes getGroupementCommunes() {
				return communeParCommune;
			}
		});
	}

	/**
	 * Produit la liste de contribuables d'un office d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode l'année de la période fiscale considérée.
	 * @param oid l'id de l'office d'impôt concerné
	 * @param nbThreads le nombre de threads de processing souhaité
	 * @param s status manager
	 * @return un rapport (technique) sur les rôles des contribuables de la commune spécifiée.
	 */
	public ProduireRolesOIDsResults runPourUnOfficeImpot(int anneePeriode, int oid, int nbThreads, StatusManager s) throws ServiceException {
		return runPourUnOfficeImpot(anneePeriode, oid, nbThreads, null, s, false);
	}

	private ProduireRolesOIDsResults runPourUnOfficeImpot(int anneePeriode, final int oid, int nbThreads, String statusMessagePrefixe, StatusManager s, boolean nullSiAucuneCommune) throws ServiceException {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// récupère les numéros Ofs des communes gérées par l'office d'impôt spécifié
		final Set<Integer> nosOfsCommunes = new HashSet<Integer>();
		try {
			final List<Commune> communes = infraService.getListeCommunesByOID(oid);
			for (Commune c : communes) {
				nosOfsCommunes.add(c.getNoOFSEtendu());
			}
		}
		catch (InfrastructureException e) {
			throw new ServiceException(e);
		}

		if (nosOfsCommunes.size() > 0 || !nullSiAucuneCommune) {
			final GroupementCommunes groupement = new GroupementCommunesPourOID(nosOfsCommunes);
			return doRun(anneePeriode, nbThreads, statusMessagePrefixe, status, new VarianteProductionRole<ProduireRolesOIDsResults>() {

				public List<Long> getIdsContribuablesConcernes(int anneePeriode) {
					return getIdsOfAllContribuablesSurCommunes(anneePeriode, nosOfsCommunes);
				}

				public ProduireRolesOIDsResults creerRapport(int anneePeriode, int nbThreads, RegDate today, boolean isRapportFinal) {
					return new ProduireRolesOIDsResults(anneePeriode, oid, nbThreads, today);
				}

				public GroupementCommunes getGroupementCommunes() {
					return groupement;
				}
			});
		}
		else {
			return null;
		}
	}

	/**
	 * Produit la liste de contribuables de tous les offices d'impôt pour la période fiscale spécifiée.
	 * <p>
	 * Note: le rapport peut contenir quelques résultats pour des communes autres que celles gérées par l'office d'impôt, en fonction des
	 * déménagement des contribuables.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param nbThreads nombre de threads de traitement
	 * @return les rapports (techniques) sur les rôles des contribuables pour chaque OID
	 */
	public ProduireRolesOIDsResults[] runPourTousOfficesImpot(final int anneePeriode, final int nbThreads, final StatusManager s) throws ServiceException {
		try {
			// on va boucler sur tous les OIDs connus dans l'ordre des numéro de collectivité administrative
			final List<OfficeImpot> oids = infraService.getOfficesImpot();
			final List<OfficeImpot> oidsTries = new ArrayList<OfficeImpot>(oids);
			Collections.sort(oidsTries, new Comparator<OfficeImpot>() {
				public int compare(OfficeImpot o1, OfficeImpot o2) {
					return o1.getNoColAdm() - o2.getNoColAdm();
				}
			});

			// on commence enfin la boucle
			int index = 0;
			final List<ProduireRolesOIDsResults> liste = new ArrayList<ProduireRolesOIDsResults>(oidsTries.size());
			for (OfficeImpot oid : oidsTries) {
				final String prefixe = String.format("%s (%d/%d)", oid.getNomCourt(), ++ index, oidsTries.size());
				final ProduireRolesOIDsResults results = runPourUnOfficeImpot(anneePeriode, oid.getNoColAdm(), nbThreads, prefixe, s, true);
				if (results != null) {
					liste.add(results);
				}
				if (s.interrupted()) {
					break;
				}
			}
			return liste.toArray(new ProduireRolesOIDsResults[liste.size()]);
		}
		catch (InfrastructureException e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Traite le contribuable spécifié et ajoute toutes les informations nécessaires au rapport
	 */
	private void processContribuable(int anneePeriode, ProduireRolesResults rapport, GroupementCommunes groupement, Contribuable ctb) {

		++ rapport.ctbsTraites;

		if (tiersService.isSourcierGris(ctb, RegDate.get(anneePeriode, 12, 31))) {
			rapport.addCtbIgnoreSourcierGris(ctb);
		}
		else if (validationService.validate(ctb).hasErrors()) {
			rapport.addErrorCtbInvalide(ctb);
		}
		else {

			// ajoute les assujettissements du contribuable de l'année spécifiée dans le rapprot
			try {
				processAssujettissements(anneePeriode, ctb, rapport, groupement);
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
	 * @param groupement
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processAssujettissements(int anneePeriode, Contribuable contribuable, ProduireRolesResults rapport, GroupementCommunes groupement) throws TraitementException {

		final AssujettissementContainer assujettissementContainer = new AssujettissementContainer(contribuable, anneePeriode, rapport);

		// traite les assujettissements
		final List<Assujettissement> assujettissements = assujettissementContainer.getAssujettissementAnneePeriode();
		if (assujettissements != null) {
			for (Assujettissement a : assujettissements) {
				processAssujettissement(a, assujettissementContainer, groupement);
			}
		}
		else {

			final DecompositionForsAnneeComplete fors = assujettissementContainer.getForsAnneePeriode();
			if (!fors.isFullyEmpty()) {
				// pas d'assujettissement, mais des fors quand-même... A priori ils se terminent dans la période...
				processNonAssujettissement(fors, assujettissementContainer, groupement);
			}
		}
	}

	/**
	 * Ajoute les détails de l'assujettissements du contribuable spécifié au rapport.
	 *
	 * @param assujet l'assujettissement à traiter
	 * @param assujettissementContainer cache pour les assujettissements calculés sur la période courante et la précédente
	 * @param groupement
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processAssujettissement(Assujettissement assujet, AssujettissementContainer assujettissementContainer, GroupementCommunes groupement) throws TraitementException {

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
			if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == f.getTypeAutoriteFiscale()) {
				processForFiscal(assujet, f, typeCtb, assujettissementContainer, groupement);
			}
		}
		for (ForFiscalRevenuFortune f : fors.secondairesDansLaPeriode) {
			processForFiscal(assujet, f, typeCtb, assujettissementContainer, groupement);
		}
	}

	/**
	 * Ajoute les détails du non-assujettissement au rapport (= cas du contribuable ayant au moins un for dans le canton mais pas du tout
	 * assujetti dans l'année)
	 *
	 * @param fors la décomposition des fors du contribuable pour l'année de la période courante
	 * @param assujettissementContainer cache pour les assujettissements calculés sur la période courante et la précédente
	 * @param groupement
	 * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	private void processNonAssujettissement(DecompositionForsAnneeComplete fors, AssujettissementContainer assujettissementContainer, GroupementCommunes groupement) throws TraitementException {

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
			traiteNonAssujettiAvecPrecedentAssujettissement(assujettissementContainer, noOfsCommune, groupement);
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
				return Assujettissement.determine(fors.contribuable, fors, true);
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
	 * @param groupement
     * @throws TraitementException en cas ayant conduit à interrompre le traitement (automatiqement renseigné dans le rapport)
	 */
	protected void processForFiscal(Assujettissement assujettissement, ForFiscalRevenuFortune forFiscal, TypeContribuable typeCtb, AssujettissementContainer assujettissementContainer,
	                                GroupementCommunes groupement) throws TraitementException {

		final ProduireRolesResults rapport = assujettissementContainer.rapport;
		final Contribuable contribuable = assujettissementContainer.ctb;
		final int anneePeriode = assujettissementContainer.anneePeriode;

		if (TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD != forFiscal.getTypeAutoriteFiscale()) {
			final String details = String.format("L'autorité fiscale du for [%s] n'est pas dans le canton.", forFiscal);
			rapport.addCtbIgnoreDonneesIncoherentes(contribuable, details);
			throw new TraitementException();
		}

		try {
			final Integer ofsCommune = forFiscal.getNumeroOfsAutoriteFiscale();
			final TypeAssujettissement typeAssujettissement = getTypeAssujettissementPourCommune(ofsCommune, assujettissement, anneePeriode);
			if (typeAssujettissement == TypeAssujettissement.NON_ASSUJETTI) {

				// pas d'assujettissement sur cette commune dans l'annee de période
				// -> s'il y en avait un avant, il faut l'indiquer dans le rapport
				// (sauf si le contribuable était alors sourcier gris, auquel cas il ne doit pas apparaître)
				if (!tiersService.isSourcierGris(contribuable, RegDate.get(anneePeriode - 1, 12, 31))) {
					traiteNonAssujettiAvecPrecedentAssujettissement(assujettissementContainer, ofsCommune, groupement);
				}
			}
			else {
				final DebutFinFor debutFin = getInformationDebutFin(forFiscal, groupement, anneePeriode);
				final InfoCommune infoCommune = rapport.getOrCreateInfoPourCommune(ofsCommune);
				final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(contribuable, anneePeriode, adresseService, tiersService);
				final InfoFor infoFor = new InfoFor(typeCtb, debutFin.dateOuverture, debutFin.motifOuverture, debutFin.dateFermeture, debutFin.motifFermeture, typeAssujettissement, forFiscal.isPrincipal(), forFiscal.getMotifRattachement(), ofsCommune);
				infoCtb.addFor(infoFor);
			}
		}
		catch (AssujettissementException e) {
			rapport.addErrorErreurAssujettissement(contribuable, e.getMessage());
			throw new TraitementException();
		}
	}

	private void traiteNonAssujettiAvecPrecedentAssujettissement(AssujettissementContainer assujettissementContainer, Integer ofsCommune, GroupementCommunes groupement) throws TraitementException {

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
						traiteNonAssujettiAvecPrecedentAssujettissement(rapport, contribuable, anneePeriode, ofsCommune, groupement, typeCtbAnneePrecedente, ff);
					}
				}
				for (ForFiscalRevenuFortune ff : forsAnneePrecedente.secondairesDansLaPeriode) {
					if (ff.getNumeroOfsAutoriteFiscale() == ofsCommune.intValue()) {
						traiteNonAssujettiAvecPrecedentAssujettissement(rapport, contribuable, anneePeriode, ofsCommune, groupement, typeCtbAnneePrecedente, ff);
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
	 * @param groupement
	 * @param typeCtbAnneePrecedente type de contribuable sur la commune à l'époque de la période précédente
	 * @param ff for fiscal sur la commune qui était ouvert dans la période précédente
	 */
	private void traiteNonAssujettiAvecPrecedentAssujettissement(ProduireRolesResults rapport, Contribuable contribuable, int anneePeriode, Integer ofsCommune,
	                                                             GroupementCommunes groupement, TypeContribuable typeCtbAnneePrecedente, ForFiscalRevenuFortune ff) {
		final DebutFinFor debutFin = getInformationDebutFin(ff, groupement, anneePeriode);
		final InfoCommune infoCommune = rapport.getOrCreateInfoPourCommune(ofsCommune);
		final InfoContribuable infoCtb = infoCommune.getOrCreateInfoPourContribuable(contribuable, anneePeriode, adresseService, tiersService);
		final InfoFor infoFor = new InfoFor(debutFin.dateOuverture, debutFin.motifOuverture, debutFin.dateFermeture, debutFin.motifFermeture, typeCtbAnneePrecedente, ff.isPrincipal(), ff.getMotifRattachement(), ofsCommune);
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

	private static DebutFinFor getInformationDebutFin(ForFiscalRevenuFortune forFiscal, GroupementCommunes groupement, int anneePeriode) {

		final Tiers tiers = forFiscal.getTiers();

		// on pousse vers le passé autant qu'on peut tant qu'on reste sur la même commune
		ForFiscalRevenuFortune candidatRetenu = forFiscal;
		while (true) {
			final List<ForFiscal> fors = tiers.getForsFiscauxValidAt(candidatRetenu.getDateDebut().getOneDayBefore());
			ForFiscalRevenuFortune nouveauCandidat = null;
			for (ForFiscal candidat : fors) {
				if (candidat instanceof ForFiscalRevenuFortune &&
						candidat.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
						groupement.isDansGroupementDeCommunes(candidat.getNumeroOfsAutoriteFiscale(), forFiscal.getNumeroOfsAutoriteFiscale()) &&
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
		final ForFiscalRevenuFortune forFiscalPourOuverture = candidatRetenu;

		// on pousse vers le futur tant qu'on peut et qu'on reste sur la même commune
		candidatRetenu = forFiscal;
		while (candidatRetenu.getDateFin() != null) {
			final List<ForFiscal> fors = tiers.getForsFiscauxValidAt(candidatRetenu.getDateFin().getOneDayAfter());
			ForFiscalRevenuFortune nouveauCandidat = null;
			for (ForFiscal candidat : fors) {
				if (candidat instanceof ForFiscalRevenuFortune &&
						candidat.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
						groupement.isDansGroupementDeCommunes(candidat.getNumeroOfsAutoriteFiscale(), forFiscal.getNumeroOfsAutoriteFiscale()) &&
						((ForFiscalRevenuFortune) candidat).getMotifRattachement() == forFiscal.getMotifRattachement()) {
					if (nouveauCandidat == null || (nouveauCandidat.getDateFin() != null && nouveauCandidat.getDateFin().isBefore(candidat.getDateFin()))) {
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
		final ForFiscalRevenuFortune forFiscalPourFermeture = candidatRetenu;

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

	private static TypeAssujettissement getTypeAssujettissementPourCommune(int noOfsCommune, Assujettissement assujettissement, int anneePeriode) throws AssujettissementException {
		final boolean communeActive = assujettissement != null && assujettissement.isActifSurCommune(noOfsCommune);
		final TypeAssujettissement typeAssujettissement;
		if (communeActive) {
			if (assujettissement.getMotifFractFin() != null) {
				typeAssujettissement = TypeAssujettissement.TERMINE_DANS_PF;
			}
			else {

				// ok, l'assujettissement global est poursuivi dans la PF suivante, mais peut-être pas pour la commune considérée
				// -> il faut le re-calculer !

				final List<Assujettissement> assujettissementsCommune = Assujettissement.determinePourCommune(assujettissement.getContribuable(), noOfsCommune);
				final Assujettissement assujettissementDebutAnneeSuivante = DateRangeHelper.rangeAt(assujettissementsCommune, RegDate.get(anneePeriode + 1, 1, 1));
				if (assujettissementDebutAnneeSuivante == null) {
					typeAssujettissement = TypeAssujettissement.TERMINE_DANS_PF;
				}
				else {
					typeAssujettissement = TypeAssujettissement.POURSUIVI_APRES_PF;
				}
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
		else if (a instanceof DiplomateSuisse && a.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
			typeCtb = TypeContribuable.HORS_SUISSE;
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

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return (List<Long>) template.execute(new TransactionCallback() {
			public List<Long> doInTransaction(TransactionStatus status) {
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
		});
	}

	/**
	 * @return la liste des ID de tous les contribuables ayant au moins un for fiscal actif dans la commune vaudoise spécifiée durant l'année
	 *         spécifiée <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement). Si aucune commune n'est donnée, aucun contribuable ne sera renvoyé.
	 */
	@SuppressWarnings({"unchecked"})
	protected List<Long> getIdsOfAllContribuablesSurCommunes(final int annee, final Collection<Integer> noOfsCommunes) {

		if (noOfsCommunes == null || noOfsCommunes.size() == 0) {
			return Collections.emptyList();
		}
		else {
			final StringBuilder b = new StringBuilder();
			b.append("SELECT DISTINCT cont.id FROM Contribuable AS cont INNER JOIN cont.forsFiscaux AS for");
			b.append(" WHERE cont.annulationDate IS NULL");
			b.append(" AND for.annulationDate IS NULL AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
			b.append(" AND for.numeroOfsAutoriteFiscale IN (:noOfsCommune)");
			b.append(" AND (for.dateDebut IS NULL OR for.dateDebut <= :finPeriode)");
			b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
			b.append(" ORDER BY cont.id ASC");
			final String hql = b.toString();

			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			return (List<Long>) template.execute(new TransactionCallback() {
				public List<Long> doInTransaction(TransactionStatus status) {
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
			});
		}
	}
}
