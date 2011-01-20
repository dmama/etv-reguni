package ch.vd.uniregctb.validation.declaration;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class EtatDeclarationSommeeValidatorTest extends AbstractValidatorTest<EtatDeclarationSommee>{

	@Override
	protected String getValidatorBeanName() {
		return "etatDeclarationSommeeValidator";
	}

		@Test
	public void testValidateEtatAnnule() {

		final EtatDeclarationSommee etatSomme = new EtatDeclarationSommee();

		// Etat invalide car sans date d'obtention et date d'envoi mais pas d'erreur car annule
		{
			etatSomme.setAnnule(true);
			assertFalse(validate(etatSomme).hasErrors());
		}

		// etat valide et annulé => pas d'erreur
		{
			etatSomme.setDateObtention(date(2008,8,8));
			etatSomme.setDateEnvoiCourrier(date(2008,8,11));
			etatSomme.setAnnule(true);
			assertFalse(validate(etatSomme).hasErrors());
		}
	}

	@Test
	public void testValidateDateObtentionDateEnvoi() {

		final EtatDeclarationSommee etatSomme = new EtatDeclarationSommee();


		// Date d'obtention et d'envoi nulle
		{
			final ValidationResults results = validate(etatSomme);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(2, errors.size());
			assertEquals("L'etat sommée de la déclaration possède une date d'obtention nulle", errors.get(0));
			assertEquals("L'etat sommé le  de la déclaration possède une date d'envoi de courrier nulle", errors.get(1));
		}

		// Date d'obtention renseignée
		{
			etatSomme.setDateObtention(RegDate.get(2000, 7, 1));
			final ValidationResults results = validate(etatSomme);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("L'etat sommé le 01.07.2000 de la déclaration possède une date d'envoi de courrier nulle", errors.get(0));
		}


		// Date d'envoi renseignée

		{
		   	etatSomme.setDateEnvoiCourrier(date(2008,7,4));
			assertFalse(validate(etatSomme).hasErrors());
		}


	}
}
