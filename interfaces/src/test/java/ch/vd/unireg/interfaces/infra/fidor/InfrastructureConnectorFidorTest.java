package ch.vd.unireg.interfaces.infra.fidor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.fidor.xml.regimefiscal.v2.CategorieEntreprise;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.webservice.fidor.v5.FidorClient;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InfrastructureConnectorFidorTest {

	@Test
	public void testParseRegimesFiscaux() {
		assertEmpty(InfrastructureConnectorFidor.parseRegimesFiscaux(""));
		assertEmpty(InfrastructureConnectorFidor.parseRegimesFiscaux(" "));
		assertEmpty(InfrastructureConnectorFidor.parseRegimesFiscaux(","));
		assertEquals(Collections.singleton("703"), InfrastructureConnectorFidor.parseRegimesFiscaux("703"));
		assertEquals(Collections.singleton("703"), InfrastructureConnectorFidor.parseRegimesFiscaux("703 "));
		assertEquals(Collections.singleton("703"), InfrastructureConnectorFidor.parseRegimesFiscaux("703,"));
		assertEquals(new HashSet<>(Arrays.asList("703", "1000")), InfrastructureConnectorFidor.parseRegimesFiscaux("703,1000"));
		assertEquals(new HashSet<>(Arrays.asList("703", "1000")), InfrastructureConnectorFidor.parseRegimesFiscaux("703, 1000"));
		assertEquals(new HashSet<>(Arrays.asList("703", "1000")), InfrastructureConnectorFidor.parseRegimesFiscaux("703, 1000 "));
	}

	/**
	 * [FISCPROJ-92] Ce test vérifie que la méthode getTousLesRegimesFiscaux applique bien la blacklist sur les régimes fiscaux.
	 */
	@Test
	public void testGetTousLesRegimesFiscauxAvecBlacklist() {

		final FidorClient client = Mockito.mock(FidorClient.class);
		Mockito.when(client.getRegimesFiscaux()).thenReturn(Arrays.asList(newRegimeFiscal("70"),newRegimeFiscal("703"), newRegimeFiscal("1000")));

		final InfrastructureConnectorFidor service = new InfrastructureConnectorFidor();
		service.setFidorClient(client);
		service.setRegimesFiscauxBlacklist("703");

		// le régime fiscal 703 ne doit pas être exposé
		final List<TypeRegimeFiscal> regimesFiscaux = service.getTousLesRegimesFiscaux();
		assertNotNull(regimesFiscaux);
		assertEquals(2, regimesFiscaux.size());
		assertRegimeFiscal("70", regimesFiscaux.get(0));
		assertRegimeFiscal("1000", regimesFiscaux.get(1));
	}

	/**
	 * [FISCPROJ-92] Ce test vérifie que la méthode getTousLesRegimesFiscaux applique bien la blacklist sur les régimes fiscaux.
	 */
	@Test
	public void testGetTousLesRegimesFiscauxSansBlacklist() {

		final FidorClient client = Mockito.mock(FidorClient.class);
		Mockito.when(client.getRegimesFiscaux()).thenReturn(Arrays.asList(newRegimeFiscal("70"),newRegimeFiscal("703"), newRegimeFiscal("1000")));

		final InfrastructureConnectorFidor service = new InfrastructureConnectorFidor();
		service.setFidorClient(client);
		service.setRegimesFiscauxBlacklist(""); // vide

		// le régime fiscal 703 doit bien être exposé
		final List<TypeRegimeFiscal> regimesFiscaux = service.getTousLesRegimesFiscaux();
		assertNotNull(regimesFiscaux);
		assertEquals(3, regimesFiscaux.size());
		assertRegimeFiscal("70", regimesFiscaux.get(0));
		assertRegimeFiscal("703", regimesFiscaux.get(1));
		assertRegimeFiscal("1000", regimesFiscaux.get(2));
	}

	private static void assertRegimeFiscal(String code, TypeRegimeFiscal regimeFiscal) {
		assertNotNull(regimeFiscal);
		assertEquals(code, regimeFiscal.getCode());
	}

	private static RegimeFiscal newRegimeFiscal(String code) {
		final RegimeFiscal rf = new RegimeFiscal();
		rf.setCode(code);
		rf.setCantonal(false);
		rf.setFederal(false);
		rf.setCategorieEntreprise(new CategorieEntreprise("PM", null, null));
		rf.setPeriodeFiscaleDebutValidite(0);
		return rf;
	}
}