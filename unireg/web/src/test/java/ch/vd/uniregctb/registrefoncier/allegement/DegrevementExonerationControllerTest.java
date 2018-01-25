package ch.vd.uniregctb.registrefoncier.allegement;

import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FlashMessage;
import ch.vd.uniregctb.common.MockControllerUtils;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(UniregJUnit4Runner.class)
public class DegrevementExonerationControllerTest {

	@Before
	public void setUp() {
		AuthenticationHelper.pushPrincipal("test-user", 22);
	}

	@After
	public void tearDown() {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * [SIFISC-27974] Ce test vérifie qu'il n'est pas possible de désannuler une demande de dégrèvement lorsqu'une autre demande active existe pour la même année.
	 */
	@Test
	public void testDesannulerDemandeActiveMemePeriodeFiscale() throws Exception {

		// les données métier : une entreprise, un immeuble, deux démandes de dégrèvement pour 2017 dont une annulée
		final Entreprise entreprise = new Entreprise();
		entreprise.setNumero(1234L);

		final BienFondsRF immeuble = new BienFondsRF();
		immeuble.setId(3333L);

		final DemandeDegrevementICI demande2017 = new DemandeDegrevementICI();
		demande2017.setId(1L);
		demande2017.setPeriodeFiscale(2017);
		demande2017.setImmeuble(immeuble);
		entreprise.addAutreDocumentFiscal(demande2017);

		final DemandeDegrevementICI demande2017annulee = new DemandeDegrevementICI();
		demande2017annulee.setId(2L);
		demande2017annulee.setPeriodeFiscale(2017);
		demande2017annulee.setAnnule(true);
		demande2017annulee.setImmeuble(immeuble);
		entreprise.addAutreDocumentFiscal(demande2017annulee);

		// un hibernate template qui retourne la demande annulée
		final HibernateTemplateImpl hibernateTemplate = new HibernateTemplateImpl() {
			@Override
			public <T> T get(Class<T> clazz, Serializable id) {
				//noinspection unchecked
				return (T) demande2017annulee;
			}
		};

		// initialisation du contrôleur
		final DegrevementExonerationController controller = new DegrevementExonerationController();
		controller.setHibernateTemplate(hibernateTemplate);
		controller.setSecurityProviderInterface(new MockSecurityProvider(Role.DEMANDES_DEGREVEMENT_ICI));
		controller.setControllerUtils(new MockControllerUtils());

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();
		final MockHttpSession session = new MockHttpSession();

		// on demande de désannuler la demande de dégrèvemment annulée de 2017
		final ResultActions res = m.perform(post("/degrevement-exoneration/desannuler.do").session(session).param("id", String.valueOf(demande2017annulee.getId())));
		res.andExpect(status().isMovedTemporarily());

		// une erreur doit être affichée (parce qu'il y a déjà une demande active pour 2017) et la demande ne doit pas être désannulée
		final FlashMessage flash = (FlashMessage) session.getAttribute("flash");
		assertNotNull(flash);
		assertEquals("Impossible de désannuler la demande spécifiée car il existe déjà une demande active pour la période fiscale 2017", flash.getMessage());
		assertEquals("flash-error", flash.getDisplayClass());
		assertTrue(demande2017annulee.isAnnule());
	}
}