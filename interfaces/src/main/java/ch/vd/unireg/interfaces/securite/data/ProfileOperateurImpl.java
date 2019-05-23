package ch.vd.unireg.interfaces.securite.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.securite.model.rest.ProfilOperateur;
import ch.vd.unireg.security.ProcedureSecurite;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.wsclient.refsec.model.User;

public class ProfileOperateurImpl implements ProfileOperateur, Serializable {

	private static final long serialVersionUID = -5017557393182920869L;

	@NotNull
	private final String visaOperateur;
	@NotNull
	private final List<ProcedureSecurite> procedures;
	@Nullable
	private final String imprimante;
	@Nullable
	private final String nom;
	@Nullable
	private final String noTelephone;
	@Nullable
	private final String prenom;
	@Nullable
	private final String titre;

	public ProfileOperateurImpl(@NotNull ProfilOperateur profile) {
		this.visaOperateur = profile.getVisaOperateur();
		this.procedures = initProcedures(profile.getProcedures());
		this.titre = profile.getTitre();
		this.prenom = profile.getPrenom();
		this.nom = profile.getNom();
		this.noTelephone = profile.getNoTelephone();
		this.imprimante = profile.getImprimante();
	}

	public ProfileOperateurImpl(@NotNull ch.vd.unireg.wsclient.refsec.model.ProfilOperateur profilOperateur, @Nullable User user) {
		this.visaOperateur = profilOperateur.getVisa();
		this.procedures = Collections.unmodifiableList(profilOperateur.getProcedures().stream()
				                                               .map(ProcedureSecuriteImpl::new)
				                                               .sorted(Comparator.comparing(ProcedureSecuriteImpl::getCode)) // SIFISC-30775, on trie les procédures pour faciliter la lisibilité
				                                               .collect(Collectors.toList()));
		this.titre = null;
		if (user == null) {
			this.nom = null;
			this.prenom = null;
		}
		else {
			this.nom = user.getLastName();
			this.prenom = user.getFirstName();
		}
		this.noTelephone = profilOperateur.getNumeroTelephone();
		this.imprimante = null;
	}

	public ProfileOperateurImpl(@NotNull String visa, @NotNull List<ProcedureSecurite> listProcedure) {
		this.visaOperateur = visa;
		this.procedures = Collections.unmodifiableList(listProcedure);
		this.titre = null;
		this.nom = null;
		this.prenom = null;
		this.noTelephone = null;
		this.imprimante = null;
	}

	@NotNull
	private List<ProcedureSecurite> initProcedures(ProfilOperateur.Procedures procedures) {
		if (procedures == null) {
			return Collections.emptyList();
		}
		final List<ProcedureSecurite> list = new ArrayList<>();
		for (ch.vd.securite.model.rest.Procedure p : procedures.getProcedure()) {
			list.add(ProcedureSecuriteImpl.get(p));
		}
		return Collections.unmodifiableList(list);
	}

	@Nullable
	@Override
	public String getImprimante() {
		return imprimante;
	}

	@Nullable
	@Override
	public String getNom() {
		return nom;
	}

	@Nullable
	@Override
	public String getNoTelephone() {
		return noTelephone;
	}

	@Nullable
	@Override
	public String getPrenom() {
		return prenom;
	}

	@NotNull
	@Override
	public List<ProcedureSecurite> getProcedures() {
		return procedures;
	}

	@Nullable
	@Override
	public String getTitre() {
		return titre;
	}

	@NotNull
	@Override
	public String getVisaOperateur() {
		return visaOperateur;
	}

	@Nullable
	public static ProfileOperateur get(@Nullable ProfilOperateur profile) {
		if (profile == null) {
			return null;
		}

		return new ProfileOperateurImpl(profile);
	}

	@Nullable
	public static ProfileOperateur get(@Nullable ch.vd.unireg.wsclient.refsec.model.ProfilOperateur profilOperateur, @Nullable User user) {
		if (profilOperateur == null) {
			return null;
		}
		return new ProfileOperateurImpl(profilOperateur, user);
	}

}
