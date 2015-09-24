<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="label.jms.gestion" />
	</tiles:put>
	<tiles:put name="body">
		<!-- La liste des queues JMS -->
		<form:form method="post" id="formGestionQeues" action="jms.do">
			<fieldset>
				<legend><span><fmt:message key="label.jms.liste"/></span></legend>
				<display:table name="queues" id="queue" class="tableJob">
					<display:column title="Nom" style="width:20%;">${queue.nom}</display:column>
					<display:column title="Déscription" style="width:30%;">${queue.description}</display:column>
					<display:column title="Statut" style="width:5%;">
						<c:if test="${!queue.running}">
							<fmt:message key="label.jms.statut.arretee"/>
						</c:if>
						<c:if test="${queue.running}">
							<fmt:message key="label.jms.statut.demarree"/>
						</c:if>
					</display:column>
					<display:column title="Action" style="width:5%;">
						<c:if test="${!queue.running}">
							<unireg:raccourciDemarrer tooltip="Démarrer" onClick="javascript:gestionJMS.confirmeDemarrageQueue('${queue.identifiant}','${queue.nom}');" />
						</c:if>
						<c:if test="${queue.running}">
							<unireg:raccourciArreter tooltip="Arrêter" onClick="javascript:gestionJMS.confirmeArretQueue('${queue.identifiant}','${queue.nom}');"/>
						</c:if>
					</display:column>
					<display:column title="Nombre de messages consommés" style="width:10%;">${queue.nombreMessagesRecues}</display:column>
					<display:column title="Nombre de consommateurs" style="width:10%;">${queue.nombreConsommateurs}</display:column>
				</display:table>
			</fieldset>
		</form:form>
	</tiles:put>
</tiles:insert>
