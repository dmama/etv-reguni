package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

import static junit.framework.Assert.assertFalse;

public class TacheAnnulationDeclarationImpotTest extends WithoutSpringTest {

	@Test
	public void testValidateTacheAnnulee() {

		final TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot();

		// Adresse invalide (di nulle) mais annulée => pas d'erreur
		{
			tache.setDeclarationImpotOrdinaire(null);
			tache.setAnnule(true);
			assertFalse(tache.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setDeclarationImpotOrdinaire(new DeclarationImpotOrdinaire());
			tache.setAnnule(true);
			assertFalse(tache.validate().hasErrors());
		}
	}
}