package ch.vd.unireg.xml.common.v2;

/**
 * Représente une élément qui peut-être annulé.
 */
public interface Cancelable {

	/**
	 * @return la date d'annulation de l'élément s'il est annulé; ou <b>null</b> si l'élément n'est pas annulé.
	 */
	Date getCancellationDate();
}
