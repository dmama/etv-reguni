package ch.vd.uniregctb.dao.jdbc;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AbstractSpringTest;
import ch.vd.uniregctb.common.CoreTestingConstants;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.HibernateEntityUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Ce test utilise la base de données d'intégration pour vérifier que les daos Jdbc et Hibernate retournent bien les mêmes données.
 */
@ContextConfiguration(locations = {
		CoreTestingConstants.UNIREG_CORE_DAO,
		CoreTestingConstants.UNIREG_CORE_SF,
		"classpath:ch/vd/uniregctb/dao/jdbc/jdbctiersdaotestapp-datasource.xml", // <-- version spéciale qui se connecte à la base de données d'intégration
		CoreTestingConstants.UNIREG_CORE_UT_PROPERTIES
})
@Ignore(value = "Schéma de base incompatible en intégration")
public class JdbcTiersDaoItTest extends AbstractSpringTest {

	private static final Logger LOGGER = Logger.getLogger(JdbcTiersDaoItTest.class);

	private TiersDAO tiersDAO;
	private JdbcTiersDaoImpl jdbcTiersDao;

	// liste de tiers ayant posé des problèmes. A mettre-à-jour lors de chaque problème, pour augmenter le coverage
	private final List<Long> fixedIds = Arrays.asList(10642250L, 10326285L, 10662878L, 1293973L, 44716602L);

	public JdbcTiersDaoItTest() {

		// Le run pour de vrai
		boolean fileNotFound = true;
		{
			File file = new File("log4j.xml");
			if (file.exists()) {
				DOMConfigurator.configure("log4j.xml");
				fileNotFound = false;
			}
		}
		// Dans Eclipse
		if (fileNotFound) {
			File file = new File("src/test/resources/ut/log4j.xml");
			if (file.exists()) {
				DOMConfigurator.configure("src/test/resources/ut/log4j.xml");
			}
			else {
				// Dans IDEA
				file = new File("business-it/src/test/resources/ut/log4j.xml");
				if (file.exists()) {
					DOMConfigurator.configure("business-it/src/test/resources/ut/log4j.xml");
				}
				else {
					Assert.fail("Pas de fichier Log4j");
				}
			}
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final DataSource dataSource = getBean(DataSource.class, "dataSource");
		jdbcTiersDao = new JdbcTiersDaoImpl();
		jdbcTiersDao.setDataSource(dataSource);
	}

	/**
	 * Compare les résultats retournés par les méthodes 'get' des daos Jdbc et Hibernate
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetTiers() {

		LOGGER.info("Liste des ids fixes utilisés pour la comparaison = " + Arrays.toString(fixedIds.toArray()));
		compareGetTiers(fixedIds);

		for (int j = 0; j < 10; j++) {
			final List<Long> randomIds = new ArrayList<Long>();

			for (int i = 0; i < 499; i++) {
				long id = (long) (100000000 * Math.random());
				randomIds.add(id);
			}

			LOGGER.info("Liste des ids random utilisés pour la comparaison = " + Arrays.toString(randomIds.toArray()));
			compareGetTiers(randomIds);
		}
	}

	/**
	 * Compare les résultats retournés par les méthodes 'getBatch' des daos Jdbc et Hibernate
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetTiersBatch() {

		LOGGER.info("Liste des ids fixes utilisés pour la comparaison = " + Arrays.toString(fixedIds.toArray()));
		compareGetTiersBatch(fixedIds);

		for (int j = 0; j < 10; j++) {
			final List<Long> randomIds = new ArrayList<Long>();

			for (int i = 0; i < 499; i++) {
				long id = (long) (100000000 * Math.random());
				randomIds.add(id);
			}

			LOGGER.info("Liste des ids random utilisés pour la comparaison = " + Arrays.toString(randomIds.toArray()));
			compareGetTiersBatch(randomIds);
		}
	}

	@SuppressWarnings({"unchecked"})
	private void compareGetTiers(final List<Long> ids) {

		final Set<TiersDAO.Parts> parts = new HashSet<TiersDAO.Parts>();
		parts.addAll(Arrays.asList(TiersDAO.Parts.values()));

		final List<Tiers> jdbcTiersList = getTiersJdbc(ids, parts);
		final List<Tiers> hibernateTiersList = getTiersHibernate(ids);

		assertEquals(hibernateTiersList.size(), jdbcTiersList.size());
		Collections.sort(hibernateTiersList, new TiersIdComparator());
		Collections.sort(jdbcTiersList, new TiersIdComparator());

		for (int i = 0, hibernateTiersListSize = hibernateTiersList.size(); i < hibernateTiersListSize; i++) {
			final Tiers hibernateTiers = hibernateTiersList.get(i);
			final Tiers jdbcTiers = jdbcTiersList.get(i);

			assertTiersEquals(hibernateTiers, jdbcTiers);
		}
	}

	private List<Tiers> getTiersJdbc(final List<Long> ids, final Set<TiersDAO.Parts> parts) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback<List<Tiers>>() {
			@Override
			public List<Tiers> doInTransaction(TransactionStatus status) {
				status.setRollbackOnly(); // on ne veut pas modifier la base
				final List<Tiers> list = new ArrayList<Tiers>(ids.size());
				for (Long id : ids) {
					final Tiers t = jdbcTiersDao.get(id, parts);
					if (t != null) {
						list.add(t);
					}
				}
				return list;
			}
		});
	}

	private List<Tiers> getTiersHibernate(final List<Long> ids) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback<List<Tiers>>() {
			@Override
			public List<Tiers> doInTransaction(TransactionStatus status) {
				status.setRollbackOnly(); // on ne veut pas modifier la base
				return tiersDAO.getHibernateTemplate().execute(new HibernateCallback<List<Tiers>>() {
					@Override
					public List<Tiers> doInHibernate(Session session) throws HibernateException, SQLException {
						session.setFlushMode(FlushMode.MANUAL);
						final List<Tiers> list = new ArrayList<Tiers>(ids.size());
						for (Long id : ids) {
							final Tiers t = tiersDAO.get(id);
							if (t != null) {
								list.add(t);
							}
						}
						for (Tiers t : list) {
							try {
								HibernateEntityUtils.forceInitializationOfCollections(t);
							}
							catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
						return list;
					}
				});
			}
		});
	}

	private void compareGetTiersBatch(final List<Long> ids) {

		final Set<TiersDAO.Parts> parts = new HashSet<TiersDAO.Parts>();
		parts.addAll(Arrays.asList(TiersDAO.Parts.values()));

		final List<Tiers> jdbcTiersList = getTiersBatchJdbc(ids, parts);
		final List<Tiers> hibernateTiersList = getTiersBatchHibernate(ids, parts);

		assertEquals(hibernateTiersList.size(), jdbcTiersList.size());
		Collections.sort(hibernateTiersList, new TiersIdComparator());
		Collections.sort(jdbcTiersList, new TiersIdComparator());

		for (int i = 0, hibernateTiersListSize = hibernateTiersList.size(); i < hibernateTiersListSize; i++) {
			final Tiers hibernateTiers = hibernateTiersList.get(i);
			final Tiers jdbcTiers = jdbcTiersList.get(i);

			assertTiersEquals(hibernateTiers, jdbcTiers);
		}

	}

	private List<Tiers> getTiersBatchJdbc(final List<Long> ids, final Set<TiersDAO.Parts> parts) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback<List<Tiers>>() {
			@Override
			public List<Tiers> doInTransaction(TransactionStatus status) {
				status.setRollbackOnly(); // on ne veut pas modifier la base
				return jdbcTiersDao.getBatch(ids, parts);
			}
		});
	}

	private List<Tiers> getTiersBatchHibernate(final List<Long> ids, final Set<TiersDAO.Parts> parts) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TransactionCallback<List<Tiers>>() {
			@Override
			public List<Tiers> doInTransaction(TransactionStatus status) {
				status.setRollbackOnly(); // on ne veut pas modifier la base
				return tiersDAO.getHibernateTemplate().execute(new HibernateCallback<List<Tiers>>() {
					@Override
					public List<Tiers> doInHibernate(Session session) throws HibernateException, SQLException {
						session.setFlushMode(FlushMode.MANUAL);
						final List<Tiers> list = tiersDAO.getBatch(ids, parts);
						for (Tiers t : list) {
							try {
								HibernateEntityUtils.forceInitializationOfCollections(t);
							}
							catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
						return list;
					}
				});
			}
		});
	}

	private void assertTiersEquals(Tiers hibernateTiers, Tiers jdbcTiers) {

		assertNotNull(hibernateTiers);
		assertNotNull(jdbcTiers);
		assertEquals(hibernateTiers.getId(), jdbcTiers.getId());
		final Long id = hibernateTiers.getId();

		if (hibernateTiers instanceof Contribuable) {
			((Contribuable) hibernateTiers).setMouvementsDossier(Collections.<MouvementDossier>emptySet()); // on ne s'intéresse pas à cette collection
			if (hibernateTiers instanceof PersonnePhysique) {
				((PersonnePhysique) hibernateTiers).setDroitsAccesAppliques(Collections.<DroitAcces>emptySet()); // on ne s'intéresse pas à cette collection
			}
		}
		for (Declaration d : hibernateTiers.getDeclarations()) {
			d.setDelais(null); // on ne s'intéresse pas à cette collection
		}

		if (!areRecursiveEquals(hibernateTiers, jdbcTiers)) {
			String message = "Les données récupérées par le dao Jdbc et le dao Hibernate pour le tiers n°" + id + " ne correspondent pas :\n";
			try {
				message += " - hibernate:\n" + ToStringBuilder.reflectionToString(hibernateTiers) + '\n';
				message += " - jdbc:\n" + ToStringBuilder.reflectionToString(jdbcTiers) + "\n\n";
			}
			catch (Exception e) {
				message += "(impossible d'afficher la liste des propriétés pour la raison suivante : " + e.getMessage() + ")\n";
			}
			fail(message);
		}
		else {
			LOGGER.info("Tiers n°" + id + " OK");
		}
	}

	@SuppressWarnings({"unchecked"})
	private static boolean areRecursiveEquals(HibernateEntity expected, HibernateEntity actual) {
		try {
			HibernateEntityUtils.assertEntityEquals(expected, actual);
			return true;
		}
		catch (Exception e) {
			LOGGER.error("Error message = "+e.getMessage());
			return false;
		}
	}

	private static class TiersIdComparator implements Comparator<Tiers> {
		@Override
		public int compare(Tiers o1, Tiers o2) {
			return o1.getId().compareTo(o2.getId());
		}
	}
}
