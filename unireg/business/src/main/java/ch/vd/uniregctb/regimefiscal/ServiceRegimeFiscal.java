package ch.vd.uniregctb.regimefiscal;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-01-25, <raphael.marmier@vd.ch>
 */
public interface ServiceRegimeFiscal {

	/**
	 * @param code le code représentant le type de régime fiscal
	 * @return le type de régime fiscal correspondant au code, ou null si le code ne correspond à rien.
	 */
	@NotNull
	TypeRegimeFiscal getTypeRegimeFiscal(@NotNull String code);

	/**
	 * @return le type de régime fiscal indéterminé
	 */
	@NotNull
	TypeRegimeFiscal getTypeRegimeFiscalIndetermine();

	/**
	 * @return le type de régime fiscal société de personnes
	 */
	@NotNull TypeRegimeFiscal getTypeRegimeFiscalSocieteDePersonnes();

	/**
	 * @param formeJuridique la forme juridique
	 * @return le type de régime fiscal approprié pour la forme juridique
	 */
	@NotNull
	TypeRegimeFiscal getTypeRegimeFiscalParDefaut(@NotNull FormeJuridiqueEntreprise formeJuridique);

	/**
	 * Retourne le type de régime fiscal associé au régime de portée VD de l'entreprise, s'il existe à la date donnée.
	 * @param entreprise l'entreprise
	 * @param date la date
	 * @return le type de régime fiscal, <code>null</code> en cas d'absence de régime fiscal VD
	 */
	TypeRegimeFiscal getTypeRegimeFiscalVD(Entreprise entreprise, RegDate date);

	/**
	 * Retourne la liste triée des régimes fiscaux vaudois non annulés d'une entreprise, sous la forme d'objets consolidés.
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
