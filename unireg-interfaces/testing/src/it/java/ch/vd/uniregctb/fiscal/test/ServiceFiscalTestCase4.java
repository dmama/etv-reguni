package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.registre.common.model.CoordonneesFinancieres;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe importée du projet host-Interfaces.
 *
 * @author Baba NGOM (xsibnm)
 */
public class ServiceFiscalTestCase4 extends TestCase {

	/** LOGGER */
	private static final java.util.logging.Logger log = java.util.logging.Logger.getAnonymousLogger();

	/** Service fiscal */
	private ServiceFiscal serviceFiscal = null;

	/** Contribuable */
	private static final int ID_CTB_CCP = 73803501;

	/** Contribuable avec IBAN */
	private static final int ID_CTB_IBAN = 30101201;

	/** Contribuable avec IBAN + BIC */
	private static final int ID_CTB_IBAN_BIC = 10020082;

	/** Contribuable avec compte bancaire */
	private static final int ID_CTB_BANCAIRE = 10020056;

	/** Contribuable avec compte bancaire */

	/**
	 * Test de récupération des coordonnées financières du contribuable ayant un IBAN suisse.
	 *
	 * @throws RemoteException
	 * @throws RegistreException
	 */
	public void testRecupCtbIBANSansBIC() throws RemoteException, RegistreException {
		log.info("testRecupCtbIBANSansBIC");
		CoordonneesFinancieres coordonneesFinancieres = serviceFiscal.getCoordonneesFinancieres(ID_CTB_IBAN);
		assertNotNull(coordonneesFinancieres);
		assertTrue(coordonneesFinancieres.isIBAN());
		assertNotNull(coordonneesFinancieres.getNoIBAN());
		assertNull(coordonneesFinancieres.getNoBIC());
	}

	/**
	 * Test de récupération des coordonnées financières du contribuable ayant un IBAN étranger.
	 *
	 * @throws RemoteException
	 * @throws RegistreException
	 */
	public void testRecupCtbIBANAvecBIC() throws RemoteException, RegistreException {
		CoordonneesFinancieres coordonneesFinancieres = serviceFiscal.getCoordonneesFinancieres(ID_CTB_IBAN_BIC);
		assertNotNull(coordonneesFinancieres);
		assertTrue(coordonneesFinancieres.isIBAN());
		assertNotNull(coordonneesFinancieres.getNoIBAN());
		assertNotNull(coordonneesFinancieres.getNoBIC());
	}

	/**
	 * Test de récupération des coordonnées financières du contribuable ayant un CCP.
	 *
	 * @throws RemoteException
	 * @throws RegistreException
	 */
	public void testRecupCtbCCP() throws RemoteException, RegistreException {
		CoordonneesFinancieres coordonneesFinancieres = serviceFiscal.getCoordonneesFinancieres(ID_CTB_CCP);
		assertNotNull(coordonneesFinancieres);
		assertTrue(coordonneesFinancieres.isComptePostal());
		assertNotNull(coordonneesFinancieres.getNoCompte());
	}

	/**
	 * Test de récupération des coordonnées financières du contribuable ayant un CCP.
	 *
	 * @throws RemoteException
	 * @throws RegistreException
	 */
	public void testRecupCtbBancaire() throws RemoteException, RegistreException {
		CoordonneesFinancieres coordonneesFinancieres = serviceFiscal.getCoordonneesFinancieres(ID_CTB_BANCAIRE);
		assertNotNull(coordonneesFinancieres);
		assertNotNull(coordonneesFinancieres.getInstitutionFinanciere());
		assertFalse(coordonneesFinancieres.isComptePostal());
		assertNotNull(coordonneesFinancieres.getNoCompte());
	}

	@Override
	protected void setUp() throws Exception {
		serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
	}

	@Override
	protected void tearDown() throws Exception {
		serviceFiscal.remove();
	}
}