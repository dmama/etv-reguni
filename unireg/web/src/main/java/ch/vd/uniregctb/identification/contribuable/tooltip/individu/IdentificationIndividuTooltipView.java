package ch.vd.uniregctb.identification.contribuable.tooltip.individu;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.Sexe;

public class IdentificationIndividuTooltipView {

	private Long numeroIndividu;
	private String nom;
	private String nomNaissance;
	private String prenom;
	private String autresPrenoms;
	private RegDate dateNaissance;
	private Sexe sexe;
	private EtatCivil etatCivil;
	private String numeroAssureSocial;
	private String ancienNumeroAVS;
	private String numeroRCE;
	private String nationalite;

	public IdentificationIndividuTooltipView() {
	}

	public void init(Individu individu) {
		if (individu != null) {
			this.numeroIndividu = individu.getNoTechnique();
			this.prenom = individu.getPrenom();
			this.nom = individu.getNom();
			this.nomNaissance = individu.getNomNaissance();
			this.autresPrenoms = individu.getAutresPrenoms();
			this.dateNaissance = individu.getDateNaissance();
			this.sexe = individu.getSexe();
			this.etatCivil = EtatCivilHelper.civil2core(individu.getEtatCivilCourant().getTypeEtatCivil());
			this.numeroAssureSocial = individu.getNouveauNoAVS();
			this.ancienNumeroAVS = individu.getNoAVS11();
			this.numeroRCE = individu.getNumeroRCE();

			final Nationalite nationalite = individu.getDerniereNationalite();
			if (nationalite == null) {
				this.nationalite = null;
			}
			else {
				this.nationalite = nationalite.getPays().getNomMinuscule();
			}
		}
	}

	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public String getNom() {
		return nom;
	}

	public String getNomNaissance() {
		return nomNaissance;
	}

	public String getPrenom() {
		return prenom;
	}

	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public String getAncienNumeroAVS() {
		return ancienNumeroAVS;
	}

	public String getNumeroRCE() {
		return numeroRCE;
	}

	public String getNationalite() {
		return nationalite;
	}
}
