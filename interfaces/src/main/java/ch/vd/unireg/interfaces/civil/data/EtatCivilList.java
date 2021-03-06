package ch.vd.unireg.interfaces.civil.data;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

public interface EtatCivilList {

	/**
	 * Détermine et retourne l'état-civil valide à la date spécifiée.
	 *
	 * @param date la date de validité de l'état-civil souhaité; ou <b>null</b> pour obtenir le dernier état-civil.
	 * @return un état-civil; ou <b>null</b> si aucun état-civil n'existe.
	 */
	EtatCivil getEtatCivilAt(RegDate date);

	List<EtatCivil> asList();
}
