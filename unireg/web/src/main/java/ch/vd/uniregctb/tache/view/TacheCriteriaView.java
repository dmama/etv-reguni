package ch.vd.uniregctb.tache.view;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class TacheCriteriaView extends TacheCriteriaViewBase {

	private static final long serialVersionUID = -1923938886869425796L;


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
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroFormate));
				setNumeroCTB(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroFormate))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumeroCTB(null);
		}
		this.numeroFormate = numeroFormate;
	}


	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false autrement.
	 */
	@Override
	public boolean isEmpty() {
		boolean flag = super.isEmpty() &&
			getTypeTache() == null &&
			getAnnee() == null &&
			(getNumeroFormate() == null || "".equals(getNumeroFormate()));
		return flag;
	}
}
