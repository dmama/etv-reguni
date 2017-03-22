package ch.vd.uniregctb.registrefoncier;

import java.util.Optional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;

public class DegrevementICIView implements Annulable, DateRange {

	private final long idDegrevement;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final DonneesUtilisation location;
	private final DonneesUtilisation propreUsage;
	private final DonneesLoiLogement loiLogement;

	public DegrevementICIView(DegrevementICI data) {
		this.idDegrevement = data.getId();
		this.annule = data.isAnnule();
		this.dateDebut = data.getDateDebut();
		this.dateFin = data.getDateFin();
		this.location = Optional.ofNullable(data.getLocation()).map(DonneesUtilisation::new).orElse(null);
		this.propreUsage = Optional.ofNullable(data.getPropreUsage()).map(DonneesUtilisation::new).orElse(null);
		this.loiLogement = Optional.ofNullable(data.getLoiLogement()).map(DonneesLoiLogement::new).orElse(null);
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
	public boolean isAnnule() {
		return annule;
	}

	public long getIdDegrevement() {
		return idDegrevement;
	}

	public DonneesUtilisation getLocation() {
		return location;
	}

	public DonneesUtilisation getPropreUsage() {
		return propreUsage;
	}

	public DonneesLoiLogement getLoiLogement() {
		return loiLogement;
	}
}
