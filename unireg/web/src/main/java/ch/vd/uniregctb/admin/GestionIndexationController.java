package ch.vd.uniregctb.admin;

import ch.vd.uniregctb.admin.indexer.GestionIndexation;
import ch.vd.uniregctb.admin.indexer.IndexDocument;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.*;
import ch.vd.uniregctb.indexer.tiers.*;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tracing.TracingManager;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class GestionIndexationController extends AbstractSimpleFormController {

	private final Logger LOGGER = Logger.getLogger(GestionIndexationController.class);

	private GlobalIndexInterface globalIndex;

	private GlobalTiersIndexer tiersIndexer;

	private static final String ACTION_PARAMETER_NAME = "action";

	private static final String ACTION_SEARCH_VALUE = "search";

	private static final String ACTION_PERFORMANCE_VALUE = "performance";

	private static final String ACTION_REINDEX_TIERS = "reindexTiers";

	public static final String INDEX_LIST_ATTRIBUTE_NAME = "index";

	public static final String GESTION_INDEXATION_NAME = "gestionIndexation";


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		GestionIndexation bean = (GestionIndexation) session.getAttribute(GESTION_INDEXATION_NAME);
		if (bean == null) {
			bean = (GestionIndexation) super.formBackingObject(request);
			session.setAttribute(GESTION_INDEXATION_NAME, bean);
		}
		bean.setChemin(globalIndex.getIndexPath());
		bean.setNombreDocumentsIndexes(globalIndex.getApproxDocCount());
		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
		throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		HttpSession session = request.getSession();
		GestionIndexation bean = (GestionIndexation) session.getAttribute(GESTION_INDEXATION_NAME);
		if ((bean != null) && (bean.getRequete() != null) && (!"".equals(bean.getRequete()))) {
			setAttribute(session, bean) ;
		}
		mav.addObject(INDEX_LIST_ATTRIBUTE_NAME, session.getAttribute(INDEX_LIST_ATTRIBUTE_NAME));

		// On flush l'indexer, c'est utile pour le debugging
		// Le fait d'accèder a la page de gestion de l'indexation
		// va provoquer un flush et l'index pourra ensuite etre copié sans problemes
		globalIndex.flush();
		LOGGER.debug("The Global index is flushed");

		TracingManager.outputMeasures(LOGGER);

		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);

		// Validate seulement si on fait une recherche
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		if (action.equals(ACTION_SEARCH_VALUE)) {
			GestionIndexation bean = (GestionIndexation)command;
			try {
				globalIndex.search(bean.getRequete(), new SearchCallback() {
					public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
						// on n'est pas intéressé par les résultats
					}
				});
			}
			catch (Exception e) {
				errors.rejectValue("requete", "error.criteres.invalide");
			}
			LOGGER.debug("La requete '"+bean.getRequete()+"' est valide");
		}
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		GestionIndexation bean = (GestionIndexation) command;
		HttpSession session = request.getSession();
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		if (action.equals(ACTION_SEARCH_VALUE)) {
			if ((bean != null) && (bean.getRequete() != null) && (!"".equals(bean.getRequete()))) {
				setAttribute(session, bean) ;
			}
		}
		else if (action.equals(ACTION_PERFORMANCE_VALUE)) {

		}
		else if (action.equals(ACTION_REINDEX_TIERS)) {
			if (bean != null) {
				String idAsString = bean.getId();
				idAsString = FormatNumeroHelper.removeSpaceAndDash(idAsString);
				long id = Long.parseLong(idAsString);
				LOGGER.info("Demande de réindexation manuelle du tiers n° " + id);
				tiersIndexer.indexTiers(id);
				return new ModelAndView(new RedirectView("/tiers/visu.do?id=" + id, true));
			}
		}
		mav.setView(new RedirectView(getSuccessView()));
		return mav;
	}


	/**
	 * Met a jour les listes en Session
	 * @param session
	 * @param bean
	 */
	private void setAttribute(HttpSession session, GestionIndexation bean) {

		final List<IndexDocument> listIndexDocument = new ArrayList<IndexDocument>();

		globalIndex.search(bean.getRequete(), new SearchCallback() {
			public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
				for (DocHit h : hits) {
					Document doc = null;
					try {
						doc = docGetter.get(h.doc);
					}
					catch (Exception e) {
						LOGGER.error(e);
						continue; // rien de mieux à faire
					}
					IndexDocument indexDocument = new IndexDocument();
					indexDocument.setEntityId(doc.get(LuceneEngine.F_ENTITYID));
					indexDocument.setNomCourrier1(doc.get(TiersIndexableData.NOM1));
					indexDocument.setNomCourrier2(doc.get(TiersIndexableData.NOM2));
					indexDocument.setDateNaissance(doc.get(TiersIndexableData.DATE_NAISSANCE));
					indexDocument.setNumeroAvs(doc.get(TiersIndexableData.NUMERO_ASSURE_SOCIAL));
					indexDocument.setNomFor(doc.get(TiersIndexableData.FOR_PRINCIPAL));
					indexDocument.setLocalite(doc.get(TiersIndexableData.LOCALITE));
					listIndexDocument.add(indexDocument);
				}
			}
		});

		session.setAttribute(INDEX_LIST_ATTRIBUTE_NAME, listIndexDocument);
		session.setAttribute(GESTION_INDEXATION_NAME, bean);
	}

	/**
	 * @param globalIndex the globalIndex to set
	 */
	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
		this.globalIndex = globalIndex;
	}

	public void setTiersIndexer(GlobalTiersIndexer tiersIndexer) {
		this.tiersIndexer = tiersIndexer;
	}
}

