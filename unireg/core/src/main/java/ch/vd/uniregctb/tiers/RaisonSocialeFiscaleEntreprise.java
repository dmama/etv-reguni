package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue(value = "RaisonSociale")
public class RaisonSocialeFiscaleEntreprise extends DonneeCivileEntreprise implements Duplicable<RaisonSocialeFiscaleEntreprise> {

	private String raisonSociale;

	/**
	 * Nécessaire pour Hibernate (et SuperGRA...)
	 */
	public RaisonSocialeFiscaleEntreprise() {
	}

	public RaisonSocialeFiscaleEntreprise(RegDate dateDebut, RegDate dateFin, String raisonSociale) {
		super(dateDebut, dateFin);
		this.raisonSociale = raisonSociale;
	}

	public RaisonSocialeFiscaleEntreprise(RaisonSocialeFiscaleEntreprise source) {
		super(source);
		this.setRaisonSociale(source.getRaisonSociale());
	}

	@Column(name = "RS_RAISON_SOCIALE", length = LengthConstants.TIERS_NOM)
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@Override
	public RaisonSocialeFiscaleEntreprise duplicate() {
		return new RaisonSocialeFiscaleEntreprise(this);
	}
}
