package ch.vd.unireg.interfaces.entreprise.data.builder;

public interface DataBuilder<T> {

	/**
	 * Instancie l'objet avec les données préparées.
	 * @return une instance de l'objet construit.
	 */
	T build();
}
