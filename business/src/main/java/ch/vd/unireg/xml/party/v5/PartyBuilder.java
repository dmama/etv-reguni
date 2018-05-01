package ch.vd.unireg.xml.party.v5;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.adminauth.v5.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.debtor.v5.Debtor;
import ch.vd.unireg.xml.party.establishment.v2.Establishment;
import ch.vd.unireg.xml.party.othercomm.v3.OtherCommunity;
import ch.vd.unireg.xml.party.person.v5.CommonHousehold;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.v5.strategy.AdminAuthStrategy;
import ch.vd.unireg.xml.party.v5.strategy.CommonHouseholdStrategy;
import ch.vd.unireg.xml.party.v5.strategy.CorporationStrategy;
import ch.vd.unireg.xml.party.v5.strategy.DebtorStrategy;
import ch.vd.unireg.xml.party.v5.strategy.EstablishmentStrategy;
import ch.vd.unireg.xml.party.v5.strategy.NaturalPersonStrategy;
import ch.vd.unireg.xml.party.v5.strategy.OtherCommunityStrategy;
import ch.vd.unireg.xml.party.v5.strategy.PartyStrategy;

public class PartyBuilder {

	private static final DebtorStrategy debtorStrategy = new DebtorStrategy();
	private static final CommonHouseholdStrategy commonHouseholdStrategy = new CommonHouseholdStrategy();
	private static final NaturalPersonStrategy naturalPersonStrategy = new NaturalPersonStrategy();
	private static final CorporationStrategy corporationStrategy = new CorporationStrategy();
	private static final AdminAuthStrategy adminAuthStrategy = new AdminAuthStrategy();
	private static final OtherCommunityStrategy otherCommunityStrategy = new OtherCommunityStrategy();
	private static final EstablishmentStrategy establishmentStrategy = new EstablishmentStrategy();
	private static final Map<Class<?>, PartyStrategy<?>> strategies;

	static {
		final Map<Class<?>, PartyStrategy<?>> map = new HashMap<>();
		registerStrategy(map, Debtor.class, debtorStrategy);
		registerStrategy(map, CommonHousehold.class, commonHouseholdStrategy);
		registerStrategy(map, NaturalPerson.class, naturalPersonStrategy);
		registerStrategy(map, Corporation.class, corporationStrategy);
		registerStrategy(map, AdministrativeAuthority.class, adminAuthStrategy);
		registerStrategy(map, OtherCommunity.class, otherCommunityStrategy);
		registerStrategy(map, Establishment.class, establishmentStrategy);
		strategies = map;
	}

	private static <T extends Party> void registerStrategy(Map<Class<?>, PartyStrategy<?>> map, Class<T> clazz, PartyStrategy<T> strategy) {
		map.put(clazz, strategy);
	}

	public static NaturalPerson newNaturalPerson(ch.vd.unireg.tiers.PersonnePhysique right, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return naturalPersonStrategy.newFrom(right, parts, context);
	}

	public static CommonHousehold newCommonHousehold(ch.vd.unireg.tiers.MenageCommun right, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return commonHouseholdStrategy.newFrom(right, parts, context);
	}

	public static Debtor newDebtor(DebiteurPrestationImposable right, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return debtorStrategy.newFrom(right, parts, context);
	}

	public static Corporation newCorporation(Entreprise entreprise, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return corporationStrategy.newFrom(entreprise, parts, context);
	}

	public static AdministrativeAuthority newAdministrativeAuthority(CollectiviteAdministrative coladm, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return adminAuthStrategy.newFrom(coladm, parts, context);
	}

	public static OtherCommunity newOtherCommunity(AutreCommunaute autreCommunaute, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return otherCommunityStrategy.newFrom(autreCommunaute, parts, context);
	}

	public static Establishment newEstablishment(Etablissement etablissement, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		return establishmentStrategy.newFrom(etablissement, parts, context);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Party> void copyParts(T to, T from, Set<InternalPartyPart> parts) {
		final PartyStrategy<T> s = (PartyStrategy<T>) strategies.get(to.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + to.getClass() + ']');
		}
		s.copyParts(to, from, parts);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Party> T clone(T tiers, Set<InternalPartyPart> parts) {
		final PartyStrategy<T> s = (PartyStrategy<T>) strategies.get(tiers.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + tiers.getClass() + ']');
		}
		return s.clone(tiers, parts);
	}
}
