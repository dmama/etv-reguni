package ch.vd.uniregctb.identification.contribuable.tooltip.individu;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
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
			final HistoriqueIndividu historique = individu.getDernierHistoriqueIndividu();

			this.numeroIndividu = individu.getNoTechnique();
			this.prenom = historique.getPrenom();
			this.nom = historique.getNom();
			this.nomNaissance = historique.getNomNaissance();
			this.autresPrenoms = historique.getAutresPrenoms();
			this.dateNaissance = individu.getDateNaissance();
			this.sexe = (individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);
			this.etatCivil = individu.getEtatCivilCourant().getTypeEtatCivil().asCore();
			this.numeroAssureSocial = individu.getNouveauNoAVS();
			this.ancienNumeroAVS = historique.getNoAVS();
			this.numeroRCE = individu.getNumeroRCE();

			final List<Nationalite> nationalites = individu.getNationalites();
			if (nationalites == null || nationalites.isEmpty()) {
				this.nationalite = null;
			}
			else {
				final StringBuilder b = new StringBuilder();
				for (Nationalite nationalite : nationalites) {
					if (b.length() > 0) {
						b.append(", ");
					}
					b.append(nationalite.getPays().getNomMinuscule());
				}
				this.nationalite = b.toString();
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
