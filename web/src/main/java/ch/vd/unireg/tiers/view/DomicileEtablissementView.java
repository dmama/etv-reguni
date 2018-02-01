package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Source;
import ch.vd.unireg.tiers.Sourced;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class DomicileEtablissementView implements Sourced<Source>, DateRange, Annulable {

	private final Long id;
	private final boolean annule;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final Integer numeroOfsAutoriteFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Source source;
	private boolean dernierElement;
	private boolean peutEditerDateFin;

	public DomicileEtablissementView(DomicileHisto domicile) {
		this.id = domicile.getId();
		this.annule = domicile.isAnnule();
		this.typeAutoriteFiscale = domicile.getTypeAutoriteFiscale();
		this.numeroOfsAutoriteFiscale = domicile.getNumeroOfsAutoriteFiscale();
		this.dateDebut = domicile.getDateDebut();
		this.dateFin = domicile.getDateFin();
		this.source = domicile.getSource();
		this.dernierElement = false;
		this.peutEditerDateFin = false;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !annule && DateRange.super.isValidAt(date);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public Integer getNumeroOfsAutoriteFiscale() {
		return numeroOfsAutoriteFiscale;
	}

	public Long getId() {
		return id;
	}

	@Override
	public Source getSource() {
		return source;
	}

	public boolean isDernierElement() {
		return dernierElement;
	}

	public void setDernierElement(boolean dernierElement) {
		this.dernierElement = dernierElement;
	}

	public boolean isPeutEditerDateFin() {
		return peutEditerDateFin;
	}

	public void setPeutEditerDateFin(boolean peutEditerDateFin) {
		this.peutEditerDateFin = peutEditerDateFin;
	}
}
