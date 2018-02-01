package ch.vd.unireg.validation.declaration;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EtatDeclarationSommeeValidatorTest extends AbstractValidatorTest<EtatDeclarationSommee>{

	@Override
	protected String getValidatorBeanName() {
		return "etatDeclarationSommeeValidator";
	}

		@Test
		@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
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
