package ch.vd.uniregctb.acls;

import org.acegisecurity.acls.AclFormattingUtils;
import org.acegisecurity.acls.Permission;
import org.acegisecurity.acls.domain.BasePermission;

/**
 * Les permissions de gestion d'un tiers.
 * 
 * @author Ludovic Bertin
 */
public final class TiersPermission implements Permission {
	// ~ Static fields/initializers
	// =====================================================================================

	/**
	 * Autorisé à créer le tiers.
	 */
	public static final Permission CREATE = new TiersPermission(1 << 0, 'C'); // 1

	/**
	 * Autorisé à lire le tiers.
	 */
	public static final Permission READ = new TiersPermission(1 << 1, 'R'); // 1

	/**
	 * Autorisé à modifier le tiers.
	 */
	public static final Permission UPDATE = new TiersPermission(1 << 2, 'U'); // 1

	/**
	 * Autorisé à supprimer le tiers.
	 */
	public static final Permission DELETE = new TiersPermission(1 << 3, 'D'); // 1

    /**
	 * Masque binaire.
	 */
	private int mask;

	/**
	 * Code utilisé pour la représentation du pattern.
	 */
	private char code;
	
	/**
	 * TiersPermission est construit avec un masque binaire et un code.
	 * 
	 * @param mask
	 *            Masque binaire
	 * @param code
	 *            Code utilisé pour la représentation du pattern
	 */
	private TiersPermission(int mask, char code) {
		this.mask = mask;
		this.code = code;
	}

	/**
	 * Renvoie le masque binaire.
	 * @return le masque binaire
	 */
	public int getMask() {
		return mask;
	}

	/**
	 * Renvoie la représentation du pattern.
	 * @return la représentation du pattern
	 */
	public String getPattern() {
		return AclFormattingUtils.printBinary(mask, code);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "TiersPermission[" + getPattern() + "=" + mask + "]";
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof BasePermission)) {
			return false;
		}

		BasePermission rhs = (BasePermission) arg0;

		return (this.mask == rhs.getMask());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.mask;
	}
}
