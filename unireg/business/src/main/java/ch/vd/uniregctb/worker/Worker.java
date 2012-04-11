package ch.vd.uniregctb.worker;

public interface Worker<T> {

	/**
	 * @return le nom du worker. Utilisé pour préfixer le nom des threads instanciés.
	 */
	String getName();
}
