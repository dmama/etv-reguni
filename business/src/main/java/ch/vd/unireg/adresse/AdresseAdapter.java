package ch.vd.unireg.adresse;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public abstract class AdresseAdapter implements AdresseGenerique {

	protected  final ServiceInfrastructureService service;

	public AdresseAdapter(ServiceInfrastructureService service) {
		Assert.notNull(service);
		this.service = service;
	}

	@Override
	public String getLocalite() {
		final Integer noOrdrePostal = getNumeroOrdrePostal();
		if (noOrdrePostal != null) {
			final Localite localite = getLocalite(noOrdrePostal, getDateFin());
			return localite.getNom();
		}
		else {
			return null;
		}
	}

	@Override
	public String getLocaliteComplete() {
		final Integer noOrdrePostal = getNumeroOrdrePostal();
		if (noOrdrePostal != null) {
			final Localite localite = getLocalite(noOrdrePostal, getDateFin());
			return localite.getNom();
		}
		else {
			return null;
		}
	}

	/**
	 * Cette méthode résoud le nom de la rue qui va bien à partir du numéro technique de la rue ou du libellé <i>texte libre</i> de la rue.
	 *
	 * @param numeroRue   le numéro technique de la rue
	 * @param nomRueLibre le libellé <i>texte libre</i> de la rue
	 * @return le nom de la rue; ou <b>null</b> si aucun nom n'a été trouvé.
	 */
	@Nullable
	protected String resolveNomRue(@Nullable Integer numeroRue, @Nullable String nomRueLibre) {
		final Rue rue = getRue(numeroRue);
		if (rue != null) {
			return rue.getDesignationCourrier();
		}
		else {
			return StringUtils.trimToNull(nomRueLibre);
		}
	}

	@Nullable
	private Rue getRue(@Nullable Integer numeroRue) {
		if (numeroRue != null) {
			final Rue rueFin = service.getRueByNumero(numeroRue);
			if (rueFin != null) {
				return rueFin;
			}
			return service.getRueByNumero(numeroRue);
		}
		return null;
	}

	/**
	 * Retourne la localité correspondant à ce numéro (RTE si pas trouvé)
	 */
	private Localite getLocalite(int numeroLocalite, RegDate dateReference) {
		final Localite localite;
		localite = service.getLocaliteByONRP(numeroLocalite, dateReference);
		if (localite == null) {
			throw new RuntimeException("La localité avec le numéro " + numeroLocalite + " est inconnue !");
		}
		return localite;
	}

	/**
	 * Extrait le numéro postal de la localité
	 */
	@Override
	public String getNumeroPostal() {
		final Integer noOrdrePostal = getNumeroOrdrePostal();
		if (noOrdrePostal != null) {
			final Localite localite = getLocalite(noOrdrePostal, getDateFin());
			return String.valueOf(localite.getNPA());
		}
		else {
			return null;
		}
	}
}
