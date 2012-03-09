package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v3.MaritalData;
import ch.vd.registre.base.date.DateRangeHelper;
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
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.typeEtatCivil = typeEtatCivil;
	}

	/**
	 * Construit une liste d'états-civils Unireg à partir d'un état-civil RcPers.
	 *
	 * @param maritalStatus l'état-civil RcPers
	 * @return un état-civil Unireg; ou <b>null</b> si l'état-civil RcPers d'entrée était aussi <b>null</b>.
	 */
	public static List<EtatCivil> get(@Nullable MaritalData maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}

		final List<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();

		// L'état civil principal
		final RegDate dateDebut = XmlUtils.xmlcal2regdate(maritalStatus.getDateOfMaritalStatus());
		final TypeEtatCivil type = EchHelper.etatCivilFromEch11(maritalStatus.getMaritalStatus(), maritalStatus.getCancelationReason());
		etatsCivils.add(new EtatCivilRCPers(dateDebut, type));

		if (type == TypeEtatCivil.MARIE || type == TypeEtatCivil.PACS) {
			
			// On construit artificellement les états-civils 'séparé' et 'pacs interrompu' qui apparaissent
			// comme dates de séparation et de réconciliation sur l'état-civil 'marié' lui-même (ceci parce qu'ils ne
			// s'agit pas d'états-civils officiels, mais seulement une particularité fiscale).
			if (maritalStatus.getSeparation() != null && !maritalStatus.getSeparation().isEmpty()) {
				for (MaritalData.Separation separation : maritalStatus.getSeparation()) {
					final RegDate dateSeparation = XmlUtils.xmlcal2regdate(separation.getDateOfSeparation());
					final RegDate dateReconciliation = XmlUtils.xmlcal2regdate(separation.getSeparationTill());

					etatsCivils.add(new EtatCivilRCPers(dateSeparation, type == TypeEtatCivil.MARIE ? TypeEtatCivil.SEPARE : TypeEtatCivil.PACS_INTERROMPU));
					if (dateReconciliation != null) {
						etatsCivils.add(new EtatCivilRCPers(dateReconciliation, type));
					}
				}
			}
		}

		return etatsCivils;
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
