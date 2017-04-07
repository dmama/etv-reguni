package ch.vd.uniregctb.regimefiscal;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-01-25, <raphael.marmier@vd.ch>
 */
public class ServiceRegimeFiscalImpl implements ServiceRegimeFiscal {

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@Override
	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscal(@NotNull String codeRegime) throws ServiceRegimeFiscalException {
		Objects.requireNonNull(codeRegime, "Impossible de déterminer le type de régime fiscal sans son code.");

		final List<TypeRegimeFiscal> typesRegimesFiscaux = serviceInfra.getRegimesFiscaux();
		final List<TypeRegimeFiscal> typesRegimeFiscal = typesRegimesFiscaux.stream().filter(r -> codeRegime.equals(r.getCode())).collect(Collectors.toList());
		if (typesRegimeFiscal.size() > 1) {
			throw new ServiceRegimeFiscalException(String.format("Fatal: Deux ou plus types de régime fiscal partagent le même code: %s. Problème de configuration FiDor.", codeRegime));
		}
		if (typesRegimeFiscal.size() == 0) {
			throw new ServiceRegimeFiscalException(String.format("Aucun type de régime fiscal ne correspond au code fourni: %s. Soit le code est erronné, soit il manque des données dans FiDoR.",
			                                                     codeRegime));
		}
		return typesRegimeFiscal.get(0);
	}

	@Override
	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscalIndetermine() {
		final List<TypeRegimeFiscal> typesRegimesFiscaux = serviceInfra.getRegimesFiscaux();
		final List<TypeRegimeFiscal> indetermines = typesRegimesFiscaux.stream()
				.filter(TypeRegimeFiscal::isIndetermine)
				.collect(Collectors.toList());
		if (indetermines.isEmpty()) {
			throw new ServiceRegimeFiscalException("Aucun régime fiscal indéterminé trouvé.");
		}
		if (indetermines.size() > 1) {
			throw new ServiceRegimeFiscalException("Plus d'un régime fiscal indéterminé trouvé.");
		}
		return indetermines.get(0);
	}

	@Override
	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscalParDefaut(@NotNull FormeJuridiqueEntreprise formeJuridique) throws ServiceRegimeFiscalException {

		final String codeRegime = FormeJuridiqueCodesRegimeFiscauxMapping.getDefaultCodePourFormeJuridique(formeJuridique);

		try {
			return this.getTypeRegimeFiscal(codeRegime);
		}
		catch (ServiceRegimeFiscalException e) {
			throw new ServiceRegimeFiscalException(
					String.format("Impossible de récupérer un type de régime fiscal avec le code %s configuré pour la forme juridique \"%s\". Faites contrôler la configuration Unireg des types par défaut.",
					              codeRegime, formeJuridique.getLibelle()));
		}
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscalVD(Entreprise entreprise, RegDate date) {
		final List<RegimeFiscal> regimesFiscauxNonAnnulesTries = entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD);
		final RegimeFiscal rf = DateRangeHelper.rangeAt(regimesFiscauxNonAnnulesTries, date);
		return rf != null ? getTypeRegimeFiscal(rf.getCode()) : null;
	}

	@Override
	@NotNull
	public List<RegimeFiscalConsolide> getRegimesFiscauxVDNonAnnulesTrie(Entreprise entreprise) {
		return entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD).stream()
				.map(r -> new RegimeFiscalConsolide(r, getTypeRegimeFiscal(r.getCode())))
				.collect(Collectors.toList());
	}

	/**
	 * @param entreprise entreprise à considérer
	 * @param genreImpot le genre d'impôt qui nous intéresse
	 * @return les périodes d'exonération avec les types d'éxonération concernés
	 */
	@NotNull
	@Override
	public List<ModeExonerationHisto> getExonerations(Entreprise entreprise, GenreImpotExoneration genreImpot) {
		final List<RegimeFiscalConsolide> regimes = getRegimesFiscauxVDNonAnnulesTrie(entreprise);
		final List<ModeExonerationHisto> histo = new LinkedList<>();
		for (RegimeFiscalConsolide rf : regimes) {
			final List<PlageExonerationFiscale> exonerations = rf.getExonerations(genreImpot);
			for (PlageExonerationFiscale exoneration : exonerations) {
				final DateRange rangeExoneration = new DateRangeHelper.Range(RegDate.get(exoneration.getPeriodeDebut(), 1, 1),
				                                                             Optional.ofNullable(exoneration.getPeriodeFin()).map(pf -> RegDate.get(pf, 12, 31)).orElse(null));
				final DateRange intersection = DateRangeHelper.intersection(rf, rangeExoneration);
				if (intersection != null) {
					histo.add(new ModeExonerationHisto(intersection.getDateDebut(), intersection.getDateFin(), exoneration.getMode()));
				}
			}
		}
		return DateRangeHelper.collate(histo);
	}
}