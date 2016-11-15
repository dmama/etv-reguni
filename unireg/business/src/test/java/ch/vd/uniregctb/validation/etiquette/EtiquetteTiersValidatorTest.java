package ch.vd.uniregctb.validation.etiquette;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.type.TypeTiersEtiquette;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class EtiquetteTiersValidatorTest extends AbstractValidatorTest<EtiquetteTiers> {

	@Override
	protected String getValidatorBeanName() {
		return "etiquetteTiersValidator";
	}

	@Test
	public void testValidationEtAnnulation() throws Exception {
		final EtiquetteTiers etiquette = new EtiquetteTiers();

		// donnée invalide (manque les dates, l'étiquette, le tiers...) mais annulée -> pas d'erreur
		{
			etiquette.setAnnule(true);
			Assert.assertFalse(validate(etiquette).hasErrors());
		}

		// donnée invalide (manque les dates, l'étiquette, le tiers) et non-annulée -> erreur
		{
			etiquette.setAnnule(false);
			Assert.assertTrue(validate(etiquette).hasErrors());
		}
	}

	@Test
	public void testDateDebut() throws Exception {
		// ça, en gros, c'est pour vérifier que le validateur fait bien appel au DateRangeEntityValidator
		final Etiquette etiquette = new Etiquette("MYCODE", "Une étiquette de test", true, TypeTiersEtiquette.PP, null);
		final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(null, null, etiquette);
		assertValidation(Collections.singletonList("L'étiquette EtiquetteTiers (? - ?) possède une date de début nulle"), null, validate(etiquetteTiers));
	}

	@Test
	public void testPresenceEtiquetteLiee() throws Exception {

		{
			final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(date(2000, 1, 1), null, null);
			assertValidation(Collections.singletonList("Le lien d'étiquetage doit être associé à une étiquette."), null, validate(etiquetteTiers));
		}

		{
			final Etiquette etiquette = new Etiquette("MYCODE", "Une étiquette de test", true, TypeTiersEtiquette.PP_MC, null);
			final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(date(2000, 1, 1), null, etiquette);
			Assert.assertFalse(validate(etiquetteTiers).hasErrors());
		}
	}
}