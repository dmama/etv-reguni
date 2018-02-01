package ch.vd.uniregctb.taglibs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class JspTagLocalite extends JspTagDatedInfra<Localite> {

	public JspTagLocalite() {
		super(Localite.class);
	}

	@Override
	protected Localite getInstance(ServiceInfrastructureService infraService, Integer noOrdreP, RegDate date) throws ServiceInfrastructureException {
		return infraService.getLocaliteByONRP(noOrdreP, date);
	}
}
