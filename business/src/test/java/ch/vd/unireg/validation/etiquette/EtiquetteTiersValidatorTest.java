package ch.vd.unireg.validation.etiquette;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.type.TypeTiersEtiquette;
import ch.vd.unireg.validation.AbstractValidatorTest;

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
	public void testDateDebutNulle() throws Exception {
		// ça, en gros, c'est pour vérifier que le validateur fait bien appel au DateRangeEntityValidator
		final Etiquette etiquette = new Etiquette("MYCODE", "Une étiquette de test", true, TypeTiersEtiquette.PP, null);
		final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(null, null, etiquette);
		assertValidation(Collections.singletonList("L'étiquette 'Une étiquette de test' (? - ?) possède une date de début nulle"), null, validate(etiquetteTiers));
	}

	@Test
	public void testDateDebutFuture() throws Exception {
		// les dates de début futures sont autorisées
		final Etiquette etiquette = new Etiquette("MYCODE", "Une étiquette de test", true, TypeTiersEtiquette.PP, null);
		final EtiquetteTiers etiquetteTiers = new EtiquetteTiers(RegDate.get().addMonths(1), null, etiquette);
		assertValidation(null, null, validate(etiquetteTiers));
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
