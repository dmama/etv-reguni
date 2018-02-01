package ch.vd.unireg.validation.tache;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class TacheAnnulationQuestionnaireSNCValidatorTest extends AbstractValidatorTest<TacheAnnulationQuestionnaireSNC> {

	@Override
	protected String getValidatorBeanName() {
		return "tacheAnnulationQuestionnaireSNCValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTacheAnnulee() {

		final TacheAnnulationQuestionnaireSNC tache = new TacheAnnulationQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, RegDate.get(), null, null, null);

		// Tâche invalide (déclaration nulle) mais annulée => pas d'erreur
		{
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}

		// Tâche valide et annulée => pas d'erreur
		{
			tache.setDeclaration(new QuestionnaireSNC());
			tache.setContribuable(new Entreprise());
			tache.setCollectiviteAdministrativeAssignee(new CollectiviteAdministrative());
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}
	}
}