package ch.vd.unireg.interfaces.service.refsec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeCommunication;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.interfaces.service.host.ProfileOperateurImpl;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.wsclient.refsec.RefSecClient;
import ch.vd.unireg.wsclient.refsec.RefSecClientException;
import ch.vd.unireg.wsclient.refsec.RefSecClientTracing;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;
import ch.vd.unireg.wsclient.refsec.model.User;


public class ServiceSecuriteRefSecImpl implements ServiceSecuriteService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSecuriteRefSecImpl.class);

	private RefSecClient refSecClient;
	private ServiceInfrastructureService serviceInfrastructureService;


	@Override
	public ProfileOperateur getProfileUtilisateur(String visa, int codeCollectivite) throws ServiceSecuriteException {

		try {

			final User user = refSecClient.getUser(visa);
			final ProfilOperateur profile = refSecClient.getProfilOperateur(visa, codeCollectivite);
			return ProfileOperateurImpl.get(profile, user);
		}
		catch (RefSecClientException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer le profil de l'utilisateur  refsec " + visa + " pour l'OID "
					                                   + codeCollectivite, e);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer l'utilisateur  IAM " + visa, e);
		}

	}

	@Override
	@Nullable
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
		try {
			final User user = refSecClient.getUser(visa);
			return Operateur.get(user, visa);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer le profil operateur  " + visa, e);
		}
	}


	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		try {
			final Set<Operateur> resultat = new HashSet<>();
			final List<CollectiviteAdministrative> collectivitesAdministratives = serviceInfrastructureService.getCollectivitesAdministratives(typesCollectivite);
			collectivitesAdministratives.forEach(collectivite -> {
				final List<User> usersFromCollectivite = refSecClient.getUsersFromCollectivite(collectivite.getNoColAdm());
				final Set<Operateur> operateurs = usersFromCollectivite.stream()
						.map(User::getVisa)
						.map(uip -> Operateur.get(refSecClient.getUser(uip), uip))
						.collect(Collectors.toSet());
				resultat.addAll(operateurs);
			});
			final List<Operateur> operateurs = new ArrayList<>(resultat);
			Collections.sort(operateurs);
			return operateurs;
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer la liste des operateurs ", e);
		}
	}

	@Override
	@Deprecated
	public Operateur getOperateur(long individuNoTechnique) {
		LOGGER.error("getOperateur avec individuNoTechnique n''est plus supporter, merci d'utiliser getOperateur avec le visa");
		return null;
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visa) throws ServiceSecuriteException {
		try {
			final Set<Integer> codeCollectivites = refSecClient.getCollectivitesOperateur(visa);
			final List<CollectiviteAdministrative> collectivitesAdministratives = serviceInfrastructureService.findCollectivitesAdministratives(new ArrayList<>(codeCollectivites), false);
			return filtreCollectiviteAdministrativeParTypeCommunicationACI(collectivitesAdministratives)
					.collect(Collectors.toList());
		}
		catch (RefSecClientException e) {
			throw new ServiceSecuriteException("impossible de récupérer les codes collectivités administrative depuis Refsec de l'operateur   " + visa, e);
		}
		catch (Exception e) {
			throw new ServiceSecuriteException("impossible de récupérer les collectivités administrative depuis fidor de l'operateur   " + visa, e);
		}
	}

	@NotNull
	private Stream<CollectiviteAdministrative> filtreCollectiviteAdministrativeParTypeCommunicationACI(List<CollectiviteAdministrative> collectivites) {
		return collectivites.stream()
				.filter(ServiceSecuriteRefSecImpl::hasTypeCommunicationACI);
	}

	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visa) throws ServiceSecuriteException {
		final List<CollectiviteAdministrative> collectivitesUtilisateur = getCollectivitesUtilisateur(visa);
		//aucun critère défini pour savoir le la collectivité par defaut, alors on renvoi la 1er de la liste.
		final CollectiviteAdministrative collectiviteAdministrative = collectivitesUtilisateur.stream().findFirst().orElse(null);
		return collectiviteAdministrative != null ? collectiviteAdministrative.getNoColAdm() : null;
	}

	private static boolean hasTypeCommunicationACI(@NotNull CollectiviteAdministrative collectiviteAdministrative) {
		return collectiviteAdministrative.getEchangeAciCom() == null
				|| collectiviteAdministrative.getEchangeAciCom().isEmpty()
				|| collectiviteAdministrative.getEchangeAciCom().stream()
				.anyMatch(echangeAciCom -> echangeAciCom.getTypeCommunication().equals(TypeCommunication.ACI) && echangeAciCom.isValidAt(RegDate.get()));
	}


	public void setRefSecClient(RefSecClientTracing refSecClient) {
		this.refSecClient = refSecClient;
	}


	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}
}
