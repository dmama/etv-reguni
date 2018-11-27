package ch.vd.unireg.param.view;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPM;
import ch.vd.unireg.type.delai.Delai;

public class DelaisAccordablesOnlinePMView {

	private int index;
	private Delai delaiDebut;
	private Delai delai1DemandeUnitaire;
	private Delai delai2DemandeUnitaire;
	private Delai delai1DemandeGroupee;
	private Delai delai2DemandeGroupee;

	public DelaisAccordablesOnlinePMView() {
	}

	public DelaisAccordablesOnlinePMView(@NotNull DelaisAccordablesOnlineDIPM right) {
		this.index = right.getIndex();
		this.delaiDebut = right.getDelaiDebut();

		// pour l'instant, l'IHM ne permet que de voir/définir 2 délais par type de demande (même si le modèle en permet plus)
		final List<Delai> delaisUnitaires = right.getDelaisDemandeUnitaire();
		final int sizeUnitaires = delaisUnitaires.size();
		if (sizeUnitaires < 1) {
			this.delai1DemandeUnitaire = null;
			this.delai2DemandeUnitaire = null;
		}
		else if (sizeUnitaires > 1) {
			this.delai1DemandeUnitaire = delaisUnitaires.get(0);
			this.delai2DemandeUnitaire = delaisUnitaires.get(1);
		}
		else {
			this.delai1DemandeUnitaire = delaisUnitaires.get(0);
			this.delai2DemandeUnitaire = null;
		}

		final List<Delai> delaisGroupees = right.getDelaisDemandeGroupee();
		final int sizeGroupees = delaisGroupees.size();
		if (sizeGroupees < 1) {
			this.delai1DemandeGroupee = null;
			this.delai2DemandeGroupee = null;
		}
		else if (sizeGroupees > 1) {
			this.delai1DemandeGroupee = delaisGroupees.get(0);
			this.delai2DemandeGroupee = delaisGroupees.get(1);
		}
		else {
			this.delai1DemandeGroupee = delaisGroupees.get(0);
			this.delai2DemandeGroupee = null;
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Delai getDelaiDebut() {
		return delaiDebut;
	}

	public void setDelaiDebut(Delai delaiDebut) {
		this.delaiDebut = delaiDebut;
	}

	public Delai getDelai1DemandeUnitaire() {
		return delai1DemandeUnitaire;
	}

	public void setDelai1DemandeUnitaire(Delai delai1DemandeUnitaire) {
		this.delai1DemandeUnitaire = delai1DemandeUnitaire;
	}

	public Delai getDelai2DemandeUnitaire() {
		return delai2DemandeUnitaire;
	}

	public void setDelai2DemandeUnitaire(Delai delai2DemandeUnitaire) {
		this.delai2DemandeUnitaire = delai2DemandeUnitaire;
	}

	public Delai getDelai1DemandeGroupee() {
		return delai1DemandeGroupee;
	}

	public void setDelai1DemandeGroupee(Delai delai1DemandeGroupee) {
		this.delai1DemandeGroupee = delai1DemandeGroupee;
	}

	public Delai getDelai2DemandeGroupee() {
		return delai2DemandeGroupee;
	}

	public void setDelai2DemandeGroupee(Delai delai2DemandeGroupee) {
		this.delai2DemandeGroupee = delai2DemandeGroupee;
	}
}
