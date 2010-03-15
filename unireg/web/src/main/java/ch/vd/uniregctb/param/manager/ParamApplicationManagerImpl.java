package ch.vd.uniregctb.param.manager;

import java.lang.reflect.InvocationTargetException;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.param.view.ParamApplicationView;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.parametrage.ParametreEnum;


/**
 * Implemente {@link ParamApplicationManager}
 *
 * @author xsifnr
 *
 */
public class ParamApplicationManagerImpl implements ParamApplicationManager{

	private ParametreAppService service;

	/**
	 * @inherit
	 */
	public ParamApplicationView getForm() {
		ParamApplicationView form = new ParamApplicationView();
		ParametreEnum.copyProperties(service, form);
		return form;
	}


	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.param.manager.ParamApplicationManager#reset()
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void reset() {
		assert service != null;
		service.reset();
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.param.manager.ParamApplicationManager#save(ch.vd.uniregctb.param.view.ParamApplicationForm)
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(ParamApplicationView form) {
		assert this.service != null;
		ParametreEnum.copyProperties(form, service);
		service.save();
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.param.manager.ParamApplicationManager#getDefaut(ch.vd.uniregctb.parametrage.ParametreEnum)
	 */
	public String getDefaut(ParametreEnum param) {
		return service.getDefaut(param);
	}

	public ParametreAppService getService() {
		return service;
	}

	public void setService(ParametreAppService service) {
		this.service = service;
	}

	/**
	 * tests rapides ...
	 *
	 * @param args
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

	}

}
