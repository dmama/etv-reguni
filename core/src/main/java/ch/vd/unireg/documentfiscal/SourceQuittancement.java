package ch.vd.unireg.documentfiscal;

/**
 * [SIFISC-1782] La source du quittancement de la déclaration (CEDI, ADDI ou manuel).
 *
 * @author Raphaël Marmier, 2017-12-08, <raphael.marmier@vd.ch>
 */
public interface SourceQuittancement {

	/**
	 * Nom de la source en cas de quittance à travers l'interface web d'Unireg.
	 */
	public static final String SOURCE_WEB = "WEB";

	/**
	 * Nom de la source en cas de quittance automatique des DIs des contribuables indigents.
	 */
	public static final String SOURCE_INDIGENT = "INDIGENT";

	/**
	 * Nom de la source en cas de quittance par le web-service lorsque la source n'est pas explicitement spécifiée (= anciens clients).
	 */
	public static final String SOURCE_CEDI = "CEDI";

	/**
	 * @return la source de quittancement
	 */
	String getSource();

	/**
	 * @param source la source de quittancement
	 */
	void setSource(String source);
}