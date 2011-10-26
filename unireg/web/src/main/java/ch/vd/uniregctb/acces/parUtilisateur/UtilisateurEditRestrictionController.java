package ch.vd.uniregctb.acces.parUtilisateur;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.acces.parUtilisateur.manager.UtilisateurEditRestrictionManager;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;

@Controller
public class UtilisateurEditRestrictionController {

    private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec pour accéder à la sécurité des droits";
    protected final Logger LOGGER = Logger.getLogger(UtilisateurEditRestrictionController.class);

    private UtilisateurEditRestrictionManager utilisateurEditRestrictionManager;

    public void setUtilisateurEditRestrictionManager(UtilisateurEditRestrictionManager utilisateurEditRestrictionManager) {
        this.utilisateurEditRestrictionManager = utilisateurEditRestrictionManager;
    }

    @RequestMapping(value = "/acces/restrictions-utilisateur.do", method = RequestMethod.GET)
    @SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR, Role.SEC_DOS_LEC}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
    public ModelAndView getRestrictionsUtilisateur(@RequestParam("noIndividuOperateur") Long noIndividuOperateur) throws Exception {
        UtilisateurEditRestrictionView utilisateurEditRestrictionView = utilisateurEditRestrictionManager.get(noIndividuOperateur);
        return new ModelAndView("acces/par-utilisateur/restrictions-utilisateur", "command", utilisateurEditRestrictionView);
    }

    @RequestMapping(value = "/acces/restrictions-utilisateur/annuler.do", method = RequestMethod.POST)
    @SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
    public String onPostAnnulerRestriction(
            @RequestParam("noIndividuOperateur") Long noIndividuOperateur,
            @RequestParam(value = "aAnnuler", required = false) List<Long> restrictionsAAnnuler,
            @RequestParam("annuleTout") Boolean annuleTout) {
        try {
            if (annuleTout) {
                utilisateurEditRestrictionManager.annulerToutesLesRestrictions(noIndividuOperateur);
            } else {
                utilisateurEditRestrictionManager.annulerRestriction(restrictionsAAnnuler);
            }
        }
        catch (DroitAccesException e) {
            throw new ActionException(e.getMessage(), e);
        }
        return "redirect:/acces/restrictions-utilisateur.do?noIndividuOperateur=" + noIndividuOperateur;
    }

    @RequestMapping(value = "/acces/restrictions-utilisateur/exporter.do", method = RequestMethod.POST)
    @SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
    public String onPostExporter(@RequestParam("noIndividuOperateur") Long noIndividuOperateur) {
        final ExtractionJob job = utilisateurEditRestrictionManager.exportListeDroitsAcces(noIndividuOperateur);
        Flash.message(String.format("Demande d'export enregistrée (%s)", job.getDescription()));
        return "redirect:/acces/restrictions-utilisateur.do?noIndividuOperateur=" + noIndividuOperateur;
    }
}
