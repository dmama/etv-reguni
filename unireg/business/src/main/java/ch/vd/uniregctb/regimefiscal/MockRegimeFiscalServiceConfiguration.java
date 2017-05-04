package ch.vd.uniregctb.regimefiscal;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-05-03, <raphael.marmier@vd.ch>
 */
public class MockRegimeFiscalServiceConfiguration implements RegimeFiscalServiceConfiguration {
	@Override
	public @Nullable String getCodeTypeRegimeFiscal(FormeJuridiqueEntreprise formeJuridique) {
		throw new UnsupportedOperationException("Le mapping des types de régimes fiscaux pour les formes juridiques n'est pas supporté et partant, n'est pas configuré.");
	}

	@Override
	public boolean isRegimeFiscalDiOptionnelleVd(String codeTypeRegimeFiscal) {
		throw new UnsupportedOperationException("La détermination des types de régimes fiscaux pour lesquels la DI vaudoise est optionnelle n'est pas supportée et partant, n'est pas configurés.");
	}
}
