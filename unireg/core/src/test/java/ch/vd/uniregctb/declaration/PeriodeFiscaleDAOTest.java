package ch.vd.uniregctb.declaration;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class PeriodeFiscaleDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(PeriodeFiscaleDAOTest.class);

	private static final String DAO_NAME = "periodeFiscaleDAO";

	private static final String DB_UNIT_DATA_FILE = "PeriodeFiscaleDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private PeriodeFiscaleDAO periodeFiscaleDAO;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, DAO_NAME);
	}

	/**
	 * Teste la methode qui recherche les LRs suivant certains criteres
	 */
	@Test
	public void testGetAllDesc() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		List<PeriodeFiscale> periodes = periodeFiscaleDAO.getAllDesc();
		assertEquals(3, periodes.size());
		Integer anneeAttendue = new Integer(2007);
		Integer annee = periodes.get(0).getAnnee();
		assertEquals(anneeAttendue, annee);
		
	}

	/**
	 * Vérifie que la méthode 'getPeriodeFiscaleByYear' ne déclenche pas l'intercepteur de validation des tiers
	 */
	@Test
	public void testGetPeriodeFiscaleByYearDoesntFlushSession() throws Exception {

		// Crée la période fiscale 2008 dans sa propre transaction pour initialiser la base de données
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale periode = new PeriodeFiscale();
				periode.setAnnee(2008);
				periodeFiscaleDAO.save(periode);
				return null;
			}
		});

		// Crée un habitant qui ne valide pas
		PersonnePhysique tiers = new PersonnePhysique(false);
		tiers.setNom("RRR");
		tiers = (PersonnePhysique) periodeFiscaleDAO.getHibernateTemplate().merge(tiers);
		tiers.setNom(null); // le nom est obligatoire
		assertTrue(tiers.validate().hasErrors());

		// On doit être capable de récupérer la période fiscale sans déclencher la validation du tiers ci-dessus
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(2008);
		assertNotNull(periode);
		assertEquals(Integer.valueOf(2008), periode.getAnnee());
	}

	public PeriodeFiscaleDAO getPeriodeFiscaleDAO() {
		return periodeFiscaleDAO;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

}
