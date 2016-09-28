package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;

/**
 * @author Raphaël Marmier, 2016-08-19, <raphael.marmier@vd.ch>
 */
public class AnnonceIDERCEnt extends ModeleAnnonceIDERCEnt implements AnnonceIDE, Serializable {

	private static final long serialVersionUID = -9145071770676454941L;

	/**
	 * Identifiant métier de l'annonce
	 */
	private Long numero;

	public AnnonceIDERCEnt(Long numero, TypeAnnonce type, Date dateAnnonce, Utilisateur utilisateur, TypeDeSite typeDeSite, Statut statut) {
		super(type, dateAnnonce, utilisateur, typeDeSite, statut);
		Assert.notNull(numero, "Un numero doit être fourni pour créer une annonce. Alternativement, créez un modèle d'annonce, qui ne nécessite pas de numéro.");
		this.numero = numero;
	}

	public AnnonceIDERCEnt(Long numero, ModeleAnnonceIDE modele, @Nullable Statut statut) {
		super(modele, statut);
		this.numero = numero;
	}

	@Override
	public Long getNumero() {
		return numero;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final AnnonceIDERCEnt that = (AnnonceIDERCEnt) o;

		return getNumero() != null ? getNumero().equals(that.getNumero()) : that.getNumero() == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (getNumero() != null ? getNumero().hashCode() : 0);
		return result;
	}
}
