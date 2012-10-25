package ch.vd.uniregctb.cache;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.Assert;

/**
 * Element stocké dans le cache qui contient une valeur et sa liste de 'parts'.
 * <p/>
 * L'idée est de compléter/restreindre à la demande la liste des parts disponibles, de manière à ne stocker qu'un seul value quelque soit les parts demandées.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class CacheValueWithParts<T, P> implements Serializable {

	private static final long serialVersionUID = -2265610043652838951L;

	@NotNull
	private final Set<P> parts;
	private T value;

	public CacheValueWithParts(Set<P> parts, T value) {
		this.parts = (parts == null ? new HashSet<P>() : new HashSet<P>(parts));
		this.value = value;
	}

	/**
	 * for testing purposes only
	 *
	 * @return les parts couramment renseignées sur l'objet caché.
	 */
	public synchronized Set<P> getParts() {
		return Collections.unmodifiableSet(parts);
	}

	/**
	 * @return <b>true</b> si la valeur est nulle; <b>false</b> autrement.
	 */
	public synchronized boolean isNull() {
		return value == null;
	}

	/**
	 * @param newParts les parts à vérifier
	 * @return la liste des parts manquantes à la valeur cachée par rapport au parts spécifiées.
	 */
	public synchronized Set<P> getMissingParts(Set<P> newParts) {
		if (newParts == null || newParts.isEmpty() || this.parts.containsAll(newParts)) {
			return null;
		}
		else {
			Set<P> missing = new HashSet<P>(newParts);
			missing.removeAll(this.parts);
			return missing;
		}
	}

	/**
	 * Ajoute les nouvelles parts spécifiées à la valeur cachée.
	 *
	 * @param newParts les parts à ajouter à la valeur cachée.
	 * @param newValue la valeur possédant les parts
	 */
	public synchronized void addParts(Set<P> newParts, T newValue) {
		Assert.notNull(newParts);
		if (this.value == null) {
			this.value = newValue;
		}
		else {
			copyParts(newParts, newValue, this.value);
		}
		this.parts.addAll(newParts);
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
	 * Cette méthode doit retourner une copie de la valeur cachée limitées au parties spécifiées.
	 *
	 * @param value une valeur qui possède plus de parts que celles spécifiées.
	 * @param parts les parts que la valeur retourné doit posséder.
	 * @return une copie du value spécifié ne possédant que les parts spécifiées.
	 */
	protected abstract T restrictTo(T value, Set<P> parts);

	/**
	 * Construit et retourne une nouvelle valeur à partir de la valeur cachée qui possède exactement les parts spécifiées.
	 *
	 * @param parts les parts devant être renseignées sur l'objet
	 * @return un nouvel objet avec les parts spécifiées.
	 * @throws IllegalArgumentException si toutes les parts spécifiées ne sont pas disponibles dans la valeur cachée.
	 */
	public synchronized T getValueForParts(Set<P> parts) {
		if (parts != null && !this.parts.containsAll(parts)) {
			throw new IllegalArgumentException("Toutes les parties demandées n'existent pas dans les parties disponibles.");
		}
		return restrictTo(value, parts);
	}

	/**
	 * Construit et retourne une nouvelle valeur à partir de la valeur cachée qui possède exactement les parts spécifiées. Si la valeur cachée ne possède pas les parts demandées, un appel au callback est
	 * effectuée pour compléter les parts manquantes.
	 *
	 * @param parts    les parts devant être renseignées sur l'objet
	 * @param callback un callback qui permet de compléter les parts manquantes si nécessaire.
	 * @return un nouvel objet avec les parts spécifiées.
	 * @throws IllegalArgumentException si toutes les parts spécifiées ne sont pas disponibles dans la valeur cachée.
	 */
	public synchronized T getValueForPartsAndCompleteIfNeeded(Set<P> parts, CompletePartsCallback<T, P> callback) {
		if (isNull()) {
			return null;
		}

		final Set<P> delta = getMissingParts(parts);
		if (delta != null) {
			// on complète la liste des parts à la volée
			final T deltaValue = callback.getDeltaValue(delta);
			addParts(delta, deltaValue);
			callback.postCompletion();
		}
		return getValueForParts(parts);
	}

	/**
	 * Construit et retourne une nouvelle valeur à partir de la valeur cachée qui possède exactement les parts spécifiées. Si la valeur cachée ne possède pas les parts demandées, un appel au callback est
	 * effectuée pour compléter les parts manquantes.
	 *
	 * @param parts    les parts devant être renseignées sur l'objet
	 * @param callback un callback qui permet de compléter les parts manquantes si nécessaire.
	 * @return un nouvel objet avec les parts spécifiées.
	 * @throws IllegalArgumentException si toutes les parts spécifiées ne sont pas disponibles dans la valeur cachée.
	 */
	public synchronized T getValueForPartsAndCompleteIfNeeded(Set<P> parts, CompletePartsCallbackWithException<T, P> callback) throws Exception {
		if (isNull()) {
			return null;
		}

		final Set<P> delta = getMissingParts(parts);
		if (delta != null) {
			// on complète la liste des parts à la volée
			final T deltaValue = callback.getDeltaValue(delta);
			addParts(delta, deltaValue);
		}
		return getValueForParts(parts);
	}
}