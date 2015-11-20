package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class EtatEntrepriseView implements DateRange, Annulable {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeEtatEntreprise type;
	private final boolean annule;

	public EtatEntrepriseView(Long id, RegDate dateDebut, RegDate dateFin, TypeEtatEntreprise type, boolean annule) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
		this.annule = annule;
	}

	public Long getId() {
		return id;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public TypeEtatEntreprise getType() {
		return type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}
}
