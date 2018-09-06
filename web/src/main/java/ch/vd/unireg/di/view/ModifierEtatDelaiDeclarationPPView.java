package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;

@SuppressWarnings("unused")
public class ModifierEtatDelaiDeclarationPPView extends AbstractEditionDelaiDeclarationPPView {

	// champs du formulaire
	private Long idDelai;

	public ModifierEtatDelaiDeclarationPPView() {
	}

	public ModifierEtatDelaiDeclarationPPView(@NotNull DelaiDeclaration delai, RegDate delaiAccordeAu) {
		super((DeclarationImpotOrdinairePP) delai.getDeclaration(), delai.getDateDemande(), delaiAccordeAu, delai.getEtat());
		this.idDelai = delai.getId();
	}

	public Long getIdDelai() {
		return idDelai;
	}

	public void setIdDelai(Long idDelai) {
		this.idDelai = idDelai;
	}
}
