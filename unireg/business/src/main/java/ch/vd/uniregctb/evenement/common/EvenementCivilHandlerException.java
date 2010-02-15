/**
 * 
 */
package ch.vd.uniregctb.evenement.common;

/**
 * Exception lancée par un gestionnaire d'événement lors d'une erreur.
 * 
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 * 
 */
public class EvenementCivilHandlerException extends RuntimeException {

	/**
	 * @param string
	 */
	public EvenementCivilHandlerException(String string) {
		super(string);
	}

	/**
	 * @param string
	 */
	public EvenementCivilHandlerException(String string, Throwable t) {
		super(string, t);
	}
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2078222377582034L;
}
