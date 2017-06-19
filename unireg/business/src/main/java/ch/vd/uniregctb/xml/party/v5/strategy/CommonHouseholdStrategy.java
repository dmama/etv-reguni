package ch.vd.uniregctb.xml.party.v5.strategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.person.v5.CommonHousehold;
import ch.vd.unireg.xml.party.person.v5.CommonHouseholdStatus;
import ch.vd.unireg.xml.party.v5.PartyLabel;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v5.LabelBuilder;
import ch.vd.uniregctb.xml.party.v5.PartyBuilder;

public class CommonHouseholdStrategy extends TaxPayerStrategy<CommonHousehold> {

	@Override
	public CommonHousehold newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final CommonHousehold menage = new CommonHousehold();
		initBase(menage, right, context);
		initParts(menage, right, parts, context);
		return menage;
	}

	@Override
	public CommonHousehold clone(CommonHousehold right, @Nullable Set<PartyPart> parts) {
		final CommonHousehold menage = new CommonHousehold();
		copyBase(menage, right);
		copyParts(menage, right, parts, CopyMode.EXCLUSIVE);
		return menage;
	}

	@Override
	protected void initParts(CommonHousehold to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final MenageCommun menage = (MenageCommun) from;
		if (parts != null && parts.contains(PartyPart.HOUSEHOLD_MEMBERS)) {
			initMembers(to, menage, context);
		}
	}

	@Override
	protected void copyParts(CommonHousehold to, CommonHousehold from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.HOUSEHOLD_MEMBERS)) {
			to.setMainTaxpayer(from.getMainTaxpayer());
			to.setSecondaryTaxpayer(from.getSecondaryTaxpayer());
			to.setStatus(from.getStatus());
		}
	}

	private static void initMembers(CommonHousehold left, MenageCommun menageCommun, Context context) throws ServiceException {
		final EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			left.setMainTaxpayer(PartyBuilder.newNaturalPerson(principal, null, context));
		}

		final PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			left.setSecondaryTaxpayer(PartyBuilder.newNaturalPerson(conjoint, null, context));
		}

		left.setStatus(initStatus(ensemble, context)); // [SIFISC-6028]
	}

	/**
	 * [SIFISC-6028] Détermine le status du ménage commun.
	 *
	 * @param ensemble l'ensemble tiers-couple du ménage-commun.
	 * @param context  le context d'exécution
	 * @return le statut du ménage
	 */
	private static CommonHouseholdStatus initStatus(EnsembleTiersCouple ensemble, Context context) {
   		return EnumHelper.coreToXMLv5(context.tiersService.getStatutMenageCommun(ensemble.getMenage()));
	}

	/**
	 * Structure qui maintient la donnée d'une étiquette à appliquer sur une période de temps, avec un flag "virtuel"
	 */
	private static class RangeEtiquette implements DateRange {

		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final boolean virtual;
		private final Etiquette etiquette;

		public RangeEtiquette(RegDate dateDebut, RegDate dateFin, boolean virtual, Etiquette etiquette) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.virtual = virtual;
			this.etiquette = etiquette;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}
	}

	@Override
	protected void initLabels(CommonHousehold tiers, Tiers right, Context context) {
		// on n'appelle pas la classe de base parce qu'on refait tout ici...
		//super.initLabels(tiers, right, context);

		final MenageCommun menage = (MenageCommun) right;
		final EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menage, null);

		// récupération de la composante "directe" (qui donneront des label non-virtuels), par "code étiquette"
		final Map<Etiquette, List<DateRange>> directesParEtiquette = new HashMap<>();
		fillEtiquettesTiersParEtiquette(directesParEtiquette, right);

		// on veut fusionner les étiquettes "virtuelles" qui viennent des membres du couple, par "code étiquette"
		// -> récupération des composantes "principal" et "conjoint"
		final Map<Etiquette, List<DateRange>> virtuellesParEtiquette = new HashMap<>();
		Stream.of(ensemble.getPrincipal(), ensemble.getConjoint())
				.filter(Objects::nonNull)
				.forEach(pp -> fillEtiquettesTiersParEtiquette(virtuellesParEtiquette, pp));

		// fusion des deux composantes "virtuelles" en enlevant la partie déjà couverte par la composante "directe"
		for (Map.Entry<Etiquette, List<DateRange>> entry : virtuellesParEtiquette.entrySet()) {
			final List<DateRange> ranges = entry.getValue();
			ranges.sort(DateRangeComparator::compareRanges);
			final List<DateRange> rangesFusionnes = DateRangeHelper.merge(ranges);

			final List<DateRange> dejaCouverts = Optional.of(entry.getKey())
					.map(directesParEtiquette::get)
					.orElseGet(Collections::emptyList);

			final List<DateRange> nonCouverts = DateRangeHelper.subtract(rangesFusionnes, dejaCouverts, new DateRangeAdapterCallback());
			entry.setValue(nonCouverts);
		}

		// constitution de la liste finale

		final Stream<RangeEtiquette> directs = directesParEtiquette.entrySet().stream()
				.map(entry -> entry.getValue().stream().map(range -> new RangeEtiquette(range.getDateDebut(), range.getDateFin(), false, entry.getKey())))
				.flatMap(Function.identity());
		final Stream<RangeEtiquette> virtuels = virtuellesParEtiquette.entrySet().stream()
				.map(entry -> entry.getValue().stream().map(range -> new RangeEtiquette(range.getDateDebut(), range.getDateFin(), true, entry.getKey())))
				.flatMap(Function.identity());

		final List<PartyLabel> labels = tiers.getLabels();
		Stream.concat(directs, virtuels)
				.sorted(DateRangeComparator::compareRanges)
				.map(range -> LabelBuilder.newLabel(range, range.etiquette, range.virtual))
				.forEach(labels::add);
	}

	private static void fillEtiquettesTiersParEtiquette(Map<Etiquette, List<DateRange>> map, Tiers tiers) {
		tiers.getEtiquettesNonAnnuleesTriees().forEach(etiqTiers -> map.merge(etiqTiers.getEtiquette(),
		                                                                      Collections.singletonList(etiqTiers),
		                                                                      (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));
	}
}
