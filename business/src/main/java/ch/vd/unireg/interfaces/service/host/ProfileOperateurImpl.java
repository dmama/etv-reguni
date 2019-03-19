package ch.vd.unireg.interfaces.service.host;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.securite.model.rest.ProfilOperateur;
import ch.vd.unireg.security.IfoSecProcedure;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.wsclient.iam.IamUser;

public class ProfileOperateurImpl implements ProfileOperateur, Serializable {

	private static final long serialVersionUID = -5988187763961971993L;

	private String imprimante;
	private String nom;
	private String noTelephone;
	private String prenom;
	private List<IfoSecProcedure> procedures;
	private String titre;
	private String visaOperateur;

	public ProfileOperateurImpl() {
	}

	public ProfileOperateurImpl(ProfilOperateur profile) {
		this.imprimante = profile.getImprimante();
		this.nom = profile.getNom();
		this.noTelephone = profile.getNoTelephone();
		this.prenom = profile.getPrenom();
		this.procedures = initProcedures(profile.getProcedures());
		this.titre = profile.getTitre();
		this.visaOperateur = profile.getVisaOperateur();
	}

	private List<IfoSecProcedure> initProcedures(ProfilOperateur.Procedures procedures) {
		if (procedures == null) {
			return null;
		}
		final List<IfoSecProcedure> list = new ArrayList<>();
		for (ch.vd.securite.model.rest.Procedure p : procedures.getProcedure()) {
			list.add(IfoSecProcedureImpl.get(p));
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

	public static ProfileOperateur get(ProfilOperateur profile) {
		if (profile == null) {
			return null;
		}

		return new ProfileOperateurImpl(profile);
	}

	// FIXME (msi) un opérateur n'est pas forcément défini dans IAM
	public static ProfileOperateur get(ch.vd.unireg.wsclient.refsec.model.ProfilOperateur profilOperateurRefSec, @NotNull IamUser iamUser) {
		if (profilOperateurRefSec == null) {
			return null;
		}
		final ProfileOperateurImpl profil = new ProfileOperateurImpl();
		profil.setVisaOperateur(profilOperateurRefSec.getVisa());
		profil.setNom(iamUser.getLastName());
		profil.setPrenom(iamUser.getFirstName());
		profil.setNoTelephone(profilOperateurRefSec.getNumeroTelephone());

		final List<IfoSecProcedure> procedures = profilOperateurRefSec.getProcedures().stream()
				.map(IfoSecProcedureImpl::new)
				.collect(Collectors.toList());
		profil.setProcedures(procedures);
		return profil;
	}

}
