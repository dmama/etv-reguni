package ch.vd.uniregctb.interfaces.fiscal;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.common.AbstractCoreDAOTest;
import ch.vd.uniregctb.common.ClientConstants;
import ch.vd.uniregctb.dbutils.SqlFileExecutor;
import ch.vd.uniregctb.interfaces.migreg.Migrator;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

@ContextConfiguration(locations = {
		"classpath:ut/uintf-ejbit-interfaces.xml",
		"classpath:ut/uintf-ejbit-beans.xml",
		"classpath:ut/uintf-ejbit-database.xml",
		"classpath:ut/uintf-ejbit-datasource.xml",
		"classpath:ut/uintf-ejbit-services.xml",
		"classpath:unireg-business-services.xml",
		"classpath:unireg-business-interfaces.xml",
		ClientConstants.UNIREG_BUSINESS_MIGREG,
		ClientConstants.UNIREG_BUSINESS_APIREG


})
public abstract class AbstractServiceFiscalTest extends AbstractCoreDAOTest {
	//private static final Logger LOGGER = Logger.getLogger(AbstractServiceFiscalTest.class);
	private static final String CORE_TRUNCATE_SQL = "/sql/core_truncate_tables.sql";

	protected TiersService tiersService;
	/** Service civil. */
	protected ProxyServiceCivil serviceCivil;
	protected TiersDAO tiersDAO;
	/** Service fiscal. */
	protected static ch.vd.uniregctb.fiscal.service.ServiceFiscal serviceFiscalUnireg;
	protected static ch.vd.registre.fiscal.service.ServiceFiscal serviceFiscalHost;
	protected HibernateTemplate hibernateTemplate;
	protected Migrator migrator;

	/**
	 * Methode d'initialisation du context spring
	 * @throws Exception
	 */


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		serviceFiscalUnireg = getBean(ch.vd.uniregctb.fiscal.service.ServiceFiscal.class, "serviceFiscalUnireg");
		serviceFiscalHost = getBean(ch.vd.registre.fiscal.service.ServiceFiscal.class, "serviceFiscalHost");
		migrator = getBean(ch.vd.uniregctb.interfaces.migreg.Migrator.class, "migrator");

	}


	@Override
	public void onTearDown() throws Exception {
		if (serviceFiscalUnireg != null) {
			serviceFiscalUnireg.remove();
		}
		if (serviceFiscalHost != null) {
			serviceFiscalHost.remove();
		}

		super.onTearDown();



	}

	@Override
	protected void truncateDatabase() throws Exception {

		SqlFileExecutor.execute(transactionManager, dataSource, CORE_TRUNCATE_SQL);

	}
}
