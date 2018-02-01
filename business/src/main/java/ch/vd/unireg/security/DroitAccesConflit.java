package ch.vd.unireg.security;

import java.io.Serializable;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

/**
 * Information remontée depuis la couche business vers les couches supérieures en cas de conflit
 * à la copie/au transfert de droits d'accès d'un opérateur vers un autre
 */
public class DroitAccesConflit implements Serializable {

	private static final long serialVersionUID = 1616793400885212728L;

	/**
	 * Regroupement d'un type d'accès (autorisation/interdiction) et d'un niveau (lecture/écriture)
	 */
	public static class NiveauAcces implements Serializable {

		private static final long serialVersionUID = 7445387369396577706L;

		private final TypeDroitAcces type;
		private final Niveau niveau;

		public NiveauAcces(TypeDroitAcces type, Niveau niveau) {
			this.type = type;
			this.niveau = niveau;
		}

		public TypeDroitAcces getType() {
			return type;
		}

		public Niveau getNiveau() {
			return niveau;
		}
	}

	private final long noContribuable;
	private final NiveauAcces accesPreexistant;
	private final NiveauAcces accesCopie;

	/**
	 * @param noContribuable le numéro du dossier sur lequel le conflit apparaît
	 * @param accesPreexistant les accès pré-existants sur ce dossier pour le destinataire de la copie
	 * @param accesCopie les accès à copier sur ce même dossier
	 */
	public DroitAccesConflit(long noContribuable, NiveauAcces accesPreexistant, NiveauAcces accesCopie) {
		this.noContribuable = noContribuable;
		this.accesPreexistant = accesPreexistant;
		this.accesCopie = accesCopie;
	}

	public DroitAccesConflit(long noContribuable, TypeDroitAcces typePreexistant, Niveau niveauPreexistant, TypeDroitAcces typeCopie, Niveau niveauCopie) {
		this(noContribuable, new NiveauAcces(typePreexistant, niveauPreexistant), new NiveauAcces(typeCopie, niveauCopie));
	}

	public long getNoContribuable() {
		return noContribuable;
	}

	public NiveauAcces getAccesPreexistant() {
		return accesPreexistant;
	}

	public NiveauAcces getAccesCopie() {
		return accesCopie;
	}

	public String toDisplayMessage() {
		return String.format("Conflit entre un accès pré-existant %s/%s et un nouvel accès %s/%s sur le dossier du contribuable %s.",
		                     accesPreexistant.getType(), accesPreexistant.getNiveau(),
		                     accesCopie.getType(), accesCopie.getNiveau(),
		                     FormatNumeroHelper.numeroCTBToDisplay(noContribuable));
	}
}
