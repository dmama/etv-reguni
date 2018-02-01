package ch.vd.unireg.efacture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.common.FormatNumeroHelper;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
public class DemandeAvecHistoView {

	private final String idDemande;
	private final RegDate dateDemande;
	private final BigInteger noAdherent;
	private final String avs;
	private final String email;
	private final String descriptionTypeDemande;
	private final List<EtatDemandeView> etats;

	public DemandeAvecHistoView(String idDemande, RegDate dateDemande, BigInteger noAdherent, String avs, String email, Demande.Action actionDemande, List<EtatDemandeView> etats) {
		this.idDemande = idDemande;
		this.dateDemande = dateDemande;
		this.noAdherent = noAdherent;

		// [SIFISC-12805] pour les cas où le numéro de sécurité sociale n'est pas un numéro AVS (et comprend éventuellement même des lettres), il faut l'afficher quand-même (mais sans formattage)
		final String formattedAvs = FormatNumeroHelper.formatNumAVS(avs);
		this.avs = StringUtils.isNotBlank(formattedAvs) ? formattedAvs : StringUtils.trimToEmpty(avs);

		this.email = StringUtils.trimToEmpty(email);
		this.descriptionTypeDemande = actionDemande != null ? actionDemande.getDescription() : StringUtils.EMPTY;

		if (etats == null || etats.isEmpty()) {
			throw new IllegalArgumentException("etats ne peut être ni null ni vide");
		}
		this.etats = new ArrayList<>(etats);
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getIdDemande() {
		return idDemande;
	}

	@SuppressWarnings("UnusedDeclaration")
	public List<EtatDemandeView> getEtats() {
		return etats;
	}

	@SuppressWarnings("UnusedDeclaration")
	public RegDate getDateDemande() {
		return dateDemande;
	}

	public BigInteger getNoAdherent() {
		return noAdherent;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getDescriptionTypeDemande() {
		return descriptionTypeDemande;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getAvs() {
		return avs;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getEmail() {
		return email;
	}

	public EtatDemandeView getEtatCourant() {
		return etats.get(0);
	}

	@SuppressWarnings("UnusedDeclaration")
	private TypeEtatDemande getTypeEtatCourant() {
		return getEtatCourant().getType();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isValidable() {
		return getEtatCourant().isValidable();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isRefusable() {
		return getEtatCourant().isRefusable();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isMettableEnAttenteContact() {
		return getEtatCourant().isMettableEnAttenteContact();
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isMettableEnAttenteSignature() {
		return getEtatCourant().isMettableEnAttenteSignature();
	}
}
