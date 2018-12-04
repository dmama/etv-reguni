package ch.vd.unireg.registrefoncier.communaute;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.TiersService;

/**
 * Information d'un modèle de communauté pour un tiers donné.
 */
public class ModeleCommunauteForTiersView extends ModeleCommunauteView {

	private final long id;
	private final long ctbId;

	public ModeleCommunauteForTiersView(long ctbId,
	                                    @NotNull ModeleCommunauteRF modele,
	                                    @NotNull TiersService tiersService,
	                                    @NotNull RegistreFoncierService registreFoncierService) {
		super(modele, tiersService, registreFoncierService);
		this.id = modele.getId();
		this.ctbId = ctbId;
	}

	public long getId() {
		return id;
	}

	public long getCtbId() {
		return ctbId;
	}
}
