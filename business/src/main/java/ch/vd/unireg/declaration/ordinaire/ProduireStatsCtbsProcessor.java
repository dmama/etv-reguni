package ch.vd.unireg.declaration.ordinaire;

import javax.persistence.FlushModeType;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.DiplomateSuisse;
import ch.vd.unireg.metier.assujettissement.HorsCanton;
import ch.vd.unireg.metier.assujettissement.HorsSuisse;
import ch.vd.unireg.metier.assujettissement.Indigent;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.assujettissement.SourcierMixte;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.metier.assujettissement.VaudoisDepense;
import ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceServiceException;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Produit des statistiques sur les contribuables assujettis pour la période fiscale spécifiée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ProduireStatsCtbsProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProduireStatsCtbsProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final ServiceInfrastructureService infraService;
	private final TiersService tiersService;
	private final PlatformTransactionManager transactionManager;
	private final AssujettissementService assujettissementService;
	private final AdresseService adresseService;
	private final PeriodeImpositionService periodeImpositionService;
	private final PeriodeImpositionImpotSourceService piisService;

	public ProduireStatsCtbsProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService, TiersService tiersService, PlatformTransactionManager transactionManager,
	                                  AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, AdresseService adresseService,
	                                  PeriodeImpositionImpotSourceService piisService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.assujettissementService = assujettissementService;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
		this.piisService = piisService;
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
		hibernateTemplate.execute(FlushModeType.COMMIT, session -> {
			boolean first = true;
			final Iterator<Long> iterator = batch.iterator();
			while (iterator.hasNext() && !status.isInterrupted()) {
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

			final Commune commune;
			final Integer oid;
			final StatistiquesCtbs.TypeContribuable typeCtb;

			if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
				final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, rapport.annee);
				if (assujettissements == null || assujettissements.isEmpty()) {
					// le contribuable n'est pas assujetti -> c'est possible car la requête SQL ne peut pas effectuer ce filtrage en amont
					return;
				}

				// Dans tous les cas, on prend l'assujettissement le plus récent
				final Assujettissement assujet = assujettissements.get(assujettissements.size() - 1);
				typeCtb = determineType(assujet);

				if (typeCtb == StatistiquesCtbs.TypeContribuable.SOURCIER_PUR) {
					//SIFISC-28319 Pas de PIIS, on ne va pas plus loin pour ce ctb
					if (absencePIIS(ctb, rapport.annee)) {
						return;
					}

					commune = getCommuneDepuisFors(ctb, RegDate.get(rapport.annee, 12, 31));
					oid = getOID(commune);
				}
				else {
					commune = getCommuneGestion(ctb, rapport.annee);
					oid = getOID(commune);
				}
			}
			else if (ctb instanceof Entreprise) {
				final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(ctb);
				if (periodesImposition == null || periodesImposition.isEmpty()) {
					// pas assujetti du tout, on oublie
					return;
				}

				// [SIFISC-24052] on cherche la dernière PI de l'année
				final PeriodeImposition lastPI = periodesImposition.stream()
						.filter(pi -> pi.getPeriodeFiscale() == rapport.annee)
						.max(Comparator.comparing(PeriodeImposition::getDateDebut))
						.orElse(null);
				if (lastPI == null) {
					// pas de bouclement dans l'année recherchée, donc pas listée
					return;
				}

				commune = getCommuneDepuisFors(ctb, lastPI.getDateFin());
				oid = ServiceInfrastructureService.noOIPM;
				typeCtb = extractTypeContribuable(lastPI);
			}
			else if (ctb != null) {
				throw new IllegalArgumentException("Mauvais type de contribuable (" + ctb.getClass().getName() + ") pour l'identifiant " + id);
			}
			else {
				throw new IllegalArgumentException("Aucun contribuable connu pour l'identifiant " + id);
			}

			rapport.addStats(oid, commune, typeCtb);
			rapport.nbCtbsTotal++;
		}
		catch (Exception e) {
			if (ctb == null) {
				rapport.addErrorException(id, e);
			}
			else {
				rapport.addErrorException(ctb, e);
			}
			LOGGER.error(String.format("La production des statistiques pour le contribuable [%d] a échoué.", id), e);
		}
	}

	/**
	 * Regarde si le ctb n'a pas de PIIS  pour la période considérée
	 * @param ctb à analyser
	 * @param annee période considéreé
	 * @return true si pas de PIIS trouvée, valse sinon
	 */
	private boolean absencePIIS(Contribuable ctb, int annee) throws PeriodeImpositionImpotSourceServiceException {

		if (ctb instanceof PersonnePhysique) {
			return absencePIIS((PersonnePhysique)ctb,annee);
		}
		else if (ctb instanceof MenageCommun) {
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, annee);
			return absencePIIS(etc.getPrincipal(),annee) && absencePIIS(etc.getConjoint(),annee);
		}
		return true;

	}

	private boolean absencePIIS(PersonnePhysique pp, int annee) throws PeriodeImpositionImpotSourceServiceException {
		if (pp == null) {
			return true;
		}
		final RegDate debut = RegDate.get(annee, 1, 1);
		final RegDate fin = RegDate.get(annee, 12, 31);
		final DateRangeHelper.Range periode = new DateRangeHelper.Range(debut, fin);
		final List<PeriodeImpositionImpotSource> listePiis = piisService.determine(pp);
		if (DateRangeHelper.intersect(periode, listePiis)) {
			return false;
		}
		else {
			return true;
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

	/**
	 * On prend les fors vaudois principaux ou secondaires non-annulés qui commencent avant la date maximale,
	 * et dans ceux-là on prend "le dernier", qui est définit par
	 * <ul>
	 *     <li>le for principal valide à la date max, ou, en absence d'un tel for,</li>
	 *     <li>le for secondaire le plus ancien valide à la date max, ou, en absence d'un tel for,</li>
	 *     <li>le dernier for fiscal principal ou secondaire, fermé avant la date max (priorité au for principal ou au for secondaire le plus ancien en cas d'égalité de dates)</li>
	 * </ul>
	 * @param ctb un contribuable
	 * @param max la date seuil avant laquelle tout se passe
	 * @return la commune du for trouvé, ou <code>null</code> s'il n'y en a pas
	 * @throws ServiceInfrastructureException en cas de souci retourné par le service infrastructure
	 */
	private Commune getCommuneDepuisFors(Contribuable ctb, RegDate max) throws ServiceInfrastructureException {
		final Function<ForFiscal, RegDate> dateFinExtractor = ff -> ff.getDateFin() == null || ff.getDateFin().isAfter(max) ? null : ff.getDateFin();
		return ctb.getForsFiscauxNonAnnules(false).stream()
				.filter(ff -> ff.getDateDebut().isBeforeOrEqual(max))
				.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				.filter(ff -> ff instanceof ForFiscalPrincipal || ff instanceof ForFiscalSecondaire)
				.max(Comparator.comparing(dateFinExtractor, NullDateBehavior.LATEST::compare)                       // date de fin maximale
						     .thenComparing(ForFiscal::isPrincipal)                                                 // principal après secondaire
						     .thenComparing(ForFiscal::getDateDebut, Comparator.reverseOrder())                     // du plus jeune au plus ancien
						     .thenComparing(ForFiscal::getId, Comparator.reverseOrder()))                           // par identifiant décroissant
				.map(ff -> infraService.getCommuneByNumeroOfs(ff.getNumeroOfsAutoriteFiscale(), ff.getDateFin()))
				.orElse(null);
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
		else if (assujet instanceof VaudoisOrdinaire) {
			type = StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE;
		}
		else {
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + assujet.getClass() + "]");
		}

		return type;
	}

	private static StatistiquesCtbs.TypeContribuable extractTypeContribuable(PeriodeImposition pi) {
		switch (pi.getTypeContribuable()) {
		case HORS_CANTON:
			return StatistiquesCtbs.TypeContribuable.HORS_CANTON;
		case HORS_SUISSE:
			return StatistiquesCtbs.TypeContribuable.HORS_SUISSE;
		case UTILITE_PUBLIQUE:
			return StatistiquesCtbs.TypeContribuable.UTILITE_PUBLIQUE;
		case VAUDOIS_ORDINAIRE:
			return StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE;
		default:
			throw new IllegalArgumentException("Type de contribuable innatendu : " + pi.getTypeContribuable());
		}
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
