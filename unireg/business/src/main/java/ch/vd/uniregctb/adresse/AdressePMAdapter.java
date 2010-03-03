package ch.vd.uniregctb.adresse;

import java.util.Date;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/*
 * Cette classe permet d'adapter une adresse personne morale à l'interface d'adresse générique, optionnellement en surchargeant ses dates de début/fin de validité.
 */
public class AdressePMAdapter implements AdresseGenerique {

	private final RegDate debutValiditeSurcharge;
	private final RegDate finValiditeSurcharge;
	private final AdresseEntreprise adresse;
	private final Source source;
	private final boolean isDefault;

	public AdressePMAdapter(AdresseEntreprise adresse, boolean isDefault) {
		this.adresse = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		this.source = Source.PM;
		this.isDefault = isDefault;
		DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
	}

	public AdressePMAdapter(AdresseEntreprise adresse, RegDate debutValiditeSurcharge, RegDate finValiditeSurcharge, Source source,
			boolean isDefault) {
		this.adresse = adresse;
		this.debutValiditeSurcharge = debutValiditeSurcharge;
		this.finValiditeSurcharge = finValiditeSurcharge;
		this.source = source;
		this.isDefault = isDefault;
		DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
	}

	public String getCasePostale() {
		return null;
	}

	public RegDate getDateDebut() {
		if (debutValiditeSurcharge == null) {
			return adresse.getDateDebutValidite();
		}
		else {
			return debutValiditeSurcharge;
		}
	}

	public RegDate getDateFin() {
		if (finValiditeSurcharge == null) {
			return adresse.getDateFinValidite();
		}
		else {
			return finValiditeSurcharge;
		}
	}

	public String getLocalite() {
		return adresse.getLocaliteAbregeMinuscule();
	}

	public String getLocaliteComplete() {
		return adresse.getLocaliteAbregeMinuscule();
	}

	public String getNumero() {
		return null;
	}

	public String getNumeroAppartement() {
		return null;
	}

	public Integer getNumeroRue() {
		return null;
	}

	public int getNumeroOrdrePostal() {
		return 0;
	}

	public String getNumeroPostal() {
		return null;
	}

	public String getNumeroPostalComplementaire() {
		return null;
	}

	public Integer getNoOfsPays() {
		return ServiceInfrastructureService.noOfsSuisse; // par définition toutes les PM sont en Suisse
	}

	public String getRue() {
		return adresse.getRue();
	}

	public Source getSource() {
		return source;
	}

	public String getComplement() {
		return adresse.getComplement();
	}

	public boolean isDefault() {
		return isDefault;
	}

	public CommuneSimple getCommuneAdresse() {
		// devra changer si un jour les adresses d'entreprises pointent
		// directement vers une commune...
		return null;
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	public Date getAnnulationDate() {
		return null;
	}

	public String getAnnulationUser() {
		return null;
	}

	public Date getLogCreationDate() {
		return null;
	}

	public String getLogCreationUser() {
		return null;
	}

	public Date getLogModifDate() {
		return null;
	}

	public String getLogModifUser() {
		return null;
	}

	public boolean isAnnule() {
		return false;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	public Long getId() {
		return null;
	}
}
