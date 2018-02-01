package ch.vd.unireg.declaration.ordinaire;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.DiplomateSuisse;
import ch.vd.unireg.metier.assujettissement.HorsCanton;
import ch.vd.unireg.metier.assujettissement.HorsSuisse;
import ch.vd.unireg.metier.assujettissement.Indigent;
import ch.vd.unireg.metier.assujettissement.SourcierMixte;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.metier.assujettissement.VaudoisDepense;
import ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class ProduireStatsDIsProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = LoggerFactory.getLogger(ProduireStatsDIsProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final ServiceInfrastructureService infraService;
	private final PlatformTransactionManager transactionManager;
	private final DeclarationImpotOrdinaireDAO diDAO;
	private final AssujettissementService assujettissementService;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public ProduireStatsDIsProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService,
	                                 PlatformTransactionManager transactionManager, DeclarationImpotOrdinaireDAO diDAO, AssujettissementService assujettissementService, TiersService tiersService,
	                                 AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.transactionManager = transactionManager;
		this.diDAO = diDAO;
		this.assujettissementService = assujettissementService;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public StatistiquesDIs runPP(int annee, RegDate dateTraitement, StatusManager statusManager) throws DeclarationException {
		return run(annee, dateTraitement, statusManager, "PP", this::chargerIdentifiantsDeclarationsPP, id -> (DeclarationImpotOrdinairePP) diDAO.get(id), this::traiterDIPP);
	}

	public StatistiquesDIs runPM(int annee, RegDate dateTraitement, StatusManager statusManager) throws DeclarationException {
		return run(annee, dateTraitement, statusManager, "PM", this::chargerIdentifiantsDeclarationsPM, id -> (DeclarationImpotOrdinairePM) diDAO.get(id), this::traiterDIPM);
	}

	private <T extends DeclarationImpotOrdinaire> StatistiquesDIs run(final int annee,
	                                                                  final RegDate dateTraitement,
	                                                                  StatusManager statusManager,
	                                                                  String population,
	                                                                  IntFunction<List<Long>> idsDeclarationsPourPF,
	                                                                  LongFunction<T> diAccessor,
	                                                                  BiConsumer<? super T, StatistiquesDIs> traitement) throws DeclarationException {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);
		final StatistiquesDIs rapportFinal = new StatistiquesDIs(annee, dateTraitement, population, tiersService, adresseService);

		status.setMessage(String.format("Production des statistiques des déclaration d'impôts ordinaires : période fiscale = %d.", annee));

		final List<Long> listeComplete = idsDeclarationsPourPF.apply(annee);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, StatistiquesDIs> template = new BatchTransactionTemplateWithResults<>(listeComplete, BATCH_SIZE, Behavior.SANS_REPRISE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, StatistiquesDIs>() {

			@Override
			public StatistiquesDIs createSubRapport() {
				return new StatistiquesDIs(annee, dateTraitement, population, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(final List<Long> batch, StatistiquesDIs rapport) throws Exception {

				final Iterator<Long> iter = batch.iterator();
				while (iter.hasNext() && !status.isInterrupted()) {

					final Long id = iter.next();

					++ rapportFinal.nbDIsTotal;
					if (rapportFinal.nbDIsTotal % 100 == 0) {
						status.setMessage(String.format("Traitement de la DI n°%d (%d/%d)", id, rapportFinal.nbDIsTotal, listeComplete.size()), progressMonitor.getProgressInPercent());
					}

					final T di = diAccessor.apply(id);
					try {
						if (di != null) {
							traitement.accept(di, rapport);
						}
					}
					catch (Exception e) {
						rapport.addErrorException(di, e);

						final String message = String.format("La production des statistiques des DIs pour le contribuable [%s] a échoué.",
												di.getTiers().getNumero());
						LOGGER.error(message, e);
					}
				}
				return true;
			}
		}, progressMonitor);

		status.setMessage("Extraction terminée.");
		
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Ajoute les statistiques de la DI PP et des détails spécifiés au rapport.
	 */
	private void traiterDIPP(DeclarationImpotOrdinairePP di, StatistiquesDIs rapport) {
		final int oid = getOID(di);
		final TypeContribuable type = getType(di);
		final TypeEtatDocumentFiscal etat = getEtat(di);

		rapport.addStats(oid, type, etat);
	}

	/**
	 * Ajoute les statistiques de la DI PM et des détails spécifiés au rapport.
	 */
	private void traiterDIPM(DeclarationImpotOrdinairePM di, StatistiquesDIs rapport) {
		final TypeContribuable type = getType(di);
		final TypeEtatDocumentFiscal etat = getEtat(di);

		rapport.addStats(ServiceInfrastructureService.noOIPM, type, etat);
	}

	/**
	 * @return l'id de l'office d'impôt responsable de la DI spécifiée.
	 */
	private int getOID(DeclarationImpotOrdinairePP di) throws ServiceInfrastructureException {
		int oid = 0;
		final Integer noCommune = di.getNumeroOfsForGestion();
		if (noCommune != null) {
			OfficeImpot office = infraService.getOfficeImpotDeCommune(noCommune);
			if (office != null) {
				oid = office.getNoColAdm();
			}
		}
		return oid;
	}

	/**
	 * @return le type de contribuable stocké sur la DI ou déterminé à partir de son assujettissement
	 */
	private TypeContribuable getType(DeclarationImpotOrdinaire di) {

		TypeContribuable type = di.getTypeContribuable();
		if (type == null) {
			/*
			 * Cas exceptionnel : si le type de contribuable n'est pas renseignée sur la DI (= cas de la migration), on essaie de retrouver
			 * cette information à partir des fors fiscaux.
			 */
			type = determineType(di);
		}

		return type;
	}

	/**
	 * Recalcul de le type de contribuable du tiers associé à l'époque de l'envoi de la déclaration.
	 * <p>
	 * Note: cette méthode est trop spécifique aux statistiques des DIs pour être généralisée à la manière d'une méthode virtuelle
	 * sur la classe Assujettissement. La classes Assujettissement est elle trop générique pour s'encombrer de règles spécifiques à un
	 * obscur job de production de statistiques ;-)
	 *
	 * @param di
	 *            le déclaration dont on cherche le type de contribuable associé
	 * @return le type de contribuable ou <b>null</b> s'il n'est pas possible de retrouver cette information pour une raison ou une autre.
	 */
	private TypeContribuable determineType(DeclarationImpotOrdinaire di) {

		final Contribuable contribuable = di.getTiers();
		if (contribuable == null) {
			return null;
		}

		final int annee = di.getPeriode().getAnnee();
		List<Assujettissement> assujettissements;
		try {
			assujettissements = assujettissementService.determine(contribuable, annee);
		}
		catch (AssujettissementException e) {
			assujettissements = null;
		}
		if (assujettissements == null || assujettissements.isEmpty()) {
			return null;
		}

		// on essaie de retrouver l'assujettissement qui correspond précisemment à la DI
		Assujettissement assujet = DateRangeHelper.rangeAt(assujettissements, di.getDateFin());
		if (assujet == null) {
			// autrement, on se rabat sur le dernier assujettissement
			assujet = assujettissements.get(assujettissements.size() - 1);
		}
		if (assujet == null) {
			return null;
		}

		final TypeContribuable type;
		if (assujet instanceof DiplomateSuisse) {
			type = TypeContribuable.DIPLOMATE_SUISSE;
		}
		else if (assujet instanceof HorsCanton) {
			type = TypeContribuable.HORS_CANTON;
		}
		else if (assujet instanceof HorsSuisse) {
			type = TypeContribuable.HORS_SUISSE;
		}
		else if (assujet instanceof Indigent) {
			type = TypeContribuable.VAUDOIS_ORDINAIRE;
		}
		else if (assujet instanceof SourcierMixte) {
			type = TypeContribuable.VAUDOIS_ORDINAIRE;
		}
		else if (assujet instanceof SourcierPur) {
			type = null; // un sourcier pur ne devrait pas recevoir de DI...
		}
		else if (assujet instanceof VaudoisDepense) {
			type = TypeContribuable.VAUDOIS_DEPENSE;
		}
		else {
			Assert.isTrue(assujet instanceof VaudoisOrdinaire);
			type = TypeContribuable.VAUDOIS_ORDINAIRE;
		}

		return type;
	}

	/**
	 * @return l'état courant de la DI spécifiée
	 */
	private TypeEtatDocumentFiscal getEtat(DeclarationImpotOrdinaire di) {
		return di.getDernierEtatDeclaration().getEtat();
	}

	private static final String queryDIsPP = // ------------------------------------------------
	"SELECT DISTINCT                                       " // --------------------------------
			+ "    di.id                                   " // --------------------------------
			+ "FROM                                        " // --------------------------------
			+ "    DeclarationImpotOrdinairePP AS di       " // --------------------------------
			+ "WHERE                                       " // --------------------------------
			+ "    di.annulationDate IS null               " // --------------------------------
			+ "    AND di.periode.annulationDate IS null   " // --------------------------------
			+ "    AND di.periode.annee = :annee           " // --------------------------------
			+ "ORDER BY di.id ASC                          ";

	private static final String queryDIsPM = // ------------------------------------------------
	"SELECT DISTINCT                                       " // --------------------------------
			+ "    di.id                                   " // --------------------------------
			+ "FROM                                        " // --------------------------------
			+ "    DeclarationImpotOrdinairePM AS di       " // --------------------------------
			+ "WHERE                                       " // --------------------------------
			+ "    di.annulationDate IS null               " // --------------------------------
			+ "    AND di.periode.annulationDate IS null   " // --------------------------------
			+ "    AND di.periode.annee = :annee           " // --------------------------------
			+ "ORDER BY di.id ASC                          ";

	private List<Long> chargerIdentifiantsDeclarationsPP(int annee) {
		return chargerIdentifiantsDeclarations(annee, queryDIsPP);
	}

	private List<Long> chargerIdentifiantsDeclarationsPM(int annee) {
		return chargerIdentifiantsDeclarations(annee, queryDIsPM);
	}

	/**
	 * Crée un iterateur sur les ID des déclarations ordinaires envoyées pour une période fiscale donnée.
	 *
	 * @param annee la période fiscale considérée
	 * @param queryDIs la query HQL qui récupère les DI qui vont bien
	 * @return itérateur sur les ID des DIs
	 */
	private List<Long> chargerIdentifiantsDeclarations(int annee, String queryDIs) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query queryObject = session.createQuery(queryDIs);
			queryObject.setParameter("annee", annee);
			//noinspection unchecked
			return queryObject.list();
		}));
	}
}
