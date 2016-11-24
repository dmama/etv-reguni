package ch.vd.uniregctb.registrefoncier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;

public class EvenementRFMutationView {

	private final Long id;
	private final EtatEvenementRF etat;
	private final TypeEntiteRF typeEntite;
	private final TypeMutationRF typeMutation;
	private final String idRF;
	private final String xmlContent;
	@Nullable
	private final String errorMessage;
	@Nullable
	private final String callstack;

	public EvenementRFMutationView(@NotNull EvenementRFMutation right) {
		this.id = right.getId();
		this.etat = right.getEtat();
		this.typeEntite = right.getTypeEntite();
		this.typeMutation = right.getTypeMutation();
		this.idRF = right.getIdRF();
		this.xmlContent = right.getXmlContent();
		this.errorMessage = right.getErrorMessage();
		this.callstack = right.getCallstack();
	}

	public Long getId() {
		return id;
	}

	public EtatEvenementRF getEtat() {
		return etat;
	}

	public TypeEntiteRF getTypeEntite() {
		return typeEntite;
	}

	public TypeMutationRF getTypeMutation() {
		return typeMutation;
	}

	public String getIdRF() {
		return idRF;
	}

	public String getXmlContent() {
		return xmlContent;
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
