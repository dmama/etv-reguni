package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DemenagementSiegeView {

	private long idEntreprise;
	private RegDate dateDebutNouveauSiege;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;

	public DemenagementSiegeView() {
	}

	public DemenagementSiegeView(long idEntreprise) {
		this.idEntreprise = idEntreprise;
		this.typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	public long getIdEntreprise() {
		return idEntreprise;
	}

	public void setIdEntreprise(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public RegDate getDateDebutNouveauSiege() {
		return dateDebutNouveauSiege;
	}

	public void setDateDebutNouveauSiege(RegDate dateDebutNouveauSiege) {
		this.dateDebutNouveauSiege = dateDebutNouveauSiege;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public void setNoAutoriteFiscale(Integer noAutoriteFiscale) {
		this.noAutoriteFiscale = noAutoriteFiscale;
	}
}
