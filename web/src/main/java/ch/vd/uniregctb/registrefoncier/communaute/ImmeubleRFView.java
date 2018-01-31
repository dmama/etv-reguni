package ch.vd.uniregctb.registrefoncier.communaute;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;

public class ImmeubleRFView {

	private final long id;
	/**
	 * L'egrid de l'immeuble
	 */
	private final String egrid;
	/**
	 * Le numéro de parcelle courant de l'immeuble
	 */
	private final String noParcelle;
	/**
	 * Le nom de la commune courante de l'immeuble
	 */
	private final String nomCommune;

	public ImmeubleRFView(@NotNull ImmeubleRF immeuble, @NotNull RegistreFoncierService registreFoncierService) {
		this.id = immeuble.getId();
		this.egrid = immeuble.getEgrid();
		this.noParcelle = registreFoncierService.getNumeroParcelleComplet(immeuble, null);
		final Commune commune = registreFoncierService.getCommune(immeuble, null);
		this.nomCommune = commune == null ? null :commune.getNomCourt();
	}

	public long getId() {
		return id;
	}

	public String getEgrid() {
		return egrid;
	}

	public String getNoParcelle() {
		return noParcelle;
	}

	public String getNomCommune() {
		return nomCommune;
	}
}
