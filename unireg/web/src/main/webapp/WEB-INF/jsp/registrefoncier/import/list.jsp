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

			<fieldset>
			<legend><span><fmt:message key="title.suivi.imports.criteria"/></span></legend>
				<span class="checkboxes">
			        Etats : <form:checkbox path="aTraiter" id="aTraiter" label="A traiter" onchange="submitForm()" onclick="submitForm()" />
			        <form:checkbox path="traite" id="traite" label="Traité" onchange="submitForm()" onclick="submitForm()" />
			        <form:checkbox path="enErreur" id="enErreur" label="En erreur" onchange="submitForm()" onclick="submitForm()" />
			        <form:checkbox path="force" id="force" label="Forcé" onchange="submitForm()" onclick="submitForm()" />
			    </span>
			</fieldset>

			<%--@elvariable id="importEvent" type="ch.vd.uniregctb.registrefoncier.EvenementRFImportView"--%>
			<%--@elvariable id="count" type="java.lang.Long"--%>
			<display:table name="list" id="importEvent" class="display_table" pagesize="10" size="${count}" sort="external" partialList="true"
			               requestURI="/registrefoncier/import/list.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.import.trouvee"/></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.un.import.trouve"/></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.imports.trouves"/></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.imports.trouves"/></span></display:setProperty>

				<display:column titleKey="label.id.import" sortable="true" sortProperty="id" class="import-id" >
					${importEvent.id}
				</display:column>
				<display:column titleKey="label.date.valeur.import" sortable="true" sortProperty="dateEvenement">
					<unireg:regdate regdate="${importEvent.dateEvenement}"/>
				</display:column>
				<display:column titleKey="label.etat.import" sortable="true" sortProperty="etat">
					<fmt:message key="option.rf.etat.evenement.${importEvent.etat}" />
				</display:column>
				<display:column titleKey="label.message.erreur.import" >
					${importEvent.errorMessage}
				</display:column>
				<display:column titleKey="label.actions" class="action">
					<c:if test="${importEvent.etat == 'EN_ERREUR' || importEvent.etat == 'A_TRAITER'}">
						<unireg:buttonTo name="Relancer l'import" confirm="Voulez-vous vraiment relancer le traitement de l'import n°${importEvent.id} ?"
						                 action="/registrefoncier/import/restart.do" params="{importId:${importEvent.id}}"/>
						<unireg:buttonTo name="Forcer l'import" confirm="Voulez-vous vraiment forcer le traitement de l'import n°${importEvent.id} (y compris les mutations) ? \n\nEn forçant cet import, il sera marqué comme 'forcé' et les mutations non-traitées ne seront jamais intégrées dans Unireg."
						                 action="/registrefoncier/import/forcer.do" params="{importId:${importEvent.id}}"/>
					</c:if>
					<unireg:consulterLog entityNature="EvenementRFImport" entityId="${importEvent.id}"/>
				</display:column>
				<display:column titleKey="label.mutations.a.traiter">
					<span class="a-traiter"><img src="<c:url value="/images/loading.gif"/>" /></span>
				</display:column>
				<display:column titleKey="label.mutations.traitees">
					<span class="traitees"><img src="<c:url value="/images/loading.gif"/>" /></span>
				</display:column>
				<display:column titleKey="label.mutations.en.erreur">
					<span class="en-erreur"><img src="<c:url value="/images/loading.gif"/>" /></span>
				</display:column>
				<display:column titleKey="label.mutations.forcees">
					<span class="forcees"><img src="<c:url value="/images/loading.gif"/>" /></span>
				</display:column>
				<display:column titleKey="label.actions" class="action">
					<div class="actionTraitementMutations" style="display:none">
						<unireg:buttonTo name="Relancer le traitement" confirm="Voulez-vous vraiment relancer le traitement des mutations de l'import n°${importEvent.id} ?"
						                 action="/registrefoncier/mutation/restart.do" params="{importId:${importEvent.id}}"/>
						<unireg:buttonTo name="Forcer les mutations" confirm="Voulez-vous vraiment forcer les mutations non-traitées de l'import n°${importEvent.id} ? \n\nEn forçant ces mutations, elles seront marquées comme 'forcées' et ne seront jamais intégrées dans Unireg."
						                 action="/registrefoncier/mutation/forcer.do" params="{importId:${importEvent.id}}"/>
					</div>
				</display:column>
			</display:table>

			<script type="text/javascript">
				$(function() {
					var table = $('#importEvent');
					// on ajoute une ligne d'entête pour différencier la génération des mutations du traitement des mutations
					table.find('thead tr:first').before('<tr class="headerGroup"><th colspan="2" style="background: none"></th><th colspan="3">Génération des mutations</th><th colspan="5">Traitement des mutations</th></tr>');

					// on va chercher de manière asynchrone les statistiques sur chaque import (optimisation pour ne pas bloquer l'affichage lors de la récupération des stats)
					table.find('tbody').find('tr').each(function() {
						var tr = $(this);
						var importId = tr.find('.import-id').text();
						$.getJSON(App.curl("/registrefoncier/import/stats.do?importId=") + importId + "&" + new Date().getTime(), function(stats) {
							tr.find('span.a-traiter').text(stats.mutationsATraiter);
							tr.find('span.traitees').text(stats.mutationsTraitees);
							tr.find('span.en-erreur').text(stats.mutationsEnErreur);
							tr.find('span.forcees').text(stats.mutationsForcees);

							// on active les boutons de relance/forçage des mutations si nécessaire
							if (stats.mutationsATraiter > 0 || stats.mutationsEnErreur >  0) {
								tr.find('.actionTraitementMutations').show();
							}
						});
					});
				});
			</script>

		</form:form>
	</tiles:put>
</tiles:insert>
