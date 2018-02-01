package ch.vd.unireg.evenement.fiscal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalBatiment;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalImplantationBatiment;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalServitude;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementFiscalServiceTest extends BusinessTest {

	private EvenementFiscalService evenementFiscalService;
	private CollectingEvenementFiscalSender evenementFiscalSender;
	private EvenementFiscalDAO evenementFiscalDAO;


	private TiersDAO tiersDAO;

	public EvenementFiscalServiceTest() {
		super();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		evenementFiscalSender = getBean(CollectingEvenementFiscalSender.class, "evenementFiscalSender");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testPublierEvenementFor() throws Exception {

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final PersonnePhysique pp = addNonHabitant("Laurent", "Schmidt", date(1970, 5, 27), Sexe.MASCULIN);
			return pp.getNumero();
		});

		// création d'un for et envoi d'un événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(id);
				assertNotNull(pp);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addDays(-5), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				evenementFiscalService.publierEvenementFiscalOuvertureFor(ffp);
			}
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		// Vérifie que l'événement est dans la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertEquals(1, events.size());
				assertForEvent(id, RegDate.get().addDays(-5), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, events.get(0));
			}
		});
	}

	@Test
	public void publierEvenementFiscalRetourLR() throws Exception {

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			// le DPI
			final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
			return debiteur.getNumero();
		});

		// création d'une LR et publication d'un événement correspondant
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				assertNotNull(dpi);
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, date(2009, 12, 31), MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.ANNUEL, pf);
				evenementFiscalService.publierEvenementFiscalEmissionListeRecapitulative(lr, RegDate.get().addDays(-2));
			}
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// Vérifie que l'événement est dans la base
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertEquals(1, events.size());
				assertDeclarationEvent(id, RegDate.get().addDays(-2), EvenementFiscalDeclarationSommable.TypeAction.EMISSION, date(2009, 1, 1), date(2009, 12, 31), DeclarationImpotSource.class, events.get(0));
			}
		});
	}

	@Test
	public void publierEvenementFiscalChangementSituation() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final PersonnePhysique pp = addNonHabitant("Laurent", "Schmidt", date(1970, 4, 2), Sexe.MASCULIN);
			evenementFiscalService.publierEvenementFiscalChangementSituationFamille(RegDate.get().addDays(-3), pp);
			return pp.getNumero();
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// Vérifie que l'événement est dans la base
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertEquals(1, events.size());
				assertCSFEvent(id, RegDate.get().addDays(-3), events.get(0));
			}
		});
	}

	@Test
	public void testPublierCreationBatiment() throws Exception {

		// appel du service
		final Long id = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final BatimentRF batiment = addBatimentRF("32099903");
			evenementFiscalService.publierCreationBatiment(RegDate.get(2000, 1, 1), batiment);
			return batiment.getId();
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalBatiment event0 = (EvenementFiscalBatiment) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalBatiment.TypeEvenementFiscalBatiment.CREATION, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(id, event0.getBatiment().getId());
			return null;
		});
	}

	@Test
	public void testPublierRadiationBatiment() throws Exception {

		// appel du service
		final Long id = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final BatimentRF batiment = addBatimentRF("32099903");
			evenementFiscalService.publierRadiationBatiment(RegDate.get(2000, 1, 1), batiment);
			return batiment.getId();
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalBatiment event0 = (EvenementFiscalBatiment) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalBatiment.TypeEvenementFiscalBatiment.RADIATION, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(id, event0.getBatiment().getId());
			return null;
		});
	}

	@Test
	public void testPublierModificationDescriptionBatiment() throws Exception {

		// appel du service
		final Long id = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final BatimentRF batiment = addBatimentRF("32099903");
			evenementFiscalService.publierModificationDescriptionBatiment(RegDate.get(2000, 1, 1), batiment);
			return batiment.getId();
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalBatiment event0 = (EvenementFiscalBatiment) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalBatiment.TypeEvenementFiscalBatiment.MODIFICATION_DESCRIPTION, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(id, event0.getBatiment().getId());
			return null;
		});
	}

	@Test
	public void testPublierOuvertureDroitPropriete() throws Exception {

		class Ids {
			Long immeuble;
			Long pp;

			public Ids(Long immeuble, Long pp) {
				this.immeuble = immeuble;
				this.pp = pp;
			}
		}

		// appel du service
		final Ids ids = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final CommuneRF commune = addCommuneRF(12, "Echallens", 2322);
			final BienFondsRF immeuble = addBienFondsRF("39393", "CH28282", commune, 212);
			final PersonnePhysiqueRF pp = addPersonnePhysiqueRF("383883", "Jean", "Jean", RegDate.get(1970, 1, 1));
			final DroitProprietePersonnePhysiqueRF droit =
					addDroitPropriete(pp, immeuble, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
					                  RegDate.get(2000, 1, 1), null, RegDate.get(2000, 1, 1), null, "Achat", null,
					                  new IdentifiantAffaireRF(12, 2000, 1, null), "232323232", "2323");

			evenementFiscalService.publierOuvertureDroitPropriete(RegDate.get(2000, 1, 1), droit);

			return new Ids(immeuble.getId(), pp.getId());
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(ids.immeuble, event0.getDroit().getImmeuble().getId());
			assertEquals(ids.pp, event0.getDroit().getAyantDroit().getId());
			return null;
		});
	}

	@Test
	public void testPublierOuvertureServitude() throws Exception {

		class Ids {
			Long immeuble;
			Long pp;

			public Ids(Long immeuble, Long pp) {
				this.immeuble = immeuble;
				this.pp = pp;
			}
		}

		// appel du service
		final Ids ids = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final CommuneRF commune = addCommuneRF(12, "Echallens", 2322);
			final BienFondsRF immeuble = addBienFondsRF("39393", "CH28282", commune, 212);
			final PersonnePhysiqueRF pp = addPersonnePhysiqueRF("383883", "Jean", "Jean", RegDate.get(1970, 1, 1));

			final UsufruitRF usufruit = addUsufruitRF(RegDate.get(2000, 1, 1), RegDate.get(2000, 1, 1), null, null, "Convention", null,
			                                            "23233", "29292", new IdentifiantAffaireRF(12, 2000, 1, null), new IdentifiantDroitRF(12, 2000, 1), pp, immeuble);

			evenementFiscalService.publierOuvertureServitude(RegDate.get(2000, 1, 1), usufruit);

			return new Ids(immeuble.getId(), pp.getId());
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalServitude event0 = (EvenementFiscalServitude) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertTrue(event0.getServitude() instanceof UsufruitRF);
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());

			final List<Long> immeublesIds = event0.getServitude().getImmeubles().stream()
					.map(ImmeubleRF::getId)
					.collect(Collectors.toList());
			assertEquals(Collections.singletonList(ids.immeuble), immeublesIds);

			final List<Long> ayantDroitIds = event0.getServitude().getAyantDroits().stream()
					.map(AyantDroitRF::getId)
					.collect(Collectors.toList());
			assertEquals(Collections.singletonList(ids.pp), ayantDroitIds);

			return null;
		});
	}

	@Test
	public void testPublierCreationImmeuble() throws Exception {

		// appel du service
		final Long id = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final CommuneRF commune = addCommuneRF(12, "Echallens", 2322);
			final BienFondsRF immeuble = addBienFondsRF("39393", "CH28282", commune, 212);

			evenementFiscalService.publierCreationImmeuble(RegDate.get(2000, 1, 1), immeuble);

			return immeuble.getId();
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.CREATION, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(id, event0.getImmeuble().getId());

			return null;
		});
	}

	@Test
	public void testPublierDebutEstimationFiscale() throws Exception {

		// appel du service
		final Long id = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final CommuneRF commune = addCommuneRF(12, "Echallens", 2322);
			final BienFondsRF immeuble = addBienFondsRF("39393", "CH28282", commune, 212);
			final EstimationRF estimation = addEstimationFiscale(RegDate.get(2000, 1, 1), RegDate.get(2000, 1, 1), null, false, 100_000L, "2017", immeuble);

			evenementFiscalService.publierDebutEstimationFiscalImmeuble(RegDate.get(2000, 1, 1), estimation);

			return immeuble.getId();
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.DEBUT_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(id, event0.getImmeuble().getId());

			return null;
		});
	}

	@Test
	public void testPublierDebutImplantation() throws Exception {

		class Ids {
			Long immeuble;
			Long batiment;

			public Ids(Long immeuble, Long batiment) {
				this.immeuble = immeuble;
				this.batiment = batiment;
			}
		}

		// appel du service
		final Ids ids = doInNewTransaction(status -> {
			evenementFiscalSender.reset();
			assertEquals(0, evenementFiscalDAO.getAll().size());

			final CommuneRF commune = addCommuneRF(12, "Echallens", 2322);
			final BienFondsRF immeuble = addBienFondsRF("39393", "CH28282", commune, 212);
			BatimentRF batiment = addBatimentRF("32099903");
			addImplantationRF(RegDate.get(2000, 1, 1), null, 100, immeuble, batiment);

			batiment = hibernateTemplate.merge(batiment);
			final ImplantationRF implantation = batiment.getImplantations().iterator().next();

			evenementFiscalService.publierDebutImplantationBatiment(RegDate.get(2000, 1, 1), implantation);

			return new Ids(immeuble.getId(), batiment.getId());
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		doInNewTransaction(status -> {
			// Vérifie que l'événement est dans la base
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImplantationBatiment event0 = (EvenementFiscalImplantationBatiment) events.get(0);
			assertNotNull(event0);
			assertEquals(EvenementFiscalImplantationBatiment.TypeEvenementFiscalImplantation.CREATION, event0.getType());
			assertEquals(RegDate.get(2000, 1, 1), event0.getDateValeur());
			assertEquals(ids.batiment, event0.getImplantation().getBatiment().getId());
			assertEquals(ids.immeuble, event0.getImplantation().getImmeuble().getId());
			return null;
		});
	}

	private static void assertCSFEvent(Long tiersId, RegDate dateEvenement, EvenementFiscal event) {
		assertTiersEvent(tiersId, dateEvenement, EvenementFiscalSituationFamille.class, event);
	}

	private static void assertForEvent(Long tiersId, RegDate dateEvenement, EvenementFiscalFor.TypeEvenementFiscalFor type, EvenementFiscal event) {
		assertTiersEvent(tiersId, dateEvenement, EvenementFiscalFor.class, event);
		final EvenementFiscalFor forEvent = (EvenementFiscalFor) event;
		assertEquals(type, forEvent.getType());
		assertNotNull(forEvent.getForFiscal());
	}

	private static void assertDeclarationEvent(Long tiersId,
	                                           RegDate dateEvenement,
	                                           EvenementFiscalDeclarationSommable.TypeAction type,
	                                           RegDate dateDebutPeriode,
	                                           RegDate dateFinPeriode,
	                                           Class<? extends Declaration> expectedDeclarationClass,
	                                           EvenementFiscal event) {
		assertTiersEvent(tiersId, dateEvenement, EvenementFiscalDeclarationSommable.class, event);

		final EvenementFiscalDeclarationSommable declaEvent = (EvenementFiscalDeclarationSommable) event;
		assertEquals(type, declaEvent.getTypeAction());

		final Declaration declaration = declaEvent.getDeclaration();
		assertInstanceOf(expectedDeclarationClass, declaration);
		assertEquals(dateDebutPeriode, declaration.getDateDebut());
		assertEquals(dateFinPeriode, declaration.getDateFin());
	}

	private static void assertTiersEvent(Long tiersId, RegDate dateEvenement, Class<? extends EvenementFiscal> expectedClass, EvenementFiscal event0) {
		assertNotNull(event0);
		assertTrue(event0 instanceof EvenementFiscalTiers);
		assertEquals(tiersId, ((EvenementFiscalTiers) event0).getTiers().getNumero());
		assertEquals(dateEvenement, event0.getDateValeur());
		assertEquals(expectedClass, event0.getClass());
	}
}
