<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="tiersId" type="java.lang.Long"--%>
<%--@elvariable id="etiquettes" type="java.util.List<ch.vd.uniregctb.tiers.view.EtiquetteTiersView>"--%>

<unireg:setAuth var="autorisations" tiersId="${tiersId}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.etiquettes"/>
	</tiles:put>
	<tiles:put name="body">

		<unireg:bandeauTiers numero="${tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false"/>

		<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
		<fieldset>
			<legend><span><fmt:message key="label.etiquette"/></span></legend>

			<c:if test="${autorisations.etiquettes}">
				<table border="0">
					<tr>
						<td>
							<unireg:linkTo name="Ajouter" title="Ajouter une étiquette" action="/etiquette/add.do" params="{tiersId:${tiersId}}" link_class="add"/>
						</td>
					</tr>
				</table>
			</c:if>

			<c:if test="${not empty etiquettes}">
				<display:table name="${etiquettes}" id="etiq" requestURI="edit-list.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:column sortable="true" titleKey="label.libelle" sortProperty="libelle" style="width: 20%;">
						<c:out value="${etiq.libelle}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 15%;">
						<unireg:regdate regdate="${etiq.dateDebut}" format="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 15%;">
						<unireg:regdate regdate="${etiq.dateFin}" format="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="false" titleKey="label.commentaire">
						<c:out value="${etiq.commentaire}"/>
					</display:column>
					<display:column class="action" style="width: 10%;">
						<c:if test="${autorisations.etiquettes && !etiq.annule}">
							<unireg:linkTo name="" action="/etiquette/edit-item.do" method="GET" params="{idEtiquette:${etiq.id}}" link_class="edit" title="Modification de l'étiquetage"/>
							<unireg:linkTo name="" action="/etiquette/cancel.do" method="POST" params="{idEtiquette:${etiq.id}}" link_class="delete" title="Annulation de l'étiquetage" confirm="Voulez-vous vraiment annuler ce lien ?"/>
						</c:if>
					</display:column>
				</display:table>

			</c:if>

		</fieldset>
		<c:set var="libelleBoutonRetour">
			<fmt:message key="label.bouton.retour"/>
		</c:set>
		<unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${tiersId}}" name="${libelleBoutonRetour}"/>
	</tiles:put>
</tiles:insert>
