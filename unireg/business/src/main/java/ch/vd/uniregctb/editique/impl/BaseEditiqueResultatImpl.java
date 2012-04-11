package ch.vd.uniregctb.editique.impl;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.editique.EditiqueResultat;

/**
 * Classe de base des résultats éditiques
 */
public abstract class BaseEditiqueResultatImpl implements EditiqueResultat {

	private final String idDocument;

	public BaseEditiqueResultatImpl(String idDocument) {
		this.idDocument = idDocument;
	}

	@Override
	public final String getIdDocument() {
		return idDocument;
	}

	/**
	 * A surcharger dans les classes dérivées si nécessaire
	 * @return une description des attributs de la classe, ou <code>null</code> si rien à ajouter
	 */
	protected abstract String getToStringComplement();

	@Override
	public final String toString() {
		final String cplt = getToStringComplement();
		final String cpltEffectif;
		if (StringUtils.isNotBlank(cplt)) {
			cpltEffectif = String.format(", %s", cplt);
		}
		else {
			cpltEffectif = StringUtils.EMPTY;
		}
		return String.format("%s{idDocument='%s'%s}", getClass().getSimpleName(), idDocument, cpltEffectif);
	}
}
