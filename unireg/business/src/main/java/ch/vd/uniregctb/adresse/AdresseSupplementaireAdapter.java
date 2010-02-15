package ch.vd.uniregctb.adresse;

import java.util.Date;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TexteCasePostale;

/*
 * Cette classe permet d'adapter une adresse supplémentaire (= spécialité UniregCTB) à l'interface d'adresse générique.
 */
public class AdresseSupplementaireAdapter extends AdresseAdapter {

	private static final long serialVersionUID = 8824009242893279454L;

	private final AdresseSupplementaire adresse;


	private final AdresseSuisse adresseSuisse;
	private final AdresseEtrangere adresseEtrangere;

	private final boolean isDefault;

	public AdresseSupplementaireAdapter(AdresseSupplementaire adresse, boolean isDefault, ServiceInfrastructureService service) {

		super(service);
		Assert.notNull(adresse);
		this.adresse = adresse;
		this.isDefault = isDefault;

		if (adresse instanceof AdresseSuisse) {
			this.adresseSuisse = (AdresseSuisse) adresse;
			this.adresseEtrangere = null;
		}
		else {
			this.adresseSuisse = null;
			this.adresseEtrangere = (AdresseEtrangere) adresse;
		}
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
		}
	}

	/**
	 * @return l'adresse adaptée.
	 */
	public AdresseSupplementaire getAdresse() {
		return adresse;
	}

	public String getCasePostale() {
		final TexteCasePostale cp = adresse.getTexteCasePostale();
		final Integer no = adresse.getNumeroCasePostale();
		if (cp == null || no == null) {
			return null;
		}
		return cp.format(no);
	}

	public RegDate getDateDebut() {
		return adresse.getDateDebut();
	}

	public RegDate getDateFin() {
		return adresse.getDateFin();
	}

	public String getLieu() {
		return getLocalite();
	}

	@Override
	public String getLocalite() {
		String nomLocalite = null;
		if (adresseSuisse != null) {
			nomLocalite = super.getLocalite();
			if (nomLocalite == null) {

				nomLocalite = getLocalite(adresseSuisse).getNomAbregeMinuscule();
			}
			return nomLocalite;
		}
		else {
			Assert.notNull(adresseEtrangere);
			return adresseEtrangere.getNumeroPostalLocalite(); // contient le npa + la localité + le complément npa
		}
	}

	@Override
	public String getLocaliteComplete() {
		String nomLocalite = null;
		if (adresseSuisse != null) {
			nomLocalite = super.getLocalite();
			if (nomLocalite == null) {
				nomLocalite = getLocalite(adresseSuisse).getNomCompletMinuscule();
			}
			return nomLocalite;
		}
		else {
			Assert.notNull(adresseEtrangere);
			return adresseEtrangere.getNumeroPostalLocalite(); // contient le npa + la localité + le complément npa
		}
	}

	public String getNpa() {
		if (adresseSuisse != null) {
			final Localite localite = getLocalite(adresseSuisse);
			return localite.getNPA().toString();
		}
		else {
			Assert.notNull(adresseEtrangere);
			return ""; // est inclus dans la localité
		}
	}

	public String getNumero() {
		return adresse.getNumeroMaison();
	}

	public String getNumeroAppartement() {
		return adresse.getNumeroAppartement();
	}

	public Integer getNumeroRue() {
		if (adresseSuisse != null) {
			return adresseSuisse.getNumeroRue();
		}
		else {
			return null;
		}
	}

	public int getNumeroOrdrePostal() {
		if (adresseSuisse != null) {
			return getNumeroOrdreLocalite(adresseSuisse);
		}
		else {
			Assert.notNull(adresseEtrangere);
			return 0;
		}
	}

	@Override
	public String getNumeroPostal() {
		return getNpa();
	}

	public String getNumeroPostalComplementaire() {
		if (adresseSuisse != null) {
			final Localite localite = getLocalite(adresseSuisse);
			final Integer complement = localite.getComplementNPA();
			return complement == null ? null : complement.toString();
		}
		else {
			Assert.notNull(adresseEtrangere);
			return ""; // est inclus dans la localité
		}
	}

	public Integer getNoOfsPays() {
		if (adresseSuisse != null) {
			return ServiceInfrastructureService.noOfsSuisse;
		}
		else {
			Assert.notNull(adresseEtrangere);
			return adresseEtrangere.getNumeroOfsPays();
		}
	}

	@Override
	public String getRue() {

		String rue = super.getRue();
		if (rue == null) {
			rue = adresse.getRue();
		}

		return rue;
	}

	public String getComplement() {
		return adresse.getComplement();
	}

	public Source getSource() {
		return Source.FISCALE;
	}

	public boolean isDefault() {
		return isDefault;
	}

	private Localite getLocalite(AdresseSuisse adresse) {
		final Integer noLocalite = getNumeroOrdreLocalite(adresse);
		final Localite localite;
		try {
			localite = service.getLocaliteByONRP(noLocalite);
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Erreur en essayant de récupérer la localité avec le numéro = " + noLocalite, e);
		}
		Assert.notNull(localite, "La localité avec le numéro " + noLocalite + " n'existe pas.");
		return localite;
	}

	private Integer getNumeroOrdreLocalite(AdresseSuisse adresse) {
		final Integer noLocalite;
		// On passe par le rue, si elle est spécifiée
		final Integer numeroRue = adresse.getNumeroRue();
		if (numeroRue != null) {
			final Rue rue;
			try {
				rue = service.getRueByNumero(numeroRue);
			}
			catch (InfrastructureException e) {
				throw new RuntimeException("Erreur en essayant de récupérer la rue avec le numéro technique = " + numeroRue, e);
			}
			noLocalite = rue.getNoLocalite();
		}
		else {
			noLocalite = adresse.getNumeroOrdrePoste();
		}
		Assert.notNull(noLocalite, "Impossible de déterminer le numéro de localité de l'adresse suisse ID = " + adresse.getId());
		return noLocalite;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	public Date getAnnulationDate() {
		return adresse.getAnnulationDate();
	}

	public String getAnnulationUser() {
		return adresse.getAnnulationUser();
	}

	public Date getLogCreationDate() {
		return adresse.getLogCreationDate();
	}

	public String getLogCreationUser() {
		return adresse.getLogCreationUser();
	}

	public Date getLogModifDate() {
		return adresse.getLogModifDate();
	}

	public String getLogModifUser() {
		return adresse.getLogModifUser();
	}

	public boolean isAnnule() {
		return adresse.isAnnule();
	}

	public Long getId() {
		return adresse.getId();
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}
}
