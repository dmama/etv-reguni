package ch.vd.unireg.interfaces.infra.data;

import org.jetbrains.annotations.Nullable;

public interface TypeRegimeFiscal {

	/**
	 * @return le code du régime fiscal (clé technique/métier)
	 */
	String getCode();

	/**
	 * @return la première période fiscale de validité du régime
	 */
	Integer getPremierePeriodeFiscaleValidite();

	/**
	 * @return la dernière période fiscale de validité du règime (<code>null</code> si régime toujours valide)
	 */
	@Nullable
	Integer getDernierePeriodeFiscaleValidite();

	/**
	 * @return un libellé présentable à l'utilisateur de ce règime fiscal
	 */
	String getLibelle();

	/**
	 * @return <code>true</code> si le régime est applicable au niveau cantonal
	 */
	boolean isCantonal();

	/**
	 * @return <code>true</code> si le régime est applicable au niveau fédéral
	 */
	boolean isFederal();

	/**
	 * @return <code>true</code> si le régime est applicable aux PM
	 */
	boolean isPourPM();

	/**
	 * @return <code>true</code> si le régime est applicable aux APM
	 */
	boolean isPourAPM();

	/**
	 * @return <code>true</code> si le régime est le régime par défaut des PM
	 */
	boolean isDefaultPourPM();

	/**
	 * @return <code>true</code> si le régime est le régime par défaut des APM
	 */
	boolean isDefaultPourAPM();

	/**
	 * @param periodeFiscale une période fiscale
	 * @return <code>true</code> si le régime fiscal correspond à une exonération fiscale pour la période fiscale donnée
	 */
	boolean isExoneration(int periodeFiscale);
}
