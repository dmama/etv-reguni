package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v4.MaritalData;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatCivilRCPers implements EtatCivil, Serializable {

	private static final long serialVersionUID = -7614825259930350637L;

	private final RegDate dateDebut;
	private final TypeEtatCivil typeEtatCivil;

	private EtatCivilRCPers(RegDate dateDebut, TypeEtatCivil typeEtatCivil) {
		this.dateDebut = dateDebut;
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

		final List<EtatCivil> etatsCivils = new ArrayList<>();

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

					// [SIFISC-5840] Les "états-civils" séparés sans date de début doivent être ignorés également
					if (dateSeparation != null) {
						etatsCivils.add(new EtatCivilRCPers(dateSeparation, type == TypeEtatCivil.MARIE ? TypeEtatCivil.SEPARE : TypeEtatCivil.PACS_SEPARE));
					}
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
	public TypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}
}
