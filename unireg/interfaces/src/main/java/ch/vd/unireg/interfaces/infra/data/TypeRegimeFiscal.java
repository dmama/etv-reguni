package ch.vd.unireg.interfaces.infra.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;
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
	 * @param periode période fiscale
	 * @return la plage d'exonération fiscale IBC qui touche la période donnée
	 */
	@Nullable
	PlageExonerationFiscale getExonerationIBC(int periode);

	/**
	 * @param periode période fiscale
	 * @return la plage d'exonération fiscale ICI qui touche la période donnée
	 */
	@Nullable
	PlageExonerationFiscale getExonerationICI(int periode);

	/**
	 * @param periode période fiscale
	 * @return la plage d'exonération fiscale IFONC qui touche la période donnée
	 */
	@Nullable
	PlageExonerationFiscale getExonerationIFONC(int periode);

	/**
	 * @return les plages d'exonération selon le genre d'impôt
	 */
	@NotNull
	List<PlageExonerationFiscale> getExonerations(GenreImpotExoneration genreImpot);
}
