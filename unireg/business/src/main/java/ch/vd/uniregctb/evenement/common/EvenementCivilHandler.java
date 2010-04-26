package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface EvenementCivilHandler {

	/**
	 * Valide que toutes les événements civils unitaires composant l'événement regroupé sont bien présents.
	 *
	 * @param target
	 * @param erreurs
	 */
	void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Crée une Adapter valide pour ce Handler
	 *
	 * @return
	 */
	GenericEvenementAdapter createAdapter();

	/**
	 * Validation spécifique de l'objet target passé en paramètre.
	 *
	 * @param target
	 * @param erreurs
	 */
	void validate(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Traite l'événement passé en paramètre.
	 * @return une pair contenant les habitants créés par cet événement (respectivement le principal et le conjoint), ou <code>null</code> si aucun n'a été nouvellement créé (ou passé habitant)
	 */
	Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException;

	void setRegistrar(EvenementHandlerRegistrar registrar);
}
