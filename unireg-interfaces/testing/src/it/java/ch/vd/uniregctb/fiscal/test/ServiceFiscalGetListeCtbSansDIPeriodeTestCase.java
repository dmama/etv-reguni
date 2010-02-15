/*
 * *********************************************
 * *        CARTOUCHE DES MODIFICATIONS        *
 * *********************************************
 *
 * - 08.07.2008 (xsibnm)
 * --> creation du fichier
 *
 */package ch.vd.uniregctb.fiscal.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import ch.vd.common.test.EjbUtils;
import ch.vd.uniregctb.fiscal.service.ServiceFiscal;

/**
 * Classe import�e du projet Tao-Interfaces.
 *
 * Test de non regression de la methode GetListeCtbSansDIPeriode
 *
 *  @author xsibnm
 * @version $Revision: 1.0 $
 */
public class ServiceFiscalGetListeCtbSansDIPeriodeTestCase extends TestCase {

    /** le nombre de threads concurrent */
    private static final int THREADS = 1;

    /** Service fiscal. */
    private ServiceFiscal serviceFiscal = null;

    /** une liste de threads */
    private final List threads = new ArrayList();

    /**
     * traitement realis� par un thread
     *
     * @param index l'index du Thread
     * @return un thread Runnable
     */
    private Runnable createRunnable(final int index) {
        return new Runnable() {
            public void run() {

                try {
                    // 10000083;
                    int nextCtbNum = 0;

                    int periodeFiscale = 2003;

                    getServiceFiscal().getListeCtbSansDIPeriode(periodeFiscale, nextCtbNum);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * @return le service fiscal.
     */
    protected ServiceFiscal getServiceFiscal() {
        return serviceFiscal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	protected void setUp() throws Exception {
        super.setUp();

        serviceFiscal = (ServiceFiscal) EjbUtils.createBean(ServiceFiscal.JNDI_NAME);
    }

    /** permet le demarrage des N Threads */
    private void startThreads() {
        for (int i = 0; i < THREADS; i++) {
            Runnable r = createRunnable(i);
            Thread t = new Thread(r);
            threads.add(t);
            t.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
	protected void tearDown() throws Exception {
        serviceFiscal.remove();
    }

    /**
     * la procedure permettant le lancement du test permet le multithreading
     *
     * @throws Exception
     */
    public final void test1() throws Exception {
        startThreads();
        waitForAllThread();
    }

    /** permet d'attendre que les Threads soit termin� */
    private void waitForAllThread() throws InterruptedException {
        for (Iterator iter = threads.iterator(); iter.hasNext();) {
            Thread t = (Thread) iter.next();
            t.join();
        }
    }
}