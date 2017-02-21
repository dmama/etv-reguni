package ch.vd.uniregctb.registrefoncier;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class EvenementRFMutationView {

	private final Long id;
	private final EtatEvenementRF etat;
	private final TypeEntiteRF typeEntite;
	private final TypeMutationRF typeMutation;
	private final String idRF;
	@Nullable
	private final Long entityId;
	private final String xmlContent;
	@Nullable
	private final String errorMessage;
	@Nullable
	private final String callstack;

	public EvenementRFMutationView(@NotNull EvenementRFMutation right, @NotNull ImmeubleRFDAO immeubleRFDAO) {
		this.id = right.getId();
		this.etat = right.getEtat();
		this.typeEntite = right.getTypeEntite();
		this.typeMutation = right.getTypeMutation();
		this.idRF = right.getIdRF();
		if (this.typeEntite == TypeEntiteRF.IMMEUBLE) {
			this.entityId = Optional.of(immeubleRFDAO)
					.map(d -> d.find(new ImmeubleRFKey(this.idRF)))
					.map(ImmeubleRF::getId)
					.orElse(null);
		}
		else {
			this.entityId = null;
		}
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

	@Nullable
	public Long getEntityId() {
		return entityId;
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
