package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;

public class ModifierDemandeDelaiDeclarationView extends AbstractEditionDelaiDeclarationPMView {

	// champs du formulaire
	private Long idDelai;

	public ModifierDemandeDelaiDeclarationView() {
	}

	public ModifierDemandeDelaiDeclarationView(DelaiDeclaration delai, RegDate delaiAccordeAu) {
		super((DeclarationImpotOrdinaire) delai.getDeclaration(), delai.getDateDemande(), delaiAccordeAu, delai.getEtat());
		this.idDelai = delai.getId();
	}

	public Long getIdDelai() {
		return idDelai;
	}

	public void setIdDelai(Long idDelai) {
		this.idDelai = idDelai;
	}
}
