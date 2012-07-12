package ch.vd.unireg.interfaces.infra.mock;

public interface MockCloneable extends Cloneable {

	/**
	 * Déclaration de la méthode clone comme publique
	 * @return un clone de l'objet
	 */
	Object clone() throws CloneNotSupportedException;
}
