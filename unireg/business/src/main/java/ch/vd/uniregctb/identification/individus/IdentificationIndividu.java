package ch.vd.uniregctb.identification.individus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;

public class IdentificationIndividu {
	public final long noInd;
	public final String prenom;
	public final String autrePrenoms;
	public final String nom;
	public final RegDate dateNaissance;
	public final String noAVS13;

	public IdentificationIndividu(long noInd) {
		this.noInd = noInd;
		this.prenom = null;
		this.autrePrenoms = null;
		this.nom = null;
		this.dateNaissance = null;
		this.noAVS13 = null;
	}

	public IdentificationIndividu(Individu individu) {
		this.noInd = individu.getNoTechnique();
		this.prenom = individu.getPrenom();
		this.autrePrenoms = individu.getAutresPrenoms();
		this.nom = individu.getNom();
		this.dateNaissance = individu.getDateNaissance();
		this.noAVS13 = individu.getNouveauNoAVS();
	}
}
