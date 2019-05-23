package ch.vd.unireg.taglibs;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class JspTagPays extends JspTagDatedInfra<Pays> {

	public JspTagPays() {
		super(Pays.class);
	}

	@Override
	protected Pays getInstance(ServiceInfrastructureService infraService, Integer noOfs, RegDate date) throws InfrastructureException {
		if (noOfs != null) {
			if (date == null) {
				final List<Pays> candidates = infraService.getPaysHisto(noOfs);
				if (candidates == null || candidates.isEmpty()) {
					return null;
				}
				else {
					return candidates.get(candidates.size() - 1);
				}
			}
			else {
				return infraService.getPays(noOfs, date);
			}
		}
		else {
			return null;
		}
	}
}
