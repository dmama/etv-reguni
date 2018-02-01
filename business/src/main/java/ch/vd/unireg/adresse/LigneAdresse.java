package ch.vd.unireg.adresse;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class LigneAdresse implements Serializable {

	private static final long serialVersionUID = 6255790490360098890L;

	/**
	 * le contenu string d'une ligne d'adresse.
	 */
	private final String texte;

	/**
	 * <code>true</code> si cette ligne représente un wrapping de la précédente, <code>false</code> sinon
	 */
	private final boolean wrapping;

	public LigneAdresse(String texte, boolean wrapping) {
		this.texte = StringUtils.trimToNull(texte);
		this.wrapping = wrapping;
	}

	@Nullable
	public String getTexte() {
		return texte;
	}

	public boolean isWrapping() {
		return wrapping;
	}

	@Override
	public String toString() {
		return texte;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final LigneAdresse that = (LigneAdresse) o;
		return wrapping == that.wrapping && (texte != null ? texte.equals(that.texte) : that.texte == null);
	}

	@Override
	public int hashCode() {
		int result = texte != null ? texte.hashCode() : 0;
		result = 31 * result + (wrapping ? 1 : 0);
		return result;
	}
}
