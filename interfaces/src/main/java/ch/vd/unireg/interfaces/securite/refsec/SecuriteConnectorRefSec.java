package ch.vd.unireg.interfaces.securite.refsec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.securite.SecuriteConnector;
import ch.vd.unireg.interfaces.securite.SecuriteConnectorException;
import ch.vd.unireg.interfaces.securite.data.OperateurImpl;
import ch.vd.unireg.interfaces.securite.data.ProfileOperateurImpl;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.wsclient.refsec.RefSecClient;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;
import ch.vd.unireg.wsclient.refsec.model.User;

public class SecuriteConnectorRefSec implements SecuriteConnector {

	private RefSecClient client;

	public void setClient(RefSecClient client) {
		this.client = client;
	}

	@Nullable
	@Override
	public Operateur getOperateur(@NotNull String visa) throws SecuriteConnectorException {
		final User user = client.getUser(visa);
		return OperateurImpl.get(user);
	}

	@Nullable
	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws SecuriteConnectorException {
		final User user = client.getUser(visaOperateur);
		final ProfilOperateur profile = client.getProfilOperateur(visaOperateur, codeCollectivite);
		return ProfileOperateurImpl.get(profile, user);
	}

	@NotNull
	@Override
	public List<String> getUtilisateurs(int noCollAdmin) throws SecuriteConnectorException {
		return client.getUsersFromCollectivite(noCollAdmin).stream()
				.map(User::getVisa) // cette méthode retourne bien des utilisateurs mais seul leurs visas sont renseignés...
				.collect(Collectors.toList());
	}

	@NotNull
	@Override
	public Set<Integer> getCollectivitesOperateur(@NotNull String visaOperateur) throws SecuriteConnectorException {
		return client.getCollectivitesOperateur(visaOperateur);
	}

	@Override
	public void ping() throws SecuriteConnectorException {
		client.ping();
	}
}
