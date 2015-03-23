package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Localite;
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
	private final String rue;

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

		this.rue = resolveNomRue(getNumeroRue(), adresse.getRue());

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
		if (cp == null) {
			return null;
		}
		Integer npa = null;
		if (adresseSuisse != null && adresseSuisse.getNpaCasePostale() != null) {
			npa = adresseSuisse.getNpaCasePostale();
		}
		return new CasePostale(cp, no, npa);
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
				nomLocalite = getLocalite(adresseSuisse).getNomAbrege();
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
			nomLocalite = super.getLocaliteComplete();
			if (nomLocalite == null) {
				nomLocalite = getLocalite(adresseSuisse).getNomComplet();
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
			if (adresseSuisse.getTexteCasePostale() != null && adresseSuisse.getNpaCasePostale() != null) {
				// [SIFISC-143] surcharge du NPA de la localité par le npa de la case postale
				return adresseSuisse.getNpaCasePostale().toString();
			}
			return localite.getNPA().toString();
		}
		else {
			Assert.notNull(adresseEtrangere);
			return ""; // est inclus dans la localité
		}
	}

	@Override
	public String getNumero() {
		// [SIFISC-4623] On ne tient compte du numéro de maison que si la rue est renseignée
		return rue == null ? null : adresse.getNumeroMaison();
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
	public Integer getNumeroOrdrePostal() {
		if (adresseSuisse != null) {
			return getNumeroOrdreLocalite(adresseSuisse);
		}
		else {
			Assert.notNull(adresseEtrangere);
			return null;
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
		final int noLocalite = getNumeroOrdreLocalite(adresse);
		final Localite localite;
		localite = service.getLocaliteByONRP(noLocalite, adresse.getDateFin());
		Assert.notNull(localite, "La localité avec le numéro " + noLocalite + " n'existe pas.");
		return localite;
	}

	private int getNumeroOrdreLocalite(AdresseSuisse adresse) {
		final Integer noLocalite = adresse.getNumeroOrdrePoste();
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

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		// les adresses purement fiscales ne sont pas attachées à des communes directement
		// -> il faut donc passer par la localité postale
		return null;
	}

	@Override
	public Integer getEgid() {
		return null;
	}

	@Override
	public Integer getEwid() {
		return null;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}
}
