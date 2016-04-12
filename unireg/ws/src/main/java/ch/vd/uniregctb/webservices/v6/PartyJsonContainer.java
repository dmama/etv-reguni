package ch.vd.uniregctb.webservices.v6;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.adminauth.v4.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v4.Corporation;
import ch.vd.unireg.xml.party.debtor.v4.Debtor;
import ch.vd.unireg.xml.party.establishment.v1.Establishment;
import ch.vd.unireg.xml.party.othercomm.v2.OtherCommunity;
import ch.vd.unireg.xml.party.person.v4.CommonHousehold;
import ch.vd.unireg.xml.party.person.v4.NaturalPerson;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclaration;
import ch.vd.unireg.xml.party.taxpayer.v4.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v3.TaxLiability;
import ch.vd.unireg.xml.party.v4.Party;

public class PartyJsonContainer {

	private final NaturalPerson naturalPerson;
	private final CommonHousehold commonHousehold;
	private final Debtor debtor;
	private final Corporation corporation;
	private final AdministrativeAuthority administrativeAuthority;
	private final OtherCommunity otherCommunity;
	private final Establishment establishment;

	private PartyJsonContainer(@NotNull NaturalPerson naturalPerson) {
		this.naturalPerson = naturalPerson;
		this.commonHousehold = null;
		this.debtor = null;
		this.corporation = null;
		this.administrativeAuthority = null;
		this.otherCommunity = null;
		this.establishment = null;
	}

	private PartyJsonContainer(@NotNull CommonHousehold household) {
		this.naturalPerson = null;
		this.commonHousehold = household;
		this.debtor = null;
		this.corporation = null;
		this.administrativeAuthority = null;
		this.otherCommunity = null;
		this.establishment = null;
	}

	private PartyJsonContainer(@NotNull Debtor debtor) {
		this.naturalPerson = null;
		this.commonHousehold = null;
		this.debtor = debtor;
		this.corporation = null;
		this.administrativeAuthority = null;
		this.otherCommunity = null;
		this.establishment = null;
	}

	private PartyJsonContainer(@NotNull Corporation corporation) {
		this.naturalPerson = null;
		this.commonHousehold = null;
		this.debtor = null;
		this.corporation = corporation;
		this.administrativeAuthority = null;
		this.otherCommunity = null;
		this.establishment = null;
	}

	private PartyJsonContainer(@NotNull AdministrativeAuthority admAuth) {
		this.naturalPerson = null;
		this.commonHousehold = null;
		this.debtor = null;
		this.corporation = null;
		this.administrativeAuthority = admAuth;
		this.otherCommunity = null;
		this.establishment = null;
	}

	private PartyJsonContainer(@NotNull OtherCommunity otherCommunity) {
		this.naturalPerson = null;
		this.commonHousehold = null;
		this.debtor = null;
		this.corporation = null;
		this.administrativeAuthority = null;
		this.otherCommunity = otherCommunity;
		this.establishment = null;
	}

	private PartyJsonContainer(@NotNull Establishment establishment) {
		this.naturalPerson = null;
		this.commonHousehold = null;
		this.debtor = null;
		this.corporation = null;
		this.administrativeAuthority = null;
		this.otherCommunity = null;
		this.establishment = establishment;
	}

	@JsonProperty(value = "naturalPerson")
	@Nullable
	public NaturalPerson getNaturalPerson() {
		return naturalPerson;
	}

	@JsonProperty(value = "commonHousehold")
	@Nullable
	public CommonHousehold getCommonHousehold() {
		return commonHousehold;
	}

	@JsonProperty(value = "debtor")
	@Nullable
	public Debtor getDebtor() {
		return debtor;
	}

	@JsonProperty(value = "corporation")
	@Nullable
	public Corporation getCorporation() {
		return corporation;
	}

	@JsonProperty(value = "administrativeAuthority")
	@Nullable
	public AdministrativeAuthority getAdministrativeAuthority() {
		return administrativeAuthority;
	}

	@JsonProperty(value = "otherCommunity")
	@Nullable
	public OtherCommunity getOtherCommunity() {
		return otherCommunity;
	}

	@JsonProperty(value = "establishment")
	@Nullable
	public Establishment getEstablishment() {
		return establishment;
	}

	private interface ContainerBuilder<T extends Party> {
		PartyJsonContainer build(T party);
	}

	private static final Map<Class<? extends Party>, ContainerBuilder<? extends Party>> BUILDERS = buildBuilders();

	private static <T extends Party> void registerBuilder(Map<Class<? extends Party>, ContainerBuilder<? extends Party>> map, Class<T> clazz, ContainerBuilder<T> builder) {
		map.put(clazz, builder);
	}

	private static Map<Class<? extends Party>, ContainerBuilder<? extends Party>> buildBuilders() {
		final Map<Class<? extends Party>, ContainerBuilder<? extends Party>> map = new HashMap<>();
		registerBuilder(map, NaturalPerson.class, new NaturalPersonContainerBuilder());
		registerBuilder(map, CommonHousehold.class, new CommonHouseholdContainerBuilder());
		registerBuilder(map, Debtor.class, new DebtorContainerBuilder());
		registerBuilder(map, Corporation.class, new CorporationContainerBuilder());
		registerBuilder(map, AdministrativeAuthority.class, new AdministrativeAuthorityContainerBuilder());
		registerBuilder(map, OtherCommunity.class, new OtherCommunityContainerBuilder());
		registerBuilder(map, Establishment.class, new EstablishmentContainerBuilder());
		return map;
	}

	private static final class NaturalPersonContainerBuilder implements ContainerBuilder<NaturalPerson> {
		@Override
		public PartyJsonContainer build(NaturalPerson naturalPerson) {
			replacePolymorphicTaxLiabilites(naturalPerson);
			replacePolymorphicTaxDeclarations(naturalPerson);
			return new PartyJsonContainer(naturalPerson);
		}
	}

	private static final class CommonHouseholdContainerBuilder implements ContainerBuilder<CommonHousehold> {
		@Override
		public PartyJsonContainer build(CommonHousehold household) {
			replacePolymorphicTaxLiabilites(household);
			replacePolymorphicTaxDeclarations(household);
			return new PartyJsonContainer(household);
		}
	}

	private static final class DebtorContainerBuilder implements ContainerBuilder<Debtor> {
		@Override
		public PartyJsonContainer build(Debtor debtor) {
			return new PartyJsonContainer(debtor);
		}
	}

	private static final class CorporationContainerBuilder implements ContainerBuilder<Corporation> {
		@Override
		public PartyJsonContainer build(Corporation corporation) {
			replacePolymorphicTaxLiabilites(corporation);
			replacePolymorphicTaxDeclarations(corporation);
			return new PartyJsonContainer(corporation);
		}
	}

	private static final class AdministrativeAuthorityContainerBuilder implements ContainerBuilder<AdministrativeAuthority> {
		@Override
		public PartyJsonContainer build(AdministrativeAuthority admAuth) {
			replacePolymorphicTaxLiabilites(admAuth);
			replacePolymorphicTaxDeclarations(admAuth);
			return new PartyJsonContainer(admAuth);
		}
	}

	private static final class OtherCommunityContainerBuilder implements ContainerBuilder<OtherCommunity> {
		@Override
		public PartyJsonContainer build(OtherCommunity otherCommunity) {
			replacePolymorphicTaxLiabilites(otherCommunity);
			replacePolymorphicTaxDeclarations(otherCommunity);
			return new PartyJsonContainer(otherCommunity);
		}
	}

	private static final class EstablishmentContainerBuilder implements ContainerBuilder<Establishment> {
		@Override
		public PartyJsonContainer build(Establishment establishment) {
			replacePolymorphicTaxLiabilites(establishment);
			replacePolymorphicTaxDeclarations(establishment);
			return new PartyJsonContainer(establishment);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Party> PartyJsonContainer fromValue(Party party) {
		final Class<? extends Party> clazz = party.getClass();
		final ContainerBuilder<T> builder = (ContainerBuilder<T>) BUILDERS.get(clazz);
		return builder.build((T) party);
	}

	private static void replacePolymorphicTaxLiabilites(Taxpayer taxpayer) {
		final List<TaxLiability> tls = taxpayer.getTaxLiabilities();
		if (tls != null) {
			for (int i = 0 ; i < tls.size() ; ++ i) {
				tls.set(i, JsonTaxLiabilityHelper.jsonEquivalentOf(tls.get(i)));
			}
		}
	}

	private static void replacePolymorphicTaxDeclarations(Party party) {
		final List<TaxDeclaration> tds = party.getTaxDeclarations();
		if (tds != null) {
			for (int i = 0 ; i < tds.size() ; ++ i) {
				tds.set(i, JsonTaxDeclarationHelper.jsonEquivalentOf(tds.get(i)));
			}
		}
	}
}
