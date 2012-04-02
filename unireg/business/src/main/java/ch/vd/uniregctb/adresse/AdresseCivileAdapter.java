package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

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
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/*
 * Cette classe permet d'adapter une adresse civile à l'interface d'adresse générique, optionnellement en surchargeant ses dates de début/fin de validité.
 */
public class AdresseCivileAdapter extends AdresseAdapter {

	private final RegDate debutValiditeSurcharge;
	private final RegDate finValiditeSurcharge;
	private final Adresse adresse;
	private final Source source;
	private final boolean isDefault;
	private final String complement; // le complément de l'adresse, préfixée par un "p.a."
	private final String rue;

	/**
	 * @param adresse   l'adresse civile à adapter
	 * @param tiers     le tiers a qui l'adresse civil appartient
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 * @param service   le service infrastructure
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, Tiers tiers, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		Assert.notNull(adresse);
		this.adresse = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		this.source = new Source(SourceType.CIVILE, tiers);
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);

		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult != null && validationResult.hasErrors()) {
			throw new DonneesCivilesException(buildContext(adresse), validationResult.getErrors());
		}
	}

	/**
	 * @param adresse   l'adresse civile à adapter
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 * @param service   le service infrastructure
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, Source source, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		Assert.notNull(adresse);
		this.adresse = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		Assert.notNull(source);
		this.source = source;
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);

		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult != null && validationResult.hasErrors()) {
			throw new DonneesCivilesException(buildContext(adresse), validationResult.getErrors());
		}
	}

	/**
	 * @param adresse   l'adresse civile à adapter
	 * @param tiers     le tiers a qui l'adresse civil appartient
	 * @param debut     (option) une nouvelle adresse de début
	 * @param fin       (option) une nouvelle adresse de fin
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 * @param service   le service infrastructure
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, Tiers tiers, RegDate debut, RegDate fin, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		Assert.notNull(adresse);
		this.adresse = adresse;
		this.debutValiditeSurcharge = debut;
		this.finValiditeSurcharge = fin;
		this.source = new Source(SourceType.CIVILE, tiers);
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);
		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult != null && validationResult.hasErrors()) {
			throw new DonneesCivilesException(buildContext(adresse), validationResult.getErrors());
		}
	}

	/**
	 * @param adresse   l'adresse civile à adapter
	 * @param debut     (option) une nouvelle adresse de début
	 * @param fin       (option) une nouvelle adresse de fin
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 * @param service   le service infrastructure
	 * @throws ch.vd.uniregctb.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, RegDate debut, RegDate fin, Source source, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		Assert.notNull(adresse);
		this.adresse = adresse;
		this.debutValiditeSurcharge = debut;
		this.finValiditeSurcharge = fin;
		Assert.notNull(source);
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
		if (debutValiditeSurcharge == null) {
			return adresse.getDateDebut();
		}
		else {
			return debutValiditeSurcharge;
		}
	}

	@Override
	public final RegDate getDateFin() {
		if (finValiditeSurcharge == null) {
			return adresse.getDateFin();
		}
		else {
			return finValiditeSurcharge;
		}
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
		final int noOrdrePostal = super.getNumeroOrdrePostal();
		if (noOrdrePostal != 0) {
			return noOrdrePostal;
		}
		else {
			return adresse.getNumeroOrdrePostal();
		}
	}

	@Override
	public String getNumeroPostal() {
		final String npa = super.getNumeroPostal();
		if (!StringUtils.isBlank(npa)) {
			return npa;
		}
		else {
			return adresse.getNumeroPostal();
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

		if (obj instanceof AdresseCivileAdapter) {
			final AdresseCivileAdapter right = (AdresseCivileAdapter) obj;

			return (this.debutValiditeSurcharge == null ? right.debutValiditeSurcharge == null : this.debutValiditeSurcharge.equals(right.debutValiditeSurcharge))
					&& (this.finValiditeSurcharge == null ? right.finValiditeSurcharge == null : this.finValiditeSurcharge.equals(right.finValiditeSurcharge))
					&& this.adresse == right.adresse
					&& this.source.getType() == right.source.getType()
					&& this.isDefault == right.isDefault;
		}
		else {
			return false;
		}
	}

	@Override
	public Date getAnnulationDate() {
		return null;
	}

	@Override
	public String getAnnulationUser() {
		return null;
	}

	@Override
	public Date getLogCreationDate() {
		return null;
	}

	@Override
	public String getLogCreationUser() {
		return null;
	}

	@Override
	public Timestamp getLogModifDate() {
		return null;
	}

	@Override
	public String getLogModifUser() {
		return null;
	}

	@Override
	public boolean isAnnule() {
		return false;
	}

	@Override
	public Commune getCommuneAdresse() {
		return adresse.getCommuneAdresse();
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
		return null;
	}

	@Override
	public boolean isPermanente() {
		return false; // par définition, seules les adresses tiers peuvent être permanentes
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}
}
