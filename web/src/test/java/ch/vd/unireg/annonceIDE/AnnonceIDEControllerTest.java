package ch.vd.unireg.annonceIDE;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDEDAO;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationCriteria;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.service.mock.MockServiceOrganisationService;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.type.TypeEvenementOrganisation;

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
		controller.setReferenceAnnonceIDEDAO(new MockDummyReferenceAnnonceIDEDAO());
		controller.setEvtOrganisationDAO(new MockDummyEvenementOrganisationDAO());

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

		// on service organisation qui ne trouve aucun annonce
		final MockServiceOrganisationService organisationService = new MockServiceOrganisationService() {
			@Nullable
			@Override
			public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) {
				return null;
			}
		};

		// on crée le contrôleur et le mock MVC
		final AnnonceIDEController controller = new AnnonceIDEController();
		controller.setOrganisationService(organisationService);

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

		// on service organisation qui retourne une annonce
		final MockServiceOrganisationService organisationService = new MockServiceOrganisationService() {
			@Nullable
			@Override
			public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) {
				final AnnonceIDEData.UtilisateurImpl testuser = new AnnonceIDEData.UtilisateurImpl("testuser", null);
				final AnnonceIDE annonce = new AnnonceIDE(1L, TypeAnnonce.CREATION, dateAnnonce, testuser, TypeDeSite.ETABLISSEMENT_PRINCIPAL,
				                                          new AnnonceIDEData.StatutImpl(StatutAnnonce.ACCEPTE_IDE, DateHelper.getDate(2000, 1, 3), null), SERVICE_UNIREG);
				annonce.setNoIde(NumeroIDE.valueOf(333111333));
				annonce.setNoIdeRemplacant(NumeroIDE.valueOf(222000555));
				annonce.setNoIdeEtablissementPrincipal(NumeroIDE.valueOf(999444777));
				annonce.setCommentaire("Que voilà un joli commentaire !");

				annonce.setInformationOrganisation(new AnnonceIDEData.InformationOrganisationImpl(22334455L, 22334466L, 11003355L));

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
		controller.setOrganisationService(organisationService);

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
		assertEquals(TypeDeSite.ETABLISSEMENT_PRINCIPAL, annonce.getTypeDeSite());
		assertEquals("CHE-333.111.333", annonce.getNoIde());
		assertEquals("CHE-222.000.555", annonce.getNoIdeRemplacant());
		assertEquals("CHE-999.444.777", annonce.getNoIdeEtablissementPrincipal());
		assertNull(annonce.getRaisonDeRadiation());
		assertEquals("Que voilà un joli commentaire !", annonce.getCommentaire());

		final InformationOrganisationView info = annonce.getInformationOrganisation();
		assertEquals(Long.valueOf(22334455L), info.getNumeroSite());
		assertEquals(Long.valueOf(22334466L), info.getNumeroOrganisation());
		assertEquals(Long.valueOf(11003355L), info.getNumeroSiteRemplacant());

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
		controller.setOrganisationService(new MockServiceOrganisationService(){
			@NotNull
			@Override
			public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {
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
		final AnnonceIDEQueryView query = (AnnonceIDEQueryView) model.get("view");
		assertNotNull(query);

		assertEquals(Long.valueOf(123456789L), query.getCantonalId());
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

	public static class MockDummyEvenementOrganisationDAO implements EvenementOrganisationDAO {
		@Override
		public List<EvenementOrganisation> getEvenementsOrganisationNonTraites(long noOrganisation) {
			return null;
		}

		@Override
		public List<EvenementOrganisation> getEvenementsOrganisation(long noOrganisation) {
			return null;
		}

		@Override
		public List<EvenementOrganisation> getEvenementsOrganisationARelancer() {
			return null;
		}

		@NotNull
		@Override
		public List<EvenementOrganisation> getEvenementsOrganisationApresDateNonAnnules(Long noOrganisation, RegDate date) {
			return null;
		}

		@Override
		public Set<Long> getOrganisationsConcerneesParEvenementsPourRetry() {
			return null;
		}

		@Override
		public List<EvenementOrganisation> find(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion, @Nullable ParamPagination paramPagination) {
			return null;
		}

		@Override
		public List<EvenementOrganisation> getEvenementsForNoEvenement(long noEvenement) {
			return null;
		}

		@Override
		public EvenementOrganisation getEvenementForNoAnnonceIDE(long noAnnonce) {
			return null;
		}

		@Override
		public List<EvenementOrganisation> getEvenementsForBusinessId(String businessId) {
			return null;
		}

		@Override
		public int count(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion) {
			return 0;
		}

		@Override
		public boolean isEvenementDateValeurDansLePasse(EvenementOrganisation event) {
			return false;
		}

		@NotNull
		@Override
		public List<EvenementOrganisation> evenementsPourDateValeurEtOrganisation(RegDate date, Long noOrganisation) {
			return null;
		}

		@Override
		public List<EvenementOrganisation> getAll() {
			return null;
		}

		@Override
		public EvenementOrganisation get(Long id) {
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
		public EvenementOrganisation save(EvenementOrganisation object) {
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
		public Iterator<EvenementOrganisation> iterate(String query) {
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