package ch.vd.uniregctb.database;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.MockDataEventService;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * [UNIREG-2581] Test qui s'assure que toutes les modifications faites sur les tiers provoquent bien l'envoi de notifications de changement.
 */
@SuppressWarnings({"JavaDoc"})
public class DatabaseChangeInterceptorTest extends BusinessTest {

	private MockDataEventService eventService;
	private DatabaseChangeInterceptor interceptor;
	private ModificationInterceptor modificationInterceptor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		eventService = new MockDataEventService();

		interceptor = new DatabaseChangeInterceptor();
		interceptor.setDataEventService(eventService);
		interceptor.setTiersService(getBean(TiersService.class, "tiersService"));

		modificationInterceptor = getBean(ModificationInterceptor.class, "modificationInterceptor");
		modificationInterceptor.register(interceptor);
	}

	@Override
	public void onTearDown() throws Exception {
		modificationInterceptor.unregister(interceptor);
		super.onTearDown();
	}

	@NotTransactional
	@Test
	public void testDetectTiersChange() throws Exception {

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		eventService.clear();

		// on change le nom
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
				pp.setNom("Weiss");
				return null;
			}
		});

		// on vérifie que le changement effectué sur le tiers a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(id, eventService.changedTiers.iterator().next());
	}

	@NotTransactional
	@Test
	public void testDetectDeclarationChange() throws Exception {

		class Ids {
			Long tiers;
			Long di;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2005);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				ids.tiers = pp.getId();
				ids.di = di.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur la déclaration
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
				di.setDateFin(date(2005, 6, 30));
				return null;
			}
		});

		// on vérifie que le changement effectué sur la déclaration a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

	@NotTransactional
	@Test
	public void testDetectForFiscalChange() throws Exception {

		class Ids {
			Long tiers;
			Long ff;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final ForFiscalPrincipal ff = addForPrincipal(pp, date(2002, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				ids.tiers = pp.getId();
				ids.ff = ff.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur le for fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ForFiscalPrincipal ff = (ForFiscalPrincipal) hibernateTemplate.get(ForFiscalPrincipal.class, ids.ff);
				ff.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur le for fiscal a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

	@NotTransactional
	@Test
	public void testDetectAdresseTiersChange() throws Exception {

		class Ids {
			Long tiers;
			Long adresse;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final AdresseSuisse adresse = addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2005, 1, 1), null, MockRue.Chamblon.RueDesUttins);
				ids.tiers = pp.getId();
				ids.adresse = adresse.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur l'adresse
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final AdresseSuisse adresse = (AdresseSuisse) hibernateTemplate.get(AdresseSuisse.class, ids.adresse);
				adresse.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur l'adresse a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

	@NotTransactional
	@Test
	public void testDetectRapportEntreTiersChange() throws Exception {

		class Ids {
			Long pupille;
			Long tuteur;
			Long tutelle;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pupille = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final PersonnePhysique tuteur = addNonHabitant("Roger", "Moore", date(1954, 3, 23), Sexe.MASCULIN);
				final Tutelle tutelle = addTutelle(pupille, tuteur, null, date(2005, 1, 1), null);
				ids.pupille = pupille.getId();
				ids.tuteur = tuteur.getId();
				ids.tutelle = tutelle.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur le rapport-entre-tiers
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Tutelle tutelle = (Tutelle) hibernateTemplate.get(Tutelle.class, ids.tutelle);
				tutelle.setDateDebut(date(2005, 7, 1));
				return null;
			}
		});

		// on vérifie que le changement effectué sur le rapport-entre-tiers a bien provoqué l'envoi d'une notification sur chacun des tiers
		assertEquals(2, eventService.changedTiers.size());
		assertTrue(eventService.changedTiers.contains(ids.pupille));
		assertTrue(eventService.changedTiers.contains(ids.tuteur));
	}

	/**
	 * Vérifie que l'annnulation d'un rapport-entre-tiers provoque bien l'émission d'événements de changements sur les tiers anciennement liés, alors même que le rapport est maintenant annulé.
	 */
	@NotTransactional
	@Test
	public void testDetectAnnulationRapportEntreTiers() throws Exception {

		class Ids {
			Long pupille;
			Long tuteur;
			Long tutelle;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pupille = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final PersonnePhysique tuteur = addNonHabitant("Roger", "Moore", date(1954, 3, 23), Sexe.MASCULIN);
				final Tutelle tutelle = addTutelle(pupille, tuteur, null, date(2005, 1, 1), null);
				ids.pupille = pupille.getId();
				ids.tuteur = tuteur.getId();
				ids.tutelle = tutelle.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur le rapport-entre-tiers
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Tutelle tutelle = (Tutelle) hibernateTemplate.get(Tutelle.class, ids.tutelle);
				tutelle.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur le rapport-entre-tiers a bien provoqué l'envoi d'une notification sur chacun des tiers
		assertEquals(2, eventService.changedTiers.size());
		assertTrue(eventService.changedTiers.contains(ids.pupille));
		assertTrue(eventService.changedTiers.contains(ids.tuteur));
	}

	@NotTransactional
	@Test
	public void testDetectSituationFamilleChange() throws Exception {

		class Ids {
			Long tiers;
			Long situation;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final SituationFamille situation = addSituation(pp, date(2005, 1, 1), null, 2);
				ids.tiers = pp.getId();
				ids.situation = situation.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur la situation de famille
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final SituationFamille situation = (SituationFamille) hibernateTemplate.get(SituationFamille.class, ids.situation);
				situation.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur la situation de famille a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

	@NotTransactional
	@Test
	public void testDetectIdentificationPersonneChange() throws Exception {

		class Ids {
			Long tiers;
			Long ident;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				final IdentificationPersonne ident = addIdentificationPersonne(pp, CategorieIdentifiant.CH_AHV_AVS, "123456789");
				ids.tiers = pp.getId();
				ids.ident = ident.getId();
				return null;
			}
		});

		eventService.clear();

		// on effectue une modification sur la situation de famille
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationPersonne ident = (IdentificationPersonne) hibernateTemplate.get(IdentificationPersonne.class, ids.ident);
				ident.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur la situation de famille a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

	/**
	 * Vérifie que des changements multiples apportés au même tiers ne provoque l'envoi que d'un seul événement
	 */
	@NotTransactional
	@Test
	public void testIgnoreDuplicatedChange() throws Exception {

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Schwarz", date(1954, 3, 23), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		eventService.clear();

		// on change le nom
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
				pp.setNom("Blanco");
				hibernateTemplate.flush(); // <-- déclenche l'interceptor de modification
				pp.setNom("Weiss");
				return null;
			}
		});

		// on vérifie que les changements effectués sur le tiers ont provoqué l'envoi d'une seule notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(id, eventService.changedTiers.iterator().next());
	}
}
