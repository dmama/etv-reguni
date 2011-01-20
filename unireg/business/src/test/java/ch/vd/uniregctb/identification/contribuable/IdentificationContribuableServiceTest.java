package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse.TypeAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.DemandeHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.evenement.identification.contribuable.EsbHeader;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests du service (qu'attendiez-vous d'autre ?).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings( {
		"FieldCanBeLocal", "JavaDoc"
})
public class IdentificationContribuableServiceTest extends BusinessTest {

	public IdentificationContribuableServiceTest() {
		setWantIndexation(true);
	}

	/**
	 * Fake message handler pour intercepter les réponses émises.
	 */
	private final class TestMessageHandler implements IdentificationContribuableMessageHandler {

		private final List<IdentificationContribuable> sentMessages = new ArrayList<IdentificationContribuable>();
		private boolean throwExceptionOnSend = false;

		private TestMessageHandler() {
		}

		public void sendReponse(IdentificationContribuable message) throws Exception {
			if (throwExceptionOnSend) {
				throw new RuntimeException("Exception de test.");
			}
			sentMessages.add(message);
		}

		public void setDemandeHandler(DemandeHandler handler) {
		}

		public List<IdentificationContribuable> getSentMessages() {
			return sentMessages;
		}

		public void setThrowExceptionOnSend(boolean throwExceptionOnSend) {
			this.throwExceptionOnSend = throwExceptionOnSend;
		}
	}

	private final class PPComparator implements Comparator<PersonnePhysique> {
		public int compare(PersonnePhysique o1, PersonnePhysique o2) {
			return o1.getNumero().compareTo(o2.getNumero());
		}
	}

	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private IdentCtbDAO identCtbDAO;

	private IdentificationContribuableServiceImpl service;
	private TestMessageHandler messageHandler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		tiersService = getBean(TiersService.class, "tiersService");
		adresseService = getBean(AdresseService.class, "adresseService");
		infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		identCtbDAO = getBean(IdentCtbDAO.class, "identCtbDAO");

		service = new IdentificationContribuableServiceImpl();
		service.setSearcher(searcher);
		service.setTiersDAO(tiersDAO);
		service.setTiersService(tiersService);
		service.setAdresseService(adresseService);
		service.setInfraService(infraService);
		service.setIdentCtbDAO(identCtbDAO);

		messageHandler = new TestMessageHandler();
		service.setMessageHandler(messageHandler);
	}

	@Test
	public void testIdentifieBaseVide() {

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Dupneu"); // du contentieux
			assertEmpty(service.identifie(criteres));
		}
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("A");
			criteres.setPrenoms("B");
			criteres.setDateNaissance(date(1970, 2, 2));
			assertEmpty(service.identifie(criteres));
		}
	}

	@Test
	public void testIdentifieUnNonHabitant() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique albert = addNonHabitant("Albert", "Zweisteinen", date(1953, 4, 3), Sexe.MASCULIN);
				return albert.getNumero();
			}
		});

		globalTiersIndexer.sync();

		assertAlbertZweisteinenSeul(id);
	}

	@Test
	public void testIdentifieUnHabitant() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1953, 4, 3), "Zweisteinen", "Albert", true);
			}
		});

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique albert = addHabitant(noIndividu);
				return albert.getNumero();
			}
		});

		globalTiersIndexer.sync();

		assertAlbertZweisteinenSeul(id);
	}

	private void assertAlbertZweisteinenSeul(final Long albertId) {
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Planck");
			criteres.setPrenoms("Max");
			assertEmpty(service.identifie(criteres));
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweistein"); // l'identification sur le nom doit être stricte
			assertEmpty(service.identifie(criteres));
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Alberto"); // l'identification sur le prénom doit être stricte
			assertEmpty(service.identifie(criteres));
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setDateNaissance(date(1953, 3, 4)); // mauvaise date de naissance
			assertEmpty(service.identifie(criteres));
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweisteinen");
			criteres.setSexe(Sexe.FEMININ);
			assertEmpty(service.identifie(criteres));
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweisteinen");
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp = list.get(0);
			assertEquals(albertId, pp.getNumero());
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Albert");
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp = list.get(0);
			assertEquals(albertId, pp.getNumero());
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setDateNaissance(date(1953, 4, 3));
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(0, list.size());

		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Albert");
			criteres.setNom("Zweisteinen");
			criteres.setSexe(Sexe.MASCULIN);
			criteres.setDateNaissance(date(1953, 4, 3));
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp = list.get(0);
			assertEquals(albertId, pp.getNumero());
		}
	}

	@Test
	public void testContribuableMisAJourSuiteIdentification() throws Exception {
		final long noIndividuAlbert = 1234;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuAlbert, date(1953, 4, 3), "Zweisteinen", "Albert", true);
				addIndividu(noIndividuAnne, date(1965, 8, 13), "Zweisteinen", "Anne", false);
			}
		});

		class Ids {
			Long albert;
			Long anne;
			Long alberto;
			Long greg;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique alberto = addNonHabitant("Alberto", "Fujimorouille", null, null);
				ids.alberto = alberto.getNumero();
				return null;
			}
		});

		// Albert
		{
			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					CriteresAdresse adresse = new CriteresAdresse();

					adresse.setNpaSuisse(3018);
					adresse.setLieu("Bümpliz");
					adresse.setLigneAdresse1("Alberto el tiburon");
					adresse.setLigneAdresse2("et son épouse");
					adresse.setNoAppartement("12");
					adresse.setNoPolice("36B");
					adresse.setRue("Chemin de la strasse verte");
					adresse.setTypeAdresse(TypeAdresse.SUISSE);
					CriteresPersonne criteres = new CriteresPersonne();
					criteres.setPrenoms("Alberto");
					criteres.setNom("Fujimori");
					criteres.setNAVS13("123654798123");
					criteres.setDateNaissance(date(1953, 12, 3));
					criteres.setAdresse(adresse);
					criteres.setSexe(Sexe.MASCULIN);
					IdentificationContribuable message = createDemandeFromCanton(criteres, "2-BE-5");
					message.setLogCreationDate(RegDate.get().asJavaDate());
					final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
					service.forceIdentification(message, alberto, Etat.TRAITE_MANUELLEMENT);
					return null;
				}
			});

		}


		{
			final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
			assertEquals(alberto.getNumeroAssureSocial(),"123654798123");
			/*assertEquals(alberto.getNom(),"Fujimori");
			assertEquals(alberto.getDateNaissance(),date(1953, 12, 3));
			assertEquals(alberto.getSexe(),Sexe.MASCULIN);
			AdresseSuisse adresse = (AdresseSuisse) alberto.getAdresseActive(TypeAdresseTiers.COURRIER,null);
			assertEquals(adresse.getNumeroAppartement(),"12");
			assertEquals(adresse.getRue(),"Chemin de la strasse verte");
			assertEquals(adresse.getComplement(),"Alberto el tiburon"+" "+"et son épouse");
			assertEquals(adresse.getNumeroMaison(),"36B");     */

		}

	}



	@Test
	public void testContribuableSurNPA() throws Exception {
		final long noIndividuClaude = 151658 ;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, date(1900, 1, 1), "Rosat", "Claude", true);
				addAdresse(indClaude, TypeAdresseCivil.COURRIER, "Rue du moulin", "12", 1148, MockLocalite.LIsle, null, RegDate.get(2000, 12, 1), null);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique habClaude = addHabitant(noIndividuClaude);
				ids.claude = habClaude.getNumero();
				return null;
			}
		});

		globalTiersIndexer.sync();

		assertCountDemandes(0);

		// création et traitement du message d'identification
		CriteresAdresse adresse = new CriteresAdresse();
		adresse.setNpaSuisse(1148);
		adresse.setLieu("L'Isle");
		adresse.setLigneAdresse1("");
		adresse.setLigneAdresse2("et son épouse");
		adresse.setNoAppartement("12");
		adresse.setNoPolice("36B");
		adresse.setRue("Chemin de la strasse verte");
		adresse.setTypeAdresse(TypeAdresse.SUISSE);
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(date(1900, 1, 1));
		criteres.setAdresse(adresse);

		final IdentificationContribuable message = createDemandeFromCanton(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// claude doit avoir été trouvée, et traitée automatiquement
		final List<IdentificationContribuable> list = identCtbDAO.getAll();
		assertEquals(1, list.size());

		final IdentificationContribuable ic = list.get(0);
		assertNotNull(ic);
		assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, ic.getEtat());
		assertEquals(Integer.valueOf(1), ic.getNbContribuablesTrouves());

		final Reponse reponse = ic.getReponse();
		assertNotNull(reponse);
		assertNull(reponse.getErreur());
		assertEquals(ids.claude, reponse.getNoContribuable());

		// La demande doit avoir reçu une réponse automatiquement
		assertEquals(1, messageHandler.getSentMessages().size());
		final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
		assertEquals(ic.getId(), sent.getId());

	}

	@Test
	public void testIdentificationAvecDate1900() throws Exception {
		final long noIndividuAlbert = 1234;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuAlbert, date(1953, 4, 3), "Zweisteinen", "Albert", true);
				addIndividu(noIndividuAnne, date(1965, 8, 13), "Zweisteinen", "Anne", false);
			}
		});

		class Ids {
			Long albert;
			Long anne;
			Long alberto;
			Long greg;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique alberto = addNonHabitant("Alberto", "Fujimorouille", null, null);
				ids.alberto = alberto.getNumero();
				return null;
			}
		});

		// Albert
		{
			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					CriteresAdresse adresse = new CriteresAdresse();

					adresse.setNpaSuisse(3018);
					adresse.setLieu("Bümpliz");
					adresse.setLigneAdresse1("Alberto el tiburon");
					adresse.setLigneAdresse2("et son épouse");
					adresse.setNoAppartement("12");
					adresse.setNoPolice("36B");
					adresse.setRue("Chemin de la strasse verte");
					adresse.setTypeAdresse(TypeAdresse.SUISSE);
					CriteresPersonne criteres = new CriteresPersonne();
					criteres.setPrenoms("Alberto");
					criteres.setNom("Fujimori");
					criteres.setNAVS13("123654798123");
					criteres.setDateNaissance(date(1953, 12, 3));
					criteres.setAdresse(adresse);
					criteres.setSexe(Sexe.MASCULIN);
					IdentificationContribuable message = createDemandeFromCanton(criteres, "2-BE-5");
					message.setLogCreationDate(RegDate.get().asJavaDate());
					final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
					service.forceIdentification(message, alberto, Etat.TRAITE_MANUELLEMENT);
					return null;
				}
			});

		}


		{
			final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
			assertEquals(alberto.getNumeroAssureSocial(),"123654798123");
			/*assertEquals(alberto.getNom(),"Fujimori");
			assertEquals(alberto.getDateNaissance(),date(1953, 12, 3));
			assertEquals(alberto.getSexe(),Sexe.MASCULIN);
			AdresseSuisse adresse = (AdresseSuisse) alberto.getAdresseActive(TypeAdresseTiers.COURRIER,null);
			assertEquals(adresse.getNumeroAppartement(),"12");
			assertEquals(adresse.getRue(),"Chemin de la strasse verte");
			assertEquals(adresse.getComplement(),"Alberto el tiburon"+" "+"et son épouse");
			assertEquals(adresse.getNumeroMaison(),"36B");       */

		}

	}

	@Test
	public void testIdentifiePlusieursPersonnesPhysiques() throws Exception {

		final long noIndividuAlbert = 1234;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuAlbert, date(1953, 4, 3), "Zweisteinen", "Albert", true);
				addIndividu(noIndividuAnne, date(1965, 8, 13), "Zweisteinen", "Anne", false);
			}
		});

		class Ids {
			Long albert;
			Long anne;
			Long alberto;
			Long greg;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Albert, le seul, le vrai
				final PersonnePhysique albert = addHabitant(noIndividuAlbert);
				ids.albert = albert.getNumero();

				// même nom de famille qu'Albert
				final PersonnePhysique anne = addHabitant(noIndividuAnne);
				ids.anne = anne.getNumero();

				// prénom similaire à celui d'Albert
				final PersonnePhysique alberto = addNonHabitant("Alberto", "Fujimori", date(1953, 12, 3), Sexe.MASCULIN);
				ids.alberto = alberto.getNumero();

				// même jour de naissance qu'Albert
				final PersonnePhysique greg = addNonHabitant("Grégoire", "Duchemolle", date(1953, 4, 3), Sexe.MASCULIN);
				ids.greg = greg.getNumero();

				return null;
			}
		});

		globalTiersIndexer.sync();

		// Albert
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Albert");
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp = list.get(0);
			assertEquals(ids.albert, pp.getNumero());
		}

		// Albert toujours
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweisteinen");
			criteres.setSexe(Sexe.MASCULIN);
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp = list.get(0);
			assertEquals(ids.albert, pp.getNumero());
		}

		// Les deux Zweisteinen
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweisteinen");
			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());
			Collections.sort(list, new PPComparator());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.albert, pp0.getNumero());

			final PersonnePhysique pp1 = list.get(1);
			assertEquals(ids.anne, pp1.getNumero());
		}


	}

	@Test
	public void testGetNomCantonFromMessage() throws Exception{

				CriteresAdresse adresse = new CriteresAdresse();

				adresse.setNpaSuisse(3018);
				adresse.setLieu("Bümpliz");
				adresse.setLigneAdresse1("Alberto el tiburon");
				adresse.setLigneAdresse2("et son épouse");
				adresse.setNoAppartement("12");
				adresse.setNoPolice("36B");
				adresse.setRue("Chemin de la strasse verte");
				adresse.setTypeAdresse(TypeAdresse.SUISSE);
				CriteresPersonne criteres = new CriteresPersonne();
				criteres.setPrenoms("Alberto");
				criteres.setNom("Fujimori");
				criteres.setNAVS13("123654798123");
				criteres.setDateNaissance(date(1953, 12, 3));
				criteres.setAdresse(adresse);
				criteres.setSexe(Sexe.MASCULIN);
				IdentificationContribuable message = createDemandeFromCanton(criteres, "2-BE-5");
				message.setLogCreationDate(RegDate.get().asJavaDate());

				String nomCanton =service.getNomCantonFromEmetteurId(message.getDemande().getEmetteurId());
				assertEquals("Berne",nomCanton);

				message = createDemandeFromCanton(criteres, "2-SS-5");
				nomCanton =service.getNomCantonFromEmetteurId(message.getDemande().getEmetteurId());
				assertEquals("2-SS-5",nomCanton);


	}


	@Test
	public void testIdentifieParAdresse() throws Exception {

		class Ids {
			Long robert;
			Long jeanne;
			Long luc;
			Long michel;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique robert = addNonHabitant("Robert", "Nicoud", date(1953, 4, 3), Sexe.MASCULIN);
				addAdresseSuisse(robert, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
				ids.robert = robert.getNumero();

				final PersonnePhysique jeanne = addNonHabitant("Jeanne", "Nicoud", date(1971, 11, 25), Sexe.FEMININ);
				addAdresseSuisse(jeanne, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Lausanne.AvenueDeMarcelin);
				ids.jeanne = jeanne.getNumero();

				final PersonnePhysique luc = addNonHabitant("Luc", "Haddoque", date(1952, 7, 29), Sexe.MASCULIN);
				addAdresseSuisse(luc, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Orbe.GrandRue);
				ids.luc = luc.getNumero();

				final PersonnePhysique michel = addNonHabitant("Michel", "Haddoque", date(1962, 4, 25), Sexe.MASCULIN);
				addAdresseSuisse(michel, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Vallorbe.GrandRue);
				ids.michel = michel.getNumero();
				return null;
			}
		});

		globalTiersIndexer.sync();

		// Robert et Jeanne
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Nicoud");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1000);
			criteres.setAdresse(adresse);

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());
			Collections.sort(list, new PPComparator());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.robert, pp0.getNumero());

			final PersonnePhysique pp1 = list.get(1);
			assertEquals(ids.jeanne, pp1.getNumero());
		}

		// Robert
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Nicoud");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1000);
			criteres.setAdresse(adresse);

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());


		}

		// Jeanne
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Jeanne");
			criteres.setNom("Nicoud");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1000);
			criteres.setAdresse(adresse);

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.jeanne, pp0.getNumero());
		}

		// Luc et Michel
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Haddoque");

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());
			Collections.sort(list, new PPComparator());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.luc, pp0.getNumero());

			final PersonnePhysique pp1 = list.get(1);
			assertEquals(ids.michel, pp1.getNumero());
		}

		// Luc et Michel, encore mais c'est luc qui gagne
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Haddoque");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1350);
			criteres.setAdresse(adresse);

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());
			Collections.sort(list, new PPComparator());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.luc, pp0.getNumero());


		}



		// Michel
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Haddoque");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1337);
			criteres.setAdresse(adresse);

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.michel, pp0.getNumero());
		}
	}

	@Test
	public void testIdentifieSansAdresse() throws Exception {

		class Ids {
			Long robert;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique robert = addNonHabitant("Robert", "Nicoud", date(1953, 4, 3), Sexe.MASCULIN);
				ids.robert = robert.getNumero();

				return null;
			}
		});

		globalTiersIndexer.sync();

		// Robert
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Nicoud");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1000);
			criteres.setAdresse(adresse);

			final List<PersonnePhysique> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());
			Collections.sort(list, new PPComparator());

			final PersonnePhysique pp0 = list.get(0);
			assertEquals(ids.robert, pp0.getNumero());


		}


	}

	@NotTransactional
	@Test
	public void testHandleDemandeZeroContribuableTrouve() throws Exception {

		// la base est vide
		assertCountDemandes(0);

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Arnold", "Duchoux");
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Arnold Duchoux ne doit pas être trouvé
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());

				// Pas de réponse automatique
				assertEmpty(messageHandler.getSentMessages());
				
				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testHandleDemandeUnContribuableTrouve() throws Exception {

		// création d'un contribuable
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Zora", "Larousse");
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				
				// Zora doit avoir été trouvée, et traitée automatiquement
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(1), ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNotNull(reponse);
				assertNull(reponse.getErreur());
				assertEquals(id, reponse.getNoContribuable());

				// La demande doit avoir reçu une réponse automatiquement
				assertEquals(1, messageHandler.getSentMessages().size());
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());

				return null;
			}
		});

	}

	@NotTransactional
	@Test
	public void testHandleDemande_SANS_MANUEL() throws Exception {

		// création d'un contribuable
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Zouzou", "LaVerte",Demande.ModeIdentificationType.SANS_MANUEL);
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Zora n'est pas trouvée
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.NON_IDENTIFIE, ic.getEtat());
				assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNotNull(reponse);
				assertNotNull(reponse.getErreur());


				// La demande doit avoir reçu une réponse automatiquement
				assertEquals(1, messageHandler.getSentMessages().size());
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());

				return null;
			}
		});

	}

	@NotTransactional
	@Test
	public void testHandleDemande_MANUEL_AVEC_ACK() throws Exception {

		// création d'un contribuable
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Zouzou", "LaVerte",Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Zora n'est pas trouvé
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNotNull(reponse);
				Assert.assertTrue(reponse.isEnAttenteIdentifManuel());


				// La demande doit avoir reçu une réponse automatiquement
				assertEquals(1, messageHandler.getSentMessages().size());
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());

				return null;
			}
		});

	}


	@NotTransactional
	@Test
	public void testHandleDemande_MANUEL_SANS_ACK() throws Exception {

		// création d'un contribuable
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Zouzou", "LaVerte",Demande.ModeIdentificationType.MANUEL_SANS_ACK);
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Zora n'est pas trouvé
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNull(reponse);
				// Pas de réponse automatique
				assertEmpty(messageHandler.getSentMessages());				
				return null;
			}
		});

	}
	@NotTransactional
	@Test
	public void testHandleDemandePlusieursContribuablesTrouves() throws Exception {

		// création de plusieurs contribuables
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addNonHabitant("Petit", "Larousse", date(1970, 4, 3), Sexe.MASCULIN);
				addNonHabitant("MonPremier", "Larousse", date(1970, 4, 3), Sexe.MASCULIN);
				return null;
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Larousse", "Larousse");
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				
				// Plus de un contribuable doivent avoir été trouvés, et le message doit être passé en mode manuel
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(3), ic.getNbContribuablesTrouves());

				return null;
			}
		});

		// Pas de réponse automatique
		assertEmpty(messageHandler.getSentMessages());
	}

	@NotTransactional
	@Test
	public void testHandleDemandeException() throws Exception {

		// création d'un contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return null;
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		try {
			// provoque une exception à l'envoi
			messageHandler.setThrowExceptionOnSend(true);

			// création et traitement du message d'identification
			final IdentificationContribuable message = createDemande("Zora", "Larousse");
			doInTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					service.handleDemande(message);
					return null;
				}
			});
		}
		finally {
			messageHandler.setThrowExceptionOnSend(false);
		}

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Zora doit avoir été trouvée, mais le traitement interrompu à cause de l'exception
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.EXCEPTION, ic.getEtat());
				assertNull(ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNotNull(reponse);
				assertNull(reponse.getNoContribuable());

				final Erreur erreur = reponse.getErreur();
				assertNotNull(erreur);
				assertEquals(TypeErreur.TECHNIQUE, erreur.getType());
				assertEquals("Exception de test.", erreur.getMessage());

				return null;  //To change body of implemented methods use File | Settings | File Templates.
			}
		});

		// Pas de réponse automatique
		assertEmpty(messageHandler.getSentMessages());
	}

	/**
	 * [UNIREG-1636] Vérifie qu'une demande d'identification qui contient un numéro AVS effectue quand même une recherche avec les autres
	 * critères si le numéro AVS n'est pas connu dans le registre.
	 *
	 * @throws Exception
	 *             en cas d'erreur
	 */
	@NotTransactional
	@Test
	public void testHandleDemandeAvecNumeroAVSInconnu() throws Exception {

		// création d'un contribuable
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique edouard = addNonHabitant("Edouard", "Bonhote", date(1965, 11, 3), Sexe.MASCULIN);
				addForPrincipal(edouard, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				return edouard.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Edouard", "Bonhote", "7569613127861");
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Le numéro AVS n'est pas connu, mais le contribuable doit néansmoins avoir été trouvé, et le message doit être passé en mode
				// automatique
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(1), ic.getNbContribuablesTrouves());
				assertEquals(id, ic.getReponse().getNoContribuable());

				// La demande doit avoir reçu une réponse automatiquement
				assertEquals(1, messageHandler.getSentMessages().size());
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());
				
				return null;
			}
		});
	}

	/**
	 * [UNIREG-1630] Vérifie qu'une demande d'identification qui contient un numéro AVS effectue quand même une recherche avec les autres
	 * critères pour vérifier que tous les critères correspondent.
	 *
	 * @throws Exception
	 *             en cas d'erreur
	 */
	@NotTransactional
	@Test
	public void testHandleDemandeAvecNumeroAVSConnuMaisFaux() throws Exception {

		// création d'un contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique edouard = addNonHabitant("Edouard", "Bonhote", date(1965, 11, 3), Sexe.MASCULIN);
				addForPrincipal(edouard, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				edouard.setNumeroAssureSocial("7569613127861");
				return edouard.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("George", "Pompidou", "7569613127861");
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Le numéro AVS est connu, mais les critères nom/prénom ne correspondnet pas -> on doit quand même trouver le contribuable
				// car la priorité du navs13 est la plus forte
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(1), ic.getNbContribuablesTrouves());

				// Réponse automatique
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());

				return null;
			}
		});
	}

	/**
	 * [UNIREG-1911] on retourne aussi le numéro de ménage du contribuable
	 */
	@NotTransactional
	@Test
	public void testHandleDemandeUnContribuableAvecMenageTrouve() throws Exception {

		class Ids {
			Long zora;
			Long bruno;
			Long mc;
		}
		final Ids ids = new Ids();

		// création d'un contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				final PersonnePhysique bruno = addNonHabitant("Bruno", "Larousse", date(1968, 7, 23), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(bruno, zora, date(1980, 4, 21), null);
				final MenageCommun menage = ensemble.getMenage();

				addForPrincipal(menage, RegDate.get(2009, 5, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.Aubonne);

				ids.bruno = bruno.getNumero();
				ids.zora = zora.getNumero();
				ids.mc = menage.getNumero();
				return null;
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemande("Zora", "Larousse");
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				
				// Zora doit avoir été trouvée, et traitée automatiquement
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(1), ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNotNull(reponse);
				assertNull(reponse.getErreur());
				assertEquals(ids.zora, reponse.getNoContribuable());
				assertEquals(ids.mc, reponse.getNoMenageCommun()); // [UNIREG-1911]

				// La demande doit avoir reçu une réponse automatiquement
				assertEquals(1, messageHandler.getSentMessages().size());
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());

				return null;
			}
		});
	}

	private static IdentificationContribuable createDemande(final String prenoms, final String nom) {

		return createDemande(prenoms,nom,Demande.ModeIdentificationType.MANUEL_SANS_ACK);
	}

	private static IdentificationContribuable createDemande(final String prenoms, final String nom, Demande.ModeIdentificationType mode) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms(prenoms);
		personne.setNom(nom);

		return createDemande(personne,mode);
	}

	private static IdentificationContribuable createDemande(final String prenoms, final String nom, final String noAVS13) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms(prenoms);
		personne.setNom(nom);
		personne.setNAVS13(noAVS13);

		return createDemande(personne);
	}

	
	private static IdentificationContribuable createDemandeFromCanton(CriteresPersonne personne, String emetteurId) {
		final EsbHeader header = new EsbHeader();
		header.setBusinessId("123456");
		header.setBusinessUser("Test");
		header.setReplyTo("Test");

		final Demande demande = new Demande();
		demande.setEmetteurId(emetteurId);
		demande.setMessageId("1111");
		demande.setPrioriteEmetteur(PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setModeIdentification(Demande.ModeIdentificationType.MANUEL_SANS_ACK);
		demande.setTypeMessage("ssk-3001-000101");
		demande.setDate(DateHelper.getCurrentDate());
		demande.setPeriodeFiscale(2009);
		demande.setPersonne(personne);
		demande.setTypeDemande(TypeDemande.MELDEWESEN);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);

		return message;
	}

	private static IdentificationContribuable createDemande(CriteresPersonne personne) {
		return createDemande(personne,Demande.ModeIdentificationType.MANUEL_SANS_ACK);
	}

	private static IdentificationContribuable createDemande(CriteresPersonne personne, Demande.ModeIdentificationType modeIdentification) {
		final EsbHeader header = new EsbHeader();
		header.setBusinessId("123456");
		header.setBusinessUser("Test");
		header.setReplyTo("Test");

		final Demande demande = new Demande();
		demande.setEmetteurId("Test");
		demande.setMessageId("1111");
		demande.setPrioriteEmetteur(PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setModeIdentification(modeIdentification);
		demande.setTypeMessage("ssk-3001-000101");
		demande.setDate(DateHelper.getCurrentDate());
		demande.setPeriodeFiscale(2009);
		demande.setPersonne(personne);
		demande.setTypeDemande(TypeDemande.MELDEWESEN);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);

		return message;
	}

	private void assertCountDemandes(final int count) throws Exception {
		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertEquals(count, identCtbDAO.getCount(IdentificationContribuable.class));
				return null;
			}
		});
	}
}
