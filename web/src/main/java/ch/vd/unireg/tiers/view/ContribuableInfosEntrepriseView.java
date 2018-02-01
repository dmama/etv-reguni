package ch.vd.unireg.tiers.view;

import java.util.Set;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.PersonnePhysique;

public class ContribuableInfosEntrepriseView {
	private String ide;

	public ContribuableInfosEntrepriseView() {
	}

	public ContribuableInfosEntrepriseView(PersonnePhysique pp) {
		final Set<IdentificationEntreprise> ies = pp.getIdentificationsEntreprise();
		setIde(ies);
	}
	public ContribuableInfosEntrepriseView(Entreprise entreprise) {
		final Set<IdentificationEntreprise> ies = entreprise.getIdentificationsEntreprise();
		setIde(ies);
	}
	public ContribuableInfosEntrepriseView(Etablissement etablissement) {
		final Set<IdentificationEntreprise> ies = etablissement.getIdentificationsEntreprise();
		setIde(ies);
	}

	protected void setIde(Set<IdentificationEntreprise> ies) {
		if (ies != null && !ies.isEmpty()) {
			// je prends le premier...
			ide = FormatNumeroHelper.formatNumIDE(ies.iterator().next().getNumeroIde());
		}
	}

	public String getIde() {
		return ide;
	}

	public void setIde(String ide) {
		this.ide = ide;
	}
}
