package ch.vd.unireg.evenement.entreprise;

import ch.vd.unireg.evenement.entreprise.engine.translator.NiveauCappingEtat;

/**
 * Interface en lecture seule sur le niveau de capping des traitements des événements entreprise
 */
public interface EvenementEntrepriseCappingLevelProvider {

	/**
	 * @return le niveau actuel de capping
	 */
	NiveauCappingEtat getNiveauCapping();
}
