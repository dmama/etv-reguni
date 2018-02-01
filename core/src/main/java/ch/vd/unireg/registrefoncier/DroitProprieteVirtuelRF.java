package ch.vd.unireg.registrefoncier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Droit de propriété virtuel généré à la volée pour un tiers RF donné. Par rapport à un droit de propriété standard, ce droit possède les caractéristiques suivantes :
 * <ul>
 * <li>il n'est pas persisté</li>
 * <li>il ne possède pas de régime de propriété</li>
 * <li>il ne possède pas de part de propriété</li>
 * <li>il ne possède pas de raison d'acquisition</li>
 * <li>il possède en plus le chemin vers l'immeuble concerné</li>
 * </ul>
 */
public class DroitProprieteVirtuelRF extends DroitVirtuelTransitifRF {

	/**
	 * Si renseigné, la communauté à travers laquelle l'ayant-droit possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

	@Nullable
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(@Nullable CommunauteRF communaute) {
		this.communaute = communaute;
	}

	@Override
	public @NotNull TypeDroit getTypeDroit() {
		return TypeDroit.DROIT_PROPRIETE;
	}
}
