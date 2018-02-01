package ch.vd.uniregctb.lr.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;

public class ListeRecapitulativeAddView implements DateRange {

	private Long idDebiteur;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegDate delaiAccorde;

	public ListeRecapitulativeAddView() {
	}

	public ListeRecapitulativeAddView(DebiteurPrestationImposable dpi, DateRange range, RegDate delaiAccorde) {
		this.idDebiteur = dpi.getNumero();
		this.dateDebut = range.getDateDebut();
		this.dateFin = range.getDateFin();
		this.delaiAccorde = delaiAccorde;
	}

	public Long getIdDebiteur() {
		return idDebiteur;
	}

	public void setIdDebiteur(Long idDebiteur) {
		this.idDebiteur = idDebiteur;
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

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}
	
	public Long getIdListe() {
		return null;
	}

	public boolean isImprimable() {
		return dateDebut != null && dateFin != null;
	}
}
