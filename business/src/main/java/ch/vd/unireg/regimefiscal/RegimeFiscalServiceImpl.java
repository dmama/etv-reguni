package ch.vd.unireg.regimefiscal;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-01-25, <raphael.marmier@vd.ch>
 */
public class RegimeFiscalServiceImpl implements RegimeFiscalService {

	private ServiceInfrastructureService serviceInfra;

	private RegimeFiscalServiceConfiguration configuration;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setConfiguration(RegimeFiscalServiceConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscal(@NotNull String code) throws RegimeFiscalServiceException {
		final TypeRegimeFiscal regimeFiscal = serviceInfra.getRegimeFiscal(code);
		if (regimeFiscal == null) {
			throw new RegimeFiscalServiceException(String.format("Aucun type de régime fiscal ne correspond au code fourni '%s'. " +
					                                                     "Soit le code est erroné, soit il manque des données dans FiDoR.", code));
		}
		return regimeFiscal;
	}

	@Override
	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscalSocieteDePersonnes() {
		final List<TypeRegimeFiscal> typesRegimesFiscaux = serviceInfra.getRegimesFiscaux();
		final List<TypeRegimeFiscal> typesSP = typesRegimesFiscaux.stream()
				.filter(TypeRegimeFiscal::isSocieteDePersonnes)
				.collect(Collectors.toList());
		if (typesSP.isEmpty()) {
			throw new RegimeFiscalServiceException("Aucun régime fiscal pour 'Société de personnes' trouvé.");
		}
		if (typesSP.size() > 1) {
			throw new RegimeFiscalServiceException("Plus d'un régime fiscal pour 'Société de personnes' trouvé.");
		}
		return typesSP.get(0);
	}

	@Override
	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscalIndetermine() {
		final List<TypeRegimeFiscal> typesRegimesFiscaux = serviceInfra.getRegimesFiscaux();
		final List<TypeRegimeFiscal> indetermines = typesRegimesFiscaux.stream()
				.filter(TypeRegimeFiscal::isIndetermine)
				.collect(Collectors.toList());
		if (indetermines.isEmpty()) {
			throw new RegimeFiscalServiceException("Aucun régime fiscal indéterminé trouvé.");
		}
		if (indetermines.size() > 1) {
			throw new RegimeFiscalServiceException("Plus d'un régime fiscal indéterminé trouvé.");
		}
		return indetermines.get(0);
	}

	@Override
	@NotNull
	public FormeJuridiqueVersTypeRegimeFiscalMapping getFormeJuridiqueMapping(@NotNull FormeJuridiqueEntreprise formeJuridique, @Nullable RegDate dateReference) {

		String codeRegime = configuration.getCodeTypeRegimeFiscal(formeJuridique);
		if (codeRegime == null) {
			codeRegime = getTypeRegimeFiscalIndetermine().getCode();
		}

		final TypeRegimeFiscal result;
		try {
			result = getTypeRegimeFiscal(codeRegime);
		}
		catch (RegimeFiscalServiceException e) {
			throw new RegimeFiscalServiceException(
					String.format("Impossible de récupérer un type de régime fiscal avec le code '%s' configuré pour la forme juridique \"%s\". Faites contrôler la configuration Unireg des types par défaut.",
					              codeRegime, formeJuridique.getLibelle()));
		}

		// TODO (msi) gérer correctement les plages de validité
		return new FormeJuridiqueVersTypeRegimeFiscalMapping(null, null, formeJuridique, result);
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

	@Override
	public boolean isRegimeFiscalDiOptionnelleVd(@NotNull TypeRegimeFiscal typeRegimeFiscal) {
		return configuration.isRegimeFiscalDiOptionnelleVd(typeRegimeFiscal.getCode());
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
