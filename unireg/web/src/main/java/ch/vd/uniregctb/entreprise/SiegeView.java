package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.Sourced;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class SiegeView implements Sourced<Source>, DateRange, Annulable {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsSiege;
	private final TypeNoOfs type;
	private final Source source;

	public SiegeView(DomicileHisto siege) {
		this.id = siege.getId();
		this.dateDebut = siege.getDateDebut();
		this.dateFin = siege.getDateFin();
		this.noOfsSiege = siege.getNoOfs();
		this.type = siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS ? TypeNoOfs.PAYS_HS : TypeNoOfs.COMMUNE_CH;
		this.annule = siege.isAnnule();
		this.source = siege.getSource();
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

	public int getNoOfsSiege() {
		return noOfsSiege;
	}

	public TypeNoOfs getType() {
		return type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !annule && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public Source getSource() {
		return source;
	}
}
