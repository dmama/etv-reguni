package ch.vd.uniregctb.interfaces;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class ServiceOrganisationServiceTest extends BusinessItTest {

	private ServiceOrganisationService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceOrganisationService.class, "serviceOrganisationService");
	}

	/*
	Test ignoré en attendant que:
	- L'API compatible soit disponible chez RCEnt
	- Que le bug RCEnt empêchant la requête basée sur un numéro cantonal soit réparé.
	 */
	@Ignore
	@Test
	public void testGetOrganisation() throws Exception {

		Organisation org = service.getOrganisationHistory(101202100L);
		assertNotNull(org);
		assertContains("Springbok Ski Tours", org.getNom().get(0).getPayload());
	}
}
