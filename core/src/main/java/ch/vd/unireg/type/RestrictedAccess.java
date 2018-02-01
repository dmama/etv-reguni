package ch.vd.unireg.type;

/**
 * Interface qui permet d'afficher de manière très visible le fait que certaines valeurs
 * énumérées ne sont pas utilisables (elles ont été créées par exemple en avance de phase
 * pour sortir un WS qui restera compatible plus longtemps mais ne doivent pas encore être
 * utilisées pour le moment)
 */
public interface RestrictedAccess {

	/**
	 * @return <code>true</code> si la valeur est utilisable
	 */
	boolean isAllowed();
}
