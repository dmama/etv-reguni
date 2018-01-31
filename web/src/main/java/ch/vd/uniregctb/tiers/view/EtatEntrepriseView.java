package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

public class EtatEntrepriseView implements Annulable {

	private final Long id;
	private final RegDate dateObtention;
	private final TypeEtatEntreprise type;
	private final TypeGenerationEtatEntreprise generation;
	private final boolean annule;
	private boolean dernierElement;

	public EtatEntrepriseView(Long id, RegDate dateObtention, TypeEtatEntreprise type, TypeGenerationEtatEntreprise generation, boolean annule) {
		this.id = id;
		this.dateObtention = dateObtention;
		this.type = type;
		this.generation = generation;
		this.annule = annule;
	}

	public Long getId() {
		return id;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	public TypeEtatEntreprise getType() {
		return type;
	}

	public TypeGenerationEtatEntreprise getGeneration() {
		return generation;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public boolean isDernierElement() {
		return dernierElement;
	}

	public void setDernierElement(boolean dernierElement) {
		this.dernierElement = dernierElement;
	}
}
