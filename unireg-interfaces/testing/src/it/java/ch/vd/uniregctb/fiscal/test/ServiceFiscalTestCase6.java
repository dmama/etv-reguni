package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.EnumCritereRechercheContribuable;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe import�e du projet host-Interfaces.
 *
 * @author Baba NGOM (xsibnm)
 */
public class ServiceFiscalTestCase6 extends TestCase {

	private static final java.util.logging.Logger log = java.util.logging.Logger.getAnonymousLogger();

	private ServiceFiscal serviceFiscal = null;

	@Override
	protected void setUp() throws Exception {
		serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
	}

	@Override
	protected void tearDown() throws Exception {
		serviceFiscal.remove();
	}

	/**
	 * @throws RegistreException
	 * @throws RemoteException
	 *
	 */
	public void testRechercheParLoocalite() throws RemoteException, RegistreException {
		log.info("testRechercheParLoocalite");
		final String localite = "Morges";
		HashMap criteresRecherche = new HashMap();
		criteresRecherche.put(EnumCritereRechercheContribuable.LOCALITE_POSTALE, localite);
		Collection result = serviceFiscal.rechercherContribuables(criteresRecherche, 49);
		assertTrue(result != null);
	}

	public void testRechercheParNpa() throws RemoteException, RegistreException {
		log.info("testRechercheParNpa");
		final Integer npa = new Integer(1006);
		HashMap criteresRecherche = new HashMap();
		criteresRecherche.put(EnumCritereRechercheContribuable.NO_NPA, npa);
		Collection result = serviceFiscal.rechercherContribuables(criteresRecherche, 49);
		assertTrue(result != null);
	}

}