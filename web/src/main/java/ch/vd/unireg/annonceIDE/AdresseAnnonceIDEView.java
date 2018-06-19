package ch.vd.unireg.annonceIDE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.entreprise.data.AdresseAnnonceIDE;

/**
 * Vue web d'une adresse d'annonce à l'IDE.
 */
public class AdresseAnnonceIDEView {

	private Integer egid;
	private String rue;
	private String numero;
	private String numeroAppartement;
	private Integer numeroCasePostale;
	private String texteCasePostale;
	private String ville;
	private Integer npa;
	private AdresseAnnonceIDE.Pays pays;

	public AdresseAnnonceIDEView(String rue, String numero, Integer npa, String ville) {
		this.rue = rue;
		this.numero = numero;
		this.ville = ville;
		this.npa = npa;
	}

	public AdresseAnnonceIDEView(@NotNull AdresseAnnonceIDE adresse) {
		this.egid = adresse.getEgid();
		this.rue = adresse.getRue();
		this.numero = adresse.getNumero();
		this.numeroAppartement = adresse.getNumeroAppartement();
		this.numeroCasePostale = adresse.getNumeroCasePostale();
		this.texteCasePostale = adresse.getTexteCasePostale();
		this.ville = adresse.getVille();
		this.npa = adresse.getNpa();
		this.pays = adresse.getPays();
	}

	public Integer getEgid() {
		return egid;
	}

	public void setEgid(Integer egid) {
		this.egid = egid;
	}

	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	public void setNumeroCasePostale(Integer numeroCasePostale) {
		this.numeroCasePostale = numeroCasePostale;
	}

	public String getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(String texteCasePostale) {
		this.texteCasePostale = texteCasePostale;
	}

	public String getVille() {
		return ville;
	}

	public void setVille(String ville) {
		this.ville = ville;
	}

	public Integer getNpa() {
		return npa;
	}

	public void setNpa(Integer npa) {
		this.npa = npa;
	}

	public AdresseAnnonceIDE.Pays getPays() {
		return pays;
	}

	public void setPays(AdresseAnnonceIDE.Pays pays) {
		this.pays = pays;
	}

	@Nullable
	public static AdresseAnnonceIDEView get(@Nullable AdresseAnnonceIDE adresse) {
		if (adresse == null) {
			return null;
		}
		return new AdresseAnnonceIDEView(adresse);
	}
}
