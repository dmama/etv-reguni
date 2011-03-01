package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockEvenementCivil implements EvenementCivil {

	// L'individu principal.
	private Individu individu;
	private Long principalPPId;

	// Le conjoint (mariage ou pacs).
	private Individu conjoint;
	private Long conjointPPId;

	private TypeEvenementCivil type;
	private RegDate date;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;

	private EvenementCivilHandler handler;

	public MockEvenementCivil() {
	}

	public MockEvenementCivil(Individu individu, Individu conjoint, TypeEvenementCivil type, RegDate date, Integer numeroOfsCommuneAnnonce) {
		this.individu = individu;
		this.conjoint = conjoint;
		this.type = type;
		this.date = date;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public MockEvenementCivil(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, TypeEvenementCivil type, RegDate date,
	                          Integer numeroOfsCommuneAnnonce) {
		this.individu = individu;
		this.principalPPId = principalPPId;
		this.conjoint = conjoint;
		this.conjointPPId = conjointPPId;
		this.type = type;
		this.date = date;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public void init(TiersDAO tiersDAO) {
		if (individu != null) {
			principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), false);
		}
		if (conjoint != null) {
			conjointPPId = tiersDAO.getNumeroPPByNumeroIndividu(conjoint.getNoTechnique(), false);
		}
	}

	public void setHandler(EvenementCivilHandler handler) {
		this.handler = handler;
	}

	public Long getNoIndividu() {
		return individu == null ? null : individu.getNoTechnique();
	}

	public Individu getIndividu() {
		return individu;
	}

	public Long getPrincipalPPId() {
		return principalPPId;
	}

	public Long getNoIndividuConjoint() {
		return conjoint == null ? null : conjoint.getNoTechnique();
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public Long getConjointPPId() {
		return conjointPPId;
	}

	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	public TypeEvenementCivil getType() {
		return type;
	}

	public RegDate getDate() {
		return date;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
