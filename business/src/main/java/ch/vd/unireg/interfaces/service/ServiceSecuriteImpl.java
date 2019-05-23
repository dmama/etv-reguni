package ch.vd.unireg.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import ch.vd.unireg.interfaces.securite.SecuriteConnector;
import ch.vd.unireg.interfaces.securite.SecuriteConnectorException;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;


public class ServiceSecuriteImpl implements ServiceSecuriteService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSecuriteImpl.class);

	private SecuriteConnector securiteConnector;
	private ServiceInfrastructureService serviceInfrastructureService;


	@Nullable
	@Override
	public ProfileOperateur getProfileUtilisateur(String visa, int codeCollectivite) throws ServiceSecuriteException {
		try {
			return securiteConnector.getProfileUtilisateur(visa, codeCollectivite);
		}
		catch (SecuriteConnectorException e) {
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
			return securiteConnector.getOperateur(visa);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer le profil operateur  " + visa, e);
		}
	}

	@NotNull
	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		try {
			final List<CollectiviteAdministrative> collectivitesAdministratives = serviceInfrastructureService.getCollectivitesAdministratives(typesCollectivite);
			return collectivitesAdministratives.stream()
					.map(CollectiviteAdministrative::getNoColAdm)
					.map(securiteConnector::getUtilisateurs)
					.flatMap(Collection::stream)
					.distinct()
					.sorted()
					.map(this::getOperateur)
					.collect(Collectors.toList());
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ServiceSecuriteException("impossible de récupérer la liste des operateurs ", e);
		}
	}

	@NotNull
	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visa) throws ServiceSecuriteException {
		if (visa == null) {
			throw new IllegalArgumentException("Le visa est obligatoire.");
		}
		try {
			final Set<Integer> codeCollectivites = securiteConnector.getCollectivitesOperateur(visa);
			final List<CollectiviteAdministrative> collectivitesAdministratives = serviceInfrastructureService.findCollectivitesAdministratives(new ArrayList<>(codeCollectivites), false);
			return filtreCollectiviteAdministrativeParTypeCommunicationACI(collectivitesAdministratives)
					.sorted(Comparator.comparing(CollectiviteAdministrative::getNomCourt))
					.collect(Collectors.toList());
		}
		catch (SecuriteConnectorException e) {
			throw new ServiceSecuriteException("impossible de récupérer les codes collectivités administrative depuis Refsec de l'operateur   " + visa, e);
		}
		catch (Exception e) {
			throw new ServiceSecuriteException("impossible de récupérer les collectivités administrative depuis fidor de l'operateur   " + visa, e);
		}
	}

	@NotNull
	private Stream<CollectiviteAdministrative> filtreCollectiviteAdministrativeParTypeCommunicationACI(List<CollectiviteAdministrative> collectivites) {
		return collectivites.stream()
				.filter(ServiceSecuriteImpl::hasTypeCommunicationACI);
	}

	@Nullable
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

	@Override
	public void ping() throws ServiceSecuriteException {
		securiteConnector.ping();
	}

	public void setSecuriteConnector(SecuriteConnector securiteConnector) {
		this.securiteConnector = securiteConnector;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}
}
