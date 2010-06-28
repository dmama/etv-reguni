package ch.vd.uniregctb.acomptes;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAOImpl;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersServiceImpl;

public class AcomptesProcessorTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/acomptes/AcomptesProcessorTest.xml";

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private AcomptesProcessor service;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		final TiersService tiersService = getBean(TiersServiceImpl.class, "tiersService");
		final TiersDAO tiersDAO = getBean(TiersDAOImpl.class, "tiersDAO");
		final ServiceCivilService serviceCivilService = getBean(ServiceCivilService.class, "serviceCivilService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		service = new AcomptesProcessor(hibernateTemplate, tiersService, serviceCivilService, transactionManager, tiersDAO);

		// évite de logger plein d'erreurs pendant qu'on teste le comportement du processor
		final Logger serviceLogger = Logger.getLogger(AcomptesServiceImpl.class);
		serviceLogger.setLevel(Level.FATAL);
	}

	@Test
	public void testCreateIteratorOnIDsOfCtbs() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		hibernateTemplate.executeWithNewSession(new HibernateCallback() {
		public Object doInHibernate(Session session) throws HibernateException {
			final Iterator<Long> idIterator = service.createIteratorOnIDsOfCtbs(session, Integer.valueOf(2010));
			assertNotNull(idIterator);
			//12600004 à la source ne fait pas partie de la population pour les bases acomptes
			//12600003 qui a un for fermé en 2010 ne fait pas partie de la population pour les bases acomptes
			//12600001 qui a un for principal à l'étranger avec motif de rattachement 'Diplômate étranger' ne fait pas partie de la population pour les bases acomptes
			//For principal vaudois, mode d'imposition ordinaire
			assertNextCtb(idIterator, Long.valueOf(12600009));
			//For principal vaudois, mode d'imposition à la dépense
			assertNextCtb(idIterator, Long.valueOf(12900001));
			//For principal hors canton, for secondaire immeuble
			assertNextCtb(idIterator, Long.valueOf(34807810));
			//For principal hors suisse, for secondaire 'Activité indépendante'
			assertNextCtb(idIterator, Long.valueOf(86006202));

			return null;
		}
	});
	}

	private void assertNextCtb(final Iterator<Long> iter, Long numeroCtbExpected) {
		assertTrue(iter.hasNext());
		final Long actual = iter.next();
		assertNotNull(actual);
		assertEquals(numeroCtbExpected, actual);
	}


}