package ch.vd.uniregctb.interfaces.fiscal;

import static junit.framework.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.fiscal.model.Contribuable;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.impl.DeclarationQuittanceImpl;

import com.thoughtworks.xstream.XStream;

/**
 * Test case du service fiscal.
 *
 * @author Baba NGOM
 *
 */
public class ServiceFiscalTestCase extends AbstractServiceFiscalTest {

	// private static final long CONTRIBUABLE_SIMPLE = 10288531;
	private static final int CONTRIBUABLE_MIGRE = 57109210;
	// private static final int CONTRIBUABLE_MIGRE =75107103;
	// private static final int CONTRIBUABLE_MIGRE = 10105123;
	// private static final int CONTRIBUABLE_MIGRE =10099496;
	// private static final int CONTRIBUABLE_MIGRE = 68109511;
	private static final long CONTRIBUABLE_INTER_57_47528204 = 47528204;
	private static final long CONTRIBUABLE_INTER_55_24108604 = 24108604;

	// private static final long CONTRIBUABLE_INTER_57 = 10054424 /* INTER_54 */;
	// private static final long CONTRIBUABLE_INTER_57 = 10092638 /* INTER_54 */;
	private static final long CONTRIBUABLE_INTER_57 = CONTRIBUABLE_INTER_55_24108604;
	private XStream xstream;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// setUpHabitant();
		// migrator.migreHabitant(CONTRIBUABLE_MIGRE,CONTRIBUABLE_MIGRE);
		// migrator.migreDonneesAnnie();
		migrator.migreCouple();
		// migrator.migreSchumi();

		xstream = new XStream();

	}

	/**
	 * Test de la m�thode getContribuable du service fiscal.
	 *
	 * @throws Exception
	 *             si un probl�me survient durant l'appel au service.
	 */
	@Test
	public void testGetContribuableLongInt() throws Exception {

		Contribuable contribuableUnireg = serviceFiscalUnireg.getContribuable(CONTRIBUABLE_MIGRE, 2008);
		assertNotNull(contribuableUnireg);

	}

	/**
	 * Permet de simulare la quittance d'une DI et de voir si Unireg reçoit bien un événement de changement de base. Fonctionne avec les
	 * données de tiers-basic.xml.
	 */
	@Ignore
	@Test
	public void testRetourneDI() throws Exception{
		List<DeclarationQuittance> declarations = new ArrayList<DeclarationQuittance>();

		final long code = 8600620220070101L;
		DeclarationQuittance quittance = new DeclarationQuittanceImpl(code);
		declarations.add(quittance);
		serviceFiscalUnireg.quittanceDeclarations(declarations);
	}

	@Test
	public void testIsContribuableI107() throws Exception {
		boolean i107Host = serviceFiscalHost.isContribuableI107(CONTRIBUABLE_MIGRE);
		boolean i107Unireg = serviceFiscalUnireg.isContribuableI107(CONTRIBUABLE_MIGRE);
		Assert.isTrue(i107Host == i107Unireg);

	}



}
