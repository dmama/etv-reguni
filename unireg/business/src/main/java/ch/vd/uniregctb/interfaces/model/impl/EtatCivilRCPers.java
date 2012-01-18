package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.ech.ech0011.v5.MaritalData;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

public class EtatCivilRCPers implements EtatCivil, Serializable {

	private static final long serialVersionUID = -3154801553713624662L;
	
	private final RegDate dateDebut;
	private RegDate dateFin;
	private final TypeEtatCivil typeEtatCivil;

	public EtatCivilRCPers(MaritalData maritalStatus) {
		this.dateDebut = XmlUtils.xmlcal2regdate(maritalStatus.getDateOfMaritalStatus());
		this.dateFin = null;
		this.typeEtatCivil = initiTypeEtatCivil(maritalStatus);
	}

	private static TypeEtatCivil initiTypeEtatCivil(MaritalData maritalStatus) {
		TypeEtatCivil type = EchHelper.etatCivilFromEch11(maritalStatus.getMaritalStatus());
		if (type == TypeEtatCivil.MARIE && maritalStatus.getSeparation() != null) {
			type = TypeEtatCivil.SEPARE;
		}
		else if (type == TypeEtatCivil.PACS && maritalStatus.getSeparation() != null) {
			type = TypeEtatCivil.PACS_INTERROMPU;
		}
		return type;
	}

	public static EtatCivil get(MaritalData maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}
		return new EtatCivilRCPers(maritalStatus);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public TypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
