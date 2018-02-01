package ch.vd.unireg.listes.suisseoupermiscresident;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.ListesProcessor;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class ListeContribuablesResidentsSansForVaudoisProcessor extends ListesProcessor<ListeContribuablesResidentsSansForVaudoisResults, ListeContribuablesResidentsSansForVaudoisThread> {

	public static final Logger LOGGER = LoggerFactory.getLogger(ListeContribuablesResidentsSansForVaudoisProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final ServiceInfrastructureService infraService;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	public ListeContribuablesResidentsSansForVaudoisProcessor(HibernateTemplate hibernateTemplate, TiersService tiersService, AdresseService adresseService,
	                                                          PlatformTransactionManager transactionManager, TiersDAO tiersDAO, ServiceInfrastructureService infraService,
	                                                          ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.infraService = infraService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	@Override
	protected String getDenominationContribuablesComptes() {
		return "contribuables potentiellement concernés";
	}

	public ListeContribuablesResidentsSansForVaudoisResults run(RegDate dateTraitement, final int nbThreads, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// lance le vrai boulot !
		return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ListeContribuablesResidentsSansForVaudoisResults, ListeContribuablesResidentsSansForVaudoisThread>() {

		    @Override
		    public ListeContribuablesResidentsSansForVaudoisResults createResults(RegDate dateTraitement) {
		        return new ListeContribuablesResidentsSansForVaudoisResults(dateTraitement, nbThreads, tiersService, adresseService);
		    }

		    @Override
		    public ListeContribuablesResidentsSansForVaudoisThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, Interruptible interruptible,
		                                                                        AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
		        return new ListeContribuablesResidentsSansForVaudoisThread(queue, dateTraitement, nbThreads, interruptible, compteur, transactionManager, hibernateTemplate, tiersDAO, tiersService, adresseService, infraService, serviceCivilCacheWarmer);
		    }

		    @Override
		    public Iterator<Long> getIdIterator(Session session) {
		        return createIterator(session);
		    }
		});
	}

	@SuppressWarnings({"unchecked"})
	private Iterator<Long> createIterator(Session session) {

		// on recherche tous les candidats potentiels : tous les contribuables couple ou personnes physiques qui ne sont pas en couple
		// et qui n'ont pas de for vaudois ouvert

		final String hql = "select ctb.id from ContribuableImpositionPersonnesPhysiques as ctb"
				+ " where not exists (select ff.id from ForFiscalPrincipalPP as ff where ff.tiers = ctb and ff.annulationDate is null and ff.dateFin is null and ff.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD')"
				+ " and not exists (select r.id from AppartenanceMenage as r where r.sujetId = ctb.id and r.annulationDate is null and r.dateFin is null)"
				+ " and ctb.annulationDate is null"
				+ " order by ctb.id asc";

		final Query query = session.createQuery(hql);
		return query.iterate();
	}
}
