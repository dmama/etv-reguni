package ch.vd.uniregctb;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.vd.uniregctb.utils.MockUniregProperties;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

public class LogoutControllerTest {

	/**
	 * Ce test vérifie que la fonctionnalité de logout renvoie bien vers la page d'accueil de IAM (+ invalidation de la session)
	 */
	@Test
	public void testLogoutIAM() throws Exception {

		final MockUniregProperties props = new MockUniregProperties();
		props.afterPropertiesSet();

		final LogoutController controller = new LogoutController();
		controller.setUniregProperties(props);

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final ResultActions resActions = m.perform(get("/logoutIAM.do"));
		resActions.andExpect(redirectedUrl("iam/accueil/"));
	}

	/**
	 * [SIFISC-26633] Ce test vérifie que l'URL relative de logout est bien calculée dans différente situations de déploiement.
	 */
	@Test
	public void testBuildRelativeLogoutUrl() throws Exception {
		assertEquals("iam/accueil/", LogoutController.buildRelativeLogoutUrl("https://{HOST}/iam/accueil/", ""));
		assertEquals("../iam/accueil/", LogoutController.buildRelativeLogoutUrl("https://{HOST}/iam/accueil/", "/unireg-web"));
		assertEquals("../../iam/accueil/", LogoutController.buildRelativeLogoutUrl("https://{HOST}/iam/accueil/", "/unireg/web"));
		assertEquals("../../../iam/accueil/", LogoutController.buildRelativeLogoutUrl("https://{HOST}/iam/accueil/", "/fiscalite/int-unireg/web"));
	}
}