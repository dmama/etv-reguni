package ch.vd.unireg.qsnc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;


public class ModifierEtatDelaiQSNCView extends AbstractEditionDelaiQSNCView {

	private Long idDelai;

	public ModifierEtatDelaiQSNCView() {
	}


	public ModifierEtatDelaiQSNCView(DelaiDeclaration delai, RegDate delaiAccordeAu) {
		super((QuestionnaireSNC) delai.getDeclaration(), delai.getDateDemande(), delaiAccordeAu, delai.getEtat());
		this.idDelai = delai.getId();

	}

	public Long getIdDelai() {
		return idDelai;
	}

	public void setIdDelai(Long idDelai) {
		this.idDelai = idDelai;
	}
}
