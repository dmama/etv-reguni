package ch.vd.uniregctb.fiscal.test;

import java.rmi.RemoteException;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.RechercherNoContribuable;
import ch.vd.registre.fiscal.model.ResultatRechercherNoContribuable;
import ch.vd.registre.fiscal.model.impl.RechercherNoContribuableImpl;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;


/**
*
* Test de non regression de la methode RechercherNoContribuable
*
*  @author xsibnm
* @version $Revision: 1.0 $
*/
public class ServiceFiscalRechercherNoContribuableTestCase extends TestCase {

    private ServiceFiscal serviceFiscal = null;

    @Override
	protected void setUp() throws Exception {
        serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
    }

    @Override
	protected void tearDown() throws Exception {
        serviceFiscal.remove();
    }

    public void testRechercherNoContribuable() throws RemoteException, RegistreException {
    	RechercherNoContribuable request = new RechercherNoContribuableImpl();
    	request.setAnnee(2007);
    	request.setNoAVS(89067122211L);
    	request.setNom("Tranchida");
    	request.setPrenom("Giampaolo");
    	request.setNoTechnique(12);

    	ResultatRechercherNoContribuable result = null;
    	try {
    		result = serviceFiscal.RechercherNoContribuable(request);
    	} catch (Exception e) {
    		e.printStackTrace();
    		fail("no error must be returned from this RechercherNoContribuable" );
    	}

    	System.out.println("No technique : " + result.getNoTechnique());
    	System.out.println("Nbr occurence : " + result.getNbrOccurence());
    	System.out.println("No contribuable seul : " + result.getNoContribuableSeul());
    	System.out.println("No contribuable couple : " + result.getNpContribuableCouple());
    	System.out.println("Statut sourcier pur : " + (result.isSourcierPur() == true ? "oui" : "non"));
    }

}
