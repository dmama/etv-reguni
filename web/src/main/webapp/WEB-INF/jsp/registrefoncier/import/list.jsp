<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<style>
			.checkboxes {
				margin: 5px;
				display: block;
			}
			.checkboxes input {
				margin-left: 10px;
				margin-right: 2px;
			}
			tr.headerGroup th {
				text-align: center;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.suivi.imports.registre.foncier" />
	</tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<form:form method="get" commandName="view" id="formSuiviImportRF" action="list.do">

			<script type="text/javascript">
				function submitForm(){
					$('#formSuiviImportRF').submit();
				}
			</script>

			<span class="checkboxes">
		        Etats : <form:checkbox path="aTraiter" id="aTraiter" label="A traiter" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="enTraitement" id="enTraitement" label="En traitement" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="traite" id="traite" label="Traité" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="enErreur" id="enErreur" label="En erreur" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="force" id="force" label="Forcé" onchange="submitForm()" onclick="submitForm()" />
		    </span>

			<%--@elvariable id="importEvent" type="ch.vd.uniregctb.registrefoncier.importrf.EvenementRFImportView"--%>
			<%--@elvariable id="pageSize" type="java.lang.Long"--%>
			<%--@elvariable id="count" type="java.lang.Long"--%>
			<display:table name="list" id="importEvent" class="display_table" pagesize="${pageSize}" size="${count}" sort="external" partialList="true"
			               requestURI="/registrefoncier/import/list.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.import.trouve"/></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.un.import.trouve"/></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.imports.trouves"/></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.imports.trouves"/></span></display:setProperty>

				<display:column titleKey="label.id.event.rf" sortable="true" sortName="id" class="import-id" >
					<unireg:linkTo action="/registrefoncier/import/show.do" name="${importEvent.id}" params="{'importId' : ${importEvent.id}}"/>
				</display:column>
				<display:column titleKey="label.date.valeur.import" sortable="true" sortName="dateEvenement">
					<unireg:regdate regdate="${importEvent.dateEvenement}"/>
				</display:column>
				<display:column titleKey="label.type.import.rf" sortable="true" sortName="type">
					<fmt:message key="option.rf.type.import.${importEvent.type}" />
				</display:column>
				<display:column titleKey="label.etat.event.rf" sortable="true" sortName="etat" class="import-etat">
					<span class="raw-value" style="display: none">${importEvent.etat}</span>
					<fmt:message key="option.rf.etat.evenement.${importEvent.etat}" />
				</display:column>
				<display:column titleKey="label.message.erreur.import" >
					${importEvent.errorMessage} <c:if test="${importEvent.callstack != null}"><a href="#" onclick="showCallstack(${importEvent.id})">callstack</a></c:if>
				</display:column>
				<display:column titleKey="label.actions" class="action">
					<c:if test="${importEvent.etat == 'EN_ERREUR' || importEvent.etat == 'A_TRAITER'}">
						<unireg:buttonTo name="Relancer l'import" confirm="Voulez-vous vraiment relancer le traitement de l'import n°${importEvent.id} ?"
						                 action="/registrefoncier/import/restart.do" params="{importId:${importEvent.id}}"/>
						<unireg:buttonTo name="Forcer l'import" confirm="Voulez-vous vraiment forcer le traitement de l'import n°${importEvent.id} (y compris les mutations) ? \n\nEn forçant cet import, il sera marqué comme 'forcé' et les mutations non-traitées ne seront jamais intégrées dans Unireg."
						                 action="/registrefoncier/import/force.do" params="{importId:${importEvent.id}}"/>
					</c:if>
					<unireg:consulterLog entityNature="EvenementRFImport" entityId="${importEvent.id}"/>
				</display:column>
				<display:column titleKey="label.mutations.a.traiter">
					<div class="a-traiter"><img src="<c:url value="/images/loading.gif"/>" /></div>
				</display:column>
				<display:column titleKey="label.mutations.traitees">
					<div class="traitees"><img src="<c:url value="/images/loading.gif"/>" /></div>
				</display:column>
				<display:column titleKey="label.mutations.en.erreur">
					<div class="en-erreur"><img src="<c:url value="/images/loading.gif"/>" /></div>
				</display:column>
				<display:column titleKey="label.mutations.forcees">
					<div class="forcees"><img src="<c:url value="/images/loading.gif"/>" /></div>
				</display:column>
				<display:column titleKey="label.actions" class="action">
					<div class="actionTraitementMutations" style="display:none">
						<unireg:buttonTo name="Relancer le traitement" confirm="Voulez-vous vraiment relancer le traitement des mutations de l'import n°${importEvent.id} ?"
						                 action="/registrefoncier/mutation/restart.do" params="{importId:${importEvent.id}}"/>
						<unireg:buttonTo name="Forcer toutes les mutations" confirm="Voulez-vous vraiment forcer toutes les mutations non-traitées de l'import n°${importEvent.id} ? \n\nEn forçant ces mutations, elles seront marquées comme 'forcées' et ne seront jamais intégrées dans Unireg."
						                 action="/registrefoncier/mutation/forceAll.do" params="{importId:${importEvent.id}}"/>
					</div>
				</display:column>
			</display:table>

			<script type="text/javascript">
				$(function() {
					var table = $('#importEvent');
					// on ajoute une ligne d'entête pour différencier la génération des mutations du traitement des mutations
					table.find('thead tr:first').before('<tr class="headerGroup"><th colspan="2" style="background: none"></th><th colspan="3">Génération des mutations</th><th colspan="5">Traitement des mutations</th></tr>');

					function addMutationCount(div, importId, count, additionalParams) {
						if (count == 0) {
							div.prop('innerHTML', count);
						}
						else {
							var url = '<c:url value="/registrefoncier/import/show.do?importId="/>' + importId + additionalParams;
							div.prop('innerHTML', '<a href="' + url + '">' + count + '</a>');
						}
					}

					// on va chercher de manière asynchrone les statistiques sur chaque import (optimisation pour ne pas bloquer l'affichage lors de la récupération des stats)
					table.find('tbody').find('tr').each(function() {
						var tr = $(this);
						var importId = tr.find('.import-id').text();
						var importEtat = tr.find('.import-etat .raw-value').text();
						(function(importId, importEtat) {   // <-- trick javascript pour fixer les paramètres importId et importEtat et pouvoir les utiliser dans le callback de l'appel Aajx
							$.getJSON(App.curl("/registrefoncier/import/stats.do?importId=") + importId + "&" + new Date().getTime(), function(stats) {
								addMutationCount(tr.find('div.a-traiter'), importId, stats.mutationsATraiter, '&aTraiter=true&_aTraiter=on&_traite=on&_enErreur=on&_force=on');
								addMutationCount(tr.find('div.traitees'), importId, stats.mutationsTraitees, '&_aTraiter=on&traite=true&_traite=on&_enErreur=on&_force=on');
								addMutationCount(tr.find('div.en-erreur'), importId, stats.mutationsEnErreur, '&_aTraiter=on&_traite=on&enErreur=true&_enErreur=on&_force=on');
								addMutationCount(tr.find('div.forcees'), importId, stats.mutationsForcees, '&_aTraiter=on&_traite=on&_enErreur=on&force=true&_force=on');

								// on active les boutons de relance/forçage des mutations si nécessaire
								if (importEtat != 'EN_TRAITEMENT' && (stats.mutationsATraiter > 0 || stats.mutationsEnErreur >  0)) {
									tr.find('.actionTraitementMutations').show();
								}
							});
						})(importId, importEtat);
					});
				});

				function showCallstack(importId) {
					// charge le contenu de la boîte de dialogue
					$.getJSON(App.curl('/registrefoncier/import/get.do?importId=') + importId + '&' + new Date().getTime(), function (imp) {
						var dialog = Dialog.create_dialog_div('show-callstack');
						dialog.html('<pre>' + imp.callstack + '</pre>');
						dialog.dialog({
							              title: 'Callstack',
							              height: 800,
							              width: 800,
							              modal: true,
							              buttons: {
								              Ok: function () {
									              dialog.dialog("close");
								              }
							              }
						              });
					});

					//prevent the browser to follow the link
					return false;
				}
			</script>

		</form:form>
	</tiles:put>
</tiles:insert>
