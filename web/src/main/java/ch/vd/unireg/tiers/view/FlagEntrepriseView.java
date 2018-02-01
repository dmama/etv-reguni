package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.type.TypeFlagEntreprise;

public class FlagEntrepriseView implements DateRange, Annulable {

	private final long pmId;
	private final long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeFlagEntreprise type;
	private final boolean annule;

	public FlagEntrepriseView(FlagEntreprise flag) {
		this.pmId = flag.getEntreprise().getNumero();
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
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	public long getPmId() {
		return pmId;
	}

	public long getId() {
		return id;
	}

	public TypeFlagEntreprise getType() {
		return type;
	}
}
