package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockEvenementCivil implements EvenementCivil {

	private Adresse adresseCourrier;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;
	private Individu individu;
	private Long principalPPId;
	private Individu conjoint;
	private Long conjointPPId;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;
	private TypeEvenementCivil type;
	private RegDate date;

	private EvenementCivilHandler handler;

	public void init(TiersDAO tiersDAO) {
		if (individu != null) {
			principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), false);
		}
		if (conjoint != null) {
			conjointPPId = tiersDAO.getNumeroPPByNumeroIndividu(conjoint.getNoTechnique(), false);
		}
	}

	public EvenementCivilHandler getHandler() {
		return handler;
	}

	public void setHandler(EvenementCivilHandler handler) {
		this.handler = handler;
	}

	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	public void setAdresseCourrier(Adresse adresseCourrier) {
		this.adresseCourrier = adresseCourrier;
	}

	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	public void setAdressePrincipale(Adresse adressePrincipale) {
		this.adressePrincipale = adressePrincipale;
	}

	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	public void setAdresseSecondaire(Adresse adresseSecondaire) {
		this.adresseSecondaire = adresseSecondaire;
	}

	public Long getNoIndividu() {
		return individu == null ? null : individu.getNoTechnique();
	}

	public Individu getIndividu() {
		return individu;
	}

	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	public Long getPrincipalPPId() {
		return principalPPId;
	}

	public void setPrincipalPPId(Long principalPPId) {
		this.principalPPId = principalPPId;
	}

	public Long getNoIndividuConjoint() {
		return conjoint == null ? null : conjoint.getNoTechnique();
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
	}

	public Long getConjointPPId() {
		return conjointPPId;
	}

	public void setConjointPPId(Long conjointPPId) {
		this.conjointPPId = conjointPPId;
	}

	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}

	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public TypeEvenementCivil getType() {
		return type;
	}

	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

	public RegDate getDate() {
		return date;
	}

	public void setDate(RegDate date) {
		this.date = date;
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
