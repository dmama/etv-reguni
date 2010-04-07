package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements de suppression de nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteHandler extends AnnulationPermisCOuNationaliteSuisseHandler {

	@Override
	public void checkCompleteness(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// rien à faire
	}

	@Override
	protected void validateSpecific(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// rien à faire
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.annulationpermis.AnnulationPermisCOuNationaliteSuisseHandler#handle(ch.vd.uniregctb.evenement.EvenementCivil, java.util.List)
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		
		final SuppressionNationalite suppressionNationalite = (SuppressionNationalite) evenement;
		switch (suppressionNationalite.getType()) {
		case SUP_NATIONALITE_SUISSE:
			return super.handle(suppressionNationalite, warnings);

		case SUP_NATIONALITE_NON_SUISSE:
			/* Seul l'obtention de nationalité suisse est traitée */
			break;
		default:
			Assert.fail();
		}
		return null;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new SuppressionNationaliteAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.SUP_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE);
		return types;
	}

}
