package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.di.EvenementDeclarationSender;
import ch.vd.uniregctb.evenement.di.MockEvenementDeclarationSender;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.ValidationService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class DeclarationImpotServiceTest extends BusinessTest {

	private DeclarationImpotServiceImpl service;
	private HibernateTemplate hibernateTemplate;
	private TacheDAO tacheDAO;
	private TiersService tiersService;
	private PeriodeFiscaleDAO periodeDAO;
	private ParametreAppService parametres;
	private ValidationService validationService;
	private Long idCedi;
	private Long idOidLausanne;
	private Long idAci;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final EditiqueCompositionService editiqueService = getBean(EditiqueCompositionService.class, "editiqueCompositionService");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		tiersService = getBean(TiersService.class, "tiersService");
		final ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper = getBean(ImpressionDeclarationImpotOrdinaireHelper.class, "impressionDIHelper");
		final PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		parametres = getBean(ParametreAppService.class, "parametreAppService");
		final ServiceCivilCacheWarmer cacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		validationService = getBean(ValidationService.class, "validationService");
		final EvenementDeclarationSender evenementDeclarationSender = new MockEvenementDeclarationSender();

		serviceCivil.setUp(new DefaultMockServiceCivil());

		/*
		 * création du service à la main de manière à pouvoir appeler les méthodes protégées (= en passant par Spring on se prend un proxy
		 * et seul l'interface publique est accessible)
		 */
		service = new DeclarationImpotServiceImpl(editiqueService, hibernateTemplate, periodeDAO, tacheDAO, modeleDAO, delaisService,
				infraService, tiersService, impressionDIHelper, transactionManager, parametres, cacheWarmer, validationService, evenementFiscalService, evenementDeclarationSender);

		// évite de logger plein d'erreurs pendant qu'on teste le comportement du service
		final Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.FATAL);

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				idCedi = cedi.getId();
				CollectiviteAdministrative oidLausanne = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				idOidLausanne = oidLausanne.getId();
				CollectiviteAdministrative aci = addCollAdm(MockOfficeImpot.ACI);
				idAci = aci.getId();
				return null;
			}
		});
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();

		// réactive le log normal
		Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.ERROR);
	}

	/**
	 * Status manager qui interrompt le processus après un certains nombre de messages
	 */
	private static class InterruptingStatusManager implements StatusManager {

		private int count = 0;

		InterruptingStatusManager(int count) {
			this.count = count;
		}

		@Override
		public boolean interrupted() {
			return count <= 0;
		}

		@Override
		public void setMessage(String msg) {
			count--;
		}

		@Override
		public void setMessage(String msg, int percentProgression) {
			count--;
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettreInterruption() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

				// Un contribuable quelconque
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

				// Un autre contribuable quelconque
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

				return null;
			}
		});

		{
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);

			DeterminationDIsAEmettreProcessor processor = new DeterminationDIsAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO,
					parametres, tiersService, transactionManager, validationService);

			// Lance et interrompt l'envoi en masse après 2 contribuables (message de démarrage + message d'envoi de la DI d'eric)
			InterruptingStatusManager status = new InterruptingStatusManager(2);
			processor.setBatchSize(1);
			assertResults(1, processor.run(2007, date(2008, 1, 20), 1, status));

			List<TacheEnvoiDeclarationImpot> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, taches);

			taches = getTachesEnvoiDeclarationImpot(john, 2007);
			assertEmpty(taches);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettreContribuableNonValide() throws Exception {

		/*
		 * Ce fichier contient deux contribuables: un contribuable valide ordinaire (Eric Bolomey, id=10000003), et un autre qui ne valide
		 * pas à cause d'un problème de fors (John Bolomey, id=10000004).
		 */
		loadDatabase("TestDetermineDIsAEmettreContribuableNonValideTest.xml");

		final long idEric = 10000003L;
		final long idJohn = 10000004L;

		final Contribuable eric = hibernateTemplate.get(Contribuable.class, idEric);
		assertFalse(validationService.validate(eric).hasErrors());

		final Contribuable john = hibernateTemplate.get(Contribuable.class, idJohn);
		assertTrue(validationService.validate(john).hasErrors());

		// Détermine les DIs à émettre : le contribuable invalide ne devrait pas être pris en compte
		assertResults(1, service.determineDIsAEmettre(2007, date(2008, 1, 20), 1, null));

		List<TacheEnvoiDeclarationImpot> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
		assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
				TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, taches);

		taches = getTachesEnvoiDeclarationImpot(john, 2007);
		assertEmpty(taches);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettrePasDePeriodeFiscale() throws Exception {

		try {
			service.determineDIsAEmettre(2007, date(2008, 1, 15), 1, null);
			fail("Il ne devrait pas être possible de créer des tâches sans que la période fiscale considérée existe.");
		}
		catch (DeclarationException expected) {
			// ok
		}
	}

	/**
	 * Teste qu'il n'est pas possible de créer des tâches d'envoi de DIs en masse en dehors des délais.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettreHorsPeriode() throws Exception {

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addPeriodeFiscale(2007);
				return null;
			}
		});

		try {
			service.determineDIsAEmettre(2007, date(2007, 12, 1), 1, null);
			fail("Il ne devrait pas être possible de créer des tâches avant la fin de la période fiscale considérée.");
		}
		catch (DeclarationException expected) {
			// ok
		}

		final RegDate finPeriodeEnvoi = date(2008, 1, 31);
		try {
			service.determineDIsAEmettre(2007, finPeriodeEnvoi.getOneDayAfter(), 1, null);
			fail("Il ne devrait pas être possible de créer des tâches après la fin de la période d'envoi de masse considérée.");
		}
		catch (DeclarationException expected) {
			// ok
		}
	}

	/**
	 * Teste que la méthode 'determineDIsAEmettre' catch les exceptions et continue son travail sans roller-back la transaction.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettreException() throws Exception {
		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode = addPeriodeFiscale(2007);
				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

				// Un contribuable habitant dans le canton
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

				// Un contribuable possédant un immeuble dans le canton
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Neuchatel);
				addForSecondaire(john, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);

				return null;
			}
		});

		{
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			assertFalse(validationService.validate(eric).hasErrors());
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			assertFalse(validationService.validate(john).hasErrors());

			// Détermine les DIs à émettre : le status manager va lancer une exception sur le traitement de John.
			StatusManager status = new StatusManager() {
				@Override
				public boolean interrupted() {
					return false;
				}

				@Override
				public void setMessage(String msg) {
				}

				@Override
				public void setMessage(String msg, int percentProgression) {
					if (msg.contains(john.getNumero().toString())) { // saute lorsque l'id de john est traité
						throw new IllegalArgumentException("exception de test");
					}
				}
			};
			assertResults(1, service.determineDIsAEmettre(2007, date(2008, 1, 20), 1, status));

			List<TacheEnvoiDeclarationImpot> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, taches);

			taches = getTachesEnvoiDeclarationImpot(john, 2007);
			assertEmpty(taches);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRetourDI() throws Exception {
		class Ids {
			public long ericId;
		}
		final Ids ids = new Ids();
		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				ModeleDocument declarationComplete2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2006);

				PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

				// Un tiers tout ce quil y a de plus ordinaire
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2006);

				return null;
			}
		});
		// Retour de la declaration
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
				final RegDate dateEvenement = RegDate.get(2007, 5, 12);
				assertTrue(eric.getDeclarationsForPeriode(2006, false).size() == 1);
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) eric.getDeclarationsForPeriode(2006, false).get(0);
				assertNotNull(service.retourDI(eric, di, dateEvenement));
				return null;
			}
		});

		// Verification de la declaration retournee
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
				assertTrue(eric.getDeclarationsForPeriode(2006, false).size() == 1);
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) eric.getDeclarationsForPeriode(2006, false).get(0);
				assertEquals(TypeEtatDeclaration.RETOURNEE, di.getDernierEtat().getEtat());
				assertEquals(RegDate.get(2007, 5, 12), di.getDateRetour());
				return null;
			}
		});

		// Modification de la date de retour de la declaration
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
				final RegDate dateEvenement = RegDate.get(2007, 8, 8);
				assertTrue(eric.getDeclarationsForPeriode(2006, false).size() == 1);
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) eric.getDeclarationsForPeriode(2006, false).get(0);
				assertNotNull(service.retourDI(eric, di, dateEvenement));
				return null;
			}
		});

		// On verifie que la date de retour a été modifiée
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				assertTrue(eric.getDeclarationsForPeriode(2006, false).size() == 1);
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) eric.getDeclarationsForPeriode(2006, false).get(0);
				assertEquals(TypeEtatDeclaration.RETOURNEE, di.getDernierEtat().getEtat());
				assertEquals(RegDate.get(2007, 8, 8), di.getDateRetour());
				return null;
			}
		});

	}

	/**
	 * Teste la détermination des tâches <b>et</b> l'envoi des déclarations d'impôt à partir de ces tâches.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineEtEnvoyerDIs() throws Exception {

		class Ids {
			public long paulId;
			public long ericId;
			public long olrikId;
			public long guillaumeId;
			public long jeanId;
			public long jacquesId;
			public long mittId;
			public long georgesId;
			public long jackyId;
			public long lionelId;
			public long brunoId;
			public long marcId;
			public long ramonId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				final ModeleDocument declarationComplete2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2006);
				final ModeleDocument declarationVaudTax2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
				addModeleFeuilleDocument("Déclaration vaud tax", "250", declarationVaudTax2006);
				final ModeleDocument declarationDep2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2006);
				addModeleFeuilleDocument("Déclaration dépense", "270", declarationDep2006);
				ModeleDocument declarationHC2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2006);
				addModeleFeuilleDocument("Déclaration HC", "200", declarationHC2006);

				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);
				final ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2007);
				addModeleFeuilleDocument("Déclaration vaud tax", "250", declarationVaudTax2007);
				final ModeleDocument declarationDep2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2007);
				addModeleFeuilleDocument("Déclaration dépense", "270", declarationDep2007);
				final ModeleDocument declarationHC2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2007);
				addModeleFeuilleDocument("Déclaration HC", "200", declarationHC2007);

				// Un tiers imposé à la dépense
				Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
				ids.paulId = paul.getNumero();
				ForFiscalPrincipal fors = addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				fors.setModeImposition(ModeImposition.DEPENSE);

				// Un tiers tout ce quil y a de plus ordinaire
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2006);

				// Un tiers ordinaire, mais sans déclaration d'impôt précédente
				Contribuable olrik = addNonHabitant("Olrick", "Pasgentil", date(1965, 4, 13), Sexe.MASCULIN);
				ids.olrikId = olrik.getNumero();
				addForPrincipal(olrik, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

				// Un tiers ordinaire mais avec VaudTax
				Contribuable guillaume = addNonHabitant("Guillaume", "Portes", date(1965, 4, 13), Sexe.MASCULIN);
				ids.guillaumeId = guillaume.getNumero();
				addForPrincipal(guillaume, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(guillaume, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationVaudTax2006);

				// contribuable hors canton ayant une activité indépendante dans le canton
				Contribuable jean = addNonHabitant("Jean", "Glasfich", date(1948, 11, 3), Sexe.MASCULIN);
				ids.jeanId = jean.getNumero();
				addForPrincipal(jean, date(1968, 11, 3), MotifFor.MAJORITE, MockCommune.Neuchatel);
				addForSecondaire(jean, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				addAdresseSuisse(jean, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);

				// contribuable hors canton ayant une activité indépendante dans le canton, ainsi qu'un autre type de for et une déclaration VaudTax pour 2006
				Contribuable jacques = addNonHabitant("Jacques", "Glasfich", date(1948, 11, 3), Sexe.MASCULIN);
				ids.jacquesId = jacques.getNumero();
				addForPrincipal(jacques, date(1968, 11, 3), MotifFor.MAJORITE, MockCommune.Neuchatel);
				addForSecondaire(jacques, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
				addForAutreImpot(jacques, date(1968, 11, 3), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, GenreImpot.DONATION);
				addAdresseSuisse(jacques, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);
				addDeclarationImpot(jacques, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.HORS_CANTON, declarationVaudTax2006);

				// contribuable hors Suisse ayant une activité indépendante dans le canton
				Contribuable mitt = addNonHabitant("Mitt", "Romney", date(1948, 11, 3), Sexe.MASCULIN);
				ids.mittId = mitt.getNumero();
				addForPrincipal(mitt, date(1968, 11, 3), null, MockPays.France);
				addForSecondaire(mitt, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
				addAdresseEtrangere(mitt, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.Danemark);

				// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors canton
				Contribuable georges = addNonHabitant("Georges", "Delatchaux", date(1948, 11, 3), Sexe.MASCULIN);
				ids.georgesId = georges.getNumero();
				addForPrincipal(georges, date(1968, 11, 3), null, MockCommune.Zurich);
				addForSecondaire(georges, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addAdresseSuisse(georges, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);

				// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors Suisse depuis toujours
				Contribuable jacky = addNonHabitant("Jacky", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
				ids.jackyId = jacky.getNumero();
				addForPrincipal(jacky, date(1968, 11, 3), null, MockPays.France);
				addForSecondaire(jacky, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addAdresseEtrangere(jacky, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.France);

				// [UNIREG-1349] contribuable propriétaire d'un immeubles depuis 2007 et domiciliée hors Suisse depuis toujours
				Contribuable lionel = addNonHabitant("Lionel", "Posjin", date(1948, 11, 3), Sexe.MASCULIN);
				ids.lionelId = lionel.getNumero();
				addForPrincipal(lionel, date(1968, 11, 3), null, MockPays.France);
				addForSecondaire(lionel, date(2007, 4, 25), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addAdresseEtrangere(lionel, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.France);

				// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors Suisse depuis mi-2007
				Contribuable bruno = addNonHabitant("Bruno", "Plisenski", date(1948, 11, 3), Sexe.MASCULIN);
				ids.brunoId = bruno.getNumero();
				addForPrincipal(bruno, date(1968, 11, 3), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Fraction.LeBrassus);
				addForPrincipal(bruno, date(2007, 7, 1), null, MockPays.France);
				addForSecondaire(bruno, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addAdresseEtrangere(bruno, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.France);

				// Un diplomate suisse en mission hors suisse
				Contribuable marc = addNonHabitant("Marc", "Ramatruelle", date(1948, 11, 3), Sexe.MASCULIN);
				ids.marcId = marc.getNumero();
				addForPrincipal(marc, date(1968, 11, 3), MotifFor.ARRIVEE_HC, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);

				// Un diplomate étranger en mission en Suisse et possédant un immeuble dans le Canton
				Contribuable ramon = addNonHabitant("Ramon", "Zapapatotoche", date(1948, 11, 3), Sexe.MASCULIN);
				ids.ramonId = ramon.getNumero();
				addForPrincipal(ramon, date(1968, 11, 3), MotifFor.ARRIVEE_HC, MockPays.Espagne, MotifRattachement.DIPLOMATE_ETRANGER);
				addForSecondaire(ramon, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return null;
			}
		});

		/*
		 * Determination des DIs et création des tâches
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(10, service.determineDIsAEmettre(2007, date(2008, 1, 15), 1, null)); // toutes sauf marc, jacky et lionel
				return null;
			}
		});

		// Etat avant envoi
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable paul = hibernateTemplate.get(Contribuable.class, ids.paulId); // depense
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
				final Contribuable olrik = hibernateTemplate.get(Contribuable.class, ids.olrikId); // ordinaire
				final Contribuable guillaume = hibernateTemplate.get(Contribuable.class, ids.guillaumeId); // vaudtax
				final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId); // hors-canton complète
				final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId); // hors-canton vaudtax
				final Contribuable mitt = hibernateTemplate.get(Contribuable.class, ids.mittId); // ordinaire
				final Contribuable georges = hibernateTemplate.get(Contribuable.class, ids.georgesId); // hors canton
				final Contribuable jacky = hibernateTemplate.get(Contribuable.class, ids.jackyId); // hors suisse
				final Contribuable lionel = hibernateTemplate.get(Contribuable.class, ids.lionelId); // hors suisse
				final Contribuable bruno = hibernateTemplate.get(Contribuable.class, ids.brunoId); // hors suisse
				final Contribuable marc = hibernateTemplate.get(Contribuable.class, ids.marcId); // diplomate suisse
				final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.ramonId); // diplomate étranger

				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE,
						TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(paul, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(olrik, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(guillaume, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jean, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jacques, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(mitt, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
						TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(georges, 2007));
				assertEmpty(getTachesEnvoiDeclarationImpot(jacky, 2007));
				assertEmpty(getTachesEnvoiDeclarationImpot(lionel, 2007)); // [UNIREG-1742] ctb HS avec immeuble => DI optionnelle
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(bruno, 2007)); // [UNIREG-1742] rattrapage de la DI normalement envoyée au moment du départ HS
				assertEmpty(getTachesEnvoiDeclarationImpot(marc, 2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon, 2007));

				assertEmpty(paul.getDeclarationsForPeriode(2007, false));
				assertEmpty(eric.getDeclarationsForPeriode(2007, false));
				assertEmpty(olrik.getDeclarationsForPeriode(2007, false));
				assertEmpty(guillaume.getDeclarationsForPeriode(2007, false));
				assertEmpty(jean.getDeclarationsForPeriode(2007, false));
				assertEmpty(jacques.getDeclarationsForPeriode(2007, false));
				assertEmpty(mitt.getDeclarationsForPeriode(2007, false));
				assertEmpty(georges.getDeclarationsForPeriode(2007, false));
				assertEmpty(jacky.getDeclarationsForPeriode(2007, false));
				assertEmpty(bruno.getDeclarationsForPeriode(2007, false));
				assertEmpty(marc.getDeclarationsForPeriode(2007, false));
				assertEmpty(ramon.getDeclarationsForPeriode(2007, false));

				return null;
			}
		});

		/*
		 * Envoi des DIs : vaudois ordinaires DIs complètes
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(3, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 100, date(2008, 1, 20), false, null)); // erik, olrik + ramon
				return null;
			}
		});
		{
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final Contribuable olrik = hibernateTemplate.get(Contribuable.class, ids.olrikId); // ordinaire
			final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.ramonId); // diplomate étranger

			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
					eric.getDeclarationsForPeriode(2007, false));
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
					olrik.getDeclarationsForPeriode(2007, false));
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
					ramon.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : vaudois ordinaire DIs VaudTax
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_VAUDTAX, null, null, 100, date(2008, 1, 20), false, null)); // guillaume
				return null;
			}
		});
		{
			final Contribuable guillaume = hibernateTemplate.get(Contribuable.class, ids.guillaumeId); // vaudtax
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
					guillaume.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : vaudois à la dépense
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_DEPENSE, null, null, 100, date(2008, 1, 20), false, null)); // paul
				return null;
			}
		});
		{
			final Contribuable paul = hibernateTemplate.get(Contribuable.class, ids.paulId); // depense
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE, idOidLausanne, date(2008, 3, 31),
					paul.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : hors canton immeuble
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.HC_IMMEUBLE, null, null, 100, date(2008, 1, 20), false, null));   // george
				return null;
			}
		});
		{
			final Contribuable georges = hibernateTemplate.get(Contribuable.class, ids.georgesId); // hors canton
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, idOidLausanne, date(2008, 3, 31),
					georges.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : hors canton activité indépendante DIs complètes
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.HC_ACTIND_COMPLETE, null, null, 100, date(2008, 1, 20), false, null)); // jean
				return null;
			}
		});
		{
			final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId); // ordinaire
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
					jean.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : hors canton activité indépendante DIs VaudTax
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.HC_ACTIND_VAUDTAX, null, null, 100, date(2008, 1, 20), false, null)); // jacques
				return null;
			}
		});
		{
			final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId); // ordinaire
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
					jacques.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : hors Suisse DIs complètes
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(2, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.HS_COMPLETE, null, null, 100, date(2008, 1, 20), false, null)); // bruno + mitt
				return null;
			}
		});
		{
			final Contribuable jacky = hibernateTemplate.get(Contribuable.class, ids.jackyId); // hors suisse
			assertEmpty(jacky.getDeclarationsForPeriode(2007, false)); // hors suisse depuis toujours: ne reçoit pas de déclaration
			final Contribuable lionel = hibernateTemplate.get(Contribuable.class, ids.lionelId); // hors suisse
			assertEmpty(lionel.getDeclarationsForPeriode(2007, false)); // [UNIREG-1742] hors suisse avec immeuble première : déclaration optionnelle

			final Contribuable bruno = hibernateTemplate.get(Contribuable.class, ids.brunoId); // hors suisse
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
					bruno.getDeclarationsForPeriode(2007, false));

			final Contribuable mitt = hibernateTemplate.get(Contribuable.class, ids.mittId); // ordinaire
			assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
					mitt.getDeclarationsForPeriode(2007, false));
		}

		/*
		 * Envoi des DIs : hors Suisse DIs vaudtax
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(0, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.HS_VAUDTAX, null, null, 100, date(2008, 1, 20), false, null)); // personne !
				return null;
			}
		});

		/*
		 * Envoi des DIs : diplomates suisses
		 */
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(0, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.DIPLOMATE_SUISSE, null, null, 100, date(2008, 1, 20), false, null)); // marc
				return null;
			}
		});
		{
			final Contribuable marc = hibernateTemplate.get(Contribuable.class, ids.marcId); // diplomate suisse
			assertEmpty(marc.getDeclarationsForPeriode(2007, false)); // diplomate suisse: ne reçoit pas de déclaration
		}

		{
			final Contribuable paul = hibernateTemplate.get(Contribuable.class, ids.paulId); // depense
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final Contribuable olrik = hibernateTemplate.get(Contribuable.class, ids.olrikId); // ordinaire
			final Contribuable guillaume = hibernateTemplate.get(Contribuable.class, ids.guillaumeId); // vaudtax
			final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId); // hors-canton complète
			final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId); // hors-canton VaudTax
			final Contribuable mitt = hibernateTemplate.get(Contribuable.class, ids.mittId); // hors-Suisse
			final Contribuable georges = hibernateTemplate.get(Contribuable.class, ids.georgesId); // hors canton
			final Contribuable jacky = hibernateTemplate.get(Contribuable.class, ids.jackyId); // hors suisse depuis toujours
			final Contribuable lionel = hibernateTemplate.get(Contribuable.class, ids.lionelId); // hors suisse depuis toujours
			final Contribuable bruno = hibernateTemplate.get(Contribuable.class, ids.brunoId); // hors suisse depuis mi-2007
			final Contribuable marc = hibernateTemplate.get(Contribuable.class, ids.marcId); // diplomate suisse
			final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.ramonId); // diplomate étranger

			// Les tâches doivent être traitées, maintenant
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE,
					TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(paul, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(olrik, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(guillaume, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jean, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX, 
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jacques, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(mitt, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
					TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(georges, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(bruno, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(jacky, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(lionel, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(marc, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon, 2007));
		}
	}

	/**
	 * Teste la limitation sur le nombre d'envois
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoyerDIsEnMasseNbMaxEnvoi() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", modele);
				addModeleFeuilleDocument("Annexe 1", "220", modele);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele);

				// Un contribuable quelconque
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, null, colAdm);

				// Un autre contribuable quelconque
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, john, null, colAdm);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// limite à 1 le nombre de DI envoyée
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1, date(2008, 1, 20), false, null));
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);

				assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31), eric.getDeclarationsForPeriode(2007, false));
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoyerDIsEnMassePlageCtb() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", modele);
				addModeleFeuilleDocument("Annexe 1", "220", modele);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele);

				// Un contribuable quelconque
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, null, colAdm);

				// Un autre contribuable quelconque
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, john, null, colAdm);
				return null;
			}
		});

		// envoi des di en masse en ciblant sur un numéro seulement
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, ids.ericId, ids.ericId, 100, date(2008, 1, 20), false, null));
				return null;
			}
		});

		// vérification que seul eric a une DI 2007, pas john
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				assertFalse(eric.getDeclarationsForPeriode(2007, false).isEmpty());
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoyerDIsEnMasseInterruption() throws Exception {

		final int taille = 1;

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", modele);
				addModeleFeuilleDocument("Annexe 1", "220", modele);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele);

				// Un contribuable quelconque
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, null, colAdm);

				// Un autre contribuable quelconque
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, john, null, colAdm);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				assertEmpty(eric.getDeclarationsForPeriode(2007, false));
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				return null;
			}
		});

		// Lance et interrompt l'envoi en masse après 2 (message de démarrage + message d'envoi du premier lot = de la DI d'eric)
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				InterruptingStatusManager s = new InterruptingStatusManager(2);
				service.setTailleLot(1);
				assertResults(taille, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 100, date(2008, 1, 20), false, s));
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				assertFalse(eric.getDeclarationsForPeriode(2007, false).isEmpty());
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				return null;
			}
		});
	}

	/**
	 * Teste que la méthode 'envoyerDIsEnMasse'.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoyerDIsEnMasseForGestionNull() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				addModeleFeuilleDocument("Déclaration HC", "200", addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
						addPeriodeFiscale(2007)));

				// Un contribuable hors-canton possédant deux immeubles dans le canton
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), null, MockCommune.Neuchatel);
				addForSecondaire(eric, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(eric, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON,
						TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, eric, null, colAdm);

				// Un autre contribuable hors-canton possédant deux immeubles dans le canton
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), null, MockCommune.Neuchatel);
				// addForSecondaire(john, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				// TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
				// addForSecondaire(john, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens.getNoOFS(),
				// TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON,
						TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, john, null, colAdm);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				assertEmpty(eric.getDeclarationsForPeriode(2007, false));
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Lance et provoque le crash de l'envoi en masse sur la DI de john
				assertResults(1, service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.HC_IMMEUBLE, null, null, 100, date(2008, 1, 20), false, null));
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				assertFalse(eric.getDeclarationsForPeriode(2007, false).isEmpty());
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				return null;
			}
		});
	}

	/**
	 * test le traitement par lot
	 */
	@Test
	public void testEnvoiDiEnMasseParLot() throws Exception {

		class Ids {
			Long jeanId;
			Long jacquesId;
			Long pierreId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

				Contribuable jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jeanId = jean.getNumero();
				addForPrincipal(jean, date(2006, 2, 5), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(10), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jean, null, colAdm);

				Contribuable jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jacquesId = jacques.getNumero();
				addForPrincipal(jacques, date(2006, 2, 5), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(10), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jacques, null, colAdm);

				Contribuable pierre = addNonHabitant("Pierre", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.pierreId = pierre.getNumero();
				addForPrincipal(pierre, date(2006, 2, 5), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(10), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pierre, null, colAdm);

				return null;
			}
		});

		final StatusManager status = new StatusManager() {
			@Override
			public boolean interrupted() {
				return false;
			}

			@Override
			public void setMessage(String msg) {
			}

			@Override
			public void setMessage(String msg, int percentProgression) {
				if (msg.contains(ids.jacquesId.toString())) { // crash au traitement de l'id de Jacques
					throw new IllegalArgumentException("exception de test");
				}
			}
		};

		// traite les trois habitants dans autant de lots séparés, et lève une exception sur le traitement de Jacques
		service.setTailleLot(1);
		service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 100, RegDate.get(), false, status);
		service.setTailleLot(100);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId);
				final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId);
				final Contribuable pierre = hibernateTemplate.get(Contribuable.class, ids.pierreId);
				assertFalse(jean.getDeclarationsForPeriode(2007, false).isEmpty());
				assertEmpty(jacques.getDeclarationsForPeriode(2007, false));
				assertFalse(pierre.getDeclarationsForPeriode(2007, false).isEmpty());

				// test les tâches
				assertEquals(1, tacheDAO.find(ids.jeanId).size());
				assertEquals(TypeEtatTache.TRAITE, tacheDAO.find(ids.jeanId).get(0).getEtat());
				assertEquals(1, tacheDAO.find(ids.jacquesId).size());
				assertEquals(TypeEtatTache.EN_INSTANCE, tacheDAO.find(ids.jacquesId).get(0).getEtat());
				assertEquals(1, tacheDAO.find(ids.pierreId).size());
				assertEquals(TypeEtatTache.TRAITE, tacheDAO.find(ids.pierreId).get(0).getEtat());

				// test les evt fiscaux
				TiersService tiersService = getBean(TiersService.class, "tiersService");
				EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

				Collection<EvenementFiscal> lesEvenementsdeJean = evenementFiscalService.getEvenementsFiscaux(tiersService
						.getTiers(ids.jeanId));
				assertFalse(lesEvenementsdeJean.isEmpty());
				Collection<EvenementFiscal> lesEvenementsdeJacques = evenementFiscalService.getEvenementsFiscaux(tiersService
						.getTiers(ids.jacquesId));
				assertEmpty("Evénements fiscaux engendrés", lesEvenementsdeJacques);
				Collection<EvenementFiscal> lesEvenementsdePierre = evenementFiscalService.getEvenementsFiscaux(tiersService
						.getTiers(ids.pierreId));
				assertFalse(lesEvenementsdePierre.isEmpty());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterTache() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
			Long ramonId;
			Long totorId;
			Long oursId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				
				PeriodeFiscale periode = addPeriodeFiscale(2007);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				addModeleFeuilleDocument("Déclaration", "210", modele);
				addModeleFeuilleDocument("Annexe 1", "220", modele);
				addModeleFeuilleDocument("Annexe 2-3", "230", modele);
				addModeleFeuilleDocument("Annexe 4-5", "240", modele);

				// Un contribuable normal
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, null, colAdm);

				// Un contribuable indigent
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				ForFiscalPrincipal f = addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				f.setModeImposition(ModeImposition.INDIGENT);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, john, null, colAdm);

				// [UNIREG-1852] Un contribuable indigent décédé dans l'année
				Contribuable ours = addNonHabitant("Ours", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.oursId = ours.getNumero();
				f = addForPrincipal(ours, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 5, 23), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				f.setModeImposition(ModeImposition.INDIGENT);
				TacheEnvoiDeclarationImpot t = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 5, 23),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ours, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);

				// Un contribuable normal avec une DI pré-existente sur toute l'année
				Contribuable ramon = addNonHabitant("Ramon", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ramonId = ramon.getNumero();
				addForPrincipal(ramon, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ramon, null, colAdm);
				addDeclarationImpot(ramon, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				// Un contribuable normal avec une DI pré-existente sur une portion de l'année (= cas erroné)
				Contribuable totor = addNonHabitant("Ramon", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.totorId = totor.getNumero();
				addForPrincipal(totor, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, totor, null, colAdm);
				addDeclarationImpot(totor, periode, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				final Contribuable ours = hibernateTemplate.get(Contribuable.class, ids.oursId);
				final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.ramonId);
				final Contribuable totor = hibernateTemplate.get(Contribuable.class, ids.totorId);
				assertEmpty(eric.getDeclarationsForPeriode(2007, false));
				assertEmpty(john.getDeclarationsForPeriode(2007, false));
				assertEmpty(ours.getDeclarationsForPeriode(2007, false));
				assertDI(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, null, ramon.getDeclarationsForPeriode(2007, false));
				assertDI(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, null, totor.getDeclarationsForPeriode(2007, false));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric,
								2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(john,
								2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon,
								2007));
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(totor,
								2007));
				return null;
			}
		});

		// Lance l'envoi en masse : eric (traité) +ours (indigent décédé donc quand même traité) + john (indigent) + ramon (ignoré), totor (en erreur)
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				EnvoiDIsResults r = service.envoyerDIsEnMasse(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 100, date(2008, 1, 20), false, null);
				assertResults(2, 1, 1, 1, r);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
				final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
				final Contribuable ours = hibernateTemplate.get(Contribuable.class, ids.oursId);
				final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.ramonId);
				final Contribuable totor = hibernateTemplate.get(Contribuable.class, ids.totorId);
				assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31), eric.getDeclarationsForPeriode(2007, false));
				assertDI(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31), john.getDeclarationsForPeriode(2007, false));
				assertDI(date(2007, 1, 1), date(2007, 5, 23), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idAci, date(2008, 3, 25), ours.getDeclarationsForPeriode(2007, false)); // [UNIREG-1852], [UNIREG-1861]
				assertDI(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, null, ramon.getDeclarationsForPeriode(2007, false));
				assertDI(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, null, totor.getDeclarationsForPeriode(2007, false));
				assertOneTache(TypeEtatTache.TRAITE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric,
								2007));
				assertOneTache(TypeEtatTache.TRAITE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(john,
								2007));
				// ramon: la DI pré-existante corresponds parfaitement avec la tâche
				assertOneTache(TypeEtatTache.TRAITE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon,
								2007));
				// totor: la DI pré-existante est en conflit avec la tâche => erreur
				assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(totor,
								2007));
				return null;
			}
		});
	}

	/**
	 * Teste l'envoi de déclaration pour les contribuables propriétaires d'immeuble sis dans le canton mais domiciliés hors suisse.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettreHorsSuisse() throws Exception {

		class Ids {
			Long jackyId;
			Long thierryId;
			Long lionelId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				ModeleDocument declarationComplete2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2006);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2006);
				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);
				ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2008));
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);
				ModeleDocument declarationComplete2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2009));
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2009);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2009);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2009);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2009);
				addModeleFeuilleDocument("Annexe 1-1", "310", declarationComplete2009);

				// Contribuable propriétaire d'un immeubles à Lausanne et ayant déménagé fin 2006 au Danemark.
				Contribuable jacky = addNonHabitant("Jacky", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
				ids.jackyId = jacky.getNumero();
				addForPrincipal(jacky, date(1968, 11, 3), MotifFor.MAJORITE, date(2006, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(jacky, date(2007, 1, 1), null, MockPays.Danemark);
				addForSecondaire(jacky, date(1990, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				addDeclarationImpot(jacky, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2006);

				// Contribuable propriétaire d'un immeubles à Lausanne et ayant déménagé mi 2007 au Danemark.
				Contribuable thierry = addNonHabitant("Thierry", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
				ids.thierryId = thierry.getNumero();
				addForPrincipal(thierry, date(1968, 11, 3), MotifFor.MAJORITE, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(thierry, date(2007, 7, 1), null, MockPays.Danemark);
				addForSecondaire(thierry, date(1990, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);
				addDeclarationImpot(thierry, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.HORS_SUISSE,
						declarationComplete2006);

				// Contribuable propriétaire d'un immeuble depuis mi-2007 à Lausanne et hors-Suisse depuis toujours
				Contribuable lionel = addNonHabitant("Lionel", "Posjin", date(1948, 11, 3), Sexe.MASCULIN);
				ids.lionelId = lionel.getNumero();
				addForPrincipal(lionel, date(1968, 11, 3), null, MockPays.Danemark);
				addForSecondaire(lionel, date(2007, 5, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
						MotifRattachement.IMMEUBLE_PRIVE);

				return null;
			}
		});

		{
			final Contribuable jacky = hibernateTemplate.get(Contribuable.class, ids.jackyId);
			final Contribuable thierry = hibernateTemplate.get(Contribuable.class, ids.thierryId);
			final Contribuable lionel = hibernateTemplate.get(Contribuable.class, ids.lionelId);

			// Envoi en masse 2007 pour les contribuables hors Suisse
			service.determineDIsAEmettre(2007, date(2008, 1, 15), 1, null);

			assertEmpty(getTachesEnvoiDeclarationImpot(jacky, 2007)); // au forfait en 2007
			// [UNIREG-1349] le contribuable possède un immeuble bien avant son départ HS: une tâche doit être générée immédiatement lors du
			// départ, mais l'immeuble n'ayant pas été acheté dans la même période on n'en génère pas ici
			// [UNIREG-1742] rattrapage des DI
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
					TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(thierry, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(lionel, 2007)); // [UNIREG-1742] les ctb HS avec immeubles sont taxés au forfait dès la première année
		}
	}

	/**
	 * Teste l'envoi de déclaration pour les contribuables dit "indigents"
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettreIndigents() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
			Long paulId;
		}

		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				addPeriodeFiscale(2007);
				ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un tiers indigent depuis toujours
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				ForFiscalPrincipal fors = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				fors.setModeImposition(ModeImposition.INDIGENT);

				// Un tiers indigent depuis 2007
				Contribuable john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.johnId = john.getNumero();
				addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, date(2006, 12, 31), MotifFor.CHGT_MODE_IMPOSITION,
						MockCommune.Lausanne);
				fors = addForPrincipal(john, date(2007, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
				fors.setModeImposition(ModeImposition.INDIGENT);
				addDeclarationImpot(john, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete);

				// Un tiers indigent décédé en 2007
				Contribuable paul = addNonHabitant("Paul", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.paulId = paul.getNumero();
				fors = addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 5, 23), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				fors.setModeImposition(ModeImposition.INDIGENT);

				return null;
			}
		});

		{
			service.determineDIsAEmettre(2007, date(2008, 1, 20), 1, null);

			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			final Contribuable paul = hibernateTemplate.get(Contribuable.class, ids.paulId);

			List<TacheEnvoiDeclarationImpot> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, taches);

			taches = getTachesEnvoiDeclarationImpot(john, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, taches);

			// [UNIREG-1852] indigent décédé -> adresse de retour = ACI
			taches = getTachesEnvoiDeclarationImpot(paul, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 5, 23),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.ACI, taches);
		}
	}

	/**
	 * @return toutes les tâches définies pour le contribuable et l'année spécifiés
	 */
	@SuppressWarnings("unchecked")
	private List<TacheEnvoiDeclarationImpot> getTachesEnvoiDeclarationImpot(Contribuable contribuable, int annee) {

		final Integer debutAnnee = date(annee, 1, 1).index();
		final Integer finAnnee = date(annee, 12, 31).index();

		final String query = "FROM TacheEnvoiDeclarationImpot AS t WHERE t.contribuable = ? AND ? <= t.dateDebut AND t.dateFin <= ? ORDER BY t.dateDebut ASC";
		return hibernateTemplate.find(query, contribuable, debutAnnee, finAnnee);
	}

	private static void assertResults(int ctbsTraites, DeterminationDIsResults results) {
		assertEquals(ctbsTraites, results.traites.size());
	}

	private static void assertResults(int ctbsTraites, EnvoiDIsResults results) {
		assertEquals(ctbsTraites, results.ctbsTraites.size());
	}

	private static void assertResults(int ctbsTraites, int ctbsIndigents, int ctbsIgnores, int ctbsEnErrors, EnvoiDIsResults results) {
		assertEquals(ctbsTraites, results.ctbsTraites.size());
		assertEquals(ctbsIndigents, results.ctbsIndigents.size());
		assertEquals(ctbsIgnores, results.ctbsIgnores.size());
		assertEquals(ctbsEnErrors, results.ctbsEnErrors.size());
	}
}
