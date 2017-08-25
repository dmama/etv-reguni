package ch.vd.uniregctb.lr.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapitulativeView implements Annulable, DateRange {

	private final long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateRetour;
	private final RegDate delaiAccorde;
	private final TypeEtatDeclaration etat;

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

	public TypeEtatDeclaration getEtat() {
		return etat;
	}
}
