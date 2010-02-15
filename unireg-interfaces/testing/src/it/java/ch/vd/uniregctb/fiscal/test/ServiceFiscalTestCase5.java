package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.ContribuableRetourInfoDi;
import ch.vd.registre.fiscal.model.EnumTypeImposition;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe importée du projet host-Interfaces.
 *
 * @author Baba NGOM (xsibnm)
 */
public class ServiceFiscalTestCase5 extends TestCase {

	/** LOGGER */
	private static final java.util.logging.Logger log = java.util.logging.Logger.getAnonymousLogger();

	/** Service fiscal */
	private ServiceFiscal serviceFiscal = null;

	/** Contribuable */
	private static final int ID_CTB = 10024519;

	/** Contribuable */
	private static final short ANNEE_DI = 2007;

	/** Contribuable */
	private static final short NUM_DI = 01;


	/**
	 * Test de récupération d'un contribuable pour modifier ses infos personnelles
	 *
	 * @throws RemoteException
	 * @throws RegistreException
	 */
	public void testModificationCtb() throws RemoteException, RegistreException {
		log.info("testModificationNumTel");

		ContribuableRetourInfoDi contribuableRetourInfoDi = new ContribuableRetourInfoDiImpl();
		contribuableRetourInfoDi.setNoContribuable(ID_CTB);
		contribuableRetourInfoDi.setNoMobile("0511479802");
		contribuableRetourInfoDi.setNoTelephone("0511479802");
		contribuableRetourInfoDi.setEmail("toto@vd.ch");
		contribuableRetourInfoDi.setAnneeFiscale(ANNEE_DI);
		contribuableRetourInfoDi.setNoImpotAnnee(NUM_DI);
		contribuableRetourInfoDi.setTypeImposition(EnumTypeImposition.MANUELLE);
		contribuableRetourInfoDi.setTitulaireCompte("titulaire");
		contribuableRetourInfoDi.setIban("1212121212121");

		serviceFiscal.modifierInformationsPersonnelles(contribuableRetourInfoDi);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
	}

	@Override
	protected void tearDown() throws Exception {
		serviceFiscal.remove();
	}
}