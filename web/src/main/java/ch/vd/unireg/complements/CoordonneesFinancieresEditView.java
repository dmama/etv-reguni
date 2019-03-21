package ch.vd.unireg.complements;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Tiers;

@SuppressWarnings("UnusedDeclaration")
public class CoordonneesFinancieresEditView implements DateRange, Annulable {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Date annulationDate;
	private String oldIban;
	private String iban;
	private String titulaireCompteBancaire;
	private String adresseBicSwift;

	public CoordonneesFinancieresEditView() {
	}

	public CoordonneesFinancieresEditView(@NotNull Tiers tiers) {
		this.id = tiers.getId();

		final CoordonneesFinancieres coords = tiers.getCoordonneesFinancieresCourantes();
		if (coords != null) {
			this.dateDebut = coords.getDateDebut();
			this.dateFin = coords.getDateFin();
			this.annulationDate = coords.getAnnulationDate();
			this.titulaireCompteBancaire = coords.getTitulaire();

			final CompteBancaire compteBancaire = coords.getCompteBancaire();
			if (compteBancaire != null) {
				this.oldIban = IbanHelper.normalize(compteBancaire.getIban());
				this.iban = IbanHelper.normalize(compteBancaire.getIban());
				this.adresseBicSwift = compteBancaire.getBicSwift();
			}
		}
	}

	public CoordonneesFinancieresEditView(@NotNull CoordonneesFinancieres coords) {
		this.id = coords.getId();
		this.dateDebut = coords.getDateDebut();
		this.dateFin = coords.getDateFin();
		this.annulationDate = coords.getAnnulationDate();
		this.titulaireCompteBancaire = coords.getTitulaire();

		final CompteBancaire compteBancaire = coords.getCompteBancaire();
		if (compteBancaire != null) {
			this.oldIban = IbanHelper.normalize(compteBancaire.getIban());
			this.iban = IbanHelper.normalize(compteBancaire.getIban());
			this.adresseBicSwift = compteBancaire.getBicSwift();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getIban() {
		return IbanHelper.toDisplayString(iban);
	}

	public void setIban(String iban) {
		this.iban = IbanHelper.normalize(iban);
	}

	public String getOldIban() {
		return oldIban;
	}

	public void setOldIban(String oldIban) {
		this.oldIban = oldIban;
	}

	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public void setTitulaireCompteBancaire(String titulaireCompteBancaire) {
		this.titulaireCompteBancaire = titulaireCompteBancaire;
	}

	public String getAdresseBicSwift() {
		return adresseBicSwift;
	}

	public void setAdresseBicSwift(String adresseBicSwift) {
		this.adresseBicSwift = adresseBicSwift;
	}

	@Override
	public boolean isAnnule() {
		return annulationDate != null;
	}

	public boolean isEmpty() {
		return StringUtils.isEmpty(iban) && StringUtils.isEmpty(oldIban) && dateDebut == null;
	}
}
