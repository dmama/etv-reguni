package ch.vd.unireg.registrefoncier.allegement;

import java.util.Optional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.foncier.DegrevementICI;
import ch.vd.unireg.foncier.DonneesLoiLogement;
import ch.vd.unireg.foncier.DonneesUtilisation;

public class DegrevementICIView implements Annulable, DateRange {

	private final long idDegrevement;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final DonneesUtilisation location;
	private final DonneesUtilisation propreUsage;
	private final DonneesLoiLogement loiLogement;
	private final boolean nonIntegrable;

	public DegrevementICIView(DegrevementICI data) {
		this.idDegrevement = data.getId();
		this.annule = data.isAnnule();
		this.dateDebut = data.getDateDebut();
		this.dateFin = data.getDateFin();
		this.location = Optional.ofNullable(data.getLocation()).map(DonneesUtilisation::new).orElse(null);
		this.propreUsage = Optional.ofNullable(data.getPropreUsage()).map(DonneesUtilisation::new).orElse(null);
		this.loiLogement = Optional.ofNullable(data.getLoiLogement()).map(DonneesLoiLogement::new).orElse(null);
		this.nonIntegrable = data.nonIntegrable();
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

	public boolean isNonIntegrable() {
		return nonIntegrable;
	}

	/**
	 * @return <code>true</code> si le dégrèvement est complètement dans le passé
	 */
	public boolean isPast() {
		return dateFin != null && dateFin.isBefore(RegDate.get());
	}
}
