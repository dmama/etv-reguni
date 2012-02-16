package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.ech.ech0011.v5.MaritalData;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

public class EtatCivilRCPers implements EtatCivil, Serializable {

	private static final long serialVersionUID = -1914075129676407885L;

	private final RegDate dateDebut;
	private RegDate dateFin;
	private final TypeEtatCivil typeEtatCivil;

	private EtatCivilRCPers(RegDate dateDebut, TypeEtatCivil typeEtatCivil) {
		this.dateDebut = dateDebut;
		this.dateFin = null;
		this.typeEtatCivil = typeEtatCivil;
	}

	/**
	 * Construit un état-civil Unireg à partir d'un état-civil RcPers.
	 *
	 * @param maritalStatus l'état-civil RcPers
	 * @return un état-civil Unireg; ou <b>null</b> si l'état-civil RcPers d'entrée était aussi <b>null</b>.
	 */
	public static EtatCivil get(@Nullable MaritalData maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}

		final EtatCivil etatCivil;

		// L'état civil principal
		final RegDate dateDebut = XmlUtils.xmlcal2regdate(maritalStatus.getDateOfMaritalStatus());
		final TypeEtatCivil type = EchHelper.etatCivilFromEch11(maritalStatus.getMaritalStatus(), maritalStatus.getCancelationReason());

		if (type == TypeEtatCivil.MARIE || type == TypeEtatCivil.PACS) {
			// On construit artificellement les états-civils 'séparé' et 'pacs interrompu' qui apparaissent
			// comme dates de séparation et de réconciliation sur l'état-civil 'marié' lui-même (ceci parce qu'ils ne
			// s'agit pas d'états-civils officiels, mais seulement une particularité fiscale).
			final RegDate dateReconciliation = XmlUtils.xmlcal2regdate(maritalStatus.getSeparationTill());
			final RegDate dateSeparation = XmlUtils.xmlcal2regdate(maritalStatus.getDateOfSeparation());

			if (dateReconciliation != null) {
				etatCivil = new EtatCivilRCPers(dateReconciliation, type);
			}
			else if (dateSeparation != null) {
				etatCivil = new EtatCivilRCPers(dateSeparation, type == TypeEtatCivil.MARIE ? TypeEtatCivil.SEPARE : TypeEtatCivil.PACS_INTERROMPU);
			}
			else {
				etatCivil = new EtatCivilRCPers(dateDebut, type);
			}
		}
		else {
			etatCivil = new EtatCivilRCPers(dateDebut, type);
		}

		return etatCivil;
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
