package ch.vd.unireg.annonceIDE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;

/**
 * Vue web du contenu du annonce à l'IDE.
 */
public class ContenuView {
	private String nom;
	private String nomAdditionnel;
	private AdresseAnnonceIDEView adresse;
	private FormeLegale formeLegale;
	private String secteurActivite;

	public ContenuView(String nom, String nomAdditionnel, AdresseAnnonceIDEView adresse, FormeLegale formeLegale, String secteurActivite) {
		this.nom = nom;
		this.nomAdditionnel = nomAdditionnel;
		this.adresse = adresse;
		this.formeLegale = formeLegale;
		this.secteurActivite = secteurActivite;
	}

	public ContenuView(@NotNull BaseAnnonceIDE.Contenu contenu) {
		this.nom = contenu.getNom();
		this.nomAdditionnel = contenu.getNomAdditionnel();
		this.adresse = AdresseAnnonceIDEView.get(contenu.getAdresse());
		this.formeLegale = contenu.getFormeLegale();
		this.secteurActivite = contenu.getSecteurActivite();
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getNomAdditionnel() {
		return nomAdditionnel;
	}

	public void setNomAdditionnel(String nomAdditionnel) {
		this.nomAdditionnel = nomAdditionnel;
	}

	public AdresseAnnonceIDEView getAdresse() {
		return adresse;
	}

	public void setAdresse(AdresseAnnonceIDEView adresse) {
		this.adresse = adresse;
	}

	public FormeLegale getFormeLegale() {
		return formeLegale;
	}

	public void setFormeLegale(FormeLegale formeLegale) {
		this.formeLegale = formeLegale;
	}

	public String getSecteurActivite() {
		return secteurActivite;
	}

	public void setSecteurActivite(String secteurActivite) {
		this.secteurActivite = secteurActivite;
	}

	@Nullable
	public static ContenuView get(@Nullable BaseAnnonceIDE.Contenu contenu) {
		if (contenu == null) {
			return null;
		}
		return new ContenuView(contenu);
	}
}
