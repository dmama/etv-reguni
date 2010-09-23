package ch.vd.uniregctb.tiers;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ForFiscalTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final ForFiscal forFiscal = new ForFiscalPrincipal();
		forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
		forFiscal.setDateFin(RegDate.get(2009, 12, 31));

		forFiscal.setAnnule(false);
		assertTrue(forFiscal.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(2060, 1, 1)));

		forFiscal.setAnnule(true);
		assertFalse(forFiscal.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(2060, 1, 1)));
	}

	@Test
	public void testValidateDateDebut() {

		final ForFiscal forFiscal = new ForFiscal() {
			public ForFiscal duplicate() {
				throw new NotImplementedException();
			}
		};

		// Date de début nulle
		{
			final ValidationResults results = forFiscal.validate();
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le for  (? - ?) possède une date de début nulle", errors.get(0));
		}

		// Date de début renseignée
		{
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(forFiscal.validate().hasErrors());
		}
	}

	@Test
	public void testValidateForAnnule() {

		final ForFiscal forFiscal = new ForFiscal() {
			public ForFiscal duplicate() {
				throw new NotImplementedException();
			}
		};

		// For invalide (date de début nulle) mais annulé => pas d'erreur
		{
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setAnnule(true);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(forFiscal.validate().hasErrors());
		}
	}

	/**
	 * Test de non régression concernant le cas JIRA UNIREG-585.<br>
	 * </br>
	 * Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger,
	 * les seuls modes d'imposition possibles sont normalement uniquement "ordinaire", "source" ou "mixte 137 al1".
	 * Voir spéc. "enregistrer un nouveau tiers" 3.1.9.
	 */
	@Test
	public void testValidateModeImposition() {

		final ForFiscalPrincipal ffp = new ForFiscalPrincipal();
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setDateDebut(RegDate.get(2000, 1, 1));

		// sans mode d'imposition => erreur
		{
			final ValidationResults results = ffp.validate();
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le mode d'imposition est obligatoire sur un for fiscal principal.", errors.get(0));
		}

		// avec mode d'imposition ordinaire => ok
		{
			ffp.setModeImposition(ModeImposition.ORDINAIRE);
			assertFalse(ffp.validate().hasErrors());
		}

		// avec mode d'imposition mixte 1 => ok
		{
			ffp.setModeImposition(ModeImposition.MIXTE_137_1);
			assertFalse(ffp.validate().hasErrors());
		}

		// avec mode d'imposition source => ok
		{
			ffp.setModeImposition(ModeImposition.SOURCE);
			assertFalse(ffp.validate().hasErrors());
		}

		final String ERREUR_MESSAGE =
				"Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, les modes d'imposition possibles sont \"ordinaire\", \"source\" ou \"mixte 137 al1\".";
		
		// avec mode d'imposition indigent => erreur
		{
			ffp.setModeImposition(ModeImposition.INDIGENT);

			final ValidationResults results = ffp.validate();
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals(ERREUR_MESSAGE, errors.get(0));
		}

		// avec mode d'imposition mixte 2 => erreur
		{
			ffp.setModeImposition(ModeImposition.MIXTE_137_2);

			final ValidationResults results = ffp.validate();
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals(ERREUR_MESSAGE, errors.get(0));
		}

		// avec mode d'imposition dépense => erreur
		{
			ffp.setModeImposition(ModeImposition.DEPENSE);

			final ValidationResults results = ffp.validate();
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals(ERREUR_MESSAGE, errors.get(0));
		}
	}
}
