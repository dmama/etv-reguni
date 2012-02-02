package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

	private static final long serialVersionUID = -3154801553713624662L;

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
	 * <p/>
	 * <b>Note:</b> cette méthode peut retourner plusieurs états-civils Unireg pour un seul état-civil RcPers dans le cas d'une personne séparée. La raison est que l'état <i>séparé</i> n'est pas un
	 * état-civil officiel : officiellement, une personne séparée possède toujours l'état-civil <i>marié</i>. L'information qu'une personne est séparée est malgré tout transmise en supplément sur
	 * l'état-civil <i>marié</i> : l'état-civil <i>marié</i> possède en plus une date de séparation. Dans ce cas, cette méthode génère deux états-civils Unireg en retour. A noter qu'une éventuelle
	 * réconciliation après séparation fonctionne sur le même principe; dans ce cas trois états-civils Unireg seront retournés.
	 *
	 * @param maritalStatus l'état-civil RcPers
	 * @return 1, 2 voire 3 états-civils Unireg; ou <b>null</b> si l'état-civil RcPers d'entrée était aussi <b>null</b>.
	 */
	public static List<EtatCivil> get(@Nullable MaritalData maritalStatus) {
		if (maritalStatus == null) {
			return null;
		}

		final List<EtatCivil> list = new ArrayList<EtatCivil>();

		// L'état civil principal
		final RegDate dateDebut = XmlUtils.xmlcal2regdate(maritalStatus.getDateOfMaritalStatus());
		final TypeEtatCivil type = EchHelper.etatCivilFromEch11(maritalStatus.getMaritalStatus());
		list.add(new EtatCivilRCPers(dateDebut, type));

		if (type == TypeEtatCivil.MARIE || type == TypeEtatCivil.PACS) {
			// On construit artificellement les états-civils 'séparé' et 'pacs interrompu' qui apparaissent
			// comme dates de séparation et de réconciliation sur l'état-civil 'marié' lui-même (ceci parce qu'ils ne
			// s'agit pas d'états-civils officiels, mais seulement une particularité fiscale).
			final RegDate dateSeparation = XmlUtils.xmlcal2regdate(maritalStatus.getDateOfSeparation());
			if (dateSeparation != null) {
				list.add(new EtatCivilRCPers(dateSeparation, type == TypeEtatCivil.MARIE ? TypeEtatCivil.SEPARE : TypeEtatCivil.PACS_INTERROMPU));

				final RegDate dateReconciliation = XmlUtils.xmlcal2regdate(maritalStatus.getSeparationTill());
				if (dateReconciliation != null) {
					list.add(new EtatCivilRCPers(dateReconciliation, type));
				}
			}
		}

		return list;
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
