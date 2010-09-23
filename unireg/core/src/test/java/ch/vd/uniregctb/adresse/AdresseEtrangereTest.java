package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;

public class AdresseEtrangereTest extends WithoutSpringTest {

	@Test
	public void testValidateAdresseAnnulee() {

		final AdresseEtrangere adresse = new AdresseEtrangere();

		// Adresse invalide (numéro ofs pays nul) mais annulée => pas d'erreur
		{
			adresse.setNumeroOfsPays(null);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setNumeroOfsPays(4321);
			adresse.setAnnule(true);
			assertFalse(adresse.validate().hasErrors());
		}
	}
}