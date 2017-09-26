package ch.vd.uniregctb.registrefoncier.importrf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;

public class EvenementRFImportView {

	private final Long id;

	/**
	 * Le type d'import concerné.
	 */
	private final TypeImportRF type;

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

	/**
	 * La callstack complète en cas d'erreur de traitement.
	 */
	@Nullable
	private final String callstack;

	public EvenementRFImportView(@NotNull EvenementRFImport right) {
		this.id = right.getId();
		this.type = right.getType();
		this.etat = right.getEtat();
		this.dateEvenement = right.getDateEvenement();
		this.fileUrl = right.getFileUrl();
		this.errorMessage = right.getErrorMessage();
		this.callstack = right.getCallstack();
	}

	public Long getId() {
		return id;
	}

	public TypeImportRF getType() {
		return type;
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

	@Nullable
	public String getCallstack() {
		return callstack;
	}
}
