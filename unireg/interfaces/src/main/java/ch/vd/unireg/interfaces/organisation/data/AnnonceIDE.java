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
	private final Long numero;

	public AnnonceIDE(Long numero, TypeAnnonce type, Date dateAnnonce, Utilisateur utilisateur, TypeDeSite typeDeSite, Statut statut, InfoServiceIDEObligEtendues infos) {
		super(type, dateAnnonce, utilisateur, typeDeSite, statut, infos);
		// SIFISC-23702 Dans les annonces renvoyées par le WS noticeRequestList de RCEnt, le userId peut être nul lorsque l'annonce n'est pas encore traitée à proprement parler par RCEnt.
		//sanityCheck(numero, utilisateur);
		Objects.requireNonNull(numero, "Impossible de créer une annonce à l'IDE sans lui donner un numéro.");
		this.numero = numero;
	}

	public AnnonceIDE(Long numero, BaseAnnonceIDE modele, @Nullable Statut statut) {
		super(modele, statut);
		sanityCheck(numero, modele.getUtilisateur());
		this.numero = numero;
	}

	private void sanityCheck(Long numero, Utilisateur utilisateur) {
		Objects.requireNonNull(numero, "Impossible de créer une annonce à l'IDE sans lui donner un numéro.");
		Objects.requireNonNull(utilisateur.getUserId(), "Impossible de créer une annonce à l'IDE sans lui donner un userId valide.");
	}

	@Override
	public Long getNumero() {
		return numero;
	}

	public String getUniqueKey() {
		// Pas d'utilisateur est une modalité valable pour ce qui est de la clé.
		return numero + (getUtilisateur() == null ? "" : getUtilisateur().getUserId());
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
