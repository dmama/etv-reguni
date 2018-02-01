package ch.vd.unireg.validation.tache;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class TacheAnnulationDeclarationImpotValidatorTest extends AbstractValidatorTest<TacheAnnulationDeclarationImpot> {

	@Override
	protected String getValidatorBeanName() {
		return "tacheAnnulationDeclarationImpotValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTacheAnnulee() {

		final TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, RegDate.get(), null, null, null);

		// Adresse invalide (di nulle) mais annulée => pas d'erreur
		{
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setDeclaration(new DeclarationImpotOrdinairePP());
			tache.setContribuable(new PersonnePhysique());
			tache.setCollectiviteAdministrativeAssignee(new CollectiviteAdministrative());
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}
	}
}