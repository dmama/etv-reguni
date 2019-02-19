package ch.vd.unireg.validation.fors;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ForFiscalPrincipalPPValidatorTest extends AbstractValidatorTest<ForFiscalPrincipalPP> {

	@Override
	protected String getValidatorBeanName() {
		return "forFiscalPrincipalPPValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForAnnule() {

		final ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();

		// For invalide (mode d'imposition incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setModeImposition(null);
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5586);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForDiplomateSuisse() {

		final ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
		forFiscal.setMotifRattachement(MotifRattachement.DIPLOMATE_SUISSE);
		forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		forFiscal.setNumeroOfsAutoriteFiscale(5586);
		forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
		forFiscal.setDateDebut(RegDate.get(2000, 1, 1));

		// For diplomate et motifs début/fin activité diplomatique => valide
		{
			// for ouvert
			forFiscal.setMotifOuverture(MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE);
			assertFalse(validate(forFiscal).hasErrors());

			// for fermé
			forFiscal.setDateFin(RegDate.get(2005,12,31));
			forFiscal.setMotifFermeture(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE);
			assertFalse(validate(forFiscal).hasErrors());
		}

		// For non-diplomate et motifs début/fin activité diplomatique => invalide
		{
			// for ouvert
			forFiscal.setDateFin(null);
			forFiscal.setMotifFermeture(null);

			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			ValidationResults results = validate(forFiscal);
			assertTrue(results.hasErrors());

			List<String> errors = results.getErrors();
			assertNotNull(errors);
			assertEquals(1, errors.size());
			assertEquals("Le motif de début d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger", errors.get(0));

			// for fermé
			forFiscal.setDateFin(RegDate.get(2005,12,31));
			forFiscal.setMotifFermeture(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE);

			results = validate(forFiscal);
			assertTrue(results.hasErrors());

			errors = results.getErrors();
			assertNotNull(errors);
			assertEquals(2, errors.size());
			assertEquals("Le motif de début d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger", errors.get(0));
			assertEquals("Le motif de fin d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger", errors.get(1));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateDebut() {

		final ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
		forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Vevey.getNoOFS());
		forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
		forFiscal.setModeImposition(ModeImposition.ORDINAIRE);

		// Date de début nulle
		{
			final ValidationResults results = validate(forFiscal);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(2, errors.size());
			assertEquals("Le for fiscal ForFiscalPrincipalPP (? - ?) possède une date de début nulle", errors.get(0));
			assertEquals("Le motif d'ouverture est obligatoire sur le for fiscal [ForFiscalPrincipalPP (? - ?)] car il est rattaché à une commune vaudoise.", errors.get(1));
		}

		// Date de début renseignée
		{
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HS);
			assertFalse(validate(forFiscal).hasErrors());
		}
	}

	/**
	 * Test de non régression concernant des cas JIRA UNIREG-585 et SIFISC-57.<br>
	 * </br>
	 * Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger,
	 * les seuls modes d'imposition possibles sont normalement uniquement "ordinaire", "source" ou "mixte 137 al1".
	 * Voir spéc. "enregistrer un nouveau tiers" 3.1.9.
	 * Update SIFISC-57 : seuls les modes d'imposition "source" (cas général) ou "ordinaire" (avec for secondaire) sont autorisés.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateModeImposition() {

		final ForFiscalPrincipalPP ffp = new ForFiscalPrincipalPP();
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setDateDebut(RegDate.get(2000, 1, 1));
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HS);

		for (TypeAutoriteFiscale taf : Arrays.asList(TypeAutoriteFiscale.COMMUNE_HC, TypeAutoriteFiscale.PAYS_HS)) {
			ffp.setNumeroOfsAutoriteFiscale(taf == TypeAutoriteFiscale.COMMUNE_HC ? MockCommune.Neuchatel.getNoOFS() : MockPays.France.getNoOFS());
			ffp.setTypeAutoriteFiscale(taf);

			// sans mode d'imposition => erreur
			{
				ffp.setModeImposition(null);
				final ValidationResults results = validate(ffp);
				Assert.assertTrue(results.hasErrors());
				final List<String> errors = results.getErrors();
				assertEquals(1, errors.size());
				assertEquals("Le mode d'imposition est obligatoire sur un for fiscal principal.", errors.get(0));
			}

			// avec mode d'imposition ordinaire => ok
			{
				ffp.setModeImposition(ModeImposition.ORDINAIRE);
				assertFalse(validate(ffp).hasErrors());
			}

			// avec mode d'imposition source => ok
			{
				ffp.setModeImposition(ModeImposition.SOURCE);
				assertFalse(validate(ffp).hasErrors());
			}

			final String ERREUR_MESSAGE =
					"Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, les modes d'imposition possibles sont \"ordinaire\" ou \"source\".";

			// avec mode d'imposition indigent => erreur
			{
				ffp.setModeImposition(ModeImposition.INDIGENT);

				final ValidationResults results = validate(ffp);
				Assert.assertTrue(results.hasErrors());
				final List<String> errors = results.getErrors();
				assertEquals(1, errors.size());
				assertEquals(ERREUR_MESSAGE, errors.get(0));
			}

			// avec mode d'imposition mixte 1 => erreur
			{
				ffp.setModeImposition(ModeImposition.MIXTE_137_1);

				final ValidationResults results = validate(ffp);
				Assert.assertTrue(results.hasErrors());
				final List<String> errors = results.getErrors();
				assertEquals(1, errors.size());
				assertEquals(ERREUR_MESSAGE, errors.get(0));
			}

			// avec mode d'imposition mixte 2 => erreur
			{
				ffp.setModeImposition(ModeImposition.MIXTE_137_2);

				final ValidationResults results = validate(ffp);
				Assert.assertTrue(results.hasErrors());
				final List<String> errors = results.getErrors();
				assertEquals(1, errors.size());
				assertEquals(ERREUR_MESSAGE, errors.get(0));
			}

			// avec mode d'imposition dépense => erreur
			{
				ffp.setModeImposition(ModeImposition.DEPENSE);

				final ValidationResults results = validate(ffp);
				Assert.assertTrue(results.hasErrors());
				final List<String> errors = results.getErrors();
				assertEquals(1, errors.size());
				assertEquals(ERREUR_MESSAGE, errors.get(0));
			}
		}
	}

	@Test
	public void testPresenceDateFermetureSiMotifFermeturePresent() throws Exception {

		final ForFiscalPrincipalPP ffp = new ForFiscalPrincipalPP();
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setDateDebut(RegDate.get(2000, 1, 1));
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HS);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp.setMotifFermeture(MotifFor.DEPART_HS);
		{
			ffp.setDateFin(null);
			final ValidationResults vr = validate(ffp);
			Assert.assertTrue(vr.hasErrors());
			final List<String> errors = vr.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Une date de fermeture doit être indiquée si un motif de fermeture l'est.", errors.get(0));
		}
		{
			ffp.setDateFin(date(2005, 5, 23));
			final ValidationResults vr = validate(ffp);
			Assert.assertFalse(vr.hasErrors());
		}
	}
}
