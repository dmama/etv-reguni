package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.infrastructure.model.Commune;
import ch.vd.infrastructure.model.Pays;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.EnumTypeFor;
import ch.vd.registre.fiscal.model.For;
import ch.vd.uniregctb.fiscal.model.Contribuable;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe importée du projet host-Interfaces.
 *
 * @author Baba NGOM (xsibnm)
 */
public class ServiceFiscalTestCase2 extends TestCase {

    private ServiceFiscal serviceFiscal = null;
    /** Service civil. */
	private ServiceCivil serviceCivil = null;
    @Override
	protected void setUp() throws Exception {
        serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
        serviceCivil = (ServiceCivil) EjbUtils.createBean(ServiceCivil.JNDI_NAME);
    }

    @Override
	protected void tearDown() throws Exception {
        serviceFiscal.remove();
        serviceCivil.remove();
    }

    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testChappuis21104105TRUE() throws RemoteException, RegistreException {
    	 Contribuable c =   serviceFiscal.getContribuable(10038357, 2007,true);
    	 assertNotNull(c);
    }

    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testGetContribuable21104105FALSE() throws RemoteException, RegistreException {
        Contribuable c = serviceFiscal.getContribuable(10038357, 2007,false);
        verifieDonneesFor(null, null, "US", c); // hors Suisse
    }
    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testGetContribuable21104105() throws RemoteException, RegistreException {
        Contribuable c = serviceFiscal.getContribuable(21104105, 2007);
        verifieDonneesFor(null, null, "US", c); // hors Suisse
    }

    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testGetContribuable10000002() throws RemoteException, RegistreException {
        Contribuable c = serviceFiscal.getContribuable(10000002, 2005);
        verifieDonneesFor(null, null, "US", c); // hors Suisse
    }

    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testGetContribuable10000208() throws RemoteException, RegistreException {
        Contribuable c = serviceFiscal.getContribuable(10000208, 2005);
        verifieDonneesFor("TOLOCHENAZ", "VD", null, c); // VD
        assertEquals("1131", c.getAdresse().getNumeroPostal());
    }

    /**
     * @throws RegistreException
     * @throws RemoteException
     *
     */
    public void testGetContribuable2() throws RemoteException, RegistreException {
        Contribuable a = serviceFiscal.getContribuable(93513104, 2005);
        assertEquals(93513104, a.getNoContribuable());

        Contribuable b = serviceFiscal.getContribuable(94201409, 2005);
        assertEquals(94201409, b.getNoContribuable());
    }

    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testGetContribuableEtenduFor() throws RemoteException, RegistreException {
        Contribuable c = serviceFiscal.getContribuable(10000083, 2005);
        For forPrincipal = null;

        Collection fors = serviceFiscal.getFors(c.getNoContribuable(), 2005);

        for (Iterator it = fors.iterator(); it.hasNext();) {
            forPrincipal = (For) it.next();

            if (forPrincipal.getTypeFor().equals(EnumTypeFor.PRINCIPAL)) {
                break;
            }

            forPrincipal = null;
        }

        For forSecondaire = null;

        for (Iterator it = fors.iterator(); it.hasNext();) {
            forSecondaire = (For) it.next();

            if (forSecondaire.getTypeFor().equals(EnumTypeFor.SECONDAIRE)) {
                break;
            }
            forSecondaire = null;
        }
        assertNotNull(forPrincipal);
        assertNull(forSecondaire);
    }

    /**
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testGetContribuableEtenduNpa() throws RemoteException, RegistreException {
        Contribuable c = serviceFiscal.getContribuable(12419207, 2005);
        assertEquals(12419207, c.getNoContribuable());
        assertEquals("1007", c.getAdresse().getNumeroPostal());
    }

    private void verifieDonneesFor(String nomCommune, String canton, String pays, Contribuable c) throws RemoteException, RegistreException {
        assertNotNull(c);
        Collection fors = serviceFiscal.getFors(c.getNoContribuable(), 2006);
        if (pays == "CH") {
            For unFor = null;

            for (Iterator it = fors.iterator(); it.hasNext();) {
                unFor = (For) it.next();

                if (unFor.getTypeFor().equals(EnumTypeFor.PRINCIPAL)) {
                    break;
                }
            }

            assertNotNull(unFor);
            Commune commune = null;
            Pays pays2 = null;

            if (unFor != null) {
                commune = unFor.getCommune();
                pays2 = unFor.getPays();
            }

            if (commune != null) {
                assertEquals(nomCommune, commune.getNomMajuscule());
                assertEquals(nomCommune, commune.getNomMajuscule());

            } else {
                assertEquals(nomCommune, null);
                assertEquals(canton, null);
            }

            String siglePays2 = null;
            if (pays2 != null) {
                siglePays2 = pays2.getSigleOFS();
            }

            assertEquals(pays, siglePays2);
        }

    }
}