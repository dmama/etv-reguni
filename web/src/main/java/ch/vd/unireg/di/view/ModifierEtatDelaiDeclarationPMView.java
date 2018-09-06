package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DelaiDeclaration;

@SuppressWarnings("unused")
public class ModifierEtatDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationPMView {

	// champs du formulaire
	private Long idDelai;

	public ModifierEtatDelaiDeclarationPMView() {
	}

	public ModifierEtatDelaiDeclarationPMView(@NotNull DelaiDeclaration delai, RegDate delaiAccordeAu) {
		super((DeclarationImpotOrdinairePM) delai.getDeclaration(), delai.getDateDemande(), delaiAccordeAu, delai.getEtat());
		this.idDelai = delai.getId();
	}

	public Long getIdDelai() {
		return idDelai;
	}

	public void setIdDelai(Long idDelai) {
		this.idDelai = idDelai;
	}
}
