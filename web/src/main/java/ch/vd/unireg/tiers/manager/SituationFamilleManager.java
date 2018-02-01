package ch.vd.unireg.tiers.manager;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.view.SituationFamilleView;


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
