package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.Address;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseEffectiveRCEnt extends AdresseRCEnt<AdresseEffectiveRCEnt> {

	private static final long serialVersionUID = 8011223968307554636L;

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return TypeAdresseCivil.COURRIER;
	}

	@Override
	public AdresseEffectiveRCEnt limitTo(RegDate dateDebut, RegDate dateFin) {
		return new AdresseEffectiveRCEnt(dateDebut == null ? getDateDebut() : dateDebut,
		                                 dateFin == null ? getDateFin() : dateFin,
		                                 this);
	}

	@Nullable
	public static AdresseEffectiveRCEnt get(DateRangeHelper.Ranged<Address> source) {
		if (source == null) {
			return null;
		}
		return new AdresseEffectiveRCEnt(source.getDateDebut(), source.getDateFin(), source.getPayload());
	}

	public AdresseEffectiveRCEnt(RegDate dateDebut, RegDate dateFin, String localite, String numeroMaison, String numeroAppartement, Integer numeroOrdrePostal, String numeroPostal, String numeroPostalComplementaire, Integer noOfsPays,
	                             String rue, String titre, Integer egid, CasePostale casePostale) {
		super(dateDebut, dateFin, localite, numeroMaison, numeroAppartement, numeroOrdrePostal, numeroPostal, numeroPostalComplementaire, noOfsPays, rue, titre, egid, casePostale);
	}

	protected AdresseEffectiveRCEnt(RegDate dateDebut, RegDate dateFin, Address address) {
		super(dateDebut, dateFin, address);
	}

	protected AdresseEffectiveRCEnt(RegDate dateDebut, RegDate dateFin, AdresseEffectiveRCEnt source) {
		super(dateDebut, dateFin, source);
	}
}
