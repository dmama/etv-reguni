package ch.vd.uniregctb.validation.tache;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class TacheEnvoiDeclarationImpotValidatorTest extends AbstractValidatorTest<TacheEnvoiDeclarationImpot> {

	@Override
	protected String getValidatorBeanName() {
		return "tacheEnvoiDeclarationImpotValidator";
	}

	@Test
	public void testValidateTacheAnnulee() {

		final int annee = RegDate.get().year() - 1;
		final RegDate debut = RegDate.get(annee, 1, 1);
		final RegDate fin = RegDate.get(annee, 12, 31);
		final TacheEnvoiDeclarationImpot tache = new TacheEnvoiDeclarationImpot(TypeEtatTache.EN_INSTANCE, RegDate.get(), null, debut, fin, null, null, null, TypeAdresseRetour.CEDI, null);

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