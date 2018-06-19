package ch.vd.unireg.annonceIDE;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;

/**
 * Vue web des informations concernant l'annonceur d'une annonce à l'IDE.
 */
public class UtilisateurView {

	private String userId;
	private String telephone;

	public UtilisateurView() {
	}

	public UtilisateurView(String userId, String telephone) {
		this.userId = userId;
		this.telephone = telephone;
	}

	public UtilisateurView(@NotNull BaseAnnonceIDE.Utilisateur utilisateur) {
		this.userId = utilisateur.getUserId();
		this.telephone = utilisateur.getTelephone();
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}
}
