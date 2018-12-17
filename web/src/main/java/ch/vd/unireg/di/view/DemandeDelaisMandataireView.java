package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.NumeroIDEHelper;
import ch.vd.unireg.mandataire.DemandeDelaisMandataire;

public class DemandeDelaisMandataireView {

	@Nullable
	private final Long numeroCtbMandataire;

	/**
	 * Le numéro IDE du mandataire (sans assurance de validité).
	 */
	private final String numeroIDE;

	/**
	 * La raison sociale du mandataire (sans assurance de validité).
	 */
	private final String raisonSociale;

	/**
	 * Le business ID de la demande.
	 */
	private final String businessId;

	/**
	 * Un ID de référence supplémentaire.
	 */
	@Nullable
	private final String referenceId;

	public DemandeDelaisMandataireView(@NotNull DemandeDelaisMandataire demande) {
		this.numeroCtbMandataire = demande.getNumeroCtbMandataire();
		this.numeroIDE = NumeroIDEHelper.formater(demande.getNumeroIDE());
		this.raisonSociale = demande.getRaisonSociale();
		this.businessId = demande.getBusinessId();
		this.referenceId = demande.getReferenceId();
	}

	@Nullable
	public Long getNumeroCtbMandataire() {
		return numeroCtbMandataire;
	}

	public String getNumeroIDE() {
		return numeroIDE;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public String getBusinessId() {
		return businessId;
	}

	@Nullable
	public String getReferenceId() {
		return referenceId;
	}
}
