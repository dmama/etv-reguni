package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements de démangement vaudois.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionNationaliteHandler extends ObtentionPermisCOuNationaliteSuisseHandler {

	/**
	 * Un logger.
	 */
	//private static final Logger LOGGER =  Logger.getLogger(ObtentionNationaliteHandler.class);

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Obsolète dans cet handler, l'obtention de nationalité est un événement ne concernant qu'un seul individu.
	}

	/**
	 * @see ch.vd.uniregctb.evenement.obtentionpermis.ObtentionPermisCOuNationaliteSuisseHandler#validateSpecific(ch.vd.uniregctb.evenement.EvenementCivil, java.util.List, java.util.List)
	 */
	@Override
	public void validateSpecific(EvenementCivil evenementCivil, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		ObtentionNationalite obtentionNationalite = (ObtentionNationalite) evenementCivil;

		if (TypeEvenementCivil.NATIONALITE_SUISSE.equals(obtentionNationalite.getType())) {
			super.validateSpecific(obtentionNationalite, erreurs, warnings);
		}
		else {
			Audit.info(obtentionNationalite.getNumeroEvenement(), "Nationalité non suisse : ignorée");
		}
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		final ObtentionNationalite obtentionNationalite = (ObtentionNationalite) evenement;

		switch (obtentionNationalite.getType()) {
			case NATIONALITE_SUISSE:
				return super.handle(evenement, warnings);

			case NATIONALITE_NON_SUISSE:
				/* Seul l'obtention de nationalité suisse est traitée */
				Audit.info(obtentionNationalite.getNumeroEvenement(), "Nationalité non suisse : ignorée");
				break;

			default:
				Assert.fail();
		}
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil>  getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ObtentionNationaliteAdapter();
	}

}
