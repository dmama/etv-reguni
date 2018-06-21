package ch.vd.unireg.registrefoncier.allegement;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.FlashMessage;
import ch.vd.unireg.common.MockControllerUtils;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.documentfiscal.EtatAutreDocumentFiscalEmis;
import ch.vd.unireg.documentfiscal.EtatAutreDocumentFiscalRappele;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.hibernate.HibernateTemplateImpl;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

public class DegrevementExonerationControllerTest extends WebTest {

	private DegrevementExonerationController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(DegrevementExonerationController.class, "degrevementExonerationController");
	}


	/**
	 * [SIFISC-27974] Ce test vérifie qu'il n'est pas possible de désannuler une demande de dégrèvement lorsqu'une autre demande active existe pour la même année.
	 */
	@Test
	public void testDesannulerDemandeActiveMemePeriodeFiscale() throws Exception {

		// user spécifique au test
		AuthenticationHelper.pushPrincipal("test-user", 22);

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

	/**
	 * [SIFISC-29014] Il ne doit pas être possible d'ajouter un délai pour une lettre de bienvenue ou un dégrèvement avec rappel déjà envoyé
	 *
	 * @throws Exception
	 */
	@Test
	public void testEditerDemandeDegrevementRappele() throws Exception {

		Long demandeId = doInNewTransaction(status -> {
			// les données métier : une entreprise, un immeuble, deux démandes de dégrèvement pour 2017 dont une annulée
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();

			final BienFondsRF immeuble = addBienFondsRF("idrf", "egrid", addCommuneRF(456, "nom rf", 2030), 1234);

			final DemandeDegrevementICI demandeRappelee = addDemandeDegrevementICI(entreprise, 2017, immeuble);
			// Ajout de l'état rappelé
			final RegDate dateEnvoiLettre = RegDate.get(2016, 6, 2);
			final RegDate dateRappel = RegDate.get(2017, 7, 1);
			demandeRappelee.addEtat(new EtatAutreDocumentFiscalEmis(dateEnvoiLettre));
			demandeRappelee.addEtat(new EtatAutreDocumentFiscalRappele(dateRappel, dateRappel));

			return demandeRappelee.getId();
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();
		final MockHttpSession session = new MockHttpSession();

		// Edition de la demande de dégrèvement
		final ResultActions res = m.perform(get("/degrevement-exoneration/edit-demande-degrevement.do").session(session).param("id", String.valueOf(demandeId)));
		res.andExpect(status().isOk());

		// Vérifie que les données exposées sont bien correctes
		final MvcResult result = res.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();
		assertNotNull(model);

		// Vérification des données de la réponse
		final EditDemandeDegrevementView view = (EditDemandeDegrevementView) model.get("editDemandeDegrevementCommand");
		assertNotNull(view);

		final boolean isAjoutDelaiAutorise = (Boolean) model.get("isAjoutDelaiAutorise");
		assertEquals(false, isAjoutDelaiAutorise);
	}

	/**
	 * [SIFISC-29014] Test pour ajout d'un délai, alors qu'un rappel a déjà été envoyé.
	 * Post = action d'ajout
	 *
	 * @throws Exception
	 */
	@Test
	public void testAjouterDelaiDegrevementRappelePost() throws Exception {

		class Ids{
			Long doc;
		}

		final Ids ids = new Ids();

		final RegDate dateEnvoiLettre = date(2017, 6, 2);
		final RegDate dateTraitement = date(2017, 7, 13);
		final RegDate dateDelai = date(2017, 7, 14);
		final RegDate dateRappel = date(2017, 7, 15);

		doInNewTransaction(status -> {
			Entreprise e = addEntrepriseConnueAuCivil(33);

			DemandeDegrevementICI dg = addDemandeDegrevement(e, 2017);
			addEtatAutreDocumentFiscalEmis(dg, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRappele(dg, dateRappel);
			addDelaiAutreDocumentFiscal(dg, dateTraitement, dateDelai, EtatDelaiDocumentFiscal.ACCORDE);

			ids.doc = dg.getId();

			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final RegDate dateDemande = RegDate.get();
		final RegDate dateNouveauDelai = dateDemande.addDays(20);
		SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy");

		final ResultActions res = m.perform(post("/degrevement-exoneration/delai/ajouter.do").param("idDocumentFiscal", String.valueOf(ids.doc))
				.param("delaiAccordeAu", formater.format(dateNouveauDelai.asJavaDate()))
				.param("dateDemande", formater.format(dateDemande.asJavaDate())));
		res.andExpect(redirectedUrl("/degrevement-exoneration/edit-demande-degrevement.do?id=" + ids.doc));
	}

	/**
	 * [SIFISC-29014] Test pour ajout d'un délai, alors qu'un rappel a déjà été envoyé.
	 * Post = action d'ajout
	 *
	 * @throws Exception
	 */
	@Test
	public void testAjouterDelaiDegrevementRappeleGet() throws Exception {

		class Ids{
			Long doc;
		}

		final Ids ids = new Ids();

		final RegDate dateEnvoiLettre = date(2017, 6, 2);
		final RegDate dateTraitement = date(2017, 7, 13);
		final RegDate dateDelai = date(2017, 7, 14);
		final RegDate dateRappel = date(2017, 7, 15);

		doInNewTransaction(status -> {
			Entreprise e = addEntrepriseConnueAuCivil(33);

			DemandeDegrevementICI dg = addDemandeDegrevement(e, 2017);
			addEtatAutreDocumentFiscalEmis(dg, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRappele(dg, dateRappel);
			addDelaiAutreDocumentFiscal(dg, dateTraitement, dateDelai, EtatDelaiDocumentFiscal.ACCORDE);

			ids.doc = dg.getId();

			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final RegDate dateDemande = RegDate.get();
		final RegDate dateNouveauDelai = dateDemande.addDays(20);
		SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy");

		final ResultActions res = m.perform(get("/degrevement-exoneration/delai/ajouter.do").param("id", String.valueOf(ids.doc))
				.param("delaiAccordeAu", formater.format(dateNouveauDelai.asJavaDate()))
				.param("dateDemande", formater.format(dateDemande.asJavaDate())));
		res.andExpect(redirectedUrl("/degrevement-exoneration/edit-demande-degrevement.do?id=" + ids.doc));
	}
}