package ch.vd.uniregctb.migration.pm.regpm;

/**
 * Interface implémentable par les entités dont l'identifiant est un {@link Long}
 */
public interface WithLongId {

	/**
	 * @return l'identifiant de l'entité
	 */
	Long getId();
}
