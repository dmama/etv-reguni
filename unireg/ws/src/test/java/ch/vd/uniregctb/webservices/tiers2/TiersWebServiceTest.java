package ch.vd.uniregctb.webservices.tiers2;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.Adresse;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
import ch.vd.uniregctb.webservices.tiers2.data.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.DeclarationImpotOrdinaireKey;
import ch.vd.uniregctb.webservices.tiers2.data.DemandeQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.data.TypeAdresseAutreTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class TiersWebServiceTest extends WebserviceTest {

	private TiersWebService service;
	private UserLogin login;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(TiersWebService.class, "tiersService2Bean");
		login = new UserLogin("iamtestuser", 22);
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	/**
	 * [UNIREG-1985] Vérifie que les fors fiscaux virtuels sont bien retournés <b>même si</b> on demande l'adresse d'envoi en même temps.
	 */
	@Test
	public void testGetBatchTiersHistoForsFiscauxVirtuelsEtAdresseEnvoi() throws Exception {

		class Ids {
			Long paul;
			Long janine;
			Long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple normal, assujetti vaudois ordinaire
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1990, 1, 1);
				final RegDate veilleMariage = dateMariage.getOneDayBefore();

				final PersonnePhysique paul = addNonHabitant("Paul", "Duchemin", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.paul = paul.getNumero();
				addForPrincipal(paul, date(1974, 3, 31), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addAdresseSuisse(paul, TypeAdresseTiers.COURRIER, date(1954, 3, 31), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final PersonnePhysique janine = addNonHabitant("Janine", "Duchemin", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.janine = janine.getNumero();
				addForPrincipal(janine, date(1974, 3, 31), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addAdresseSuisse(janine, TypeAdresseTiers.COURRIER, date(1954, 3, 31), null, MockRue.Lausanne.AvenueDeMarcelin);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(paul, janine, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				return null;
			}
		});

		// Demande de retourner les deux tiers en un seul batch
		final GetBatchTiersHisto params = new GetBatchTiersHisto();
		params.login = login;
		params.tiersNumbers = new HashSet<Long>(Arrays.asList(ids.paul, ids.janine));
		params.parts = new HashSet<TiersPart>(Arrays.asList(TiersPart.FORS_FISCAUX, TiersPart.FORS_FISCAUX_VIRTUELS, TiersPart.ADRESSES_ENVOI));

		final BatchTiersHisto batch = service.getBatchTiersHisto(params);
		assertNotNull(batch);
		assertEquals(2, batch.entries.size());

		Collections.sort(batch.entries, new Comparator<BatchTiersHistoEntry>() {
			public int compare(BatchTiersHistoEntry o1, BatchTiersHistoEntry o2) {
				return o1.number.compareTo(o2.number);
			}
		});

		// On vérifie les fors fiscaux de Paul, il doit y en avoir 2 dont un virtuel
		final BatchTiersHistoEntry entry0 = batch.entries.get(0);
		assertEquals(ids.paul, entry0.number);

		final PersonnePhysiqueHisto paulHisto = (PersonnePhysiqueHisto) entry0.tiers;
		assertNotNull(paulHisto);
		assertEquals(2, paulHisto.forsFiscauxPrincipaux.size());

		final ForFiscal for0 = paulHisto.forsFiscauxPrincipaux.get(0);
		assertNotNull(for0);
		assertEquals(newDate(1974, 3, 31), for0.dateOuverture);
		assertEquals(newDate(1989, 12, 31), for0.dateFermeture);
		assertFalse(for0.virtuel);

		final ForFiscal for1 = paulHisto.forsFiscauxPrincipaux.get(1);
		assertNotNull(for1);
		assertEquals(newDate(1990, 1, 1), for1.dateOuverture);
		assertNull(for1.dateFermeture);
		assertTrue(for1.virtuel); // il s'agit donc du for du ménage reporté sur la personne physique
	}

	/**
	 * [UNIREG-2227] Cas du contribuable n°237.056.03
	 */
	@Test
	public void testGetAdressesTiersAvecTuteur() throws Exception {

		class Ids {
			Long jeanpierre;
			Long marie;
			Long menage;
			Long tuteur;
		}
		final Ids ids = new Ids();

		// Crée un couple dont le mari est sous tutelle
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1976, 1, 1);

				final PersonnePhysique jeanpierre = addNonHabitant("Jean-Pierre", "Bürki", RegDate.get(1947, 1, 11), Sexe.MASCULIN);
				ids.jeanpierre = jeanpierre.getNumero();
				addAdresseSuisse(jeanpierre, TypeAdresseTiers.COURRIER, date(1947, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final PersonnePhysique marie = addNonHabitant("Marie", "Bürki", RegDate.get(1954, 1, 1), Sexe.FEMININ);
				ids.marie = marie.getNumero();
				addAdresseSuisse(marie, TypeAdresseTiers.COURRIER, date(1954, 1, 11), null, MockRue.Lausanne.AvenueDeMarcelin);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jeanpierre, marie, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final PersonnePhysique tuteur = addNonHabitant("Jacky", "Rod", RegDate.get(1947, 1, 1), Sexe.MASCULIN);
				ids.tuteur = tuteur.getNumero();
				addAdresseSuisse(tuteur, TypeAdresseTiers.COURRIER, date(1947, 1, 11), null, MockRue.Lausanne.BoulevardGrancy);

				addTutelle(jeanpierre, tuteur, null, date(2007, 9, 11), null);

				return null;
			}
		});

		{
			final GetTiers params = new GetTiers();
			params.login = login;
			params.tiersNumber = ids.menage;
			params.parts = new HashSet<TiersPart>(Arrays.asList(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI));

			final MenageCommun menage = (MenageCommun) service.getTiers(params);
			assertNotNull(menage);

			assertNotNull(menage.adresseCourrier);
			assertAdresse(new Date(1954, 1, 11), null, "Av de Marcelin", "Lausanne", menage.adresseCourrier); // adresse de madame (puisque monsieur est sous tutelle)

			assertNotNull(menage.adressePoursuite);
// FIXME (msi) : l'adresse poursuite n'est pas calculée de la même manière qu'en 'histo', à cause de la manière différente d'appliquer les défauts.
// 			assertAdresse(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne", menage.adressePoursuite); // adresse de monsieur (non-impacté par la tutelle, car pas d'autorité tutelaire renseignée)

			assertNull(menage.adressePoursuiteAutreTiers); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}

		{
			final GetTiersHisto params = new GetTiersHisto();
			params.login = login;
			params.tiersNumber = ids.menage;
			params.parts = new HashSet<TiersPart>(Arrays.asList(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI));

			final MenageCommunHisto menage = (MenageCommunHisto) service.getTiersHisto(params);
			assertNotNull(menage);

			assertNotNull(menage.adressesCourrier);
			assertEquals(2, menage.adressesCourrier.size());
			assertAdresse(new Date(1947, 1, 1), new Date(2007, 9, 10), "Av de Beaulieu", "Lausanne", menage.adressesCourrier.get(0)); // adresse de monsieur
			assertAdresse(new Date(2007, 9, 11), null, "Av de Marcelin", "Lausanne", menage.adressesCourrier.get(1)); // adresse de madame (puisque monsieur est sous tutelle)

			assertNotNull(menage.adressesPoursuite);
			assertEquals(1, menage.adressesPoursuite.size());
			assertAdresse(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne", menage.adressesPoursuite.get(0)); // adresse de monsieur (non-impacté par la tutelle, car pas d'autorité tutelaire renseignée)

			assertEmpty(menage.adressesPoursuiteAutreTiers); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}
	}

	/**
	 * [UNIREG-2227] Cas du contribuable n°100.864.90 : on s'assure que la source de l'adresse 'poursuite autre tiers' est bien CURATELLE
	 * dans le cas d'une curatelle dont les adresses de début et de fin sont nulles.
	 */
	@Test
	public void testGetAdressesTiersAvecCurateur() throws Exception {

		final long noIndividuTiia = 339619;
		final long noIndividuSylvie = 339618;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu tiia = addIndividu(noIndividuTiia, date(1989, 12, 21), "Tauxe", "Tiia", false);
				addAdresse(tiia, EnumTypeAdresse.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, EnumTypeAdresse.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, EnumTypeAdresse.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, EnumTypeAdresse.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(tiia, EnumTypeAdresse.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);

				MockIndividu sylvie = addIndividu(noIndividuSylvie, date(1955, 9, 19), "Tauxe", "Sylvie", false);
				addAdresse(sylvie, EnumTypeAdresse.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, EnumTypeAdresse.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, EnumTypeAdresse.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, EnumTypeAdresse.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(sylvie, EnumTypeAdresse.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
			}
		});

		class Ids {
			long tiia;
			long sylvie;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique tiia = addHabitant(noIndividuTiia);
				addAdresseSuisse(tiia, TypeAdresseTiers.COURRIER, date(2009, 7, 8), null, MockRue.Lausanne.PlaceSaintFrancois);
				ids.tiia = tiia.getId();
				PersonnePhysique sylvie = addHabitant(noIndividuSylvie);
				ids.sylvie = sylvie.getId();
				addCuratelle(tiia, sylvie, null, null);
				return null;
			}
		});

		{
			final GetTiers params = new GetTiers();
			params.login = login;
			params.tiersNumber = ids.tiia;
			params.parts = new HashSet<TiersPart>(Arrays.asList(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI));

			final ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique tiia = (ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique) service.getTiers(params);
			assertNotNull(tiia);

			assertNotNull(tiia.adresseCourrier);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adresseCourrier);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adresseRepresentation);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adresseDomicile);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressePoursuite);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressePoursuiteAutreTiers);
			assertEquals(TypeAdresseAutreTiers.CURATELLE, tiia.adressePoursuiteAutreTiers.type);

			assertEquals("Madame", tiia.adresseEnvoi.ligne1);
			assertEquals("Tiia Tauxe", tiia.adresseEnvoi.ligne2);
			assertEquals("p.a. Sylvie Tauxe", tiia.adresseEnvoi.ligne3);
			assertEquals("Place Saint-François", tiia.adresseEnvoi.ligne4);
			assertEquals("1000 Lausanne", tiia.adresseEnvoi.ligne5);
			assertNull(tiia.adresseEnvoi.ligne6);

			assertEquals("Madame", tiia.adresseRepresentationFormattee.ligne1);
			assertEquals("Tiia Tauxe", tiia.adresseRepresentationFormattee.ligne2);
			assertEquals("Place Saint-François", tiia.adresseRepresentationFormattee.ligne3);
			assertEquals("1000 Lausanne", tiia.adresseRepresentationFormattee.ligne4);
			assertNull(tiia.adresseRepresentationFormattee.ligne5);
			assertNull(tiia.adresseRepresentationFormattee.ligne6);

			assertEquals("Madame", tiia.adresseDomicileFormattee.ligne1);
			assertEquals("Tiia Tauxe", tiia.adresseDomicileFormattee.ligne2);
			assertEquals("Place Saint-François", tiia.adresseDomicileFormattee.ligne3);
			assertEquals("1000 Lausanne", tiia.adresseDomicileFormattee.ligne4);
			assertNull(tiia.adresseDomicileFormattee.ligne5);
			assertNull(tiia.adresseDomicileFormattee.ligne6);

			assertEquals("Madame", tiia.adressePoursuiteFormattee.ligne1);
			assertEquals("Tiia Tauxe", tiia.adressePoursuiteFormattee.ligne2);
			assertEquals("Place Saint-François", tiia.adressePoursuiteFormattee.ligne3);
			assertEquals("1000 Lausanne", tiia.adressePoursuiteFormattee.ligne4);
			assertNull(tiia.adressePoursuiteFormattee.ligne5);
			assertNull(tiia.adressePoursuiteFormattee.ligne6);

			assertEquals("Madame", tiia.adressePoursuiteAutreTiersFormattee.ligne1);
			assertEquals("Sylvie Tauxe", tiia.adressePoursuiteAutreTiersFormattee.ligne2);
			assertEquals("Place Saint-François", tiia.adressePoursuiteAutreTiersFormattee.ligne3);
			assertEquals("1000 Lausanne", tiia.adressePoursuiteAutreTiersFormattee.ligne4);
			assertNull(tiia.adressePoursuiteAutreTiersFormattee.ligne5);
			assertNull(tiia.adressePoursuiteAutreTiersFormattee.ligne6);
			assertEquals(TypeAdresseAutreTiers.CURATELLE, tiia.adressePoursuiteAutreTiersFormattee.type);
		}

		{
			final GetTiersHisto params = new GetTiersHisto();
			params.login = login;
			params.tiersNumber = ids.tiia;
			params.parts = new HashSet<TiersPart>(Arrays.asList(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI));

			final PersonnePhysiqueHisto tiia = (PersonnePhysiqueHisto) service.getTiersHisto(params);
			assertNotNull(tiia);

			assertNotNull(tiia.adressesCourrier);
			assertEquals(3, tiia.adressesCourrier.size());
			assertAdresse(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.adressesCourrier.get(0));
			assertAdresse(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.adressesCourrier.get(1));
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressesCourrier.get(2));

			assertNotNull(tiia.adressesRepresentation);
			assertEquals(3, tiia.adressesRepresentation.size());
			assertAdresse(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.adressesRepresentation.get(0));
			assertAdresse(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.adressesRepresentation.get(1));
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressesRepresentation.get(2));

			assertNotNull(tiia.adressesDomicile);
			assertEquals(3, tiia.adressesDomicile.size());
			assertAdresse(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.adressesDomicile.get(0));
			assertAdresse(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.adressesDomicile.get(1));
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressesDomicile.get(2));

			assertNotNull(tiia.adressesPoursuite);
			assertEquals(3, tiia.adressesPoursuite.size());
			assertAdresse(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.adressesPoursuite.get(0));
			assertAdresse(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.adressesPoursuite.get(1));
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressesPoursuite.get(2));

			assertNotNull(tiia.adressesPoursuiteAutreTiers);
			assertEquals(3, tiia.adressesPoursuiteAutreTiers.size());
			assertAdresse(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.adressesPoursuiteAutreTiers.get(0));
			assertEquals(TypeAdresseAutreTiers.CURATELLE, tiia.adressesPoursuiteAutreTiers.get(0).type);
			assertAdresse(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.adressesPoursuiteAutreTiers.get(1));
			assertEquals(TypeAdresseAutreTiers.CURATELLE, tiia.adressesPoursuiteAutreTiers.get(1).type);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressesPoursuiteAutreTiers.get(2));
			assertEquals(TypeAdresseAutreTiers.CURATELLE, tiia.adressesPoursuiteAutreTiers.get(2).type);
		}
	}

	/**
	 * [UNIREG-2782] vérification que le quittancement d'une DI pour laquelle l'état "EMISE"
	 * n'a pas été créé fonctionne quand-même
	 */
	@Test
	public void testQuittanceDeclarationSansDateEmission() throws Exception {

		class Ids {
			long ppId;
			long diId;
		}

		// mise en place
		final Ids ids = (Ids) doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Ids execute(TransactionStatus transactionStatus) throws Exception {

				final PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1957, 10, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Croy);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				assertNull(di.getEtatDeclarationActif(TypeEtatDeclaration.EMISE));

				final EtatDeclaration retour = new EtatDeclaration(RegDate.get(), TypeEtatDeclaration.RETOURNEE);
				di.addEtat(retour);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// quittancement de la DI
		final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
		demande.dateRetour = new Date(RegDate.get());
		demande.key = new DeclarationImpotOrdinaireKey();
		demande.key.ctbId = ids.ppId;
		demande.key.numeroSequenceDI = 1;
		demande.key.periodeFiscale = 2009;

		final QuittancerDeclarations quittances = new QuittancerDeclarations();
		quittances.login = login;
		quittances.demandes = Arrays.asList(demande);

		final List<ReponseQuittancementDeclaration> retours = service.quittancerDeclarations(quittances);
		assertNotNull(retours);
		assertEquals(1, retours.size());

		final ReponseQuittancementDeclaration retour = retours.get(0);
		assertNotNull(retour);
		assertEquals(ids.ppId, retour.key.ctbId);
		assertEquals(1, retour.key.numeroSequenceDI);
		assertEquals(2009, retour.key.periodeFiscale);
		assertEquals(CodeQuittancement.OK, retour.code);
	}


	private static void assertAdresse(Date dateDebut, Date dateFin, String rue, String localite, Adresse adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.dateDebut);
		assertEquals(dateFin, adresse.dateFin);
		assertEquals(rue, adresse.rue);
		assertEquals(localite, adresse.localite);
	}

	private Date newDate(int year, int month, int day) {
		return new Date(year, month, day);
	}
}
