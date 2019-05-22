package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;

public class PaysImpl extends EntiteOFSImpl implements Pays, Serializable {

	private static final DateRange ETERNITY = new DateRangeHelper.Range(null, null);

	private static final long serialVersionUID = 3230761033923586871L;

	private final boolean valide;
	private final DateRange validityRange;
	private final boolean etatSouverain;
	private final Integer ofsEtatSouverainParent;
	private final String codeIso2;
	private final String codeIso3;
	private final TypeAffranchissement typeAffranchissement;

	public static Pays get(ch.vd.evd0007.v1.Country target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target.getCountry(), target.getCountryAddOn(), target.getValidityDates());
	}

	private static DateRange buildValidityRangeBasedOnFlagAndDateOfChange(ch.ech.ech0072.v1.Country target) {
		if (!target.isEntryValid()) {
			return new DateRangeHelper.Range(null, XmlUtils.xmlcal2regdate(target.getDateOfChange()));
		}
		else {
			return ETERNITY;
		}
	}

	private static DateRange buildValidityRange(ch.ech.ech0072.v1.Country target, ch.vd.evd0007.v1.ValidityDate validityDates) {
		final DateRange validityRange;
		if (validityDates != null) {
			final RegDate dateDebutValidite = XmlUtils.xmlcal2regdate(validityDates.getAdmissionDate());
			final RegDate dateFinValidite = XmlUtils.xmlcal2regdate(validityDates.getAbolitionDate());
			if (dateDebutValidite == null && dateFinValidite == null) {
				validityRange = buildValidityRangeBasedOnFlagAndDateOfChange(target);
			}
			else {
				validityRange = new DateRangeHelper.Range(dateDebutValidite, dateFinValidite);
			}
		}
		else {
			validityRange = buildValidityRangeBasedOnFlagAndDateOfChange(target);
		}
		return validityRange;
	}

	public PaysImpl(ch.ech.ech0072.v1.Country target, ch.vd.evd0007.v1.CountryAddOn addOn, ch.vd.evd0007.v1.ValidityDate validityDates) {
		super(target.getId(), target.getShortNameFr(), target.getOfficialNameFr(), target.getIso2Id());
		this.valide = target.isEntryValid();
		this.validityRange = buildValidityRange(target, validityDates);
		this.etatSouverain = target.isState();
		this.ofsEtatSouverainParent = target.getAreaState();
		this.codeIso2 = target.getIso2Id();
		this.codeIso3 = target.getIso3Id();

		if (addOn != null) {
			switch (addOn.getSwissPostPriceZone()) {
				case 1:
					this.typeAffranchissement = TypeAffranchissement.SUISSE;
					break;
				case 2:
					this.typeAffranchissement = TypeAffranchissement.EUROPE;
					break;
				case 0:     // utilisé apparemment par FiDoR pour les pays bidons ajoutés ("Apatridie" et "Pays Inconnu"), voir SIFISC-8754
				case 3:
					this.typeAffranchissement = TypeAffranchissement.MONDE;
					break;
				default:
					throw new IllegalArgumentException("Unsupported swiss post price zone : " + addOn.getSwissPostPriceZone() + " for country " + getNoOFS() + " (" + getNomCourt() + ")");
			}
		}
		else {
			// dans le doute, on paie le prix fort !
			this.typeAffranchissement = TypeAffranchissement.MONDE;
		}
	}

	@Override
	public boolean isSuisse() {
		return isSuisse(getNoOFS());
	}

	private static boolean isSuisse(int noOfs) {
		return noOfs == InfrastructureConnector.noOfsSuisse;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public String getCodeIso2() {
		return codeIso2;
	}

	@Override
	public String getCodeIso3() {
		return codeIso3;
	}

	@Override
	public boolean isEtatSouverain() {
		return etatSouverain;
	}

	@Override
	public int getNoOfsEtatSouverain() {
		return ofsEtatSouverainParent != null ? ofsEtatSouverainParent : getNoOFS();
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return valide && validityRange.isValidAt(date);
	}

	@Override
	public RegDate getDateDebut() {
		return validityRange.getDateDebut();
	}

	@Override
	public RegDate getDateFin() {
		return validityRange.getDateFin();
	}

	@Override
	public TypeAffranchissement getTypeAffranchissement() {
		return typeAffranchissement;
	}

	@Override
	protected String getMemberString() {
		return String.format("%s, dateDebut=%s, dateFin=%s, etatSouverain=%b, ofsParent=%s, iso2=%s, iso3=%s, affranchissement=%s",
		                     super.getMemberString(), validityRange.getDateDebut(), validityRange.getDateFin(), etatSouverain, ofsEtatSouverainParent,
		                     buildQuotedString(codeIso2), buildQuotedString(codeIso3), typeAffranchissement);
	}
}
