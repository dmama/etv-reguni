package ch.vd.uniregctb.lr.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;

public class DelaiAddView {

	private Long idListe;
	private RegDate dateDemande;
	private RegDate delaiAccorde;

	public DelaiAddView() {
	}

	public DelaiAddView(DeclarationImpotSource lr) {
		this.idListe = lr.getId();
		this.dateDemande = RegDate.get();
	}

	public Long getIdListe() {
		return idListe;
	}

	public void setIdListe(Long idListe) {
		this.idListe = idListe;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}
}
