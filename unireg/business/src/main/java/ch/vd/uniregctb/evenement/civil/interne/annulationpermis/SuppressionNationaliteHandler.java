package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements de suppression de nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteHandler extends AnnulationPermisCOuNationaliteSuisseHandler {

	public void checkCompleteness(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// rien à faire
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// rien à faire
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.annulationpermis.AnnulationPermisCOuNationaliteSuisseHandler#handle(ch.vd.uniregctb.evenement.EvenementCivilInterne, java.util.List)
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		
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
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new SuppressionNationaliteAdapter(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.SUP_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE);
		return types;
	}

}
