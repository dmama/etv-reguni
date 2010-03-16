package ch.vd.uniregctb.tiers.view;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Critères de recherche pour les tiers.
 */
public class TiersCriteriaView extends TiersCriteria {

	/**
	 *
	 */
	private static final long serialVersionUID = 6704538187645788412L;

	/**
	 * URL TAO
	 */
	private String urlTaoPP;

	/**
	 * URL TAO
	 */
	private String urlTaoBA;

	/**
	 * URL SIPF
	 */
	private String urlSipf;

	/**
	 * URL TAO
	 */
	private String urlCat;

	/**
	 * URL SIPF
	 */
	private String urlRegView;

	/**
	 * Numero sourcier
	 */
	private Long numeroSourcier;

	/**
	 * Numero debiteur
	 */
	private Long numeroDebiteur;

	/**
	 * Numero non habitant
	 */
	private Long numeroNonHabitant;

	/**
	 * Numero habitant
	 */
	private Long numeroHabitant;

	/**
	 * Numero premiere personne
	 */
	private Long numeroPremierePersonne;

	/**
	 * Numero seconde personne
	 */
	private Long numeroSecondePersonne;

	private String forAll;

	private String modeImpositionAsString;

	public String getForAll() {
		return forAll;
	}

	public void setForAll(String forAll) {
		this.forAll = forAll;
	}

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false autrement.
	 */
	@Override
	public boolean isEmpty() {
		return super.isEmpty()
				&& (getNumeroFormatte() == null || "".equals(getNumeroFormatte()));
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}


	private String numeroFormatte;

	/**
	 * @return the numero formatte
	 */
	public String getNumeroFormatte() {
		return this.numeroFormatte;
	}

	public void setNumeroFormatte(String numeroFormatte) {

		if (StringUtils.isNotEmpty(numeroFormatte)) {
			try {
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroFormatte));
				setNumero(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumero(null);
		}
		this.numeroFormatte = numeroFormatte;
	}

	public String getUrlTaoPP() {
		return urlTaoPP;
	}

	public void setUrlTaoPP(String urlTaoPP) {
		this.urlTaoPP = urlTaoPP;
	}

	public String getUrlTaoBA() {
		return urlTaoBA;
	}

	public void setUrlTaoBA(String urlTaoBA) {
		this.urlTaoBA = urlTaoBA;
	}

	public String getUrlSipf() {
		return urlSipf;
	}

	public void setUrlSipf(String urlSipf) {
		this.urlSipf = urlSipf;
	}

	public String getUrlCat() {
		return urlCat;
	}

	public void setUrlCat(String urlCAT) {
		this.urlCat = urlCAT;
	}

	public String getUrlRegView() {
		return urlRegView;
	}

	public void setUrlRegView(String urlRegView) {
		this.urlRegView = urlRegView;
	}

	public Long getNumeroSourcier() {
		return numeroSourcier;
	}

	public void setNumeroSourcier(Long numeroSourcier) {
		this.numeroSourcier = numeroSourcier;
	}

	public Long getNumeroDebiteur() {
		return numeroDebiteur;
	}

	public void setNumeroDebiteur(Long numeroDebiteur) {
		this.numeroDebiteur = numeroDebiteur;
	}

	public Long getNumeroNonHabitant() {
		return numeroNonHabitant;
	}

	public void setNumeroNonHabitant(Long numeroNonHabitant) {
		this.numeroNonHabitant = numeroNonHabitant;
	}

	public Long getNumeroHabitant() {
		return numeroHabitant;
	}

	public void setNumeroHabitant(Long numeroHabitant) {
		this.numeroHabitant = numeroHabitant;
	}

	public Long getNumeroPremierePersonne() {
		return numeroPremierePersonne;
	}

	public void setNumeroPremierePersonne(Long numeroPremierePersonne) {
		this.numeroPremierePersonne = numeroPremierePersonne;
	}

	public Long getNumeroSecondePersonne() {
		return numeroSecondePersonne;
	}

	public void setNumeroSecondePersonne(Long numeroSecondePersonne) {
		this.numeroSecondePersonne = numeroSecondePersonne;
	}

	public String getModeImpositionAsString() {
		return modeImpositionAsString;
	}

	public void setModeImpositionAsString(String modeImpositionAsString) {
		if (!"TOUS".equals(modeImpositionAsString)) {
			setModeImposition(ModeImposition.valueOf(modeImpositionAsString));
		}
		else {
			setModeImposition(null);
		}
		this.modeImpositionAsString = modeImpositionAsString;
	}

}
