package ch.vd.uniregctb.registrefoncier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;

public class EvenementRFImportView {

	private final Long id;

	/**
	 * L'état courant de l'événement.
	 */
	private final EtatEvenementRF etat;

	/**
	 * La date de valeur du fichier.
	 */
	private final RegDate dateEvenement;

	/**
	 * L'URL Raft du fichier qui contient les données à traiter.
	 */
	private final String fileUrl;

	/**
	 * Un message d'erreur en cas d'erreur de traitement.
	 */
	@Nullable
	private final String errorMessage;

	public EvenementRFImportView(@NotNull EvenementRFImport right) {
		this.id = right.getId();
		this.etat = right.getEtat();
		this.dateEvenement = right.getDateEvenement();
		this.fileUrl = right.getFileUrl();
		this.errorMessage = right.getErrorMessage();
	}

	public Long getId() {
		return id;
	}

	public EtatEvenementRF getEtat() {
		return etat;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}
}
