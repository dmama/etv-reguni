package ch.vd.uniregctb.evenement.view;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementCriteria;

public class EvenementCriteriaView extends EvenementCriteria {

	private static final long serialVersionUID = -440782547474527432L;

	private String numeroIndividuFormatte;

	private String numeroCTBFormatte;

	public String getNumeroIndividuFormatte() {
		return numeroIndividuFormatte;
	}

	public void setNumeroIndividuFormatte(String numeroIndividuFormatte) {

		if (StringUtils.isNotEmpty(numeroIndividuFormatte)) {
			try {
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroIndividuFormatte));
				setNumeroIndividu(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroIndividuFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumeroIndividu(null);
		}
		this.numeroIndividuFormatte = numeroIndividuFormatte;
	}

	public String getNumeroCTBFormatte() {
		return numeroCTBFormatte;
	}

	public void setNumeroCTBFormatte(String numeroCTBFormatte) {
		if (StringUtils.isNotEmpty(numeroCTBFormatte)) {
			try {
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroCTBFormatte));
				setNumeroCTB(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroCTBFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumeroCTB(null);
		}
		this.numeroCTBFormatte = numeroCTBFormatte;
	}

	/**
	 * @return the dateEvenementDebut
	 */
	public Date getDateEvenementDebut() {
		return RegDate.asJavaDate(dateEvenementDebut);
	}

	/**
	 * @param dateEvenementDebut the dateEvenementDebut to set
	 */
	public void setDateEvenementDebut(Date dateEvenementDebut) {
		this.dateEvenementDebut = RegDate.get(dateEvenementDebut);
	}

	/**
	 * @return the dateEvenementDebut
	 */
	public Date getDateEvenementFin() {
		return RegDate.asJavaDate(dateEvenementFin);
	}

	/**
	 * @param dateEvenementFin the dateEvenementFin to set
	 */
	public void setDateEvenementFin(Date dateEvenementFin) {
		this.dateEvenementFin = RegDate.get(dateEvenementFin);
	}
}
