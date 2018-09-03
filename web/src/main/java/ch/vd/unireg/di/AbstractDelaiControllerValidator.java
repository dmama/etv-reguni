package ch.vd.unireg.di;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.declaration.DelaiDeclaration;

public abstract class AbstractDelaiControllerValidator {
	public abstract DelaiDeclaration getDelaiDeclarationById(@NotNull Long idDocument);
}
