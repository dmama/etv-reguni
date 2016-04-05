package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EditSiegeView implements DateRange {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Long tiersId;
	private Long entrepriseId;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;
	private boolean peutEditerDateFin;

	public EditSiegeView() {}

	public EditSiegeView(DomicileEtablissement dom, Long entrepriseId, boolean peutEditerDateFin) {
		this(dom.getId(), dom.getEtablissement().getNumero(), entrepriseId, dom.getDateDebut(), dom.getDateFin(), dom.getTypeAutoriteFiscale(), dom.getNumeroOfsAutoriteFiscale(), peutEditerDateFin);
	}

	public EditSiegeView(Long id, Long tiersId, Long entrepriseId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, boolean peutEditerDateFin) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.tiersId = tiersId;
		this.entrepriseId = entrepriseId;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noAutoriteFiscale = noAutoriteFiscale;
		this.peutEditerDateFin = peutEditerDateFin;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
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

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public boolean isPeutEditerDateFin() {
		return peutEditerDateFin;
	}

	public void setPeutEditerDateFin(boolean peutEditerDateFin) {
		this.peutEditerDateFin = peutEditerDateFin;
	}
}
