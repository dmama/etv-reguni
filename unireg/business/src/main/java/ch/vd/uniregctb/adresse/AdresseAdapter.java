package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public abstract class AdresseAdapter implements AdresseGenerique {

	protected  final ServiceInfrastructureService service;

	public AdresseAdapter(ServiceInfrastructureService service) {
		Assert.notNull(service);
		this.service = service;
	}

	public String getLocalite() {
		String nomLocalite = null;

		Integer noOfsRue = getNumeroRue();

		if (noOfsRue != null && noOfsRue!=0) {

			final Rue rue = getRue(noOfsRue);
			final int numeroLocalite = rue.getNoLocalite();

			if (numeroLocalite != 0) {
				final Localite localite = getLocalite(numeroLocalite);
				nomLocalite = localite.getNomAbregeMinuscule();
			}
		}
		return nomLocalite;
	}

	public String getLocaliteComplete() {
		String nomLocalite = null;

		Integer noOfsRue = getNumeroRue();

		if (noOfsRue != null && noOfsRue!=0) {

			final Rue rue = getRue(noOfsRue);
			final int numeroLocalite = rue.getNoLocalite();

			if (numeroLocalite != 0) {
				final Localite localite = getLocalite(numeroLocalite);
				nomLocalite = localite.getNomCompletMinuscule();
			}
		}
		return nomLocalite;
	}

	public String getRue() {
		String rue = null;

		final Integer numeroRue = getNumeroRue();
		if (numeroRue != null && numeroRue!=0) {
			final Rue r = getRue(numeroRue);
			rue = r.getDesignationCourrier();
		}
		return rue;
	}

	/**
	 * Retourne la rue donnée par son numéro (si pas d'exception, forcément non-null)
	 */
	private Rue getRue(int numeroRue) {
		final Rue r;
		r = service.getRueByNumero(numeroRue);
		if (r == null) {
			throw new RuntimeException("La rue avec le numéro technique " + numeroRue + " est inconnue !");
		}
		return r;
	}

	/**
	 * Retourne la localité correspondant à ce numéro (RTE si pas trouvé)
	 */
	private Localite getLocalite(int numeroLocalite) {
		final Localite localite;
		localite = service.getLocaliteByONRP(numeroLocalite);
		if (localite == null) {
			throw new RuntimeException("La localité avec le numéro " + numeroLocalite + " est inconnue !");
		}
		return localite;
	}

	/**
	 * Extrait le numero postal d'un numero de rue existant
	 */
	public String getNumeroPostal() {
		String numero = null;
		final Integer numeroRue = getNumeroRue();
		if (numeroRue != null && numeroRue != 0) {
			final Rue rue = getRue(numeroRue);
			final Localite localite = getLocalite(rue.getNoLocalite());
			numero = Integer.toString(localite.getNPA());
		}
		return numero;
	}

	/**
	 * Extrait le numéro d'ordre postal d'un numéro de rue existant
	 */
	public int getNumeroOrdrePostal() {
		int noOrdrePostal = 0;
		final Integer numeroRue = getNumeroRue();
		if (numeroRue != null && numeroRue != 0) {
			final Rue rue = getRue(numeroRue);
			final Localite localite = getLocalite(rue.getNoLocalite());
			noOrdrePostal = localite.getNoOrdre();
		}
		return noOrdrePostal;
	}
}
