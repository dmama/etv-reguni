package ch.vd.uniregctb.webservices.tiers.cache;

import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

/**
 * Element stocké dans le cache qui contient un tiers et sa liste de 'parts'.
 * <p>
 * L'idée est de compléter/restreindre à la demande la liste des parts disponibles, de manière à ne stocker qu'un seul tiers quelque soit
 * les parts demandées.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
abstract class CacheValueWithParts<T> {

	private Set<TiersPart> parts;
	private T tiers;

	public CacheValueWithParts(Set<TiersPart> parts, T tiers) {
		this.parts = (parts == null ? null : new HashSet<TiersPart>(parts));
		this.tiers = tiers;
	}

	/** for testing purposes only */
	protected Set<TiersPart> getParts() {
		return parts;
	}

	/**
	 * @return <b>true</b> si la valeur est nulle; <b>false</b> autrement.
	 */
	public boolean isNull() {
		return tiers == null;
	}

	/**
	 * @return la liste des parts manquantes au tiers caché par rapport au parts spécifiées.
	 */
	public Set<TiersPart> getMissingParts(Set<TiersPart> newParts) {
		if (this.parts == null) {
			return newParts;
		}
		else if (newParts == null || this.parts.containsAll(newParts)) {
			return null;
		}
		else {
			Set<TiersPart> missing = new HashSet<TiersPart>(newParts);
			missing.removeAll(this.parts);
			return missing;
		}
	}

	/**
	 * Ajoute les nouvelles parts spécifiées au tiers caché.
	 */
	public void addParts(Set<TiersPart> newParts, T newTiers) {
		if (this.parts == null) {
			Assert.notNull(newParts);
			this.parts = new HashSet<TiersPart>(newParts);
			this.tiers = newTiers;
		}
		else {
			copyParts(newParts, newTiers, this.tiers);
			this.parts.addAll(newParts);
		}
	}

	/**
	 * Cette méthode doit copier la partie spécifiée de l'objet <i>from</i> à l'objet <i>to</i>.
	 */
	protected abstract void copyParts(Set<TiersPart> parts, T from, T to);

	/**
	 * Cette méthode doit retourner une copie du tiers caché limitées au parties spécifiées.
	 */
	protected abstract T restrictTo(T tiers, Set<TiersPart> parts);

	/**
	 * Construit et retourn un nouveau tiers à partir du tiers caché qui possède exactement les parts spécifiées.
	 *
	 * @param parts
	 * @return
	 */
	public T getTiersForParts(Set<TiersPart> parts) {
		Assert.isTrue(parts == null || this.parts.containsAll(parts));
		return restrictTo(tiers, parts);
	}
}
