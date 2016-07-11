package ch.vd.uniregctb.taglibs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public final class JspTagDatedInfraBean extends JspTagDatedInfra<Object> {

	public JspTagDatedInfraBean() {
		super(Object.class);
	}

	@Override
	protected Object getInstance(ServiceInfrastructureService infraService, Integer noOfs, RegDate date) throws ServiceInfrastructureException {
		throw new IllegalStateException("This bean should not be used directly");
	}
}
