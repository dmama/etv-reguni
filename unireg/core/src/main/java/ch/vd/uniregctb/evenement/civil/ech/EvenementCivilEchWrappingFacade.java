package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchWrappingFacade implements EvenementCivilEchFacade {

	private final EvenementCivilEchFacade target;

	public EvenementCivilEchWrappingFacade(EvenementCivilEchFacade target) {
		this.target = target;
	}

	public EvenementCivilEchFacade getTarget() {
		return target;
	}

	@Override
	public String getLogCreationUser() {
		return target.getLogCreationUser();
	}

	@Override
	public Long getId() {
		return target.getId();
	}

	@Override
	public Long getRefMessageId() {
		return target.getRefMessageId();
	}

	@Override
	public void setRefMessageId(Long refMessageId) {
		target.setRefMessageId(refMessageId);
	}

	@Override
	public TypeEvenementCivilEch getType() {
		return target.getType();
	}

	@Override
	public ActionEvenementCivilEch getAction() {
		return target.getAction();
	}

	@Override
	public EtatEvenementCivil getEtat() {
		return target.getEtat();
	}

	@Override
	public RegDate getDateEvenement() {
		return target.getDateEvenement();
	}

	@Override
	public Long getNumeroIndividu() {
		return target.getNumeroIndividu();
	}

	@Override
	public String getCommentaireTraitement() {
		return target.getCommentaireTraitement();
	}

	@Override
	public void setCommentaireTraitement(String commentaire) {
		target.setCommentaireTraitement(commentaire);
	}

	@Override
	public Set<EvenementCivilEchErreur> getErreurs() {
		return target.getErreurs();
	}
}
