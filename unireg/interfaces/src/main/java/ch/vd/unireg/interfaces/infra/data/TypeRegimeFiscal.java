package ch.vd.unireg.interfaces.infra.data;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.CategorieEntreprise;

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
	 * @return un libellé présentable à l'utilisateur de ce règime fiscal, préfixé du code.
	 */
	String getLibelleAvecCode();

	/**
	 * @return <code>true</code> si le régime est applicable au niveau cantonal
	 */
	boolean isCantonal();

	/**
	 * @return <code>true</code> si le régime est applicable au niveau fédéral
	 */
	boolean isFederal();

	/**
	 * @return la catégorie d'entreprise déterminée par ce type de régime
	 */
	CategorieEntreprise getCategorie();

	/**
	 * @return <code>true</code> si le régime fiscal correspond à celui "en attente de détermination"
	 */
	boolean isIndetermine();

	/**
	 * @param periodeFiscale une période fiscale
	 * @return <code>true</code> si le régime fiscal correspond à une exonération fiscale pour la période fiscale donnée
	 */
	boolean isExoneration(int periodeFiscale);
	/**
	 * @param periodeFiscale une période fiscale
	 * @return <code>true</code> si le régime fiscal correspond à une exonération fiscale pour la période fiscale donnée
	 */
	boolean isExonerationIBC(int periodeFiscale);
	/**
	 * @param periodeFiscale une période fiscale
	 * @return <code>true</code> si le régime fiscal correspond à une exonération fiscale pour la période fiscale donnée
	 */
	boolean isExonerationICI(int periodeFiscale);
	/**
	 * @param periodeFiscale une période fiscale
	 * @return <code>true</code> si le régime fiscal correspond à une exonération fiscale pour la période fiscale donnée
	 */
	boolean isExonerationIFONC(int periodeFiscale);

	List<PlageExonerationFiscales> getExonerationsIBC();

	List<PlageExonerationFiscales> getExonerationsICI();

	List<PlageExonerationFiscales> getExonerationsIFONC();


}
