package ch.vd.uniregctb.migration.pm.rcent.model.history;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.CommercialRegisterData;
import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.evd0022.v1.UidRegisterData;
import ch.vd.evd0022.v1.VatRegisterData;
import ch.vd.registre.base.date.RegDate;

public class RCEntOrganisationLocationHistoryElement extends RCEntHistoryElement {

	private final OrganisationLocation location;

	public RCEntOrganisationLocationHistoryElement(RegDate beginDate, RegDate endDateDate, OrganisationLocation location) {
		super(beginDate, endDateDate);
		this.location = location;
	}

	public BigInteger getCantonalId() {
		return location.getCantonalId();
	}

	public void setUidRegisterData(UidRegisterData value) {
		location.setUidRegisterData(value);
	}

	public Identification getReplacedBy() {
		return location.getReplacedBy();
	}

	public List<String> getOtherName() {
		return location.getOtherName();
	}

	public void setSeat(SwissMunicipality value) {
		location.setSeat(value);
	}

	public UidRegisterData getUidRegisterData() {
		return location.getUidRegisterData();
	}

	public List<Identification> getInReplacementOf() {
		return location.getInReplacementOf();
	}

	public List<Identifier> getIdentifier() {
		return location.getIdentifier();
	}

	public VatRegisterData getVatRegisterData() {
		return location.getVatRegisterData();
	}

	public void setReplacedBy(Identification value) {
		location.setReplacedBy(value);
	}

	public void setVatRegisterData(VatRegisterData value) {
		location.setVatRegisterData(value);
	}

	public void setName(String value) {
		location.setName(value);
	}

	public List<Function> getFunction() {
		return location.getFunction();
	}

	public String getNogaCode() {
		return location.getNogaCode();
	}

	public KindOfLocation getKindOfLocation() {
		return location.getKindOfLocation();
	}

	public SwissMunicipality getSeat() {
		return location.getSeat();
	}

	public void setKindOfLocation(KindOfLocation value) {
		location.setKindOfLocation(value);
	}

	public String getName() {
		return location.getName();
	}

	public void setCantonalId(BigInteger value) {
		location.setCantonalId(value);
	}

	public CommercialRegisterData getCommercialRegisterData() {
		return location.getCommercialRegisterData();
	}

	public void setCommercialRegisterData(CommercialRegisterData value) {
		location.setCommercialRegisterData(value);
	}

	public void setNogaCode(String value) {
		location.setNogaCode(value);
	}
}
