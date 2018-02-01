package ch.vd.unireg.tiers.view;

import java.util.Set;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.type.FormeJuridique;

public class AutreCommunauteCivilView {

	private String nom;
	private String ide;
	private FormeJuridique formeJuridique;

	@SuppressWarnings("UnusedDeclaration")
	public AutreCommunauteCivilView() {
	}

	public AutreCommunauteCivilView(AutreCommunaute ac) {
		this.nom = ac.getNom();
		this.formeJuridique = ac.getFormeJuridique();

		final Set<IdentificationEntreprise> ies = ac.getIdentificationsEntreprise();
		if (ies != null && !ies.isEmpty()) {
			// je prends le premier...
			this.ide = FormatNumeroHelper.formatNumIDE(ies.iterator().next().getNumeroIde());
		}
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getIde() {
		return ide;
	}

	public void setIde(String ide) {
		this.ide = ide;
	}

	public FormeJuridique getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridique formeJuridique) {
		this.formeJuridique = formeJuridique;
	}
}
