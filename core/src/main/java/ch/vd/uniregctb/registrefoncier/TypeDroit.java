package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;
import java.util.List;

/**
 * Enum qui expose les deux principaux types de droit.
 */
public enum TypeDroit {

	DROIT_PROPRIETE(DroitProprieteCommunauteRF.class,
	                DroitProprietePersonneMoraleRF.class,
	                DroitProprietePersonnePhysiqueRF.class),
	SERVITUDE(DroitHabitationRF.class,
	          UsufruitRF.class);

	private final List<Class<? extends DroitRF>> entityClasses;

	@SafeVarargs
	TypeDroit(Class<? extends DroitRF>... entityClasses) {
		this.entityClasses = Arrays.asList(entityClasses);
	}

	/**
	 * @return les classes concrètes des entités hibernate qui correspondent au type.
	 */
	public List<Class<? extends DroitRF>> getEntityClasses() {
		return entityClasses;
	}
}
