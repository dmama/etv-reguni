package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseDetailBordereauView;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

/**
 * Controlleur pour l'affichage des mouvements sur un proto-bordereau, la sélection
 * de certains d'entre eux et l'impression du bordereau correspondant
 */
public class MouvementMasseDetailBordereauController extends AbstractMouvementMasseController {

	public static final Logger LOGGER = Logger.getLogger(MouvementMasseDetailBordereauController.class);

	private static final String TYPE = "type";      // type de mouvement du bordereau : EnvoiDossier ou ReceptionDossier
	private static final String SRC = "src";        // ID technique de l'OID initiateur des mouvements
	private static final String DEST = "dest";      // en cas d'envoi, ID technique de l'OID destinataire des mouvements

	private static final String IMPRIMER = "imprimer";

	private ServiceInfrastructureService infraService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	protected MouvementMasseDetailBordereauView formBackingObject(HttpServletRequest request) throws Exception {
		final MouvementMasseDetailBordereauView view = new MouvementMasseDetailBordereauView();
		doFind(request, view);
		return view;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, final BindException errors) throws Exception {
		final String imprimer = request.getParameter(IMPRIMER);
		// validation de l'impression ?
		if (imprimer != null) {
			checkAccess();
			final MouvementMasseDetailBordereauView view = (MouvementMasseDetailBordereauView) command;
			try {

				final TraitementRetourEditique erreur = new TraitementRetourEditique() {
					@Override
					public ModelAndView doJob(EditiqueResultat resultat) {
						final String msg;
						if (resultat instanceof EditiqueResultatErreur) {
							msg = ((EditiqueResultatErreur) resultat).getError();
						}
						else {
							msg = "Erreur inconnue";
						}
						errors.reject("global.error.msg", msg);
						return null;
					}
				};

				final EditiqueResultat resultat = getMouvementManager().imprimerBordereau(view.getSelection());
				traiteRetourEditique(resultat, response, "bordereau", null, erreur, erreur);
			}
			catch (EditiqueException e) {
				errors.reject("global.error.msg", e.getMessage());
			}
		}
		return showForm(request, response, errors);
	}

	/**
	 * Renvoie une paire d'identifiants (numéro de tiers + numéro de collectivité administrative)
	 * trouvés depuis une chaîne de caractères dont le séparateur est "/"
	 * @param paramValue
	 * @return
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

	private void doFind(HttpServletRequest request, MouvementMasseDetailBordereauView view) throws ServiceInfrastructureException {
		final String typeStr = request.getParameter(TYPE);
		final String srcStr = request.getParameter(SRC);        // format "ID/noCa"
		final String destStr = request.getParameter(DEST);      // format "ID/noCa"

		final Pair<Long, Integer> src = decodeCollAdmParamter(srcStr);
		final Pair<Long, Integer> dest = decodeCollAdmParamter(destStr);

		final TypeMouvement typeMouvement = TypeMouvement.valueOf(typeStr);
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
		final List<MouvementDetailView> mouvements = getMouvementManager().find(criteria);
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
	}
}
