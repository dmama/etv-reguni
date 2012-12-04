package ch.vd.uniregctb.interfaces.service.host;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.securite.service.SecuriteException;
import ch.vd.securite.service.ServiceSecurite;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.IfoSecProfil;
import ch.vd.utils.proxy.ProxyBuisnessException;

public class ServiceSecuriteHostInterfaces implements ServiceSecuriteService {

	private ServiceSecurite serviceSecurite;

	public ServiceSecuriteHostInterfaces() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	/**
	 * @param serviceSecurite
	 *            the serviceSecurite to set
	 */
	public void setServiceSecurite(ServiceSecurite serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		try {
			return serviceSecurite.getCollectivitesUtilisateurCommunicationTier(visaOperateur);
		}
		catch (RemoteException e) {
			throw new ServiceSecuriteException("impossible de récupérer les collectivités de l'utilisateur " + visaOperateur, e);
		}
		catch (SecuriteException e) {
			if (isOperateurInconnuException(e) || isOperateurTermineException(e)) {
				// [hack] L'EJB du service sécurité lève une exception lorsque l'opérateur n'est pas connu, plutôt que de retourner null/vide, on
				// corrige ce comportement ici
				return Collections.emptyList();
			}
			throw new ServiceSecuriteException("impossible de récupérer les collectivités de l'utilisateur " + visaOperateur, e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
		try {
			return serviceSecurite.getListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
		}
		catch (RemoteException e) {
			throw new ServiceSecuriteException("impossible de récupérer la list des utilisateurs pour la fonction " + codeFonction
					+ " et l'OID " + noCollectivite, e);
		}
		catch (SecuriteException e) {
			throw new ServiceSecuriteException("impossible de récupérer la list des utilisateurs pour la fonction " + codeFonction
					+ " et l'OID " + noCollectivite, e);
		}
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		try {
			final ProfilOperateur profile = serviceSecurite.getProfileUtilisateur(visaOperateur, codeCollectivite);
			return IfoSecProfilImpl.get(profile);
		}
		catch (RemoteException e) {
			throw new ServiceSecuriteException("impossible de récupérer le profil de l'utilisateur " + visaOperateur + " pour l'OID "
					+ codeCollectivite, e);
		}
		catch (SecuriteException e) {
			if (isOperateurInconnuException(e) || isOperateurTermineException(e)) {
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
	private static boolean isOperateurInconnuException(SecuriteException e) {
		final Exception root = getRootException(e);
		if (root instanceof ProxyBuisnessException) {
			final ProxyBuisnessException pbe = (ProxyBuisnessException) root;
			if (pbe.getMessage().endsWith("rateur n'existe pas.")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Dans le cas où on demande les collectivités d'un opérateur maintenant invalide (= terminé), Host-Interface nous renvoie une exception...
	 * @return vrai si l'exception spécifiée est levée par host-interface parce que l'opérateur est terminé (= avec une date de fin de validité)
	 */
	private static boolean isOperateurTermineException(SecuriteException e) {
		final Exception root = getRootException(e);
		if (root instanceof ProxyBuisnessException) {
			final ProxyBuisnessException pbe = (ProxyBuisnessException) root;
			if (pbe.getMessage().endsWith("rateur est terminé.")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return l'exception première à la racine de l'exception spécifiée.
	 */
	private static Exception getRootException(SecuriteException e) {
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
	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {
		try {
			EnumTypeCollectivite[] tabTypesCollectivite = new EnumTypeCollectivite[typesCollectivite.size()];
			Iterator<EnumTypeCollectivite> itTypesCol = typesCollectivite.iterator();
			int i = 0;
			while (itTypesCol.hasNext()) {
				EnumTypeCollectivite typeCol = itTypesCol.next();
				tabTypesCollectivite[i] = typeCol;
				i++;
			}
			Operateur[] operateurs = serviceSecurite.getOperateurs(tabTypesCollectivite);
			List<Operateur> lOperateurs = new ArrayList<Operateur>();
			for (i = 0; i < operateurs.length; i++) {
				lOperateurs.add(operateurs[i]);
			}
			return lOperateurs;

		}
		catch (RemoteException e) {
			throw new ServiceSecuriteException("impossible de récupérer la list des utilisateurs ", e);
		}
		catch (SecuriteException e) {
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
			return serviceSecurite.getOperateurTous(individuNoTechnique);
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
			return serviceSecurite.getOperateur(visa);
		}
		catch (Exception e) {
			throw new ServiceSecuriteException("impossible de récupérer l'utilisateur correspondant au visa " + visa, e);
		}
	}
}
