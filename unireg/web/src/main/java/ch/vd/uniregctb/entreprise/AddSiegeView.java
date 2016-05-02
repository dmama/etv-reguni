package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AddSiegeView implements DateRange {

	private Long etablissementId;
	private Long entrepriseId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;
	private String nomAutoriteFiscale;

	public AddSiegeView() {
	}

	public AddSiegeView(Long etablissementId, Long entrepriseId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, String nomAutoriteFiscale) {
		this.etablissementId = etablissementId;
		this.entrepriseId = entrepriseId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noAutoriteFiscale = noAutoriteFiscale;
		this.nomAutoriteFiscale = nomAutoriteFiscale;
	}

	public Long getEtablissementId() {
		return etablissementId;
	}

	public void setEtablissementId(Long etablissementId) {
		this.etablissementId = etablissementId;
	}

	public Long getEntrepriseId() {
		return entrepriseId;
	}

	public void setEntrepriseId(Long entrepriseId) {
		this.entrepriseId = entrepriseId;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
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

	public String getNomAutoriteFiscale() {
		return nomAutoriteFiscale;
	}

	public void setNomAutoriteFiscale(String nomAutoriteFiscale) {
		this.nomAutoriteFiscale = nomAutoriteFiscale;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

}
