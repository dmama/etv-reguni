package ch.vd.unireg.evenement.externe;

import org.jetbrains.annotations.NotNull;

/**
 * Interface des connecteurs qui permettent de transformer un message XML entrant en un "événement externe"
 * @param <T> classe de la structure qui décrit l'XML entrant
 */
public interface EvenementExterneConnector<T> {

	/**
	 * @param request la structure de l'XML entrant
	 * @return le nouvel "événement externe", <code>null</code> s'il n'y rien à en faire (les champs {@link EvenementExterne#message} et
	 * {@link EvenementExterne#businessId} seront remplis depuis l'appelant)
	 */
	EvenementExterne parse(T request);

	/**
	 * @return la classe de structure XML entrante traitée par ce connecteur
	 */
	Class<T> getSupportedClass();

	/**
	 * @return la XSD retenue par ce connecteur
	 */
	@NotNull
	String getRequestXSD();
}
