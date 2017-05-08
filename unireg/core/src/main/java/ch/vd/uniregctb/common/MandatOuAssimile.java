package ch.vd.uniregctb.common;

import ch.vd.uniregctb.type.TypeMandat;

/**
 * Interface implémentée par les entités qui ont les caractéristiques d'un mandat (= liaison particulière entre un mandant et un mandataire)
 */
public interface MandatOuAssimile {

	/**
	 * @return le type de mandat
	 */
	TypeMandat getTypeMandat();

	/**
	 * @return le code du genre d'impôt concerné (non-null pour les mandats de type {@link TypeMandat#SPECIAL})
	 */
	String getCodeGenreImpot();
}
