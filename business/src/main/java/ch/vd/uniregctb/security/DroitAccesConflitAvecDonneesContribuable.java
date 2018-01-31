package ch.vd.uniregctb.security;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

public class DroitAccesConflitAvecDonneesContribuable implements Serializable {

	private static final long serialVersionUID = -1487262043378533023L;

	private final DroitAccesConflit conflit;
	private final String prenomNom;
	private final RegDate dateNaissance;
	private final String npaLocalite;

	public DroitAccesConflitAvecDonneesContribuable(DroitAccesConflit conflit, String prenomNom, RegDate dateNaissance, String npaLocalite) {
		this.conflit = conflit;
		this.prenomNom = prenomNom;
		this.dateNaissance = dateNaissance;
		this.npaLocalite = npaLocalite;
	}

	public long getNoContribuable() {
		return conflit.getNoContribuable();
	}

	public DroitAccesConflit.NiveauAcces getAccesPreexistant() {
		return conflit.getAccesPreexistant();
	}

	public DroitAccesConflit.NiveauAcces getAccesCopie() {
		return conflit.getAccesCopie();
	}

	public String getPrenomNom() {
		return prenomNom;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public String getNpaLocalite() {
		return npaLocalite;
	}
}
