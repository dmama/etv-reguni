package ch.vd.uniregctb.validation.tache;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class TacheAnnulationDeclarationImpotValidatorTest extends AbstractValidatorTest<TacheAnnulationDeclarationImpot> {

	@Override
	protected String getValidatorBeanName() {
		return "tacheAnnulationDeclarationImpotValidator";
	}

	@Test
	public void testValidateTacheAnnulee() {

		final TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, RegDate.get(), null, null, null);

		// Adresse invalide (di nulle) mais annulée => pas d'erreur
		{
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setDeclarationImpotOrdinaire(new DeclarationImpotOrdinaire());
			tache.setContribuable(new PersonnePhysique());
			tache.setCollectiviteAdministrativeAssignee(new CollectiviteAdministrative());
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}
	}
}