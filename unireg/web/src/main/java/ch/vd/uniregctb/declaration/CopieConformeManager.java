package ch.vd.uniregctb.declaration;

import ch.vd.uniregctb.editique.EditiqueException;

/**
 * Manager des copies conformes autour des déclarations
 */
public interface CopieConformeManager {

	/**
	 * Renvoie un document PDF de la copie conforme de la sommation de déclaration
	 * dont l'état (de type 'SOMME') est indiqué en paramètre
	 * @return document PDF
	 */
	byte[] getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException;

}
