package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface EvenementCivilHandler {

	/**
	 * Valide que toutes les données nécessaires sur l'événement sont bien présentes.
	 *
	 * @param target
	 * @param erreurs
	 */
	void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Crée une Adapter valide pour ce Handler
	 *
	 *
	 *
	 * @param event
	 * @param context le context d'exécution de l'événement civil
	 * @return un événement civil interne qui corresponds à l'événement civil externe reçu
	 */
	GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException;

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
