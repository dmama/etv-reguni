package ch.vd.uniregctb.webservices.tiers2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdressePM;
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
import ch.vd.uniregctb.webservices.tiers2.data.Debiteur;
import ch.vd.uniregctb.webservices.tiers2.data.DeclarationImpotOrdinaireKey;
import ch.vd.uniregctb.webservices.tiers2.data.DemandeQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.data.TypeAdresseAutreTiers;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
		doInNewTransaction(new TxCallback<Object>() {
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
		params.tiersNumbers = new HashSet<>(Arrays.asList(ids.paul, ids.janine));
		params.parts = EnumSet.of(TiersPart.FORS_FISCAUX, TiersPart.FORS_FISCAUX_VIRTUELS, TiersPart.ADRESSES_ENVOI);

		final BatchTiersHisto batch = service.getBatchTiersHisto(params);
		assertNotNull(batch);
		assertEquals(2, batch.entries.size());

		Collections.sort(batch.entries, new Comparator<BatchTiersHistoEntry>() {
			@Override
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
		doInNewTransaction(new TxCallback<Object>() {
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
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

			final MenageCommun menage = (MenageCommun) service.getTiers(params);
			assertNotNull(menage);

			assertNotNull(menage.adresseCourrier);
			assertAdresse(new Date(2007, 9, 11), null, "Av de Marcelin", "Lausanne", menage.adresseCourrier); // adresse de madame (puisque monsieur est sous tutelle)

			assertNotNull(menage.adressePoursuite);
 			assertAdresse(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne", menage.adressePoursuite); // adresse de monsieur (non-impacté par la tutelle, car pas d'autorité tutelaire renseignée)

			assertNull(menage.adressePoursuiteAutreTiers); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}

		{
			final GetTiersHisto params = new GetTiersHisto();
			params.login = login;
			params.tiersNumber = ids.menage;
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

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
	 * [SIFISC-1868], cas des débiteurs IS 12.941.04 et 12.926.19<br/>
	 * Le débiteur a une surcharge annulée sur son adresse de domicile (ces adresses fiscales de domicile sont maintenant interdites...)
	 * -> l'appel à la méthode GetTiers ne renvoie pas d'adresse de domicile correcte (qui aurait dû être recopiée de la seule adresse connue : l'adresse courrier)
	 */
	@Test
	public void testGetAdresseDomicileSiSurchargeAnnuleeExiste() throws Exception {

		final long idDpi = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				dpi.setNom1("Ma petite entreprise");
				addAdresseSuisse(dpi, TypeAdresseTiers.COURRIER, date(2009, 1, 1), null, MockRue.Aubonne.RueTrevelin);
				final AdresseSuisse dom = addAdresseSuisse(dpi, TypeAdresseTiers.DOMICILE, date(2009, 1, 1), null, MockRue.Aubonne.CheminCurzilles);
				dom.setAnnule(true);

				return dpi.getNumero();
			}
		});

		// appel à GetTiers
		{
			final GetTiers params = new GetTiers();
			params.login = login;
			params.tiersNumber = idDpi;
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

			final Debiteur dpi = (Debiteur) service.getTiers(params);
			assertNotNull(dpi);

			assertNotNull(dpi.adresseCourrier);
			assertAdresse(new Date(2009, 1, 1), null, "Rue de Trévelin", "Aubonne", dpi.adresseCourrier);

			assertNotNull(dpi.adresseDomicile);
			assertAdresse(new Date(2009, 1, 1), null, "Rue de Trévelin", "Aubonne", dpi.adresseDomicile);

			assertNotNull(dpi.adressePoursuite);
			assertAdresse(new Date(2009, 1, 1), null, "Rue de Trévelin", "Aubonne", dpi.adressePoursuite);

			assertNull(dpi.adressePoursuiteAutreTiers);
		}
	}

	/**
	 * [UNIREG-3203] Cas du contribuable n°497.050.02
	 */
	@Test
	public void testGetAdressesTiersAvecConseilLegal() throws Exception {

		class Ids {
			Long jeandaniel;
			Long myriam;
			Long menage;
			Long conseiller;
		}
		final Ids ids = new Ids();

		// Crée un couple dont le mari est sous tutelle
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1976, 1, 1);

				final PersonnePhysique jeandaniel = addNonHabitant("Jean-Daniel", "Guex-Martin", RegDate.get(1947, 1, 11), Sexe.MASCULIN);
				ids.jeandaniel = jeandaniel.getNumero();
				addAdresseSuisse(jeandaniel, TypeAdresseTiers.COURRIER, date(1947, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);

				final PersonnePhysique myriam = addNonHabitant("Myriam", "Guex-Martin", RegDate.get(1954, 1, 1), Sexe.FEMININ);
				ids.myriam = myriam.getNumero();
				addAdresseSuisse(myriam, TypeAdresseTiers.COURRIER, date(1954, 1, 11), null, MockRue.Lausanne.AvenueDeMarcelin);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jeandaniel, myriam, dateMariage, null);
				ids.menage = ensemble.getMenage().getNumero();
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final PersonnePhysique conseiller = addNonHabitant("Philippe", "Rossy", RegDate.get(1947, 1, 1), Sexe.MASCULIN);
				ids.conseiller = conseiller.getNumero();
				addAdresseSuisse(conseiller, TypeAdresseTiers.COURRIER, date(1947, 1, 11), null, MockRue.Lausanne.BoulevardGrancy);

				addConseilLegal(jeandaniel, conseiller, date(2007, 9, 11), null);

				return null;
			}
		});

		{
			final GetTiers params = new GetTiers();
			params.login = login;
			params.tiersNumber = ids.menage;
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

			final MenageCommun menage = (MenageCommun) service.getTiers(params);
			assertNotNull(menage);

			assertNotNull(menage.adresseCourrier);
			assertAdresse(new Date(2007, 9, 11), null, "Av de Marcelin", "Lausanne", menage.adresseCourrier); // adresse de madame (puisque monsieur est sous conseil légal)

			assertNotNull(menage.adressePoursuite);
 			assertAdresse(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne", menage.adressePoursuite); // adresse de monsieur (non-impacté par le conseil légal)

			assertNull(menage.adressePoursuiteAutreTiers); // [UNIREG-2227] pas d'adresse autre tiers car madame remplace monsieur dans la gestion du ménage
		}

		{
			final GetTiersHisto params = new GetTiersHisto();
			params.login = login;
			params.tiersNumber = ids.menage;
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

			final MenageCommunHisto menage = (MenageCommunHisto) service.getTiersHisto(params);
			assertNotNull(menage);

			assertNotNull(menage.adressesCourrier);
			assertEquals(2, menage.adressesCourrier.size());
			assertAdresse(new Date(1947, 1, 1), new Date(2007, 9, 10), "Av de Beaulieu", "Lausanne", menage.adressesCourrier.get(0)); // adresse de monsieur
			assertAdresse(new Date(2007, 9, 11), null, "Av de Marcelin", "Lausanne", menage.adressesCourrier.get(1)); // adresse de madame (puisque monsieur est sous conseil légal)

			assertNotNull(menage.adressesPoursuite);
			assertEquals(1, menage.adressesPoursuite.size());
			assertAdresse(new Date(1947, 1, 1), null, "Av de Beaulieu", "Lausanne", menage.adressesPoursuite.get(0)); // adresse de monsieur (non-impacté par le conseil légal)

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
				addAdresse(tiia, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, TypeAdresseCivil.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(tiia, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(tiia, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(tiia, TypeAdresseCivil.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);

				MockIndividu sylvie = addIndividu(noIndividuSylvie, date(1955, 9, 19), "Tauxe", "Sylvie", false);
				addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Moudon.LeBourg, null, null, date(2006, 9, 24));
				addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Pully.CheminDesRoches, null, date(2006, 9, 25), date(2009, 1, 31));
				addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
				addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, null, date(2009, 2, 1), null);
			}
		});

		class Ids {
			long tiia;
			long sylvie;
		}
		final Ids ids = new Ids();

		// pas de validation : pour permettre l'ajout d'une curatelle avec date de début nulle
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
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
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

			final ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique tiia = (ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique) service.getTiers(params);
			assertNotNull(tiia);

			assertNotNull(tiia.adresseCourrier);
			assertAdresse(new Date(2009, 7, 8), null, "Place Saint-François", "Lausanne", tiia.adresseCourrier); // [UNIREG-3025] les adresses surchargées priment sur toutes les autres adresses
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adresseRepresentation);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adresseDomicile);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressePoursuite);
			assertAdresse(new Date(2009, 2, 1), null, "Place Saint-François", "Lausanne", tiia.adressePoursuiteAutreTiers);
			assertEquals(TypeAdresseAutreTiers.CURATELLE, tiia.adressePoursuiteAutreTiers.type);

			assertEquals("Madame", tiia.adresseEnvoi.ligne1);
			assertEquals("Tiia Tauxe", tiia.adresseEnvoi.ligne2);
			assertEquals("Place Saint-François", tiia.adresseEnvoi.ligne3);
			assertEquals("1000 Lausanne", tiia.adresseEnvoi.ligne4);
			assertNull(tiia.adresseEnvoi.ligne5);
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
			params.parts = EnumSet.of(TiersPart.ADRESSES, TiersPart.ADRESSES_ENVOI);

			final PersonnePhysiqueHisto tiia = (PersonnePhysiqueHisto) service.getTiersHisto(params);
			assertNotNull(tiia);

			assertNotNull(tiia.adressesCourrier);
			assertEquals(4, tiia.adressesCourrier.size());
			assertAdresse(null, new Date(2006, 9, 24), "Rue du Bourg", "Moudon", tiia.adressesCourrier.get(0));
			assertAdresse(new Date(2006, 9, 25), new Date(2009, 1, 31), "Chemin des Roches", "Pully", tiia.adressesCourrier.get(1));
			assertAdresse(new Date(2009, 2, 1), new Date(2009, 7, 7), "Place Saint-François", "Lausanne", tiia.adressesCourrier.get(2));
			assertAdresse(new Date(2009, 7, 8), null, "Place Saint-François", "Lausanne", tiia.adressesCourrier.get(3));

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
		final Ids ids = doInNewTransactionAndSession(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus transactionStatus) throws Exception {

				final PersonnePhysique pp = addNonHabitant("Albus", "Dumbledore", date(1957, 10, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Croy);
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				assertNull(di.getDernierEtatOfType(TypeEtatDeclaration.EMISE));

				final EtatDeclaration retour = new EtatDeclarationRetournee(RegDate.get(), "TEST");
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

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertNotNull(di.getDernierEtat());
				assertEquals(TypeEtatDeclaration.RETOURNEE, di.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	// [UNIREG-3179] si un tiers ne valide pas au milieu du lot, tout explose
	@Test
	public void testQuittanceDeclarationSurContribuableQuiNeValidePas() throws Exception {

		class Ids {
			long ppId;
			long diId;
		}
		final Ids ids = new Ids();

		final int annee = 2010;

		// on désactive la validation pour pouvoir sauver un tiers au moins qui ne valide pas...
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final RegDate debutAnnee = date(annee, 1, 1);
				final RegDate finAnnee = date(annee, 12, 31);
				final RegDate venteImmeuble = finAnnee.addMonths(-3);

				final PersonnePhysique pp = addNonHabitant(null, "Anonyme", date(1980, 10, 25), Sexe.MASCULIN);
				pp.setNom(null);        // <-- c'est là le problème de validation

				addForPrincipal(pp, debutAnnee, MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
				addForSecondaire(pp, debutAnnee, MotifFor.ACHAT_IMMOBILIER, venteImmeuble, MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, debutAnnee, venteImmeuble, TypeContribuable.HORS_SUISSE, md);
				final RegDate dateEmission = date(annee + 1, 1, 11);
				di.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateTraitement(dateEmission);
				delai.setDelaiAccordeAu(date(annee + 1, 6, 30));
				di.addDelai(delai);

				ids.ppId = pp.getNumero();
				ids.diId = di.getId();

				return null;
			}
		});

		// quittancement de la DI
		final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
		demande.dateRetour = new Date(RegDate.get());
		demande.key = new DeclarationImpotOrdinaireKey();
		demande.key.ctbId = ids.ppId;
		demande.key.numeroSequenceDI = 1;
		demande.key.periodeFiscale = annee;

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
		assertEquals(annee, retour.key.periodeFiscale);
		assertEquals(CodeQuittancement.EXCEPTION, retour.code);
		assertEquals(WebServiceExceptionType.BUSINESS, retour.exceptionType);

		final String expectedMessage = String.format("PersonnePhysique #%d - 1 erreur(s) - 0 avertissement(s):\n [E] Le nom est un attribut obligatoire pour un non-habitant\n", ids.ppId);
		assertEquals(expectedMessage, retour.exceptionMessage);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertNotNull(di.getDernierEtat());
				assertEquals(TypeEtatDeclaration.EMISE, di.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	@Test
	public void testQuittanceDeclarationsGroupeesAvecUnContribuableQuiNeValidePas() throws Exception {

		final int annee = 2010;
		final RegDate debutAnnee = date(annee, 1, 1);
		final RegDate finAnnee = date(annee, 12, 31);
		final RegDate venteImmeuble = finAnnee.addMonths(-3);

		class Ids {
			final long idCtb;
			final long idDi;

			Ids(long idCtb, long idDi) {
				this.idCtb = idCtb;
				this.idDi = idDi;
			}
		}
		final List<Ids> liste = new ArrayList<>();

		// on désactive la validation pour pouvoir sauver un tiers au moins qui ne valide pas...
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique validePP = addPersonnePhysiqueAvecFor("Alfred", "de Montauban", date(1980, 10, 25), Sexe.MASCULIN);
				final PersonnePhysique invalidePP = addPersonnePhysiqueAvecFor(null, null, date(1986, 12, 5), Sexe.FEMININ);

				final DeclarationImpotOrdinaire valideDi = addDi(validePP, debutAnnee, venteImmeuble, pf, md);
				final DeclarationImpotOrdinaire invalideDi = addDi(invalidePP, debutAnnee, venteImmeuble, pf, md);

				liste.add(new Ids(validePP.getNumero(), valideDi.getId()));
				liste.add(new Ids(invalidePP.getNumero(), invalideDi.getId()));
				return null;
			}

			private PersonnePhysique addPersonnePhysiqueAvecFor(@Nullable String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
				final PersonnePhysique pp = addNonHabitant(prenom, nom, dateNaissance, sexe);
				addForPrincipal(pp, debutAnnee, MotifFor.ACHAT_IMMOBILIER, MockPays.Allemagne);
				addForSecondaire(pp, debutAnnee, MotifFor.ACHAT_IMMOBILIER, venteImmeuble, MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp;
			}

			private DeclarationImpotOrdinaire addDi(Contribuable ctb, RegDate dateDebut, RegDate dateFin, PeriodeFiscale pf, ModeleDocument md) {
				final DeclarationImpotOrdinaire di = addDeclarationImpot(ctb, pf, dateDebut, dateFin, TypeContribuable.HORS_SUISSE, md);
				final RegDate dateEmission = date(annee + 1, 1, 11);
				di.addEtat(new EtatDeclarationEmise(dateEmission));
				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateTraitement(dateEmission);
				delai.setDelaiAccordeAu(date(annee + 1, 6, 30));
				di.addDelai(delai);
				return di;
			}
		});

		// quittancement des DI
		final List<DemandeQuittancementDeclaration> demandes = new ArrayList<>();
		for (Ids ids : liste) {
			final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
			demande.dateRetour = new Date(RegDate.get());
			demande.key = new DeclarationImpotOrdinaireKey();
			demande.key.ctbId = ids.idCtb;
			demande.key.numeroSequenceDI = 1;
			demande.key.periodeFiscale = annee;
			demandes.add(demande);
		}

		final QuittancerDeclarations quittances = new QuittancerDeclarations();
		quittances.login = login;
		quittances.demandes = demandes;

		final List<ReponseQuittancementDeclaration> retours = service.quittancerDeclarations(quittances);
		assertNotNull(retours);
		assertEquals(2, retours.size());

		final ReponseQuittancementDeclaration retourValide = retours.get(0);
		assertNotNull(retourValide);
		assertEquals(liste.get(0).idCtb, retourValide.key.ctbId);
		assertEquals(1, retourValide.key.numeroSequenceDI);
		assertEquals(annee, retourValide.key.periodeFiscale);
		assertEquals(CodeQuittancement.OK, retourValide.code);

		final ReponseQuittancementDeclaration retourInvalide = retours.get(1);
		assertNotNull(retourInvalide);
		assertEquals(liste.get(1).idCtb, retourInvalide.key.ctbId);
		assertEquals(1, retourInvalide.key.numeroSequenceDI);
		assertEquals(annee, retourInvalide.key.periodeFiscale);
		assertEquals(CodeQuittancement.EXCEPTION, retourInvalide.code);
		assertEquals(WebServiceExceptionType.BUSINESS, retourInvalide.exceptionType);

		final String expectedMessage = String.format("PersonnePhysique #%d - 1 erreur(s) - 0 avertissement(s):\n [E] Le nom est un attribut obligatoire pour un non-habitant\n", liste.get(1).idCtb);
		assertEquals(expectedMessage, retourInvalide.exceptionMessage);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire diValide = hibernateTemplate.get(DeclarationImpotOrdinaire.class, liste.get(0).idDi);
				assertNotNull(diValide);
				assertNotNull(diValide.getDernierEtat());
				assertEquals(TypeEtatDeclaration.RETOURNEE, diValide.getDernierEtat().getEtat());

				final DeclarationImpotOrdinaire diInvalide = hibernateTemplate.get(DeclarationImpotOrdinaire.class, liste.get(1).idDi);
				assertNotNull(diInvalide);
				assertNotNull(diInvalide.getDernierEtat());
				assertEquals(TypeEtatDeclaration.EMISE, diInvalide.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-2950] Vérifie que les rapports-entre-tiers d'appartenance ménage annulés ne sont pas pris en compte lors du calcul des fors fiscaux virtuels
	 */
	@Test
	public void testGetForFiscauxVirtuelsPersonnePhysiqueAvecAppartenanceMenageAnnulee() throws Exception {

		final Long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final RegDate veilleMariage = date(1995, 7, 30);
				final RegDate dateMariage = date(1995, 8, 1);

				final PersonnePhysique arnold = addNonHabitant("Arnold", "Lokker", date(1971, 3, 12), Sexe.MASCULIN);
				addForPrincipal(arnold, date(1991, 3, 12), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				final PersonnePhysique lucette = addNonHabitant("Lucette", "Tartare", date(1973, 5, 12), Sexe.FEMININ);
				addForPrincipal(lucette, date(1993, 5, 12), MotifFor.MAJORITE, veilleMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(arnold, lucette, dateMariage, null);
				final ch.vd.uniregctb.tiers.MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				// on clone les liens d'appartenance ménage et on les annule (de cette manière, l'ensemble reste valide mais avec des rapports annulés)
				for (RapportEntreTiers r : menage.getRapportsObjet()) {
					RapportEntreTiers clone = r.duplicate();
					clone.setAnnule(true);
					hibernateTemplate.merge(clone);
				}

				return arnold.getNumero();
			}
		});

		// on vérifie que les données en base sont bien comme on pense
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique arnold = (PersonnePhysique) hibernateTemplate.get(Tiers.class, id);
				assertNotNull(arnold);
				final Set<RapportEntreTiers> rapports = arnold.getRapportsSujet();
				assertNotNull(rapports);
				assertEquals(2, rapports.size()); // 2 rapports d'appartenance ménage identiques, mais un est annulé
				return null;
			}
		});

		{
			final GetTiers params = new GetTiers();
			params.login = login;
			params.tiersNumber = id;
			params.parts = EnumSet.of(TiersPart.FORS_FISCAUX_VIRTUELS);

			// on s'assure que l'appartenance ménage annulé n'est pas pris en compte (s'il l'était, on recevrait une exception avec le message "Détecté 2 fors fiscaux principaux valides à la même date")
			final ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique arnold = (ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique) service.getTiers(params);
			assertNotNull(arnold);
			assertNotNull(arnold.forFiscalPrincipal);
			assertEquals(newDate(1995, 8, 1), arnold.forFiscalPrincipal.dateOuverture);
			assertTrue(arnold.forFiscalPrincipal.virtuel);
			assertNull(arnold.forFiscalPrincipal.dateFermeture);
			assertEquals(ForFiscal.GenreImpot.REVENU_FORTUNE, arnold.forFiscalPrincipal.genreImpot);
			assertEquals(ForFiscal.ModeImposition.ORDINAIRE, arnold.forFiscalPrincipal.modeImposition);
			assertEquals(ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, arnold.forFiscalPrincipal.typeAutoriteFiscale);
			assertEquals(MockCommune.Bussigny.getNoOFS(), arnold.forFiscalPrincipal.noOfsAutoriteFiscale);
		}
	}

	private static void assertAdresse(@Nullable Date dateDebut, @Nullable Date dateFin, String rue, String localite, Adresse adresse) {
		assertAdresse(dateDebut, dateFin, rue, null, localite, adresse);
	}

	private static void assertAdresse(@Nullable Date dateDebut, @Nullable Date dateFin, @Nullable String rue, @Nullable String numeroMaison, String localite,
	                                  Adresse adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.dateDebut);
		assertEquals(dateFin, adresse.dateFin);
		assertEquals(rue, adresse.rue);
		assertEquals(numeroMaison, adresse.numeroRue);
		assertEquals(localite, adresse.localite);
	}

	private Date newDate(int year, int month, int day) {
		return new Date(year, month, day);
	}

	@Test
	public void testQuittancementAvecPlusieursDiQuiPartagentLeMemeNumeroDeSequence() throws Exception {

		final class Ids {
			long ppId;
			long diAnnuleeId;
			long diNonAnnuleeId;
			int noSequence;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				diAnnulee.setAnnule(true);

				final DeclarationImpotOrdinaire diNonAnnulee = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				diNonAnnulee.setNumero(diAnnulee.getNumero());

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diAnnuleeId = diAnnulee.getId();
				ids.diNonAnnuleeId = diNonAnnulee.getId();
				ids.noSequence = diAnnulee.getNumero();
				return ids;
			}
		});

		final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
		demande.dateRetour = new Date(RegDate.get());
		demande.key = new DeclarationImpotOrdinaireKey();
		demande.key.ctbId = ids.ppId;
		demande.key.numeroSequenceDI = ids.noSequence;
		demande.key.periodeFiscale = annee;

		final QuittancerDeclarations params = new QuittancerDeclarations();
		params.login = login;
		params.demandes = Arrays.asList(demande);

		final List<ReponseQuittancementDeclaration> result = service.quittancerDeclarations(params);
		assertNotNull(result);
		assertEquals(1, result.size());

		final ReponseQuittancementDeclaration reponse = result.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.OK, reponse.code);
	}

	@Test
	public void testQuittancementAvecSourceRenseignee() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// On quittance la DI en précisant la source
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
				demande.dateRetour = new Date(RegDate.get());
				demande.source = "TEST_QUITTANCEMENT";
				demande.key = new DeclarationImpotOrdinaireKey();
				demande.key.ctbId = ids.ppId;
				demande.key.numeroSequenceDI = 1;
				demande.key.periodeFiscale = annee;

				final QuittancerDeclarations params = new QuittancerDeclarations();
				params.login = login;
				params.demandes = Arrays.asList(demande);

				final List<ReponseQuittancementDeclaration> result = service.quittancerDeclarations(params);
				assertNotNull(result);
				assertEquals(1, result.size());

				final ReponseQuittancementDeclaration reponse = result.get(0);
				assertNotNull(reponse);
				assertEquals(CodeQuittancement.OK, reponse.code);
				return null;
			}
		});

		// On vérifie que la DI a été quittancée par le CEDI (= valeur par défaut)
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertEquals(RegDate.get(), di.getDateRetour());

				final EtatDeclaration etat = di.getDernierEtat();
				assertNotNull(etat);
				assertInstanceOf(EtatDeclarationRetournee.class, etat);

				final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
				assertEquals(RegDate.get(), retour.getDateObtention());
				assertEquals("TEST_QUITTANCEMENT", retour.getSource());
				return null;
			}
		});
	}

	@Test
	public void testQuittancementSansSourceRenseignee() throws Exception {

		final class Ids {
			long ppId;
			long diId;
		}

		final int annee = 2009;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.diId = di.getId();
				return ids;
			}
		});

		// On quittance la DI sans préciser la source
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
				demande.dateRetour = new Date(RegDate.get());
				demande.source = null;
				demande.key = new DeclarationImpotOrdinaireKey();
				demande.key.ctbId = ids.ppId;
				demande.key.numeroSequenceDI = 1;
				demande.key.periodeFiscale = annee;

				final QuittancerDeclarations params = new QuittancerDeclarations();
				params.login = login;
				params.demandes = Arrays.asList(demande);

				final List<ReponseQuittancementDeclaration> result = service.quittancerDeclarations(params);
				assertNotNull(result);
				assertEquals(1, result.size());

				final ReponseQuittancementDeclaration reponse = result.get(0);
				assertNotNull(reponse);
				assertEquals(CodeQuittancement.OK, reponse.code);
				return ids;
			}
		});

		// On vérifie que la DI a été quittancée par le CEDI (= valeur par défaut)
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.diId);
				assertNotNull(di);
				assertEquals(RegDate.get(), di.getDateRetour());

				final EtatDeclaration etat = di.getDernierEtat();
				assertNotNull(etat);
				assertInstanceOf(EtatDeclarationRetournee.class, etat);

				final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
				assertEquals(RegDate.get(), retour.getDateObtention());
				assertEquals("CEDI", retour.getSource());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-4023] Vérifie que la date d'arrivée dans le canton de Vaud est bien renseignée sur les personne physiques connue au contrôle des habitants
	 */
	@Test
	public void testDateArriveeVD() throws Exception {

		final int NO_IND = 123456;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(NO_IND, date(1953, 2, 1), "Grogniard", "Ralf", true);
				ind.setDateArriveeVD(date(1967, 12, 31));
			}
		});

		// création d'un habitant sans for fiscal
		final Long noPP = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = addHabitant(NO_IND);
				return pp.getNumero();
			}
		});

		// getTiers
		{
			final GetTiers params = new GetTiers();
			params.tiersNumber = noPP;
			params.login = login;

			final ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique pp = (ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);
			assertNull(pp.dateDebutActivite); // pas de for fiscal
			assertEquals(newDate(1967, 12, 31), pp.dateArrivee);
		}

		// getTiersHisto
		{
			final GetTiersHisto params = new GetTiersHisto();
			params.tiersNumber = noPP;
			params.login = login;

			final PersonnePhysiqueHisto pp = (PersonnePhysiqueHisto) service.getTiersHisto(params);
			assertNotNull(pp);
			assertNull(pp.dateDebutActivite); // pas de for fiscal
			assertEquals(newDate(1967, 12, 31), pp.dateArrivee);
		}
	}

	/**
	 * [SIFISC-4623] Vérifie que le numéro de maison n'est retourné que si la rue est renseignée.
	 */
	@Test
	public void testAdresseNumeroMaisonSansRue() throws Exception {

		final long noPM = 20151L;

		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				final MockPersonneMorale pm = addPM(noPM, "Fiduciaire Galper S.A.", "", date(1993, 7, 23), null);
				addAdresse(pm, TypeAdressePM.COURRIER, null, "3bis", null, MockLocalite.CossonayVille, date(1993, 7, 23), date(1999, 12, 31));
				addAdresse(pm, TypeAdressePM.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, "3bis", null, MockLocalite.CossonayVille, date(2000, 1, 1), null);
			}
		});

		final long idPM = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise pm = addEntreprise(noPM);
				return pm.getNumero();
			}
		});

		// getTiers - 1995
		{
			final GetTiers params = new GetTiers();
			params.tiersNumber = idPM;
			params.login = login;
			params.date = newDate(1995, 1, 1);
			params.parts = new HashSet<>();
			params.parts.add(TiersPart.ADRESSES);

			final ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale pm = (PersonneMorale) service.getTiers(params);
			assertNotNull(pm);
			// pas de rue, pas de numéro
			assertAdresse(newDate(1993, 7, 23), newDate(1999, 12, 31), null, null, "Cossonay-Ville", pm.adresseCourrier);
		}

		// getTiers - 2005
		{
			final GetTiers params = new GetTiers();
			params.tiersNumber = idPM;
			params.login = login;
			params.date = newDate(2005, 1, 1);
			params.parts = new HashSet<>();
			params.parts.add(TiersPart.ADRESSES);

			final ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale pm = (PersonneMorale) service.getTiers(params);
			assertNotNull(pm);
			assertAdresse(newDate(2000, 1, 1), null, "Avenue du Funiculaire", "3bis", "Cossonay-Ville", pm.adresseCourrier);
		}
	}

	/**
	 * Type de débiteur non-supporté par cette version du service
	 */
	@Test
	public void testGetDebiteurParticipationsHorsSuisse() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
				return dpi.getNumero();
			}
		});

		final GetTiers params = new GetTiers();
		params.login = login;
		params.tiersNumber = dpiId;
		params.parts = new HashSet<>();
		params.parts.add(TiersPart.PERIODICITES);

		try {
			ch.vd.uniregctb.webservices.tiers2.data.Tiers tiers = service.getTiers(params);
			fail("Ca aurait dû exploser");
		}
		catch (BusinessException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getMessage());
		}
	}

	/**
	 * Type de débiteur non-supporté par cette version du service
	 */
	@Test
	public void testGetDebiteurEffeuilleuses() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.EFFEUILLEUSES, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
				return dpi.getNumero();
			}
		});

		final GetTiers params = new GetTiers();
		params.login = login;
		params.tiersNumber = dpiId;
		params.parts = new HashSet<>();
		params.parts.add(TiersPart.PERIODICITES);

		try {
			ch.vd.uniregctb.webservices.tiers2.data.Tiers tiers = service.getTiers(params);
			fail("Ca aurait dû exploser");
		}
		catch (BusinessException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getMessage());
		}
	}

	/**
	 * Types de débiteur non-supportés par cette version du service
	 */
	@Test
	public void testSearchDebiteurAvecTypeNonSupporte() throws Exception {

		final class Ids {
			long phs;
			long eff;
			long reg;
		}

		// pour être certain de ne rien récupérer d'autre que ce que je vais créer maintenant
		globalTiersIndexer.overwriteIndex();

		final boolean otfi = globalTiersIndexer.isOnTheFlyIndexation();
		globalTiersIndexer.setOnTheFlyIndexation(true);
		try {
			// mise en place des débiteurs
			final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final DebiteurPrestationImposable reg = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
					reg.setNom1("Toto");        // pour la recherche

					final DebiteurPrestationImposable eff = addDebiteur(CategorieImpotSource.EFFEUILLEUSES, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
					eff.setNom1("Toto");        // pour la recherche

					final DebiteurPrestationImposable phs = addDebiteur(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
					phs.setNom1("Toto");        // pour la recherche

					final Ids ids = new Ids();
					ids.phs = phs.getNumero();
					ids.eff = eff.getNumero();
					ids.reg = reg.getNumero();
					return ids;
				}
			});

			// on attend que l'indexation des trois nouveaux débiteurs soit terminée
			globalTiersIndexer.sync();

			// recherche par nom
			final SearchTiers params = new SearchTiers();
			params.login = login;
			params.nomCourrier = "Toto";

			final List<TiersInfo> found = service.searchTiers(params);
			assertNotNull(found);
			assertEquals(1, found.size());

			final TiersInfo singleton = found.get(0);
			assertNotNull(singleton);
			assertEquals(ids.reg, singleton.numero);
		}
		finally {
			globalTiersIndexer.setOnTheFlyIndexation(otfi);
		}
	}
}
