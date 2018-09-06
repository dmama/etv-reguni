package ch.vd.unireg.qsnc;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;

@SuppressWarnings("unused")
public class ModifierEtatDelaiQSNCView extends AbstractEditionDelaiQSNCView {

	private Long idDelai;

	public ModifierEtatDelaiQSNCView() {
	}

	public ModifierEtatDelaiQSNCView(@NotNull DelaiDeclaration delai, RegDate delaiAccordeAu) {
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
