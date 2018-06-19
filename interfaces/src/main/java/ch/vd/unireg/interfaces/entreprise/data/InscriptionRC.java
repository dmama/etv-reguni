package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

public class InscriptionRC implements Serializable {

	private static final long serialVersionUID = -5611746288492907291L;

	private final StatusInscriptionRC status;
	private final RaisonDeDissolutionRC raisonDissolutionVD;
	private final RegDate dateInscriptionVD;
	private final RegDate dateRadiationVD;
	private final RegDate dateInscriptionCH;
	private final RegDate dateRadiationCH;

	public InscriptionRC(StatusInscriptionRC status, RaisonDeDissolutionRC raisonDissolutionVD, RegDate dateInscriptionVD, RegDate dateRadiationVD, RegDate dateInscriptionCH, RegDate dateRadiationCH) {
		this.status = status;
		this.raisonDissolutionVD = raisonDissolutionVD;
		this.dateInscriptionVD = dateInscriptionVD;
		this.dateRadiationVD = dateRadiationVD;
		this.dateInscriptionCH = dateInscriptionCH;
		this.dateRadiationCH = dateRadiationCH;
	}

	public StatusInscriptionRC getStatus() {
		return status;
	}

	public RaisonDeDissolutionRC getRaisonDissolutionVD() {
		return raisonDissolutionVD;
	}

	public RegDate getDateInscriptionVD() {
		return dateInscriptionVD;
	}

	public RegDate getDateInscriptionCH() {
		return dateInscriptionCH;
	}

	public RegDate getDateRadiationVD() {
		return dateRadiationVD;
	}

	public RegDate getDateRadiationCH() {
		return dateRadiationCH;
	}

	public boolean isInscrit() {
		return status != null && status != StatusInscriptionRC.INCONNU && status != StatusInscriptionRC.NON_INSCRIT;
	}
}
