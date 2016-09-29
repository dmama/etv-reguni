package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

public class InscriptionREE implements Serializable {

	private static final long serialVersionUID = -4981395425613937166L;

	private final StatusREE status;
	private final RegDate dateInscription;

	public InscriptionREE(StatusREE status, RegDate dateInscription) {
		this.status = status;
		this.dateInscription = dateInscription;
	}

	public StatusREE getStatus() {
		return status;
	}

	public RegDate getDateInscription() {
		return dateInscription;
	}
}
