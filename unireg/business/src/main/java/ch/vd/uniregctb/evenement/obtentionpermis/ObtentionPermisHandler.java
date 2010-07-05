package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements d'obtention de permis.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionPermisHandler extends ObtentionPermisCOuNationaliteSuisseHandler {

	//private static final Logger LOGGER =  Logger.getLogger(ObtentionPermisHandler.class);

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Obsolète dans cet handler, l'obtention de permis est un événement ne concernant qu'un seul individu.
	}

	@Override
	public void validateSpecific(EvenementCivil evenementCivil, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		ObtentionPermis obtentionPermis = (ObtentionPermis) evenementCivil;

		/* Seul le permis C a une influence */
		if (obtentionPermis.getTypePermis() != EnumTypePermis.ETABLLISSEMENT) {
			Audit.info(obtentionPermis.getNumeroEvenement(), "Permis non C : ignoré");
			return;
		}

		super.validateSpecific(evenementCivil, erreurs, warnings);
	}

	/**
	 * Traite l'événement passé en paramètre.
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		ObtentionPermis obtentionPermis = (ObtentionPermis) evenement;

		/* Seul le permis C a une influence */
		if (obtentionPermis.getTypePermis() != EnumTypePermis.ETABLLISSEMENT) {
			Audit.info(obtentionPermis.getNumeroEvenement(), "Permis non C : ignoré");
			return null;
		}
		else {
			return super.handle(evenement, warnings);
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ObtentionPermisAdapter();
	}

}
