package ch.vd.uniregctb.evenement.depart;

import java.util.Collection;
import java.util.Iterator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

public class MockDepart implements Depart, Cloneable {

	private Adresse ancienneAdressePrincipale;
	private Commune nouvelleCommunePrincipale;
	private Commune ancienneCommunePrincipale;
	private Adresse ancienneAdresseCourrier;
	private Adresse adresseCourrier;
	private Adresse adressePrincipale;
	private Adresse ancienneAdresseSecondaire;
	private Commune ancienneCommuneSecondaire;

	private MockIndividu individu;
	private MockIndividu conjoint;
	private RegDate date;
	private String etat;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;
	private TypeEvenementCivil type;
	private final boolean isAncienTypeDepart = false;

	public void setAncienneAdresseCourrier(Adresse adresseCourrier) {
		this.ancienneAdresseCourrier = adresseCourrier;
	}

	public void setAncienneAdressePrincipale(Adresse adressePrincipale) {
		this.ancienneAdressePrincipale = adressePrincipale;
	}

	public void setIndividu(MockIndividu individu) {
		this.individu = individu;
	}

	public void setConjoint(MockIndividu conjoint) {
		this.conjoint = conjoint;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}

	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public Adresse getNouvelleAdresseSecondaire() {
		return getAdresseSecondaire();
	}

	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	public Adresse getAncienneAdresseCourrier() {
		return ancienneAdresseCourrier;
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public RegDate getDate() {
		return date;
	}

	public String getEtat() {
		return etat;
	}

	public Individu getIndividu() {
		return individu;
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

	public boolean isContribuablePresentBefore() {
		return false; // par définition
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public void setNouvelleCommunePrincipale(Commune nouvelleCommunePrincipale) {
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
	}

	public Commune getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public void setAncienneCommunePrincipale(Commune communePrincipale) {
		this.ancienneCommunePrincipale = communePrincipale;
	}

	public static boolean findEvenementFermetureFor(Collection<EvenementFiscal> lesEvenements,Depart depart) {

		boolean isPresent = false;
		Iterator<EvenementFiscal> iteEvFiscal = lesEvenements.iterator();
		EvenementFiscal evenement = null;
		while (iteEvFiscal.hasNext()) {
			evenement = iteEvFiscal.next();
			if (evenement.getType().equals(TypeEvenementFiscal.FERMETURE_FOR)&& evenement.getDateEvenement()==depart.getDate()) {
				isPresent = true;
				break;
			}
		}
		return isPresent;

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


	public void setAncienneAdresseSecondaire(Adresse adresseSecondaire) {
		this.ancienneAdresseSecondaire = adresseSecondaire;
	}

	public Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public Commune getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}


	public Adresse getAdresseSecondaire() {
		return null;
	}

	public void setAncienneCommuneSecondaire(Commune ancienneCommuneSecondaire) {
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
	}

	public Pays getPaysInconnu() {

		return MockPays.PaysInconnu;
	}

	public boolean isAncienTypeDepart() {
		// TODO Auto-generated method stub
		return isAncienTypeDepart;
	}
}
