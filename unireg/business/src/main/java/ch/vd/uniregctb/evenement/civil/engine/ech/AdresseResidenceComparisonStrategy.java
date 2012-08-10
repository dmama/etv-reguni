package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Comparateur d'individu basé sur les adresses de résidence (tant principales que secondaires) de l'individu
 */
public abstract class AdresseResidenceComparisonStrategy implements IndividuComparisonStrategy {

	private static final Comparator<LocalisationType> TYPE_LOCALISATION_COMPARATOR = new IndividuComparisonHelper.DefaultComparator<LocalisationType>(true);

	private static final Comparator<Localisation> LOCALISATION_COMPARATOR = new IndividuComparisonHelper.NullableComparator<Localisation>(true) {
		@Override
		protected int compareNonNull(@NotNull Localisation o1, @NotNull Localisation o2) {
			int comparison = TYPE_LOCALISATION_COMPARATOR.compare(o1.getType(), o2.getType());
			if (comparison == 0) {
				comparison = IndividuComparisonHelper.INTEGER_COMPARATOR.compare(o1.getNoOfs(), o2.getNoOfs());
			}
			return comparison;
		}
	};

	private static final IndividuComparisonHelper.Equalator<Localisation> LOCALISATION_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Localisation>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Localisation o1, @NotNull Localisation o2) {
			return o1.getType() == o2.getType() && IndividuComparisonHelper.INTEGER_EQUALATOR.areEqual(o1.getNoOfs(), o2.getNoOfs());
		}
	};

	private static final Comparator<Adresse> ADRESSE_COMPARATOR = new IndividuComparisonHelper.NullableComparator<Adresse>(true) {
		@Override
		protected int compareNonNull(@NotNull Adresse o1, @NotNull Adresse o2) {
			int comparison = Integer.signum(o1.getTypeAdresse().ordinal() - o2.getTypeAdresse().ordinal());
			if (comparison == 0) {
				comparison = IndividuComparisonHelper.RANGE_COMPARATOR.compare(o1, o2);
				if (comparison == 0) {
					comparison = IndividuComparisonHelper.INTEGER_COMPARATOR.compare(o1.getEgid(), o2.getEgid());
					if (comparison == 0) {
						comparison = LOCALISATION_COMPARATOR.compare(o1.getLocalisationPrecedente(), o2.getLocalisationPrecedente());
						if (comparison == 0) {
							comparison = LOCALISATION_COMPARATOR.compare(o1.getLocalisationSuivante(), o2.getLocalisationSuivante());
						}
					}
				}
			}

			return comparison;
		}
	};

	private static final IndividuComparisonHelper.Equalator<Adresse> ADRESSE_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Adresse>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Adresse o1, @NotNull Adresse o2) {
			if (o1.getTypeAdresse() != o2.getTypeAdresse()) {
				return false;
			}
			if (!IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2) ) {
				return false;
			}
			if (!IndividuComparisonHelper.INTEGER_EQUALATOR.areEqual(o1.getEgid(), o2.getEgid())) {
				return false;
			}
			if (!LOCALISATION_EQUALATOR.areEqual(o1.getLocalisationPrecedente(), o2.getLocalisationPrecedente())) {
				return false;
			}
			if (!LOCALISATION_EQUALATOR.areEqual(o1.getLocalisationSuivante(), o2.getLocalisationSuivante())) {
				return false;
			}
			return true;
		}
	};

	/**
	 * @param src les adresses d'un individu
	 * @return la liste des adresses de résidence à considérer pour cette stratégie
	 */
	private List<Adresse> extractAdressesResidence(Collection<Adresse> src) {
		final List<Adresse> res = new ArrayList<Adresse>();
		if (src != null && src.size() > 0) {
			for (Adresse adr : src) {
				if (isTakenIntoAccount(adr)) {
					res.add(adr);
				}
			}
		}
		return res;
	}

	@Override
	public boolean sansDifferenceFiscalementImportante(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
		final List<Adresse> resOriginelles = extractAdressesResidence(originel.getIndividu().getAdresses());
		final List<Adresse> resCorrigees = extractAdressesResidence(corrige.getIndividu().getAdresses());
		if (!IndividuComparisonHelper.areContentsEqual(resOriginelles, resCorrigees, ADRESSE_COMPARATOR, ADRESSE_EQUALATOR)) {
			msg.set(getAttribute());
			return false;
		}
		return true;
	}

	/**
	 * @param adresse une adresse de l'individu
	 * @return <code>true</code> si elle doit être prise en compte, <code>false</code> sinon
	 */
	protected abstract boolean isTakenIntoAccount(Adresse adresse);

	/**
	 * @return le nom de l'attribut à afficher dans les rapports d'erreur
	 */
	protected abstract String getAttribute();
}