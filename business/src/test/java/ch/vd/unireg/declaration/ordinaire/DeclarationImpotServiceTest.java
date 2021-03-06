package ch.vd.unireg.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.AjoutDelaiDeclarationException;
import ch.vd.unireg.declaration.AjoutDelaiDeclarationException.Raison;
import ch.vd.unireg.declaration.AjoutDemandeLiberationException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.LiberationDeclaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.pp.DeterminationDIsPPAEmettreProcessor;
import ch.vd.unireg.declaration.ordinaire.pp.DeterminationDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.unireg.declaration.ordinaire.pp.ImpressionDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.evenement.di.EvenementDeclarationPPSender;
import ch.vd.unireg.evenement.di.MockEvenementDeclarationPPSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.fiscal.MockEvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.mandataire.DemandeDelaisMandataireDAO;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;
	private DeclarationImpotService diService;
	private DemandeDelaisMandataireDAO demandeDelaisMandataireDAO;

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
		final ImpressionDeclarationImpotPersonnesPhysiquesHelper impressionDIPPHelper = getBean(ImpressionDeclarationImpotPersonnesPhysiquesHelper.class, "impressionDIPPHelper");
		final PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		parametres = getBean(ParametreAppService.class, "parametreAppService");
		final ServiceCivilCacheWarmer cacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		validationService = getBean(ValidationService.class, "validationService");
		final EvenementDeclarationPPSender evenementDeclarationSender = new MockEvenementDeclarationPPSender();
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		adresseService = getBean(AdresseService.class, "adresseService");
		final TicketService ticketService = getBean(TicketService.class, "ticketService");
		final RegimeFiscalService regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
		diService = getBean(DeclarationImpotService.class, "diService");
		demandeDelaisMandataireDAO = getBean(DemandeDelaisMandataireDAO.class, "demandeDelaisMandataireDAO");

		serviceCivil.setUp(new DefaultMockIndividuConnector());

		/*
		 * création du service à la main de manière à pouvoir appeler les méthodes protégées (= en passant par Spring on se prend un proxy
		 * et seule l'interface publique est accessible)
		 */
		service = new DeclarationImpotServiceImpl(editiqueService, hibernateTemplate, periodeDAO, tacheDAO, modeleDAO, delaisService, infraService, tiersService,
		                                          transactionManager, parametres, cacheWarmer, validationService, evenementFiscalService, evenementDeclarationSender, periodeImpositionService,
		                                          assujettissementService, ticketService, regimeFiscalService, demandeDelaisMandataireDAO, audit);

		doInNewTransactionAndSession(status -> {
			CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(InfrastructureConnector.noCEDI);
			idCedi = cedi.getId();
			CollectiviteAdministrative oidLausanne = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
			idOidLausanne = oidLausanne.getId();
			CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(InfrastructureConnector.noACI);
			idAci = aci.getId();
			return null;
		});
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
		public boolean isInterrupted() {
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
		doInNewTransaction(status -> {
			ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2007));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2007);

			// Un contribuable quelconque
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

			// Un autre contribuable quelconque
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			return null;
		});

		{
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			final PersonnePhysique john = hibernateTemplate.get(PersonnePhysique.class, ids.johnId);

			DeterminationDIsPPAEmettreProcessor processor = new DeterminationDIsPPAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO,
			                                                                                        parametres, tiersService, transactionManager, validationService, periodeImpositionService, adresseService);

			// Lance et interrompt l'envoi en masse après 2 contribuables (message de démarrage + message d'envoi de la DI d'eric)
			InterruptingStatusManager status = new InterruptingStatusManager(2);
			processor.setBatchSize(1);
			assertResults(1, processor.run(2007, date(2008, 1, 20), 1, status));

			List<TacheEnvoiDeclarationImpotPP> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, taches);

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

		final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, idEric);
		assertFalse(validationService.validate(eric).hasErrors());

		final PersonnePhysique john = hibernateTemplate.get(PersonnePhysique.class, idJohn);
		assertTrue(validationService.validate(john).hasErrors());

		// Détermine les DIs à émettre : le contribuable invalide ne devrait pas être pris en compte
		assertResults(1, service.determineDIsPPAEmettre(2007, date(2008, 1, 20), 1, null));

		List<TacheEnvoiDeclarationImpotPP> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
		assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
		               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, taches);

		taches = getTachesEnvoiDeclarationImpot(john, 2007);
		assertEmpty(taches);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDIsAEmettrePasDePeriodeFiscale() throws Exception {

		try {
			service.determineDIsPPAEmettre(2007, date(2008, 1, 15), 1, null);
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

		doInNewTransaction(status -> {
			addPeriodeFiscale(2007);
			return null;
		});

		try {
			service.determineDIsPPAEmettre(2007, date(2007, 12, 1), 1, null);
			fail("Il ne devrait pas être possible de créer des tâches avant la fin de la période fiscale considérée.");
		}
		catch (DeclarationException expected) {
			// ok
		}

		final RegDate finPeriodeEnvoi = date(2008, 1, 31);
		try {
			service.determineDIsPPAEmettre(2007, finPeriodeEnvoi.getOneDayAfter(), 1, null);
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
		doInNewTransaction(status -> {
			PeriodeFiscale periode = addPeriodeFiscale(2007);
			ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2007);

			// Un contribuable habitant dans le canton
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

			// Un contribuable possédant un immeuble dans le canton
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Neuchatel);
			addForSecondaire(john, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			return null;
		});

		{
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			assertFalse(validationService.validate(eric).hasErrors());
			final PersonnePhysique john = hibernateTemplate.get(PersonnePhysique.class, ids.johnId);
			assertFalse(validationService.validate(john).hasErrors());

			// Détermine les DIs à émettre : le status manager va lancer une exception sur le traitement de John.
			StatusManager status = new StatusManager() {
				@Override
				public boolean isInterrupted() {
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
			assertResults(1, service.determineDIsPPAEmettre(2007, date(2008, 1, 20), 1, status));

			List<TacheEnvoiDeclarationImpotPP> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, taches);

			taches = getTachesEnvoiDeclarationImpot(john, 2007);
			assertEmpty(taches);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testQuittancementDI() throws Exception {
		class Ids {
			public long ericId;
		}
		final Ids ids = new Ids();
		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(status -> {
			PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
			ModeleDocument declarationVaudTax2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2006);

			PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
			ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2007);

			// Un tiers tout ce quil y a de plus ordinaire
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationVaudTax2006);
			return null;
		});
		// Retour de la declaration
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final RegDate dateEvenement = date(2007, 5, 12);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, dateEvenement, "TEST", false);
			return null;
		});

		// Verification de la declaration retournee
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());

			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);

			final EtatDeclaration dernierEtat = di.getDernierEtatDeclaration();
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, dernierEtat.getEtat());
			assertEquals(date(2007, 5, 12), di.getDateRetour());

			final EtatDeclarationRetournee etatRetourne = (EtatDeclarationRetournee) dernierEtat;
			assertEquals(date(2007, 5, 12), etatRetourne.getDateObtention());
			assertEquals("TEST", etatRetourne.getSource());
			return null;
		});

		// Modification de la date de retour de la declaration
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final RegDate dateEvenement = date(2007, 8, 8);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, dateEvenement, "TEST2", false);
			return null;
		});

		// On verifie que la date de retour a été modifiée
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);

			final EtatDeclaration dernierEtat = di.getDernierEtatDeclaration();
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, dernierEtat.getEtat());
			assertEquals(date(2007, 8, 8), di.getDateRetour());

			final EtatDeclarationRetournee etatRetourne = (EtatDeclarationRetournee) dernierEtat;
			assertEquals(date(2007, 8, 8), etatRetourne.getDateObtention());
			assertEquals("TEST2", etatRetourne.getSource());
			return null;
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

		doInNewTransaction(status -> {
			final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
			final ModeleDocument declarationComplete2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2006);
			final ModeleDocument declarationVaudTax2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_250, declarationVaudTax2006);
			final ModeleDocument declarationDep2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_270, declarationDep2006);
			ModeleDocument declarationHC2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_200, declarationHC2006);

			final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
			final ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2007);
			final ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_250, declarationVaudTax2007);
			final ModeleDocument declarationDep2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_270, declarationDep2007);
			final ModeleDocument declarationHC2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_200, declarationHC2007);

			// Un tiers imposé à la dépense
			PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
			ids.paulId = paul.getNumero();
			addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.DEPENSE);

			// Un tiers tout ce quil y a de plus ordinaire
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2006);

			// Un tiers ordinaire, mais sans déclaration d'impôt précédente
			PersonnePhysique olrik = addNonHabitant("Olrick", "Pasgentil", date(1965, 4, 13), Sexe.MASCULIN);
			ids.olrikId = olrik.getNumero();
			addForPrincipal(olrik, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

			// Un tiers ordinaire mais avec VaudTax
			PersonnePhysique guillaume = addNonHabitant("Guillaume", "Portes", date(1965, 4, 13), Sexe.MASCULIN);
			ids.guillaumeId = guillaume.getNumero();
			addForPrincipal(guillaume, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(guillaume, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationVaudTax2006);

			// contribuable hors canton ayant une activité indépendante dans le canton
			PersonnePhysique jean = addNonHabitant("Jean", "Glasfich", date(1948, 11, 3), Sexe.MASCULIN);
			ids.jeanId = jean.getNumero();
			addForPrincipal(jean, date(1968, 11, 3), MotifFor.MAJORITE, MockCommune.Neuchatel);
			addForSecondaire(jean, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ACTIVITE_INDEPENDANTE);
			addAdresseSuisse(jean, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);
			addDeclarationImpot(jean, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2006);

			// contribuable hors canton ayant une activité indépendante dans le canton, ainsi qu'un autre type de for et une déclaration VaudTax pour 2006
			PersonnePhysique jacques = addNonHabitant("Jacques", "Glasfich", date(1948, 11, 3), Sexe.MASCULIN);
			ids.jacquesId = jacques.getNumero();
			addForPrincipal(jacques, date(1968, 11, 3), MotifFor.MAJORITE, MockCommune.Neuchatel);
			addForSecondaire(jacques, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ACTIVITE_INDEPENDANTE);
			addForAutreImpot(jacques, date(1968, 11, 3), null, MockCommune.Lausanne, GenreImpot.DONATION);
			addAdresseSuisse(jacques, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);
			addDeclarationImpot(jacques, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.HORS_CANTON, declarationVaudTax2006);

			// contribuable hors Suisse ayant une activité indépendante dans le canton
			PersonnePhysique mitt = addNonHabitant("Mitt", "Romney", date(1948, 11, 3), Sexe.MASCULIN);
			ids.mittId = mitt.getNumero();
			addForPrincipal(mitt, date(1968, 11, 3), null, MockPays.France);
			addForSecondaire(mitt, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ACTIVITE_INDEPENDANTE);
			addAdresseEtrangere(mitt, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.Danemark);

			// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors canton
			PersonnePhysique georges = addNonHabitant("Georges", "Delatchaux", date(1948, 11, 3), Sexe.MASCULIN);
			ids.georgesId = georges.getNumero();
			addForPrincipal(georges, date(1968, 11, 3), null, MockCommune.Zurich);
			addForSecondaire(georges, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			addAdresseSuisse(georges, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);

			// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors Suisse depuis toujours
			PersonnePhysique jacky = addNonHabitant("Jacky", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
			ids.jackyId = jacky.getNumero();
			addForPrincipal(jacky, date(1968, 11, 3), null, MockPays.France);
			addForSecondaire(jacky, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			addAdresseEtrangere(jacky, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.France);

			// [UNIREG-1349] contribuable propriétaire d'un immeubles depuis 2007 et domiciliée hors Suisse depuis toujours
			PersonnePhysique lionel = addNonHabitant("Lionel", "Posjin", date(1948, 11, 3), Sexe.MASCULIN);
			ids.lionelId = lionel.getNumero();
			addForPrincipal(lionel, date(1968, 11, 3), null, MockPays.France);
			addForSecondaire(lionel, date(2007, 4, 25), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			addAdresseEtrangere(lionel, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.France);

			// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors Suisse depuis mi-2007
			PersonnePhysique bruno = addNonHabitant("Bruno", "Plisenski", date(1948, 11, 3), Sexe.MASCULIN);
			ids.brunoId = bruno.getNumero();
			addForPrincipal(bruno, date(1968, 11, 3), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Fraction.LeBrassus);
			addForPrincipal(bruno, date(2007, 7, 1), null, MockPays.France);
			addForSecondaire(bruno, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			addAdresseEtrangere(bruno, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, null, null, MockPays.France);
			addDeclarationImpot(bruno, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2006);

			// Un diplomate suisse en mission hors suisse
			PersonnePhysique marc = addNonHabitant("Marc", "Ramatruelle", date(1948, 11, 3), Sexe.MASCULIN);
			ids.marcId = marc.getNumero();
			addForPrincipal(marc, date(1968, 11, 3), MotifFor.ARRIVEE_HC, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);

			// Un diplomate étranger en mission en Suisse et possédant un immeuble dans le Canton
			PersonnePhysique ramon = addNonHabitant("Ramon", "Zapapatotoche", date(1948, 11, 3), Sexe.MASCULIN);
			ids.ramonId = ramon.getNumero();
			addForPrincipal(ramon, date(1968, 11, 3), MotifFor.ARRIVEE_HC, MockPays.Espagne, MotifRattachement.DIPLOMATE_ETRANGER);
			addForSecondaire(ramon, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			return null;
		});

		/*
		 * Determination des DIs et création des tâches
		 */
		doInNewTransaction(status -> {
			assertResults(10, service.determineDIsPPAEmettre(2007, date(2008, 1, 15), 1, null)); // toutes sauf marc, jacky et lionel;
			return null;
		});

		// Etat avant envoi
		doInNewTransaction(status -> {
			final PersonnePhysique paul = hibernateTemplate.get(PersonnePhysique.class, ids.paulId); // depense
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId); // ordinaire
			final PersonnePhysique olrik = hibernateTemplate.get(PersonnePhysique.class, ids.olrikId); // ordinaire
			final PersonnePhysique guillaume = hibernateTemplate.get(PersonnePhysique.class, ids.guillaumeId); // vaudtax
			final PersonnePhysique jean = hibernateTemplate.get(PersonnePhysique.class, ids.jeanId); // hors-canton complète
			final PersonnePhysique jacques = hibernateTemplate.get(PersonnePhysique.class, ids.jacquesId); // hors-canton vaudtax
			final PersonnePhysique mitt = hibernateTemplate.get(PersonnePhysique.class, ids.mittId); // ordinaire
			final PersonnePhysique georges = hibernateTemplate.get(PersonnePhysique.class, ids.georgesId); // hors canton
			final PersonnePhysique jacky = hibernateTemplate.get(PersonnePhysique.class, ids.jackyId); // hors suisse
			final PersonnePhysique lionel = hibernateTemplate.get(PersonnePhysique.class, ids.lionelId); // hors suisse
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, ids.brunoId); // hors suisse
			final PersonnePhysique marc = hibernateTemplate.get(PersonnePhysique.class, ids.marcId); // diplomate suisse
			final PersonnePhysique ramon = hibernateTemplate.get(PersonnePhysique.class, ids.ramonId); // diplomate étranger

			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE,
			               TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(paul, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(olrik, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(guillaume, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jean, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jacques, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(mitt, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
			               TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(georges, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(jacky, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(lionel, 2007)); // [UNIREG-1742] ctb HS avec immeuble => DI optionnelle
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(bruno, 2007)); // [UNIREG-1742] rattrapage de la DI normalement envoyée au moment du départ HS
			assertEmpty(getTachesEnvoiDeclarationImpot(marc, 2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon, 2007));

			assertEmpty(paul.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(olrik.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(guillaume.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(jean.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(jacques.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(mitt.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(georges.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(jacky.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(bruno.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(marc.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(ramon.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
		});

		/*
		 * Envoi des DIs : vaudois ordinaires DIs complètes
		 */
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 100, date(2008, 1, 20), false, 1, null)); // erik;
			return null;
		});
		{
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire complète
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
			           eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
		}

		/*
		 * Envoi des DIs : vaudois ordinaire DIs VaudTax
		 */
		doInNewTransaction(status -> {
			assertResults(2, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 100, date(2008, 1, 20), false, 1, null)); // guillaume, olrik + ramon;
			return null;
		});
		{
			final Contribuable guillaume = hibernateTemplate.get(Contribuable.class, ids.guillaumeId); // vaudtax
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
			           guillaume.getDeclarationsDansPeriode(Declaration.class, 2007, false));

			final Contribuable olrik = hibernateTemplate.get(Contribuable.class, ids.olrikId); // ordinaire
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
			           olrik.getDeclarationsDansPeriode(Declaration.class, 2007, false));

			final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.ramonId); // diplomate étranger
			assertEmpty(ramon.getDeclarationsDansPeriode(Declaration.class, 2007, false));
		}

		/*
		 * Envoi des DIs : vaudois à la dépense
		 */
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_DEPENSE, null, null, 100, date(2008, 1, 20), false, 1, null)); // paul;
			return null;
		});
		{
			final Contribuable paul = hibernateTemplate.get(Contribuable.class, ids.paulId); // depense
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE, idOidLausanne, date(2008, 3, 31),
			           paul.getDeclarationsDansPeriode(Declaration.class, 2007, false));
		}

		/*
		 * Envoi des DIs : hors canton immeuble
		 */
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.HC_IMMEUBLE, null, null, 100, date(2008, 1, 20), false, 1, null));   // george;
			return null;
		});
		{
			final Contribuable georges = hibernateTemplate.get(Contribuable.class, ids.georgesId); // hors canton
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, idOidLausanne, date(2008, 3, 31),
			           georges.getDeclarationsDansPeriode(Declaration.class, 2007, false));
		}

		/*
		 * Envoi des DIs : hors canton activité indépendante DIs complètes
		 */
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, null, null, 100, date(2008, 1, 20), false, 1, null)); // jean;
			return null;
		});
		{
			final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId); // ordinaire
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
			           jean.getDeclarationsDansPeriode(Declaration.class, 2007, false));
		}

		/*
		 * Envoi des DIs : hors canton activité indépendante DIs VaudTax
		 */
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.HC_ACTIND_VAUDTAX, null, null, 100, date(2008, 1, 20), false, 1, null)); // jacques;
			return null;
		});
		{
			final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId); // ordinaire
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
			           jacques.getDeclarationsDansPeriode(Declaration.class, 2007, false));
		}

		/*
		 * Envoi des DIs : hors Suisse DIs complètes
		 */
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.HS_COMPLETE, null, null, 100, date(2008, 1, 20), false, 1, null)); // bruno;
			return null;
		});
		{
			final Contribuable jacky = hibernateTemplate.get(Contribuable.class, ids.jackyId); // hors suisse
			assertEmpty(jacky.getDeclarationsDansPeriode(Declaration.class, 2007, false)); // hors suisse depuis toujours: ne reçoit pas de déclaration
			final Contribuable lionel = hibernateTemplate.get(Contribuable.class, ids.lionelId); // hors suisse
			assertEmpty(lionel.getDeclarationsDansPeriode(Declaration.class, 2007, false)); // [UNIREG-1742] hors suisse avec immeuble première : déclaration optionnelle

			final Contribuable bruno = hibernateTemplate.get(Contribuable.class, ids.brunoId); // hors suisse
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, idCedi, date(2008, 3, 31),
			           bruno.getDeclarationsDansPeriode(Declaration.class, 2007, false));

		}

		/*
		 * Envoi des DIs : hors Suisse DIs vaudtax
		 */
		doInNewTransaction(status -> {
			assertResults(2, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.HS_VAUDTAX, null, null, 100, date(2008, 1, 20), false, 1, null)); // mitt + ramon;
			return null;
		});
		{
			final Contribuable mitt = hibernateTemplate.get(Contribuable.class, ids.mittId); // ordinaire
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
			           mitt.getDeclarationsDansPeriode(Declaration.class, 2007, false));

			final Contribuable ramon = hibernateTemplate.get(Contribuable.class, ids.mittId); // hors-suisse
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31),
			           ramon.getDeclarationsDansPeriode(Declaration.class, 2007, false));

		}

		/*
		 * Envoi des DIs : diplomates suisses
		 */
		doInNewTransaction(status -> {
			assertResults(0, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.DIPLOMATE_SUISSE, null, null, 100, date(2008, 1, 20), false, 1, null)); // marc;
			return null;
		});
		{
			final Contribuable marc = hibernateTemplate.get(Contribuable.class, ids.marcId); // diplomate suisse
			assertEmpty(marc.getDeclarationsDansPeriode(Declaration.class, 2007, false)); // diplomate suisse: ne reçoit pas de déclaration
		}

		/*
		 * Vérification des tâches d'envoi
		 */
		{
			final PersonnePhysique paul = hibernateTemplate.get(PersonnePhysique.class, ids.paulId); // depense
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId); // ordinaire
			final PersonnePhysique olrik = hibernateTemplate.get(PersonnePhysique.class, ids.olrikId); // ordinaire
			final PersonnePhysique guillaume = hibernateTemplate.get(PersonnePhysique.class, ids.guillaumeId); // vaudtax
			final PersonnePhysique jean = hibernateTemplate.get(PersonnePhysique.class, ids.jeanId); // hors-canton complète
			final PersonnePhysique jacques = hibernateTemplate.get(PersonnePhysique.class, ids.jacquesId); // hors-canton VaudTax
			final PersonnePhysique mitt = hibernateTemplate.get(PersonnePhysique.class, ids.mittId); // hors-Suisse
			final PersonnePhysique georges = hibernateTemplate.get(PersonnePhysique.class, ids.georgesId); // hors canton
			final PersonnePhysique jacky = hibernateTemplate.get(PersonnePhysique.class, ids.jackyId); // hors suisse depuis toujours
			final PersonnePhysique lionel = hibernateTemplate.get(PersonnePhysique.class, ids.lionelId); // hors suisse depuis toujours
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, ids.brunoId); // hors suisse depuis mi-2007
			final PersonnePhysique marc = hibernateTemplate.get(PersonnePhysique.class, ids.marcId); // diplomate suisse
			final PersonnePhysique ramon = hibernateTemplate.get(PersonnePhysique.class, ids.ramonId); // diplomate étranger

			// Les tâches doivent être traitées, maintenant
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, TypeDocument.DECLARATION_IMPOT_DEPENSE,
			               TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(paul, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(olrik, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(guillaume, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jean, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(jacques, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(mitt, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE,
			               TypeAdresseRetour.OID, getTachesEnvoiDeclarationImpot(georges, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
			               TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(bruno, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(jacky, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(lionel, 2007));
			assertEmpty(getTachesEnvoiDeclarationImpot(marc, 2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
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
		doInNewTransaction(status -> {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2007));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, modele);

			// Un contribuable quelconque
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, eric, null, null, colAdm);

			// Un autre contribuable quelconque
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, john, null, null, colAdm);
			return null;
		});

		doInNewTransaction(status -> {
			// limite à 1 le nombre de DI envoyée
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 1, date(2008, 1, 20), false, 1, null));
			return null;
		});

		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);

			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31), eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
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
		doInNewTransaction(status -> {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2007));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, modele);

			// Un contribuable quelconque
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, eric, null, null, colAdm);

			// Un autre contribuable quelconque
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, john, null, null, colAdm);
			return null;
		});

		// envoi des di en masse en ciblant sur un numéro seulement
		doInNewTransaction(status -> {
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, ids.ericId, ids.ericId, 100, date(2008, 1, 20), false, 1, null));
			return null;
		});

		// vérification que seul eric a une DI 2007, pas john
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			assertFalse(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false).isEmpty());
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
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
		doInNewTransaction(status -> {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2007));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, modele);

			// Un contribuable quelconque
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, eric, null, null, colAdm);

			// Un autre contribuable quelconque
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, john, null, null, colAdm);
			return null;
		});

		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			assertEmpty(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
		});

		// Lance et interrompt l'envoi en masse après 2 (message de démarrage + message d'envoi du premier lot = de la DI d'eric)
		doInNewTransaction(status -> {
			InterruptingStatusManager s = new InterruptingStatusManager(2);
			service.setTailleLot(1);
			assertResults(taille, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 100, date(2008, 1, 20), false, 1, s));
			return null;
		});

		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			assertFalse(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false).isEmpty());
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
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
		doInNewTransaction(status -> {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_200, addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, addPeriodeFiscale(2007)));

			// Un contribuable hors-canton possédant deux immeubles dans le canton
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), null, MockCommune.Neuchatel);
			addForSecondaire(eric, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(eric, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON,
			                  TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, eric, null, null, colAdm);

			// Un autre contribuable hors-canton possédant deux immeubles dans le canton
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), null, MockCommune.Neuchatel);
			// addForSecondaire(john, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
			// TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
			// addForSecondaire(john, date(1983, 4, 13), MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens,
			// TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_CANTON,
			                  TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, john, null, null, colAdm);
			return null;
		});

		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			assertEmpty(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
		});

		doInNewTransaction(status -> {
			// Lance et provoque le crash de l'envoi en masse sur la DI de john
			assertResults(1, service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.HC_IMMEUBLE, null, null, 100, date(2008, 1, 20), false, 1, null));
			return null;
		});

		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final Contribuable john = hibernateTemplate.get(Contribuable.class, ids.johnId);
			assertFalse(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false).isEmpty());
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			return null;
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

		doInNewTransaction(status -> {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
			ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2007);

			PersonnePhysique jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
			ids.jeanId = jean.getNumero();
			addForPrincipal(jean, date(2006, 2, 5), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(10), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, jean, null, null, colAdm);

			PersonnePhysique jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
			ids.jacquesId = jacques.getNumero();
			addForPrincipal(jacques, date(2006, 2, 5), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(10), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, jacques, null, null, colAdm);

			PersonnePhysique pierre = addNonHabitant("Pierre", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
			ids.pierreId = pierre.getNumero();
			addForPrincipal(pierre, date(2006, 2, 5), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(10), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, pierre, null, null, colAdm);
			return null;
		});

		final StatusManager status = new StatusManager() {
			@Override
			public boolean isInterrupted() {
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
		service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 100, RegDate.get(), false, 1, status);
		service.setTailleLot(100);

		doInNewTransaction(s -> {
			final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId);
			final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId);
			final Contribuable pierre = hibernateTemplate.get(Contribuable.class, ids.pierreId);
			assertFalse(jean.getDeclarationsDansPeriode(Declaration.class, 2007, false).isEmpty());
			assertEmpty(jacques.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertFalse(pierre.getDeclarationsDansPeriode(Declaration.class, 2007, false).isEmpty());

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

			Collection<EvenementFiscal> lesEvenementsdeJean = evenementFiscalService.getEvenementsFiscaux(tiersService.getTiers(ids.jeanId));
			assertFalse(lesEvenementsdeJean.isEmpty());
			Collection<EvenementFiscal> lesEvenementsdeJacques = evenementFiscalService.getEvenementsFiscaux(tiersService.getTiers(ids.jacquesId));
			assertEmpty("Evénements fiscaux engendrés", lesEvenementsdeJacques);
			Collection<EvenementFiscal> lesEvenementsdePierre = evenementFiscalService.getEvenementsFiscaux(tiersService.getTiers(ids.pierreId));
			assertFalse(lesEvenementsdePierre.isEmpty());
			return null;
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
		doInNewTransaction(status -> {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			PeriodeFiscale periode = addPeriodeFiscale(2007);
			ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, modele);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, modele);

			// Un contribuable normal
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, eric, null, null, colAdm);

			// Un contribuable indigent
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.INDIGENT);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, john, null, null, colAdm);

			// [UNIREG-1852] Un contribuable indigent décédé dans l'année
			PersonnePhysique ours = addNonHabitant("Ours", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.oursId = ours.getNumero();
			addForPrincipal(ours, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 5, 23), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.INDIGENT);
			TacheEnvoiDeclarationImpotPP t = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 5, 23),
			                                                   TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, ours, null, null, colAdm);
			t.setAdresseRetour(TypeAdresseRetour.ACI);

			// Un contribuable normal avec une DI pré-existente sur toute l'année
			PersonnePhysique ramon = addNonHabitant("Ramon", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ramonId = ramon.getNumero();
			addForPrincipal(ramon, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, ramon, null, null, colAdm);
			addDeclarationImpot(ramon, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

			// Un contribuable normal avec une DI pré-existente sur une portion de l'année (= cas erroné)
			PersonnePhysique totor = addNonHabitant("Ramon", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.totorId = totor.getNumero();
			addForPrincipal(totor, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, totor, null, null, colAdm);
			addDeclarationImpot(totor, periode, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			return null;
		});

		doInNewTransaction(status -> {
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			final PersonnePhysique john = hibernateTemplate.get(PersonnePhysique.class, ids.johnId);
			final PersonnePhysique ours = hibernateTemplate.get(PersonnePhysique.class, ids.oursId);
			final PersonnePhysique ramon = hibernateTemplate.get(PersonnePhysique.class, ids.ramonId);
			final PersonnePhysique totor = hibernateTemplate.get(PersonnePhysique.class, ids.totorId);
			assertEmpty(eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertEmpty(ours.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, null, ramon.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertDIPP(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, null, totor.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric,
			                                                                                                                                                  2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(john,
			                                                                                                                                                  2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon,
			                                                                                                                                                  2007));
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(totor,
			                                                                                                                                                  2007));
			return null;
		});

		// Lance l'envoi en masse : eric (traité) +ours (indigent décédé donc quand même traité) + john (indigent) + ramon (ignoré), totor (en erreur)
		doInNewTransaction(status -> {
			EnvoiDIsPPResults r = service.envoyerDIsPPEnMasse(2007, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 100, date(2008, 1, 20), false, 1, null);
			assertResults(2, 1, 1, 1, r);
			return null;
		});

		doInNewTransaction(status -> {
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			final PersonnePhysique john = hibernateTemplate.get(PersonnePhysique.class, ids.johnId);
			final PersonnePhysique ours = hibernateTemplate.get(PersonnePhysique.class, ids.oursId);
			final PersonnePhysique ramon = hibernateTemplate.get(PersonnePhysique.class, ids.ramonId);
			final PersonnePhysique totor = hibernateTemplate.get(PersonnePhysique.class, ids.totorId);
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31), eric.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), TypeEtatDocumentFiscal.RETOURNE, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, date(2008, 3, 31), john.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertDIPP(date(2007, 1, 1), date(2007, 5, 23), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idAci, date(2008, 3, 25), ours.getDeclarationsDansPeriode(Declaration.class, 2007, false)); // [UNIREG-1852], [UNIREG-1861]
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, null, ramon.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertDIPP(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE,
			           TypeDocument.DECLARATION_IMPOT_VAUDTAX, idCedi, null, totor.getDeclarationsDansPeriode(Declaration.class, 2007, false));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(eric,
			                                                                                                                                                  2007));
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(john,
			                                                                                                                                                  2007));
			// ramon: la DI pré-existante corresponds parfaitement avec la tâche
			assertOneTache(TypeEtatTache.TRAITE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(ramon,
			                                                                                                                                                  2007));
			// totor: la DI pré-existante est en conflit avec la tâche => erreur
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 2, 1), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, getTachesEnvoiDeclarationImpot(totor,
			                                                                                                                                                  2007));
			return null;
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
		doInNewTransaction(status -> {
			PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
			ModeleDocument declarationVaudTax2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2006);
			ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2007));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2007);
			ModeleDocument declarationVaudTax2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2008));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2008);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2008);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2008);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2008);
			ModeleDocument declarationVaudTax2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, addPeriodeFiscale(2009));
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2009);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2009);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2009);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2009);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_310, declarationVaudTax2009);

			// Contribuable propriétaire d'un immeubles à Lausanne et ayant déménagé fin 2006 au Danemark.
			PersonnePhysique jacky = addNonHabitant("Jacky", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
			ids.jackyId = jacky.getNumero();
			addForPrincipal(jacky, date(1968, 11, 3), MotifFor.MAJORITE, date(2006, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
			addForPrincipal(jacky, date(2007, 1, 1), null, MockPays.Danemark);
			addForSecondaire(jacky, date(1990, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			addDeclarationImpot(jacky, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationVaudTax2006);

			// Contribuable propriétaire d'un immeubles à Lausanne et ayant déménagé mi 2007 au Danemark.
			PersonnePhysique thierry = addNonHabitant("Thierry", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
			ids.thierryId = thierry.getNumero();
			addForPrincipal(thierry, date(1968, 11, 3), MotifFor.MAJORITE, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
			addForPrincipal(thierry, date(2007, 7, 1), null, MockPays.Danemark);
			addForSecondaire(thierry, date(1990, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			addDeclarationImpot(thierry, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.HORS_SUISSE,
			                    declarationVaudTax2006);

			// Contribuable propriétaire d'un immeuble depuis mi-2007 à Lausanne et hors-Suisse depuis toujours
			PersonnePhysique lionel = addNonHabitant("Lionel", "Posjin", date(1948, 11, 3), Sexe.MASCULIN);
			ids.lionelId = lionel.getNumero();
			addForPrincipal(lionel, date(1968, 11, 3), null, MockPays.Danemark);
			addForSecondaire(lionel, date(2007, 5, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
			                 MotifRattachement.IMMEUBLE_PRIVE);
			return null;
		});

		{
			final PersonnePhysique jacky = hibernateTemplate.get(PersonnePhysique.class, ids.jackyId);
			final PersonnePhysique thierry = hibernateTemplate.get(PersonnePhysique.class, ids.thierryId);
			final PersonnePhysique lionel = hibernateTemplate.get(PersonnePhysique.class, ids.lionelId);

			// Envoi en masse 2007 pour les contribuables hors Suisse
			service.determineDIsPPAEmettre(2007, date(2008, 1, 15), 1, null);

			assertEmpty(getTachesEnvoiDeclarationImpot(jacky, 2007)); // au forfait en 2007
			// [UNIREG-1349] le contribuable possède un immeuble bien avant son départ HS: une tâche doit être générée immédiatement lors du
			// départ, mais l'immeuble n'ayant pas été acheté dans la même période on n'en génère pas ici
			// [UNIREG-1742] rattrapage des DI
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
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
		doInNewTransaction(status -> {
			PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
			addPeriodeFiscale(2007);
			ModeleDocument declarationVaudTax = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax);

			// Un tiers indigent depuis toujours
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.INDIGENT);

			// Un tiers indigent depuis 2007
			PersonnePhysique john = addNonHabitant("John", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.johnId = john.getNumero();
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, date(2006, 12, 31), MotifFor.CHGT_MODE_IMPOSITION,
			                MockCommune.Lausanne);
			addForPrincipal(john, date(2007, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.INDIGENT);
			addDeclarationImpot(john, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationVaudTax);

			// Un tiers indigent décédé en 2007
			PersonnePhysique paul = addNonHabitant("Paul", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.paulId = paul.getNumero();
			addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 5, 23), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.INDIGENT);
			return null;
		});

		{
			service.determineDIsPPAEmettre(2007, date(2008, 1, 20), 1, null);

			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			final PersonnePhysique john = hibernateTemplate.get(PersonnePhysique.class, ids.johnId);
			final PersonnePhysique paul = hibernateTemplate.get(PersonnePhysique.class, ids.paulId);

			List<TacheEnvoiDeclarationImpotPP> taches = getTachesEnvoiDeclarationImpot(eric, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, taches);

			taches = getTachesEnvoiDeclarationImpot(john, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, taches);

			// [UNIREG-1852] indigent décédé -> adresse de retour = ACI
			// [SIFISC-23095] en fait, maintenant, l'adresse de retour reste au CEDI
			taches = getTachesEnvoiDeclarationImpot(paul, 2007);
			assertOneTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 5, 23),
			               TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeAdresseRetour.CEDI, taches);
		}
	}

	/**
	 * @return toutes les tâches définies pour le contribuable et l'année spécifiés
	 */
	@SuppressWarnings("unchecked")
	private List<TacheEnvoiDeclarationImpotPP> getTachesEnvoiDeclarationImpot(ContribuableImpositionPersonnesPhysiques contribuable, int annee) {

		final Map<String, Object> params = new HashMap<>(3);
		params.put("ctb", contribuable);
		params.put("debutAnnee", date(annee, 1, 1));
		params.put("finAnnee", date(annee, 12, 31));

		final String query = "FROM TacheEnvoiDeclarationImpotPP AS t WHERE t.contribuable = :ctb AND t.dateDebut >= :debutAnnee AND t.dateFin <= :finAnnee ORDER BY t.dateDebut ASC";
		return hibernateTemplate.find(query, params, null);
	}

	private static void assertResults(int ctbsTraites, DeterminationDIsPPResults results) {
		assertEquals(ctbsTraites, results.traites.size());
	}

	private static void assertResults(int ctbsTraites, EnvoiDIsPPResults results) {
		assertEquals(ctbsTraites, results.ctbsAvecDiGeneree.size());
	}

	private static void assertResults(int ctbsTraites, int ctbsIndigents, int ctbsIgnores, int ctbsEnErrors, EnvoiDIsPPResults results) {
		assertEquals(ctbsTraites, results.ctbsAvecDiGeneree.size());
		assertEquals(ctbsIndigents, results.ctbsIndigents.size());
		assertEquals(ctbsIgnores, results.ctbsIgnores.size());
		assertEquals(ctbsEnErrors, results.ctbsEnErrors.size());
	}

	@Test
	public void testAnnulationDeclaration() throws Exception {

		final int annee = 2012;

		class Ids {
			Long pp;
			Long di;
		}
		final Ids ids = new Ids();

		// on créée un contribuable assujetti durant l'année 2012 avec une déclaration
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Robert", "Robert", date(1965, 3, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(annee, 1, 1), MotifFor.ARRIVEE_HC, date(annee, 12, 31), MotifFor.DEPART_HC, MockCommune.Aubonne);
			ids.pp = pp.getId();

			final PeriodeFiscale p2012 = addPeriodeFiscale(annee);
			final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, p2012);
			final DeclarationImpotOrdinaire di2012 = addDeclarationImpot(pp, p2012, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
			ids.di = di2012.getId();
			return null;
		});

		final Set<Long> fiscEventEnvoiDI = new HashSet<>();
		final Set<Long> fiscEventAnnulationDI = new HashSet<>();
		final Set<Integer> diEventEmission = new HashSet<>();
		final Set<Integer> diEventAnnulation = new HashSet<>();

		service.setEvenementFiscalService(new MockEvenementFiscalService() {
			@Override
			public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
				fiscEventEnvoiDI.add(di.getId());
			}

			@Override
			public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
				fiscEventAnnulationDI.add(di.getId());
			}
		});
		service.setEvenementDeclarationPPSender(new MockEvenementDeclarationPPSender() {
			@Override
			public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, RegDate date, String codeControle, String codeRoutage) {
				diEventEmission.add(periodeFiscale);
			}

			@Override
			public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, RegDate date) {
				diEventAnnulation.add(periodeFiscale);
			}
		});

		// on annule la déclaration
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.di);

			service.annulationDI(pp, di, null, RegDate.get());
			return null;
		});

		// on vérifie que la déclaration est bien annulée et que les événements fiscaux ont bien été envoyés
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
			assertTrue(di.isAnnule());

			assertEmpty(fiscEventEnvoiDI);
			assertEquals(1, fiscEventAnnulationDI.size());
			assertEquals(ids.di, fiscEventAnnulationDI.iterator().next());

			assertEmpty(diEventEmission);
			assertEquals(1, diEventAnnulation.size());
			assertEquals(Integer.valueOf(annee), diEventAnnulation.iterator().next());
			return null;
		});
	}

	@Test
	public void testAnnulationDeclarationAvecTacheAssociee() throws Exception {

		final int annee = 2011;
		class Ids {
			Long pp;
			Long di;
			Long tache;
		}

		// on créée un contribuable non-assujetti avec une déclaration
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Robert", "Robert", date(1965, 3, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(annee, 1, 1), MotifFor.ARRIVEE_HC, date(annee, 12, 12), MotifFor.DEPART_HC, MockCommune.Aubonne);      // il n'est pas assujetti !!!

			final PeriodeFiscale p2010 = addPeriodeFiscale(annee);
			final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, p2010);
			final DeclarationImpotOrdinaire di2010 = addDeclarationImpot(pp, p2010, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			di2010.addEtat(new EtatDeclarationRetournee(date(annee + 1, 1, 4), null));      // état retourné ajouté pour être certain que la tâche d'annulation ne va pas être traitée automatiquement

			final CollectiviteAdministrative ca = (CollectiviteAdministrative) tiersDAO.get(di2010.getRetourCollectiviteAdministrativeId());
			final Tache tache = addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di2010, pp, ca);

			final Ids ids1 = new Ids();
			ids1.pp = pp.getId();
			ids1.di = di2010.getId();
			ids1.tache = tache.getId();
			return ids1;
		});

		doInNewTransactionAndSession(status -> {
			final TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setContribuable((Contribuable) tiersDAO.get(ids.pp));
			final List<Tache> taches = tacheDAO.find(criterion);
			assertNotNull(taches);
			assertEquals(1, taches.size());

			final Tache tache = taches.get(0);
			assertNotNull(tache);
			assertEquals(ids.tache, tache.getId());
			assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
			return null;
		});

		final Set<Long> fiscEventEnvoiDI = new HashSet<>();
		final Set<Long> fiscEventAnnulationDI = new HashSet<>();
		final Set<Integer> diEventEmission = new HashSet<>();
		final Set<Integer> diEventAnnulation = new HashSet<>();

		service.setEvenementFiscalService(new MockEvenementFiscalService() {
			@Override
			public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
				fiscEventEnvoiDI.add(di.getId());
			}

			@Override
			public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
				fiscEventAnnulationDI.add(di.getId());
			}
		});
		service.setEvenementDeclarationPPSender(new MockEvenementDeclarationPPSender() {
			@Override
			public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, RegDate date, String codeControle, String codeRoutage) {
				diEventEmission.add(periodeFiscale);
			}

			@Override
			public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, RegDate date) {
				diEventAnnulation.add(periodeFiscale);
			}
		});

		// on annule la déclaration
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.di);

			service.annulationDI(pp, di, ids.tache, RegDate.get());
			return null;
		});

		// on vérifie que la déclaration est bien annulée et que la tâche est bien traitée
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
			assertTrue(di.isAnnule());

			final Tache tache = hibernateTemplate.get(TacheAnnulationDeclarationImpot.class, ids.tache);
			assertNotNull(tache);
			assertFalse(tache.isAnnule());
			assertEquals(TypeEtatTache.TRAITE, tache.getEtat());

			assertEmpty(fiscEventEnvoiDI);
			assertEquals(1, fiscEventAnnulationDI.size());
			assertEquals(ids.di, fiscEventAnnulationDI.iterator().next());

			assertEmpty(diEventEmission);
			assertEquals(1, diEventAnnulation.size());
			assertEquals(Integer.valueOf(annee), diEventAnnulation.iterator().next());
			return null;
		});
	}

	/**
	 * [SIFISC-5517] Vérifie que la désannulation d'une déclaration fonctionne bien et que les événements fiscaux et de DI sont bien envoyés.
	 */
	@Test
	public void testDesannulationDeclaration() throws Exception {

		final int annee = 2012;

		class Ids {
			Long pp;
			Long di;
		}
		final Ids ids = new Ids();

		// on créée un contribuable assujetti durant l'année 2012 avec une déclaration annulée
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Robert", "Robert", date(1965, 3, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(annee, 1, 1), MotifFor.ARRIVEE_HC, date(annee, 12, 31), MotifFor.DEPART_HC, MockCommune.Aubonne);
			ids.pp = pp.getId();

			final PeriodeFiscale p2012 = addPeriodeFiscale(annee);
			final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, p2012);
			final DeclarationImpotOrdinaire di2012 = addDeclarationImpot(pp, p2012, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
			di2012.setAnnule(true);
			ids.di = di2012.getId();
			return null;
		});

		final Set<Long> fiscEventEmissionDI = new HashSet<>();
		final Set<Long> fiscEventAnnulationDI = new HashSet<>();
		final Set<Integer> diEventEmission = new HashSet<>();
		final Set<Integer> diEventAnnulation = new HashSet<>();

		service.setEvenementFiscalService(new MockEvenementFiscalService() {
			@Override
			public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
				fiscEventEmissionDI.add(di.getId());
			}

			@Override
			public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
				fiscEventAnnulationDI.add(di.getId());
			}
		});
		service.setEvenementDeclarationPPSender(new MockEvenementDeclarationPPSender() {
			@Override
			public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, RegDate date, String codeControle, String codeRoutage) {
				diEventEmission.add(periodeFiscale);
			}

			@Override
			public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, RegDate date) {
				diEventAnnulation.add(periodeFiscale);
			}
		});

		// on désannule la déclaration
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.di);

			service.desannulationDI(pp, di, RegDate.get());
			return null;
		});

		// on vérifie que la déclaration est bien désannulée et que les événements fiscaux ont bien été envoyés
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
			assertFalse(di.isAnnule());

			assertEquals(1, fiscEventEmissionDI.size());
			assertEquals(ids.di, fiscEventEmissionDI.iterator().next());
			assertEmpty(fiscEventAnnulationDI);

			assertEquals(1, diEventEmission.size());
			assertEquals(Integer.valueOf(annee), diEventEmission.iterator().next());
			assertEmpty(diEventAnnulation);
			return null;
		});
	}

	/**
	 * [SIFISC-8598] Désannulation d'une DI 2011 qui avait été générée avant l'attribution généralisée des codes contrôles, ne doit rien envoyer à ADDI
	 */
	@Test
	public void testDesannulationDeclaration2011SansCodeControle() throws Exception {

		final int annee = 2011;

		class Ids {
			Long pp;
			Long di;
		}
		final Ids ids = new Ids();

		// on créée un contribuable assujetti durant l'année 2012 avec une déclaration annulée
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Robert", "Robert", date(1965, 3, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(annee, 1, 1), MotifFor.ARRIVEE_HC, date(annee, 12, 31), MotifFor.DEPART_HC, MockCommune.Aubonne);
			ids.pp = pp.getId();

			final PeriodeFiscale pf = addPeriodeFiscale(annee);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setAnnule(true);
			di.setCodeControle(null);
			ids.di = di.getId();
			return null;
		});

		final Set<Long> fiscEventEmissionDI = new HashSet<>();
		final Set<Long> fiscEventAnnulationDI = new HashSet<>();
		final Set<Integer> diEventEmission = new HashSet<>();
		final Set<Integer> diEventAnnulation = new HashSet<>();

		service.setEvenementFiscalService(new MockEvenementFiscalService() {
			@Override
			public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
				fiscEventEmissionDI.add(di.getId());
			}

			@Override
			public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
				fiscEventAnnulationDI.add(di.getId());
			}
		});
		service.setEvenementDeclarationPPSender(new MockEvenementDeclarationPPSender() {
			@Override
			public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, RegDate date, String codeControle, String codeRoutage) {
				diEventEmission.add(periodeFiscale);
			}

			@Override
			public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, RegDate date) {
				diEventAnnulation.add(periodeFiscale);
			}
		});

		// on désannule la déclaration
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.di);

			service.desannulationDI(pp, di, RegDate.get());
			return null;
		});

		// on vérifie que la déclaration est bien désannulée et que les événements fiscaux ont bien été envoyés
		doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.pp);
			final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
			assertFalse(di.isAnnule());

			assertEquals(1, fiscEventEmissionDI.size());
			assertEquals(ids.di, fiscEventEmissionDI.iterator().next());
			assertEmpty(fiscEventAnnulationDI);

			assertEmpty(diEventEmission);
			assertEmpty(diEventAnnulation);
			return null;
		});
	}

	/**
	 * [SIFISC-5208] Vérifie qu'il est possible de quittancer plusieurs fois le retour d'une DI, et que toutes les états retournés sont mémorisés tels quels (sans annuler l'état précédent).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testQuittancementDIMultiples() throws Exception {

		class Ids {
			public long ericId;
		}
		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(status -> {
			PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
			ModeleDocument declarationVaudTax2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2006);

			PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
			ModeleDocument declarationVaudTax2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationVaudTax2007);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationVaudTax2007);

			// Un tiers tout ce quil y a de plus ordinaire
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationVaudTax2006);
			return null;
		});

		// 1er quittance de la declaration
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, date(2007, 5, 12), "TEST0", true);
			return null;
		});

		// On s'assure que l'état retourné est bien enregistré
		doInNewTransaction(status -> {
			Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());

			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			assertEquals(date(2007, 5, 12), di.getDateRetour());

			final Set<EtatDeclaration> etats = di.getEtatsDeclaration();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final Iterator<EtatDeclaration> iterator = etats.iterator();

			final EtatDeclarationRetournee etat0 = (EtatDeclarationRetournee) iterator.next();
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat0.getEtat());
			assertEquals(date(2007, 5, 12), etat0.getDateObtention());
			assertEquals("TEST0", etat0.getSource());
			assertFalse(etat0.isAnnule());

			// l'état retourné est le dernier, comme il se doit
			assertSame(etat0, di.getDernierEtatDeclaration());
			assertSame(etat0, di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
			return null;
		});

		// 2ème quittance de la declaration
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, date(2007, 10, 28), "TEST1", true);
			return null;
		});

		// On s'assure que le nouvel état retourné est aussi enregistré
		doInNewTransaction(status -> {
			Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId); // ordinaire
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());

			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			assertEquals(date(2007, 10, 28), di.getDateRetour());

			final List<EtatDeclaration> etats = di.getEtatsDeclarationSorted();
			assertNotNull(etats);
			assertEquals(2, etats.size());

			final EtatDeclarationRetournee etat0 = (EtatDeclarationRetournee) etats.get(0);
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat0.getEtat());
			assertEquals(date(2007, 5, 12), etat0.getDateObtention());
			assertEquals("TEST0", etat0.getSource());
			assertFalse(etat0.isAnnule());

			final EtatDeclarationRetournee etat1 = (EtatDeclarationRetournee) etats.get(1);
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat1.getEtat());
			assertEquals(date(2007, 10, 28), etat1.getDateObtention());
			assertEquals("TEST1", etat1.getSource());
			assertFalse(etat1.isAnnule());

			// le nouvel état retourné doit être le dernier
			assertSame(etat1, di.getDernierEtatDeclaration());
			assertSame(etat1, di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
			return null;
		});
	}

	@Test
	public void testMonoQuittancements() throws Exception {

		class Ids {
			public long ericId;
		}
		final Ids ids = new Ids();

		// les quittancements avec cette source ne doivent pas être multiples
		final String SOURCE = "FAR_FAR_AWAY";
		service.setSourcesMonoQuittancement(new HashSet<>(Collections.singletonList(SOURCE)));

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(status -> {
			PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
			ModeleDocument declarationComplete2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2006);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2006);

			// Un tiers tout ce quil y a de plus ordinaire
			PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			ids.ericId = eric.getNumero();
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationComplete2006);
			return null;
		});

		// 1er quittance de la declaration
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, date(2007, 5, 12), SOURCE, true);
			return null;
		});

		// On s'assure que l'état retourné est bien enregistré
		doInNewTransaction(status -> {
			Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());

			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			assertEquals(date(2007, 5, 12), di.getDateRetour());

			final Set<EtatDeclaration> etats = di.getEtatsDeclaration();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final Iterator<EtatDeclaration> iterator = etats.iterator();

			final EtatDeclarationRetournee etat0 = (EtatDeclarationRetournee) iterator.next();
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat0.getEtat());
			assertEquals(date(2007, 5, 12), etat0.getDateObtention());
			assertEquals(SOURCE, etat0.getSource());
			assertFalse(etat0.isAnnule());

			// l'état retourné est le dernier, comme il se doit
			assertSame(etat0, di.getDernierEtatDeclaration());
			assertSame(etat0, di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
			return null;
		});

		// 2ème quittance de la declaration, source différence
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, date(2007, 10, 25), "TEST", true);
			return null;
		});

		// 2ème quittance de la declaration, source initiale qui ne supporte pas le multi-quittancement
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());
			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			service.quittancementDI(eric, di, date(2007, 10, 28), SOURCE, true);
			return null;
		});

		// on s'assure maintenant que premier quittancement de la source est bien annulé au profit du second, mais que les quittancements de sources annexes n'ont pas été touchés
		doInNewTransaction(status -> {
			final Contribuable eric = hibernateTemplate.get(Contribuable.class, ids.ericId);
			final List<DeclarationImpotOrdinaire> dis = eric.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, 2006, false);
			assertEquals(1, dis.size());

			final DeclarationImpotOrdinaire di = dis.get(0);
			assertNotNull(di);
			assertEquals(date(2007, 10, 28), di.getDateRetour());

			final List<EtatDeclaration> etats = di.getEtatsDeclarationSorted();
			assertNotNull(etats);
			assertEquals(3, etats.size());

			final EtatDeclarationRetournee etat0 = (EtatDeclarationRetournee) etats.get(0);
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat0.getEtat());
			assertEquals(date(2007, 5, 12), etat0.getDateObtention());
			assertEquals(SOURCE, etat0.getSource());
			assertTrue(etat0.isAnnule());

			final EtatDeclarationRetournee etat1 = (EtatDeclarationRetournee) etats.get(1);
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat1.getEtat());
			assertEquals(date(2007, 10, 25), etat1.getDateObtention());
			assertEquals("TEST", etat1.getSource());
			assertFalse(etat1.isAnnule());

			final EtatDeclarationRetournee etat2 = (EtatDeclarationRetournee) etats.get(2);
			assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat2.getEtat());
			assertEquals(date(2007, 10, 28), etat2.getDateObtention());
			assertEquals(SOURCE, etat2.getSource());
			assertFalse(etat2.isAnnule());

			// le nouvel état retourné doit être le dernier
			assertSame(etat2, di.getDernierEtatDeclaration());
			assertSame(etat2, di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
			return null;
		});
	}

	/**
	 * Code segment PM <BR /> 1: PM Vaudoise
	 *
	 * @throws DeclarationException
	 */
	@Test
	public void testComputeCodeRoutagePMVD() throws DeclarationException {
		Entreprise entreprise = new Entreprise();
		RegDate dateReference = date(2017, 9, 23);

		// For fiscal principal
		Set<ForFiscal> forFiscalSet = new HashSet<ForFiscal>();
		entreprise.setForsFiscaux(forFiscalSet);
		ForFiscalPrincipalPM forFiscalPrincipalPM =
				new ForFiscalPrincipalPM(date(2017, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE);
		forFiscalSet.add(forFiscalPrincipalPM);

		// Régime fiscal
		ch.vd.unireg.tiers.RegimeFiscal regimeFiscal = new ch.vd.unireg.tiers.RegimeFiscal(date(2017, 1, 1), null, ch.vd.unireg.tiers.RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode());
		regimeFiscal.setEntreprise(entreprise);
		entreprise.addRegimeFiscal(regimeFiscal);

		assertEquals(EnumCodeRoutageDI.PM_VAUDOISE, service.computeCodeRoutage(entreprise, dateReference, TypeDocument.DECLARATION_IMPOT_PM_LOCAL));
	}

	/**
	 * Code segment PM <BR/> 2: APM
	 *
	 * @throws DeclarationException
	 */
	@Test
	public void testComputeCodeRoutageAPMNonExoneree() throws DeclarationException {
		Entreprise entreprise = new Entreprise();
		RegDate dateReference = date(2017, 9, 23);

		// for fiscal principal
		Set<ForFiscal> forFiscalSet = new HashSet<ForFiscal>();
		entreprise.setForsFiscaux(forFiscalSet);
		ForFiscalPrincipalPM forFiscalPrincipalPM =
				new ForFiscalPrincipalPM(date(2017, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE);
		forFiscalSet.add(forFiscalPrincipalPM);

		// Régime fiscal
		ch.vd.unireg.tiers.RegimeFiscal regimeFiscal = new ch.vd.unireg.tiers.RegimeFiscal(date(2017, 1, 1), null, ch.vd.unireg.tiers.RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_APM.getCode());
		regimeFiscal.setEntreprise(entreprise);
		entreprise.addRegimeFiscal(regimeFiscal);

		assertEquals(EnumCodeRoutageDI.APM_NON_EXONEREE, service.computeCodeRoutage(entreprise, dateReference, TypeDocument.DECLARATION_IMPOT_APM_LOCAL));
	}

	@Test
	public void testComputeCodeRoutageAPMExoneree() throws DeclarationException {
		Entreprise entreprise = new Entreprise();
		RegDate dateReference = date(2017, 9, 23);

		// for fiscal principal
		Set<ForFiscal> forFiscalSet = new HashSet<ForFiscal>();
		entreprise.setForsFiscaux(forFiscalSet);
		ForFiscalPrincipalPM forFiscalPrincipalPM =
				new ForFiscalPrincipalPM(date(2017, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE);
		forFiscalSet.add(forFiscalPrincipalPM);

		// Régime fiscal
		ch.vd.unireg.tiers.RegimeFiscal regimeFiscal = new ch.vd.unireg.tiers.RegimeFiscal(date(2017, 1, 1), null, ch.vd.unireg.tiers.RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ART90D.getCode());
		regimeFiscal.setEntreprise(entreprise);
		entreprise.addRegimeFiscal(regimeFiscal);

		assertEquals(EnumCodeRoutageDI.APM_EXONEREE, service.computeCodeRoutage(entreprise, dateReference, TypeDocument.DECLARATION_IMPOT_APM_LOCAL));
	}

	/**
	 * Code segment PM <BR/> 3: PM HS (Hors Suisse)
	 *
	 * @throws DeclarationException
	 */
	@Test
	public void testComputeCodeRoutagePMHS() throws DeclarationException {
		Entreprise entreprise = new Entreprise();
		RegDate dateReference = date(2017, 9, 23);

		// For fiscal principal
		Set<ForFiscal> forFiscalSet = new HashSet<ForFiscal>();
		entreprise.setForsFiscaux(forFiscalSet);
		ForFiscalPrincipalPM forFiscalPrincipalPM = new ForFiscalPrincipalPM(date(2017, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, null, TypeAutoriteFiscale.PAYS_HS, MotifRattachement.ETABLISSEMENT_STABLE);
		forFiscalSet.add(forFiscalPrincipalPM);

		// Régime fiscal
		ch.vd.unireg.tiers.RegimeFiscal regimeFiscal = new ch.vd.unireg.tiers.RegimeFiscal(date(2017, 1, 1), null, ch.vd.unireg.tiers.RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode());
		regimeFiscal.setEntreprise(entreprise);
		entreprise.addRegimeFiscal(regimeFiscal);

		assertEquals(EnumCodeRoutageDI.PM_HORS_SUISSE, service.computeCodeRoutage(entreprise, dateReference, TypeDocument.DECLARATION_IMPOT_PM_BATCH));
	}

	/**
	 * Code segment PM <BR/> 4: PM Holding
	 *
	 * @throws DeclarationException
	 */
	@Test
	public void testComputeCodeRoutagePMHolding() throws DeclarationException {
		Entreprise entreprise = new Entreprise();
		RegDate dateReference = date(2017, 9, 23);

		// For fiscal principal
		Set<ForFiscal> forFiscalSet = new HashSet<ForFiscal>();
		entreprise.setForsFiscaux(forFiscalSet);
		ForFiscalPrincipalPM forFiscalPrincipalPM =
				new ForFiscalPrincipalPM(date(2017, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE);
		forFiscalSet.add(forFiscalPrincipalPM);

		// Régime fiscal
		ch.vd.unireg.tiers.RegimeFiscal regimeFiscal = new ch.vd.unireg.tiers.RegimeFiscal(date(2017, 1, 1), null, ch.vd.unireg.tiers.RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.STE_BASE_MIXTE.getCode());
		regimeFiscal.setEntreprise(entreprise);
		entreprise.addRegimeFiscal(regimeFiscal);

		assertEquals(EnumCodeRoutageDI.PM_HOLDING, service.computeCodeRoutage(entreprise, dateReference, TypeDocument.DECLARATION_IMPOT_PM_LOCAL));


	}

	/**
	 * Code segment PM <BR /> 5: PM HC (Hors Canton)
	 *
	 * @throws DeclarationException
	 */
	@Test
	public void testComputeCodeRoutagePMHC() throws DeclarationException {
		Entreprise entreprise = new Entreprise();
		RegDate dateReference = date(2017, 9, 23);

		// For fiscal principal
		Set<ForFiscal> forFiscalSet = new HashSet<ForFiscal>();
		entreprise.setForsFiscaux(forFiscalSet);
		ForFiscalPrincipalPM forFiscalPrincipalPM = new ForFiscalPrincipalPM(date(2017, 1, 1), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.ETABLISSEMENT_STABLE);
		forFiscalSet.add(forFiscalPrincipalPM);

		// Régime fiscal
		ch.vd.unireg.tiers.RegimeFiscal regimeFiscal = new ch.vd.unireg.tiers.RegimeFiscal(date(2017, 1, 1), null, ch.vd.unireg.tiers.RegimeFiscal.Portee.VD, "23");
		regimeFiscal.setEntreprise(entreprise);
		entreprise.addRegimeFiscal(regimeFiscal);

		assertEquals(EnumCodeRoutageDI.PM_HORS_CANTON, service.computeCodeRoutage(entreprise, dateReference, TypeDocument.DECLARATION_IMPOT_PM_LOCAL));
	}

	@Test
	public void testAjouterDelaiAccordeDIAnnulee() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.setAnnule(true);

		try {
			service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addMonths(2), EtatDelaiDocumentFiscal.ACCORDE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DECLARATION_ANNULEE, e.getRaison());
			assertEquals("La déclaration est annulée.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiRefuseDIAnnulee() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.setAnnule(true);

		try {
			service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addMonths(2), EtatDelaiDocumentFiscal.REFUSE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DECLARATION_ANNULEE, e.getRaison());
			assertEquals("La déclaration est annulée.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiAccordeDIMauvaisEtat() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());
		di.addEtat(new EtatDeclarationSommee());

		try {
			service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addMonths(2), EtatDelaiDocumentFiscal.ACCORDE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.MAUVAIS_ETAT_DECLARATION, e.getRaison());
			assertEquals("La déclaration n'est pas dans l'état 'EMIS'.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiRefuseDIMauvaisEtat() throws AjoutDelaiDeclarationException {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());
		di.addEtat(new EtatDeclarationSommee());

		service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addMonths(2), EtatDelaiDocumentFiscal.REFUSE, null);

		// [FISCPROJ-1068] les délais refusés doivent être acceptés, même sur une déclaration non-émise
		final Set<DelaiDocumentFiscal> delais = di.getDelais();
		assertEquals(1, delais.size());
		final DelaiDocumentFiscal delai0 = delais.iterator().next();
		assertEquals(EtatDelaiDocumentFiscal.REFUSE, delai0.getEtat());
		assertNull(delai0.getDelaiAccordeAu());
	}

	@Test
	public void testAjouterDelaiAccordeDIDateObtentionInvalide() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		try {
			service.ajouterDelaiDI(di, RegDate.get().addMonths(2), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.ACCORDE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DATE_OBTENTION_INVALIDE, e.getRaison());
			assertEquals("La date d'obtention du délai ne peut pas être dans le futur de la date du jour.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiRefuseDIDateObtentionInvalide() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		try {
			service.ajouterDelaiDI(di, RegDate.get().addMonths(2), RegDate.get().addMonths(6), EtatDelaiDocumentFiscal.REFUSE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DATE_OBTENTION_INVALIDE, e.getRaison());
			assertEquals("La date d'obtention du délai ne peut pas être dans le futur de la date du jour.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiAccordeDIDateDelaiAvantAujourdhui() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		try {
			service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addDays(-10), EtatDelaiDocumentFiscal.ACCORDE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DATE_DELAI_INVALIDE, e.getRaison());
			assertEquals("Un nouveau délai ne peut pas être demandé dans le passé de la date du jour.", e.getMessage());
		}
	}

	/**
	 * [FISCPROJ-754]
	 */
	@Test
	public void testAjouterDelaiAccordeDIDateDelaiIdentiqueDelaiExistant() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(RegDate.get());
		delai.setDelaiAccordeAu(RegDate.get().addDays(10));
		di.addDelai(delai);

		try {
			service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addDays(10), EtatDelaiDocumentFiscal.ACCORDE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DELAI_DEJA_EXISTANT, e.getRaison());
			assertEquals("Un délai à la date demandée existe déjà.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiAccordeDIDateDelaiAvantDelaiExistant() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(RegDate.get());
		delai.setDelaiAccordeAu(RegDate.get().addDays(10));
		di.addDelai(delai);

		try {
			service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addDays(1), EtatDelaiDocumentFiscal.ACCORDE, null);
			fail();
		}
		catch (AjoutDelaiDeclarationException e) {
			assertEquals(Raison.DATE_DELAI_INVALIDE, e.getRaison());
			assertEquals("Un délai plus lointain existe déjà.", e.getMessage());
		}
	}

	@Test
	public void testAjouterDelaiAccordeDICasPassant() throws AjoutDelaiDeclarationException {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(RegDate.get());
		delai.setDelaiAccordeAu(RegDate.get().addDays(1));
		di.addDelai(delai);

		service.ajouterDelaiDI(di, RegDate.get(), RegDate.get().addDays(10), EtatDelaiDocumentFiscal.ACCORDE, null);

		final DelaiDeclaration dernier = di.getDernierDelaiDeclarationAccorde();
		assertNotNull(dernier);
		assertEquals(EtatDelaiDocumentFiscal.ACCORDE, dernier.getEtat());
		assertEquals(RegDate.get().addDays(10), dernier.getDelaiAccordeAu());
		assertEquals(RegDate.get(), dernier.getDateTraitement());
	}

	@Test
	public void testAjouterDelaiRefuseDICasPassant() throws AjoutDelaiDeclarationException {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addEtat(new EtatDeclarationEmise());

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(RegDate.get().addMonths(-1));
		delai.setDelaiAccordeAu(RegDate.get().addDays(1));
		di.addDelai(delai);

		service.ajouterDelaiDI(di, RegDate.get(), null, EtatDelaiDocumentFiscal.REFUSE, null);

		final List<DelaiDocumentFiscal> delais = di.getDelaisSorted();
		assertEquals(2, delais.size());

		final DelaiDeclaration dernier = (DelaiDeclaration) delais.get(1);
		assertNotNull(dernier);
		assertEquals(EtatDelaiDocumentFiscal.REFUSE, dernier.getEtat());
		assertNull(dernier.getDelaiAccordeAu());
		assertEquals(RegDate.get(), dernier.getDateTraitement());
	}

	@Test
	public void testCodeControleIdentiquePourPlusiersDiPMsurMemePFCasPassant() throws Exception {
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2000, 2, 1), DayMonth.get(12, 31), 12);              // tous les 31.12 depuis 2000
			addRegimeFiscalVD(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(pm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));

			//Annulation de la demande
			diService.annulationDI(pm, (DeclarationImpotOrdinairePM) pm.getDeclarations().iterator().next(), null, RegDate.get());

			// Envoi de la 2e demande
			addDeclarationImpot(pm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			return pm.getNumero();
		});
		verificationCodeControle(pmId);
	}

	@Test
	public void testCodeControleIdentiquePourPlusiersDiAPMsurMemePFCasPassant() throws Exception {
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise apm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(apm, date(2000, 2, 1), DayMonth.get(12, 31), 12);              // tous les 31.12 depuis 2000
			addRegimeFiscalVD(apm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(apm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(apm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(apm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));

			//Annulation de la demande
			diService.annulationDI(apm, (DeclarationImpotOrdinairePM) apm.getDeclarations().iterator().next(), null, RegDate.get());

			// Envoi de la 2e demande
			addDeclarationImpot(apm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			return apm.getNumero();
		});
		verificationCodeControle(pmId);
	}

	@Test
	public void testCodeControleIdentiquePourPlusiersDiAPMsurMemePF() throws Exception {
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise apm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(apm, date(2000, 2, 1), DayMonth.get(12, 31), 12);              // tous les 31.12 depuis 2000
			addRegimeFiscalVD(apm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(apm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(apm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

			//1er Di sur 2017
			final DeclarationImpotOrdinairePM di2017_1 = addDeclarationImpot(apm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di2017_1.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));

			//1e DI sur 2016
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, periode2016);
			final DeclarationImpotOrdinairePM di2016 = addDeclarationImpot(apm, periode2016, date(2016, 1, 1), date(2016, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
			di2016.addEtat(new EtatDeclarationEmise(date(2017, 1, 15)));

			//Annulation de la demande
			diService.annulationDI(apm, di2017_1, null, RegDate.get());

			//2e DI sur 2017
			final DeclarationImpotOrdinairePM di2017_2 = addDeclarationImpot(apm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di2017_2.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));

			Assert.assertEquals(3, apm.getDeclarations().size());

			// verif code de controle 2016
			final Optional<Declaration> declaration2016 = apm.getDeclarations().stream()
					.filter(declaration -> declaration.getPeriode().equals(periode2016))
					.findFirst();
			Assert.assertTrue(declaration2016.isPresent());
			Assert.assertTrue("Vérifier que le code de controle a bien été généré", StringUtils.isNotBlank(((DeclarationImpotOrdinairePM) declaration2016.get()).getCodeControle()));


			final List<Declaration> declarations2017 = apm.getDeclarations().stream()
					.filter(declaration -> declaration.getPeriode().equals(periode2017))
					.collect(Collectors.toList());
			Assert.assertEquals(2, declarations2017.size());
			Assert.assertEquals("Vérifier que les code de controle de la DI 2017 sont bien identique", ((DeclarationImpotOrdinairePM) declarations2017.get(0)).getCodeControle(),
			                    ((DeclarationImpotOrdinairePM) declarations2017.get(1)).getCodeControle());


			Assert.assertNotEquals("vérifier que le code de controle de la DI 2016 est bien différent de celui de la DI 2017",
			                       ((DeclarationImpotOrdinairePM) declarations2017.get(0)).getCodeControle(),
			                       ((DeclarationImpotOrdinairePM) declaration2016.get()).getCodeControle());
			return apm.getNumero();
		});

	}

	@Test
	public void testAjouterDemandeLiberationValide() throws Exception {
		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		service.ajouterDemandeLiberationDI(di, "test", "ZAIZZT", "businessID");
		assertNotNull(di.getLiberations());
		assertEquals(di.getLiberations().size(),1);
	}

	@Test
	public void testAjouterDemandeLiberationInvalide() {
		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.addLiberation(new LiberationDeclaration());

		try {
			service.ajouterDemandeLiberationDI(di, "test", "ZAIZZT", "businessID");
			fail();
		}
		catch (AjoutDemandeLiberationException e) {
			assertEquals(AjoutDemandeLiberationException.Raison.LIBERATION_DEJA_EXISTANT, e.getRaison());
		}
	}

	private void verificationCodeControle(long pmId) throws Exception {
		doInNewTransactionAndSession(status -> {
			Entreprise pm = (Entreprise) tiersDAO.get(pmId);
			Assert.assertEquals(2, pm.getDeclarations().size());
			final List<Declaration> declarations = new ArrayList<>(pm.getDeclarations());
			Assert.assertEquals(((DeclarationImpotOrdinairePM) declarations.get(0)).getCodeControle(), ((DeclarationImpotOrdinairePM) declarations.get(1)).getCodeControle());
			return pm.getNumero();
		});
	}
}
