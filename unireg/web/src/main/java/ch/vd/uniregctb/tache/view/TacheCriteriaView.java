package ch.vd.uniregctb.tache.view;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class TacheCriteriaView extends TacheCriteriaViewBase {

	private static final long serialVersionUID = 4001757624155437937L;

	private String numeroFormate;

	/**
	 * @return the numero formatte
	 */
	public String getNumeroFormate() {
		return this.numeroFormate;
	}

	public void setNumeroFormate(String numeroFormate) {
		if (StringUtils.isNotEmpty(numeroFormate)) {
			try {
				setNumeroCTB(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroFormate))));
			}
			catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		}
		else {
			setNumeroCTB(null);
		}
		this.numeroFormate = numeroFormate;
	}

	/**
	 * @return true si aucun paramètre de recherche n'est renseigné. false autrement.
	 */
	@Override
	public boolean isEmpty() {
		return super.isEmpty() &&
				getTypeTache() == null &&
				getAnnee() == null &&
				StringUtils.isBlank(getNumeroFormate());
	}
}
