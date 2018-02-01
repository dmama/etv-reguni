package ch.vd.unireg.tiers.view;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

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

	private CompteBancaireView compteBancaire = new CompteBancaireView();

	private Boolean blocageRemboursementAutomatique;

	private Long ancienNumeroSourcier;

	public ComplementView() {
	}

	public ComplementView(Tiers tiers, IbanValidator ibanValidator) {

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
		final String iban = tiers.getNumeroCompteBancaire();
		final String ibanValidationMessage = verifierIban(iban, ibanValidator); // [UNIREG-2582]
		compteBancaire = new CompteBancaireView(null, null, tiers.getNumero(), tiers.getTitulaireCompteBancaire(), null, null, null, iban, ibanValidationMessage, tiers.getAdresseBicSwift());

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			this.ancienNumeroSourcier = pp.getAncienNumeroSourcier();
		}

		this.blocageRemboursementAutomatique = tiers.getBlocageRemboursementAutomatique();
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

	/**
	 * @return le compte bancaire du tiers courant.
	 */
	public CompteBancaireView getCompteBancaire() {
		return compteBancaire;
	}

	public void setCompteBancaire(CompteBancaireView compteBancaire) {
		this.compteBancaire = compteBancaire;
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
