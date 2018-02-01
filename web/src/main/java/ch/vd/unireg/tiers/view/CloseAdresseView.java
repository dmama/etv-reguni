package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeAdresseTiers;

public class CloseAdresseView {

	private long idAdresse;
	private long idTiers;
	private TypeAdresseTiers usage;
	private RegDate dateDebut;
	private RegDate dateFin;

	public long getIdAdresse() {
		return idAdresse;
	}

	public void setIdAdresse(long idAdresse) {
		this.idAdresse = idAdresse;
	}

	public long getIdTiers() {
		return idTiers;
	}

	public void setIdTiers(long idTiers) {
		this.idTiers = idTiers;
	}

	public TypeAdresseTiers getUsage() {
		return usage;
	}

	public void setUsage(TypeAdresseTiers usage) {
		this.usage = usage;
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
