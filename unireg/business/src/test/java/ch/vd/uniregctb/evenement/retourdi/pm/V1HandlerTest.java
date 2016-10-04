package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.xml.event.taxation.ibc.v1.AddressInformation;
import ch.vd.unireg.xml.event.taxation.ibc.v1.InformationPersonneMoraleModifiee;
import ch.vd.unireg.xml.event.taxation.ibc.v1.MailAddress;
import ch.vd.unireg.xml.event.taxation.ibc.v1.OrganisationMailAddressInfo;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.EsbBusinessException;

public class V1HandlerTest extends BusinessTest {

	private CollectingRetourService retourService;
	private V1Handler handler;

	private static class CollectingRetourService implements RetourDIPMService {

		private final List<RetourDI> collected = new ArrayList<>();

		@Override
		public void traiterRetour(RetourDI retour, Map<String, String> incomingHeaders) throws EsbBusinessException {
			collected.add(retour);
		}

		public List<RetourDI> getCollected() {
			return collected;
		}
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		retourService = new CollectingRetourService();
		handler = new V1Handler();
		handler.setInfraService(getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"));
		handler.setRetourService(retourService);
	}

	@Test
	public void testExtractDonneesEntreprisesAvecContactEntrepriseEtFlagAdresseModifieeFalse() throws Exception {

		final InformationPersonneMoraleModifiee info = new InformationPersonneMoraleModifiee();
		final OrganisationMailAddressInfo organisation = new OrganisationMailAddressInfo(null, "Toubidou", null, null, "Mme", "Albertine", "Martin");
		final AddressInformation addressInformation = new AddressInformation(null, null, null, "Chemin du pont", "43", null, null, "Lausanne", null, MockLocalite.Lausanne.getNPA().longValue(), null, MockLocalite.Lausanne.getNoOrdre(), null, null);
		info.setAdresseCourrierStructuree(new MailAddress(organisation, null, addressInformation, Boolean.FALSE));

		final InformationsEntreprise infoEntreprise = handler.extractInformationsEntreprise(info);
		Assert.assertNotNull(infoEntreprise);
		Assert.assertEquals("Mme Albertine Martin", infoEntreprise.getAdresseCourrier().getContact());
		Assert.assertNotNull(infoEntreprise.getAdresseCourrier().getDestinataire());
		Assert.assertEquals("Mme Albertine Martin", infoEntreprise.getAdresseCourrier().getDestinataire().getContact());
	}
}
