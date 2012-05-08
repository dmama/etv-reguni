package ch.vd.uniregctb.interfaces.model.mock;

public interface MockCloneable extends Cloneable {

	/**
	 * Déclaration de la méthode clone comme publique
	 * @return un clone de l'objet
	 */
	Object clone() throws CloneNotSupportedException;
}
