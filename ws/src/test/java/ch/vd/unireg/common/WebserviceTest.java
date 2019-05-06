package ch.vd.unireg.common;

import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.unireg.interfaces.service.mock.DefaultMockServiceSecurite;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceEntreprise;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceSecuriteService;
import ch.vd.unireg.security.Role;

/**
 * Test case abstrait permettant de tester les controlleurs Spring.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CONFIG,
		WebserviceTestingConstants.UNIREG_WSUT_WS,
		WebserviceTestingConstants.UNIREG_WSUT_SERVICES,
		WebserviceTestingConstants.UNIREG_WSUT_SECURITY
})
public abstract class WebserviceTest extends AbstractBusinessTest {

	protected MockHttpServletRequest request;
	protected MockHttpSession session;
	protected HttpServletResponse response;

	protected ProxyServiceEntreprise serviceEntreprise;
	protected ProxyServiceCivil serviceCivil;
	protected ProxyServiceInfrastructureService serviceInfra;
	protected ProxyServiceSecuriteService serviceSecurite;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		request = new MockHttpServletRequest();
		session = new MockHttpSession();
		request.setSession(session);
		response = new MockHttpServletResponse();

		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
		serviceEntreprise = getBean(ProxyServiceEntreprise.class, "serviceEntreprise");
		serviceInfra = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceSecurite = getBean(ProxyServiceSecuriteService.class, "serviceSecuriteService");

		serviceSecurite.setUp(new DefaultMockServiceSecurite() {
			@Override
			protected void init() {
				// on définit l'opérateur par défaut
				super.init();
				addOperateur(getDefaultOperateurName(), Role.VISU_ALL);
			}
		});
	}

	@Override
	protected void setAuthentication() {
		// on assigne également l'OID dans l'authentification pour pouvoir faire des tests sur les rôles par collectivité
		setAuthentication(getDefaultOperateurName(), 22);
	}

	@Override
	protected void loadDatabase(String filename) {
		super.loadDatabase(filename);
		indexTiersData();
	}
}