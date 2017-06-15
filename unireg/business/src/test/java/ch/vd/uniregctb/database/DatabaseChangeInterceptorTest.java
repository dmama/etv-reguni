package ch.vd.uniregctb.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.MockDataEventService;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.LinkedEntity;
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
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * [UNIREG-2581] Test qui s'assure que toutes les modifications faites sur les tiers provoquent bien l'envoi de notifications de changement.
 */
@SuppressWarnings({"JavaDoc", "Duplicates"})
public class DatabaseChangeInterceptorTest extends BusinessTest {

	private MockDataEventService eventService;
	private DatabaseChangeInterceptor interceptor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		eventService = new MockDataEventService();

		interceptor = new DatabaseChangeInterceptor();
		interceptor.setDataEventService(eventService);
		interceptor.setTiersService(getBean(TiersService.class, "tiersService"));
		interceptor.setParent(getBean(ModificationInterceptor.class, "modificationInterceptor"));
		interceptor.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		interceptor.destroy();
		super.onTearDown();
	}

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
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
				di.setDateFin(date(2005, 6, 30));
				return null;
			}
		});

		// on vérifie que le changement effectué sur la déclaration a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

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
				final ForFiscalPrincipal ff = hibernateTemplate.get(ForFiscalPrincipal.class, ids.ff);
				ff.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur le for fiscal a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

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
				final AdresseSuisse adresse = hibernateTemplate.get(AdresseSuisse.class, ids.adresse);
				adresse.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur l'adresse a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

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
				final Tutelle tutelle = hibernateTemplate.get(Tutelle.class, ids.tutelle);
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
				final Tutelle tutelle = hibernateTemplate.get(Tutelle.class, ids.tutelle);
				tutelle.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur le rapport-entre-tiers a bien provoqué l'envoi d'une notification sur chacun des tiers
		assertEquals(2, eventService.changedTiers.size());
		assertTrue(eventService.changedTiers.contains(ids.pupille));
		assertTrue(eventService.changedTiers.contains(ids.tuteur));
	}

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
				final SituationFamille situation = hibernateTemplate.get(SituationFamille.class, ids.situation);
				situation.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur la situation de famille a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

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
				final IdentificationPersonne ident = hibernateTemplate.get(IdentificationPersonne.class, ids.ident);
				ident.setAnnule(true);
				return null;
			}
		});

		// on vérifie que le changement effectué sur la situation de famille a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedTiers.size());
		assertEquals(ids.tiers, eventService.changedTiers.iterator().next());
	}

	@Test
	public void testDetectImmeubleChange() throws Exception {

		assertEmpty(eventService.changedImmeubles);

		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			return bienFonds.getId();
		});

		// on vérifie que la création de l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(id, eventService.changedImmeubles.iterator().next());

		eventService.clear();

		// on effectue une modification sur l'immeuble
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, id);
			bienFonds.setEgrid("CH99999999");
			return null;
		});

		// on vérifie que le changement sur l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(id, eventService.changedImmeubles.iterator().next());
	}

	/**
	 * [SIFISC-24600] Ce test vérifie que des événements sont bien envoyés pour chacun des immeubles concernés dans le cas d'une changement sur un droit entre immeubles.
	 */
	@Test
	public void testDetectDroitProprieteImmeubleChange() throws Exception {

		assertEmpty(eventService.changedImmeubles);

		class Ids {
			long bienFonds;
			long ppe;
			long beneficiaire;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			final ProprieteParEtageRF ppe = addProprieteParEtageRF("78228218", "CH88828222", new Fraction(1, 2), commune, 544, null, null, null);
			final ImmeubleBeneficiaireRF beneficiaire = addImmeubleBeneficiaireRF(ppe);

			ids.bienFonds = bienFonds.getId();
			ids.ppe = ppe.getId();
			ids.beneficiaire = beneficiaire.getId();
			return null;
		});

		// on vérifie que la création des immeubles a bien provoqué l'envoi de deux notifications
		assertEquals(2, eventService.changedImmeubles.size());
		assertEquals(new HashSet<>(Arrays.asList(ids.bienFonds, ids.ppe)), eventService.changedImmeubles);

		eventService.clear();

		// on ajout un droit de propriété entre les deux immeubles
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, ids.bienFonds);
			final ImmeubleBeneficiaireRF beneficiaire = hibernateTemplate.get(ImmeubleBeneficiaireRF.class, ids.beneficiaire);

			addDroitImmeubleRF(null, RegDate.get(2000, 1, 1), null, null, "Constitution PPE", null, "4834834838", "1818181",
			                   new IdentifiantAffaireRF(61, 2000, 4, 12), new Fraction(1, 4), GenrePropriete.PPE,
			                   beneficiaire, bienFonds);
			return null;
		});

		// on vérifie que l'ajout du droit entre les immeubles a bien provoqué l'envoi d'une notification sur chaque immeuble
		assertEquals(2, eventService.changedImmeubles.size());
		assertEquals(new HashSet<>(Arrays.asList(ids.bienFonds, ids.ppe)), eventService.changedImmeubles);
	}

	/**
	 * [SIFISC-24979] Ce test vérifie que la méthode {@link ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF#getLinkedEntities(LinkedEntity.Context, boolean)}
	 * ne crashe pas lorsqu'on ajoute un lien entre immeubles et que l'immeuble dominant est possédé par une communauté.
	 */
	@Test
	public void testDetectDroitProprieteImmeubleChangeAvecCommunaute() throws Exception {

		assertEmpty(eventService.changedImmeubles);

		class Ids {
			long bienFonds;
			long ppe;
			long beneficiaire;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			// les deux immeubles
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			final ProprieteParEtageRF ppe = addProprieteParEtageRF("78228218", "CH88828222", new Fraction(1, 2), commune, 544, null, null, null);
			final ImmeubleBeneficiaireRF beneficiaire = addImmeubleBeneficiaireRF(ppe);

			// la communauté
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("2872822", "Jean", "Lassale", RegDate.get(1960, 1, 1));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2872823", "Gudule", "Lassale", RegDate.get(1960, 1, 1));
			final CommunauteRF communaute = addCommunauteRF("89393939", TypeCommunaute.COMMUNAUTE_DE_BIENS);

			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(61, 2000, 4, 12);
			addDroitCommunauteRF(null, RegDate.get(2000, 1, 1), null, null, "Achat", null, "9292929", "0103920",
			                     numeroAffaire, new Fraction(3, 4), GenrePropriete.COPROPRIETE, communaute, ppe);
			addDroitPropriete(pp1, ppe, communaute, GenrePropriete.COPROPRIETE, new Fraction(3, 4), null, RegDate.get(2000, 1, 1),
			                  null, null, "Achat", null, numeroAffaire, "3377822", "1");
			addDroitPropriete(pp2, ppe, communaute, GenrePropriete.COPROPRIETE, new Fraction(3, 4), null, RegDate.get(2000, 1, 1),
			                  null, null, "Achat", null, numeroAffaire, "3377823", "1");

			ids.bienFonds = bienFonds.getId();
			ids.ppe = ppe.getId();
			ids.beneficiaire = beneficiaire.getId();
			return null;
		});

		// on vérifie que la création des immeubles a bien provoqué l'envoi de deux notifications
		assertEquals(2, eventService.changedImmeubles.size());
		assertEquals(new HashSet<>(Arrays.asList(ids.bienFonds, ids.ppe)), eventService.changedImmeubles);

		eventService.clear();

		// on ajout un droit de propriété entre les deux immeubles et un droit de propriété d'une communauté
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, ids.bienFonds);
			final ImmeubleBeneficiaireRF beneficiaire = hibernateTemplate.get(ImmeubleBeneficiaireRF.class, ids.beneficiaire);

			// le droit entre immeubles
			addDroitImmeubleRF(null, RegDate.get(2000, 1, 1), null, null,
			                   "Constitution PPE", null, "4834834838", "1818181",
			                   new IdentifiantAffaireRF(61, 2000, 5, 1), new Fraction(1, 4), GenrePropriete.PPE,
			                   beneficiaire, bienFonds);

			return null;
		});

		// on vérifie que l'ajout du droit entre les immeubles a bien provoqué l'envoi d'une notification sur chaque immeuble
		assertEquals(2, eventService.changedImmeubles.size());
		assertEquals(new HashSet<>(Arrays.asList(ids.bienFonds, ids.ppe)), eventService.changedImmeubles);
	}

	/**
	 * [SIFISC-24553] Ce test vérifie que des événements sont bien envoyés pour chacun des propriétaires virtuels concernés dans le cas d'une changement sur un droit entre immeubles.
	 * <p>
	 * <pre>
	 *     +----------+  individuelle0 (1/1)       +-----------+  copropriété4 (20/100)
	 *     | Tiers0   |--------------------------->|   PPE0    |--------------------------+
	 *     +----------+                            +-----------+                          |
	 *                                                                                    v
	 *     +----------+  copropriété1 (10/100)                                     +------------+  fond dominant6  +------------+
	 *     | Tiers1   |----------------------------------------------------------->| BienFonds2 |----------------->| BienFonds3 |
	 *     +----------+                                                            +------------+                  +------------+
	 *                                                                                    ^
	 *     +----------+  copropriété2 (1/3)                                               |
	 *     | Tiers2   |------------------------+                                          |
	 *     +----------+                        |   +-----------+  copropriété5 (70/100)   |
	 *                                         +-->|   PPE1    |--------------------------+
	 *     +----------+  copropriété3 (2/3)    |   +-----------+
	 *     | Tiers3   |------------------------+
	 *     +----------+
	 * </pre>
	 */
	@Test
	public void testDetectDroitProprieteImmeubleChangeProprietairesVirtuels() throws Exception {

		assertEmpty(eventService.changedImmeubles);

		class Ids {
			long pp0;
			long pm1;
			long pm2;
			long pp3;
			long ppe0;
			long ppe1;
			long bienFonds2;
			long bienFonds3;
		}
		final Ids ids = new Ids();

		// création de la situation décrite dans la javadoc de la méthode mais *sans* le droit n°6
		doInNewTransaction(status -> {

			// les personnes physiques
			final PersonnePhysique pp0 = addNonHabitant("Jean0", "Propriétaire", RegDate.get(1970, 1, 1), null);
			final Entreprise pm1 = addEntrepriseInconnueAuCivil("Entreprise1", RegDate.get(1970, 1, 1));
			final Entreprise pm2 = addEntrepriseInconnueAuCivil("Entreprise2", RegDate.get(1970, 1, 1));
			final PersonnePhysique pp3 = addNonHabitant("Jean3", "Propriétaire", RegDate.get(1970, 1, 1), null);
			ids.pp0 = pp0.getId();
			ids.pm1 = pm1.getId();
			ids.pm2 = pm2.getId();
			ids.pp3 = pp3.getId();

			final PersonnePhysiqueRF tiers0 = addPersonnePhysiqueRF("PP0", "Jean0", "Propriétaire", RegDate.get(1970, 1, 1));
			final PersonneMoraleRF tiers1 = addPersonneMoraleRF("Entreprise1", "CHE1", "PM1", 101, null);
			final PersonneMoraleRF tiers2 = addPersonneMoraleRF("Entreprise2", "CHE2", "PM2", 102, null);
			final PersonnePhysiqueRF tiers3 = addPersonnePhysiqueRF("PP3", "Jean3", "Propriétaire", RegDate.get(1970, 1, 1));

			addRapprochementRF(pp0, tiers0, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(pm1, tiers1, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(pm2, tiers2, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(pp3, tiers3, null, null, TypeRapprochementRF.AUTO);

			// les immeubles
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final ProprieteParEtageRF ppe0 = addProprieteParEtageRF("PPE0", "CHPPE0", new Fraction(1, 1), commune, 200, null, null, null);
			final ProprieteParEtageRF ppe1 = addProprieteParEtageRF("PPE1", "CHPPE1", new Fraction(1, 1), commune, 201, null, null, null);
			final BienFondsRF bienFonds2 = addBienFondsRF("BienFonds2", "CHBF2", commune, 30);
			final BienFondsRF bienFonds3 = addBienFondsRF("BienFonds3", "CHBF3", commune, 31);
			final ImmeubleBeneficiaireRF beneficiaire0 = addImmeubleBeneficiaireRF(ppe0);
			final ImmeubleBeneficiaireRF beneficiaire1 = addImmeubleBeneficiaireRF(ppe1);
			ids.ppe0 = ppe0.getId();
			ids.ppe1 = ppe1.getId();
			ids.bienFonds2 = bienFonds2.getId();
			ids.bienFonds3 = bienFonds3.getId();

			// les droits de propriété
			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(61, 2000, 1, 1);
			final RegDate dateDebutMetier = RegDate.get(2000, 1, 1);
			addDroitPersonnePhysiqueRF(null, dateDebutMetier, null, null, "Achat", null, "DROIT0", "1", numeroAffaire,
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, tiers0, ppe0, null);
			addDroitPersonneMoraleRF(null, dateDebutMetier, null, null, "Achat", null, "DROIT1", "1", numeroAffaire,
			                         new Fraction(10, 100), GenrePropriete.COPROPRIETE, tiers1, bienFonds2, null);
			addDroitPersonneMoraleRF(null, dateDebutMetier, null, null, "Achat", null, "DROIT2", "1", numeroAffaire,
			                         new Fraction(1, 3), GenrePropriete.COPROPRIETE, tiers2, ppe1, null);
			addDroitPersonnePhysiqueRF(null, dateDebutMetier, null, null, "Achat", null, "DROIT3", "1", numeroAffaire,
			                           new Fraction(2, 3), GenrePropriete.COPROPRIETE, tiers3, ppe1, null);
			addDroitImmeubleRF(null, dateDebutMetier, null, null, "Achat", null, "DROIT4", "1", numeroAffaire,
			                   new Fraction(20, 100), GenrePropriete.COPROPRIETE, beneficiaire0, bienFonds2);
			addDroitImmeubleRF(null, dateDebutMetier, null, null, "Achat", null, "DROIT5", "1", numeroAffaire,
			                   new Fraction(70, 100), GenrePropriete.COPROPRIETE, beneficiaire1, bienFonds2);
			return null;
		});

		// on vérifie que la création des immeubles et des tiers a bien provoqué l'envoi des notifications suivantes :
		//  - 4 pour les tiers
		//  - 4 pour les immeubles
		assertEquals(new HashSet<>(Arrays.asList(ids.pp0, ids.pm1, ids.pm2, ids.pp3)), eventService.changedTiers);
		assertEquals(new HashSet<>(Arrays.asList(ids.ppe0, ids.ppe1, ids.bienFonds2, ids.bienFonds3)), eventService.changedImmeubles);

		eventService.clear();

		// on ajout le droit de propriété entre les deux bien-fonds
		doInNewTransaction(status -> {
			final BienFondsRF bienFondsDominant = hibernateTemplate.get(BienFondsRF.class, ids.bienFonds2);
			final BienFondsRF bienFondServant = hibernateTemplate.get(BienFondsRF.class, ids.bienFonds3);
			final ImmeubleBeneficiaireRF beneficiaire = addImmeubleBeneficiaireRF(bienFondsDominant);

			addDroitImmeubleRF(null, RegDate.get(2000, 1, 1), null, null, "Achat", null, "DROIT6", "1",
			                   new IdentifiantAffaireRF(61, 2000, 1, 1),
			                   new Fraction(1, 1), GenrePropriete.COPROPRIETE, beneficiaire, bienFondServant);

			return null;
		});

		// on vérifie que l'ajout du droit entre les immeubles a bien provoqué l'envoi des notifications suivantes :
		//  - 4 pour les tiers
		//  - 2 pour les bien-fonds (car les PPEs ne sont pas impactées : les droits virtuels ne sont pas exposés sur les immeubles)
		assertEquals(new HashSet<>(Arrays.asList(ids.pp0, ids.pm1, ids.pm2, ids.pp3)), eventService.changedTiers);
		assertEquals(new HashSet<>(Arrays.asList(ids.bienFonds2, ids.bienFonds3)), eventService.changedImmeubles);
	}

	/**
	 * [SIFISC-24553] Ce test vérifie que l'envoi des événements ne plante pas lorsqu'il y a un cycle dans les droits de propriété entre immeubles.
	 * <p>
	 * <pre>
	 *     +----------+
	 *     | Tiers RF |---------+
	 *     +----------+         | copropriété (1/3)
	 *                          v
	 *                     +------------+
	 *                     | Immeuble 0 |--------------------+
	 *                     +------------+                    | ppe (1/5)
	 *                          ^                            v
	 *                          | fond dominant (4/7)   +------------+
	 *                          +-----------------------| Immeuble 1 |
	 *                                                  +------------+
	 * </pre>
	 */
	@Test(timeout = 10000L)
	public void testDetectDroitProprieteImmeubleAvecCycle() throws Exception {

		assertEmpty(eventService.changedImmeubles);

		class Ids {
			Long ctb;
			Long immeuble0;
			Long immeuble1;
		}
		final Ids ids = new Ids();

		// on crée le tiers et les immeubles (sans les droits entre immeubles)
		doInNewTransaction(status -> {

			final PersonnePhysique ctb = addNonHabitant("Charles-Jean", "Widmer", RegDate.get(1970, 1, 2), null);
			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));
			addRapprochementRF(ctb, tiersRF, null, null, TypeRapprochementRF.AUTO);
			ids.ctb = ctb.getId();

			// un tiers RF avec un immeuble qui fait partie d'un cycle de possession avec un autre immeuble.
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", laSarraz, 4298);
			addImmeubleBeneficiaireRF(immeuble0);
			addImmeubleBeneficiaireRF(immeuble1);

			// tiers RF -> immeuble0
			addDroitPropriete(tiersRF, immeuble0, null, GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "1");

			ids.immeuble0 = immeuble0.getId();
			ids.immeuble1 = immeuble1.getId();
			return null;
		});

		// on vérifie que la création des immeubles et des tiers a bien provoqué l'envoi des notifications suivantes :
		//  - 1 pour les tiers
		//  - 2 pour les immeubles
		assertEquals(new HashSet<>(Collections.singletonList(ids.ctb)), eventService.changedTiers);
		assertEquals(new HashSet<>(Arrays.asList(ids.immeuble0, ids.immeuble1)), eventService.changedImmeubles);

		// on ajoute les droits entre immeubles
		doInNewTransaction(status -> {

			final BienFondsRF immeuble0 = hibernateTemplate.get(BienFondsRF.class, ids.immeuble0);
			final BienFondsRF immeuble1 = hibernateTemplate.get(BienFondsRF.class, ids.immeuble1);

			// immeuble0 -> immeuble 1
			addDroitPropriete(immeuble0, immeuble1, GenrePropriete.PPE, new Fraction(1, 5),
			                  null, RegDate.get(2000, 1, 1), null, "Constitution de PPE", null,
			                  new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");

			// immeuble1 -> immeuble 0
			addDroitPropriete(immeuble1, immeuble0, GenrePropriete.FONDS_DOMINANT, new Fraction(4, 7),
			                  null, RegDate.get(2000, 1, 1), null, "Constitution de PPE", null,
			                  new IdentifiantAffaireRF(123, 2000, 6, 1), "6680384444", "1");
			return null;
		});

		// on vérifie que l'ajout des droits entre les immeubles a bien provoqué l'envoi des notifications suivantes :
		//  - 1 pour les tiers
		//  - 2 pour les immeubles
		assertEquals(new HashSet<>(Collections.singletonList(ids.ctb)), eventService.changedTiers);
		assertEquals(new HashSet<>(Arrays.asList(ids.immeuble0, ids.immeuble1)), eventService.changedImmeubles);
	}

	@Test
	public void testDetectSituationChange() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			return bienFonds.getId();
		});

		eventService.clear();
		assertEmpty(eventService.changedImmeubles);

		// on modifie la situation de l'immeuble
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, id);
			final SituationRF situation0 = bienFonds.getSituations().iterator().next();
			situation0.setDateFin(RegDate.get(2004, 12, 31));
			final SituationRF situation1 = new SituationRF();
			situation1.setNoParcelle(1022);
			situation1.setDateDebut(RegDate.get(2005, 1, 1));
			situation1.setCommune(situation0.getCommune());
			bienFonds.addSituation(situation1);
			return null;
		});

		// on vérifie que la modification de la situaiton de l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(id, eventService.changedImmeubles.iterator().next());
	}

	@Test
	public void testDetectSurfaceTotaleChange() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			return bienFonds.getId();
		});

		eventService.clear();
		assertEmpty(eventService.changedImmeubles);

		// on ajoute une surface totale à l'immeuble
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, id);
			final SurfaceTotaleRF sf = new SurfaceTotaleRF();
			sf.setDateDebut(RegDate.get(2005, 1, 1));
			sf.setSurface(200);
			bienFonds.addSurfaceTotale(sf);
			return null;
		});

		// on vérifie que l'ajout de la surface totale sur l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(id, eventService.changedImmeubles.iterator().next());
	}

	@Test
	public void testDetectSurfaceAuSolChange() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			return bienFonds.getId();
		});

		eventService.clear();
		assertEmpty(eventService.changedImmeubles);

		// on ajoute une surface au sol à l'immeuble
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, id);
			final SurfaceAuSolRF s = new SurfaceAuSolRF();
			s.setDateDebut(RegDate.get(2005, 1, 1));
			s.setSurface(200);
			s.setType("Porcherie");
			s.setImmeuble(bienFonds);
			hibernateTemplate.merge(s);
			return null;
		});

		// on vérifie que l'ajout de la surface au sol sur l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(id, eventService.changedImmeubles.iterator().next());
	}

	@Test
	public void testDetectEstimationChange() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			return bienFonds.getId();
		});

		eventService.clear();
		assertEmpty(eventService.changedImmeubles);

		// on ajoute une estimation à l'immeuble
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, id);
			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(2005, 1, 1));
			estimation.setDateInscription(RegDate.get(2005, 1, 1));
			estimation.setEnRevision(false);
			estimation.setMontant(2_003_030L);
			estimation.setReference("voir le chef");
			bienFonds.addEstimation(estimation);
			return null;
		});

		// on vérifie que l'ajout de l'estimation sur l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(id, eventService.changedImmeubles.iterator().next());
	}

	@Test
	public void testDetectImplantationChange() throws Exception {

		class Ids {
			long immeuble;
			long batiment;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38828288a", "CH38278228", commune, 234);
			final BatimentRF batiment = addBatimentRF("8388338");
			ids.immeuble = bienFonds.getId();
			ids.batiment = batiment.getId();
			return null;
		});

		eventService.clear();
		assertEmpty(eventService.changedImmeubles);
		assertEmpty(eventService.changedBatiments);

		// on ajoute une implantation entre un bâtiment et un immeuble
		doInNewTransaction(status -> {
			final BienFondsRF bienFonds = hibernateTemplate.get(BienFondsRF.class, ids.immeuble);
			final BatimentRF batiment = hibernateTemplate.get(BatimentRF.class, ids.batiment);
			final ImplantationRF implantation = new ImplantationRF();
			implantation.setDateDebut(RegDate.get(2005, 1, 1));
			implantation.setSurface(2_300);
			implantation.setBatiment(batiment);
			implantation.setImmeuble(bienFonds);
			batiment.addImplantation(implantation);
			return null;
		});

		// on vérifie que l'ajout de l'implantation entre le bâtiment et l'immeuble a bien provoqué l'envoi d'une notification sur chacune des entités
		assertEquals(1, eventService.changedImmeubles.size());
		assertEquals(Long.valueOf(ids.immeuble), eventService.changedImmeubles.iterator().next());
		assertEquals(1, eventService.changedBatiments.size());
		assertEquals(Long.valueOf(ids.batiment), eventService.changedBatiments.iterator().next());
	}

	@Test
	public void testDetectBatimentChange() throws Exception {

		assertEmpty(eventService.changedBatiments);

		final Long id = doInNewTransaction(status -> {
			final BatimentRF batiment = addBatimentRF("8388338");
			return batiment.getId();
		});

		// on vérifie que la création de l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedBatiments.size());
		assertEquals(id, eventService.changedBatiments.iterator().next());

		eventService.clear();

		// on effectue une modification sur le bâtiment
		doInNewTransaction(status -> {
			final BatimentRF batiment = hibernateTemplate.get(BatimentRF.class, id);
			batiment.setMasterIdRF("38383838");
			return null;
		});

		// on vérifie que le changement sur l'immeuble a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedBatiments.size());
		assertEquals(id, eventService.changedBatiments.iterator().next());
	}

	@Test
	public void testDetectDescriptionBatimentChange() throws Exception {

		assertEmpty(eventService.changedBatiments);

		final Long id = doInNewTransaction(status -> {
			final BatimentRF batiment = addBatimentRF("8388338");
			return batiment.getId();
		});

		eventService.clear();
		assertEmpty(eventService.changedBatiments);

		// on ajoute une description sur le bâtiment
		doInNewTransaction(status -> {
			final BatimentRF batiment = hibernateTemplate.get(BatimentRF.class, id);
			final DescriptionBatimentRF description = new DescriptionBatimentRF();
			description.setDateDebut(RegDate.get(2000, 1, 1));
			description.setType("Centrale électrique sur le champ de betteraves");
			description.setSurface(23);
			batiment.addDescription(description);
			return null;
		});

		// on vérifie que l'ajout de la description a bien provoqué l'envoi d'une notification
		assertEquals(1, eventService.changedBatiments.size());
		assertEquals(id, eventService.changedBatiments.iterator().next());
	}
}
