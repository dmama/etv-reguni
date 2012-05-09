package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/*
 * Cette classe permet d'adapter une adresse civile à l'interface d'adresse générique, optionnellement en surchargeant ses dates de début/fin de validité.
 */

public class AdresseTiersCivileAdapter extends AdresseAdapter {


	private final Adresse adresse;
	private final AdresseCivile adresseCivileSurcharge;
	private final Source source;
	private final boolean isDefault;
	private final String complement; // le complément de l'adresse, préfixée par un "p.a."e
	private final String rue;

	/**
	 * @param adresse                 l'adresse civile à adapter
	 * @param adresseCivileSurchargee  Adresse tiers de type civile
	 * @param source                  la source de l'adresse à publier
	 * @param isDefault               vrai si l'adresse représente une adresse par défaut
	 * @param service                 le service infrastructure
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseTiersCivileAdapter(Adresse adresse, AdresseCivile adresseCivileSurchargee, Source source, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		Assert.notNull(adresse);
		this.adresse = adresse;
		this.adresseCivileSurcharge = adresseCivileSurchargee;
		this.source = source;
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);
		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult != null && validationResult.hasErrors()) {
			throw new DonneesCivilesException(buildContext(adresse), validationResult.getErrors());
		}
	}

	private static String buildContext(Adresse adresse) {
		final String type;
		if (TypeAdresseCivil.COURRIER == adresse.getTypeAdresse()) {
			type = "courrier";
		}
		else if (TypeAdresseCivil.PRINCIPALE == adresse.getTypeAdresse()) {
			type = "principal";
		}
		else if (TypeAdresseCivil.SECONDAIRE == adresse.getTypeAdresse()) {
			type = "secondaire";
		}
		else {
			type = "tutelle";
		}
		return "adresse civile " + type + " :";
	}

	/**
	 * Extrait le complément de l'adresse, et préfixe par "p.a." si nécessaire [UNIREG-723].
	 *
	 * @param adresse l'adresse civil dont on veut obtenir le complément
	 * @return le complément; ou <b>null</b> si ce dernier n'est pas renseigné
	 */
	private String extractComplement(Adresse adresse) {
		final String titre = adresse.getTitre();
		if (titre == null) {
			return null;
		}
		else {
			return AdresseServiceImpl.prefixByPourAdresseIfNeeded(titre);
		}
	}

	@Override
	public CasePostale getCasePostale() {
		return adresse.getCasePostale();
	}

	@Override
	public final RegDate getDateDebut() {

		return adresseCivileSurcharge.getDateDebut();

	}

	@Override
	public final RegDate getDateFin() {

		return adresseCivileSurcharge.getDateFin();

	}

	@Override
	public String getLocalite() {
		String nomLocalite = super.getLocalite();
		if (nomLocalite != null) {
			return nomLocalite;
		}
		else {
			return adresse.getLocalite();
		}
	}

	@Override
	public String getLocaliteComplete() {
		String nomLocalite = super.getLocaliteComplete();
		if (nomLocalite != null) {
			return nomLocalite;
		}
		else {
			return adresse.getLocalite();
		}
	}

	@Override
	public String getNumero() {
		// [SIFISC-4623] On ne tient compte du numéro de maison que si la rue est renseignée
		return rue == null ? null : adresse.getNumero();
	}

	@Override
	public String getNumeroAppartement() {
		return adresse.getNumeroAppartement();
	}

	@Override
	public Integer getNumeroRue() {
		return adresse.getNumeroRue();
	}

	@Override
	public int getNumeroOrdrePostal() {
		final int noOrdreFourni = adresse.getNumeroOrdrePostal();
		if (noOrdreFourni == 0) {
			return super.getNumeroOrdrePostal();
		}
		else {
			return noOrdreFourni;
		}
	}

	@Override
	public String getNumeroPostal() {
		final String numeroFourni = adresse.getNumeroPostal();
		if (numeroFourni == null || numeroFourni.trim().length() == 0) {
			return super.getNumeroPostal();
		}
		else {
			return numeroFourni;
		}
	}


	@Override
	public String getNumeroPostalComplementaire() {
		return adresse.getNumeroPostalComplementaire();
	}

	@Override
	public Integer getNoOfsPays() {
		return adresse.getNoOfsPays();
	}

	@Override
	public String getRue() {
		return rue;
	}

	@Override
	public String getComplement() {
		return complement;
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof AdresseTiersCivileAdapter) {
			final AdresseTiersCivileAdapter right = (AdresseTiersCivileAdapter) obj;

			return (this.adresseCivileSurcharge.getDateDebut() == null ? right.adresseCivileSurcharge.getDateDebut() == null :
					this.adresseCivileSurcharge.getDateDebut().equals(right.adresseCivileSurcharge.getDateDebut()))
					&& (this.adresseCivileSurcharge.getDateFin() == null ? right.adresseCivileSurcharge.getDateFin() == null :
					this.adresseCivileSurcharge.getDateFin().equals(right.adresseCivileSurcharge.getDateFin()))
					&& this.adresse == right.adresse
					&& this.source == right.source
					&& this.isDefault == right.isDefault;
		}
		else {
			return false;
		}
	}

	@Override
	public Date getAnnulationDate() {
		return adresseCivileSurcharge.getAnnulationDate();

	}

	@Override
	public String getAnnulationUser() {
		return adresseCivileSurcharge.getAnnulationUser();
	}

	@Override
	public Date getLogCreationDate() {
		return adresseCivileSurcharge.getLogCreationDate();
	}

	@Override
	public String getLogCreationUser() {
		return adresseCivileSurcharge.getLogCreationUser();
	}

	@Override
	public Timestamp getLogModifDate() {
		return adresseCivileSurcharge.getLogModifDate();
	}

	@Override
	public String getLogModifUser() {
		return adresseCivileSurcharge.getAnnulationUser();
	}

	@Override
	public boolean isAnnule() {
		if (adresseCivileSurcharge.getAnnulationDate() != null) {
			return true;
		}
		return false;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return adresse.getNoOfsCommuneAdresse();
	}

	@Override
	public Integer getEgid() {
		return adresse.getEgid();
	}

	@Override
	public Integer getEwid() {
		return adresse.getEwid();
	}

	@Override
	public Long getId() {
		return adresseCivileSurcharge.getId();
	}

	@Override
	public boolean isPermanente() {
		return false;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}
}
