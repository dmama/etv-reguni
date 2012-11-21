package ch.vd.uniregctb.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("UnusedDeclaration")
public class EditForDebiteurView implements EditForView {

	private long id;
	private long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;
	private boolean dateDebutEditable;
	private boolean dateFinEditable;

	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;

	public EditForDebiteurView() {
	}

	public EditForDebiteurView(ForDebiteurPrestationImposable fdpi) {
		this.id = fdpi.getId();
		this.tiersId = fdpi.getTiers().getId();
		this.dateDebut = fdpi.getDateDebut();
		this.dateFin = fdpi.getDateFin();
		this.dateDebutEditable = fdpi.getDateDebut() == null;
		this.dateFinEditable = fdpi.getDateFin() == null || fdpi.getDateFin().isAfter(RegDate.get());
		this.typeAutoriteFiscale = fdpi.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = fdpi.getNumeroOfsAutoriteFiscale();
	}

	public void initReadOnlyData(ForDebiteurPrestationImposable fdpi) {
		this.id = fdpi.getId();
		this.tiersId = fdpi.getTiers().getId();
		this.dateDebutEditable = fdpi.getDateDebut() == null;
		this.dateFinEditable = fdpi.getDateFin() == null || fdpi.getDateFin().isAfter(RegDate.get());
		this.typeAutoriteFiscale = fdpi.getTypeAutoriteFiscale();
		this.noAutoriteFiscale = fdpi.getNumeroOfsAutoriteFiscale();
	}

	@Override
	public long getId() {
		return id;
	}

	public void setId(long id) {
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

	public boolean isDateDebutEditable() {
		return dateDebutEditable;
	}

	public boolean isDateFinEditable() {
		return dateFinEditable;
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
