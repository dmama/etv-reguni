package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;

public class FlagEntrepriseView implements DateRange, Annulable {

	private final long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeFlagEntreprise type;
	private final boolean annule;

	public FlagEntrepriseView(FlagEntreprise flag) {
		this.id = flag.getId();
		this.dateDebut = flag.getDateDebut();
		this.dateFin = flag.getDateFin();
		this.type = flag.getType();
		this.annule = flag.isAnnule();
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

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public long getId() {
		return id;
	}

	public TypeFlagEntreprise getType() {
		return type;
	}
}
