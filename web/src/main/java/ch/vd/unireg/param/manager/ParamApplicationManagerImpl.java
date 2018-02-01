package ch.vd.unireg.param.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.param.view.ParamApplicationView;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.parametrage.ParametreEnum;


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
	@Override
	@Transactional(readOnly = true)
	public ParamApplicationView getForm() {
		return new ParamApplicationView(service);
	}


	/* (non-Javadoc)
	 * @see ch.vd.unireg.param.manager.ParamApplicationManager#reset()
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reset() {
		assert service != null;
		service.reset();
	}

	/* (non-Javadoc)
	 * @see ch.vd.unireg.param.manager.ParamApplicationManager#save(ch.vd.unireg.param.view.ParamApplicationForm)
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(ParamApplicationView form) {
		assert this.service != null;
		form.saveTo(service);
		service.save();
	}

	/* (non-Javadoc)
	 * @see ch.vd.unireg.param.manager.ParamApplicationManager#getDefaut(ch.vd.unireg.parametrage.ParametreEnum)
	 */
	@Override
	@Transactional(readOnly = true)
	public String getDefaut(ParametreEnum param) {
		return service.getDefaut(param);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setService(ParametreAppService service) {
		this.service = service;
	}
}
