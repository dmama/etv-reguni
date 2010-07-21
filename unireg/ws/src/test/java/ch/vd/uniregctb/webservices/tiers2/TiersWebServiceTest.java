package ch.vd.uniregctb.webservices.tiers2;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.Adresse;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;

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
