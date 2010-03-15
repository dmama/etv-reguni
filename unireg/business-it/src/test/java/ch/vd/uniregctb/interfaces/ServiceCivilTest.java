package ch.vd.uniregctb.interfaces;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class ServiceCivilTest extends BusinessItTest {

	private ServiceCivilService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceCivilService.class, "serviceCivilService");
	}

	@Test
	public void testGetIndividu() throws Exception {

		Individu elie = service.getIndividu(333527L, 2007);
		assertNotNull(elie);
		// En 2005, il n'etait pas né.. devrait etre null!
		assertEquals("Elie", elie.getDernierHistoriqueIndividu().getPrenom());
		elie = service.getIndividu(333527L, 2005);
		// assertNull(elie);

		Individu jean = service.getIndividu(333528, 2007);
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getDernierHistoriqueIndividu().getPrenom());
		jean = service.getIndividu(333528, 2001);
		assertNotNull(jean);
		jean = service.getIndividu(333528, 2006, EnumAttributeIndividu.CONJOINT);
		assertNotNull(jean);
		assertNotNull(jean.getConjoint());
		Individu sara = jean.getConjoint();
		assertEquals("Sara", sara.getDernierHistoriqueIndividu().getPrenom());
	}

}
