package ch.vd.uniregctb.interfaces.model.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AssujettissementPM;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.interfaces.model.EtatPM;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.RegimeFiscal;
import ch.vd.uniregctb.interfaces.model.Siege;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.type.TypeAdressePM;

public class MockPersonneMorale implements PersonneMorale {

	private RegDate debut;
	private RegDate fin;
	private String nomContact;
	private long numeroEntreprise;
	private String raisonSociale;
	private String telecopieContact;
	private String telephoneContact;
	private String raisonSociale1;
	private String raisonSociale2;
	private String raisonSociale3;
	private String titulaireCompte;
	private String designationAbregee;
	private RegDate dateBouclementFuture;
	private String numeroIPMRO;
	private Collection<AdresseEntreprise> adresses = new ArrayList<AdresseEntreprise>();
	private List<FormeJuridique> formesJuridiques = new ArrayList<FormeJuridique>();
	private List<CompteBancaire> comptesBancaires = new ArrayList<CompteBancaire>();
	private List<Capital> capitaux = new ArrayList<Capital>();
	private List<EtatPM> etats = new ArrayList<EtatPM>();
	private List<RegimeFiscal> regimesVD = new ArrayList<RegimeFiscal>();
	private List<RegimeFiscal> regimesCH = new ArrayList<RegimeFiscal>();
	private List<Siege> sieges = new ArrayList<Siege>();
	private List<AssujettissementPM> assujettissementsLIC = new ArrayList<AssujettissementPM>();
	private List<AssujettissementPM> assujettissementsLIFD = new ArrayList<AssujettissementPM>();
	private List<ForPM> forsFiscauxPrincipaux = new ArrayList<ForPM>();
	private List<ForPM> forsFiscauxSecondaires = new ArrayList<ForPM>();

	public static final MockPersonneMorale NestleSuisse = new MockPersonneMorale(27769, "Nestlé Suisse S.A.", "S.A.", "Myriam Steiner", RegDate.get(1996, 12, 18), null);
	public static final MockPersonneMorale BCV = new MockPersonneMorale(20222, "Banque Cantonale Vaudoise", "S.A.", "Daniel Kuffer", RegDate.get(1901, 1, 1), null);
	public static final MockPersonneMorale KPMG = new MockPersonneMorale(2058, "KPMG SA", "S.A.", null, RegDate.get(1901, 1, 1), null);
	public static final MockPersonneMorale CuriaTreuhand = new MockPersonneMorale(21038, "Curia Treuhand AG", "S.A.", null, RegDate.get(1901, 1, 1), null);
	public static final MockPersonneMorale JalHolding = new MockPersonneMorale(1314, "JAL HOLDING", "Jal holding S.A.", null, "en liquidation", "S.A.", "R. Borgo", RegDate.get(1975, 12, 24), null);
	public static final MockPersonneMorale EvianRussie = new MockPersonneMorale(7777, "Evian Water Russia", "Distributor (Evian Water)", "LLC PepsiCo Holdings", "Free Economic Zone Sherrizone", "S.A.", "Dimitri Vokda", RegDate.get(1966, 11, 10), null);
	public static final MockPersonneMorale BanqueCoopBale = new MockPersonneMorale(15489, "BANQUE COOP", "Bank Coop AG (Banque Coop SA)", "(Banca Coop SA)", "(Bank Coop Ltd)", "S.A.", "H. Roller", null, null);
	public static final MockPersonneMorale KhatAnstalt = new MockPersonneMorale(13433, "KHAT ANSTALT", "Khat Anstalt", null, null, "S.A.", null, null, null);

	static {
		{
			MockAdresseEntreprise siege = new MockAdresseEntreprise();
			siege.setComplement(null);
			siege.setLocalite("1800 Vevey");
			siege.setRue("Entre-Deux-Villes");
			siege.setNumeroMaison(null);
			siege.setType(TypeAdressePM.SIEGE);
			siege.setDateDebutValidite(RegDate.get(1996, 12, 5));
			siege.setDateFinValidite(null);
			NestleSuisse.addAdresse(siege);

			MockAdresseEntreprise courrier = new MockAdresseEntreprise();
			courrier.setComplement("Finance et Audit");
			courrier.setLocalite("1800 Vevey");
			courrier.setRue("pa Myriam Steiner / CP 352");
			courrier.setNumeroMaison(null);
			courrier.setType(TypeAdressePM.COURRIER);
			courrier.setDateDebutValidite(RegDate.get(2003, 6, 13));
			courrier.setDateFinValidite(null);
			NestleSuisse.addAdresse(courrier);

			MockAdresseEntreprise facturation = new MockAdresseEntreprise();
			facturation.setComplement("Finance et Audit");
			facturation.setLocalite("1800 Vevey");
			facturation.setRue("pa Myriam Steiner / CP 352");
			facturation.setNumeroMaison(null);
			facturation.setType(TypeAdressePM.FACTURATION);
			facturation.setDateDebutValidite(RegDate.get(2003, 6, 13));
			facturation.setDateFinValidite(null);
			NestleSuisse.addAdresse(facturation);
		}

		{
			BCV.setDesignationAbregee("BCV");

			final MockForPM ffp = new MockForPM();
//			ffp.setDateDebut(RegDate.get(1883, 1, 6));
			ffp.setDateDebut(null); // TODO (msi) remettre la date correcte quand on autorisera les dates < 1900
			ffp.setDateFin(null);
			ffp.setNoOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			ffp.setTypeAutoriteFiscale(TypeNoOfs.COMMUNE_CH);
			BCV.addForsFiscalPrincipal(ffp);

			MockAdresseEntreprise siege = new MockAdresseEntreprise();
			siege.setComplement(null);
			siege.setLocalite("1003 Lausanne Secteur de dist.");
			siege.setRue("Saint-François, place");
			siege.setNumeroMaison("14");
			siege.setType(TypeAdressePM.SIEGE);
			siege.setDateDebutValidite(null);
			siege.setDateFinValidite(null);
			siege.setNumeroTechniqueRue(30370);
			siege.setNumeroOrdrePostal(150);
			BCV.addAdresse(siege);

			MockAdresseEntreprise courrier = new MockAdresseEntreprise();
			courrier.setComplement("pa Comptabilité financière");
			courrier.setLocalite("1003 Lausanne Secteur de dist.");
			courrier.setRue("Saint-François, place");
			courrier.setNumeroMaison("14");
			courrier.setType(TypeAdressePM.COURRIER);
			courrier.setDateDebutValidite(RegDate.get(2008, 1, 1));
			courrier.setDateFinValidite(null);
			courrier.setNumeroTechniqueRue(30370);
			courrier.setNumeroOrdrePostal(150);
			BCV.addAdresse(courrier);

			MockAdresseEntreprise facturation = new MockAdresseEntreprise();
			facturation.setComplement("pa Comptabilité financière");
			facturation.setLocalite("1003 Lausanne Secteur de dist.");
			facturation.setRue("Pl. St-François 14 / C.P. 300");
			facturation.setNumeroMaison(null);
			facturation.setType(TypeAdressePM.FACTURATION);
			facturation.setDateDebutValidite(RegDate.get(2006, 12, 15));
			facturation.setDateFinValidite(null);
			courrier.setNumeroOrdrePostal(150);
			BCV.addAdresse(facturation);
		}
		{
			MockAdresseEntreprise siege = new MockAdresseEntreprise();
			siege.setRue("Badenerstrasse 172");
			siege.setNumeroMaison("14");
			siege.setNumeroPostal("8004");
			siege.setLocalite("Zürich");
			siege.setType(TypeAdressePM.SIEGE);
			siege.setDateDebutValidite(null);
			siege.setDateFinValidite(null);
			siege.setNumeroTechniqueRue(64718);
			siege.setNumeroOrdrePostal(4388);
			KPMG.addAdresse(siege);

			MockAdresseEntreprise courrier = new MockAdresseEntreprise();
			courrier.setRue("Avenue de Rumine");
			courrier.setNumeroMaison("37");
			courrier.setNumeroPostal("1005");
			courrier.setLocalite("Lausanne");
			courrier.setType(TypeAdressePM.COURRIER);
			courrier.setDateDebutValidite(null);
			courrier.setDateFinValidite(null);
			courrier.setNumeroTechniqueRue(30525);
			courrier.setNumeroOrdrePostal(152);
			KPMG.addAdresse(courrier);
		}

		{
			MockAdresseEntreprise siege = new MockAdresseEntreprise();
			siege.setRue("Grabenstrasse");
			siege.setNumeroMaison("15");
			siege.setNumeroPostal("7000");
			siege.setLocalite("Chur");
			siege.setType(TypeAdressePM.SIEGE);
			siege.setDateDebutValidite(null);
			siege.setDateFinValidite(null);
			siege.setNumeroTechniqueRue(39838);
			siege.setNumeroOrdrePostal(3970);
			CuriaTreuhand.addAdresse(siege);
		}

		{
			MockAdresseEntreprise courrier = new MockAdresseEntreprise();
			courrier.setComplement("pa Fidu. Commerce & Industrie ");
			courrier.setRue("Avenue de la Gare");
			courrier.setNumeroMaison("10");
			courrier.setNumeroTechniqueRue(30317);
			courrier.setNumeroPostal("1003");
			courrier.setLocalite("Lausanne");
			courrier.setType(TypeAdressePM.COURRIER);
			courrier.setDateDebutValidite(RegDate.get(2007, 6, 11));
			courrier.setDateFinValidite(null);
			JalHolding.addAdresse(courrier);

			MockAdresseEntreprise facturation = new MockAdresseEntreprise();
			facturation.setComplement("pa Fidu. Commerce & Industrie ");
			facturation.setRue("Avenue de la Gare");
			facturation.setNumeroMaison("10");
			facturation.setNumeroTechniqueRue(30317);
			facturation.setNumeroPostal("1003");
			facturation.setLocalite("Lausanne");
			facturation.setType(TypeAdressePM.FACTURATION);
			facturation.setDateDebutValidite(RegDate.get(2007, 6, 11));
			facturation.setDateFinValidite(null);
			JalHolding.addAdresse(facturation);
			
			MockAdresseEntreprise siege = new MockAdresseEntreprise();
			siege.setComplement("Fid.Commerce & Industrie S.A. ");
			siege.setRue("Chemin Messidor");
			siege.setNumeroMaison("5");
			siege.setNumeroTechniqueRue(30593);
			siege.setNumeroPostal("1006");
			siege.setLocalite("Lausanne");
			siege.setType(TypeAdressePM.SIEGE);
			siege.setDateDebutValidite(RegDate.get(1997, 5, 14));
			siege.setDateFinValidite(null);
			JalHolding.addAdresse(siege);
		}

		{
			MockAdresseEntreprise courrier = new MockAdresseEntreprise();
			courrier.setComplement("p.a. Aleksey Fyodorovich Karamazov");
			courrier.setRue("Solnechnogorsk Dist.");
			courrier.setNumeroPostal("141580");
			courrier.setLocalite("Moscow region");
			courrier.setPays(MockPays.Russie);
			courrier.setType(TypeAdressePM.COURRIER);
			courrier.setDateDebutValidite(RegDate.get(1966, 11, 10));
			courrier.setDateFinValidite(null);
			EvianRussie.addAdresse(courrier);
		}

		{
			final MockForPM ffp = new MockForPM();
			ffp.setDateDebut(RegDate.get(1971, 3,23));
			ffp.setDateFin(null);
			ffp.setNoOfsAutoriteFiscale(MockCommune.Bale.getNoOFS());
			ffp.setTypeAutoriteFiscale(TypeNoOfs.COMMUNE_CH);

			BanqueCoopBale.addForsFiscalPrincipal(ffp);
		}

		{
			final MockForPM ffp = new MockForPM();
			ffp.setDateDebut(RegDate.get(1992, 1, 1));
			ffp.setDateFin(null);
			ffp.setNoOfsAutoriteFiscale(MockPays.Liechtenstein.getNoOFS());
			ffp.setTypeAutoriteFiscale(TypeNoOfs.PAYS_HS);

			KhatAnstalt.addForsFiscalPrincipal(ffp);
		}
	}

	public MockPersonneMorale() {
	}

	public MockPersonneMorale(long numeroEntreprise, String raisonSociale, String codeFormeJuridique, String nomContact, RegDate debut, RegDate fin) {
		this.numeroEntreprise = numeroEntreprise;
		this.raisonSociale = raisonSociale;
		this.raisonSociale1 = raisonSociale;
		this.formesJuridiques.add(new MockFormeJuridique(null, null, codeFormeJuridique));
		this.nomContact = nomContact;
		this.debut = debut;
		this.fin = fin;
	}

	public MockPersonneMorale(long numeroEntreprise, String raisonSociale, String raisonSociale1, String raisonSociale2, String raisonSociale3, String codeFormeJuridique, String nomContact,
	                          RegDate debut, RegDate fin) {
		this.numeroEntreprise = numeroEntreprise;
		this.raisonSociale = raisonSociale;
		this.raisonSociale1 = raisonSociale1;
		this.raisonSociale2 = raisonSociale2;
		this.raisonSociale3 = raisonSociale3;
		this.formesJuridiques.add(new MockFormeJuridique(null, null, codeFormeJuridique));
		this.nomContact = nomContact;
		this.debut = debut;
		this.fin = fin;
	}

	public MockPersonneMorale(MockPersonneMorale right, Set<PartPM> parts) {
		this.debut = right.debut;
		this.fin = right.fin;
		this.nomContact = right.nomContact;
		this.numeroEntreprise = right.numeroEntreprise;
		this.raisonSociale = right.raisonSociale;
		this.telecopieContact = right.telecopieContact;
		this.telephoneContact = right.telephoneContact;
		this.raisonSociale1 = right.raisonSociale1;
		this.raisonSociale2 = right.raisonSociale2;
		this.raisonSociale3 = right.raisonSociale3;
		this.titulaireCompte = right.titulaireCompte;
		this.designationAbregee = right.designationAbregee;
		this.dateBouclementFuture = right.dateBouclementFuture;
		this.numeroIPMRO = right.numeroIPMRO;
		this.comptesBancaires = right.comptesBancaires;

		if (parts != null && parts.contains(PartPM.ADRESSES)) {
			this.adresses = right.adresses;
		}
		if (parts != null && parts.contains(PartPM.FORMES_JURIDIQUES)) {
			this.formesJuridiques = right.formesJuridiques;
		}
		if (parts != null && parts.contains(PartPM.CAPITAUX)) {
			this.capitaux = right.capitaux;
		}
		if (parts != null && parts.contains(PartPM.ETATS)) {
			this.etats = right.etats;
		}
		if (parts != null && parts.contains(PartPM.REGIMES_FISCAUX)) {
			this.regimesVD = right.regimesVD;
			this.regimesCH = right.regimesCH;
		}
		if (parts != null && parts.contains(PartPM.SIEGES)) {
			this.sieges = right.sieges;
		}
		if (parts != null && parts.contains(PartPM.ASSUJETTISSEMENTS)) {
			this.assujettissementsLIC = right.assujettissementsLIC;
			this.assujettissementsLIFD = right.assujettissementsLIFD;
		}
		if (parts != null && parts.contains(PartPM.FORS_FISCAUX)) {
			this.forsFiscauxPrincipaux = right.forsFiscauxPrincipaux;
			this.forsFiscauxSecondaires = right.forsFiscauxSecondaires;
		}
	}

	public void addAdresse(AdresseEntreprise adresse) {
		adresses.add(adresse);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getAdresses() {
		return adresses;
	}

	@Override
	public RegDate getDateConstitution() {
		return debut;
	}

	@Override
	public RegDate getDateFinActivite() {
		return fin;
	}

	@Override
	public List<FormeJuridique> getFormesJuridiques() {
		return formesJuridiques;
	}

	@Override
	public String getNomContact() {
		return nomContact;
	}

	@Override
	public long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	@Override
	public String getRaisonSociale() {
		return raisonSociale;
	}

	@Override
	public String getRaisonSociale1() {
		return raisonSociale1;
	}

	@Override
	public String getRaisonSociale2() {
		return raisonSociale2;
	}

	@Override
	public String getRaisonSociale3() {
		return raisonSociale3;
	}

	@Override
	public String getTelecopieContact() {
		return telecopieContact;
	}

	@Override
	public String getDesignationAbregee() {
		return designationAbregee;
	}

	@Override
	public String getTelephoneContact() {
		return telephoneContact;
	}

	@Override
	public String getTitulaireCompte() {
		return titulaireCompte;
	}

	@Override
	public RegDate getDateBouclementFuture() {
		return dateBouclementFuture;
	}

	@Override
	public String getNumeroIPMRO() {
		return numeroIPMRO;
	}

	@Override
	public List<CompteBancaire> getComptesBancaires() {
		return comptesBancaires;
	}

	public RegDate getDebut() {
		return debut;
	}

	public RegDate getFin() {
		return fin;
	}

	public void setTelecopieContact(String telecopieContact) {
		this.telecopieContact = telecopieContact;
	}

	public void setTelephoneContact(String telephoneContact) {
		this.telephoneContact = telephoneContact;
	}

	public void setTitulaireCompte(String titulaireCompte) {
		this.titulaireCompte = titulaireCompte;
	}

	public void setDebut(RegDate debut) {
		this.debut = debut;
	}

	public void setFin(RegDate fin) {
		this.fin = fin;
	}

	public void setDateConstitution(RegDate debut) {
		this.debut = debut;
	}

	public void setDateFinActivite(RegDate fin) {
		this.fin = fin;
	}

	public void setNomContact(String nomContact) {
		this.nomContact = nomContact;
	}

	public void setNumeroEntreprise(long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public void setAdresses(Collection<AdresseEntreprise> adresses) {
		this.adresses = adresses;
	}

	public void setFormesJuridiques(List<FormeJuridique> formesJuridiques) {
		this.formesJuridiques = formesJuridiques;
	}

	public void setComptesBancaires(List<CompteBancaire> comptesBancaires) {
		this.comptesBancaires = comptesBancaires;
	}

	public void setRaisonSociale1(String raisonSociale1) {
		this.raisonSociale1 = raisonSociale1;
	}

	public void setRaisonSociale2(String raisonSociale2) {
		this.raisonSociale2 = raisonSociale2;
	}

	public void setRaisonSociale3(String raisonSociale3) {
		this.raisonSociale3 = raisonSociale3;
	}

	public void setDesignationAbregee(String designationAbregee) {
		this.designationAbregee = designationAbregee;
	}

	public void setDateBouclementFuture(RegDate dateBouclementFuture) {
		this.dateBouclementFuture = dateBouclementFuture;
	}

	public void setNumeroIPMRO(String numeroIPMRO) {
		this.numeroIPMRO = numeroIPMRO;
	}

	@Override
	public List<Capital> getCapitaux() {
		return capitaux;
	}

	@Override
	public List<EtatPM> getEtats() {
		return etats;
	}

	@Override
	public List<RegimeFiscal> getRegimesVD() {
		return regimesVD;
	}

	@Override
	public List<RegimeFiscal> getRegimesCH() {
		return regimesCH;
	}

	@Override
	public List<Siege> getSieges() {
		return sieges;
	}

	@Override
	public List<AssujettissementPM> getAssujettissementsLIC() {
		return assujettissementsLIC;
	}

	@Override
	public List<AssujettissementPM> getAssujettissementsLIFD() {
		return assujettissementsLIFD;
	}

	@Override
	public List<ForPM> getForsFiscauxPrincipaux() {
		return forsFiscauxPrincipaux;
	}

	@Override
	public List<ForPM> getForsFiscauxSecondaires() {
		return forsFiscauxSecondaires;
	}

	public void setForsFiscauxPrincipaux(List<ForPM> forsFiscauxPrincipaux) {
		this.forsFiscauxPrincipaux = forsFiscauxPrincipaux;
	}

	public void addForsFiscalPrincipal(ForPM ffp) {
		if (this.forsFiscauxPrincipaux == null) {
			this.forsFiscauxPrincipaux = new ArrayList<ForPM>();
		}
		this.forsFiscauxPrincipaux.add(ffp);
	}

	public void setForsFiscauxSecondaires(List<ForPM> forsFiscauxSecondaires) {
		this.forsFiscauxSecondaires = forsFiscauxSecondaires;
	}

	@Override
	public List<Mandat> getMandats() {
		return null;
	}

	@Override
	public void copyPartsFrom(PersonneMorale pm, Set<PartPM> parts) {
		if (parts != null && parts.contains(PartPM.ADRESSES)) {
			adresses = pm.getAdresses();
		}
		if (parts != null && parts.contains(PartPM.FORMES_JURIDIQUES)) {
			formesJuridiques = pm.getFormesJuridiques();
		}
		if (parts != null && parts.contains(PartPM.CAPITAUX)) {
			capitaux = pm.getCapitaux();
		}
		if (parts != null && parts.contains(PartPM.ETATS)) {
			etats = pm.getEtats();
		}
		if (parts != null && parts.contains(PartPM.REGIMES_FISCAUX)) {
			regimesVD = pm.getRegimesVD();
			regimesCH = pm.getRegimesCH();
		}
		if (parts != null && parts.contains(PartPM.SIEGES)) {
			sieges = pm.getSieges();
		}
		if (parts != null && parts.contains(PartPM.ASSUJETTISSEMENTS)) {
			assujettissementsLIC = pm.getAssujettissementsLIC();
			assujettissementsLIFD = pm.getAssujettissementsLIFD();
		}
		if (parts != null && parts.contains(PartPM.FORS_FISCAUX)) {
			forsFiscauxPrincipaux = pm.getForsFiscauxPrincipaux();
			forsFiscauxSecondaires = pm.getForsFiscauxSecondaires();
		}
	}

	@Override
	public PersonneMorale clone(Set<PartPM> parts) {
		return new MockPersonneMorale(this, parts);
	}
}
