package ch.vd.uniregctb.interfaces.service.host;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.securite.model.rest.ListeOperateurs;
import ch.vd.securite.model.rest.ProfilOperateur;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeImpl;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.wsclient.host.interfaces.ServiceSecuriteClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceSecuriteClientException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.IfoSecProfil;

public class ServiceSecuriteHostInterfacesRest implements ServiceSecuriteService {

	private ServiceSecuriteClient client;
	private final static  String MESSAGE_VISA_OPERATEUR_TERMINE = "Ce visa op.+rateur est termin.+\\.";


	/**
	 * @param client
	 *            the serviceSecurite to set
	 */
	public void setClient(ServiceSecuriteClient client) {
		this.client = client;
	}


	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrativeUtilisateur> getCollectivitesUtilisateur(String visaOperateur) {
		try {
			final ListeCollectiviteAdministrative collectivitesUtilisateurCommunicationTier = client.getCollectivitesUtilisateurCommunicationTier(visaOperateur);
			final List<ch.vd.infrastructure.model.rest.CollectiviteAdministrative> collectiviteAdministrative = collectivitesUtilisateurCommunicationTier.getCollectiviteAdministrative();
			final List<CollectiviteAdministrativeUtilisateur> collectivites = new ArrayList<>();
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

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		try {
			final ProfilOperateur profile = client.getProfileUtilisateur(visaOperateur, codeCollectivite);
			return IfoSecProfilImpl.get(profile);
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
	 * @return vrai si l'exception spécifiée est levée par host-interface parce que l'opérateur inconnu.
	 */
	private static boolean isOperateurInconnuException(ServiceSecuriteClientException e) {
		final Exception root = getRootException(e);
		if (root instanceof WebApplicationException) {
			//TODO En attendant d'avoir des codes http et des messages host interfaces qui  sont exploitables
			return true;
		}
		return false;
	}

	/**
	 * Dans le cas où on demande les collectivités d'un opérateur maintenant invalide (= terminé), Host-Interface nous renvoie une exception...
	 * @return vrai si l'exception spécifiée est levée par host-interface parce que l'opérateur est terminé (= avec une date de fin de validité)
	 */
	private static boolean isOperateurTermineException(ServiceSecuriteClientException e) {
		final Exception root = getRootException(e);
		if (root instanceof WebApplicationException) {
			final Pattern pattern = Pattern.compile(MESSAGE_VISA_OPERATEUR_TERMINE);
			final Matcher matcher = pattern.matcher(root.getMessage());
			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}
	/**
	 * @return l'exception première à la racine de l'exception spécifiée.
	 */
	private static Exception getRootException(ServiceSecuriteClientException e) {
		Exception root = e;
		Exception current = e;
		while (current != null) {
			root = current;
			current = (Exception) current.getCause();
		}
		return root;
	}

	/**
	 * Retourne tous les utilisateurs
	 *
	 * @param typesCollectivite
	 * @return la liste des utilisateurs
	 */
	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {
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
					res.add(Operateur.get(operateur));
				}
			}
			return res;

		}

		catch (ServiceSecuriteClientException e) {
			throw new ServiceSecuriteException("impossible de récupérer la list des utilisateurs pour la fonction ", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		try {
			// [SIFISC-7231] On ne veut pas se limiter aux opérateurs actuellement valides
			final ch.vd.securite.model.rest.Operateur operateurTous = client.getOperateurTous(individuNoTechnique);
			return Operateur.get(operateurTous);
		}
		catch (Exception e) {
			throw new ServiceSecuriteException("impossible de récupérer l'utilisateur correspondant au numéro d'individu " + individuNoTechnique, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Operateur getOperateur(String visa) {
		try {
			// [SIFISC-7231] On ne veut pas se limiter aux opérateurs actuellement valides
			final ch.vd.securite.model.rest.Operateur operateur = client.getOperateur(visa);
			return Operateur.get(operateur);
		}
		catch (Exception e) {
			throw new ServiceSecuriteException("impossible de récupérer l'utilisateur correspondant au visa " + visa, e);
		}
	}
}
