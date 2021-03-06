package ch.vd.unireg.taglibs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public final class JspTagDatedInfraBean extends JspTagDatedInfra<Object> {

	public JspTagDatedInfraBean() {
		super(Object.class);
	}

	@Override
	protected Object getInstance(ServiceInfrastructureService infraService, Integer noOfs, RegDate date) throws InfrastructureException {
		throw new IllegalStateException("This bean should not be used directly");
	}
}
