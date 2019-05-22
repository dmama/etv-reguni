package ch.vd.unireg.taglibs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class JspTagLocalite extends JspTagDatedInfra<Localite> {

	public JspTagLocalite() {
		super(Localite.class);
	}

	@Override
	protected Localite getInstance(ServiceInfrastructureService infraService, Integer noOrdreP, RegDate date) throws InfrastructureException {
		return infraService.getLocaliteByONRP(noOrdreP, date);
	}
}
