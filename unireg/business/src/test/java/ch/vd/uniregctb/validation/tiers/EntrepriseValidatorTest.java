package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
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
	public void testChevauchementDonneesCiviles() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2000, 1, 1), date(2005, 12, 31), "Ma petite entreprise"));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2000, 1, 1), date(2005, 12, 31), FormeJuridiqueEntreprise.SARL));
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2000, 1, 1), date(2005, 12, 31), new MontantMonetaire(10000L, MontantMonetaire.CHF)));

		// aucun chevauchement (= 1 seule donnée de chaque type, de toute façon...)
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui ne chevauche pas -> pas de souci
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2006, 1, 1), date(2009, 12, 1), "Ma petite entreprise"));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2006, 1, 1), date(2009, 12, 1), FormeJuridiqueEntreprise.SARL));
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2007, 1, 1), date(2009, 12, 1), new MontantMonetaire(10000L, MontantMonetaire.CHF)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui chevauche -> rien ne va plus
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2005, 1, 1), date(2007, 12, 31), "Ma petite entreprise"));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2005, 1, 1), date(2007, 12, 31), FormeJuridiqueEntreprise.SARL));
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2005, 1, 1), date(2007, 12, 31), new MontantMonetaire(10000L, MontantMonetaire.CHF)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(4, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2007] est couverte par plusieurs valeurs de raison sociale", errors.get(0));
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2007] est couverte par plusieurs valeurs de forme juridique", errors.get(1));
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs valeurs de capital", errors.get(2));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs valeurs de capital", errors.get(3));
		}
	}

	@Test
	public void testDonneesCivilesRaisonSocialeInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2010, 1, 1), date(2005, 12, 31), "Ma grande entreprise"));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("La raison sociale RaisonSocialeFiscaleEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testDonneesCivilesRaisonSocialeTrouee() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2005, 1, 1), date(2010, 12, 31), "Ma grande entreprise"));
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2011, 6, 1), date(2015, 1, 1), "Ma très grande entreprise")); // Il y a un trou du 1.1.2011 au 31.5.2011

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Rupture de continuité: période vide [01.01.2011 ; 31.05.2011] dans la valeur de raison sociale", errors.get(0));
	}

	@Test
	public void testDonneesCivilesFormeJuridiqueInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2010, 1, 1), date(2005, 12, 31), FormeJuridiqueEntreprise.SA));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("La forme juridique FormeJuridiqueFiscaleEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testDonneesCivilesFormeJuridiqueTrouee() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2005, 1, 1), date(2010, 12, 31), FormeJuridiqueEntreprise.SA));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2011, 6, 1), date(2015, 1, 1), FormeJuridiqueEntreprise.SA)); // Il y a un trou du 1.1.2011 au 31.5.2011

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Rupture de continuité: période vide [01.01.2011 ; 31.05.2011] dans la valeur de forme juridique", errors.get(0));
	}

	@Test
	public void testDonneesCivilesCapitalInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), date(2005, 12, 31), new MontantMonetaire(10000L, MontantMonetaire.CHF)));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital CapitalFiscalEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
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
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(1000000L, null)));     // pas de monnaie !

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
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(null, CHF)));     // pas de montant !

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
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(null, null)));

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
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, null));

		final ValidationResults vr = validate(entreprise);
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant et d'une monnaie (tous deux vides ici).", errors.get(0));
	}

	@Test
	public void testCapitalAvecMontantNegatif() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(-42L, CHF)));

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise ne peut être négatif (-42).", errors.get(0));
	}
}
