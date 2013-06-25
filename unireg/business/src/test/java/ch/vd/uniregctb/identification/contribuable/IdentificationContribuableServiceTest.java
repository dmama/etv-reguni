package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse.TypeAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.evenement.identification.contribuable.EsbHeader;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcherImpl;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.indexer.tiers.TopList;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests du service (qu'attendiez-vous d'autre ?).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({
		"FieldCanBeLocal", "JavaDoc"
})
public class IdentificationContribuableServiceTest extends BusinessTest {

	public IdentificationContribuableServiceTest() {
		setWantIndexation(true);
	}

	/**
	 * Fake message handler pour intercepter les réponses émises.
	 */
	private static final class TestMessageHandler implements IdentificationContribuableMessageHandler {

		private final List<IdentificationContribuable> sentMessages = new ArrayList<>();
		private boolean throwExceptionOnSend = false;

		private TestMessageHandler() {
		}

		@Override
		public void sendReponse(IdentificationContribuable message) throws Exception {
			if (throwExceptionOnSend) {
				throw new RuntimeException("Exception de test.");
			}
			sentMessages.add(message);
		}

		public List<IdentificationContribuable> getSentMessages() {
			return sentMessages;
		}

		public void setThrowExceptionOnSend(boolean throwExceptionOnSend) {
			this.throwExceptionOnSend = throwExceptionOnSend;
		}

		public void reset() {
			sentMessages.clear();
			throwExceptionOnSend = false;
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
	private IdentificationContribuableHelper helper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		tiersService = getBean(TiersService.class, "tiersService");
		adresseService = getBean(AdresseService.class, "adresseService");
		infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		identCtbDAO = getBean(IdentCtbDAO.class, "identCtbDAO");
		helper = getBean(IdentificationContribuableHelper.class, "identificationContribuableHelper");

		service = new IdentificationContribuableServiceImpl() {
			@Override
			protected boolean isUpdateCriteresOnStartup() {
				// en test, ce n'est pas la peine...
				return false;
			}
		};
		service.setSearcher(searcher);
		service.setTiersDAO(tiersDAO);
		service.setTiersService(tiersService);
		service.setAdresseService(adresseService);
		service.setInfraService(infraService);
		service.setIdentCtbDAO(identCtbDAO);
		service.setTransactionManager(transactionManager);

		messageHandler = new TestMessageHandler();
		service.setMessageHandler(messageHandler);
		service.setIdentificationContribuableHelper(helper);
		service.setFlowSearchThreadPoolSize(1);

		service.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		service.destroy();
		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieBaseVide() throws Exception {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieUnNonHabitant() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique albert = addNonHabitant("Albert", "Zweisteinen", date(1953, 4, 3), Sexe.MASCULIN);
				return albert.getNumero();
			}
		});

		globalTiersIndexer.sync();

		assertAlbertZweisteinenSeul(id);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieUnHabitant() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1953, 4, 3), "Zweisteinen", "Albert", true);
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique albert = addHabitant(noIndividu);
				return albert.getNumero();
			}
		});

		globalTiersIndexer.sync();

		assertAlbertZweisteinenSeul(id);
	}

	private void assertAlbertZweisteinenSeul(final Long albertId) throws Exception {
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
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(albertId, pp);
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Albert");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(albertId, pp);
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setDateNaissance(date(1953, 4, 3));
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(albertId, pp);
		}

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Albert");
			criteres.setNom("Zweisteinen");
			criteres.setSexe(Sexe.MASCULIN);
			criteres.setDateNaissance(date(1953, 4, 3));
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(albertId, pp);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique alberto = addNonHabitant("Alberto", "Fujimorouille", null, null);
				ids.alberto = alberto.getNumero();
				return null;
			}
		});

		// Albert
		{
			doInNewTransaction(new TxCallback<Object>() {
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
					IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "2-BE-5");
					message.setLogCreationDate(RegDate.get().asJavaDate());
					final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
					service.forceIdentification(message, alberto, Etat.TRAITE_MANUELLEMENT);
					return null;
				}
			});

		}


		{
			final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
			assertEquals(alberto.getNumeroAssureSocial(), "123654798123");
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurNPA() throws Exception {
		final long noIndividuClaude = 151658;
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

		doInNewTransaction(new TxCallback<Object>() {
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

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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

		// création et traitement du message d'identification
		CriteresPersonne criteress = new CriteresPersonne();
		criteres.setPrenoms("Jean-Pierre");
		criteres.setNom("ZANOLARI");
		criteres.setNAVS11("97750420110");
		criteres.setDateNaissance(date(1954, 1, 1));


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissance() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, date(1978,5,6), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(date(1978,5,6));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissanceVide() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, null, "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(null);

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissanceMessageVide() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (date(1979,10,4)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(null);

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissanceCompletesDifferentes() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (date(1979,10,3)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(date(1979,10,4));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
		final List<IdentificationContribuable> list = identCtbDAO.getAll();
		assertEquals(1, list.size());

		final IdentificationContribuable ic = list.get(0);
		assertNotNull(ic);
		assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
		assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissancePartielesDifferentes() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (date(1979,10,3)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(RegDate.get(1979,11));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
		final List<IdentificationContribuable> list = identCtbDAO.getAll();
		assertEquals(1, list.size());

		final IdentificationContribuable ic = list.get(0);
		assertNotNull(ic);
		assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
		assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissanceRegistreVide() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, null, "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(date(1979,10,4));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissancePartielAnneeMessage() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (date(1979,10,4)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(RegDate.get(1979));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissancePartielAnneeRegistre() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (RegDate.get(1979)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(date(1979,10,4));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissancePartielAnneeMoisMessage() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (date(1979,10,4)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(RegDate.get(1979,10));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurDateNaissancePartielAnneeMoisRegistre() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, (RegDate.get(1979,10)), "Rosat", "Claude", true);
			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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
		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Claude");
		criteres.setNom("Rosat");

		criteres.setDateNaissance(date(1979,10,4));

		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurNAVS11Habitant() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indClaude = addIndividu(noIndividuClaude, date(1954, 1, 1), "ZANOLARI", "Jean-Pierre", true);
				addFieldsIndividu(indClaude, "", "97750420000", "");

			}
		});

		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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

		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Jean-Pierre");
		criteres.setNom("ZANOLARI");
		criteres.setDateNaissance(date(1954, 1, 1));
		criteres.setNAVS11("97750420110");


		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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

	//SIFISC-5040
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNavs13EtHomonymes() throws Exception {
		final long noIndividuJerome1 = 151658;
		final long noIndividuJerome2 = 123497;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indJerome1 = addIndividu(noIndividuJerome1, date(1982, 7, 26), "Lathion", "Jérôme", true);
				MockIndividu indJerome2 = addIndividu(noIndividuJerome2, date(1962, 11, 14), "Lathion", "Jérôme", true);
				addFieldsIndividu(indJerome1, "7568174973276", "", "");
				addFieldsIndividu(indJerome2, "7566199749203", "", "");
			}
		});

		class Ids {
			Long jerome2;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique habJerome2 = addHabitant(noIndividuJerome2);
				ids.jerome2 = habJerome2.getNumero();
				return null;
			}
		});

		globalTiersIndexer.sync();

		assertCountDemandes(0);

		// création et traitement du message d'identification

		CriteresPersonne criteresJerome1 = new CriteresPersonne();
		criteresJerome1.setPrenoms("Jérôme");
		criteresJerome1.setNom("Lathion");
		criteresJerome1.setNAVS13("7568174973276");


		final IdentificationContribuable messageJerome1 = createDemandeWithEmetteurId(criteresJerome1, "EMPTY");
		messageJerome1.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(messageJerome1);
				return null;
			}
		});

		// L'identification a du échouer car le numéro NAVS13 ne correspond pas à la demande
		final List<IdentificationContribuable> list = identCtbDAO.getAll();
		assertEquals(1, list.size());

		final IdentificationContribuable ic = list.get(0);
		assertNotNull(ic);
		assertEquals(Etat.A_TRAITER_MANUELLEMENT, ic.getEtat());
		assertEquals(Integer.valueOf(0), ic.getNbContribuablesTrouves());



		CriteresPersonne criteresJerome2 = new CriteresPersonne();
		criteresJerome2.setPrenoms("Jérôme");
		criteresJerome2.setNom("Lathion");
		criteresJerome2.setNAVS13("7566199749203");


		final IdentificationContribuable messageJerome2 = createDemandeWithEmetteurId(criteresJerome2, "EPSA1003");
		messageJerome2.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(messageJerome2);
				return null;
			}
		});

		// jérome doit avoir été trouvée, et traitée automatiquement
		final List<IdentificationContribuable> listJerome2 = identCtbDAO.getAll();
		assertEquals(2, listJerome2.size());

		//Afin de guarantir que l'on prend toujours le bon message, on trie la liste
		Collections.sort(list,new Comparator<IdentificationContribuable>() {
			@Override
			public int compare(IdentificationContribuable identificationContribuable, IdentificationContribuable identificationContribuable1) {
				return identificationContribuable.getLogCreationDate().compareTo(identificationContribuable1.getLogCreationDate());
			}
		});

		final IdentificationContribuable icJerome2 = listJerome2.get(1);


		assertNotNull(icJerome2);
		assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, icJerome2.getEtat());
		assertEquals(Integer.valueOf(1), icJerome2.getNbContribuablesTrouves());

		final Reponse reponse = icJerome2.getReponse();
		assertNotNull(reponse);
		assertNull(reponse.getErreur());
		assertEquals(ids.jerome2, reponse.getNoContribuable());

		// La demande doit avoir reçu une réponse automatiquement
		assertEquals(1, messageHandler.getSentMessages().size());
		final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
		assertEquals(icJerome2.getId(), sent.getId());


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSurNAVS11NonHabitant() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;


		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique habClaude = addNonHabitant("Jean-Pierre", "ZANOLARI", date(1954, 1, 1), Sexe.MASCULIN);
				addIdentificationPersonne(habClaude, CategorieIdentifiant.CH_AHV_AVS, "97750420000");
				ids.claude = habClaude.getNumero();
				return null;
			}
		});

		globalTiersIndexer.sync();

		assertCountDemandes(0);

		// création et traitement du message d'identification

		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Jean-Pierre");
		criteres.setNom("ZANOLARI");
		criteres.setDateNaissance(date(1954, 1, 1));
		criteres.setNAVS11("97750420110");


		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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

		// création et traitement du message d'identification
		CriteresPersonne criteress = new CriteresPersonne();
		criteres.setPrenoms("Jean-Pierre");
		criteres.setNom("ZANOLARI");
		criteres.setNAVS11("97750420110");
		criteres.setDateNaissance(date(1954, 1, 1));

	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testContribuableSansNAVS11NonHabitant() throws Exception {
		final long noIndividuClaude = 151658;
		final long noIndividuAnne = 2345;


		class Ids {
			Long claude;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique habClaude = addNonHabitant("Jean-Pierre", "ZANOLARI", date(1954, 1, 1), Sexe.MASCULIN);
				ids.claude = habClaude.getNumero();
				return null;
			}
		});

		globalTiersIndexer.sync();

		assertCountDemandes(0);

		// création et traitement du message d'identification

		CriteresPersonne criteres = new CriteresPersonne();
		criteres.setPrenoms("Jean-Pierre");
		criteres.setNom("ZANOLARI");
		criteres.setDateNaissance(date(1954, 1, 1));
		criteres.setNAVS11("97750420110");


		final IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "3-CH-30");
		message.setLogCreationDate(RegDate.get().asJavaDate());
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		// zanolari doit avoir été trouvée, et traitée automatiquement
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
	@Transactional(rollbackFor = Throwable.class)
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

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique alberto = addNonHabitant("Alberto", "Fujimorouille", null, null);
				ids.alberto = alberto.getNumero();
				return null;
			}
		});

		// Albert
		{
			doInNewTransaction(new TxCallback<Object>() {
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
					IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "2-BE-5");
					message.setLogCreationDate(RegDate.get().asJavaDate());
					final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
					service.forceIdentification(message, alberto, Etat.TRAITE_MANUELLEMENT);
					return null;
				}
			});

		}


		{
			final PersonnePhysique alberto = (PersonnePhysique) tiersService.getTiers(ids.alberto);
			assertEquals(alberto.getNumeroAssureSocial(), "123654798123");
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
	@Transactional(rollbackFor = Throwable.class)
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

		doInNewTransaction(new TxCallback<Object>() {
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
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(ids.albert, pp);
		}

		// Albert toujours
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweisteinen");
			criteres.setSexe(Sexe.MASCULIN);
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(ids.albert, pp);
		}

		// Les deux Zweisteinen
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Zweisteinen");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());
			Collections.sort(list);

			final Long pp0 = list.get(0);
			assertEquals(ids.albert, pp0);

			final Long pp1 = list.get(1);
			assertEquals(ids.anne, pp1);
		}
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testManyResults() throws Exception {


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
		}
		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Albert, le seul, le vrai
				final PersonnePhysique albert = addHabitant(noIndividuAlbert);
				ids.albert = albert.getNumero();

				// même nom de famille qu'Albert
				final PersonnePhysique anne = addHabitant(noIndividuAnne);
				ids.anne = anne.getNumero();

				for (int i = 0; i < 150; i++) {
					addNonHabitant("Alberto", "Fujimori", date(1953, 12, 3), Sexe.MASCULIN);
				}

				return null;
			}
		});

		globalTiersIndexer.sync();

		// Albert
		{
			final CriteresPersonne criteres = new CriteresPersonne();
			criteres.setPrenoms("Alberto");

			try {
				final List<Long> list = service.identifie(criteres);
				fail(ArrayUtils.toString(list.toArray()));
			}
			catch (TooManyIdentificationPossibilitiesException e) {
				// ok, tout va bien...
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetNomCantonFromMessage() throws Exception {

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
		IdentificationContribuable message = createDemandeWithEmetteurId(criteres, "2-BE-5");
		message.setLogCreationDate(RegDate.get().asJavaDate());

		String nomCanton = service.getNomCantonFromEmetteurId(message.getDemande().getEmetteurId());
		assertEquals("Berne", nomCanton);

		message = createDemandeWithEmetteurId(criteres, "2-SS-5");
		nomCanton = service.getNomCantonFromEmetteurId(message.getDemande().getEmetteurId());
		assertEquals("2-SS-5", nomCanton);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieParAdresse() throws Exception {

		class Ids {
			Long robert;
			Long jeanne;
			Long luc;
			Long michel;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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

			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());
			Collections.sort(list);

			final Long pp0 = list.get(0);
			assertEquals(ids.robert, pp0);

			final Long pp1 = list.get(1);
			assertEquals(ids.jeanne, pp1);
		}

		// Robert
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Nicoud");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1000);
			criteres.setAdresse(adresse);

			final List<Long> list = service.identifie(criteres);
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

			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp0 = list.get(0);
			assertEquals(ids.jeanne, pp0);
		}

		// Luc et Michel
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Haddoque");

			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(2, list.size());
			Collections.sort(list);

			final Long pp0 = list.get(0);
			assertEquals(ids.luc, pp0);

			final Long pp1 = list.get(1);
			assertEquals(ids.michel, pp1);
		}

		// Luc et Michel, encore mais c'est luc qui gagne
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Haddoque");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1350);
			criteres.setAdresse(adresse);

			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp0 = list.get(0);
			assertEquals(ids.luc, pp0);
		}


		// Michel
		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Haddoque");
			CriteresAdresse adresse = new CriteresAdresse();
			adresse.setNpaSuisse(1337);
			criteres.setAdresse(adresse);

			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp0 = list.get(0);
			assertEquals(ids.michel, pp0);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieSansAdresse() throws Exception {

		class Ids {
			Long robert;

		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
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

			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());
			Collections.sort(list);

			final Long pp0 = list.get(0);
			assertEquals(ids.robert, pp0);
		}
	}

	@Test
	public void testHandleDemandeZeroContribuableTrouve() throws Exception {

		// la base est vide
		assertCountDemandes(0);

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Arnold", "Duchoux");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemandeUnContribuableTrouve() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Zora", "Larousse");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemande_SANS_MANUEL() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Zouzou", "LaVerte", Demande.ModeIdentificationType.SANS_MANUEL);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemande_MANUEL_AVEC_ACK() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Zouzou", "LaVerte", Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemandeEnErreur_MANUEL_AVEC_ACK() throws Exception {

		final long noIndividu = 1234;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1953, 1, 1), "Zora", "Larousse", true);
			}
		});
		searcher = new GlobalTiersSearcherImpl() {
			@Override
			public TopList<TiersIndexedData> searchTop(TiersCriteria criteria, int max) throws IndexerException {
				throw new RuntimeException("Exception de test");
			}
		};
		service.setSearcher(searcher);

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addHabitant(noIndividu);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();


		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Zouzou", "LaVerte", Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Zora n'est pas trouvé
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.EXCEPTION, ic.getEtat());
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


	@Test
	public void testHandleDemande_NCS_MANUEL_AVEC_ACK() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeNCS("Zouzou", "LaVerte", Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemande_EMPACI_MANUEL_AVEC_ACK() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Laurent", "LeRoux", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeEMPACI("Zouzou", "Leroux", Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Laurent n'est pas trouvé
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

	@Test
	public void testHandleDemande_E_FACTURE_SANS_MANUEL() throws Exception {

		// création d'un contribuable
		final Long numeroZora = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				zora.setNumeroAssureSocial("7569613127861");
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeE_Facture("7569613127861");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Zora est trouvée
				final List<IdentificationContribuable> list = identCtbDAO.getAll();
				assertEquals(1, list.size());

				final IdentificationContribuable ic = list.get(0);
				assertNotNull(ic);
				assertEquals(Etat.TRAITE_AUTOMATIQUEMENT, ic.getEtat());
				assertEquals(Integer.valueOf(1), ic.getNbContribuablesTrouves());

				final Reponse reponse = ic.getReponse();
				assertNotNull(reponse);
				assertEquals(numeroZora, reponse.getNoContribuable());


				// La demande doit avoir reçu une réponse automatiquement
				assertEquals(1, messageHandler.getSentMessages().size());
				final IdentificationContribuable sent = messageHandler.getSentMessages().get(0);
				assertEquals(ic.getId(), sent.getId());

				return null;
			}
		});

	}


	@Test
	public void testHandleDemande_MANUEL_SANS_ACK() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique zora = addNonHabitant("Zora", "Larousse", date(1970, 4, 3), Sexe.FEMININ);
				addForPrincipal(zora, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return zora.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Zouzou", "LaVerte", Demande.ModeIdentificationType.MANUEL_SANS_ACK);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemandePlusieursContribuablesTrouves() throws Exception {

		// création de plusieurs contribuables
		doInNewTransaction(new TxCallback<Object>() {
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
		final IdentificationContribuable message = createDemandeMeldewesen("Larousse", "Larousse");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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

	@Test
	public void testHandleDemandeException() throws Exception {

		// création d'un contribuable
		doInNewTransaction(new TxCallback<Object>() {
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
			final IdentificationContribuable message = createDemandeMeldewesen("Zora", "Larousse");
			doInTransaction(new TxCallback<Object>() {
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

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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
	 * [UNIREG-1636] Vérifie qu'une demande d'identification qui contient un numéro AVS effectue quand même une recherche avec les autres critères si le numéro AVS n'est pas connu dans le registre.
	 *
	 * @throws Exception en cas d'erreur
	 */
	@Test
	public void testHandleDemandeAvecNumeroAVSInconnu() throws Exception {

		// création d'un contribuable
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique edouard = addNonHabitant("Edouard", "Bonhote", date(1965, 11, 3), Sexe.MASCULIN);
				addForPrincipal(edouard, RegDate.get(2009, 3, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				return edouard.getNumero();
			}
		});
		assertCountDemandes(0);

		globalTiersIndexer.sync();

		// création et traitement du message d'identification
		final IdentificationContribuable message = createDemandeMeldewesen("Edouard", "Bonhote", "7569613127861");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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
	 * [UNIREG-1630] Vérifie qu'une demande d'identification qui contient un numéro AVS effectue quand même une recherche avec les autres critères pour vérifier que tous les critères correspondent.
	 *
	 * @throws Exception en cas d'erreur
	 */
	@Test
	public void testHandleDemandeAvecNumeroAVSConnuMaisFaux() throws Exception {

		// création d'un contribuable
		doInNewTransaction(new TxCallback<Object>() {
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
		final IdentificationContribuable message = createDemandeMeldewesen("George", "Pompidou", "7569613127861");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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
	@Test
	public void testHandleDemandeUnContribuableAvecMenageTrouve() throws Exception {

		class Ids {
			Long zora;
			Long bruno;
			Long mc;
		}
		final Ids ids = new Ids();

		// création d'un contribuable
		doInNewTransaction(new TxCallback<Object>() {
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
		final IdentificationContribuable message = createDemandeMeldewesen("Zora", "Larousse");
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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
	//SIFISC-4845 : Pour une periode
	//Dans le cas ou la période fiscale du message est inférieure à 2003,
	//Unireg renvoi le contribuable à la date du 31.12 de l'année précédent à l'année en cours (ex. au 21 janvier 2013,
	// le traitement d'une PF de 2001 donnera le contribuable à la date du 31.12.2012).
	@Test
	public void testHandleDemandeUnContribuableAvecMenageSurPeriodeAvant2003() throws Exception {

		class Ids {
			Long zora;
			Long bruno;
			Long mc;
		}
		final Ids ids = new Ids();

		// création d'un contribuable
		doInNewTransaction(new TxCallback<Object>() {
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
		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms("Zora");
		personne.setNom("Larousse");


		//on set un eperiode fiscale dans le passé
		final IdentificationContribuable message = createDemandeMeldewesen(personne, Demande.ModeIdentificationType.MANUEL_SANS_ACK, 1011);
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				service.handleDemande(message);
				return null;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
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


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieUE() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "Müller", "Maya", true);
				addFieldsIndividu(indMaya, "", "67142770412", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("MUELLER");
			criteres.setPrenoms("Maya");
			criteres.setNAVS11("67142770412");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieAE() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indAlex = addIndividu(noIndividu, date(1953, 4, 3), "OHLENSCHLÄGER", "Alex", true);
				addFieldsIndividu(indAlex, "", "69447258153", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique alex = addHabitant(noIndividu);
				return alex.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("OHLENSCHLAEGER");
			criteres.setPrenoms("Alex");
			criteres.setNAVS11("69447258153");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieOE() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "Schönenberg", "Peter", true);
				addFieldsIndividu(indMaya, "", "83143380117", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Schoenenberg");
			criteres.setPrenoms("Peter");
			criteres.setNAVS11("83143380117");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieSansDernierPrenom() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "STEVENSON", "Hugh", true);
				addFieldsIndividu(indMaya, "", "85948265155", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("STEVENSON");
			criteres.setPrenoms("Hugh-Clark");
			criteres.setNAVS11("85948265155");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieSansDernierNom() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "RICHOZ", "Jean-Pierre", true);
				addFieldsIndividu(indMaya, "", "74150388116", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("RICHOZ-VINCENT");
			criteres.setPrenoms("Jean-Pierre");
			criteres.setNAVS11("74150388116");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieOESansDernierPrenom() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "Schönenberg", "Peter", true);
				addFieldsIndividu(indMaya, "", "83143380117", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Schoenenberg");
			criteres.setPrenoms("Peter-Hanz");
			criteres.setNAVS11("83143380117");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieOESansDernierNom() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "Schönenberg", "Peter", true);
				addFieldsIndividu(indMaya, "", "83143380117", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Schoenenberg-Mueller");
			criteres.setPrenoms("Peter");
			criteres.setNAVS11("83143380117");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIdentifieSansE_NomPrenom() throws Exception {

		final long noIndividu = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indMaya = addIndividu(noIndividu, date(1953, 4, 3), "Schönenberg", "Ülrich", true);
				addFieldsIndividu(indMaya, "", "83143380117", "");
			}
		});

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique maya = addHabitant(noIndividu);
				return maya.getNumero();
			}
		});

		globalTiersIndexer.sync();

		{
			CriteresPersonne criteres = new CriteresPersonne();
			criteres.setNom("Schoenenberg-Mueller");
			criteres.setPrenoms("Ülrich");
			criteres.setNAVS11("83143380117");
			final List<Long> list = service.identifie(criteres);
			assertNotNull(list);
			assertEquals(1, list.size());

			final Long pp = list.get(0);
			assertEquals(id, pp);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunRetourneAvantMariage() throws Exception {

		final long noIndividu = 13624325642L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 5, 12), "Labeille", "Maya", false);
				ind.setNouveauNoAVS("7569613127861");
			}
		});

		class Ids {
			final long ppId;
			final long mcId;

			Ids(long ppId, long mcId) {
				this.ppId = ppId;
				this.mcId = mcId;
			}
		}
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(2000, 6, 1), date(2004, 11, 28));
				return new Ids(pp.getNumero(), couple.getMenage().getNumero());
			}
		});

		globalTiersIndexer.sync();

		// avant 2000, on doit toujours trouver Maya seule, sans ménage commun
		{
			final IdentificationContribuable demande = createDemandeMeldewesen("Maya", "Labeille", "7569613127861");
			demande.getDemande().setPeriodeFiscale(1999);
			service.handleDemande(demande);

			assertNotNull(messageHandler.getSentMessages());
			assertEquals(1, messageHandler.getSentMessages().size());

			final IdentificationContribuable msg = messageHandler.getSentMessages().get(0);
			assertNotNull(msg);
			assertEquals((Long) ids.ppId, msg.getReponse().getNoContribuable());
			assertNull(msg.getReponse().getNoMenageCommun());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunRetournePendantValiditeMariage() throws Exception {

		final long noIndividu = 13624325642L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 5, 12), "Labeille", "Maya", false);
				ind.setNouveauNoAVS("7569613127861");
			}
		});

		class Ids {
			final long ppId;
			final long mcId;

			Ids(long ppId, long mcId) {
				this.ppId = ppId;
				this.mcId = mcId;
			}
		}
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(2000, 6, 1), date(2005, 11, 28));
				return new Ids(pp.getNumero(), couple.getMenage().getNumero());
			}
		});

		globalTiersIndexer.sync();

		// depuis 2000 jusqu'à 2002, on ne renvoie aucun ménage
		for (int i = 2000; i <= 2002; ++i) {
			messageHandler.reset();

			final IdentificationContribuable demande = createDemandeMeldewesen("Maya", "Labeille", "7569613127861");
			demande.getDemande().setPeriodeFiscale(i);
			service.handleDemande(demande);

			assertNotNull("Année " + i, messageHandler.getSentMessages());
			assertEquals("Année " + i, 1, messageHandler.getSentMessages().size());

			final IdentificationContribuable msg = messageHandler.getSentMessages().get(0);
			assertNotNull("Année " + i, msg);
			assertEquals("Année " + i, (Long) ids.ppId, msg.getReponse().getNoContribuable());
			assertEquals("Année " + i, null, msg.getReponse().getNoMenageCommun());
		}

		// depuis 2003 jusqu'à 2004, on doit renvoyer le numéro de ménage commun aussi
		for (int i = 2003; i <= 2004; ++i) {
			messageHandler.reset();

			final IdentificationContribuable demande = createDemandeMeldewesen("Maya", "Labeille", "7569613127861");
			demande.getDemande().setPeriodeFiscale(i);
			service.handleDemande(demande);

			assertNotNull("Année " + i, messageHandler.getSentMessages());
			assertEquals("Année " + i, 1, messageHandler.getSentMessages().size());

			final IdentificationContribuable msg = messageHandler.getSentMessages().get(0);
			assertNotNull("Année " + i, msg);
			assertEquals("Année " + i, (Long) ids.ppId, msg.getReponse().getNoContribuable());
			assertEquals("Année " + i, (Long) ids.mcId, msg.getReponse().getNoMenageCommun());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunRetourneApresValiditeMariage() throws Exception {

		final long noIndividu = 13624325642L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1974, 5, 12), "Labeille", "Maya", false);
				ind.setNouveauNoAVS("7569613127861");
			}
		});

		class Ids {
			final long ppId;
			final long mcId;

			Ids(long ppId, long mcId) {
				this.ppId = ppId;
				this.mcId = mcId;
			}
		}
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(2000, 6, 1), date(2004, 11, 28));
				return new Ids(pp.getNumero(), couple.getMenage().getNumero());
			}
		});

		globalTiersIndexer.sync();

		// depuis 2004, on ne doit à nouveau plus retourner de ménage commun dans la réponse
		{
			final IdentificationContribuable demande = createDemandeMeldewesen("Maya", "Labeille", "7569613127861");
			demande.getDemande().setPeriodeFiscale(2004);
			service.handleDemande(demande);

			assertNotNull(messageHandler.getSentMessages());
			assertEquals(1, messageHandler.getSentMessages().size());

			final IdentificationContribuable msg = messageHandler.getSentMessages().get(0);
			assertNotNull(msg);
			assertEquals((Long) ids.ppId, msg.getReponse().getNoContribuable());
			assertNull(msg.getReponse().getNoMenageCommun());
		}
	}

	private static IdentificationContribuable createDemandeMeldewesen(final String prenoms, final String nom) {

		return createDemandeMeldewesen(prenoms, nom, Demande.ModeIdentificationType.MANUEL_SANS_ACK);
	}

	private static IdentificationContribuable createDemandeMeldewesen(final String prenoms, final String nom, Demande.ModeIdentificationType mode) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms(prenoms);
		personne.setNom(nom);

		return createDemandeMeldewesen(personne, mode, 2009);
	}

	private static IdentificationContribuable createDemandeNCS(final String prenoms, final String nom, Demande.ModeIdentificationType mode) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms(prenoms);
		personne.setNom(nom);

		return createDemandeNCS(personne, mode);
	}

	private static IdentificationContribuable createDemandeEMPACI(final String prenoms, final String nom, Demande.ModeIdentificationType mode) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms(prenoms);
		personne.setNom(nom);

		return createDemandeImpotSource(personne, mode);
	}

	private static IdentificationContribuable createDemandeE_Facture(final String navs13) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setNAVS13(navs13);

		return createDemandeE_Facture(personne);
	}

	private static IdentificationContribuable createDemandeMeldewesen(final String prenoms, final String nom, final String noAVS13) {

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setPrenoms(prenoms);
		personne.setNom(nom);
		personne.setNAVS13(noAVS13);

		return createDemandeMeldewesen(personne, 2009);
	}


	private static IdentificationContribuable createDemandeWithEmetteurId(CriteresPersonne personne, String emetteurId) {
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

	private static IdentificationContribuable createDemandeMeldewesen(CriteresPersonne personne, int periodeFiscale) {
		return createDemandeMeldewesen(personne, Demande.ModeIdentificationType.MANUEL_SANS_ACK, periodeFiscale);
	}

	private static IdentificationContribuable createDemandeMeldewesen(CriteresPersonne personne, Demande.ModeIdentificationType modeIdentification, int periodeFiscale) {
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
		demande.setPeriodeFiscale(periodeFiscale);
		demande.setPersonne(personne);
		demande.setTypeDemande(TypeDemande.MELDEWESEN);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);

		return message;
	}

	private static IdentificationContribuable createDemandeNCS(CriteresPersonne personne, Demande.ModeIdentificationType modeIdentification) {
		final EsbHeader header = new EsbHeader();
		header.setBusinessId("123456");
		header.setBusinessUser("Test");
		header.setReplyTo("Test");

		final Demande demande = new Demande();
		demande.setEmetteurId("Test");
		demande.setMessageId("1111");
		demande.setPrioriteEmetteur(PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setModeIdentification(modeIdentification);
		demande.setTypeMessage("CS_EMPLOYEUR");
		demande.setDate(DateHelper.getCurrentDate());
		demande.setPeriodeFiscale(2010);
		demande.setPersonne(personne);
		demande.setTypeDemande(TypeDemande.NCS);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);

		return message;
	}

	private static IdentificationContribuable createDemandeImpotSource(CriteresPersonne personne, Demande.ModeIdentificationType modeIdentification) {
		final EsbHeader header = new EsbHeader();
		header.setBusinessId("123456");
		header.setBusinessUser("Test");
		header.setReplyTo("Test");

		final Demande demande = new Demande();
		demande.setEmetteurId("empaciTao");
		demande.setMessageId("2222");
		demande.setPrioriteEmetteur(PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setModeIdentification(modeIdentification);
		demande.setTypeMessage("LISTE_IS");
		demande.setDate(DateHelper.getCurrentDate());
		demande.setPeriodeFiscale(2010);
		demande.setPersonne(personne);
		demande.setTypeDemande(TypeDemande.IMPOT_SOURCE);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);

		return message;
	}

	private static IdentificationContribuable createDemandeE_Facture(CriteresPersonne personne) {
		final EsbHeader header = new EsbHeader();
		header.setBusinessId("123456");
		header.setBusinessUser("Test");
		header.setReplyTo("Test");

		final Demande demande = new Demande();
		demande.setEmetteurId("Test");
		demande.setMessageId("1111");
		demande.setPrioriteEmetteur(PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setModeIdentification(Demande.ModeIdentificationType.SANS_MANUEL);
		demande.setTypeMessage("CYBER_EFACTURE");
		demande.setDate(DateHelper.getCurrentDate());
		demande.setPeriodeFiscale(2012);
		demande.setPersonne(personne);
		demande.setTypeDemande(TypeDemande.E_FACTURE);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);

		return message;
	}


	private void assertCountDemandes(final int count) throws Exception {
		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertEquals(count, identCtbDAO.getCount(IdentificationContribuable.class));
				return null;
			}
		});
	}

	@Test
	public void testExtractionCantonEmetteur() throws Exception {
		Assert.assertNull(IdentificationContribuableServiceImpl.getSigleCantonEmetteur(null));
		Assert.assertNull(IdentificationContribuableServiceImpl.getSigleCantonEmetteur(""));
		Assert.assertNull(IdentificationContribuableServiceImpl.getSigleCantonEmetteur("12367124567"));
		Assert.assertNull(IdentificationContribuableServiceImpl.getSigleCantonEmetteur("12VD34"));
		Assert.assertNull(IdentificationContribuableServiceImpl.getSigleCantonEmetteur("1-VD-60"));
		Assert.assertNull(IdentificationContribuableServiceImpl.getSigleCantonEmetteur("12-VD-6"));
		Assert.assertEquals("VD", IdentificationContribuableServiceImpl.getSigleCantonEmetteur("1-VD-6"));
		Assert.assertEquals("VS", IdentificationContribuableServiceImpl.getSigleCantonEmetteur("3-VS-4"));
	}

	@Test
	public void testAdresseFromCantonEmetteur() throws Exception {
		final CriteresPersonne personne = new CriteresPersonne();
		personne.setNom("Tartempion");
		personne.setPrenoms("Toto");
		final CriteresAdresse adresse = new CriteresAdresse();
		adresse.setNpaSuisse(MockLocalite.Bumpliz.getNPA());
		personne.setAdresse(adresse);
		{
			final IdentificationContribuable ident = createDemandeWithEmetteurId(personne, "2-BE-3");
			Assert.assertTrue(service.isAdresseFromCantonEmetteur(ident));
		}
		{
			final IdentificationContribuable ident = createDemandeWithEmetteurId(personne, "2-VS-3");
			Assert.assertFalse(service.isAdresseFromCantonEmetteur(ident));
		}
		{
			final IdentificationContribuable ident = createDemandeWithEmetteurId(personne, "CABETO");
			Assert.assertFalse(service.isAdresseFromCantonEmetteur(ident));
		}
	}

	@Test
	public void testNpaFarfelu() throws Exception {
		// SIFISC-2104 : on imagine une identification manuelle d'un message du canton de Berne avec une localité dont le NPA est farfelu (9999, ça s'est vu) ;
		// lors du test pour savoir si le canton émetteur de la demande est bien celui de domicile du contribuable, il ne faut pas que cela explose
		// sous prétexte que le NPA ne correspond à rien...

		final CriteresPersonne personne = new CriteresPersonne();
		personne.setNom("Toto");
		final CriteresAdresse adresse = new CriteresAdresse();
		adresse.setNpaSuisse(9999);
		personne.setAdresse(adresse);
		final IdentificationContribuable ident = createDemandeWithEmetteurId(personne, "2-BE-3");

		Assert.assertFalse(service.isAdresseFromCantonEmetteur(ident));
	}
}
