package ch.vd.unireg.tiers.view;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.Tiers;

/**
 * Form backing-object de l'onglet "complément" associé à un tiers.
 */
public class ComplementView {

	private final String personneContact;
	private final String complementNom;
	private final String numeroTelephonePrive;
	private final String numeroTelephonePortable;
	private final String numeroTelephoneProfessionnel;
	private final String numeroTelecopie;
	private final String adresseCourrierElectronique;
	private final List<CoordonneesFinancieresView> coordonneesFinancieres;
	private final Boolean blocageRemboursementAutomatique;

	public ComplementView(@NotNull Tiers tiers, boolean coordonneesHisto, @NotNull IbanValidator ibanValidator) {

		// nom
		this.personneContact = tiers.getPersonneContact();
		this.complementNom = tiers.getComplementNom();

		// téléphone
		this.numeroTelecopie = tiers.getNumeroTelecopie();
		this.numeroTelephonePortable = tiers.getNumeroTelephonePortable();
		this.numeroTelephonePrive = tiers.getNumeroTelephonePrive();
		this.numeroTelephoneProfessionnel = tiers.getNumeroTelephoneProfessionnel();
		this.adresseCourrierElectronique = tiers.getAdresseCourrierElectronique();

		// coordonnées financières
		this.coordonneesFinancieres = tiers.getCoordonneesFinancieres().stream()
				.filter(c -> c.isValidAt(null) || coordonneesHisto)
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.map(c -> new CoordonneesFinancieresView(c, ibanValidator))
				.collect(Collectors.toList());

		this.blocageRemboursementAutomatique = tiers.getBlocageRemboursementAutomatique();
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public String getComplementNom() {
		return complementNom;
	}

	public String getNumeroTelephonePrive() {
		return numeroTelephonePrive;
	}

	public String getNumeroTelephonePortable() {
		return numeroTelephonePortable;
	}

	public String getNumeroTelephoneProfessionnel() {
		return numeroTelephoneProfessionnel;
	}

	public String getNumeroTelecopie() {
		return numeroTelecopie;
	}

	public String getAdresseCourrierElectronique() {
		return adresseCourrierElectronique;
	}

	public List<CoordonneesFinancieresView> getCoordonneesFinancieres() {
		return coordonneesFinancieres;
	}

	public Boolean getBlocageRemboursementAutomatique() {
		return blocageRemboursementAutomatique;
	}
}
