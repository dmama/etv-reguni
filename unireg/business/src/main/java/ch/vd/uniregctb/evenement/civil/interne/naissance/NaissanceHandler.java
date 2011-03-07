package ch.vd.uniregctb.evenement.civil.interne.naissance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règle métiers permettant de traiter les événements de naissance.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class NaissanceHandler extends EvenementCivilHandlerBase {

	/**
	 * Vérifie que les informations sont complètes.
	 * Pour un événement de déménagement, de séparation  ou de divorce (sans séparation préalable),
	 * si l’individu est marié avec un autre individu présent dans le registre des individus,
	 * l’application recherche l’événement correspondant de l’autre membre du couple.
	 * Si ce 2e événement n’est pas trouvé, le 1er est mis en attente (en cas de déménagement d’un seul membre du couple,
	 * il y a suspicion de séparation fiscale).
	 * Les éventuels événements manquants de déménagement des enfants sont ignorés.
	 */
	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	/**
	 * @see ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandler#validate(java.lang.Object, java.util.List)
	 */
	@Override
	protected void validateSpecific(EvenementCivilInterne evenementCivil, List<EvenementCivilExterneErreur> errors, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 * @throws EvenementCivilHandlerException
	 */
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenementCivil, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		throw new NotImplementedException();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.NAISSANCE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new NaissanceAdapter(event, context);
	}

}
