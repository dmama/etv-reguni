package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;

/*
 * Cette classe permet d'adapter une adresse personne morale à l'interface d'adresse générique, optionnellement en surchargeant ses dates de début/fin de validité.
 */
public class AdressePMAdapter implements AdresseGenerique {

	private final RegDate debutValiditeSurcharge;
	private final RegDate finValiditeSurcharge;
	private final AdresseEntreprise adresse;
	private final Source source;
	private final boolean isDefault;
	private final String rue;

	public AdressePMAdapter(AdresseEntreprise adresse, Entreprise entreprise, boolean isDefault) {
		this.adresse = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		this.source = new Source(SourceType.PM, entreprise);
		this.isDefault = isDefault;
		this.rue = StringUtils.trimToNull(adresse.getRue());
		DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
	}

	public AdressePMAdapter(AdresseEntreprise adresse, RegDate debutValiditeSurcharge, RegDate finValiditeSurcharge, Source source, boolean isDefault) {
		this.adresse = adresse;
		this.debutValiditeSurcharge = debutValiditeSurcharge;
		this.finValiditeSurcharge = finValiditeSurcharge;
		this.source = source;
		this.isDefault = isDefault;
		this.rue = StringUtils.trimToNull(adresse.getRue());
		DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
	}

	@Override
	public CasePostale getCasePostale() {
		return null;
	}

	@Override
	public RegDate getDateDebut() {
		if (debutValiditeSurcharge == null) {
			return adresse.getDateDebutValidite();
		}
		else {
			return debutValiditeSurcharge;
		}
	}

	@Override
	public RegDate getDateFin() {
		if (finValiditeSurcharge == null) {
			return adresse.getDateFinValidite();
		}
		else {
			return finValiditeSurcharge;
		}
	}

	@Override
	public String getLocalite() {
		return adresse.getLocaliteAbregeMinuscule();
	}

	@Override
	public String getLocaliteComplete() {
		return adresse.getLocaliteAbregeMinuscule();
	}

	@Override
	public String getNumero() {
		// [SIFISC-4623] On ne tient compte du numéro de maison que si la rue est renseignée
		return rue == null ? null : adresse.getNumeroMaison();
	}

	@Override
	public String getNumeroAppartement() {
		return null;
	}

	@Override
	public Integer getNumeroRue() {
		return adresse.getNumeroTechniqueRue();
	}

	@Override
	public int getNumeroOrdrePostal() {
		return adresse.getNumeroOrdrePostal();
	}

	@Override
	public String getNumeroPostal() {
		return adresse.getNumeroPostal();
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return adresse.getNumeroPostalComplementaire();
	}

	@Override
	public Integer getNoOfsPays() {
		return adresse.getPays() == null ? ServiceInfrastructureService.noOfsSuisse : adresse.getPays().getNoOFS();
	}

	@Override
	public String getRue() {
		return rue;
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public String getComplement() {
		return adresse.getComplement();
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		// devra changer si un jour les adresses d'entreprises pointent
		// directement vers une commune...
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
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
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
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	@Override
	public Long getId() {
		return null;
	}

	@Override
	public boolean isPermanente() {
		return false;
	}
}
