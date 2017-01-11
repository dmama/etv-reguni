package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Service qui expose des méthodes d'accès aux données du registre foncier (Capistastra).
 */
public interface RegistreFoncierService {

	/**
	 * Détermine les droits sur des immeubles d'un contribuable Unireg.
	 *
	 * @param ctb un contribuable Unireg
	 * @return une liste de droits.
	 */
	@NotNull
	List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb);
}
