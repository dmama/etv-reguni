package ch.vd.unireg.evenement.organisation;

import ch.vd.unireg.evenement.organisation.engine.translator.NiveauCappingEtat;

/**
 * Interface en lecture seule sur le niveau de capping des traitements des événements entreprise
 */
public interface EvenementEntrepriseCappingLevelProvider {

	/**
	 * @return le niveau actuel de capping
	 */
	NiveauCappingEtat getNiveauCapping();
}
