package ch.vd.uniregctb.declaration.ordinaire.common;

import java.util.Collection;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AddAndSaveHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;

public final class DelaiDeclarationAddAndSaveAccessor implements AddAndSaveHelper.EntityAccessor<DeclarationImpotOrdinaire, DelaiDeclaration> {

	public static final DelaiDeclarationAddAndSaveAccessor INSTANCE = new DelaiDeclarationAddAndSaveAccessor();

	@Override
	public Collection<DelaiDeclaration> getEntities(DeclarationImpotOrdinaire declarationImpotOrdinaire) {
		return declarationImpotOrdinaire.getDelais();
	}

	@Override
	public void addEntity(DeclarationImpotOrdinaire declaration, DelaiDeclaration d) {
		declaration.addDelai(d);
	}

	@Override
	public void assertEquals(DelaiDeclaration d1, DelaiDeclaration d2) {
		Assert.isSame(d1.getDelaiAccordeAu(), d2.getDelaiAccordeAu());
		Assert.isSame(d1.getDateDemande(), d2.getDateDemande());
		Assert.isSame(d1.getDateTraitement(), d2.getDateTraitement());
	}
};

