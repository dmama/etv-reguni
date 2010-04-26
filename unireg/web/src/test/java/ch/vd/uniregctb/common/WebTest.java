package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceSecurite;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServicePM;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceSecuriteService;
import ch.vd.uniregctb.security.Role;

/**
 * Test case abstrait permettant de tester les controlleurs Spring.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
@ContextConfiguration(locations = {
		"classpath:WEB-INF/unireg-web-common.xml",
		"classpath:WEB-INF/unireg-web-config.xml",
		"classpath:WEB-INF/unireg-web-tiers.xml",
		"classpath:WEB-INF/unireg-web-info.xml",
		"classpath:WEB-INF/unireg-web-admin.xml",
		"classpath:WEB-INF/unireg-web-evenements.xml",
		"classpath:WEB-INF/unireg-web-lr.xml",
		"classpath:WEB-INF/unireg-web-rapport.xml",
		"classpath:WEB-INF/unireg-web-fusion.xml",
		"classpath:WEB-INF/unireg-web-couple.xml",
		"classpath:WEB-INF/unireg-web-separation.xml",
		"classpath:WEB-INF/unireg-web-deces.xml",
		"classpath:WEB-INF/unireg-web-di.xml",
		"classpath:WEB-INF/unireg-web-mouvement.xml",
		"classpath:WEB-INF/unireg-web-tache.xml",
		"classpath:WEB-INF/unireg-web-fiscal.xml",
		"classpath:WEB-INF/unireg-web-adresse.xml",
		"classpath:WEB-INF/unireg-web-complement.xml",
		"classpath:WEB-INF/unireg-web-civil.xml",
		"classpath:WEB-INF/unireg-web-parametrage.xml",
		"classpath:WEB-INF/unireg-web-acces.xml",
		"classpath:WEB-INF/unireg-web-activation.xml",
		"classpath:WEB-INF/unireg-web-identification.xml",
		WebTestingConstants.UNIREG_WEBUT_SERVICES,
		WebTestingConstants.UNIREG_WEBUT_SECURITY
})
public abstract class WebTest extends AbstractBusinessTest {

	/**
	 * Une mock request.
	 */
	protected MockHttpServletRequest request;

	/**
	 * Une mock session.
	 */
	protected MockHttpSession session;

	/**
	 * Une response.
	 */
	protected HttpServletResponse response;

	protected GlobalTiersIndexer globalTiersIndexer;

	protected ProxyServicePM servicePM;
	protected ProxyServiceCivil serviceCivil;
	protected ProxyServiceInfrastructureService serviceInfra;
	protected ProxyServiceSecuriteService serviceSecurite;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {

		globalTiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");

		super.onSetUp();

		if (AuthenticationHelper.isAuthenticated()) {
			// msi: pourquoi est-il nécessaire de forcer l'OID ???
			AuthenticationHelper.setCurrentOID(ServiceInfrastructureService.noACI); // ACI
		}

		request = new MockHttpServletRequest();
		session = new MockHttpSession();
		request.setSession(session);
		response = new MockHttpServletResponse();

		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
		servicePM = getBean(ProxyServicePM.class, "servicePersonneMoraleService");
		serviceInfra = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceSecurite = getBean(ProxyServiceSecuriteService.class, "serviceSecuriteService");

		serviceSecurite.setUp(new DefaultMockServiceSecurite() {
			@Override
			protected void init() {
				// on définit l'opérateur par défaut
				super.init();
				addOperateur(getDefaultOperateurName(), 0, Role.VISU_ALL.getIfosecCode());
			}
		});
	}

	@Override
	public void onTearDown() throws Exception {

		if (AuthenticationHelper.isAuthenticated()) {
			AuthenticationHelper.setCurrentOID(-1);
		}

		try {
			super.onTearDown();
		}
		finally {

			/*
			 * Il faut l'enlever apres le onTearDown parce que le endTransaction en a besoin pour faire l'indexation lors du commit()
			 */
			if (serviceCivil != null) {
				serviceCivil.tearDown();
			}
			if (servicePM != null) {
				servicePM.tearDown();
			}
			if (serviceSecurite != null) {
				serviceSecurite.tearDown();
			}
		}
	}

	@Override
	protected void loadDatabase(String filename) throws Exception {
		try {
			super.loadDatabase(filename);

			indexData();
		}
		catch (Exception e) {
			serviceCivil.tearDown(); // autrement les tests suivants foirent
			servicePM.tearDown();
			serviceSecurite.tearDown();
			serviceInfra.tearDown();
			throw e;
		}
	}

	@Override
	protected void removeIndexData() throws Exception {
		globalTiersIndexer.overwriteIndex();
	}

	/**
	 * @throws Exception
	 */
	@Override
	protected void indexData() throws Exception {
		globalTiersIndexer.indexAllDatabaseAsync(null, 1, Mode.FULL, false);
	}

	/**
	 * @return l'objet BeanPropertyBindingResult renseigné par spring suite à l'exécution d'une méthode 'onSubmit' d'un controller.
	 */
	protected BeanPropertyBindingResult getBindingResult(final ModelAndView mav) {
		final BeanPropertyBindingResult exception = (BeanPropertyBindingResult) mav.getModel().get(
				"org.springframework.validation.BindingResult.command");
		return exception;
	}
}
