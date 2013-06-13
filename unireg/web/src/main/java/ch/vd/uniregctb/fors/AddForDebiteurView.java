package ch.vd.uniregctb.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class AddForDebiteurView implements AddForView {

	private Long id;
	private long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;

	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;

	public AddForDebiteurView() {
	}

	public AddForDebiteurView(long tiersId) {
		this.tiersId = tiersId;
		this.typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public long getTiersId() {
		return tiersId;
	}

	public void setTiersId(long tiersId) {
		this.tiersId = tiersId;
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

	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	@Override
	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public void setNoAutoriteFiscale(Integer noAutoriteFiscale) {
		this.noAutoriteFiscale = noAutoriteFiscale;
	}
}
