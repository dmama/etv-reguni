package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.webservices.tiers3.Debiteur;
import ch.vd.unireg.webservices.tiers3.MenageCommun;
import ch.vd.unireg.webservices.tiers3.PersonneMorale;
import ch.vd.unireg.webservices.tiers3.PersonnePhysique;
import ch.vd.unireg.webservices.tiers3.Tiers;
import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.webservices.tiers3.data.strategy.DebiteurStrategy;
import ch.vd.uniregctb.webservices.tiers3.data.strategy.MenageCommunStrategy;
import ch.vd.uniregctb.webservices.tiers3.data.strategy.PersonneMoraleStrategy;
import ch.vd.uniregctb.webservices.tiers3.data.strategy.PersonnePhysiqueStrategy;
import ch.vd.uniregctb.webservices.tiers3.data.strategy.TiersStrategy;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;

public class TiersBuilder {

	private static final DebiteurStrategy debiteurStrategy = new DebiteurStrategy();

	private static final MenageCommunStrategy menageCommunStrategy= new MenageCommunStrategy();
	private static final PersonnePhysiqueStrategy personnePhysiqueStrategy= new PersonnePhysiqueStrategy();
	private static final PersonneMoraleStrategy personneMoraleStrategy= new PersonneMoraleStrategy();
	private static Map<Class, TiersStrategy<?>> strategies = new HashMap<Class, TiersStrategy<?>>();

	static {
		strategies.put(Debiteur.class, debiteurStrategy);
		strategies.put(MenageCommun.class, menageCommunStrategy);
		strategies.put(PersonnePhysique.class, personnePhysiqueStrategy);
		strategies.put(PersonneMorale.class, personneMoraleStrategy);
	}
	public static PersonnePhysique newPersonnePhysique(ch.vd.uniregctb.tiers.PersonnePhysique right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		return personnePhysiqueStrategy.newFrom(right, parts, context);
	}

	public static MenageCommun newMenageCommun(ch.vd.uniregctb.tiers.MenageCommun right, Set<TiersPart> parts, Context context) throws WebServiceException {
		return menageCommunStrategy.newFrom(right, parts, context);
	}

	public static Debiteur newDebiteur(DebiteurPrestationImposable right, Set<TiersPart> parts, Context context) throws WebServiceException {
		return debiteurStrategy.newFrom(right, parts, context);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Tiers> void copyParts(T to, T from, Set<TiersPart> parts) {
		final TiersStrategy<T> s = (TiersStrategy<T>) strategies.get(to.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + to.getClass() + "]");
		}
		s.copyParts(to, from, parts);
	}

	@SuppressWarnings({"unchecked"})
	public static <T extends Tiers> T clone(T tiers, Set<TiersPart> parts) {
		final TiersStrategy<T> s = (TiersStrategy<T>) strategies.get(tiers.getClass());
		if (s == null) {
			throw new IllegalArgumentException("Pas de stratégie pour la classe [" + tiers.getClass() + "]");
		}
		return s.clone(tiers, parts);
	}
}
