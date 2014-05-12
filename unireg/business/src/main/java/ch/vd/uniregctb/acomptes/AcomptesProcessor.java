package ch.vd.uniregctb.acomptes;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesProcessor;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class AcomptesProcessor extends ListesProcessor<AcomptesResults, AcomptesThread> {

	private final Logger LOGGER = Logger.getLogger(AcomptesProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final AssujettissementService assujettissementService;
	private final AdresseService adresseService;

	public AcomptesProcessor(HibernateTemplate hibernateTemplate,
	                         TiersService tiersService,
	                         ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                         PlatformTransactionManager transactionManager,
	                         TiersDAO tiersDAO,
	                         AssujettissementService assujettissementService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.assujettissementService = assujettissementService;
		this.adresseService = adresseService;
	}

	/**
	 * Appel principal de génération des populations pour les bases acomptes
	 */
	public AcomptesResults run(RegDate dateTraitement, final int nbThreads, final Integer annee, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// lance le vrai boulot!
		return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<AcomptesResults, AcomptesThread>() {

			@Override
			public AcomptesResults createResults(RegDate dateTraitement) {
				return new AcomptesResults(dateTraitement, nbThreads, annee, tiersService, assujettissementService, adresseService);
			}

			@Override
			public AcomptesThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status, AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
				return new AcomptesThread(queue, dateTraitement, nbThreads, annee, serviceCivilCacheWarmer, tiersService,
						status, compteur, transactionManager, tiersDAO, hibernateTemplate, assujettissementService, adresseService);
			}

			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return createIteratorOnIDsOfCtbs(session, annee);
			}

		});
	}

	@SuppressWarnings("unchecked")
	protected Iterator<Long> createIteratorOnIDsOfCtbs(Session session, Integer annee) {
		final RegDate debutAnnee = RegDate.get(annee - 1, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);
		final String queryString = "SELECT DISTINCT                                                                     "
									+ "    cont.id                                                                     	"
									+ "FROM                                                                          	"
									+ "    Contribuable AS cont                                                      	"
									+ "INNER JOIN                                                                    	"
									+ "    cont.forsFiscaux AS fors                                                  	"
									+ "WHERE                                                                         	"
									+ "    cont.annulationDate IS null                                               	"
									+ "    AND fors.annulationDate IS null                                           	"
									+ "    AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'                   	"
									+ "    AND (fors.class = ForFiscalPrincipal OR fors.class = ForFiscalSecondaire) 	"
									+ "    AND (fors.modeImposition IS null OR fors.modeImposition IN ('ORDINAIRE', 'INDIGENT', 'DEPENSE', 'MIXTE_137_1')) "
									+ "    AND fors.motifRattachement != 'DIPLOMATE_ETRANGER'							"
									+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               	"
									+ "    AND                                                                       	"
									+ "        (                                                                     	"
									+ "            (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)               	" // = for actif n'importe quand dans l'année
									+ "        OR                                                                    	"
									+ "            (fors.class = ForFiscalSecondaire                                 	"
									+ "             AND (fors.motifRattachement = 'IMMEUBLE_PRIVE'                    	"
									+ "					OR fors.motifRattachement = 'ACTIVITE_INDEPENDANTE')			"
									+ "             AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee))       	" // = for actif n'importe quand dans l'année
									+ "        )                                                                     	"
									+ "ORDER BY cont.id ASC                                                          	";
		final Query query = session.createQuery(queryString);
		query.setParameter("debutAnnee", debutAnnee);
		query.setParameter("finAnnee", finAnnee);
		return query.iterate();
	}

}
