package ch.vd.unireg.interfaces;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.fidor.InfrastructureConnectorFidor;
import ch.vd.unireg.webservice.fidor.v5.FidorClientImpl;
import ch.vd.unireg.wsclient.WebClientPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InfrastructureConnectorFidorItTest extends BusinessItTest {

	private InfrastructureConnectorFidor fidorService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final String url = uniregProperties.getProperty("testprop.webservice.fidor.url");
		final String user = uniregProperties.getProperty("testprop.webservice.fidor.username");
		final String password = uniregProperties.getProperty("testprop.webservice.fidor.password");

		final WebClientPool fidorPool = new WebClientPool(false, "");
		fidorPool.setBaseUrl(url);
		fidorPool.setUsername(user);
		fidorPool.setPassword(password);

		final FidorClientImpl fidorClient = new FidorClientImpl();
		fidorClient.setWcPool(fidorPool);

		fidorService = new InfrastructureConnectorFidor();
		fidorService.setFidorClient(fidorClient);
		fidorService.setRegimesFiscauxBlacklist("");
	}

	@Test
	public void testGetCollectiviteAciVd() {

		final CollectiviteAdministrative aci = fidorService.getCollectivite(22);
		assertNotNull(aci);
		assertEquals(22, aci.getNoColAdm());
		assertEquals("Administration cantonale des impôts", aci.getNomComplet1());
		assertNull(aci.getNomComplet2());
		assertNull(aci.getNomComplet3());
		assertEquals("021'316'21'21", aci.getNoTelephone());
		assertEquals("VD", aci.getSigleCanton());

		final Adresse adresse = aci.getAdresse();
		assertNotNull(adresse);
		assertNull(adresse.getCasePostale());
		assertNull(adresse.getDateDebut());
		assertNull(adresse.getDateFin());
		assertEquals("Route de Berne", adresse.getRue());
		assertEquals("46", adresse.getNumero());
		assertEquals("Lausanne Adm cant", adresse.getLocalite());
		assertEquals("1014", adresse.getNumeroPostal());
		assertNull(adresse.getNumeroPostalComplementaire());
		assertEquals(Integer.valueOf(162), adresse.getNumeroOrdrePostal());
		assertEquals(Integer.valueOf(8100), adresse.getNoOfsPays());
	}

	@Test
	public void testGetOfficesImpot() {

		final Map<Integer, OfficeImpot> map = fidorService.getOfficesImpot().stream()
				.collect(Collectors.toMap(CollectiviteAdministrative::getNoColAdm, o -> o));
		assertNotNull(map);
		assertTrue(map.size() >= 12);   // on se prémunit contre la création d'OID de tests en intégration

		assertEquals("OID AIGLE", map.get(1).getNomCourt());
		assertEquals("OID ECHALLENS", map.get(5).getNomCourt());
		assertEquals("OID LAUSANNE", map.get(7).getNomCourt());
		assertEquals("OID LA VALLEE", map.get(8).getNomCourt());
		assertEquals("OID NYON", map.get(12).getNomCourt());
		assertEquals("OID PAYS-D'ENHAUT", map.get(16).getNomCourt());
		assertEquals("OID VEVEY", map.get(18).getNomCourt());
		assertEquals("OID YVERDON", map.get(19).getNomCourt());
		assertEquals("OI PERSONNES MORALES", map.get(21).getNomCourt());
		assertEquals("ACI - SECTION TAXATION", map.get(25).getNomCourt());
		assertEquals("CEDI", map.get(1012).getNomCourt());
		assertEquals("CAT", map.get(1341).getNomCourt());
	}

	/**
	 * [SIFISC-30980] Ce test vérifie que les collectivités retournées sont bien actives (= pas de collectivité inactive retournée).
	 */
	@Test
	public void testGetCollectivitesAdministrativesParSigle() {

		// on recherche toutes les collectivités de type OID (+ filtre sur le nom pour éviter les collectivités de test créées en intégration)
		final List<CollectiviteAdministrative> list = fidorService.getCollectivitesAdministratives(Collections.singletonList(TypeCollectivite.SIGLE_CIR)).stream()
				.filter(c -> c.getNomCourt().startsWith("OID"))
				.sorted(Comparator.comparing(CollectiviteAdministrative::getNomCourt))
				.collect(Collectors.toList());
		assertEquals(8, list.size());
		assertEquals("OID AIGLE", list.get(0).getNomCourt());
		assertEquals("OID ECHALLENS", list.get(1).getNomCourt());
		assertEquals("OID LA VALLEE", list.get(2).getNomCourt());
		assertEquals("OID LAUSANNE", list.get(3).getNomCourt());
		assertEquals("OID NYON", list.get(4).getNomCourt());
		assertEquals("OID PAYS-D'ENHAUT", list.get(5).getNomCourt());
		assertEquals("OID VEVEY", list.get(6).getNomCourt());
		assertEquals("OID YVERDON", list.get(7).getNomCourt());
	}
}
