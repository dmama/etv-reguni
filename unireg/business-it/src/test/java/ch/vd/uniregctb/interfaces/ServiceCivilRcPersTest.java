package ch.vd.uniregctb.interfaces;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-rcpers.xml"
})
public class ServiceCivilRcPersTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(ServiceCivilService.class, "serviceCivilRcPers");
	}

	@Override
	public void testGetIndividu() throws Exception {
		// TODO (msi) on ne teste rien en attendant la correction de SIREF-1480
	}

	@Override
	public void testGetConjoint() throws Exception {
		// TODO (msi) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}

	@Override
	public void testGetNumeroIndividuConjoint() {
		// TODO (msi) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}

	@Override
	public void testGetIndividuConjoint() {
		// TODO (msi) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}
}
