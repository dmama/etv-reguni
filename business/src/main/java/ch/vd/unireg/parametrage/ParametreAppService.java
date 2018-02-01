package ch.vd.uniregctb.parametrage;

public interface ParametreAppService extends ParametreAppAccessor {

	/**
	 * Remise à la valeur par défaut pour tous les paramètres
	 */
	void reset();

	/**
	 * Sauvegarde en base de tous les paramètres
	 */
	void save();

}
