package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Informations sur le mandataire général annoncé dans la déclaration
 */
public class InformationsMandataire {

	private final String ideMandataire;
	private final AdresseRaisonSociale adresse;
	private final Boolean sansCopieMandataire;
	private final String noTelContact;

	public InformationsMandataire(String ideMandataire, AdresseRaisonSociale adresse, Boolean sansCopieMandataire, String noTelContact) {
		this.ideMandataire = NumeroIDEHelper.normalize(StringUtils.trimToNull(ideMandataire));
		this.adresse = adresse;
		this.sansCopieMandataire = sansCopieMandataire;
		this.noTelContact = StringUtils.trimToNull(noTelContact);
	}

	public String getIdeMandataire() {
		return ideMandataire;
	}

	public AdresseRaisonSociale getAdresse() {
		return adresse;
	}

	public Boolean getSansCopieMandataire() {
		return sansCopieMandataire;
	}

	public String getNoTelContact() {
		return noTelContact;
	}

	public String getContact() {
		return adresse != null ? adresse.getContact() : null;
	}

	public boolean isNumeroIdeMandataireUtilisable() {
		return ideMandataire != null && !"CHE".equalsIgnoreCase(ideMandataire);
	}

	public boolean isNotEmpty() {
		return isNumeroIdeMandataireUtilisable() || adresse != null;
	}

	/**
	 * @return une chaîne de caractères descriptive du contenu des données reçues
	 * @param infraService service infrastructure
	 * @param adresseService service d'adresses
	 * @param dateReference date de référence pour les résolutions de noms...
	 */
	public String toDisplayString(ServiceInfrastructureService infraService, AdresseService adresseService, RegDate dateReference) {
		final List<String> lignes = new ArrayList<>(4);
		if (isNumeroIdeMandataireUtilisable()) {
			lignes.add(String.format("- IDE : %s", FormatNumeroHelper.formatNumIDE(ideMandataire)));
		}
		if (adresse != null) {
			lignes.add(String.format("- adresse : %s", adresse.toDisplayString(infraService, adresseService, dateReference)));
		}
		if (sansCopieMandataire != null) {
			lignes.add(String.format("- copie mandataire : %s", sansCopieMandataire ? "sans" : "avec"));
		}
		if (noTelContact != null) {
			lignes.add(String.format("- téléphone contact : %s", noTelContact));
		}
		return CollectionsUtils.toString(lignes, StringRenderer.DEFAULT, "\n");
	}
}
