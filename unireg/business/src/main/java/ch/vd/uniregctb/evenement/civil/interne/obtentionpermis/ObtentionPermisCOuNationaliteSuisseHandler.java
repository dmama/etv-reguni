package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Règles métiers permettant de traiter les événements suivants :
 * - obtention de permis C
 * - obtention de la nationalité suisse
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public abstract class ObtentionPermisCOuNationaliteSuisseHandler extends EvenementCivilHandlerBase {

	private AdresseService adresseService;
	
	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.EvenementCivilHandler#validate(java.lang.Object,
	 *      java.util.List)
	 */
	@Override
	public void validateSpecific(EvenementCivilInterne evenementCivil, List<EvenementCivilExterneErreur> errors, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		throw new NotImplementedException();
	}
}
