package ch.vd.uniregctb.regimefiscal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
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
		final List<RegimeFiscal> regimesFiscauxNonAnnulesTries = entreprise.getRegimesFiscauxNonAnnulesTries();
		final List<RegimeFiscal> regimesFiscaux = DateRangeHelper.rangesAt(regimesFiscauxNonAnnulesTries, date);
		// Il ne peut y en avoir qu'un
		for (final RegimeFiscal regime : regimesFiscaux) {
			if (regime.getPortee() == RegimeFiscal.Portee.VD) {
				return getTypeRegimeFiscal(regime.getCode());
			}
		}
		return null;
	}

	@Override
	@NotNull
	public List<RegimeFiscalConsolide> getRegimesFiscauxVDNonAnnulesTrie(Entreprise entreprise) {
		return entreprise.getRegimesFiscauxNonAnnulesTries()
				.stream()
				.filter(r -> r.getPortee() == RegimeFiscal.Portee.VD)
				.map(r -> new RegimeFiscalConsolide(r, getTypeRegimeFiscal(r.getCode())))
				.collect(Collectors.toList());
	}
}
