package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.type.TypePermis;

public class PermisImpl implements Permis, Serializable {

	private static final long serialVersionUID = 2542802766807273221L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;
	private final int noSequence;
	private final TypePermis typePermis;

	public static PermisImpl get(ch.vd.registre.civil.model.Permis target) {
		if (target == null) {
			return null;
		}
		return new PermisImpl(target);
	}

	private PermisImpl(ch.vd.registre.civil.model.Permis target) {
		this.dateDebut = RegDateHelper.get(target.getDateDebutValidite());
		this.dateFin = RegDateHelper.get(target.getDateFinValidite());
		this.dateAnnulation = RegDateHelper.get(target.getDateAnnulation());
		this.noSequence = target.getNoSequence();
		this.typePermis = initTypePermis(target.getTypePermis());
	}

	private static TypePermis initTypePermis(EnumTypePermis type) {
		if (type == null) {
			return null;
		}
		if (type == EnumTypePermis.ANNUEL) {
			return TypePermis.SEJOUR;
		}
		else if (type == EnumTypePermis.ETABLLISSEMENT) {
			return TypePermis.ETABLISSEMENT;
		}
		else if (type == EnumTypePermis.COURTE_DUREE) {
			return TypePermis.COURTE_DUREE;
		}
		else if (type == EnumTypePermis.DIPLOMATE) {
			return TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE;
		}
		else if (type == EnumTypePermis.FONCTIONNAIRE_INTERNATIONAL) {
			return TypePermis.FONCT_INTER_SANS_IMMUNITE;
		}
		else if (type == EnumTypePermis.FRONTALIER) {
			return TypePermis.FRONTALIER;
		}
		else if (type == EnumTypePermis.PERSONNE_A_PROTEGER) {
			return TypePermis.PERSONNE_A_PROTEGER;
		}
		else if (type == EnumTypePermis.PROVISOIRE) {
			return TypePermis.PROVISOIRE;
		}
		else if (type == EnumTypePermis.REQUERANT_ASILE_AVANT_DECISION) {
			return TypePermis.REQUERANT_ASILE;
		}
		else if (type == EnumTypePermis.REQUERANT_ASILE_REFUSE) {
			return TypePermis.ETRANGER_ADMIS_PROVISOIREMENT;
		}
		else if (type == EnumTypePermis.SUISSE_SOURCIER) {
			return TypePermis.SUISSE_SOURCIER;
		}
		else {
			throw new IllegalArgumentException("Type de permis inconnu  = [" + type.getName() + ']');
		}
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public int getNoSequence() {
		return noSequence;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}

}
