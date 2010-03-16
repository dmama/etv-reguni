package ch.vd.uniregctb.param.manager;

import ch.vd.uniregctb.param.view.ParamApplicationView;
import ch.vd.uniregctb.parametrage.ParametreEnum;

public interface ParamApplicationManager {
	
	/**
	 * Permet de récuperer le formBackingObject pour la vue
	 */
	ParamApplicationView getForm();
	
	/**
	 * Sauvegarde les valeurs du formulaire
	 * 
	 * @param form le formBackingObject contenant les valeurs des paramétres à sauver
	 */
	void save(ParamApplicationView form);
	
	/**
	 * Remet les paramètres à leur valeur initiales.
	 */
	void reset();

	/**
	 * Retrouve la valeur par défaut d'un paramètre
	 * 
	 * @param param
	 * @return
	 */
	String getDefaut(ParametreEnum param);
	
	
	
	
}
