package ch.vd.unireg.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeLettreBienvenue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

public class AutreDocumentFiscalControllerTest extends WebTest {

	private AutreDocumentFiscalController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		controller = getBean(AutreDocumentFiscalController.class, "autreDocumentFiscalController");
	}

	/**
	 * [SIFISC-29014] Test pour ajout d'un délai: affichage du bouton "Ajouter".
	 * Cas où le bouton s'affiche, la lettre a seulement été émise.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEditerLettreBienvenueEmise() throws Exception {

		class Ids{
			Long doc;
		}

		final Ids ids = new Ids();

		final RegDate dateEnvoiLettre = date(1990, 6, 2);

		doInNewTransaction(status -> {
			Entreprise e = addEntrepriseConnueAuCivil(33);

			LettreBienvenue lb = addLettreBienvenue(TypeLettreBienvenue.VD_RC, e);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);

			ids.doc = lb.getId();

			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final ResultActions res = m.perform(get("/autresdocs/editer.do").param("id", String.valueOf(ids.doc)));
		res.andExpect(status().isOk());

		// Vérifie que les données exposées sont bien correctes
		final MvcResult result = res.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();
		assertNotNull(model);

		// Vérification des données de la réponse
		final AutreDocumentFiscalView view = (AutreDocumentFiscalView) model.get("command");
		assertNotNull(view);
		assertEquals(ids.doc.longValue(), view.getId());

		final boolean isAjoutDelaiAutorise = (Boolean) model.get("isAjoutDelaiAutorise");
		assertEquals(true, isAjoutDelaiAutorise);
	}

	/**
	 * [SIFISC-29014] Test pour ajout d'un délai: Non affichage du bouton "Ajouter"
	 * Cas où le bouton ne s'affiche pas, car un rappel a déjà été envoyé.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEditerLettreBienvenueRappelee() throws Exception {

		class Ids{
			Long doc;
		}

		final Ids ids = new Ids();

		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateRappel = date(1990, 7, 1);

		doInNewTransaction(status -> {
			Entreprise e = addEntrepriseConnueAuCivil(33);

			LettreBienvenue lb = addLettreBienvenue(TypeLettreBienvenue.VD_RC, e);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRappele(lb, dateRappel);

			ids.doc = lb.getId();

			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final ResultActions res = m.perform(get("/autresdocs/editer.do").param("id", String.valueOf(ids.doc)));
		res.andExpect(status().isOk());

		// Vérifie que les données exposées sont bien correctes
		final MvcResult result = res.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();
		assertNotNull(model);

		// Vérification des données de la réponse
		final AutreDocumentFiscalView view = (AutreDocumentFiscalView) model.get("command");
		assertNotNull(view);
		assertEquals(ids.doc.longValue(), view.getId());

		final boolean isAjoutDelaiAutorise = (Boolean) model.get("isAjoutDelaiAutorise");
		assertEquals(false, isAjoutDelaiAutorise);
	}


	/**
	 * [SIFISC-29014] Test pour ajout d'un délai, alors qu'un rappel a déjà été envoyé.
	 * Get = Affichage
	 *
	 * @throws Exception
	 */
	@Test
	public void testAjouterDelaiLettreBienvenueRappeleeGet() throws Exception {

		class Ids{
			Long doc;
		}

		final Ids ids = new Ids();

		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateTraitement = date(1990, 7, 13);
		final RegDate dateDelai = date(1990, 7, 14);
		final RegDate dateRappel = date(1990, 7, 15);

		doInNewTransaction(status -> {
			Entreprise e = addEntrepriseConnueAuCivil(33);

			LettreBienvenue lb = addLettreBienvenue(TypeLettreBienvenue.VD_RC, e);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRappele(lb, dateRappel);
			addDelaiAutreDocumentFiscal(lb, dateTraitement, dateDelai, EtatDelaiDocumentFiscal.ACCORDE);

			ids.doc = lb.getId();

			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final ResultActions res = m.perform(get("/autresdocs/delai/ajouter.do").param("id", String.valueOf(ids.doc)));
		res.andExpect(redirectedUrl("/autresdocs/editer.do?id=" + ids.doc));
	}

	/**
	 * [SIFISC-29014] Test pour ajout d'un délai, alors qu'un rappel a déjà été envoyé.
	 * Post = action d'ajout
	 *
	 * @throws Exception
	 */
	@Test
	public void testAjouterDelaiLettreBienvenueRappeleePost() throws Exception {

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

			LettreBienvenue lb = addLettreBienvenue(TypeLettreBienvenue.VD_RC, e);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRappele(lb, dateRappel);
			addDelaiAutreDocumentFiscal(lb, dateTraitement, dateDelai, EtatDelaiDocumentFiscal.ACCORDE);

			ids.doc = lb.getId();

			return null;
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final RegDate dateDemande = RegDate.get();
		final RegDate dateNouveauDelai = dateDemande.addDays(20);
		SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy");

		final ResultActions res = m.perform(post("/autresdocs/delai/ajouter.do").param("idDocumentFiscal", String.valueOf(ids.doc))
									.param("delaiAccordeAu", formater.format(dateNouveauDelai.asJavaDate()))
									.param("dateDemande", formater.format(dateDemande.asJavaDate())));
		res.andExpect(redirectedUrl("/autresdocs/editer.do?id=" + ids.doc));
	}

}
