package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.SiegeHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.Sourced;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DomicileEtablissementView implements Sourced<Source>, DateRange, Annulable {

	private final Long id;
	private final boolean annule;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final Integer numeroOfsAutoriteFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Source source;

	public DomicileEtablissementView(SiegeHisto domicile) {
		this.id = null;
		this.annule = false;
		this.typeAutoriteFiscale = domicile.getTypeAutoriteFiscale();
		this.numeroOfsAutoriteFiscale = domicile.getNoOfs();
		this.dateDebut = domicile.getDateDebut();
		this.dateFin = domicile.getDateFin();
		this.source = domicile.getSource();
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !annule && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
}
