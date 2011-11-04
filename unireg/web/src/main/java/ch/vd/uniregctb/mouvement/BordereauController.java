package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.mouvement.manager.MouvementMasseManager;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiListView;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiReceptionView;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiView;
import ch.vd.uniregctb.mouvement.view.BordereauListElementView;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseDetailBordereauView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseImpressionBordereauxView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

@Controller
@RequestMapping(value = "/mouvement/bordereau")
public class BordereauController {

	// dans les forms où on peut choisir plusieurs éléments, ceux-ci sont regroupés dans un champ commun
	private static final String SELECTION = "selection";

	// impression des bordereaux
	private static final String TYPE = "type";      // type de mouvement du bordereau : EnvoiDossier ou ReceptionDossier
	private static final String SRC = "src";        // ID technique de l'OID initiateur des mouvements
	private static final String DEST = "dest";      // en cas d'envoi, ID technique de l'OID destinataire des mouvements

	// réception des bordereaux
	private static final String ID = "id";

	private MouvementMasseManager mouvementManager;

	private ServiceInfrastructureService infraService;

	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementManager(MouvementMasseManager mouvementManager) {
		this.mouvementManager = mouvementManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
	}

	@RequestMapping(value = "/a-imprimer.do", method = RequestMethod.GET)
	public String getListeProtoBordereaux(Model model) {
		final Integer noCollAdmFiltrage = MouvementDossierHelper.getNoCollAdmFiltree();
		final MouvementMasseImpressionBordereauxView view = new MouvementMasseImpressionBordereauxView();
		final List<BordereauListElementView> bordereaux = mouvementManager.getProtoBordereaux(noCollAdmFiltrage);
		view.setBordereaux(bordereaux);
		view.setMontreExpediteur(noCollAdmFiltrage == null);
		model.addAttribute("command", view);
		return "/mouvement/masse/imprimer/list";
	}

	/**
	 * Renvoie une paire d'identifiants (numéro de tiers + numéro de collectivité administrative)
	 * trouvés depuis une chaîne de caractères dont le séparateur est "/"
	 * @param paramValue valeur du paramètre
	 * @return chaîne décodée
	 */
	private static Pair<Long, Integer> decodeCollAdmParamter(String paramValue) {
		if (paramValue != null) {
			final StringTokenizer tokenizer = new StringTokenizer(paramValue, "/", false);
			if (tokenizer.hasMoreTokens()) {
				final String noTiers = tokenizer.nextToken();
				final String noCa = tokenizer.nextToken();
				return new Pair<Long, Integer>(Long.valueOf(noTiers), Integer.valueOf(noCa));
			}
		}
		return null;
	}

	@RequestMapping(value = "/detail-avant-impression.do", method = RequestMethod.GET)
	public String getDetailProtoBordereau(Model model, @RequestParam(value = SRC) String source, @RequestParam(value = DEST) String destination, @RequestParam(value = TYPE) TypeMouvement typeMouvement) {
		final Pair<Long, Integer> src = decodeCollAdmParamter(source);
		final Pair<Long, Integer> dest = decodeCollAdmParamter(destination);

		final MouvementDossierCriteria criteria = new MouvementDossierCriteria();
		criteria.setTypeMouvement(typeMouvement);
		criteria.setEtatsMouvement(Arrays.asList(EtatMouvementDossier.A_ENVOYER));
		criteria.setInclureMouvementsAnnules(false);
		if (typeMouvement == TypeMouvement.ReceptionDossier) {
			// les bordereaux de réception sont pour les archives
			criteria.setLocalisation(Localisation.ARCHIVES);
			criteria.setIdCollAdministrativeInitiatrice(src.getFirst());
		}
		else {
			criteria.setIdCollAdministrativeDestinataire(dest.getFirst());
			criteria.setIdCollAdministrativeInitiatrice(src.getFirst());
		}

		// la liste des mouvements
		final List<MouvementDetailView> mouvements = mouvementManager.find(criteria);

		final MouvementMasseDetailBordereauView view = new MouvementMasseDetailBordereauView();
		view.setMouvements(mouvements);

		// les détails de la recherche...
		view.setTypeMouvement(typeMouvement);
		final String nomCollInitiatrice = infraService.getCollectivite(src.getSecond()).getNomCourt();
		final String nomCollReceptrice;
		if (typeMouvement == TypeMouvement.ReceptionDossier) {
			nomCollReceptrice = null;
		}
		else {
			nomCollReceptrice = infraService.getCollectivite(dest.getSecond()).getNomCourt();
		}
		view.setNomCollAdmInitiatrice(nomCollInitiatrice);
		view.setNomCollAdmDestinataire(nomCollReceptrice);

		model.addAttribute("command", view);
		model.addAttribute(SRC, source);
		model.addAttribute(DEST, destination);
		model.addAttribute(TYPE, typeMouvement);
		return "/mouvement/masse/imprimer/detail";
	}

	private static String buildRedirectToDetailAvantImpression(String source, String destination, TypeMouvement typeMouvement) {
		return String.format("redirect:/mouvement/bordereau/detail-avant-impression.do?%s=%s&%s=%s&%s=%s",
		                     SRC, source, DEST, destination, TYPE, typeMouvement);
	}

	@RequestMapping(value = "/imprimer-nouveau-bordereau.do", method = RequestMethod.POST)
	public String imprimerNouveauBordereau(@RequestParam(value = SELECTION) long[] ids,
	                                       @RequestParam(value = SRC) final String source,
	                                       @RequestParam(value = DEST) final String destination,
	                                       @RequestParam(value = TYPE) final TypeMouvement typeMouvement,
	                                       HttpServletResponse response) throws IOException, AccessDeniedException {

		MouvementDossierHelper.checkAccess();

		try {
			final RetourEditiqueControllerHelper.TraitementRetourEditique erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique() {
				@Override
				public String doJob(EditiqueResultat resultat) {
					final String msg;
					if (resultat instanceof EditiqueResultatErreur) {
						msg = ((EditiqueResultatErreur) resultat).getError();
					}
					else {
						msg = "Erreur inconnue";
					}
					Flash.error(msg);
					return buildRedirectToDetailAvantImpression(source, destination, typeMouvement);
				}
			};

			final EditiqueResultat resultat = mouvementManager.imprimerBordereau(ids);
			return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "bordereau", null, erreur, erreur);
		}
		catch (EditiqueException e) {
			Flash.error(e.getMessage());
			return buildRedirectToDetailAvantImpression(source, destination, typeMouvement);
		}
	}

	@RequestMapping(value = "/reception.do", method = RequestMethod.GET)
	public String afficherBordereauxAReceptionner(Model model) {
		final Integer noCollAdmReceptrice = MouvementDossierHelper.getNoCollAdmFiltree();
		final List<BordereauEnvoiView> bordereaux = mouvementManager.findBordereauxAReceptionner(noCollAdmReceptrice);
		final BordereauEnvoiListView view = new BordereauEnvoiListView();
		view.setBordereaux(bordereaux);
		view.setMontreDestinataire(noCollAdmReceptrice == null);
		model.addAttribute("command", view);
		return "/mouvement/masse/receptionner/list";
	}

	@RequestMapping(value = "/detail-reception.do", method = RequestMethod.GET)
	public String afficherDetailBordereauAReceptionner(Model model,
	                                                   @RequestParam(value = ID) long idBordereau) {
		final BordereauEnvoiReceptionView view = mouvementManager.getBordereauPourReception(idBordereau);
		model.addAttribute("command", view);
		return "/mouvement/masse/receptionner/detail";
	}

	@RequestMapping(value = "/valider-reception.do", method = RequestMethod.POST)
	public String validerReceptionBordereau(@RequestParam(value = SELECTION) long[] idMouvementsValides,
	                                        @RequestParam(value = ID) long bordereauId) throws AccessDeniedException {

		MouvementDossierHelper.checkAccess();
		mouvementManager.receptionnerMouvementsEnvoi(idMouvementsValides);
		return String.format("redirect:/mouvement/bordereau/detail-reception.do?%s=%d", ID, bordereauId);
	}
}
