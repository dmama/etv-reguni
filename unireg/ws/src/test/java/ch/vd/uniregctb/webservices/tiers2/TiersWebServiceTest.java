package ch.vd.uniregctb.webservices.tiers2;

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
import ch.vd.uniregctb.webservices.tiers2.data.*;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import static org.junit.Assert.*;

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

	private Date newDate(int year, int month, int day) {
		return new Date(year, month, day);
	}
}
