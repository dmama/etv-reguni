package ch.vd.unireg.listes.ear;

import java.util.ArrayList;
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
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class ListeEchangeRenseignementsProcessor extends ListesProcessor<ListeEchangeRenseignementsResults, ListeEchangeRenseignementsThreads> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListeEchangeRenseignementsProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final AssujettissementService assujettissementService;
	private final AdresseService adresseService;

	public ListeEchangeRenseignementsProcessor(HibernateTemplate hibernateTemplate, TiersService tiersService, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                           PlatformTransactionManager transactionManager, TiersDAO tiersDAO, AssujettissementService assujettissementService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.assujettissementService = assujettissementService;
		this.adresseService = adresseService;
	}

	public ListeEchangeRenseignementsResults run(RegDate dateTraitement, final int nbThreads, final int anneeFiscale, final boolean avecContribuablesPP, final boolean avecContribuablesPM, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ListeEchangeRenseignementsResults, ListeEchangeRenseignementsThreads>() {

			@Override
			public Iterator<Long> getIdIterator(Session session) {
				//return new SingletonListIterator<>(10577284L);
				return getIteratorOnCtbs(session, anneeFiscale, avecContribuablesPP, avecContribuablesPM);
			}

			@Override
			public ListeEchangeRenseignementsResults createResults(RegDate dateTraitement) {
				return new ListeEchangeRenseignementsResults(dateTraitement, nbThreads, anneeFiscale, avecContribuablesPP, avecContribuablesPM, tiersService, assujettissementService, adresseService);
			}

			@Override
			public ListeEchangeRenseignementsThreads createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, Interruptible interruptible, AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
				return new ListeEchangeRenseignementsThreads(queue, interruptible, compteur, dateTraitement, nbThreads, anneeFiscale, avecContribuablesPP, avecContribuablesPM, serviceCivilCacheWarmer, tiersService, transactionManager, tiersDAO,
				                                             hibernateTemplate, assujettissementService, adresseService);
			}
		});
	}

	/**
	 * @param session      session hibernate à utiliser
	 * @param anneeFiscale période fiscale
	 * @return un itérateur sur tous les contribuables avec un for vaudois actif sur au moins une partie de la période fiscale donnée
	 */
	@SuppressWarnings({"unchecked"})
	private Iterator<Long> getIteratorOnCtbs(Session session, int anneeFiscale, final boolean avecContribuablesPP, final boolean avecContribuablesPM) {

		final String ppPart = "PersonnePhysique, MenageCommun";
		final String pmPart = "Entreprise";

		final List<String> type = new ArrayList<>(2);
		if (avecContribuablesPP) {
			type.add(ppPart);
		}
		if (avecContribuablesPM) {
			type.add(pmPart);
		}


		final StringBuilder sb = new StringBuilder();
		final Iterator<String> wherePartIterator = type.iterator();
		while (wherePartIterator.hasNext()) {
			final String part = wherePartIterator.next();
			sb.append(part);
			if (wherePartIterator.hasNext()) {
				sb.append(", ");
			}
		}

		final String inPart = sb.toString();

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT ctb.id FROM Contribuable AS ctb");
		b.append(" INNER JOIN ctb.forsFiscaux AS fors");
		b.append(" WHERE ctb.annulationDate IS NULL");
		b.append(String.format(" AND ctb.class IN (%s)", inPart));
		b.append(" AND fors.annulationDate IS NULL");
		b.append(" AND fors.class IN (ForFiscalPrincipalPP,ForFiscalPrincipalPM)");  // SIFISC-29785
		b.append(" AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND fors.dateDebut <= :finAnnee");
		b.append(" AND (fors.dateFin IS NULL OR fors.dateFin >= :debutAnnee)");
		b.append(" ORDER BY ctb.id ASC");
		final String hql = b.toString();

		final Query query = session.createQuery(hql);
		query.setParameter("debutAnnee", RegDate.get(anneeFiscale, 1, 1));
		query.setParameter("finAnnee", RegDate.get(anneeFiscale, 12, 31));
		return query.iterate();
	}
}
