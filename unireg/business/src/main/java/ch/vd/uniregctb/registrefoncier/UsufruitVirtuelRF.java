package ch.vd.uniregctb.registrefoncier;

import org.jetbrains.annotations.NotNull;


/**
 * Usufruit virtuel générée à la volée pour un tiers RF donné. Par rapport à un usufruit standard, possède les caractéristiques suivantes :
 * <ul>
 * <li>il n'est pas persisté</li>
 * <li>il n'a qu'un seul ayant-droit : le tiers RF donné</li>
 * <li>il n'a qu'un seul immeuble : l'immeuble pointé par le chemin</li>
 * <li>il ne possède pas d'identificant du droit</li>
 * <li>il ne possède pas de numéro d'affaire</li>
 * <li>il possède en plus le chemin vers l'immeuble concernée</li>
 * </ul>
 */
public class UsufruitVirtuelRF extends DroitVirtuelRF {

	@Override
	public @NotNull TypeDroit getTypeDroit() {
		return TypeDroit.SERVITUDE;
	}
}
