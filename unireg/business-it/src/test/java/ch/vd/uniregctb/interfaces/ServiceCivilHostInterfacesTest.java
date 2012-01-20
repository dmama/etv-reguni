package ch.vd.uniregctb.interfaces;

import org.junit.Test;

import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

import static org.junit.Assert.assertNotNull;

public class ServiceCivilHostInterfacesTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceCivilService.class, "serviceCivilService");
	}

	@Test
	public void dumpTest() throws Exception {

		final Individu jean =
				service.getIndividu(702832, null, AttributeIndividu.ADRESSES, AttributeIndividu.ORIGINE, AttributeIndividu.NATIONALITE, AttributeIndividu.ADOPTIONS, AttributeIndividu.ENFANTS,
						AttributeIndividu.PARENTS, AttributeIndividu.PERMIS, AttributeIndividu.TUTELLE);
		assertNotNull(jean);

		System.out.println(IndividuDumper.dump(jean, true));
	}
}
