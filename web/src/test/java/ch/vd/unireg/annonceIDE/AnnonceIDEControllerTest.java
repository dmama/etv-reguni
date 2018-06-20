package ch.vd.unireg.annonceIDE;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.hibernate.FlushMode;
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
import org.springframework.web.util.NestedServletException;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.MockMessageSource;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseCriteria;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseDAO;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDEDAO;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.NumeroIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.service.mock.MockServiceEntreprise;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AnnonceIDEControllerTest {

	private static final AnnonceIDEData.InfoServiceIDEObligEtenduesImpl SERVICE_UNIREG =
			new AnnonceIDEData.InfoServiceIDEObligEtenduesImpl(RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS,
			                                                   RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG,
			                                                   RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG);

	private TiersMapHelper tiersMapHelper;

	@Before
	public void setUp() throws Exception {
		tiersMapHelper = new TiersMapHelper();
		tiersMapHelper.setMessageSource(new MockMessageSource());
	}

	/**
	 * Ce test vérifie que la pagination de la table de résultat fonctionne correctement.
	 */
	@Test
	public void testFindPagination() throws Exception {

		// la première page de résultats
		final AnnonceIDEData.UtilisateurImpl testuser = new AnnonceIDEData.UtilisateurImpl("testuser", null);
		final List<AnnonceIDE> firstPage = Arrays.<AnnonceIDE>asList(
				new AnnonceIDE(1L, TypeAnnonce.CREATION, DateHelper.getDate(2000, 1, 1), testuser, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL,
				               new AnnonceIDEData.StatutImpl(StatutAnnonce.ACCEPTE_IDE, DateHelper.getDate(2000, 1, 3), null), SERVICE_UNIREG),
				new AnnonceIDE(2L, TypeAnnonce.CREATION, DateHelper.getDate(2001, 4, 10), testuser, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL,
				               new AnnonceIDEData.StatutImpl(StatutAnnonce.REFUSE_IDE, DateHelper.getDate(2001, 4, 23), null), SERVICE_UNIREG)
		);

		// la seconde page de résultats
		final List<AnnonceIDE> secondPage = Collections.<AnnonceIDE>singletonList(
				new AnnonceIDE(3L, TypeAnnonce.MUTATION, DateHelper.getDate(2002, 7, 11), testuser, TypeEtablissementCivil.ETABLISSEMENT_SECONDAIRE,
				               new AnnonceIDEData.StatutImpl(StatutAnnonce.REJET_RCENT, DateHelper.getDate(2002, 7, 12), null), SERVICE_UNIREG)
		);

		// on service entreprise qui retourne les deux pages ci-dessus
		final MockServiceEntreprise serviceEntreprise = new MockServiceEntreprise() {
			@NotNull
			@Override
			public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws
					ServiceEntrepriseException {
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
		controller.setServiceEntreprise(serviceEntreprise);
		controller.setReferenceAnnonceIDEDAO(new MockDummyReferenceAnnonceIDEDAO());
		controller.setEvtEntrepriseDAO(new MockDummyEvenementEntrepriseDAO());

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

	/**
	 * Ce test vérifie que le contrôleur lève bien une exception si on lui demande d'afficher une annonce qui n'existe pas.
	 */
	@Test
	public void testVisuObjectNotFound() throws Exception {

		// on service entreprise qui ne trouve aucun annonce
		final MockServiceEntreprise serviceEntreprise = new MockServiceEntreprise() {
			@Nullable
			@Override
			public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) {
				return null;
			}
		};

		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setServiceEntreprise(serviceEntreprise);

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// on fait l'appel
		try {
			m.perform(get("/annonceIDE/visu.do").param("id", "2"));
			fail();
		}
		catch (NestedServletException e) {
			final ObjectNotFoundException cause = (ObjectNotFoundException) e.getCause();
			assertEquals("Aucune demande ne correspond à l'identifiant 2", cause.getMessage());
		}
	}

	/**
	 * Ce test vérifie que le contrôleur affiche bien une annonce qui existe.
	 */
	@Test
	public void testVisu() throws Exception {

		final Date dateAnnonce = DateHelper.getDate(2000, 1, 1);

		// on service entreprise qui retourne une annonce
		final MockServiceEntreprise serviceEntreprise = new MockServiceEntreprise() {
			@Nullable
			@Override
			public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) {
				final AnnonceIDEData.UtilisateurImpl testuser = new AnnonceIDEData.UtilisateurImpl("testuser", null);
				final AnnonceIDE annonce = new AnnonceIDE(1L, TypeAnnonce.CREATION, dateAnnonce, testuser, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL,
				                                          new AnnonceIDEData.StatutImpl(StatutAnnonce.ACCEPTE_IDE, DateHelper.getDate(2000, 1, 3), null), SERVICE_UNIREG);
				annonce.setNoIde(NumeroIDE.valueOf(333111333));
				annonce.setNoIdeRemplacant(NumeroIDE.valueOf(222000555));
				annonce.setNoIdeEtablissementPrincipal(NumeroIDE.valueOf(999444777));
				annonce.setCommentaire("Que voilà un joli commentaire !");

				annonce.setInformationEntreprise(new AnnonceIDEData.InformationEntrepriseImpl(22334455L, 22334466L, 11003355L));

				final AdresseAnnonceIDERCEnt adresse = new AdresseAnnonceIDERCEnt();
				adresse.setRue("chemin de la date qui glisse");
				adresse.setNumero("23bis");
				adresse.setTexteCasePostale("Case postale jolie");
				adresse.setNumeroCasePostale(101);
				adresse.setNpa(1020);
				adresse.setNumeroOrdrePostal(165);
				adresse.setVille("Renens");

				final AnnonceIDEData.ContenuImpl contenu = new AnnonceIDEData.ContenuImpl();
				contenu.setNom("Ma petite entreprise");
				contenu.setNomAdditionnel("et quand même vachement importante");
				contenu.setFormeLegale(FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE);
				contenu.setSecteurActivite("Fourrage pour chef de projet");
				contenu.setAdresse(adresse);
				annonce.setContenu(contenu);

				return annonce;
			}
		};

		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setServiceEntreprise(serviceEntreprise);

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// on fait l'appel
		final ResultActions resActions = m.perform(get("/annonceIDE/visu.do").param("id", "1"));
		resActions.andExpect(status().isOk());

		final MvcResult result = resActions.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();

		// on vérifie que l'annonce est bien affichée
		final AnnonceIDEView annonce = (AnnonceIDEView) model.get("annonce");
		assertNotNull(annonce);

		assertEquals(Long.valueOf(1L), annonce.getNumero());
		assertEquals(TypeAnnonce.CREATION, annonce.getType());
		assertEquals(dateAnnonce, annonce.getDateAnnonce());
		assertEquals("testuser", annonce.getUtilisateur().getUserId());
		assertEquals("UNIREG", annonce.getServiceIDE().getApplicationName());
		assertEquals(StatutAnnonce.ACCEPTE_IDE, annonce.getStatut().getStatut());
		assertEquals(TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, annonce.getTypeEtablissementCivil());
		assertEquals("CHE-333.111.333", annonce.getNoIde());
		assertEquals("CHE-222.000.555", annonce.getNoIdeRemplacant());
		assertEquals("CHE-999.444.777", annonce.getNoIdeEtablissementPrincipal());
		assertNull(annonce.getRaisonDeRadiation());
		assertEquals("Que voilà un joli commentaire !", annonce.getCommentaire());

		final InformationEntrepriseView info = annonce.getInformationOrganisation();
		assertEquals(Long.valueOf(22334455L), info.getNumeroEtablissement());
		assertEquals(Long.valueOf(22334466L), info.getNumeroOrganisation());
		assertEquals(Long.valueOf(11003355L), info.getNumeroEtablissementRemplacant());

		final ContenuView contenu = annonce.getContenu();
		assertEquals("Ma petite entreprise", contenu.getNom());
		assertEquals("et quand même vachement importante", contenu.getNomAdditionnel());
		assertEquals(FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE, contenu.getFormeLegale());
		assertEquals("Fourrage pour chef de projet", contenu.getSecteurActivite());

		final AdresseAnnonceIDEView adresse = contenu.getAdresse();
		assertNull(adresse.getEgid());
		assertEquals("chemin de la date qui glisse", adresse.getRue());
		assertEquals("23bis", adresse.getNumero());
		assertNull(adresse.getNumeroAppartement());
		assertEquals("Case postale jolie", adresse.getTexteCasePostale());
		assertEquals(Integer.valueOf(101), adresse.getNumeroCasePostale());
		assertEquals(Integer.valueOf(1020), adresse.getNpa());
		assertEquals("Renens", adresse.getVille());
		assertNull(adresse.getPays());
	}

	/**
	 * Ce test vérifie que le contrôleur supporte bien de recevoir des numéros cantonaux formattés (123-456-789)
	 */
	@Test
	public void testParseCantonalId() throws Exception {

		final Date dateAnnonce = DateHelper.getDate(2000, 1, 1);

		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setTiersMapHelper(tiersMapHelper);
		controller.setServiceEntreprise(new MockServiceEntreprise(){
			@NotNull
			@Override
			public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {
				return new PageImpl<AnnonceIDE>(Collections.<AnnonceIDE>emptyList());
			}
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// on fait l'appel
		final ResultActions resActions = m.perform(get("/annonceIDE/find.do").param("cantonalId", "123-456-789"));
		resActions.andExpect(status().isOk());

		final MvcResult result = resActions.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();

		// on vérifie que l'annonce est bien affichée
		final AnnonceIDEQueryView view = (AnnonceIDEQueryView) model.get("view");
		assertNotNull(view);

		assertEquals(Long.valueOf(123456789L), view.getCantonalId());
	}

	/**
	 * [SIFISCBS-83] Teste que le controlleur initie un tri par défaut
	 */
	@Test
	public void testDefaultTriColonne() throws Exception {
		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setTiersMapHelper(tiersMapHelper);
		controller.setServiceEntreprise(new MockServiceEntreprise(){
			@NotNull
			@Override
			public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {
				return new PageImpl<AnnonceIDE>(Collections.<AnnonceIDE>emptyList());
			}
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// on fait l'appel
		final ResultActions resActions = m.perform(get("/annonceIDE/find.do"));
		resActions.andExpect(status().isOk());

		final MvcResult result = resActions.andReturn();
		assertNotNull(result);

		// vérification que l'annonce est bien affichée
		final Map<String, Object> model = result.getModelAndView().getModel();
		final Page<AnnonceIDEView> page = (Page<AnnonceIDEView>) model.get("page");
		assertNotNull(page);
		assertNotNull(page.getSort());

		// Vérification du tri par défaut: "noticeRequestId" décroissant
		Sort.Order order = page.getSort().getOrderFor("noticeRequestId");
		assertNotNull(order);
		assertEquals("DESC", order.getDirection().name());
	}

	/**
	 * [SIFISCBS-83] Teste que le controlleur traite bien le tri sur une colonne
	 */
	@Test
	public void testTriColonne() throws Exception {
		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setTiersMapHelper(tiersMapHelper);
		controller.setServiceEntreprise(new MockServiceEntreprise(){
			@NotNull
			@Override
			public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {
				return new PageImpl<AnnonceIDE>(Collections.<AnnonceIDE>emptyList());
			}
		});

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		// Encodage des paramètres passés dans l'url
		ParamEncoder encoder = new ParamEncoder("annonce");
		String orderParam = encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);
		String sortParam  = encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);

		// on fait l'appel (Paramètre ordre : 1 = ASC, 2 = DESC)
		final ResultActions resActions = m.perform(get("/annonceIDE/find.do")
				.param(orderParam, "1")
				.param(sortParam, "date"));

		resActions.andExpect(status().isOk());

		final MvcResult result = resActions.andReturn();
		assertNotNull(result);

		// vérification que l'annonce est bien affichée
		final Map<String, Object> model = result.getModelAndView().getModel();
		final Page<AnnonceIDEView> page = (Page<AnnonceIDEView>) model.get("page");
		assertNotNull(page);
		assertNotNull(page.getSort());

		// Vérification du tri par défaut: "noticeRequestId" décroissant
		Sort.Order order = page.getSort().getOrderFor("date");
		assertNotNull(order);
		assertEquals("ASC", order.getDirection().name());
	}

	public static class MockDummyReferenceAnnonceIDEDAO implements ReferenceAnnonceIDEDAO {

		@Override
		public List<ReferenceAnnonceIDE> getReferencesAnnonceIDE(long etablissementId) {
			return null;
		}

		@Override
		public ReferenceAnnonceIDE getLastReferenceAnnonceIDE(long etablissementId) {
			return null;
		}

		@Override
		public List<ReferenceAnnonceIDE> getAll() {
			return null;
		}

		@Override
		public ReferenceAnnonceIDE get(Long id) {
			return null;
		}

		@Override
		public boolean exists(Long id) {
			return false;
		}

		@Override
		public boolean exists(Long id, FlushMode flushModeOverride) {
			return false;
		}

		@Override
		public ReferenceAnnonceIDE save(ReferenceAnnonceIDE object) {
			return null;
		}

		@Override
		public Object saveObject(Object object) {
			return null;
		}

		@Override
		public void remove(Long id) {

		}

		@Override
		public void removeAll() {

		}

		@Override
		public Iterator<ReferenceAnnonceIDE> iterate(String query) {
			return null;
		}

		@Override
		public int getCount(Class<?> clazz) {
			return 0;
		}

		@Override
		public void clearSession() {

		}

		@Override
		public void evict(Object o) {

		}
	}

	public static class MockDummyEvenementEntrepriseDAO implements EvenementEntrepriseDAO {
		@Override
		public List<EvenementEntreprise> getEvenementsNonTraites(long noEntrepriseCivile) {
			return null;
		}

		@Override
		public List<EvenementEntreprise> getEvenements(long noEntrepriseCivile) {
			return null;
		}

		@Override
		public List<EvenementEntreprise> getEvenementsARelancer() {
			return null;
		}

		@NotNull
		@Override
		public List<EvenementEntreprise> getEvenementsApresDateNonAnnules(Long noEntrepriseCivile, RegDate date) {
			return null;
		}

		@Override
		public Set<Long> getNosEntreprisesCivilesConcerneesParEvenementsPourRetry() {
			return null;
		}

		@Override
		public List<EvenementEntreprise> find(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion, @Nullable ParamPagination paramPagination) {
			return null;
		}

		@Override
		public List<EvenementEntreprise> getEvenementsForNoEvenement(long noEvenement) {
			return null;
		}

		@Override
		public EvenementEntreprise getEvenementForNoAnnonceIDE(long noAnnonce) {
			return null;
		}

		@Override
		public List<EvenementEntreprise> getEvenementsForBusinessId(String businessId) {
			return null;
		}

		@Override
		public int count(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion) {
			return 0;
		}

		@Override
		public boolean isEvenementDateValeurDansLePasse(EvenementEntreprise event) {
			return false;
		}

		@NotNull
		@Override
		public List<EvenementEntreprise> evenementsPourDateValeurEtEntreprise(RegDate date, Long noEntrepriseCivile) {
			return null;
		}

		@Override
		public List<EvenementEntreprise> getAll() {
			return null;
		}

		@Override
		public EvenementEntreprise get(Long id) {
			return null;
		}

		@Override
		public boolean exists(Long id) {
			return false;
		}

		@Override
		public boolean exists(Long id, FlushMode flushModeOverride) {
			return false;
		}

		@Override
		public EvenementEntreprise save(EvenementEntreprise object) {
			return null;
		}

		@Override
		public Object saveObject(Object object) {
			return null;
		}

		@Override
		public void remove(Long id) {

		}

		@Override
		public void removeAll() {

		}

		@Override
		public Iterator<EvenementEntreprise> iterate(String query) {
			return null;
		}

		@Override
		public int getCount(Class<?> clazz) {
			return 0;
		}

		@Override
		public void clearSession() {

		}

		@Override
		public void evict(Object o) {

		}
	}
}