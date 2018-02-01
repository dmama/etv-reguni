package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.ActiviteEconomique;

public class CloseActiviteEconomiqueView {

	private long idRapportActiviteEconomique;
	private RegDate dateDebut;
	private RegDate dateFin;

	public CloseActiviteEconomiqueView() {
	}

	public CloseActiviteEconomiqueView(ActiviteEconomique ae) {
		this.idRapportActiviteEconomique = ae.getId();
		this.dateDebut = ae.getDateDebut();
		this.dateFin = ae.getDateFin();
	}

	public long getIdRapportActiviteEconomique() {
		return idRapportActiviteEconomique;
	}

	public void setIdRapportActiviteEconomique(long idRapportActiviteEconomique) {
		this.idRapportActiviteEconomique = idRapportActiviteEconomique;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}
}
