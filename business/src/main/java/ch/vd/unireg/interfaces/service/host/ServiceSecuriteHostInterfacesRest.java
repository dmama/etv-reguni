package ch.vd.unireg.interfaces.service.host;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.securite.model.rest.ListeOperateurs;
import ch.vd.securite.model.rest.ProfilOperateur;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeImpl;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.securite.data.OperateurImpl;
import ch.vd.unireg.interfaces.securite.data.ProfileOperateurImpl;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.wsclient.host.interfaces.ServiceSecuriteClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceSecuriteClientException;

public class ServiceSecuriteHostInterfacesRest implements ServiceSecuriteService {

	private ServiceSecuriteClient client;

	private static final Pattern MESSAGE_VISA_OPERATEUR_TERMINE_PATTERN = Pattern.compile("Ce visa op.+rateur est termin.+\\.");


	/**
	 * @param client
	 *            the serviceSecurite to set
	 */
	public void setClient(ServiceSecuriteClient client) {
		this.client = client;
	}


	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException {
		try {
			final ListeCollectiviteAdministrative collectivitesUtilisateurCommunicationTier = client.getCollectivitesUtilisateurCommunicationTier(visaOperateur);
			final List<ch.vd.infrastructure.model.rest.CollectiviteAdministrative> collectiviteAdministrative = collectivitesUtilisateurCommunicationTier.getCollectiviteAdministrative();
			final List<CollectiviteAdministrative> collectivites = new ArrayList<>();
			if (CollectionUtils.isNotEmpty(collectiviteAdministrative)) {

				for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative administrative : collectiviteAdministrative) {
					collectivites.add(CollectiviteAdministrativeImpl.get(administrative));
				}
			}
			return collectivites;
		}
		catch (ServiceSecuriteClientException e) {
			if (isOperateurTermineException(e)) {
				// [hack] L'EJB du service sécurité lève une exception lorsque l'opérateur n'est pas connu, plutôt que de retourner null/vide, on
				// corrige ce comportement ici
				return null;
			}
			throw new ServiceSecuriteException("impossible de récupérer les collectivités de l'utilisateur " + visaOperateur, e);
		}
	}

	private static final String IS_ACTIVE = "O";

	@Nullable
	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException {
		try {
			final ListeCollectiviteAdministrative list = client.getCollectivitesUtilisateurCommunicationTier(visaOperateur);
			if (list == null) {
				return null;
			}
			return list.getCollectiviteAdministrative().stream()
					.filter(c -> IS_ACTIVE.equals(c.getCodeActivite()))
					.findFirst()
					.map(ch.vd.infrastructure.model.rest.CollectiviteAdministrative::getNoColAdm)
					.orElse(null);
		}
		catch (ServiceSecuriteClientException e) {
			if (isOperateurTermineException(e)) {
				// [hack] L'EJB du service sécurité lève une exception lorsque l'opérateur n'est pas connu, plutôt que de retourner null/vide, on
				// corrige ce comportement ici
				return null;
			}
			throw new ServiceSecuriteException("impossible de récupérer les collectivités de l'utilisateur " + visaOperateur, e);
		}
	}

	@Nullable
	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException {
		try {
			final ProfilOperateur profile = client.getProfileUtilisateur(visaOperateur, codeCollectivite);
			return ProfileOperateurImpl.get(profile);
		}

		catch (ServiceSecuriteClientException e) {
			if (isOperateurTermineException(e)) {
				// [hack] L'EJB du service sécurité lève une exception lorsque l'opérateur n'est pas connu, plutôt que de retourner null, on
				// corrige ce comportement ici
				return null;
			}
			throw new ServiceSecuriteException("impossible de récupérer le profil de l'utilisateur " + visaOperateur + " pour l'OID "
					+ codeCollectivite, e);
		}
	}

	/**
	 * Dans le cas où on demande les collectivités d'un opérateur maintenant invalide (= terminé), Host-Interface nous renvoie une exception...
	 *
	 * @return vrai si l'exception spécifiée est levée par host-interface parce que l'opérateur est terminé (= avec une date de fin de validité)
	 */
	private static boolean isOperateurTermineException(ServiceSecuriteClientException e) {
		// [SIFISC-26756] Depuis le passage à CXF 3.1.9, le message retourné par Host-Interface est directement contenu dans le message de l'exception.
		final Matcher matcher = MESSAGE_VISA_OPERATEUR_TERMINE_PATTERN.matcher(e.getMessage());
		return matcher.find();
	}

	/**
	 * Retourne tous les utilisateurs
	 *
	 * @param typesCollectivite
	 * @return la liste des utilisateurs
	 */
	@NotNull
	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		try {
			List<Operateur> res = new ArrayList<>();
			ch.vd.infrastructure.model.rest.TypeCollectivite[] tabTypesCollectivite = new ch.vd.infrastructure.model.rest.TypeCollectivite[typesCollectivite.size()];
			int i=0;

			for (TypeCollectivite typeCollectivite : typesCollectivite) {
				tabTypesCollectivite[i]= new ch.vd.infrastructure.model.rest.TypeCollectivite(typeCollectivite.getCode(),null,null,0);
				i++;
			}
			final ListeOperateurs operateurs = client.getOperateurs(tabTypesCollectivite);
			final List<ch.vd.securite.model.rest.Operateur> listeOperateurs = operateurs.getOperateur();
			if (CollectionUtils.isNotEmpty(listeOperateurs)) {
				for (ch.vd.securite.model.rest.Operateur operateur : listeOperateurs) {
					res.add(OperateurImpl.get(operateur));
				}
			}
			return res;

		}

		catch (ServiceSecuriteClientException e) {
			throw new ServiceSecuriteException("impossible de récupérer la list des utilisateurs pour la fonction ", e);
		}
	}

	@Nullable
	@Override
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
		try {
			// [SIFISC-7231] On ne veut pas se limiter aux opérateurs actuellement valides
			final ch.vd.securite.model.rest.Operateur operateur = client.getOperateurTous(visa);
			return OperateurImpl.get(operateur);
		}
		catch (Exception e) {
			throw new ServiceSecuriteException("impossible de récupérer l'utilisateur correspondant au visa " + visa, e);
		}
	}

	@Override
	public void ping() throws ServiceSecuriteException {
		client.ping();
	}
}
