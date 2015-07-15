package ch.vd.uniregctb.param.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.param.view.ParamApplicationView;
import ch.vd.uniregctb.parametrage.ParametreEnum;

public interface ParamApplicationManager {
	
	/**
	 * Permet de récuperer le formBackingObject pour la vue
	 */
	@Transactional(readOnly = true)
	ParamApplicationView getForm();
	
	/**
	 * Sauvegarde les valeurs du formulaire
	 * 
	 * @param form le formBackingObject contenant les valeurs des paramétres à sauver
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(ParamApplicationView form);
	
	/**
	 * Remet les paramètres à leur valeur initiales.
	 */
	@Transactional(rollbackFor = Throwable.class)
	void reset();

	/**
	 * Retrouve la valeur par défaut d'un paramètre
	 * 
	 * @param param
	 * @return
	 */
	@Transactional(readOnly = true)
	String getDefaut(ParametreEnum param);
}
