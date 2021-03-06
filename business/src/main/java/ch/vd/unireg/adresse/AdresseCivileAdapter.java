package ch.vd.unireg.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.Tiers;

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
	 * @throws ch.vd.unireg.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, Tiers tiers, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		if (adresse == null) {
			throw new IllegalArgumentException();
		}
		this.adresse = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		this.source = new Source(getSourceType(tiers), tiers);
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);

		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult.hasErrors()) {
			throw new DonneesCivilesException(buildContext(adresse), validationResult.getErrors());
		}
	}

	@NotNull
	public static AdresseGenerique.SourceType getSourceType(Tiers tiers) {
		if (tiers instanceof Entreprise || tiers instanceof Etablissement) {
			return SourceType.CIVILE_ENT;
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			return SourceType.INFRA;
		}
		else {
			return SourceType.CIVILE_PERS;
		}
	}

	/**
	 * @param adresse   l'adresse civile à adapter
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 * @param service   le service infrastructure
	 * @throws ch.vd.unireg.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, Source source, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		if (adresse == null) {
			throw new IllegalArgumentException();
		}
		this.adresse = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		if (source == null) {
			throw new IllegalArgumentException();
		}
		this.source = source;
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);

		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult.hasErrors()) {
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
	 * @throws ch.vd.unireg.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, Tiers tiers, RegDate debut, RegDate fin, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		if (adresse == null) {
			throw new IllegalArgumentException();
		}
		this.adresse = adresse;
		this.debutValiditeSurcharge = debut;
		this.finValiditeSurcharge = fin;
		this.source = new Source(SourceType.CIVILE_PERS, tiers);
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);
		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult.hasErrors()) {
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
	 * @throws ch.vd.unireg.common.DonneesCivilesException
	 *          en cas d'erreur dans les données du service civil
	 */
	public AdresseCivileAdapter(Adresse adresse, RegDate debut, RegDate fin, Source source, boolean isDefault, ServiceInfrastructureService service) throws DonneesCivilesException {
		super(service);
		if (adresse == null) {
			throw new IllegalArgumentException();
		}
		this.adresse = adresse;
		this.debutValiditeSurcharge = debut;
		this.finValiditeSurcharge = fin;
		if (source == null) {
			throw new IllegalArgumentException();
		}
		this.source = source;
		this.isDefault = isDefault;
		this.complement = extractComplement(adresse);
		this.rue = resolveNomRue(adresse.getNumeroRue(), adresse.getRue());

		final ValidationResults validationResult = ValidationHelper.validate(this, true, true);
		if (validationResult.hasErrors()) {
			throw new DonneesCivilesException(buildContext(adresse), validationResult.getErrors());
		}
	}

	private static String buildContext(Adresse adresse) {
		return "adresse civile " + adresse.getTypeAdresse().getDescription() + " :";
	}

	/**
	 * Extrait le complément de l'adresse, et préfixe par "p.a." si nécessaire [UNIREG-723].
	 *
	 * @param adresse l'adresse civil dont on veut obtenir le complément
	 * @return le complément; ou <b>null</b> si ce dernier n'est pas renseigné
	 */
	public static String extractComplement(Adresse adresse) {
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
	public Integer getNumeroOrdrePostal() {
		return adresse.getNumeroOrdrePostal();
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
