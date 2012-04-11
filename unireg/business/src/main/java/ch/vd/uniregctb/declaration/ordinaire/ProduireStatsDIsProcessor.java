package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Iterator;
import java.util.List;

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

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
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
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ProduireStatsDIsProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ProduireStatsDIsProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final ServiceInfrastructureService infraService;
	private final PlatformTransactionManager transactionManager;
	private final DeclarationImpotOrdinaireDAO diDAO;
	private final AssujettissementService assujettissementService;

	public ProduireStatsDIsProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService,
	                                 PlatformTransactionManager transactionManager, DeclarationImpotOrdinaireDAO diDAO, AssujettissementService assujettissementService) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.transactionManager = transactionManager;
		this.diDAO = diDAO;
		this.assujettissementService = assujettissementService;
	}

	public StatistiquesDIs run(final int anneePeriode, final RegDate dateTraitement, StatusManager statusManager) throws DeclarationException {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);
		final StatistiquesDIs rapportFinal = new StatistiquesDIs(anneePeriode, dateTraitement);

		status.setMessage(String.format("Début de la production des statistiques des déclaration d'impôts ordinaires : période fiscale = %d.", anneePeriode));

		final List<Long> listeComplete = chargerIdentifiantsDeclarations(anneePeriode);
		final BatchTransactionTemplate<Long, StatistiquesDIs> template = new BatchTransactionTemplate<Long, StatistiquesDIs>(listeComplete, BATCH_SIZE, Behavior.SANS_REPRISE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, StatistiquesDIs>() {

			@Override
			public StatistiquesDIs createSubRapport() {
				return new StatistiquesDIs(anneePeriode, dateTraitement);
			}

			@Override
			public boolean doInTransaction(final List<Long> batch, StatistiquesDIs rapport) throws Exception {

				final Iterator<Long> iter = batch.iterator();
				while (iter.hasNext() && !status.interrupted()) {

					final Long id = iter.next();

					++ rapportFinal.nbDIsTotal;
					if (rapportFinal.nbDIsTotal % 100 == 0) {
						status.setMessage(String.format("Traitement de la DI n°%d (%d/%d)", id, rapportFinal.nbDIsTotal, listeComplete.size()), percent);
					}

					final DeclarationImpotOrdinaire di = diDAO.get(id);
					try {
						if (di != null) {
							traiterDI(di, rapport);
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
		});

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Ajoute les statistiques de la DI et des détails spécifiés au rapport.
	 */
	private void traiterDI(DeclarationImpotOrdinaire di, StatistiquesDIs rapport) throws Exception {
		final int oid = getOID(di);
		final TypeContribuable type = getType(di);
		final TypeEtatDeclaration etat = getEtat(di);

		rapport.addStats(oid, type, etat);
	}

	/**
	 * @return l'id de l'office d'impôt responsable de la DI spécifiée.
	 */
	private int getOID(DeclarationImpotOrdinaire di) throws ServiceInfrastructureException {
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

		final Contribuable contribuable = (Contribuable) di.getTiers();
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
	private TypeEtatDeclaration getEtat(DeclarationImpotOrdinaire di) {
		TypeEtatDeclaration etat = di.getDernierEtat().getEtat();
		return etat;
	}

	final private static String queryDIs = // --------------------------------------------------
	"SELECT DISTINCT                                       " // --------------------------------
			+ "    di.id                                   " // --------------------------------
			+ "FROM                                        " // --------------------------------
			+ "    DeclarationImpotOrdinaire AS di         " // --------------------------------
			+ "WHERE                                       " // --------------------------------
			+ "    di.annulationDate IS null               " // --------------------------------
			+ "    AND di.periode.annulationDate IS null   " // --------------------------------
			+ "    AND di.periode.annee = :annee           " // --------------------------------
			+ "ORDER BY di.id ASC                          ";

	/**
	 * Crée un iterateur sur les ID des déclarations ordinaires envoyées pour une période fiscale donnée.
	 *
	 * @param annee
	 *            la période fiscale considérée
	 * @return itérateur sur les ID des DIs
	 */
	protected List<Long> chargerIdentifiantsDeclarations(final int annee) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> i = hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryDIs);
						queryObject.setParameter("annee", annee);
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return i;
			}
		});
	}
}
