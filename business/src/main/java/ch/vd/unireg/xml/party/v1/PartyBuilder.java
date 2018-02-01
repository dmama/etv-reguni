package ch.vd.unireg.xml.party.v1;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.adminauth.v1.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.debtor.v1.Debtor;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.v1.strategy.AdminAuthStrategy;
import ch.vd.unireg.xml.party.v1.strategy.CommonHouseholdStrategy;
import ch.vd.unireg.xml.party.v1.strategy.CorporationStrategy;
import ch.vd.unireg.xml.party.v1.strategy.DebtorStrategy;
import ch.vd.unireg.xml.party.v1.strategy.NaturalPersonStrategy;
import ch.vd.unireg.xml.party.v1.strategy.PartyStrategy;

public class PartyBuilder {

	private static final DebtorStrategy debtorStrategy = new DebtorStrategy();

	private static final CommonHouseholdStrategy commonHouseholdStrategy = new CommonHouseholdStrategy();
	private static final NaturalPersonStrategy naturalPersonStrategy = new NaturalPersonStrategy();
	private static final CorporationStrategy corporationStrategy = new CorporationStrategy();
	private static final AdminAuthStrategy adminAuthStrategy = new AdminAuthStrategy();
	private static final Map<Class, PartyStrategy<?>> strategies = new HashMap<>();

	static {
		strategies.put(Debtor.class, debtorStrategy);
		strategies.put(CommonHousehold.class, commonHouseholdStrategy);
		strategies.put(NaturalPerson.class, naturalPersonStrategy);
		strategies.put(Corporation.class, corporationStrategy);
		strategies.put(AdministrativeAuthority.class, adminAuthStrategy);
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
