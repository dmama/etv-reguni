package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

import static junit.framework.Assert.assertFalse;

public class TacheTest extends WithoutSpringTest {

	@Test
	public void testValidateTacheAnnulee() {

		final Tache tache = new Tache() {
			@Override
			public TypeTache getTypeTache() {
				throw new NotImplementedException();
			}
		};

		// Adresse invalide (collectivité nulle) mais annulée => pas d'erreur
		{
			tache.setEtat(TypeEtatTache.EN_INSTANCE);
			tache.setCollectiviteAdministrativeAssignee(null);
			tache.setAnnule(true);
			assertFalse(tache.validate().hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			tache.setCollectiviteAdministrativeAssignee(new CollectiviteAdministrative());
			tache.setAnnule(true);
			assertFalse(tache.validate().hasErrors());
		}
	}
}
