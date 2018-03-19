package ch.vd.unireg.acces.parUtilisateur.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.extraction.ExtractionJob;
import ch.vd.unireg.security.DroitAccesException;

public interface UtilisateurEditRestrictionManager {

    /**
     * Alimente la vue du controller
     */
    @Transactional(readOnly = true)
    UtilisateurEditRestrictionView get(long noIndividuOperateur, WebParamPagination pagination) throws ServiceInfrastructureException, AdresseException;

    /**
     * Alimente la vue RecapPersonneUtilisateurView
     */
    @Transactional(readOnly = true)
    RecapPersonneUtilisateurView get(Long numeroTiers, Long noIndividuOperateur) throws ServiceInfrastructureException, AdressesResolutionException;

    /**
     * Annule une liste de restrictions
     */
    @Transactional(rollbackFor = Throwable.class)
    void annulerRestrictions(List<Long> listIdRestriction) throws DroitAccesException;

    /**
     * Annule toutes les restrictions
     */
    @Transactional(rollbackFor = Throwable.class)
    void annulerToutesLesRestrictions(Long noIndividuOperateur);
    /**
     * Persiste le DroitAcces
     */
    @Transactional(rollbackFor = Throwable.class)
    void save(RecapPersonneUtilisateurView recapPersonneUtilisateurView) throws DroitAccesException;

    /**
     * Demande l'export des droits d'accès d'un utilisateur
     *
     * @param operateurId l'id de l'operateur pour lequel on veux exporter les droits
     * @return la demande d'extraction
     */
    @Transactional(readOnly = true)
    ExtractionJob exportListeDroitsAcces(Long operateurId);
}