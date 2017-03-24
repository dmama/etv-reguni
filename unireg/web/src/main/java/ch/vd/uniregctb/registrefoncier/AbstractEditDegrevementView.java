package ch.vd.uniregctb.registrefoncier;

import java.util.Optional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;

public abstract class AbstractEditDegrevementView implements DateRange {

	private Integer pfDebut;
	private Integer pfFin;
	private final DonneesUtilisation location;
	private final DonneesUtilisation propreUsage;
	private boolean avecLoiLogement;
	private final DonneesLoiLogement loiLogement;

	public AbstractEditDegrevementView() {
		this.location = new DonneesUtilisation();
		this.propreUsage = new DonneesUtilisation();
		this.loiLogement = new DonneesLoiLogement();
	}

	public AbstractEditDegrevementView(DegrevementICI degrevement) {
		this.pfDebut = Optional.ofNullable(degrevement.getDateDebut()).map(RegDate::year).orElse(null);
		this.pfFin = Optional.ofNullable(degrevement.getDateFin()).map(RegDate::year).orElse(null);
		this.avecLoiLogement = degrevement.getLoiLogement() != null;
		this.location = Optional.ofNullable(degrevement.getLocation()).map(DonneesUtilisation::new).orElseGet(DonneesUtilisation::new);
		this.propreUsage = Optional.ofNullable(degrevement.getPropreUsage()).map(DonneesUtilisation::new).orElseGet(DonneesUtilisation::new);
		this.loiLogement = Optional.ofNullable(degrevement.getLoiLogement()).map(DonneesLoiLogement::new).orElseGet(DonneesLoiLogement::new);
	}

	@Override
	public RegDate getDateDebut() {
		return Optional.ofNullable(pfDebut).map(pf -> RegDate.get(pf, 1, 1)).orElse(null);
	}

	@Override
	public RegDate getDateFin() {
		return Optional.ofNullable(pfFin).map(pf -> RegDate.get(pf, 12, 31)).orElse(null);
	}

	public Integer getPfDebut() {
		return pfDebut;
	}

	public void setPfDebut(Integer pfDebut) {
		this.pfDebut = pfDebut;
	}

	public Integer getPfFin() {
		return pfFin;
	}

	public void setPfFin(Integer pfFin) {
		this.pfFin = pfFin;
	}

	public DonneesUtilisation getLocation() {
		return location;
	}

	public DonneesUtilisation getPropreUsage() {
		return propreUsage;
	}

	public boolean isAvecLoiLogement() {
		return avecLoiLogement;
	}

	public void setAvecLoiLogement(boolean avecLoiLogement) {
		this.avecLoiLogement = avecLoiLogement;
	}

	public DonneesLoiLogement getLoiLogement() {
		return loiLogement;
	}
}
