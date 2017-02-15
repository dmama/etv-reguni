package ch.vd.uniregctb.common;

import java.time.Duration;

/**
 * Interface implémentable par les entités qui possèdent une notion d'âge
 */
public interface Aged {

	/**
	 * @return âge de l'entité
	 */
	Duration getAge();
}
