package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.Address;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseBoitePostaleRCEnt extends AdresseRCEnt<AdresseBoitePostaleRCEnt> {

	private static final long serialVersionUID = -1383393360848528413L;

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return TypeAdresseCivil.CASE_POSTALE;
	}

	@Override
	public AdresseBoitePostaleRCEnt limitTo(RegDate dateDebut, RegDate dateFin) {
		return new AdresseBoitePostaleRCEnt(dateDebut == null ? getDateDebut() : dateDebut,
		                                    dateFin == null ? getDateFin() : dateFin,
		                                    this);
	}

	@Nullable
	public static AdresseBoitePostaleRCEnt get(DateRangeHelper.Ranged<Address> source) {
		if (source == null) {
			return null;
		}
		return new AdresseBoitePostaleRCEnt(source.getDateDebut(), source.getDateFin(), source.getPayload());
	}

	public AdresseBoitePostaleRCEnt(RegDate dateDebut, RegDate dateFin, String localite, String numeroMaison, String numeroAppartement, Integer numeroOrdrePostal, String numeroPostal, String numeroPostalComplementaire, Integer noOfsPays,
	                                String rue, String titre, Integer egid, CasePostale casePostale) {
		super(dateDebut, dateFin, localite, numeroMaison, numeroAppartement, numeroOrdrePostal, numeroPostal, numeroPostalComplementaire, noOfsPays, rue, titre, egid, casePostale);
	}

	protected AdresseBoitePostaleRCEnt(RegDate dateDebut, RegDate dateFin, Address address) {
		super(dateDebut, dateFin, address);
	}

	protected AdresseBoitePostaleRCEnt(RegDate dateDebut, RegDate dateFin, AdresseBoitePostaleRCEnt source) {
		super(dateDebut, dateFin, source);
	}
}
