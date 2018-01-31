package ch.vd.uniregctb.registrefoncier.allegement;

import java.util.Optional;

import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;

public abstract class AbstractEditDegrevementView extends AbstractYearRangeView {

	private final DonneesUtilisation location;
	private final DonneesUtilisation propreUsage;
	private boolean avecLoiLogement;
	private final DonneesLoiLogement loiLogement;

	public AbstractEditDegrevementView() {
		super();
		this.location = new DonneesUtilisation();
		this.propreUsage = new DonneesUtilisation();
		this.loiLogement = new DonneesLoiLogement();
	}

	public AbstractEditDegrevementView(DegrevementICI degrevement) {
		super(degrevement);
		this.avecLoiLogement = degrevement.getLoiLogement() != null;
		this.location = Optional.ofNullable(degrevement.getLocation()).map(DonneesUtilisation::new).orElseGet(DonneesUtilisation::new);
		this.propreUsage = Optional.ofNullable(degrevement.getPropreUsage()).map(DonneesUtilisation::new).orElseGet(DonneesUtilisation::new);
		this.loiLogement = Optional.ofNullable(degrevement.getLoiLogement()).map(DonneesLoiLogement::new).orElseGet(DonneesLoiLogement::new);
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
