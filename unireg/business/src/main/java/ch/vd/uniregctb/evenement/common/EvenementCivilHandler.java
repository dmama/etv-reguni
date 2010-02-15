package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.engine.EvenementHandlerRegistrar;

public interface EvenementCivilHandler {

	/**
	 * Valide que toutes les événements civils unitaires composant l'événement regroupé sont bien présents.
	 *
	 * @param target
	 * @param erreurs
	 */
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Crée une Adapter valide pour ce Handler
	 *
	 * @return
	 */
	public GenericEvenementAdapter createAdapter();

	/**
	 * Validation spécifique de l'objet target passé en paramètre.
	 *
	 * @param target
	 * @param erreurs
	 */
	public abstract void validate(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Traite l'événement passé en paramètre.
	 */
	public abstract void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException;

	public void setRegistrar(EvenementHandlerRegistrar registrar);

}
