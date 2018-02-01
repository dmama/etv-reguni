package ch.vd.unireg.fourreNeutre;

import ch.vd.unireg.editique.EditiqueResultat;

public interface FourreNeutreService {
	/**
	 * Envoie une demande d'impression de fourre neutre à l'édituque pour un contribuable
	 * @param tiersId
	 * @param periodeFiscale
	 * @return
	 * @throws FourreNeutreException
	 */
	EditiqueResultat imprimerFourreNeutre(long tiersId, int periodeFiscale) throws FourreNeutreException;

	Integer getPremierePeriodeSelonType(long tiersId);

	/**
	 * Indique si le iters dont l'identifiant est passé en paramètre appartient à une population pour laquelle on peut emettre une fourre neutre
	 * @param tiersId le numéro du tiers à analyser
	 * @return true si on peut emettre une fourre neutre sur ce tiers, false si non
	 */
	boolean isAutorisePourFourreNeutre(long tiersId);
}
