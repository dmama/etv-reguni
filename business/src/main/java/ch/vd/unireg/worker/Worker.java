package ch.vd.unireg.worker;

public interface Worker {

	/**
	 * @return le nom du worker. Utilisé pour préfixer le nom des threads instanciés.
	 */
	String getName();
}
