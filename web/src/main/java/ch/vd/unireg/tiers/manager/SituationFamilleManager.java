package ch.vd.uniregctb.tiers.manager;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;


/**
 * Service à disposition du controller SituationFamilleController
 *
 * @author xcifde
 *
 */
public interface SituationFamilleManager {

	/**
	 * Annule une situation de famille
	 *
	 * @param idSituationFamille
	 */
	void annulerSituationFamille(Long idSituationFamille);

	/**
	 * Cree une nouvelle vue SituationFamilleView
	 *
	 * @param numeroCtb
	 * @return
	 * @throws AdressesResolutionException
	 */
	SituationFamilleView create(Long numeroCtb) throws AdresseException;

	/**
	 * Sauvegarde de la situation de famille
	 *
	 * @param situationFamilleView
	 */
	void save(SituationFamilleView situationFamilleView) ;

	/**
	 * @param situationId l'id d'une situation de famille
	 * @return le contribuable associé à la situation de famille; ou <b>null</b> si la situation de famille n'existe pas.
	 */
	@Nullable
	Contribuable getContribuableForSituation(long situationId);
}
