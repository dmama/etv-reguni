package ch.vd.uniregctb.validation.tache;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class TacheEnvoiDeclarationImpotPPValidatorTest extends AbstractValidatorTest<TacheEnvoiDeclarationImpotPP> {

	@Override
	protected String getValidatorBeanName() {
		return "tacheEnvoiDeclarationImpotPPValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTacheAnnulee() {

		final int annee = RegDate.get().year() - 1;
		final RegDate debut = RegDate.get(annee, 1, 1);
		final RegDate fin = RegDate.get(annee, 12, 31);
		final TacheEnvoiDeclarationImpotPP tache = new TacheEnvoiDeclarationImpotPP(TypeEtatTache.EN_INSTANCE, RegDate.get(), null, debut, fin, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, null, null, TypeAdresseRetour.CEDI, null);

		// Adresse invalide (type contribuable nul) mais annulée => pas d'erreur
		{
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
			tache.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
			tache.setContribuable(new PersonnePhysique());
			tache.setCollectiviteAdministrativeAssignee(new CollectiviteAdministrative());
			tache.setAnnule(true);
			assertFalse(validate(tache).hasErrors());
		}
	}
}