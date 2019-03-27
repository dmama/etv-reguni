package ch.vd.unireg.interfaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.fidor.ServiceInfrastructureFidor;
import ch.vd.unireg.webservice.fidor.v5.FidorClientImpl;
import ch.vd.unireg.wsclient.WebClientPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ServiceInfrastructureFidorItTest extends BusinessItTest {

	private ServiceInfrastructureFidor fidorService;

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

		fidorService = new ServiceInfrastructureFidor();
		fidorService.setFidorClient(fidorClient);
		fidorService.setUniregCacheManager(Mockito.mock(UniregCacheManager.class));
		fidorService.setRegimesFiscauxBlacklist("");
		fidorService.afterPropertiesSet();
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
		assertEquals("Lausanne", adresse.getLocalite());
		assertEquals("1000", adresse.getNumeroPostal());
		assertNull(adresse.getNumeroPostalComplementaire());
		assertEquals(Integer.valueOf(104), adresse.getNumeroOrdrePostal());
		assertEquals(Integer.valueOf(8100), adresse.getNoOfsPays());
	}

	@Test
	public void testGetOfficesImpot() {

		final List<OfficeImpot> list = new ArrayList<>(fidorService.getOfficesImpot());
		assertNotNull(list);
		assertEquals(13, list.size());

		list.sort(Comparator.comparing(CollectiviteAdministrative::getNoColAdm));
		assertEquals("OID AIGLE", list.get(0).getNomCourt());
		assertEquals("OID ECHALLENS", list.get(1).getNomCourt());
		assertEquals("OID LAUSANNE", list.get(2).getNomCourt());
		assertEquals("OID LA VALLEE", list.get(3).getNomCourt());
		assertEquals("OID NYON", list.get(4).getNomCourt());
		assertEquals("OID PAYS-D'ENHAUT", list.get(5).getNomCourt());
		assertEquals("OID VEVEY", list.get(6).getNomCourt());
		assertEquals("OID YVERDON", list.get(7).getNomCourt());
		assertEquals("OI PERSONNES MORALES", list.get(8).getNomCourt());
		assertEquals("ACI - SECTION TAXATION", list.get(9).getNomCourt());
		assertEquals("CEDI", list.get(10).getNomCourt());
		assertEquals("Test nom court", list.get(11).getNomCourt());
		assertEquals("test new OID", list.get(12).getNomCourt());
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
