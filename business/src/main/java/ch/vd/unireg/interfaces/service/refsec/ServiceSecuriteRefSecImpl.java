package ch.vd.unireg.interfaces.service.refsec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.cfg.NotYetImplementedException;
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
import ch.vd.unireg.wsclient.iam.IamClient;
import ch.vd.unireg.wsclient.iam.IamUser;
import ch.vd.unireg.wsclient.refsec.RefSecClient;
import ch.vd.unireg.wsclient.refsec.RefSecClientException;
import ch.vd.unireg.wsclient.refsec.RefSecClientTracing;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;


public class ServiceSecuriteRefSecImpl implements ServiceSecuriteService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSecuriteRefSecImpl.class);

	private RefSecClient refSecClient;
	private IamClient iamClient;
	private ServiceInfrastructureService serviceInfrastructureService;


	@Override
	public ProfileOperateur getProfileUtilisateur(String visa, int codeCollectivite) {

		try {
			final IamUser iamUser = iamClient.getUser(visa);
			final ProfilOperateur profile = refSecClient.getProfilOperateur(visa, codeCollectivite);
			return ProfileOperateurImpl.get(profile, iamUser);
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
	public Operateur getOperateur(@NotNull String visa) {
		try {
			final IamUser iamUser = iamClient.getUser(visa);
			return Operateur.get(iamUser);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer le profil operateur  " + visa, e);
		}
	}


	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {
		throw new NotYetImplementedException("getAuthorizationsByCodeCollectivite  pas implémenté");
	}

	@Override
	@Deprecated
	public Operateur getOperateur(long individuNoTechnique) {
		LOGGER.error("getOperateur avec individuNoTechnique n''est plus supporter, merci d'utiliser getOperateur avec le visa");
		return null;
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visa) {
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
	public @Nullable Integer getCollectiviteParDefaut(@NotNull String visa) {
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

	public void setIamClient(IamClient iamClient) {
		this.iamClient = iamClient;
	}


	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}
}
