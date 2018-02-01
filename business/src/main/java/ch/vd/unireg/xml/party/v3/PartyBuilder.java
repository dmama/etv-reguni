package ch.vd.unireg.xml.party.v3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.adminauth.v3.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v3.Corporation;
import ch.vd.unireg.xml.party.debtor.v3.Debtor;
import ch.vd.unireg.xml.party.othercomm.v1.OtherCommunity;
import ch.vd.unireg.xml.party.person.v3.CommonHousehold;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.v3.strategy.AdminAuthStrategy;
import ch.vd.unireg.xml.party.v3.strategy.CommonHouseholdStrategy;
import ch.vd.unireg.xml.party.v3.strategy.CorporationStrategy;
import ch.vd.unireg.xml.party.v3.strategy.DebtorStrategy;
import ch.vd.unireg.xml.party.v3.strategy.NaturalPersonStrategy;
import ch.vd.unireg.xml.party.v3.strategy.OtherCommunityStrategy;
import ch.vd.unireg.xml.party.v3.strategy.PartyStrategy;

public class PartyBuilder {

	private static final DebtorStrategy debtorStrategy = new DebtorStrategy();
	private static final CommonHouseholdStrategy commonHouseholdStrategy = new CommonHouseholdStrategy();
	private static final NaturalPersonStrategy naturalPersonStrategy = new NaturalPersonStrategy();
	private static final CorporationStrategy corporationStrategy = new CorporationStrategy();
	private static final AdminAuthStrategy adminAuthStrategy = new AdminAuthStrategy();
	private static final OtherCommunityStrategy otherCommunityStrategy = new OtherCommunityStrategy();
	private static final Map<Class<?>, PartyStrategy<?>> strategies;

	static {
		final Map<Class<?>, PartyStrategy<?>> map = new HashMap<>();
		registerStrategy(map, Debtor.class, debtorStrategy);
		registerStrategy(map, CommonHousehold.class, commonHouseholdStrategy);
		registerStrategy(map, NaturalPerson.class, naturalPersonStrategy);
		registerStrategy(map, Corporation.class, corporationStrategy);
		registerStrategy(map, AdministrativeAuthority.class, adminAuthStrategy);
		registerStrategy(map, OtherCommunity.class, otherCommunityStrategy);
		strategies = map;
	}

	private static <T extends Party> void registerStrategy(Map<Class<?>, PartyStrategy<?>> map, Class<T> clazz, PartyStrategy<T> strategy) {
		map.put(clazz, strategy);
	}

	public static NaturalPerson newNaturalPerson(ch.vd.unireg.tiers.PersonnePhysique right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		return naturalPersonStrategy.newFrom(right, parts, context);
	}

	public static CommonHousehold newCommonHousehold(ch.vd.unireg.tiers.MenageCommun right, Set<PartyPart> parts, Context context) throws ServiceException {
		return commonHouseholdStrategy.newFrom(right, parts, context);
	}

	public static Debtor newDebtor(DebiteurPrestationImposable right, Set<PartyPart> parts, Context context) throws ServiceException {
		return debtorStrategy.newFrom(right, parts, context);
	}

	public static Corporation newCorporation(Entreprise entreprise, Set<PartyPart> parts, Context context) throws ServiceException {
		return corporationStrategy.newFrom(entreprise, parts, context);
	}

	public static AdministrativeAuthority newAdministrativeAuthority(CollectiviteAdministrative coladm, Set<PartyPart> parts, Context context) throws ServiceException {
		return adminAuthStrategy.newFrom(coladm, parts, context);
	}

	public static OtherCommunity newOtherCommunity(AutreCommunaute autreCommunaute, Set<PartyPart> parts, Context context) throws ServiceException {
		return otherCommunityStrategy.newFrom(autreCommunaute, parts, context);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Party> void copyParts(T to, T from, Set<PartyPart> parts) {
		final PartyStrategy<T> s = (PartyStrategy<T>) strategies.get(to.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + to.getClass() + ']');
		}
		s.copyParts(to, from, parts);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Party> T clone(T tiers, Set<PartyPart> parts) {
		final PartyStrategy<T> s = (PartyStrategy<T>) strategies.get(tiers.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + tiers.getClass() + ']');
		}
		return s.clone(tiers, parts);
	}
}
