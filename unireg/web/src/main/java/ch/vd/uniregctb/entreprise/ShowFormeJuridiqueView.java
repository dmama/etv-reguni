package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.Sourced;

public class ShowFormeJuridiqueView implements Sourced<Source>, Annulable, DateRange {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final FormeLegale type;
	private final Source source;
	private final boolean annule;
	private boolean dernierElement;

	public ShowFormeJuridiqueView(FormeLegaleHisto forme) {
		this(forme.getId(), forme.isAnnule(), forme.getDateDebut(), forme.getDateFin(), forme.getFormeLegale(), forme.getSource());
	}

	private ShowFormeJuridiqueView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, FormeLegale type, Source source) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
		this.annule = annule;
		this.source = source;
		this.dernierElement = false;
	}

	public Long getId() {
		return id;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public FormeLegale getType() {
		return type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isAnnule() {
		return annule;
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
}