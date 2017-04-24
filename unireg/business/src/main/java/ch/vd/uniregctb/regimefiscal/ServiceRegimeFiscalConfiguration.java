package ch.vd.uniregctb.regimefiscal;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-05-03, <raphael.marmier@vd.ch>
 */
public interface ServiceRegimeFiscalConfiguration {

	/**
	 * Retourne le code du type de régime fiscal correspondant par configuration à la forme juridique.
	 * @param formeJuridique la forme juridique pour laquelle on cherche un code.
	 * @return le code configuré pour la forme juridique, ou <code>null</code> si aucun.
	 */
	@Nullable
	String getCodeTypeRegimeFiscal(FormeJuridiqueEntreprise formeJuridique);

	/**
	 * Détermine si le type de régime fiscal entraine une DI vaudoise optionnelle
	 */
	boolean isRegimeFiscalDiOptionnelleVd(String codeTypeRegimeFiscal);
}
