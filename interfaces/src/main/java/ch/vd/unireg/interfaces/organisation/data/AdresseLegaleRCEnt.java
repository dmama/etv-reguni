package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.Address;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseLegaleRCEnt extends AdresseRCEnt<AdresseLegaleRCEnt> {

	private static final long serialVersionUID = -8831068971401677643L;

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return TypeAdresseCivil.PRINCIPALE;
	}

	@Override
	public AdresseLegaleRCEnt limitTo(RegDate dateDebut, RegDate dateFin) {
		return new AdresseLegaleRCEnt(dateDebut == null ? getDateDebut() : dateDebut,
		                              dateFin == null ? getDateFin() : dateFin,
		                              this);
	}

	@Nullable
	public static AdresseLegaleRCEnt get(DateRangeHelper.Ranged<Address> source) {
		if (source == null) {
			return null;
		}
		return new AdresseLegaleRCEnt(source.getDateDebut(), source.getDateFin(), source.getPayload());
	}

	public AdresseLegaleRCEnt(RegDate dateDebut, RegDate dateFin, String localite, String numeroMaison, String numeroAppartement, Integer numeroOrdrePostal, String numeroPostal,
	                          String numeroPostalComplementaire, Integer noOfsPays, String rue, String titre, Integer egid, CasePostale casePostale) {
		super(dateDebut, dateFin, localite, numeroMaison, numeroAppartement, numeroOrdrePostal, numeroPostal, numeroPostalComplementaire, noOfsPays, rue, titre, egid, casePostale);
	}

	protected AdresseLegaleRCEnt(RegDate dateDebut, RegDate dateFin, Address address) {
		super(dateDebut, dateFin, address);
	}

	protected AdresseLegaleRCEnt(RegDate dateDebut, RegDate dateFin, AdresseLegaleRCEnt source) {
		super(dateDebut, dateFin, source);
	}
}
