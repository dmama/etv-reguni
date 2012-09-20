package ch.vd.uniregctb.listes.suisseoupermiscresident;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesProcessor;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class ListeContribuablesResidentsSansForVaudoisProcessor extends ListesProcessor<ListeContribuablesResidentsSansForVaudoisResults, ListeContribuablesResidentsSansForVaudoisThread> {

	public static final Logger LOGGER = Logger.getLogger(ListeContribuablesResidentsSansForVaudoisProcessor.class);

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
		        return new ListeContribuablesResidentsSansForVaudoisResults(dateTraitement, nbThreads, tiersService);
		    }

		    @Override
		    public ListeContribuablesResidentsSansForVaudoisThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status,
		                                               AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
		        return new ListeContribuablesResidentsSansForVaudoisThread(queue, dateTraitement, nbThreads, status, compteur, transactionManager, hibernateTemplate, tiersDAO, tiersService, adresseService, infraService, serviceCivilCacheWarmer);
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

		final String hql = "select ctb.id from Contribuable as ctb where ctb.class in (MenageCommun, PersonnePhysique)"
				+ " and not exists (select ff.id from ForFiscalPrincipal as ff where ff.tiers = ctb and ff.annulationDate is null and ff.dateFin is null and ff.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD')"
				+ " and not exists (select r.id from AppartenanceMenage as r where r.sujetId = ctb.id and r.annulationDate is null and r.dateFin is null)"
				+ " and ctb.annulationDate is null"
				+ " order by ctb.id asc";

		final Query query = session.createQuery(hql);
		return query.iterate();
	}
}
