package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesCtbs.TypeContribuable;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixte;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Produit des statistiques sur les contribuables assujettis pour la période fiscale spécifiée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ProduireStatsCtbsProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ProduireStatsCtbsProcessor.class);

	private final HibernateTemplate hibernateTemplate;

	private final ServiceInfrastructureService infraService;

	private final TiersService tiersService;

	private final PlatformTransactionManager transactionManager;

	public ProduireStatsCtbsProcessor(HibernateTemplate hibernateTemplate, ServiceInfrastructureService infraService, TiersService tiersService, PlatformTransactionManager transactionManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
	}

	public StatistiquesCtbs run(final int anneePeriode, final RegDate dateTraitement, StatusManager statusManager) throws DeclarationException {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);

		final StatistiquesCtbs rapportFinal = new StatistiquesCtbs(anneePeriode, dateTraitement);

		status.setMessage(String.format("Début de la production des statistiques des contribuables assujettis : période fiscale = %d.", anneePeriode));

		final List<Long> listeComplete = chargerIdentifiantsContribuables(anneePeriode);
		final BatchTransactionTemplate<Long, StatistiquesCtbs> template = new BatchTransactionTemplate<Long, StatistiquesCtbs>(listeComplete, BATCH_SIZE, Behavior.SANS_REPRISE, transactionManager, status, hibernateTemplate);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchCallback<Long, StatistiquesCtbs>() {

			@Override
			public StatistiquesCtbs createSubRapport() {
				return new StatistiquesCtbs(anneePeriode, dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, StatistiquesCtbs rapport) throws Exception {
				traiteBatch(batch, rapport, status, listeComplete.size(), rapportFinal.nbCtbsTotal);
				return true;
			}
		});

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite tous les contribuables spécifiés par l'iterateur passé en paramètre.
	 */
	private void traiteBatch(final List<Long> batch, final StatistiquesCtbs rapport, final StatusManager status, final int nbTotalContribuables, final int nbCtbTraites) {

		hibernateTemplate.getSessionFactory().getCurrentSession().setFlushMode(FlushMode.MANUAL); // on ne va rien changer

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
	}

	/**
	 * Traite le contribuable dont l'id est passé en paramètre
	 */
	private void traiterCtb(Long id, final StatistiquesCtbs rapport) {

		Contribuable ctb = null;
		try {
			ctb = hibernateTemplate.get(Contribuable.class, id);

			final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, rapport.annee);
			if (assujettissements == null || assujettissements.isEmpty()) {
				// le contribuable n'est pas assujetti -> c'est possible car la requête SQL ne peut pas effectuer ce filtrage en amont
				return;
			}

			// Dans tous les cas, on prend l'assujettissement le plus récent
			final Assujettissement assujet = assujettissements.get(assujettissements.size() - 1);
			final TypeContribuable typeCtb = determineType(assujet);
			final Commune commune = typeCtb == TypeContribuable.SOURCIER_PUR ? getCommuneDepuisFor(ctb, rapport.annee) : getCommuneGestion(ctb, rapport.annee);
			final Integer oid = getOID(commune);

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

		int noOfsCommune = commune.getNoOFSEtendu();
		OfficeImpot office = infraService.getOfficeImpotDeCommune(noOfsCommune);
		if (office == null) {
			return null;
		}

		int oid = office.getNoColAdm();
		return oid;
	}

	/**
	 * @return la commune du for de gestion du contribuable spécifié, ou <b>null</b> si le contribuable ne possède pas de for de gestion.
	 * @throws ServiceInfrastructureException
	 */
	private Commune getCommuneGestion(Contribuable ctb, int annee) throws ServiceInfrastructureException {
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, RegDate.get(annee, 12, 31));
		Commune commune = null;
		if (forGestion != null) {
			commune = infraService.getCommuneByNumeroOfsEtendu(forGestion.getNoOfsCommune(), forGestion.getDateFin());
		}
		return commune;
	}

	private Commune getCommuneDepuisFor(Contribuable ctb, int annee) throws ServiceInfrastructureException {
		final ForFiscalPrincipal ffp = ctb.getDernierForFiscalPrincipalVaudoisAvant(RegDate.get(annee, 12, 31));
		Commune commune = null;
		if (ffp != null) {
			commune = infraService.getCommuneByNumeroOfsEtendu(ffp.getNumeroOfsAutoriteFiscale(), ffp.getDateFin());
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
	private TypeContribuable determineType(Assujettissement assujet) {

		final TypeContribuable type;
		if (assujet instanceof DiplomateSuisse) {
			type = TypeContribuable.VAUDOIS_ORDINAIRE;
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
				type = TypeContribuable.VAUDOIS_ORDINAIRE;
				break;
			case COMMUNE_HC:
				type = TypeContribuable.HORS_CANTON;
				break;
			case PAYS_HS:
				type = TypeContribuable.HORS_SUISSE;
				break;
			default:
				throw new IllegalArgumentException("Type d'autorité fiscale inconnue [" + dernierFor.getTypeAutoriteFiscale() + ']');
			}
		}
		else if (assujet instanceof SourcierPur) {
			type = TypeContribuable.SOURCIER_PUR;
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

	final private static String queryCtbs = // --------------------------------------------------
	"SELECT DISTINCT                                                                         "
			+ "    cont.id                                                                   "
			+ "FROM                                                                          "
			+ "    Contribuable AS cont                                                      "
			+ "INNER JOIN                                                                    "
			+ "    cont.forsFiscaux AS fors                                                  "
			+ "WHERE                                                                         "
			+ "    cont.annulationDate IS null                                               "
			+ "    AND fors.annulationDate IS null                                           "
			+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               " // = au moins 1 for actif dans l'année
			+ "    AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)                 "
			+ "ORDER BY cont.id ASC                                                          ";

	/**
	 * Crée un iterateur sur les ids des contribuables ayant au moins un for fiscal ouvert sur la période fiscale spécifiée.
	 *
	 * @param annee
	 *            la période fiscale considérée
	 * @return itérateur sur les ids des contribuables trouvés
	 */
	protected List<Long> chargerIdentifiantsContribuables(final int annee) {

		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> i = hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryCtbs);
						queryObject.setParameter("debutAnnee", debutAnnee.index());
						queryObject.setParameter("finAnnee", finAnnee.index());
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return i;
			}
		});
	}
}
