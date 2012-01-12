package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface Etablissement {

	long getNoEtablissement();

	RegDate getDateDebut();

	RegDate getDateFin();

	String getRaisonSociale1();

	String getRaisonSociale2();

	String getRaisonSociale3();

	String getEnseigne();

	String getChez();

	String getRue();

	String getNoTelephone();

	String getNoFax();

	String getNoPolice();

	String getCCP();

	String getCompteBancaire();

	String getIBAN();

	String getBicSwift();

	String getNomInstitutionFinanciere();

	Long getNoOrdreLocalitePostale();

	Long getNoRue();
}
