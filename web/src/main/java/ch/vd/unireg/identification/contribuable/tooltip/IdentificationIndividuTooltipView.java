package ch.vd.unireg.identification.contribuable.tooltip;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.NationaliteHelper;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.Sexe;

public class IdentificationIndividuTooltipView {

	private Long numeroIndividu;
	private String nom;
	private String nomNaissance;
	private String prenomUsuel;
	private String tousPrenoms;
	private RegDate dateNaissance;
	private Sexe sexe;
	private EtatCivil etatCivil;
	private String numeroAssureSocial;
	private String ancienNumeroAVS;
	private String numeroRCE;
	private String nationalites;

	public IdentificationIndividuTooltipView(Individu individu) {
		if (individu != null) {
			this.numeroIndividu = individu.getNoTechnique();
			this.prenomUsuel = individu.getPrenomUsuel();
			this.nom = individu.getNom();
			this.nomNaissance = individu.getNomNaissance();
			this.tousPrenoms = individu.getTousPrenoms();
			this.dateNaissance = individu.getDateNaissance();
			this.sexe = individu.getSexe();
			this.etatCivil = EtatCivilHelper.civil2core(individu.getEtatCivilCourant().getTypeEtatCivil());
			this.numeroAssureSocial = individu.getNouveauNoAVS();
			this.ancienNumeroAVS = individu.getNoAVS11();
			this.numeroRCE = individu.getNumeroRCE();

			final List<Nationalite> nationalites = NationaliteHelper.validAt(individu.getNationalites(), null);
			if (nationalites.size() > 0) {
				final StringBuilder b = new StringBuilder();
				for (Nationalite nat : nationalites) {
					if (b.length() > 0) {
						b.append(", ");
					}
					b.append(nat.getPays().getNomCourt());
				}
				this.nationalites = b.toString();
			}
			else {
				this.nationalites = null;
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

	public String getPrenomUsuel() {
		return prenomUsuel;
	}

	public String getTousPrenoms() {
		return tousPrenoms;
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

	public String getNationalites() {
		return nationalites;
	}
}
