package ch.vd.unireg.validation.fors;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.regimefiscal.RegimeFiscalConsolide;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;

public class ForFiscalPrincipalPMValidator extends ForFiscalPrincipalValidator<ForFiscalPrincipalPM> {

	protected static final RegDate DATE_SAISIE_REGIME_FISCAUX = RegDate.get(2009, 1, 1);

	private RegimeFiscalService regimeFiscalService;

	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	@Override
	protected Class<ForFiscalPrincipalPM> getValidatedClass() {
		return ForFiscalPrincipalPM.class;
	}

	/**
	 * Détermine les genres d'impôt autorisés pour l'entreprise dans la période du for fiscal spécifié.
	 * <p/>
	 * Depuis le SIFISC-26314, les genres d'impôt autorisés sont déduits des régimes fiscaux selon la règle suivante :
	 * <ul>
	 *     <li>régime fiscal <i>société de personnes</i> : genre d'impôt <i>revenu/fortune</i></li>
	 *     <li>régime fiscal <i>personne morale</i> (et autre) : genre d'impôt <i>bénéfice/capital</i></li>
	 * </ul>
	 * Lorsque les fors fiscaux et les régimes fiscaux ne coincident pas en terme de dates, on fait preuve de souplesse et on
	 * autorise les deux régimes fiscaux pendant la période de chevauchement.
	 *
	 * @param forFiscal un for fiscal
	 * @return les genres d'impôt autorisés.
	 */
	@NotNull
	protected Set<GenreImpot> determineAllowedGenreImpots(@NotNull ForFiscalPrincipalPM forFiscal) {

		final RegDate dateFin = forFiscal.getDateFin();
		if (dateFin != null && dateFin.isBefore(DATE_SAISIE_REGIME_FISCAUX)) {
			// [SIFISC-26314] les régimes fiscaux n'ont été saisis qu'à partir de 2009, avant on considère donc que les deux genres d'impôt sont autorisés.
			return EnumSet.of(GenreImpot.BENEFICE_CAPITAL, GenreImpot.REVENU_FORTUNE);
		}

		final Set<GenreImpot> allowed;

		final ContribuableImpositionPersonnesMorales cipm = forFiscal.getTiers();
		if (cipm instanceof Entreprise) {

			// [SIFISC-26314] on va chercher les régimes fiscaux de l'entreprise
			final List<RegimeFiscalConsolide> regimes = regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie((Entreprise) cipm);
			final List<RegimeFiscalConsolide> regimesPM = regimes.stream()
					.filter(r -> !r.isSocieteDePersonnes())
					.collect(Collectors.toList());
			final List<RegimeFiscalConsolide> regimesSP = regimes.stream()
					.filter(RegimeFiscalConsolide::isSocieteDePersonnes)
					.collect(Collectors.toList());

			allowed = new HashSet<>();
			if (DateRangeHelper.intersect(forFiscal, regimesPM)) {
				// le for fiscal est sur une période avec des régimes 'personne morales' -> bénéfice capital
				allowed.add(GenreImpot.BENEFICE_CAPITAL);
			}
			if (DateRangeHelper.intersect(forFiscal, regimesSP)) {
				// le for fiscal est sur une période avec des régimes 'société de personnes' -> revenu fortune
				allowed.add(GenreImpot.REVENU_FORTUNE);
			}
		}
		else {
			allowed = EnumSet.of(GenreImpot.BENEFICE_CAPITAL);
		}
		return allowed;
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.DOMICILE == motif;
	}
}
