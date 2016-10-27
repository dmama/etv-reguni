package ch.vd.uniregctb.validation.etiquette;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class EtiquetteValidatorTest extends AbstractValidatorTest<Etiquette> {

	@Override
	protected String getValidatorBeanName() {
		return "etiquetteValidator";
	}

	@Test
	public void testValidationEtAnnulation() throws Exception {
		final Etiquette etiquette = new Etiquette();

		// donnée invalide (manque le code et le libellé) mais annulée -> pas d'erreur
		{
			etiquette.setCode(null);
			etiquette.setAnnule(true);
			Assert.assertFalse(validate(etiquette).hasErrors());
		}

		// donnée invalide (manque le code et le libellé) et non-annulée -> erreur
		{
			etiquette.setCode(null);
			etiquette.setAnnule(false);
			Assert.assertTrue(validate(etiquette).hasErrors());
		}
	}

	@Test
	public void testCodeAbsent() throws Exception {
		final Etiquette etiquette = new Etiquette();

		// invalide sans code
		{
			etiquette.setLibelle("Mon libellé");
			etiquette.setCode(null);
			assertValidation(Collections.singletonList("Le code ne doit pas être vide."), null, validate(etiquette));
		}

		// invalide avec code vide
		{
			etiquette.setLibelle("Mon libellé");
			etiquette.setCode("  ");
			assertValidation(Collections.singletonList("Le code ne doit pas être vide."), null, validate(etiquette));
		}

		// valide avec code
		{
			etiquette.setLibelle("Mon libellé");
			etiquette.setCode("ZDF");
			Assert.assertFalse(validate(etiquette).hasErrors());
		}
	}

	@Test
	public void testLibelleAbsent() throws Exception {
		final Etiquette etiquette = new Etiquette();

		// invalide sans libellé
		{
			etiquette.setLibelle(null);
			etiquette.setCode("TOTO");
			assertValidation(Collections.singletonList("Le libellé ne doit pas être vide."), null, validate(etiquette));
		}

		// invalide avec libellé vide
		{
			etiquette.setLibelle("    ");
			etiquette.setCode("TOTO");
			assertValidation(Collections.singletonList("Le libellé ne doit pas être vide."), null, validate(etiquette));
		}

		// valide avec libellé
		{
			etiquette.setLibelle("Mon libellé");
			etiquette.setCode("TOTO");
			Assert.assertFalse(validate(etiquette).hasErrors());
		}
	}
}
