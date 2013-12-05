package ch.vd.uniregctb.taglibs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class JspTagPays extends JspTagDatedInfra<Pays> {

	public JspTagPays() {
		super(Pays.class);
	}

	@Override
	protected Pays getInstance(ServiceInfrastructureService infraService, Integer noOfs, RegDate date) throws ServiceInfrastructureException {
		return infraService.getPays(noOfs, date);
	}
}
