package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.CapitalEntreprise;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class EntrepriseValidatorTest extends AbstractValidatorTest<Entreprise> {

	private static final String CHF = "CHF";

	@Override
	protected String getValidatorBeanName() {
		return "entrepriseValidator";
	}

	@Test
	public void testChevauchementRegimesFiscaux() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), date(2005, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), null, RegimeFiscal.Portee.CH, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));

		// ici, tout va bien, les différentes portées ne se marchent pas dessus
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons un régime fiscal VD qui ne chevauche pas -> pas de souci

		entreprise.addRegimeFiscal(new RegimeFiscal(date(2007, 1, 1), date(2009, 12, 1), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons un régime fiscal VD qui chevauche -> rien ne va plus
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2005, 1, 1), date(2007, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(2, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs régimes fiscaux VD", errors.get(0));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs régimes fiscaux VD", errors.get(1));
		}
	}

	@Test
	public void testRegimeFiscalInvalide() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2010, 1, 1), date(2005, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le régime fiscal RegimeFiscal VD (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testChevauchementDonneesRegistreCommerce() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneesRC(new DonneesRegistreCommerce(date(2000, 1, 1), date(2005, 12, 31), "Ma petite entreprise", FormeJuridiqueEntreprise.SARL));

		// aucun chevauchement (= 1 seule donnée, de toute façon...)
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui ne chevauche pas -> pas de souci
		entreprise.addDonneesRC(new DonneesRegistreCommerce(date(2007, 1, 1), date(2009, 12, 1), "Ma petite entreprise", FormeJuridiqueEntreprise.SARL));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui chevauche -> rien ne va plus
		entreprise.addDonneesRC(new DonneesRegistreCommerce(date(2005, 1, 1), date(2007, 12, 31), "Ma petite entreprise", FormeJuridiqueEntreprise.SARL));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(2, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs ensembles de données RC", errors.get(0));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs ensembles de données RC", errors.get(1));
		}
	}

	@Test
	public void testDonneesRegistreCommerceInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneesRC(new DonneesRegistreCommerce(date(2010, 1, 1), date(2005, 12, 31), "Ma grande entreprise", FormeJuridiqueEntreprise.SA));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("L'ensemble de données du registre du commerce DonneesRegistreCommerce (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testChevauchementCapitaux() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2000, 1, 1), date(2005, 12, 31), new MontantMonetaire(50000L, CHF)));

		// aucun chevauchement (= 1 seule donnée, de toute façon...)
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui ne chevauche pas -> pas de souci
		entreprise.addCapital(new CapitalEntreprise(date(2007, 1, 1), date(2009, 12, 1), new MontantMonetaire(60000L, CHF)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui chevauche -> rien ne va plus
		entreprise.addCapital(new CapitalEntreprise(date(2005, 1, 1), date(2007, 12, 31), new MontantMonetaire(55000L, CHF)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(2, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs données de capital", errors.get(0));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs données de capital", errors.get(1));
		}
	}

	@Test
	public void testCapitauxInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2010, 1, 1), date(2005, 12, 31), new MontantMonetaire(1000000L, CHF)));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("La surcharge de capital CapitalEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testChevauchementAllegementsFiscaux() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addAllegementFiscal(new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.valueOf(25L), AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscal.TypeCollectivite.CANTON, null));
		entreprise.addAllegementFiscal(new AllegementFiscal(date(2000, 1, 1), null, BigDecimal.TEN, AllegementFiscal.TypeImpot.CAPITAL, AllegementFiscal.TypeCollectivite.CANTON, null));

		// ici, tout va bien, les différents allègements ne se marchent pas dessus
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
			Assert.assertFalse(vr.toString(), vr.hasWarnings());
		}

		// ajoutons un nouvel allègement avec une cible déjà existante, sans chevauchement
		entreprise.addAllegementFiscal(new AllegementFiscal(date(2006, 1, 1), date(2006, 12, 31), BigDecimal.valueOf(35L), AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscal.TypeCollectivite.CANTON, null));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
			Assert.assertFalse(vr.toString(), vr.hasWarnings());
		}

		// ajoutons maintenant un chevauchement
		entreprise.addAllegementFiscal(new AllegementFiscal(date(2004, 1, 1), date(2007, 12, 31), BigDecimal.valueOf(22L), AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscal.TypeCollectivite.CANTON, null));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("La période [01.01.2004 ; 31.12.2006] est couverte par plusieurs allègements fiscaux de type 'allègement BENEFICE CANTON'.", error);
		}
	}

	@Test
	public void testAllegementsFiscauxInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addAllegementFiscal(new AllegementFiscal(date(2010, 1, 1), date(2005, 12, 31), BigDecimal.TEN, AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscal.TypeCollectivite.COMMUNE, null));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("L'allègement fiscal AllegementFiscal BENEFICE COMMUNE (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testCapitalSansMonnaie() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(1000000L, null)));     // pas de monnaie !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant (1000000 ici) et d'une monnaie ('' ici).", errors.get(0));
	}

	@Test
	public void testCapitalSansMontant() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(null, CHF)));     // pas de montant !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant (vide ici) et d'une monnaie ('CHF' ici).", errors.get(0));
	}

	@Test
	public void testCapitalSansMontantNiMonnaie() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(null, null)));

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant (vide ici) et d'une monnaie ('' ici).", errors.get(0));
	}

	@Test
	public void testSansCapital() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2010, 1, 1), null, null));

		final ValidationResults vr = validate(entreprise);
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant et d'une monnaie (tous deux vides ici).", errors.get(0));
	}

	@Test
	public void testCapitalAvecMontantNegatif() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addCapital(new CapitalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(-42L, CHF)));

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise ne peut être négatif (-42).", errors.get(0));
	}
}
