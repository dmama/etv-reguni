package ch.vd.uniregctb.cache;

import ch.vd.registre.base.utils.Assert;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Element stocké dans le cache qui contient un value et sa liste de 'parts'.
 * <p/>
 * L'idée est de compléter/restreindre à la demande la liste des parts disponibles, de manière à ne stocker qu'un seul value quelque soit les parts demandées.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class CacheValueWithParts<T, P> implements Serializable {

	private static final long serialVersionUID = -2265610043652838951L;

	private Set<P> parts;
	private T value;

	public CacheValueWithParts(Set<P> parts, T value) {
		this.parts = (parts == null ? null : new HashSet<P>(parts));
		this.value = value;
	}

	/**
	 * for testing purposes only
	 *
	 * @return les parts couramment renseignées sur l'objet caché.
	 */
	public Set<P> getParts() {
		return parts;
	}

	/**
	 * @return <b>true</b> si la valeur est nulle; <b>false</b> autrement.
	 */
	public boolean isNull() {
		return value == null;
	}

	/**
	 * @param newParts les parts à vérifier
	 * @return la liste des parts manquantes au value caché par rapport au parts spécifiées.
	 */
	public Set<P> getMissingParts(Set<P> newParts) {
		if (this.parts == null) {
			return newParts;
		}
		else if (newParts == null || this.parts.containsAll(newParts)) {
			return null;
		}
		else {
			Set<P> missing = new HashSet<P>(newParts);
			missing.removeAll(this.parts);
			return missing;
		}
	}

	/**
	 * Ajoute les nouvelles parts spécifiées au value caché.
	 *
	 * @param newParts les parts à ajouter à la valeur cachée.
	 * @param newValue la valeur possédant les parts
	 */
	public void addParts(Set<P> newParts, T newValue) {
		if (this.parts == null) {
			Assert.notNull(newParts);
			this.parts = new HashSet<P>(newParts);
			this.value = newValue;
		}
		else {
			copyParts(newParts, newValue, this.value);
			this.parts.addAll(newParts);
		}
	}

	/**
	 * Cette méthode doit copier la partie spécifiée de l'objet <i>from</i> à l'objet <i>to</i>.
	 *
	 * @param parts les parts à copier
	 * @param from  la valeur possédant les parts
	 * @param to    la valeur sur lequel les parts doivent être ajoutées
	 */
	protected abstract void copyParts(Set<P> parts, T from, T to);

	/**
	 * Cette méthode doit retourner une copie du value caché limitées au parties spécifiées.
	 *
	 * @param value une valeur qui possède plus de parts que celles spécifiées.
	 * @param parts les parts que la valeur retourné doit posséder.
	 * @return une copie du value spécifié ne possédant que les parts spécifiées.
	 */
	protected abstract T restrictTo(T value, Set<P> parts);

	/**
	 * Construit et retourn un nouveau value à partir du value caché qui possède exactement les parts spécifiées.
	 *
	 * @param parts les parts devant être renseignées sur l'objet
	 * @return un nouvel objet avec les parts spécifiées.
	 */
	public T getValueForParts(Set<P> parts) {
		Assert.isTrue(parts == null || this.parts.containsAll(parts));
		return restrictTo(value, parts);
	}
}