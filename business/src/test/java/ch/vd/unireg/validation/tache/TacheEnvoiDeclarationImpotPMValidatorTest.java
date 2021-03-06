package ch.vd.unireg.validation.tache;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class TacheEnvoiDeclarationImpotPMValidatorTest extends AbstractValidatorTest<TacheEnvoiDeclarationImpotPM> {

	@Override
	protected String getValidatorBeanName() {
		return "tacheEnvoiDeclarationImpotPMValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTacheAnnulee() {

		final int annee = RegDate.get().year() - 1;
		final RegDate debut = RegDate.get(annee, 1, 1);
		final RegDate fin = RegDate.get(annee, 12, 31);
		final TacheEnvoiDeclarationImpotPM tache = new TacheEnvoiDeclarationImpotPM(TypeEtatTache.EN_INSTANCE, RegDate.get(), null, debut, fin, debut, fin, TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_PM_BATCH, null, null);

		// Adresse invalide (type contribuable nul) mais annulée => pas d'erreur
		{
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setTypeDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH);
			tache.setContribuable(new Entreprise());
			tache.setCollectiviteAdministrativeAssignee(new CollectiviteAdministrative());
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}
	}
}