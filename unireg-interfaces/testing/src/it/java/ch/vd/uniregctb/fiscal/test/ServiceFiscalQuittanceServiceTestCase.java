package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.impl.DeclarationQuittanceImpl;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe importée du projet host-Interfaces.
 */
public class ServiceFiscalQuittanceServiceTestCase extends TestCase {

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
     * M�thode � tester.
     *
     * @throws RemoteException
     * @throws RegistreException
     */
    public void testRegistreQuittance() throws RemoteException, RegistreException {
        assertEquals(0, serviceFiscal.quittanceDeclarations(new ArrayList()).size());

        List quittancesList = new ArrayList();

        long noDeclaration = Long.parseLong("1000000220030207");
        DeclarationQuittance noDec = new DeclarationQuittanceImpl(noDeclaration);

        quittancesList.add(noDec);

        noDeclaration = Long.parseLong("1000001720030120");
        DeclarationQuittance noDec2 = new DeclarationQuittanceImpl(noDeclaration);

        quittancesList.add(noDec2);

        List c = serviceFiscal.quittanceDeclarations(quittancesList);

        DeclarationQuittance d = (DeclarationQuittance) c.get(0);

        assertEquals(Long.parseLong("1000000220030207"), d.getNoDeclaration());
        assertEquals(2, d.getCodeRetour().getNameAsInt());

        Calendar maintenant = new GregorianCalendar();
        Calendar cal = new GregorianCalendar();
        cal.setTime(d.getQuittanceTime());
        assertEquals(maintenant.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(maintenant.get(Calendar.MONTH), cal.get(Calendar.MONTH));
        assertEquals(maintenant.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));

        DeclarationQuittance d2 = (DeclarationQuittance) c.get(1);

        assertEquals(Long.parseLong("1000001720030120"), d2.getNoDeclaration());
        assertEquals(1, d2.getCodeRetour().getNameAsInt());
    }
}