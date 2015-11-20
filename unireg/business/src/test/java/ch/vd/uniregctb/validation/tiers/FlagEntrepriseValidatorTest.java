package ch.vd.uniregctb.validation.tiers;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class FlagEntrepriseValidatorTest extends AbstractValidatorTest<FlagEntreprise> {

	@Override
	protected String getValidatorBeanName() {
		return "flagEntrepriseValidator";
	}

	@Test
	public void testAnnule() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise();
		final ValidationResults vrNonAnnule = validate(flag);
		Assert.assertTrue(vrNonAnnule.hasErrors());     // il manque au moins le type...

		flag.setAnnule(true);
		final ValidationResults vrAnnule = validate(flag);
		Assert.assertFalse(vrAnnule.hasErrors());
	}

	@Test
	public void testTypeManquant() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise(null, 2000, null);       // sans type
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("Le type de flag entreprise est obligatoire.", error);
		}

		flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}
	}

	@Test
	public void testAnneeDebut() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise();
		flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'année de début de validité est obligatoire sur un flag entreprise.", error);
		}

		flag.setAnneeDebutValidite(1290);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'année de début de validité d'un flag entreprise doit être comprise entre 1291 et 2399 (trouvé 1290).", error);
		}

		flag.setAnneeDebutValidite(2400);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'année de début de validité d'un flag entreprise doit être comprise entre 1291 et 2399 (trouvé 2400).", error);
		}

		flag.setAnneeDebutValidite(2014);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}
	}

	@Test
	public void testAnneeFin() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise();
		flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
		flag.setAnneeDebutValidite(2000);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}

		flag.setAnneeFinValidite(1290);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(2, vr.getErrors().size());

			Assert.assertEquals("L'année de fin de validité (1290) d'un flag entreprise doit être postérieure ou égale à son année de début de validité (2000).", vr.getErrors().get(0));
			Assert.assertEquals("L'année de fin de validité d'un flag entreprise doit être comprise entre 1291 et 2399 (trouvé 1290).", vr.getErrors().get(1));
		}

		flag.setAnneeFinValidite(2400);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'année de fin de validité d'un flag entreprise doit être comprise entre 1291 et 2399 (trouvé 2400).", error);
		}

		flag.setAnneeFinValidite(1999);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'année de fin de validité (1999) d'un flag entreprise doit être postérieure ou égale à son année de début de validité (2000).", error);
		}

		flag.setAnneeFinValidite(2000);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}

		flag.setAnneeFinValidite(2006);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}
	}
}