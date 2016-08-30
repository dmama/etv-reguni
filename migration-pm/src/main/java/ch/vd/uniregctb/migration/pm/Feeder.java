package ch.vd.uniregctb.migration.pm;

/**
 * Interface implémentée par les entités capable de produire des Graphes d'objets
 */
public interface Feeder {

	/**
	 * @param worker consommateur à qui fournir les graphes d'objets
	 * @throws Exception en cas de problème
	 */
	void feed(Worker worker) throws Exception;
}
