package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Tiers;
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
	private final Source source;

	public AdresseSupplementaireAdapter(AdresseSupplementaire adresse, Tiers tiers, boolean isDefault, ServiceInfrastructureService service) {

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
		this.source = new Source(SourceType.FISCALE, tiers);
	}

	/**
	 * @return l'adresse adaptée.
	 */
	public AdresseSupplementaire getAdresse() {
		return adresse;
	}

	@Override
	public CasePostale getCasePostale() {
		final TexteCasePostale cp = adresse.getTexteCasePostale();
		final Integer no = adresse.getNumeroCasePostale();
		if (cp == null || no == null) {
			return null;
		}
		return new CasePostale(cp, no);
	}

	@Override
	public RegDate getDateDebut() {
		return adresse.getDateDebut();
	}

	@Override
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

	@Override
	public String getNumero() {
		return adresse.getNumeroMaison();
	}

	@Override
	public String getNumeroAppartement() {
		return adresse.getNumeroAppartement();
	}

	@Override
	public Integer getNumeroRue() {
		if (adresseSuisse != null) {
			return adresseSuisse.getNumeroRue();
		}
		else {
			return null;
		}
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public String getComplement() {
		return adresse.getComplement();
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	private Localite getLocalite(AdresseSuisse adresse) {
		final Integer noLocalite = getNumeroOrdreLocalite(adresse);
		final Localite localite;
		localite = service.getLocaliteByONRP(noLocalite);
		Assert.notNull(localite, "La localité avec le numéro " + noLocalite + " n'existe pas.");
		return localite;
	}

	private Integer getNumeroOrdreLocalite(AdresseSuisse adresse) {
		final Integer noLocalite;
		// On passe par le rue, si elle est spécifiée
		final Integer numeroRue = adresse.getNumeroRue();
		if (numeroRue != null) {
			final Rue rue;
			rue = service.getRueByNumero(numeroRue);
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
	@Override
	public boolean isValidAt(RegDate date) {
		// [UNIREG-2895] on ignore les adresses annulées ne doivent pas être considérées comme valides
		return !isAnnule() && RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	@Override
	public Date getAnnulationDate() {
		return adresse.getAnnulationDate();
	}

	@Override
	public String getAnnulationUser() {
		return adresse.getAnnulationUser();
	}

	@Override
	public Date getLogCreationDate() {
		return adresse.getLogCreationDate();
	}

	@Override
	public String getLogCreationUser() {
		return adresse.getLogCreationUser();
	}

	@Override
	public Timestamp getLogModifDate() {
		return adresse.getLogModifDate();
	}

	@Override
	public String getLogModifUser() {
		return adresse.getLogModifUser();
	}

	@Override
	public boolean isAnnule() {
		return adresse.isAnnule();
	}

	@Override
	public Long getId() {
		return adresse.getId();
	}

	@Override
	public boolean isPermanente() {
		return adresse.isPermanente();
	}

	@Override
	public Commune getCommuneAdresse() {
		// les adresses purement fiscales ne sont pas attachées à des communes directement
		// -> il faut donc passer par la localité postale
		return null;
	}

	@Override
	public Integer getEgid() {
		return null;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}
}
