package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * @author Raphaël Marmier, 2016-08-19, <raphael.marmier@vd.ch>
 */
public class AnnonceIDE extends AnnonceIDEData implements AnnonceIDEEnvoyee, Serializable {

	private static final long serialVersionUID = -9145071770676454941L;

	/**
	 * Identifiant métier de l'annonce
	 */
	private Long numero;

	public AnnonceIDE(Long numero, TypeAnnonce type, Date dateAnnonce, Utilisateur utilisateur, TypeDeSite typeDeSite, Statut statut, InfoServiceIDEObligEtendues infos) {
		super(type, dateAnnonce, utilisateur, typeDeSite, statut, infos);
		sanityCheck(numero, utilisateur);
		this.numero = numero;
	}

	public AnnonceIDE(Long numero, BaseAnnonceIDE modele, @Nullable Statut statut) {
		super(modele, statut);
		sanityCheck(numero, modele.getUtilisateur());
		this.numero = numero;
	}

	protected void sanityCheck(Long numero, Utilisateur utilisateur) {
		Objects.requireNonNull(numero, "Un numero doit être fourni pour créer une annonce. Alternativement, créez un modèle d'annonce, qui ne nécessite pas de numéro.");
		Objects.requireNonNull(utilisateur.getUserId(), "Un utilisateur avec son userId doit être présent pour créer une annonce. Alternativement, créez un modèle d'annonce, qui ne nécessite pas de userId.");
	}

	@Override
	public Long getNumero() {
		return numero;
	}

	public String getUniqueKey() {
		return numero + getUtilisateur().getUserId();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final AnnonceIDE that = (AnnonceIDE) o;

		return getNumero() != null ? getNumero().equals(that.getNumero()) : that.getNumero() == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (getNumero() != null ? getNumero().hashCode() : 0);
		return result;
	}
}
