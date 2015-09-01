package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
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
		"classpath:WEB-INF/unireg-web-parametrage.xml",
		"classpath:WEB-INF/unireg-web-acces.xml",
		"classpath:WEB-INF/unireg-web-activation.xml",
		"classpath:WEB-INF/unireg-web-identification.xml",
		"classpath:WEB-INF/unireg-web-supergra.xml",
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CONFIG,
		WebTestingConstants.UNIREG_WEBUT_SERVICES,
		WebTestingConstants.UNIREG_WEBUT_SECURITY,
		WebTestingConstants.UNIREG_WEBUT_JMX
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

		super.onSetUp();

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
				addOperateur(getDefaultOperateurName(), 0, Role.VISU_ALL);
			}
		});
	}

	@Override
	protected void setAuthentication() {
		// on assigne également l'OID dans l'authentification pour pouvoir faire des tests sur les rôles par collectivité
		setAuthentication(getDefaultOperateurName(), 22);
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
	protected BeanPropertyBindingResult getBindingResult(final ModelAndView mav) {
		return (BeanPropertyBindingResult) mav.getModel().get("org.springframework.validation.BindingResult.command");
	}

	protected interface IndividuModification {
		void modifyIndividu(MockIndividu individu);
	}

	protected interface IndividusModification {
		void modifyIndividus(MockIndividu individu, MockIndividu other);
	}

	protected void doModificationIndividu(long noIndividu, IndividuModification modifier) {
		final MockIndividu ind = ((MockServiceCivil) serviceCivil.getUltimateTarget()).getIndividu(noIndividu);
		modifier.modifyIndividu(ind);
	}

	protected void doModificationIndividus(long noIndividu, long noOther, IndividusModification modifier) {
		final MockIndividu ind = ((MockServiceCivil) serviceCivil.getUltimateTarget()).getIndividu(noIndividu);
		final MockIndividu other = ((MockServiceCivil) serviceCivil.getUltimateTarget()).getIndividu(noOther);
		modifier.modifyIndividus(ind, other);
	}
}
