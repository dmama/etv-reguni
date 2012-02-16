package ch.vd.uniregctb.interfaces;

import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class ServiceCivilRcPersTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(ServiceCivilService.class, "serviceCivilRcPers");
	}

	@Override
	public void testGetIndividuConjoint() {
		// TODO (rcpers) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}
}
