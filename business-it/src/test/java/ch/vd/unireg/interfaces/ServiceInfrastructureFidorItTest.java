package ch.vd.unireg.interfaces;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.fidor.ServiceInfrastructureFidor;
import ch.vd.unireg.webservice.fidor.v5.FidorClientImpl;
import ch.vd.unireg.wsclient.WebClientPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

// TODO (msi)
@Ignore(value = "A activer quand la version 19R1 de Fidor sera disponible en intégration")
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
		assertNull(adresse.getRue());
		assertNull(adresse.getNumero());
		assertEquals("Lausanne Adm cant", adresse.getLocalite());
		assertEquals("1014", adresse.getNumeroPostal());
		assertNull(adresse.getNumeroPostalComplementaire());
		assertEquals(Integer.valueOf(162), adresse.getNumeroOrdrePostal());
		assertEquals(Integer.valueOf(8100), adresse.getNoOfsPays());
	}

	@Test
	public void testGetOfficesImpot() {

		final List<OfficeImpot> list = new ArrayList<>(fidorService.getOfficesImpot());
		assertNotNull(list);
		assertEquals(27, list.size());

		list.sort(Comparator.comparing(CollectiviteAdministrative::getNoColAdm));
		assertEquals("OID AIGLE", list.get(0).getNomCourt());
		assertEquals("OID AUBONNE/ROLLE", list.get(1).getNomCourt());
		assertEquals("OID AVENCHES", list.get(2).getNomCourt());
		assertEquals("OID COSSONAY", list.get(3).getNomCourt());
		assertEquals("OID ECHALLENS", list.get(4).getNomCourt());
		assertEquals("OID GRANDSON", list.get(5).getNomCourt());
		assertEquals("OID LAUSANNE", list.get(6).getNomCourt());
		assertEquals("OID LA VALLEE", list.get(7).getNomCourt());
		assertEquals("OID LAVAUX", list.get(8).getNomCourt());
		assertEquals("OID MORGES", list.get(9).getNomCourt());
		assertEquals("OID MOUDON", list.get(10).getNomCourt());
		assertEquals("OID NYON", list.get(11).getNomCourt());
		assertEquals("OID ORBE", list.get(12).getNomCourt());
		assertEquals("OID ORON", list.get(13).getNomCourt());
		assertEquals("OID PAYERNE", list.get(14).getNomCourt());
		assertEquals("OID PAYS-D'ENHAUT", list.get(15).getNomCourt());
		assertEquals("OID ROLLE/AUBONNE", list.get(16).getNomCourt());
		assertEquals("OID VEVEY", list.get(17).getNomCourt());
		assertEquals("OID YVERDON", list.get(18).getNomCourt());
		assertEquals("OID LAUSANNE (VILLE)", list.get(19).getNomCourt());
		assertEquals("OI PERSONNES MORALES", list.get(20).getNomCourt());
		assertEquals("ACI - SECTION TAXATION", list.get(21).getNomCourt());
		assertEquals("IMPOT SOURCE - ACI VD", list.get(22).getNomCourt());
		assertEquals("OID GRANDSON, STE-CROIX", list.get(23).getNomCourt());
		assertEquals("CEDI", list.get(24).getNomCourt());
		assertEquals("CAT", list.get(25).getNomCourt());
		assertEquals("ACI-SUCCESSIONS_UNIREG", list.get(26).getNomCourt());
	}
}
