package ch.vd.uniregctb.registrefoncier.situation.surcharge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.registrefoncier.CapitastraURLProvider;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;

/**
 * Données résumées d'un immeuble RF.
 */
public class ImmeubleSummaryView {

	/**
	 * Identifiant technique de l'immeuble au registre foncier.
	 */
	private String idRF;

	/**
	 * Identifiant fédéral de l'immeuble.
	 */
	private String egrid;

	/**
	 * URL vers Intercapi
	 */
	private String urlIntercapi;

	public ImmeubleSummaryView(@NotNull ImmeubleRF immeuble, @Nullable CapitastraURLProvider capitastraURLProvider) {
		this.idRF = immeuble.getIdRF();
		this.egrid = immeuble.getEgrid();
		this.urlIntercapi = (capitastraURLProvider == null ? null : capitastraURLProvider.apply(immeuble.getId()));
	}

	public String getIdRF() {
		return idRF;
	}

	public String getEgrid() {
		return egrid;
	}

	public String getUrlIntercapi() {
		return urlIntercapi;
	}
}
