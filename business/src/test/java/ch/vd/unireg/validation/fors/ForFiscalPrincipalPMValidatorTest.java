package ch.vd.unireg.validation.fors;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ForFiscalPrincipalPMValidatorTest extends AbstractValidatorTest<ForFiscalPrincipalPM> {

	@Override
	protected String getValidatorBeanName() {
		return "forFiscalPrincipalPMValidator";
	}

	/**
	 * [SIFISC-26314] Ce test que les deux genres d'impôt sont autorisés sur les fors fiscaux qui se ferment avant le 1er janvier 2009.
	 */
	@Test
	public void testForFiscalAvant2009() throws Exception {

		final ForFiscalPrincipalPM ff = new ForFiscalPrincipalPM();
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Grandson.getNoOFS());
		ff.setDateDebut(date(1960, 1, 1));
		ff.setDateFin(date(2008, 12, 31));
		ff.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		ff.setMotifFermeture(MotifFor.FIN_EXPLOITATION);

		// autorisé
		ff.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
		assertFalse(validate(ff).hasErrors());

		// autorisé
		ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		assertFalse(validate(ff).hasErrors());

		// pas autorisé
		ff.setGenreImpot(GenreImpot.CHIENS);
		assertTrue(validate(ff).hasErrors());
	}

	/**
	 * [SIFISC-26314] Ce test que seul le genre d'impôt REVENU_FORTUNE est autorisé sur les sociétés de personnes.
	 */
	@Test
	public void testForFiscalSocieteDePersonne() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));

		final ForFiscalPrincipalPM ff = new ForFiscalPrincipalPM();
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Grandson.getNoOFS());
		ff.setDateDebut(date(2009, 1, 1));
		ff.setDateFin(null);
		ff.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		ff.setMotifFermeture(null);
		ff.setTiers(entreprise);

		// autorisé
		ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		assertFalse(validate(ff).hasErrors());

		// pas autorisé
		ff.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
		{
			final ValidationResults results = validate(ff);
			assertTrue(results.hasErrors());

			final List<String> errors = results.getErrors();
			assertNotNull(errors);
			assertEquals(1, errors.size());
			assertEquals("Le for ForFiscalPrincipalPM (01.01.2009 - ?) avec genre d'impôt 'BENEFICE_CAPITAL' est invalide.", errors.get(0));
		}
	}

	/**
	 * [SIFISC-26314] Ce test que seul le genre d'impôt BENEFICE_CAPITAL est autorisé sur les sociétés ordinaires.
	 */
	@Test
	public void testForFiscalRegimeOrdinaire() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));

		final ForFiscalPrincipalPM ff = new ForFiscalPrincipalPM();
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Grandson.getNoOFS());
		ff.setDateDebut(date(2009, 1, 1));
		ff.setDateFin(null);
		ff.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		ff.setMotifFermeture(null);
		ff.setTiers(entreprise);

		// autorisé
		ff.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
		assertFalse(validate(ff).hasErrors());

		// pas autorisé
		ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		{
			final ValidationResults results = validate(ff);
			assertTrue(results.hasErrors());

			final List<String> errors = results.getErrors();
			assertNotNull(errors);
			assertEquals(1, errors.size());
			assertEquals("Le for ForFiscalPrincipalPM (01.01.2009 - ?) avec genre d'impôt 'REVENU_FORTUNE' est invalide.", errors.get(0));
		}
	}
}