package ch.vd.unireg.webservices.v7.cache;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.cache.CacheValueWithParts;
import ch.vd.unireg.cache.CompletePartsCallbackWithException;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyBuilder;

/**
 * Container d'une valeur mise en cache pour un tiers sorti d'un GetParty
 */
final class GetPartyValue extends CacheValueWithParts<Party, InternalPartyPart> implements Serializable {

	private static final long serialVersionUID = 2622109544331991998L;

	/**
	 * Ensemble des <i>parts</i> qui ne sont pas gérées par le cache
	 */
	private static final Set<InternalPartyPart> NON_CACHED_PARTS = EnumSet.of(InternalPartyPart.EBILLING_STATUSES);

	/**
	 * Classe interne qui permet de regrouper ensemble de <i>parts</i> et tiers dans une seule structure
	 * afin d'être construite en une seule fois dans le constructeur de {@link GetPartyValue}
	 */
	private static final class PartsAndValue {
		public final Set<InternalPartyPart> parts;
		public final Party value;

		public PartsAndValue(Set<InternalPartyPart> parts, Party value) {
			this.parts = parts;
			this.value = value;
		}
	}

	/**
	 * Re-centre les données d'entrée sur ce qui est effectivement gérable par le cache
	 * @param parts l'ensemble des parts en entrée
	 * @param value la valeur du tiers en entrée
	 * @return les données d'entrée éventuellement tronquées à la partie gérable par le cache
	 */
	private static PartsAndValue buildPartsAndValue(Set<InternalPartyPart> parts, Party value) {
		final Set<InternalPartyPart> allowedParts = getCacheablePartsIn(parts);
		final Party valueForCache = PartyBuilder.clone(value, allowedParts);
		return new PartsAndValue(allowedParts, valueForCache);
	}

	public GetPartyValue(Set<InternalPartyPart> parts, Party value) {
		this(buildPartsAndValue(parts, value));
	}

	private GetPartyValue(PartsAndValue partsAndValue) {
		super(InternalPartyPart.class, partsAndValue.parts, partsAndValue.value);
	}

	@Override
	public synchronized void addParts(Set<InternalPartyPart> newParts, Party newValue) {
		final PartsAndValue limited = buildPartsAndValue(newParts, newValue);
		super.addParts(limited.parts, limited.value);
	}

	@Override
	public synchronized Party getValueForPartsAndCompleteIfNeeded(Set<InternalPartyPart> parts, CompletePartsCallbackWithException<Party, InternalPartyPart> callback) throws Exception {
		final Set<InternalPartyPart> notAllowedParts = getNonCacheablePartsIn(parts);
		if (notAllowedParts.isEmpty() || isNull()) {
			return super.getValueForPartsAndCompleteIfNeeded(parts, callback);
		}

		// dans le cas où une ou plusieurs <i>parts</i> demandées ne sont pas gérées par le cache, il y a forcément des "missingParts"
		// et celles-ci ne seront pas récupérables par un appel à <i>getValueForParts</i> mais directement depuis la <i>deltaValue</i>

		final Set<InternalPartyPart> delta = getMissingParts(parts);
		final Party deltaValue = callback.getDeltaValue(delta);
		addParts(delta, deltaValue);

		final Set<InternalPartyPart> allowedParts = getCacheablePartsIn(parts);
		final Party fromCache = getValueForParts(allowedParts);
		copyParts(notAllowedParts, deltaValue, fromCache);
		return fromCache;
	}

	@Override
	protected void copyParts(Set<InternalPartyPart> parts, Party from, Party to) {
		PartyBuilder.copyParts(to, from, parts);
	}

	@Override
	protected Party restrictTo(Party tiers, Set<InternalPartyPart> parts) {
		return tiers == null ? null : PartyBuilder.clone(tiers, parts);
	}

	/**
	 * Récupère le sous-ensemble (jamais <code>null</code>) des <i>parts</i> demandées qui ne sont pas gérées par le cache
	 * @param askedForParts ensemble des <i>parts</i> demandées
	 * @return ensemble (peut-être vide, mais pas <code>null</code>) des <i>parts</i> demandées non-gérées par le cache
	 */
	@NotNull
	private static Set<InternalPartyPart> getNonCacheablePartsIn(@Nullable Set<InternalPartyPart> askedForParts) {
		final Set<InternalPartyPart> askedForNonCacheableParts = EnumSet.noneOf(InternalPartyPart.class);
		if (askedForParts != null) {
			askedForNonCacheableParts.addAll(askedForParts);
			askedForNonCacheableParts.retainAll(NON_CACHED_PARTS);
		}
		return askedForNonCacheableParts;
	}

	/**
	 * Récupère le sous-ensemble (jamais <code>null</code>) des <i>parts</i> demandées qui sont gérées par le cache
	 * @param askedForParts ensemble des <i>parts</i> demandées
	 * @return ensemble (peut-être vide, mais pas <code>null</code>) des <i>parts</i> demandées gérées par le cache
	 */
	@NotNull
	private static Set<InternalPartyPart> getCacheablePartsIn(@Nullable Set<InternalPartyPart> askedForParts) {
		final Set<InternalPartyPart> askedForNonCacheableParts = EnumSet.noneOf(InternalPartyPart.class);
		if (askedForParts != null) {
			askedForNonCacheableParts.addAll(askedForParts);
			askedForNonCacheableParts.removeAll(NON_CACHED_PARTS);
		}
		return askedForNonCacheableParts;
	}
}
