package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAdresseCivil;

/**
 * Comparateur d'individu basé sur les adresses de résidence (tant principales que secondaires) de l'individu
 */
public abstract class AdresseResidenceComparisonStrategy implements IndividuComparisonStrategy {

	private static final String TYPE = "type";
	private static final String COMMUNE = "commune";
	private static final String DATES = "dates";
	private static final String LOCALISATION_PRECEDENTE = "localisation précédente";
	private static final String LOCALISATION_SUIVANTE = "localisation suivante";

	private static final Comparator<LocalisationType> TYPE_LOCALISATION_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	private static final Comparator<Localisation> LOCALISATION_COMPARATOR = Comparator.nullsLast(Comparator.comparing(Localisation::getType, TYPE_LOCALISATION_COMPARATOR)
			                                                                                             .thenComparing(Localisation::getNoOfs, IndividuComparisonHelper.INTEGER_COMPARATOR));

	private static final IndividuComparisonHelper.Equalator<Localisation> LOCALISATION_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Localisation>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Localisation o1, @NotNull Localisation o2, @Nullable IndividuComparisonHelper.FieldMonitor monitor, @Nullable String fieldName) {
			boolean equal = o1.getType() == o2.getType();
			if (!equal) {
				IndividuComparisonHelper.fillMonitor(monitor, TYPE);
			}
			else if (o1.getType() == LocalisationType.CANTON_VD) {
				equal = IndividuComparisonHelper.INTEGER_EQUALATOR.areEqual(o1.getNoOfs(), o2.getNoOfs(), monitor, COMMUNE);
			}
			if (!equal) {
				IndividuComparisonHelper.fillMonitor(monitor, fieldName);
			}
			return equal;
		}
	};

	private final Comparator<Adresse> ADRESSE_COMPARATOR = Comparator.nullsLast(Comparator.comparing(Adresse::getTypeAdresse)
			                                                                            .thenComparing(DateRangeComparator::compareRanges)
			                                                                            .thenComparing(this::getNoOfsCommune, IndividuComparisonHelper.INTEGER_COMPARATOR)
			                                                                            .thenComparing(Adresse::getLocalisationPrecedente, LOCALISATION_COMPARATOR)
			                                                                            .thenComparing(Adresse::getLocalisationSuivante, LOCALISATION_COMPARATOR));

	private static final IndividuComparisonHelper.Equalator<TypeAdresseCivil> TYPE_ADRESSE_EQUALATOR = new IndividuComparisonHelper.DefaultEqualator<>();

	private final IndividuComparisonHelper.Equalator<Adresse> ADRESSE_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Adresse>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Adresse o1, @NotNull Adresse o2, @Nullable IndividuComparisonHelper.FieldMonitor monitor, @Nullable String fieldName) {
			boolean equal = TYPE_ADRESSE_EQUALATOR.areEqual(o1.getTypeAdresse(), o2.getTypeAdresse(), monitor, TYPE);
			equal = equal && IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2, monitor, DATES);
			equal = equal && IndividuComparisonHelper.INTEGER_EQUALATOR.areEqual(getNoOfsCommune(o1), getNoOfsCommune(o2), monitor, COMMUNE);
			equal = equal && LOCALISATION_EQUALATOR.areEqual(o1.getLocalisationPrecedente(), o2.getLocalisationPrecedente(), monitor, LOCALISATION_PRECEDENTE);
			equal = equal && LOCALISATION_EQUALATOR.areEqual(o1.getLocalisationSuivante(), o2.getLocalisationSuivante(), monitor, LOCALISATION_SUIVANTE);
			if (!equal) {
				IndividuComparisonHelper.fillMonitor(monitor, fieldName);
			}
			return equal;
		}
	};

	private final ServiceInfrastructureService infraService;

	public AdresseResidenceComparisonStrategy(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * @param src les adresses d'un individu
	 * @return la liste des adresses de résidence à considérer pour cette stratégie
	 */
	private List<Adresse> extractAdressesResidence(Collection<Adresse> src) {
		final List<Adresse> res = new ArrayList<>();
		if (src != null && src.size() > 0) {
			for (Adresse adr : src) {
				if (isTakenIntoAccount(adr)) {
					res.add(adr);
				}
			}
		}
		return res;
	}

	@Nullable
	private Integer getNoOfsCommune(Adresse adr) {
		if (adr.getEgid() != null) {
			// on passe par l'EGID si on en a un
			return infraService.getNoOfsCommuneByEgid(adr.getEgid(), adr.getDateDebut());
		}
		else {
			// sinon l'adresse fournit peut-être aussi l'information
			return adr.getNoOfsCommuneAdresse();
		}
	}

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {
		final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
		final List<Adresse> resOriginelles = extractAdressesResidence(originel.getIndividu().getAdresses());
		final List<Adresse> resCorrigees = extractAdressesResidence(corrige.getIndividu().getAdresses());
		if (!IndividuComparisonHelper.areContentsEqual(resOriginelles, resCorrigees, ADRESSE_COMPARATOR, ADRESSE_EQUALATOR, monitor, getAttribute())) {
			msg.setValue(IndividuComparisonHelper.buildErrorMessage(monitor));
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