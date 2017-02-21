<%@ taglib prefix="util" uri="http://www.unireg.com/uniregTagLib" %>
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
		<fmt:message key="title.details.import.registre.foncier" />
	</tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

		<unireg:linkTo name="<< Retour à la liste des imports" action="/registrefoncier/import/list.do"/>

		<fieldset class="information">
			<legend><span><fmt:message key="title.details.import.rf" /></span></legend>

			<%--@elvariable id="importEvent" type="ch.vd.uniregctb.registrefoncier.EvenementRFImportView"--%>
			<table border="0">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.id.event.rf" />&nbsp;:</td>
					<td width="25%">${importEvent.id}</td>
					<td width="25%"><fmt:message key="label.mutations.a.traiter" />&nbsp;:</td>
					<td width="25%"><div class="a-traiter"><img src="<c:url value="/images/loading.gif"/>" /></div></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.date.valeur.import"/>&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${importEvent.dateEvenement}"/></td>
					<td width="25%"><fmt:message key="label.mutations.traitees" />&nbsp;:</td>
					<td width="25%"><div class="traitees"><img src="<c:url value="/images/loading.gif"/>" /></div></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.etat.import.rf" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.rf.etat.evenement.${importEvent.etat}" /></td>
					<td width="25%"><fmt:message key="label.mutations.en.erreur" />&nbsp;:</td>
					<td width="25%"><div class="en-erreur"><img src="<c:url value="/images/loading.gif"/>" /></div></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.message.erreur.import" />&nbsp;:</td>
					<td width="25%"><c:out value="${importEvent.errorMessage}"/></td>
					<td width="25%"><fmt:message key="label.mutations.forcees" />&nbsp;:</td>
					<td width="25%"><div class="forcees"><img src="<c:url value="/images/loading.gif"/>" /></div></td>
				</tr>
			</table>
		</fieldset>


		<form:form method="get" commandName="view" id="formDetailsImportRF" action="show.do">

			<input type="hidden" name="importId" value="${importEvent.id}"/>

			<script type="text/javascript">
				function submitForm(){
					$('#formDetailsImportRF').submit();
				}
			</script>

			<div class="actionTraitementMutations" style="display:none; float: right">
				<unireg:buttonTo name="Relancer le traitement" confirm="Voulez-vous vraiment relancer le traitement des mutations de l'import n°${importEvent.id} ?"
				                 action="/registrefoncier/mutation/restart.do" params="{importId:${importEvent.id}}"/>
				<unireg:buttonTo name="Forcer toutes les mutations" confirm="Voulez-vous vraiment forcer toutes les mutations non-traitées de l'import n°${importEvent.id} ? \n\nEn forçant ces mutations, elles seront marquées comme 'forcées' et ne seront jamais intégrées dans Unireg."
				                 action="/registrefoncier/mutation/forceAll.do" params="{importId:${importEvent.id}}"/>
			</div>

			<span class="checkboxes">
		        Etats : <form:checkbox path="aTraiter" id="aTraiter" label="A traiter" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="traite" id="traite" label="Traité" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="enErreur" id="enErreur" label="En erreur" onchange="submitForm()" onclick="submitForm()" />
		        <form:checkbox path="force" id="force" label="Forcé" onchange="submitForm()" onclick="submitForm()" />
		    </span>

			<%--@elvariable id="count" type="java.lang.Long"--%>
			<%--@elvariable id="mutation" type="ch.vd.uniregctb.registrefoncier.EvenementRFMutationView"--%>

			<display:table name="mutations" id="mutation" class="display_table" pagesize="50" size="${count}" sort="external" partialList="true"
			               requestURI="/registrefoncier/import/show.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucune.mutation.trouvee"/></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.une.mutation.trouvee"/></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mutations.trouves"/></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mutations.trouves"/></span></display:setProperty>

				<display:column titleKey="label.id.event.rf" sortable="true" sortProperty="id" class="import-id" >
					${mutation.id}
				</display:column>
				<display:column titleKey="label.etat.event.rf" sortable="true" sortProperty="etat">
					<fmt:message key="option.rf.etat.evenement.${mutation.etat}" />
				</display:column>
				<display:column titleKey="label.type.entite.rf" sortable="true" sortProperty="etat">
					<fmt:message key="option.rf.type.entite.${mutation.typeEntite}" />
				</display:column>
				<display:column titleKey="label.type.mutation.rf" sortable="true" sortProperty="etat">
					<fmt:message key="option.rf.type.mutation.${mutation.typeMutation}" />
				</display:column>
				<display:column titleKey="label.id.rf" sortable="true" sortProperty="etat">
					<authz:authorize ifAnyGranted="ROLE_SUPERGRA">
						<c:if test="${mutation.entityId != null}">
							<unireg:linkTo name="${mutation.idRF}" action="/supergra/entity/show.do" params="{id:${mutation.entityId},class:'ImmeubleRF'}"/>
						</c:if>
						<c:if test="${mutation.entityId == null}">
							${mutation.idRF}
						</c:if>
					</authz:authorize>
					<authz:authorize ifNotGranted="ROLE_SUPERGRA">
						${mutation.idRF}
					</authz:authorize>
				</display:column>
				<display:column titleKey="label.contenu.event.rf" >
					<unireg:limitedOut limit="200" value="${mutation.xmlContent}"/> <c:if test="${mutation.xmlContent != null}"><a href="#" onclick="showContenuXml(${mutation.id})">contenu complet</a></c:if>
				</display:column>
				<display:column titleKey="label.message.erreur.import" >
					${mutation.errorMessage} <c:if test="${mutation.callstack != null}"><a href="#" onclick="showCallstack(${mutation.id})">callstack</a></c:if>
				</display:column>
				<display:column titleKey="label.actions" class="action">
					<c:if test="${mutation.etat == 'EN_ERREUR'}">
						<unireg:buttonTo name="Forcer" confirm="Voulez-vous vraiment forcer la mutation n°${mutation.id} ? \n\nEn forçant cette mutation, elle sera marquée comme 'forcée' et ne sera jamais intégrée dans Unireg."
						                 action="/registrefoncier/mutation/force.do" params="{mutId:${mutation.id}}"/>
					</c:if>
					<unireg:consulterLog entityNature="EvenementRFMutation" entityId="${mutation.id}"/>
				</display:column>
			</display:table>

			<script type="text/javascript">

				function showContenuXml(mutationId) {
					// charge le contenu de la boîte de dialogue
					$.getJSON(App.curl('/registrefoncier/mutation/get.do?mutId=') + mutationId + '&' + new Date().getTime(), function (mutation) {
						var dialog = Dialog.create_dialog_div('show-xml-content');
						dialog.html('<pre>' + StringUtils.escapeHTML(mutation.xmlContent) + '</pre>');
						dialog.dialog({
							              title: 'Contenu XML',
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

				function showCallstack(mutationId) {
					// charge le contenu de la boîte de dialogue
					$.getJSON(App.curl('/registrefoncier/mutation/get.do?mutId=') + mutationId + '&' + new Date().getTime(), function (mutation) {
						var dialog = Dialog.create_dialog_div('show-callstack');
						dialog.html('<pre>' + mutation.callstack + '</pre>');
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

				$(function() {
					var importId = '${importEvent.id}';
					var importEtat = '${importEvent.etat}';
					$.getJSON(App.curl("/registrefoncier/import/stats.do?importId=") + importId + "&" + new Date().getTime(), function(stats) {
						$('div.a-traiter').text(stats.mutationsATraiter);
						$('div.traitees').text(stats.mutationsTraitees);
						$('div.en-erreur').text(stats.mutationsEnErreur);
						$('div.forcees').text(stats.mutationsForcees);

						// on active les boutons de relance/forçage des mutations si nécessaire
						if (importEtat != 'EN_TRAITEMENT' && (stats.mutationsATraiter > 0 || stats.mutationsEnErreur >  0)) {
							$('.actionTraitementMutations').show();
						}
					});
				});
			</script>

		</form:form>
	</tiles:put>
</tiles:insert>
