package ch.vd.unireg.validation.remarque;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class RemarqueValidatorTest extends AbstractValidatorTest<Remarque> {

	@Override
	protected String getValidatorBeanName() {
		return "remarqueValidator";
	}

	@Test
	public void testRemarqueAnnuleeAvecTexteVide() throws Exception {
		final Remarque remarque = new Remarque();
		remarque.setAnnule(true);
		remarque.setTexte(null);

		final ValidationResults vr = validate(remarque);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(0, vr.errorsCount());
	}

	@Test
	public void testRemarqueNonAnnuleeAvecTexteVide() throws Exception {
		final Remarque remarque = new Remarque();
		remarque.setAnnule(false);
		remarque.setTexte(null);

		final ValidationResults vr = validate(remarque);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(1, vr.errorsCount());

		final String error = vr.getErrors().get(0);
		Assert.assertNotNull(error);
		Assert.assertEquals("Le texte de la remarque ne doit pas être vide.", error);
	}

	@Test
	public void testRemarqueNonAnnuleeAvecTextePleinDeVide() throws Exception {
		final Remarque remarque = new Remarque();
		remarque.setAnnule(false);
		remarque.setTexte(" \t\n  \n");

		final ValidationResults vr = validate(remarque);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(1, vr.errorsCount());

		final String error = vr.getErrors().get(0);
		Assert.assertNotNull(error);
		Assert.assertEquals("Le texte de la remarque ne doit pas être vide.", error);
	}

	@Test
	public void testRemarqueNonAnnuleeAvecTexteCorrect() throws Exception {
		final Remarque remarque = new Remarque();
		remarque.setAnnule(false);
		remarque.setTexte("Ceci est ma remarque.");

		final ValidationResults vr = validate(remarque);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(0, vr.errorsCount());
	}
}
