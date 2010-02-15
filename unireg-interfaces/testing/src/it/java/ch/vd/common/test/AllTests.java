package ch.vd.common.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalMDBTestCase;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalRechercherNoContribuableTestCase;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalTestCase1;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalTestCase2;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalTestCase3;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalTestCase4;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalTestCase5;
import ch.vd.uniregctb.fiscal.test.ServiceFiscalTestCase6;


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

        suite.addTestSuite(ServiceFiscalTestCase1.class);
        suite.addTestSuite(ServiceFiscalTestCase2.class);
        suite.addTestSuite(ServiceFiscalTestCase3.class);
        suite.addTestSuite(ServiceFiscalTestCase4.class);
        suite.addTestSuite(ServiceFiscalTestCase5.class);
        suite.addTestSuite(ServiceFiscalTestCase6.class);
        suite.addTestSuite(ServiceFiscalRechercherNoContribuableTestCase.class);
        suite.addTestSuite(ServiceFiscalMDBTestCase.class);

        return suite;
    }
}
