package ch.vd.uniregctb.validation.documentfiscal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.documentfiscal.DelaiAutreDocumentFiscal;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class DelaiAutreDocumentFiscalValidatorTest extends AbstractValidatorTest<DelaiAutreDocumentFiscal> {

	@Override
	protected String getValidatorBeanName() {
		return "delaiAutreDocumentFiscalValidator";
	}

	@Test
	public void testDateTraitement() throws Exception {
		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setEtat(EtatDelaiDocumentFiscal.DEMANDE);

		// pas de date de traitement -> c'est un problème
		final ValidationResults invalide = validate(delai);
		Assert.assertNotNull(invalide);
		Assert.assertFalse(invalide.hasWarnings());
		Assert.assertEquals(1, invalide.errorsCount());
		Assert.assertEquals("La date de traitement n'est pas renseignée sur le délai du document fiscal.", invalide.getErrors().get(0));

		// mais dès qu'on rajoute la date de demande, ça va mieux
		delai.setDateTraitement(RegDate.get());
		final ValidationResults valide = validate(delai);
		Assert.assertNotNull(valide);
		Assert.assertFalse(valide.hasErrors());
		Assert.assertFalse(valide.hasWarnings());
	}

	@Test
	public void testEtat() throws Exception {
		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setDateTraitement(RegDate.get());

		// pas d'état -> c'est un problème
		final ValidationResults invalide = validate(delai);
		Assert.assertNotNull(invalide);
		Assert.assertFalse(invalide.hasWarnings());
		Assert.assertEquals(1, invalide.errorsCount());
		Assert.assertEquals("L'état du délai n'est pas renseigné sur le délai du document fiscal.", invalide.getErrors().get(0));

		// mais dès qu'on rajoute l'état, ça va mieux
		delai.setEtat(EtatDelaiDocumentFiscal.DEMANDE);
		final ValidationResults valide = validate(delai);
		Assert.assertNotNull(valide);
		Assert.assertFalse(valide.hasErrors());
		Assert.assertFalse(valide.hasWarnings());
	}

	@Test
	public void testDateDelaiAccorde() throws Exception {
		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setDateTraitement(RegDate.get());

		final RegDate[] dates = { null, RegDate.get().addMonths(3) };
		for (EtatDelaiDocumentFiscal etat : EtatDelaiDocumentFiscal.values()) {
			for (RegDate date : dates) {
				delai.setEtat(etat);
				delai.setDelaiAccordeAu(date);
				final ValidationResults vr = validate(delai);
				Assert.assertNotNull(vr);
				if ((etat == EtatDelaiDocumentFiscal.ACCORDE && date != null) || (etat != EtatDelaiDocumentFiscal.ACCORDE && date == null)) {
					Assert.assertFalse(String.format("%s/%s", etat, date), vr.hasWarnings());
					Assert.assertFalse(String.format("%s/%s", etat, date), vr.hasErrors());
				}
				else if (etat == EtatDelaiDocumentFiscal.ACCORDE) {
					Assert.assertFalse(vr.hasWarnings());
					Assert.assertTrue(vr.hasErrors());
					Assert.assertEquals(1, vr.errorsCount());
					Assert.assertEquals("La date de délai accordé est obligatoire sur un délai dans l'état 'accordé' du document fiscal.", vr.getErrors().get(0));
				}
				else {
					Assert.assertFalse(String.format("%s/%s", etat, date), vr.hasWarnings());
					Assert.assertTrue(String.format("%s/%s", etat, date), vr.hasErrors());
					Assert.assertEquals(String.format("%s/%s", etat, date), 1, vr.errorsCount());
					Assert.assertEquals(String.format("%s/%s", etat, date), "La date de délai accordé est interdite sur un délai dans un état différent de 'accordé'.", vr.getErrors().get(0));
				}
			}
		}
	}

	@Test
	public void testSursis() throws Exception {
		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setDateTraitement(RegDate.get());
		delai.setSursis(true);

		for (EtatDelaiDocumentFiscal etat : EtatDelaiDocumentFiscal.values()) {
			// la date de délai accordé par rapport à l'état est un autre test...
			if (etat == EtatDelaiDocumentFiscal.ACCORDE) {
				delai.setDelaiAccordeAu(RegDate.get().addDays(50));
			}
			else {
				delai.setDelaiAccordeAu(null);
			}
			delai.setEtat(etat);

			final ValidationResults vr = validate(delai);
			Assert.assertNotNull(vr);
			Assert.assertFalse(vr.hasWarnings());
			if (etat == EtatDelaiDocumentFiscal.ACCORDE) {
				Assert.assertFalse(vr.hasErrors());
			}
			else {
				Assert.assertTrue(etat.name(), vr.hasErrors());
				Assert.assertEquals(etat.name(), 1, vr.errorsCount());
				Assert.assertEquals(etat.name(), "Seuls les délais accordés peuvent être dotés du flag 'sursis'.", vr.getErrors().get(0));
			}
		}
	}
}
