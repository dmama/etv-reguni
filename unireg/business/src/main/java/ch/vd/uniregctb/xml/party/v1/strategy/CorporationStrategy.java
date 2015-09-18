package ch.vd.uniregctb.xml.party.v1.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.xml.party.corporation.v1.Capital;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.corporation.v1.LegalForm;
import ch.vd.unireg.xml.party.corporation.v1.LegalSeat;
import ch.vd.unireg.xml.party.corporation.v1.TaxSystem;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class CorporationStrategy extends TaxPayerStrategy<Corporation> {

	@Override
	public Corporation newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final Corporation corp = new Corporation();
		initBase(corp, right, context);
		initParts(corp, right, parts, context);
		return corp;
	}

	@Override
	protected void initBase(Corporation to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final Entreprise entreprise = (Entreprise) from;
		final String raisonSociale = context.tiersService.getRaisonSociale(entreprise);
		to.setName1(raisonSociale);
		to.setShortName(raisonSociale);

		final RegDate dernierBouclement = context.bouclementService.getDateDernierBouclement(entreprise.getBouclements(), RegDate.get(), false);
		final RegDate prochainBouclement = context.bouclementService.getDateProchainBouclement(entreprise.getBouclements(), RegDate.get(), true);
		to.setEndDateOfLastBusinessYear(DataHelper.coreToXMLv1(dernierBouclement));
		to.setEndDateOfNextBusinessYear(DataHelper.coreToXMLv1(prochainBouclement));
	}

	@Override
	protected void initParts(Corporation to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final Entreprise entreprise = (Entreprise) from;
		final List<DonneesRegistreCommerce> rcData = entreprise.getDonneesRegistreCommerceNonAnnuleesTriees();

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			to.getCapitals().addAll(extractCapitaux(rcData));
		}

		// TODO [SIPM] les états ??? encore eut-il fallu les migrer...
//		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
//			to.getStatuses().addAll(corporationStatuses2web(hostCorp.getEtats()));
//		}
//
		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			to.getLegalForms().addAll(extractFormesJuridiques(entreprise));
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			final List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			to.getTaxSystemsVD().addAll(extractRegimesFiscaux(regimesFiscaux, RegimeFiscal.Portee.VD));
			to.getTaxSystemsCH().addAll(extractRegimesFiscaux(regimesFiscaux, RegimeFiscal.Portee.CH));
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			to.getLegalSeats().addAll(extractSieges(entreprise, context));
		}
	}

	private interface DonneesRegistreCommerceExtractor<T> {
		T extract(DonneesRegistreCommerce rc);
	}

	private static <T> List<DateRangeHelper.Ranged<T>> extractRanges(List<DonneesRegistreCommerce> rcData, DonneesRegistreCommerceExtractor<? extends T> extractor) {
		// extraction des ranges par valeur distinctes (un nouveau range peut exister alors que la valeur n'a pas changé,
		// car une autre partie de la structure a été modifiée)
		final Map<T, List<DateRange>> values = new HashMap<>(rcData.size());
		for (DonneesRegistreCommerce data : rcData) {
			final T key = extractor.extract(data);
			if (key != null) {
				List<DateRange> ranges = values.get(key);
				if (ranges == null) {
					ranges = new ArrayList<>(rcData.size());
					values.put(key, ranges);
				}
				ranges.add(data);
			}
		}

		// recombinaison des intervales de valeurs fixes
		final List<DateRangeHelper.Ranged<T>> result = new ArrayList<>(rcData.size());
		for (Map.Entry<T, List<DateRange>> entry : values.entrySet()) {
			final List<DateRange> validity = DateRangeHelper.merge(entry.getValue());
			for (DateRange range : validity) {
				result.add(new DateRangeHelper.Ranged<>(range.getDateDebut(), range.getDateFin(), entry.getKey()));
			}
		}

		// re-tri par dates
		Collections.sort(result, new DateRangeComparator<>());
		return result;
	}

	@NotNull
	private List<Capital> extractCapitaux(List<DonneesRegistreCommerce> rcData) {
		final List<DateRangeHelper.Ranged<Long>> rcCapitaux = extractRanges(rcData, new DonneesRegistreCommerceExtractor<Long>() {
			@Override
			public Long extract(DonneesRegistreCommerce rc) {
				return rc.getCapital() != null && rc.getCapital().isEnFrancsSuisses() ? rc.getCapital().getMontant() : null;
			}
		});

		final List<Capital> liste = new ArrayList<>(rcCapitaux.size());
		for (DateRangeHelper.Ranged<Long> data : rcCapitaux) {
			final Capital capital = new Capital();
			capital.setDateFrom(DataHelper.coreToXMLv1(data.getDateDebut()));
			capital.setDateTo(DataHelper.coreToXMLv1(data.getDateFin()));
			capital.setPaidInCapital(data.getPayload());
			liste.add(capital);
		}
		return liste;
	}

	@NotNull
	private List<LegalForm> extractFormesJuridiques(Entreprise entreprise) {
		final List<LegalForm> liste;
		if (entreprise.isConnueAuCivil()) {
			// TODO [SIPM][RCEnt] aller chercher les formes juridiques historiques
			liste = Collections.emptyList();
		}
		else {
			final List<DateRangeHelper.Ranged<FormeJuridiqueEntreprise>> rcFormesJuridiques = extractRanges(entreprise.getDonneesRegistreCommerceNonAnnuleesTriees(),
			                                                                                                new DonneesRegistreCommerceExtractor<FormeJuridiqueEntreprise>() {
				                                                                                                @Override
				                                                                                                public FormeJuridiqueEntreprise extract(DonneesRegistreCommerce rc) {
					                                                                                                return rc.getFormeJuridique();
				                                                                                                }
			                                                                                                });
			liste = new ArrayList<>(rcFormesJuridiques.size());
			for (DateRangeHelper.Ranged<FormeJuridiqueEntreprise> data : rcFormesJuridiques) {
				final LegalForm lf = new LegalForm();
				lf.setDateFrom(DataHelper.coreToXMLv1(data.getDateDebut()));
				lf.setDateTo(DataHelper.coreToXMLv1(data.getDateFin()));
				lf.setCode(EnumHelper.coreToXMLv1v2v3(data.getPayload()));
				liste.add(lf);
			}
		}
		return liste;
	}

	@NotNull
	private List<TaxSystem> extractRegimesFiscaux(List<RegimeFiscal> regimes, RegimeFiscal.Portee portee) {
		final List<TaxSystem> liste = new ArrayList<>(regimes.size());
		for (RegimeFiscal regime : regimes) {
			if (regime.getPortee() == portee) {
				final TaxSystem ts = new TaxSystem();
				ts.setDateFrom(DataHelper.coreToXMLv1(regime.getDateDebut()));
				ts.setDateTo(DataHelper.coreToXMLv1(regime.getDateFin()));
				ts.setCode(EnumHelper.coreToXMLv1v2v3(regime.getType()));
				liste.add(ts);
			}
		}
		return liste;
	}

	@NotNull
	private List<LegalSeat> extractSieges(Entreprise entreprise, Context context) {
		final List<LegalSeat> liste = new ArrayList<>();
		final List<DateRanged<Etablissement>> etablissements = context.tiersService.getEtablissementsForEntreprise(entreprise);
		for (DateRanged<Etablissement> etb : etablissements) {
			if (etb.getPayload().isPrincipal()) {
				final List<DomicileEtablissement> domiciles = etb.getPayload().getSortedDomiciles(false);
				for (DomicileEtablissement domicile : domiciles) {
					final DateRange intersection = DateRangeHelper.intersection(domicile, etb);
					if (intersection != null) {
						final LegalSeat seat = new LegalSeat();
						seat.setDateFrom(DataHelper.coreToXMLv1(intersection.getDateDebut()));
						seat.setDateTo(DataHelper.coreToXMLv1(intersection.getDateFin()));
						seat.setFsoId(domicile.getNumeroOfsAutoriteFiscale());
						seat.setType(EnumHelper.coreToXMLLegalSeatv1(domicile.getTypeAutoriteFiscale()));
						liste.add(seat);
					}
				}
			}
		}
		return liste;
	}

	@Override
	public Corporation clone(Corporation right, @Nullable Set<PartyPart> parts) {
		final Corporation pm = new Corporation();
		copyBase(pm, right);
		copyParts(pm, right, parts, CopyMode.EXCLUSIVE);
		return pm;
	}

	@Override
	protected void copyBase(Corporation to, Corporation from) {
		super.copyBase(to, from);
		to.setShortName(from.getShortName());
		to.setName1(from.getName1());
		to.setName2(from.getName2());
		to.setName3(from.getName3());
		to.setEndDateOfLastBusinessYear(from.getEndDateOfLastBusinessYear());
		to.setEndDateOfNextBusinessYear(from.getEndDateOfNextBusinessYear());
		to.setIpmroNumber(from.getIpmroNumber());
	}

	@Override
	protected void copyParts(Corporation to, Corporation from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			copyColl(to.getLegalSeats(), from.getLegalSeats());
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			copyColl(to.getLegalForms(), from.getLegalForms());
		}

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			copyColl(to.getCapitals(), from.getCapitals());
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			copyColl(to.getTaxSystemsVD(), from.getTaxSystemsVD());
			copyColl(to.getTaxSystemsCH(), from.getTaxSystemsCH());
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
			copyColl(to.getStatuses(), from.getStatuses());
		}
	}
}
