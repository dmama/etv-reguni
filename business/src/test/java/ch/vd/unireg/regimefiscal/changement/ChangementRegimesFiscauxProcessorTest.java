package ch.vd.unireg.regimefiscal.changement;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RegimeFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ChangementRegimesFiscauxProcessorTest extends BusinessTest {

	private ChangementRegimesFiscauxProcessor processor;

	@Before
	public void setUp() throws Exception {
		processor = new ChangementRegimesFiscauxProcessor(transactionManager, tiersDAO, tiersService, regimeFiscalService);
	}

	/**
	 * Vérifie que le processeur n'essaie pas de faire n'importe quoi.
	 */
	@Test
	public void testProcessEntrepriseSansRegimeAModifier() throws Exception {

		final Long id = doInNewTransaction(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, "01"));
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.CH, "01"));
			return entreprise.getId();
		});

		final RegDate dateChangement = date(2018, 1, 1);

		doInNewTransaction(status -> {
			try {
				final ChangementRegimesFiscauxJobResults rapport = new ChangementRegimesFiscauxJobResults(null, null, dateChangement);
				processor.processEntreprise(id, "70", "703", dateChangement, rapport);
				fail();
			}
			catch (ProgrammingException e) {
				assertEquals("L'entreprise n°" + id + " ne possède pas de régime fiscal de type = [70] valide le 01.01.2018", e.getMessage());
			}
			return null;
		});

	}

	/**
	 * Vérifie que les entreprises qui possèdent des régimes fiscaux qui commencent juse à la date de changement sont bien passées en erreur
	 */
	@Test
	public void testProcessEntrepriseAvecRegimesCommencantALaDateChangement() throws Exception {

		final RegDate dateChangement = date(2018, 1, 1);

		// une entreprise avec un régime 01 ouvert juste à la date de changement
		final Long id = doInNewTransaction(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			entreprise.addRegimeFiscal(new RegimeFiscal(dateChangement, null, RegimeFiscal.Portee.VD, "70"));
			entreprise.addRegimeFiscal(new RegimeFiscal(dateChangement, null, RegimeFiscal.Portee.CH, "70"));
			return entreprise.getId();
		});

		// on ne doit pas la traiter
		doInNewTransaction(status -> {
			final ChangementRegimesFiscauxJobResults rapport = new ChangementRegimesFiscauxJobResults(null, null, dateChangement);
			processor.processEntreprise(id, "70", "703", dateChangement, rapport);

			assertEmpty(rapport.getTraites());
			final List<ChangementRegimesFiscauxJobResults.ErreurInfo> erreurs = rapport.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());

			final ChangementRegimesFiscauxJobResults.ErreurInfo erreur0 = erreurs.get(0);
			assertNotNull(erreur0);
			assertEquals(id.longValue(), erreur0.entrepriseId);
			assertEquals("L'entreprise possède déjà des régimes fiscaux qui commencent justement le 01.01.2018", erreur0.message);
			return null;
		});
	}

	/**
	 * Vérifie que les entreprises qui possèdent des régimes fiscaux déjà modifiés après la date de changement sont bien passées en erreur
	 */
	@Test
	public void testProcessEntrepriseAvecRegimesModifiesApresDateChangement() throws Exception {

		// une entreprise avec un régime 01 ouvert après la date de changement
		final Long id = doInNewTransaction(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), date(2018, 6, 30), RegimeFiscal.Portee.VD, "70"));
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), date(2018, 6, 30), RegimeFiscal.Portee.CH, "70"));
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2018, 7, 1), null, RegimeFiscal.Portee.VD, "01"));
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2018, 7, 1), null, RegimeFiscal.Portee.CH, "01"));
			return entreprise.getId();
		});

		final RegDate dateChangement = date(2018, 1, 1);

		// on ne doit pas la traiter
		doInNewTransaction(status -> {
			final ChangementRegimesFiscauxJobResults rapport = new ChangementRegimesFiscauxJobResults(null, null, dateChangement);
			processor.processEntreprise(id, "70", "703", dateChangement, rapport);

			assertEmpty(rapport.getTraites());
			final List<ChangementRegimesFiscauxJobResults.ErreurInfo> erreurs = rapport.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());

			final ChangementRegimesFiscauxJobResults.ErreurInfo erreur0 = erreurs.get(0);
			assertNotNull(erreur0);
			assertEquals(id.longValue(), erreur0.entrepriseId);
			assertEquals("L'entreprise possède des régimes fiscaux modifiés après le 01.01.2018", erreur0.message);
			return null;
		});
	}

	/**
	 * Vérifie que le processeur met bien à jour les régimes fiscaux correspondent aux critères.
	 */
	@Test
	public void testProcessEntrepriseAvecRegimesAModifier() throws Exception {

		// une entreprise avec un régime 70
		final Long id = doInNewTransaction(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, "70"));
			entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.CH, "70"));
			return entreprise.getId();
		});

		final RegDate dateChangement = date(2018, 1, 1);

		// on doit bien la traiter
		doInNewTransaction(status -> {
			final ChangementRegimesFiscauxJobResults rapport = new ChangementRegimesFiscauxJobResults(null, null, dateChangement);
			processor.processEntreprise(id, "70", "703", dateChangement, rapport);

			assertEmpty(rapport.getErreurs());
			final List<ChangementRegimesFiscauxJobResults.TraiteInfo> traites = rapport.getTraites();
			assertNotNull(traites);
			final ChangementRegimesFiscauxJobResults.TraiteInfo traite0 = traites.get(0);
			assertEquals(id.longValue(), traite0.entrepriseId);

			final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
			assertNotNull(entreprise);

			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(4, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(date(2009, 1, 1), dateChangement.getOneDayBefore(), RegimeFiscal.Portee.VD, "70", regimesFiscaux.get(0));
			assertRegimeFiscal(date(2009, 1, 1), dateChangement.getOneDayBefore(), RegimeFiscal.Portee.CH, "70", regimesFiscaux.get(1));
			assertRegimeFiscal(dateChangement, null, RegimeFiscal.Portee.VD, "703", regimesFiscaux.get(2));
			assertRegimeFiscal(dateChangement, null, RegimeFiscal.Portee.CH, "703", regimesFiscaux.get(3));
			return null;
		});
	}
}