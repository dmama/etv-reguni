package ch.vd.uniregctb.webservices.v5;


import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.uniregctb.webservices.common.JsonPolymorphicContainer;

public class PartyJsonContainer extends JsonPolymorphicContainer<Party> {

	private final PartyType dataType;

	public PartyJsonContainer(Party data) {
		super(data);
		dataType = EnumHelper.getPartyType(data);
	}

	@Override
	public String getDataType() {
		return dataType != null ? dataType.value() : "UNKNOWN";
	}
}
