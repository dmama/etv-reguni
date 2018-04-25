package ch.vd.unireg.validation.complements;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class CoordonneesFinancieresValidatorTest extends AbstractValidatorTest<CoordonneesFinancieres> {

	@Override
	protected String getValidatorBeanName() {
		return "coordonneesFinancieresValidator";
	}

	@Test
	public void testValidationEtAnnulation() throws Exception {
		final CoordonneesFinancieres coords = new CoordonneesFinancieres();

		// donnée invalide (manque le code et le libellé) mais annulée -> pas d'erreur
		{
			coords.setTitulaire(null);
			coords.setCompteBancaire(null);
			coords.setAnnule(true);
			Assert.assertFalse(validate(coords).hasErrors());
		}

		// donnée invalide (manque le code et le libellé) et non-annulée -> erreur
		{
			coords.setTitulaire(null);
			coords.setCompteBancaire(null);
			coords.setAnnule(false);
			Assert.assertTrue(validate(coords).hasErrors());
		}
	}

	@Test
	public void testAucuneDonnee() throws Exception {
		final CoordonneesFinancieres coords = new CoordonneesFinancieres();

		// invalide sans aucune donnée
		{
			coords.setTitulaire(null);
			coords.setCompteBancaire(null);
			assertValidation(Collections.singletonList("Au minimum le titulaire, l'IBAN ou le code BicSwift doit être renseigné."), null, validate(coords));
		}

		// valide avec un titulaire
		{
			coords.setTitulaire("titulaire");
			coords.setCompteBancaire(null);
			Assert.assertFalse(validate(coords).hasErrors());
		}

		// valide avec un iban
		{
			coords.setTitulaire(null);
			coords.setCompteBancaire(new CompteBancaire("iban", null));
			Assert.assertFalse(validate(coords).hasErrors());
		}

		// valide avec un bicswift
		{
			coords.setTitulaire(null);
			coords.setCompteBancaire(new CompteBancaire(null, "bicswift"));
			Assert.assertFalse(validate(coords).hasErrors());
		}
	}
}
