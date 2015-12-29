package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DomicileEtablissementView implements DateRange, Annulable {

	private final Long id;
	private final boolean annule;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final Integer numeroOfsAutoriteFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public DomicileEtablissementView(DomicileEtablissement domicile) {
		this.id = domicile.getId();
		this.annule = domicile.isAnnule();
		this.typeAutoriteFiscale = domicile.getTypeAutoriteFiscale();
		this.numeroOfsAutoriteFiscale = domicile.getNumeroOfsAutoriteFiscale();
		this.dateDebut = domicile.getDateDebut();
		this.dateFin = domicile.getDateFin();
	}

	public DomicileEtablissementView(Siege siege) {
		this.id = null;
		this.annule = false;
		this.typeAutoriteFiscale = siege.getTypeAutoriteFiscale();
		this.numeroOfsAutoriteFiscale = siege.getNoOfs();
		this.dateDebut = siege.getDateDebut();
		this.dateFin = siege.getDateFin();
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
}
