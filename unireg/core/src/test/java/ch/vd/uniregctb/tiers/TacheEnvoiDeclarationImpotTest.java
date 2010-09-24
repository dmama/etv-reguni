package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static junit.framework.Assert.assertFalse;

public class TacheEnvoiDeclarationImpotTest extends WithoutSpringTest {

	@Test
	public void testValidateTacheAnnulee() {

		final TacheEnvoiDeclarationImpot tache = new TacheEnvoiDeclarationImpot();

		// Adresse invalide (type contribuable nul) mais annulée => pas d'erreur
		{
			tache.setTypeContribuable(null);
			tache.setAnnule(true);
			assertFalse(tache.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
			tache.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
			tache.setAnnule(true);
			assertFalse(tache.validate().hasErrors());
		}
	}
}