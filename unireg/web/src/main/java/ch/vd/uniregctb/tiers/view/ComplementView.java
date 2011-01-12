package ch.vd.uniregctb.tiers.view;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.PartPM;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Form backing-object de l'onglet "complément" associé à un tiers.
 */
public class ComplementView {

	private String personneContact;
	private String complementNom;
	private String numeroTelephonePrive;
	private String numeroTelephonePortable;
	private String numeroTelephoneProfessionnel;
	private String numeroTelecopie;
	private String adresseCourrierElectronique;

	private String numeroCompteBancaire;
	private String nomInstitutionCompteBancaire;
	private String ibanValidationMessage;
	private String titulaireCompteBancaire;
	private String adresseBicSwift;
	private Boolean blocageRemboursementAutomatique;

	private Long ancienNumeroSourcier;

	public ComplementView() {
	}

	public ComplementView(Tiers tiers, ServicePersonneMoraleService servicePM, IbanValidator ibanValidator) {

		if (tiers instanceof Entreprise) {

			final PersonneMorale pm = servicePM.getPersonneMorale(tiers.getNumero(), PartPM.MANDATS);
			if (pm != null) {
				// numéros de téléphone
				this.numeroTelephonePrive = pm.getTelephoneContact();
				this.numeroTelecopie = pm.getTelecopieContact();
				this.numeroTelephonePortable = null;
				this.numeroTelephoneProfessionnel = null;

				// comptes bancaires
				this.titulaireCompteBancaire = pm.getTitulaireCompte();

				final List<CompteBancaire> comptes = pm.getComptesBancaires();
				if (comptes != null && !comptes.isEmpty()) {
					final CompteBancaire c = comptes.get(0);
					this.numeroCompteBancaire = c.getNumero();
					this.nomInstitutionCompteBancaire = c.getNomInstitution();
					this.adresseBicSwift = null; // pas disponible
				}
			}
		}
		else {
			// nom
			this.personneContact = tiers.getPersonneContact();
			this.complementNom = tiers.getComplementNom();

			// téléphone
			this.numeroTelecopie = tiers.getNumeroTelecopie();
			this.numeroTelephonePortable = tiers.getNumeroTelephonePortable();
			this.numeroTelephonePrive = tiers.getNumeroTelephonePrive();
			this.numeroTelephoneProfessionnel = tiers.getNumeroTelephoneProfessionnel();
			this.adresseCourrierElectronique = tiers.getAdresseCourrierElectronique();

			// compte bancaire
			this.numeroCompteBancaire = tiers.getNumeroCompteBancaire();
			this.titulaireCompteBancaire = tiers.getTitulaireCompteBancaire();
			this.adresseBicSwift = tiers.getAdresseBicSwift();
			this.ibanValidationMessage = verifierIban(tiers, ibanValidator); // [UNIREG-2582]

			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				this.ancienNumeroSourcier = pp.getAncienNumeroSourcier();
			}
		}

		this.blocageRemboursementAutomatique = tiers.getBlocageRemboursementAutomatique();
	}

	/**
	 * Permet renseigner la view sur le fait que l'iban du tiers associé est valide ou pas
	 *
	 * @param tiers le tiers dont l'IBAN doit être vérifié
	 * @param ibanValidator
	 * @return <code>null</code> si l'IBAN est valide, explication textuelle de l'erreur sinon
	 */
	private static String verifierIban(Tiers tiers, IbanValidator ibanValidator) {
		final String iban = tiers.getNumeroCompteBancaire();
		if (StringUtils.isNotBlank(iban)) {
			return ibanValidator.getIbanValidationError(iban);
		}
		return null;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public String getComplementNom() {
		return complementNom;
	}

	public void setComplementNom(String complementNom) {
		this.complementNom = complementNom;
	}

	public String getNumeroTelephonePrive() {
		return numeroTelephonePrive;
	}

	public void setNumeroTelephonePrive(String numeroTelephonePrive) {
		this.numeroTelephonePrive = numeroTelephonePrive;
	}

	public String getNumeroTelephonePortable() {
		return numeroTelephonePortable;
	}

	public void setNumeroTelephonePortable(String numeroTelephonePortable) {
		this.numeroTelephonePortable = numeroTelephonePortable;
	}

	public String getNumeroTelephoneProfessionnel() {
		return numeroTelephoneProfessionnel;
	}

	public void setNumeroTelephoneProfessionnel(String numeroTelephoneProfessionnel) {
		this.numeroTelephoneProfessionnel = numeroTelephoneProfessionnel;
	}

	public String getNumeroTelecopie() {
		return numeroTelecopie;
	}

	public void setNumeroTelecopie(String numeroTelecopie) {
		this.numeroTelecopie = numeroTelecopie;
	}

	public String getAdresseCourrierElectronique() {
		return adresseCourrierElectronique;
	}

	public void setAdresseCourrierElectronique(String adresseCourrierElectronique) {
		this.adresseCourrierElectronique = adresseCourrierElectronique;
	}

	public String getNumeroCompteBancaire() {
		return numeroCompteBancaire;
	}

	public void setNumeroCompteBancaire(String numeroCompteBancaire) {
		this.numeroCompteBancaire = numeroCompteBancaire;
	}

	public String getNomInstitutionCompteBancaire() {
		return nomInstitutionCompteBancaire;
	}

	public void setNomInstitutionCompteBancaire(String nomInstitutionCompteBancaire) {
		this.nomInstitutionCompteBancaire = nomInstitutionCompteBancaire;
	}

	public String getIbanValidationMessage() {
		return ibanValidationMessage;
	}

	public void setIbanValidationMessage(String ibanValidationMessage) {
		this.ibanValidationMessage = ibanValidationMessage;
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

	public Boolean getBlocageRemboursementAutomatique() {
		return blocageRemboursementAutomatique;
	}

	public void setBlocageRemboursementAutomatique(Boolean blocageRemboursementAutomatique) {
		this.blocageRemboursementAutomatique = blocageRemboursementAutomatique;
	}

	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	public void setAncienNumeroSourcier(Long ancienNumeroSourcier) {
		this.ancienNumeroSourcier = ancienNumeroSourcier;
	}
}
