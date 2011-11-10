<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.traiter.full"/></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/traiter-mouvement-dossiermasse.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<fieldset>
			<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
			<form:form method="post" action="rechercher-pour-traitement.do" commandName="criteria">
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</form:form>
		</fieldset>

		<c:if test="${found != null}">
			<form:form method="post" action="inclure-dans-bordereau.do" commandName="found">
				<script type="text/javascript">
					function reinitMouvementDossierMasse(id) {
						var form = $('<form method="POST" action="' + getContextPath() + '/mouvement/masse/reinit.do?id=' + id +'&pagination=${pagination}"/>');
						form.appendTo('body'); // [UNIREG-3151] obligatoire pour que cela fonctionne avec IE6
						form.submit();
					}

					function annulerMouvementDossierMasse(id) {
						var form = $('<form method="POST" action="' + getContextPath() + '/mouvement/masse/cancel.do?id=' + id +'&pagination=${pagination}"/>');
						form.appendTo('body'); // [UNIREG-3151] obligatoire pour que cela fonctionne avec IE6
						form.submit();
					}

					function selectAllMouvements(checkSelectAll) {
						var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
						var taille = lignesMvts.length;
						for(var i=1; i < taille; i++) {
							var cols = lignesMvts[i].getElementsByTagName("td");
							if (cols != null && cols.length > 1) {
								var inputs = cols[0].getElementsByTagName("input");
								if (inputs != null && inputs.length > 0) {
									var chkbox = inputs[0];
									if (chkbox != null && !chkbox.disabled && chkbox.name == 'tabIdsMvts') {
										chkbox.checked = checkSelectAll.checked;
									}
								}
							}
						}
					}

					function confirmeInclusionBordereau() {
						var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
						var taille = lignesMvts.length;
						var nbSelectionnes = 0;
						for(var i=1; i < taille; i++) {
							var cols = lignesMvts[i].getElementsByTagName("td");
							if (cols != null && cols.length > 1) {
								var inputs = cols[0].getElementsByTagName("input");
								if (inputs != null && inputs.length > 0) {
									var chkbox = inputs[0];
									if (chkbox != null && !chkbox.disabled && chkbox.name == 'tabIdsMvts' && chkbox.checked) {
										++ nbSelectionnes;
									}
								}
							}
						}
						if (nbSelectionnes == 0) {
							alert('Veuillez sélectionner au moins un dossier à inclure');
							return false;
						}
						else {
							var txtMvt;
							if (nbSelectionnes == 1) {
								txtMvt = 'le dossier sélectionné';
							}
							else {
								txtMvt = 'les ' + nbSelectionnes + ' dossiers sélectionnés';
							}
							return confirm('Voulez-vous inclure ' + txtMvt + ' dans un bordereau ?');
						}
					}
				</script>

				<display:table name="found.results" id="mvt" pagesize="25" requestURI="/mouvement/masse/pour-traitement.do" class="display_table" sort="external" size="found.resultSize" partialList="true" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.mouvement.trouve" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.mouvement.trouve" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>

					<display:column title="<input type='checkbox' name='selectAll' onclick='selectAllMouvements(this);'/>">
						<c:if test="${!mvt.annule && mvt.etatMouvement == 'A_TRAITER'}">
							<input type="checkbox" name="tabIdsMvts" id="tabIdsMvts_${mvt_rowNum}" value="${mvt.id}" >
						</c:if>
						<c:if test="${!mvt.annule && mvt.etatMouvement == 'A_ENVOYER'}">
							<input type="checkbox" name="tabIdsMvts" id="tabIdsMvts_${mvt_rowNum}" value="" checked="true" disabled="true">
						</c:if>
					</display:column>

					<display:column sortable="true" titleKey="label.numero.tiers" sortName="contribuable.numero">
						<unireg:numCTB numero="${mvt.contribuable.numero}" />
					</display:column>
					<display:column titleKey="label.nom.raison" >
						<c:forEach var="ligne" items="${mvt.contribuable.nomPrenom}" varStatus="rowCounter">
							<c:if test="${rowCounter.count > 1}"><br/></c:if><c:out value="${ligne}"/>
						</c:forEach>
					</display:column>
					<display:column titleKey="label.type.mouvement" >
						<fmt:message key="option.type.mouvement.${mvt.typeMouvement}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.etat.mouvement" sortName="etat">
						<fmt:message key="option.etat.mouvement.${mvt.etatMouvement}"/>
					</display:column>
					<c:if test="${montrerInitiateur}">
						<display:column titleKey="label.collectivite.administrative">
							<c:out value="${mvt.collectiviteAdministrative}"/>
						</display:column>
					</c:if>
					<display:column titleKey="label.destination" >
						<c:out value="${mvt.destinationUtilisateur}" />
					</display:column>

					<display:column style="action">
						<a href="#" class="detail" title="Détail d'un mouvement" onclick="return open_details_mouvement(${mvt.id});">&nbsp;</a>
						<unireg:consulterLog entityNature="MouvementDossier" entityId="${mvt.id}"/>
						<c:if test="${mvt.etatMouvement == 'A_TRAITER'}">
							<unireg:raccourciAnnuler onClick="annulerMouvementDossierMasse(${mvt.id});" tooltip="Annuler un mouvement"/>
						</c:if>
						<c:if test="${mvt.etatMouvement == 'A_ENVOYER' || mvt.etatMouvement == 'RETIRE'}">
							<unireg:raccourciReinit onClick="reinitMouvementDossierMasse(${mvt.id});" tooltip="Ré-initialiser un mouvement"/>
						</c:if>
					</display:column>

				</display:table>

				<input type="hidden" name="pagination" value="${pagination}"/>

				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%">&nbsp;</td>
						<td width="50%">
							<div id="boutonInclusion" class="navigation-action">
								<input type="submit" value="<fmt:message key='label.bouton.inclure.dans.bordereau'/>" name="inclure" onclick="return confirmeInclusionBordereau();"/>
							</div>
							&nbsp;
						</td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>

				<script type="text/javascript">
					var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
					var taille = lignesMvts.length;
					var nbSelectionnables = 0;
					for(var i=1; i < taille; i++) {
						var cols = lignesMvts[i].getElementsByTagName("td");
						if (cols != null && cols.length > 1) {
							var inputs = cols[0].getElementsByTagName("input");
							if (inputs != null && inputs.length > 0) {
								var chkbox = inputs[0];
								if (chkbox != null && !chkbox.disabled && chkbox.name == 'tabIdsMvts') {
									++ nbSelectionnables;
								}
							}
						}
					}
					if (nbSelectionnables == 0) {
						document.getElementById('boutonInclusion').style.display = "none";
					}
				</script>
			</form:form>
		</c:if>

   </tiles:put>

</tiles:insert>
