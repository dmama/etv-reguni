package ch.vd.unireg.efacture;

import java.math.BigInteger;

import ch.vd.registre.base.date.RegDate;

public class ActionDemandeView {

	private long ctbId;
	private String idDemande;
	private RegDate dateDemande;
	private BigInteger noAdherent;

	public long getCtbId() {
		return ctbId;
	}

	public void setCtbId(long ctbId) {
		this.ctbId = ctbId;
	}

	public String getIdDemande() {
		return idDemande;
	}

	public void setIdDemande(String idDemande) {
		this.idDemande = idDemande;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public BigInteger getNoAdherent() {
		return noAdherent;
	}

	public void setNoAdherent(BigInteger noAdherent) {
		this.noAdherent = noAdherent;
	}
}
