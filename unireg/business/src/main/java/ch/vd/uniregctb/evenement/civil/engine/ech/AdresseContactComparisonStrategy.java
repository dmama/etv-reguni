package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseContactComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUTE = "adresse de contact";

	private static final Comparator<Adresse> ADDRESS_COMPARATOR = new IndividuComparisonHelper.NullableComparator<Adresse>(true) {
		@Override
		protected int compareNonNull(@NotNull Adresse o1, @NotNull Adresse o2) {
			// on n'est pas censé avoir plusieurs adresses de contact en même temps, donc une simple comparaison sur les dates devrait suffire
			return IndividuComparisonHelper.RANGE_COMPARATOR.compare(o1, o2);
		}
	};

	private static final IndividuComparisonHelper.Equalator<Object> DEFAULT_EQUALATOR = new IndividuComparisonHelper.DefaultEqualator<Object>();

	private static final IndividuComparisonHelper.Equalator<Adresse> ADDRESS_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Adresse>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Adresse o1, @NotNull Adresse o2) {
			boolean equal = IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2);
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getCasePostale(), o2.getCasePostale());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getLocalite(), o2.getLocalite());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNoOfsPays(), o2.getNoOfsPays());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumero(), o2.getNumero());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroAppartement(), o2.getNumeroAppartement());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroOrdrePostal(), o2.getNumeroOrdrePostal());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroPostal(), o2.getNumeroPostal());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroPostalComplementaire(), o2.getNumeroPostalComplementaire());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroRue(), o2.getNumeroRue());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getRue(), o2.getRue());
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getTitre(), o2.getTitre());
			return equal;
		}
	};

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
		final List<Adresse> contact1 = filterContact(originel.getIndividu().getAdresses());
		final List<Adresse> contact2 = filterContact(corrige.getIndividu().getAdresses());
		final boolean equal = IndividuComparisonHelper.areContentsEqual(contact1, contact2, ADDRESS_COMPARATOR, ADDRESS_EQUALATOR);
		if (!equal) {
			msg.set(ATTRIBUTE);
		}
		return equal;
	}

	private static List<Adresse> filterContact(Collection<Adresse> allAddresses) {
		if (allAddresses != null && allAddresses.size() > 0) {
			final List<Adresse> list = new ArrayList<Adresse>(allAddresses.size());
			for (Adresse adr : allAddresses) {
				if (adr.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
					list.add(adr);
				}
			}
			return list;
		}
		else {
			return Collections.emptyList();
		}
	}
}
