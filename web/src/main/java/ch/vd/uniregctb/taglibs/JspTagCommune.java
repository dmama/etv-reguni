package ch.vd.uniregctb.taglibs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Tag jsp qui permet de récupérer les éléments d'une commune (à une date donnée) depuis l'infrastructure
 */
public class JspTagCommune extends JspTagDatedInfra<Commune> {

	public JspTagCommune() {
		super(Commune.class);
	}

	@Override
	protected Commune getInstance(ServiceInfrastructureService infraService, Integer noOfs, RegDate date) throws ServiceInfrastructureException {
		return noOfs != null ? infraService.getCommuneByNumeroOfs(noOfs, date) : null;
	}
}
