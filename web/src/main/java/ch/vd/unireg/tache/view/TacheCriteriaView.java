package ch.vd.uniregctb.tache.view;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEtatTache;

public class TacheCriteriaView extends TacheCriteriaViewBase {

	private static final long serialVersionUID = 4001757624155437937L;

	private String numeroFormate;

	public TacheCriteriaView() {
		this.setEtatTache(TypeEtatTache.EN_INSTANCE);
		this.setOfficeImpot(getDefaultOID());
	}

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

	private static String getDefaultOID() {
		final Integer officeImpot = AuthenticationHelper.getCurrentOID();
		if (officeImpot == null || officeImpot == ServiceInfrastructureService.noACI) {
			return null;
		}
		return officeImpot.toString();
	}
}
