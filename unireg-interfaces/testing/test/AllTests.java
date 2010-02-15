package ch.vd.common.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.vd.infrastructure.fiscal.test.ServiceInfrastructureFiscalCase;
import ch.vd.infrastructure.test.ServiceInfrastructureCommuneTestCase;
import ch.vd.infrastructure.test.ServiceInfrastructureTestCase;
import ch.vd.registre.civil.test.ServiceCivilTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalGetListeCtbModifiesTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalGetListeCtbSansDIPeriodeTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalMDBTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalQuittanceServiceTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalRechercherNoContribuableTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalTestCase;
import ch.vd.registre.fiscal.test.ServiceFiscalTestCase2;
import ch.vd.registre.fiscal.test.ServiceFiscalTestCase3;
import ch.vd.registre.fiscal.test.ServiceFiscalTestCase4;
import ch.vd.registre.fiscal.test.ServiceFiscalTestCase5;
import ch.vd.registre.fiscal.test.ServiceFiscalTestCase6;
import ch.vd.securite.test.ServiceSecuriteTestCase;

/**
 * Classe regroupant l'ensemble des cas de test du projet interface-ejb.
 *
 * @author Fabrice Willemin (xcifwi) - SQLI (last modified by $Author: xcifwi $ @ $Date: 2007/07/25 08:06:00 $)
 * @version $Revision: 1.5 $
 */
public class AllTests {

    /**
     * Retourne l'ensemble des cas de test du projet interface-ejb.
     *
     * @return l'ensemble des cas de test du projet interface-ejb.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(ServiceCivilTestCase.class);
        suite.addTestSuite(ServiceFiscalTestCase.class);
        suite.addTestSuite(ServiceSecuriteTestCase.class);
        suite.addTestSuite(ServiceInfrastructureTestCase.class);
        suite.addTestSuite(ServiceInfrastructureFiscalCase.class);
        suite.addTestSuite(ServiceFiscalQuittanceServiceTestCase.class);
        suite.addTestSuite(ServiceInfrastructureCommuneTestCase.class);
        suite.addTestSuite(ServiceFiscalTestCase2.class);
        suite.addTestSuite(ServiceFiscalTestCase3.class);
        suite.addTestSuite(ServiceFiscalTestCase4.class);
        suite.addTestSuite(ServiceFiscalTestCase5.class);
        suite.addTestSuite(ServiceFiscalTestCase6.class);
        suite.addTestSuite(ServiceFiscalRechercherNoContribuableTestCase.class);
        suite.addTestSuite(ServiceFiscalMDBTestCase.class);
        if(false) {
            /* La durée d'exécution des deux tests suivants est très longue... */
            suite.addTestSuite(ServiceFiscalGetListeCtbModifiesTestCase.class);
            suite.addTestSuite(ServiceFiscalGetListeCtbSansDIPeriodeTestCase.class);
        }
        return suite;
    }
}
