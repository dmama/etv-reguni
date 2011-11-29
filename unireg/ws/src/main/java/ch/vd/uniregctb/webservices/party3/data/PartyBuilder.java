package ch.vd.uniregctb.webservices.party3.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.debtor.v1.Debtor;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.webservices.party3.data.strategy.CommonHouseholdStrategy;
import ch.vd.uniregctb.webservices.party3.data.strategy.CorporationStrategy;
import ch.vd.uniregctb.webservices.party3.data.strategy.DebtorStrategy;
import ch.vd.uniregctb.webservices.party3.data.strategy.NaturalPersonStrategy;
import ch.vd.uniregctb.webservices.party3.data.strategy.PartyStrategy;
import ch.vd.uniregctb.webservices.party3.impl.Context;

public class PartyBuilder {

	private static final DebtorStrategy debtorStrategy = new DebtorStrategy();

	private static final CommonHouseholdStrategy commonHouseholdStrategy = new CommonHouseholdStrategy();
	private static final NaturalPersonStrategy naturalPersonStrategy = new NaturalPersonStrategy();
	private static final CorporationStrategy corporationStrategy = new CorporationStrategy();
	private static Map<Class, PartyStrategy<?>> strategies = new HashMap<Class, PartyStrategy<?>>();

	static {
		strategies.put(Debtor.class, debtorStrategy);
		strategies.put(CommonHousehold.class, commonHouseholdStrategy);
		strategies.put(NaturalPerson.class, naturalPersonStrategy);
		strategies.put(Corporation.class, corporationStrategy);
	}

	public static NaturalPerson newNaturalPerson(ch.vd.uniregctb.tiers.PersonnePhysique right, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		return naturalPersonStrategy.newFrom(right, parts, context);
	}

	public static CommonHousehold newCommonHousehold(ch.vd.uniregctb.tiers.MenageCommun right, Set<PartyPart> parts, Context context) throws WebServiceException {
		return commonHouseholdStrategy.newFrom(right, parts, context);
	}

	public static Debtor newDebtor(DebiteurPrestationImposable right, Set<PartyPart> parts, Context context) throws WebServiceException {
		return debtorStrategy.newFrom(right, parts, context);
	}

	public static Corporation newCorporation(Entreprise entreprise, Set<PartyPart> parts, Context context) throws WebServiceException {
		return corporationStrategy.newFrom(entreprise, parts, context);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Party> void copyParts(T to, T from, Set<PartyPart> parts) {
		final PartyStrategy<T> s = (PartyStrategy<T>) strategies.get(to.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + to.getClass() + "]");
		}
		s.copyParts(to, from, parts);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Party> T clone(T tiers, Set<PartyPart> parts) {
		final PartyStrategy<T> s = (PartyStrategy<T>) strategies.get(tiers.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + tiers.getClass() + "]");
		}
		return s.clone(tiers, parts);
	}
}
