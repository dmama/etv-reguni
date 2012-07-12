package ch.vd.uniregctb.interfaces.service.host;

import java.util.ArrayList;
import java.util.List;

import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.security.IfoSecProcedure;
import ch.vd.uniregctb.security.IfoSecProfil;

public class IfoSecProfilImpl implements IfoSecProfil {

	private String imprimante;
	private String nom;
	private String noTelephone;
	private String prenom;
	private List<IfoSecProcedure> procedures;
	private String titre;
	private String visaOperateur;

	public IfoSecProfilImpl() {
	}

	public IfoSecProfilImpl(String imprimante, String nom, String noTelephone, String prenom, List<IfoSecProcedure> procedures, String titre, String visaOperateur) {
		this.imprimante = imprimante;
		this.nom = nom;
		this.noTelephone = noTelephone;
		this.prenom = prenom;
		this.procedures = procedures;
		this.titre = titre;
		this.visaOperateur = visaOperateur;
	}

	public IfoSecProfilImpl(ProfilOperateur profile) {
		this.imprimante = profile.getImprimante();
		this.nom = profile.getNom();
		this.noTelephone = profile.getNoTelephone();
		this.prenom = profile.getPrenom();
		this.procedures = initProcedures(profile.getProcedures());
		this.titre = profile.getTitre();
		this.visaOperateur = profile.getVisaOperateur();
	}

	private static List<IfoSecProcedure> initProcedures(List<?> procedures) {
		if (procedures == null) {
			return null;
		}
		final List<IfoSecProcedure> list = new ArrayList<IfoSecProcedure>();
		for (Object p : procedures) {
			list.add(IfoSecProcedureImpl.get((Procedure) p));
		}
		return list;
	}

	@Override
	public String getImprimante() {
		return imprimante;
	}

	public void setImprimante(String imprimante) {
		this.imprimante = imprimante;
	}

	@Override
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Override
	public String getNoTelephone() {
		return noTelephone;
	}

	public void setNoTelephone(String noTelephone) {
		this.noTelephone = noTelephone;
	}

	@Override
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Override
	public List<IfoSecProcedure> getProcedures() {
		return procedures;
	}

	public void setProcedures(List<IfoSecProcedure> procedures) {
		this.procedures = procedures;
	}

	@Override
	public String getTitre() {
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	@Override
	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = visaOperateur;
	}

	public static IfoSecProfil get(ProfilOperateur profile) {
		if (profile == null) {
		return null;
		}

		return new IfoSecProfilImpl(profile);
	}
}
