package ch.vd.unireg.tiers.view;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.CoordonneesFinancieres;

@SuppressWarnings("UnusedDeclaration")
public class CoordonneesFinancieresView implements DateRange, Annulable {
	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Date annulationDate;
	private String titulaireCompteBancaire;
	private String iban;
	private String ibanValidationMessage;
	private String adresseBicSwift;

	public CoordonneesFinancieresView() {
	}

	public CoordonneesFinancieresView(@NotNull CoordonneesFinancieres coords, @NotNull IbanValidator ibanValidator) {
		this.id = coords.getId();
		this.dateDebut = coords.getDateDebut();
		this.dateFin = coords.getDateFin();
		this.annulationDate = coords.getAnnulationDate();
		this.titulaireCompteBancaire = coords.getTitulaire();
		final CompteBancaire compteBancaire = coords.getCompteBancaire();
		this.iban = (compteBancaire == null ? null : compteBancaire.getIban());
		this.ibanValidationMessage = verifierIban(iban, ibanValidator); // [UNIREG-2582]
		this.adresseBicSwift = (compteBancaire == null ? null : compteBancaire.getBicSwift());
	}

	/**
	 * Permet renseigner la view sur le fait que l'iban du tiers associé est valide ou pas
	 *
	 * @param iban          l'iban à vérifier
	 * @param ibanValidator le validator d'iban
	 * @return <code>null</code> si l'IBAN est valide, explication textuelle de l'erreur sinon
	 */
	private static String verifierIban(String iban, IbanValidator ibanValidator) {
		if (StringUtils.isNotBlank(iban)) {
			return ibanValidator.getIbanValidationError(iban);
		}
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public void setTitulaireCompteBancaire(String titulaireCompteBancaire) {
		this.titulaireCompteBancaire = titulaireCompteBancaire;
	}

	public String getIban() {
		return IbanHelper.toDisplayString(iban);
	}

	public void setIban(String iban) {
		this.iban = IbanHelper.normalize(iban);
	}

	public String getIbanValidationMessage() {
		return ibanValidationMessage;
	}

	public void setIbanValidationMessage(String ibanValidationMessage) {
		this.ibanValidationMessage = ibanValidationMessage;
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
}
