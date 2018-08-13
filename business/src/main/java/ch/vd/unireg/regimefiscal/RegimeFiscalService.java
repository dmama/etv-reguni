package ch.vd.unireg.regimefiscal;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/**
 * Service qui permet de récupérer et de manipuler les types de régimes fiscaux disponibles dans l'infrastructure fiscale. Les types de régimes fiscaux peuvent varier dans le temps (précisément, de nouveaux types peuvent être ajoutés indépendament
 * des mises-en-production), c'est pourquoi ils ne sont pas définis par une énumération.
 */
public interface RegimeFiscalService {

	/**
	 * @param code le code métier (01, 109, 41, 41C, 70, ...) qui identifie un type de régime fiscal
	 * @return le type de régime fiscal correspondant au code, ou null si le code ne correspond à rien.
	 */
	@NotNull
	TypeRegimeFiscal getTypeRegimeFiscal(@NotNull String code);

	/**
	 * @return le type de régime fiscal indéterminé (code = 00)
	 */
	@NotNull
	TypeRegimeFiscal getTypeRegimeFiscalIndetermine();

	/**
	 * @return le type de régime fiscal société de personnes (code = 80)
	 */
	@NotNull
	TypeRegimeFiscal getTypeRegimeFiscalSocieteDePersonnes();

	/**
	 * Détermine le régime fiscal par défaut à appliquer sur une entreprise à partir de sa forme juridique.
	 *
	 * @param formeJuridique la forme juridique de l'entreprise valide à la date de référence
	 * @param dateReference  la date de validité de la forme juridique, si nulle la date est considérée comme le début des temps.
	 * @return le type de régime fiscal demandé et sa plage de validité
	 */
	@NotNull
	FormeJuridiqueVersTypeRegimeFiscalMapping getFormeJuridiqueMapping(@NotNull FormeJuridiqueEntreprise formeJuridique, @Nullable RegDate dateReference);

	/**
	 * Retourne le type de régime fiscal associé au régime de portée VD de l'entreprise, s'il existe à la date donnée.
	 *
	 * @param entreprise l'entreprise
	 * @param date       la date
	 * @return le type de régime fiscal, <code>null</code> en cas d'absence de régime fiscal VD
	 */
	TypeRegimeFiscal getTypeRegimeFiscalVD(Entreprise entreprise, RegDate date);

	/**
	 * Retourne la liste triée des régimes fiscaux vaudois non annulés d'une entreprise, sous la forme d'objets consolidés.
	 *
	 * @param entreprise L'entreprise concernée
	 * @return une liste de régimes fiscaux consolidés, potentiellement vide
	 */
	@NotNull
	List<RegimeFiscalConsolide> getRegimesFiscauxVDNonAnnulesTrie(Entreprise entreprise);

	/**
	 * Détermine si le type de régime fiscal fait partie de ceux entrainant une DI optionnelle pour les entités vaudoise.
	 */
	boolean isRegimeFiscalDiOptionnelleVd(@NotNull TypeRegimeFiscal typeRegimeFiscal);

	/**
	 * @param entreprise entreprise à considérer
	 * @param genreImpot le genre d'impôt qui nous intéresse
	 * @return les périodes d'exonération avec les types d'éxonération concernés
	 */
	@NotNull
	List<ModeExonerationHisto> getExonerations(Entreprise entreprise, GenreImpotExoneration genreImpot);
}
