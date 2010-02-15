package ch.vd.uniregctb.fiscal.test;


import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.uniregctb.fiscal.model.Contribuable;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Test case du service fiscal proposé par unireg-interfaces.
 *
 * @author Baba NGOM (xsibnm) -
 *  @version $Revision: 1.0 $
 */
public class ServiceFiscalTestCase1 extends TestCase {

	private static final long CONTRIBUABLE_INTER_57_40809768 = 40809768;
	private static final long CONTRIBUABLE_INTER_57_47528204 = 47528204;
	private static final long CONTRIBUABLE_INTER_55_24108604 = 24108604;

//	private static final long CONTRIBUABLE_INTER_57 = 10054424 /* INTER_54 */;
//	private static final long CONTRIBUABLE_INTER_57 = 10092638 /* INTER_54 */;
	private static final long CONTRIBUABLE_INTER_57 = CONTRIBUABLE_INTER_55_24108604;

    /** Service fiscal. */
    private ServiceFiscal serviceFiscal = null;

    /**
     * {@inheritDoc}
     */
    @Override
	protected void setUp() throws Exception {
        super.setUp();

        serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	protected void tearDown() throws Exception {
        serviceFiscal.remove();
    }



    /**
     * Test de la m�thode getContribuable du service fiscal.
     *
     * @throws Exception si un probl�me survient durant l'appel au service.
     */
    public void testGetContribuableLongInt() throws Exception {

    	Contribuable ctb = serviceFiscal.getContribuable(21104105, 2007);
    	assertNotNull(ctb);
    }

}
