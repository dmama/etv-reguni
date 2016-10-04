package ch.vd.uniregctb.annonceIDE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.displaytag.util.ParamEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.common.MockMessageSource;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceOrganisationService;
import ch.vd.uniregctb.tiers.TiersMapHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AnnonceIDEControllerTest {

	private static final AnnonceIDEData.InfoServiceIDEObligEtenduesImpl SERVICE_UNIREG =
			new AnnonceIDEData.InfoServiceIDEObligEtenduesImpl(RCEntAnnonceIDEHelper.NO_IDE_SERVICE_IDE,
			                                                   RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG,
			                                                   RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG);

	private TiersMapHelper tiersMapHelper;

	@Before
	public void setUp() throws Exception {
		tiersMapHelper = new TiersMapHelper();
		tiersMapHelper.setMessageSourceAccessor(new MockMessageSource());
	}

	/**
	 * Ce test vérifie que la pagination de la table de résultat fonctionne correctement.
	 */
	@Test
	public void testFindPagination() throws Exception {

		// la première page de résultats
		final AnnonceIDEData.UtilisateurImpl testuser = new AnnonceIDEData.UtilisateurImpl("testuser", null);
		final List<AnnonceIDE> firstPage = Arrays.<AnnonceIDE>asList(
				new AnnonceIDE(1L, TypeAnnonce.CREATION, DateHelper.getDate(2000, 1, 1), testuser, TypeDeSite.ETABLISSEMENT_PRINCIPAL,
				               new AnnonceIDEData.StatutImpl(StatutAnnonce.ACCEPTE_IDE, DateHelper.getDate(2000, 1, 3), null), SERVICE_UNIREG),
				new AnnonceIDE(2L, TypeAnnonce.CREATION, DateHelper.getDate(2001, 4, 10), testuser, TypeDeSite.ETABLISSEMENT_PRINCIPAL,
				               new AnnonceIDEData.StatutImpl(StatutAnnonce.REFUSE_IDE, DateHelper.getDate(2001, 4, 23), null), SERVICE_UNIREG)
		);

		// la seconde page de résultats
		final List<AnnonceIDE> secondPage = Collections.<AnnonceIDE>singletonList(
				new AnnonceIDE(3L, TypeAnnonce.MUTATION, DateHelper.getDate(2002, 7, 11), testuser, TypeDeSite.ETABLISSEMENT_SECONDAIRE,
				               new AnnonceIDEData.StatutImpl(StatutAnnonce.REJET_RCENT, DateHelper.getDate(2002, 7, 12), null), SERVICE_UNIREG)
		);

		// on service organisation qui retourne les deux pages ci-dessus
		final MockServiceOrganisationService organisationService = new MockServiceOrganisationService() {
			@NotNull
			@Override
			public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws
					ServiceOrganisationException {
				if (pageNumber == 0) {
					return new PageImpl<>(firstPage, null, 3);
				}
				else if (pageNumber == 1) {
					return new PageImpl<>(secondPage, null, 3);
				}
				else {
					return new PageImpl<>(Collections.<AnnonceIDE>emptyList());
				}
			}
		};

		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setTiersMapHelper(tiersMapHelper);
		controller.setOrganisationService(organisationService);

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// l'encoder compatible avec le tag <display:table>
		final ParamEncoder paramEncoder = new ParamEncoder("annonce");

		// on appelle la première page
		{
			final ResultActions resActions = m.perform(get("/annonceIDE/find.do").param("resultsPerPage", "2").param(paramEncoder.encodeParameterName("p"), "1"));
			resActions.andExpect(status().isOk());

			final MvcResult result = resActions.andReturn();
			assertNotNull(result);
			final Map<String, Object> model = result.getModelAndView().getModel();
			assertEquals(3, model.get("totalElements"));

			final Page<?> page = (Page<?>) model.get("page");
			assertNotNull(page);
			assertEquals(2, page.getNumberOfElements());
			assertEquals(3, page.getTotalElements());
			assertEquals(Long.valueOf(1L), ((AnnonceIDEView) page.getContent().get(0)).getNumero());
			assertEquals(Long.valueOf(2L), ((AnnonceIDEView) page.getContent().get(1)).getNumero());
		}

		// on appelle la seconde page
		{
			final ResultActions resActions = m.perform(get("/annonceIDE/find.do").param("resultsPerPage", "2").param(paramEncoder.encodeParameterName("p"), "2"));
			resActions.andExpect(status().isOk());

			final MvcResult result = resActions.andReturn();
			assertNotNull(result);
			final Map<String, Object> model = result.getModelAndView().getModel();
			assertEquals(3, model.get("totalElements"));

			final Page<?> page = (Page<?>) model.get("page");
			assertNotNull(page);
			assertEquals(1, page.getNumberOfElements());
			assertEquals(3, page.getTotalElements());
			assertEquals(Long.valueOf(3L), ((AnnonceIDEView) page.getContent().get(0)).getNumero());
		}
	}

}