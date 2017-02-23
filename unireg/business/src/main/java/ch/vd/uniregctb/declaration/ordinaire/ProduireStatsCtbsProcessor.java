package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixte;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Produit des statistiques sur les contribuables assujettis pour la période fiscale spécifiée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ProduireStatsCtbsProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = LoggerFactory.getLogger(ProduireStatsCtbsProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final ServiceInfrastructureService infraService;
	private final TiersService tiersService;
	private final PlatformTransactionManager transactionManager;
	private final AssujettissementService assujettissementService;
	private final AdresseService adresseService;

	public ProduireStatsCtbsProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService, TiersService tiersService, PlatformTransactionManager transactionManager,
	                                  AssujettissementService assujettissementService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.assujettissementService = assujettissementService;
		this.adresseService = adresseService;
	}

	public StatistiquesCtbs runPP(int annee, RegDate dateTraitement, StatusManager statusManager) throws DeclarationException {
		return run(annee, dateTraitement, "PP", statusManager, this::chargerIdentifiantsContribuablesPP);
	}

	public StatistiquesCtbs runPM(int annee, RegDate dateTraitement, StatusManager statusManager) throws DeclarationException {
		return run(annee, dateTraitement, "PM", statusManager, this::chargerIdentifiantsContribuablesPM);
	}

	private StatistiquesCtbs run(int annee, RegDate dateTraitement, String population, StatusManager statusManager, IntFunction<List<Long>> idsContribuablesForPF) throws DeclarationException {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);

		final StatistiquesCtbs rapportFinal = new StatistiquesCtbs(annee, dateTraitement, population, tiersService, adresseService);

		status.setMessage(String.format("Production des statistiques des contribuables assujettis : période fiscale = %d.", annee));

		final List<Long> idsContribuables = idsContribuablesForPF.apply(annee);
		final BatchTransactionTemplateWithResults<Long, StatistiquesCtbs> template = new BatchTransactionTemplateWithResults<>(idsContribuables, BATCH_SIZE, Behavior.SANS_REPRISE, transactionManager, status);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, StatistiquesCtbs>() {

			@Override
			public StatistiquesCtbs createSubRapport() {
				return new StatistiquesCtbs(annee, dateTraitement, population, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, StatistiquesCtbs rapport) throws Exception {
				traiteBatch(batch, rapport, status, idsContribuables.size(), rapportFinal.nbCtbsTotal);
				return true;
			}
		}, null);

		status.setMessage("Extraction terminée.");

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite tous les contribuables spécifiés par l'iterateur passé en paramètre.
	 */
	private void traiteBatch(final List<Long> batch, final StatistiquesCtbs rapport, final StatusManager status, final int nbTotalContribuables, final int nbCtbTraites) {

		// on ne va rien changer
		hibernateTemplate.execute(FlushMode.MANUAL, session -> {
			boolean first = true;
			final Iterator<Long> iterator = batch.iterator();
			while (iterator.hasNext() && !status.interrupted()) {
				final Long id = iterator.next();
				if (first) {
					status.setMessage(String.format("Traitement du contribuable n°%d (%d/%d)", id, nbCtbTraites, nbTotalContribuables), (nbCtbTraites * 100) / nbTotalContribuables);
					first = false;
				}

				traiterCtb(id, rapport);
			}
			return null;
		});
	}

	/**
	 * Traite le contribuable dont l'id est passé en paramètre
	 */
	private void traiterCtb(Long id, final StatistiquesCtbs rapport) {

		Contribuable ctb = null;
		try {
			ctb = hibernateTemplate.get(Contribuable.class, id);

			final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, rapport.annee);
			if (assujettissements == null || assujettissements.isEmpty()) {
				// le contribuable n'est pas assujetti -> c'est possible car la requête SQL ne peut pas effectuer ce filtrage en amont
				return;
			}

			// Dans tous les cas, on prend l'assujettissement le plus récent
			final Assujettissement assujet = assujettissements.get(assujettissements.size() - 1);
			final StatistiquesCtbs.TypeContribuable typeCtb = determineType(assujet);
			final Commune commune;
			final Integer oid;
			if (typeCtb == StatistiquesCtbs.TypeContribuable.SOURCIER_PUR || ctb instanceof Entreprise) {
				commune = getCommuneDepuisFor(ctb, rapport.annee);
				oid = ctb instanceof Entreprise ? ServiceInfrastructureService.noOIPM : getOID(commune);
			}
			else {
				commune = getCommuneGestion(ctb, rapport.annee);
				oid = getOID(commune);
			}

			rapport.addStats(oid, commune, typeCtb);
			rapport.nbCtbsTotal++;
		}
		catch (Exception e) {
			rapport.addErrorException(ctb, e);
			LOGGER.error(String.format("La production des statistiques pour le contribuable [%d] a échoué.", id), e);
		}
	}

	/**
	 * @return l'id de l'office d'impôt responsable de la commune spécifiée.
	 */
	private Integer getOID(Commune commune) throws ServiceInfrastructureException {
		if (commune == null) {
			return null;
		}

		int noOfsCommune = commune.getNoOFS();
		OfficeImpot office = infraService.getOfficeImpotDeCommune(noOfsCommune);
		if (office == null) {
			return null;
		}

		return office.getNoColAdm();
	}

	/**
	 * @return la commune du for de gestion du contribuable spécifié, ou <b>null</b> si le contribuable ne possède pas de for de gestion.
	 * @throws ServiceInfrastructureException en cas de souci retourné par le service infrastructure
	 */
	private Commune getCommuneGestion(Contribuable ctb, int annee) throws ServiceInfrastructureException {
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, RegDate.get(annee, 12, 31));
		Commune commune = null;
		if (forGestion != null) {
			commune = infraService.getCommuneByNumeroOfs(forGestion.getNoOfsCommune(), forGestion.getDateFin());
		}
		return commune;
	}

	private Commune getCommuneDepuisFor(Contribuable ctb, int annee) throws ServiceInfrastructureException {
		final ForFiscalPrincipal ffp = ctb.getDernierForFiscalPrincipalVaudoisAvant(RegDate.get(annee, 12, 31));
		Commune commune = null;
		if (ffp != null) {
			commune = infraService.getCommuneByNumeroOfs(ffp.getNumeroOfsAutoriteFiscale(), ffp.getDateFin());
		}
		return commune;
	}

	/**
	 * Détermine le type de contribuable en fonction de son assujettissement.
	 * <p>
	 * Note: cette méthode est trop spécifique aux statistiques des contribuables pour être généralisée à la manière d'une méthode virtuelle
	 * sur la classe Assujettissement. Réciproquement, la classe Assujettissement est trop générique pour s'encombrer de règles spécifiques
	 * à un obscur job de production de statistiques ;-)
	 */
	private StatistiquesCtbs.TypeContribuable determineType(Assujettissement assujet) {

		final StatistiquesCtbs.TypeContribuable type;
		if (assujet instanceof DiplomateSuisse) {
			type = StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE;
		}
		else if (assujet instanceof HorsCanton) {
			type = StatistiquesCtbs.TypeContribuable.HORS_CANTON;
		}
		else if (assujet instanceof HorsSuisse) {
			type = StatistiquesCtbs.TypeContribuable.HORS_SUISSE;
		}
		else if (assujet instanceof Indigent) {
			type = StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE;
		}
		else if (assujet instanceof SourcierMixte) {
			/*
			 * Dans le cas où un contribuable possède plusieurs fors principaux, on prend juste le dernier (il s'agit de produire des
			 * statistiques, pas de réguler une centrale nucléaire quand même).
			 */
			final ForFiscalPrincipal dernierFor = assujet.getFors().principauxDansLaPeriode.last();

			/*
			 * La spécification dit : les sourciers mixtes (ayant un mode d’imposition « Mixte 137 al. 1 » ou « Mixte 137 al. 2 » sont
			 * comptés parmi les contribuables ordinaires, hors canton ou hors Suisse en fonction de leur for principal
			 */
			switch (dernierFor.getTypeAutoriteFiscale()) {
			case COMMUNE_OU_FRACTION_VD:
				type = StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE;
				break;
			case COMMUNE_HC:
				type = StatistiquesCtbs.TypeContribuable.HORS_CANTON;
				break;
			case PAYS_HS:
				type = StatistiquesCtbs.TypeContribuable.HORS_SUISSE;
				break;
			default:
				throw new IllegalArgumentException("Type d'autorité fiscale inconnue [" + dernierFor.getTypeAutoriteFiscale() + ']');
			}
		}
		else if (assujet instanceof SourcierPur) {
			type = StatistiquesCtbs.TypeContribuable.SOURCIER_PUR;
		}
		else if (assujet instanceof VaudoisDepense) {
			type = StatistiquesCtbs.TypeContribuable.VAUDOIS_DEPENSE;
		}
		else {
			Assert.isTrue(assujet instanceof VaudoisOrdinaire);
			type = StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE;
		}

		return type;
	}

	private static final String queryCtbsPP = // --------------------------------------------------
	"SELECT DISTINCT                                                                         "
			+ "    cont.id                                                                   "
			+ "FROM                                                                          "
			+ "    ContribuableImpositionPersonnesPhysiques AS cont                          "
			+ "INNER JOIN                                                                    "
			+ "    cont.forsFiscaux AS fors                                                  "
			+ "WHERE                                                                         "
			+ "    cont.annulationDate IS null                                               "
			+ "    AND fors.annulationDate IS null                                           "
			+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               " // = au moins 1 for actif dans l'année
			+ "    AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)                 "
			+ "ORDER BY cont.id ASC                                                          ";

	private static final String queryCtbsPM = // --------------------------------------------------
	"SELECT DISTINCT                                                                         "
			+ "    cont.id                                                                   "
			+ "FROM                                                                          "
			+ "    Entreprise AS cont                                                        "
			+ "INNER JOIN                                                                    "
			+ "    cont.forsFiscaux AS fors                                                  "
			+ "WHERE                                                                         "
			+ "    cont.annulationDate IS null                                               "
			+ "    AND fors.annulationDate IS null                                           "
			+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               " // = au moins 1 for actif dans l'année
			+ "    AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)                 "
			+ "ORDER BY cont.id ASC                                                          ";

	private List<Long> chargerIdentifiantsContribuablesPP(int annee) {
		return chargerIdentifiantsContribuables(annee, queryCtbsPP);
	}

	private List<Long> chargerIdentifiantsContribuablesPM(int annee) {
		return chargerIdentifiantsContribuables(annee, queryCtbsPM);
	}

	/**
	 * Crée un iterateur sur les ids des contribuables ayant au moins un for fiscal ouvert sur la période fiscale spécifiée.
	 * @param annee la période fiscale considérée
	 * @return itérateur sur les ids des contribuables trouvés
	 */
	private List<Long> chargerIdentifiantsContribuables(int annee, String query) {

		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query queryObject = session.createQuery(query);
			queryObject.setParameter("debutAnnee", debutAnnee);
			queryObject.setParameter("finAnnee", finAnnee);
			//noinspection unchecked
			return queryObject.list();
		}));
	}
}