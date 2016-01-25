package ch.vd.uniregctb.tiers.view;

import java.util.Set;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class ContribuableInfosEntrepriseView {
	private String ide;

	public ContribuableInfosEntrepriseView() {
	}

	public ContribuableInfosEntrepriseView(PersonnePhysique pp) {
		final Set<IdentificationEntreprise> ies = pp.getIdentificationsEntreprise();
		if (ies != null && !ies.isEmpty()) {
			// je prends le premier...
			final String identifiant = FormatNumeroHelper.formatNumIDE(ies.iterator().next().getNumeroIde());
			ide = identifiant;
		}

	}


	public String getIde() {
		return ide;
	}

	public void setIde(String ide) {
		this.ide = ide;
	}
}
