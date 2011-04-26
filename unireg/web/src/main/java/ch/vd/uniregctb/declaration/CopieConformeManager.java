package ch.vd.uniregctb.declaration;

import java.io.InputStream;

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
	InputStream getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException;

}
