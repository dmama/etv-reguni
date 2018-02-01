package ch.vd.unireg.lr.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class ListeRecapitulativeView implements Annulable, DateRange {

	private final long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateRetour;
	private final RegDate delaiAccorde;
	private final TypeEtatDocumentFiscal etat;

	public ListeRecapitulativeView(DeclarationImpotSource lr) {
		this.id = lr.getId();
		this.annule = lr.isAnnule();
		this.dateDebut = lr.getDateDebut();
		this.dateFin = lr.getDateFin();
		this.dateRetour = lr.getDateRetour();
		this.delaiAccorde = lr.getDelaiAccordeAu();
		this.etat = lr.getDernierEtatDeclaration().getEtat();
	}

	public long getId() {
		return id;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public TypeEtatDocumentFiscal getEtat() {
		return etat;
	}
}
