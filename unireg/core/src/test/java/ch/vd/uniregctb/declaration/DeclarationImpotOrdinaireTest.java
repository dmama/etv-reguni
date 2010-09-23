package ch.vd.uniregctb.declaration;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;

public class DeclarationImpotOrdinaireTest extends WithoutSpringTest {

	@Test
	public void testValidateDeclarationAnnulee() {

		final DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();

		// Adresse invalide (numéro de séquence nul) mais annulée => pas d'erreur
		{
			declaration.setNumero(null);
			declaration.setAnnule(true);
			assertFalse(declaration.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			declaration.setPeriode(new PeriodeFiscale());
			declaration.setModeleDocument(new ModeleDocument());
			declaration.setNumero(1);
			declaration.setAnnule(true);
			assertFalse(declaration.validate().hasErrors());
		}
	}

}