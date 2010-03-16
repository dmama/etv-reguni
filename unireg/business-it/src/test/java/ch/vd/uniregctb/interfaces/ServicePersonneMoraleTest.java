package ch.vd.uniregctb.interfaces;

import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

public class ServicePersonneMoraleTest extends BusinessItTest {

	private ServicePersonneMoraleService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServicePersonneMoraleService.class, "servicePersonneMoraleService");
	}

	@Test
	public void testGetPM() throws Exception {

		PersonneMorale pm = service.getPersonneMorale(10245L);
		assertNotNull(pm);
		assertContains("Mon Foyer", pm.getRaisonSociale());
	}

}
