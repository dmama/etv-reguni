package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.builders;

import java.math.BigInteger;
import java.util.List;

import ch.ech.ech0097.v2.NamedOrganisationId;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.convertor.MultivalueListConverter;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.Organisation;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationLocation;

public class OrganisationBuilder {
	private final BigInteger cantonalId;
	private final List<DateRangeHelper.Ranged<NamedOrganisationId>> organisationIdentifiers;
	private final List<DateRangeHelper.Ranged<BigInteger>> locations;

private final List<OrganisationLocation> locationsData;

	public OrganisationBuilder(BigInteger cantonalId,
	                           List<DateRangeHelper.Ranged<NamedOrganisationId>> organisationIdentifiers,
	                           List<DateRangeHelper.Ranged<BigInteger>> locations,
	                           List<OrganisationLocation> locationsData) {
		this.cantonalId = cantonalId;
		this.organisationIdentifiers = organisationIdentifiers;
		this.locations = locations;
		this.locationsData = locationsData;
	}

	public Organisation build() {
		return new Organisation(cantonalId.longValue(),
		                        MultivalueListConverter.toMapOfListsOfDateRangedValues(organisationIdentifiers, NamedOrganisationId::getOrganisationIdCategory, NamedOrganisationId::getOrganisationId),
		                        MultivalueListConverter.toMapOfListsOfDateRangedValues(locations, BigInteger::longValue, BigInteger::longValue),
		                        locationsData);
	}
}
