package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceSecurite;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceSecuriteService;
import ch.vd.uniregctb.security.Role;

import static org.junit.Assert.assertNotNull;

/**
 * Test case abstrait permettant de tester les controlleurs Spring.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
@ContextConfiguration(locations = {
		"classpath:WEB-INF/unireg-web-common.xml",
		"classpath:WEB-INF/unireg-web-remoting.xml",
		"classpath:WEB-INF/unireg-web-tiers.xml",
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
		"classpath:WEB-INF/unireg-web-etats.xml",
		"classpath:WEB-INF/unireg-web-parametrage.xml",
		"classpath:WEB-INF/unireg-web-acces.xml",
		"classpath:WEB-INF/unireg-web-activation.xml",
		"classpath:WEB-INF/unireg-web-identification.xml",
		"classpath:WEB-INF/unireg-web-supergra.xml",
		"classpath:WEB-INF/unireg-web-entreprise.xml",
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CONFIG,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_ADRESSES,
		WebTestingConstants.UNIREG_WEBUT_SERVICES,
		WebTestingConstants.UNIREG_WEBUT_SECURITY,
		WebTestingConstants.UNIREG_WEBUT_ULRMAPPING,
		WebTestingConstants.UNIREG_WEBUT_JMX,
		WebTestingConstants.UNIREG_WEBUT_ADMIN,
		WebTestingConstants.UNIREG_WEBUT_CONTROLLER
})
public abstract class WebTestSpring3 extends AbstractBusinessTest {

	protected MockHttpServletRequest request;
	protected MockHttpSession session;
	protected HttpServletResponse response;
	protected HandlerAdapter handlerAdapter;

	protected ProxyServiceCivil serviceCivil;
	protected ProxyServiceInfrastructureService serviceInfra;
	protected ProxyServiceSecuriteService serviceSecurite;
	private HandlerMapping handlerMapping;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		request = new MockHttpServletRequest();
		session = new MockHttpSession();
		request.setSession(session);
		response = new MockHttpServletResponse();

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		handlerAdapter = getBean(HandlerAdapter.class, "annotationHandlerAdapter");
		handlerMapping = getBean(HandlerMapping.class, "annotationHandlerMapping");

		serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
		serviceInfra = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		serviceSecurite = getBean(ProxyServiceSecuriteService.class, "serviceSecuriteService");

		serviceSecurite.setUp(new DefaultMockServiceSecurite() {
			@Override
			protected void init() {
				// on définit l'opérateur par défaut
				super.init();
				addOperateur(getDefaultOperateurName(), 0, Role.VISU_ALL);
			}
		});
	}

	@Override
	protected void setAuthentication() {
		// on assigne également l'OID dans l'authentification pour pouvoir faire des tests sur les rôles par collectivité
		setAuthentication(getDefaultOperateurName(), 22);
	}

	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
	        final HandlerExecutionChain handler = handlerMapping.getHandler(request);
	        assertNotNull("No handler found for request, check you request mapping", handler);

	        final Object controller = handler.getHandler();
	        // if you want to override any injected attributes do it here

	        final HandlerInterceptor[] interceptors = handlerMapping.getHandler(request).getInterceptors();
	        for (HandlerInterceptor interceptor : interceptors) {
	            final boolean carryOn = interceptor.preHandle(request, response, controller);
	            if (!carryOn) {
	                return null;
	            }
	        }

			return handlerAdapter.handle(request, response, controller);
		}
		catch (UnexpectedRollbackException e) {
			@SuppressWarnings("ThrowableResultOfMethodCallIgnored") final Throwable rootCause = ExceptionUtils.getRootCause(e);
			if (rootCause instanceof Exception) {
				throw (Exception) rootCause;
			}
			else {
				throw e;
			}
		}
    }

	/**
	 * <b>NE PAS UTILISER CETTE METHODE dans des nouveaux tests !</b>
	 * <p/>
	 * Le chargement des fichiers DbUnit est extrêmenent lent avec Oracle 11g : veuillez créer les données de test à partir du code Java (voir les méthodes addXXX).
	 */
	@Override
	protected void loadDatabase(String filename) throws Exception {
		super.loadDatabase(filename);
		indexTiersData();
	}

	/**
	 * @return l'objet BeanPropertyBindingResult renseigné par spring suite à l'exécution d'une méthode 'onSubmit' d'un controller.
	 */
	protected BeanPropertyBindingResult getBindingResult(ModelAndView mav) {
		return getBindingResult(mav, "command");
	}

	/**
	 * @return l'objet BeanPropertyBindingResult renseigné par spring suite à l'exécution d'une méthode 'onSubmit' d'un controller.
	 */
	protected BeanPropertyBindingResult getBindingResult(ModelAndView mav, String commandName) {
		final String key = String.format("org.springframework.validation.BindingResult.%s", commandName);
		return (BeanPropertyBindingResult) mav.getModel().get(key);
	}
}
