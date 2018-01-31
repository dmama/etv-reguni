package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseContactComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUTE = "adresse de contact";

	// on n'est pas censé avoir plusieurs adresses de contact en même temps, donc une simple comparaison sur les dates devrait suffire
	private static final Comparator<Adresse> ADDRESS_COMPARATOR = Comparator.nullsLast(DateRangeComparator::compareRanges);

	private static final IndividuComparisonHelper.Equalator<Object> DEFAULT_EQUALATOR = new IndividuComparisonHelper.DefaultEqualator<>();

	private static final IndividuComparisonHelper.Equalator<Adresse> ADDRESS_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Adresse>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Adresse o1, @NotNull Adresse o2, @Nullable IndividuComparisonHelper.FieldMonitor monitor, @Nullable String fieldName) {
			boolean equal = IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2, monitor, "dates");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getCasePostale(), o2.getCasePostale(), monitor, "case postale");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getLocalite(), o2.getLocalite(), monitor, "localité");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNoOfsPays(), o2.getNoOfsPays(), monitor, "pays");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumero(), o2.getNumero(), monitor, "numéro");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroAppartement(), o2.getNumeroAppartement(), monitor, "numéro appartement");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroOrdrePostal(), o2.getNumeroOrdrePostal(), monitor, "numéro d'ordre postal");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroPostal(), o2.getNumeroPostal(), monitor, "numéro postal");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroPostalComplementaire(), o2.getNumeroPostalComplementaire(), monitor, "numéro postal complémentaire");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getNumeroRue(), o2.getNumeroRue(), monitor, "numéro rue");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getRue(), o2.getRue(), monitor, "rue");
			equal = equal && DEFAULT_EQUALATOR.areEqual(o1.getTitre(), o2.getTitre(), monitor, "titre");
			if (!equal) {
				IndividuComparisonHelper.fillMonitor(monitor, fieldName);
			}
			return equal;
		}
	};

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {
		final List<Adresse> contact1 = filterContact(originel.getIndividu().getAdresses());
		final List<Adresse> contact2 = filterContact(corrige.getIndividu().getAdresses());
		final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
		final boolean equal = IndividuComparisonHelper.areContentsEqual(contact1, contact2, ADDRESS_COMPARATOR, ADDRESS_EQUALATOR, monitor, ATTRIBUTE);
		if (!equal) {
			msg.setValue(IndividuComparisonHelper.buildErrorMessage(monitor));
		}
		return equal;
	}

	private static List<Adresse> filterContact(Collection<Adresse> allAddresses) {
		if (allAddresses != null && allAddresses.size() > 0) {
			final List<Adresse> list = new ArrayList<>(allAddresses.size());
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
