<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
<c:set var="showEditLink" value="${autorisations.donneesFiscales && empty param['message'] && empty param['retour']}" />
<c:set var="hasDroitGestionDecision" value="${autorisations.decisionsAci}" />
<c:set var="showTimelineLink" value="${false}" />
<c:set var="showMessagePresenceDecision" value="${command.decisionRecente && ! hasDroitGestionDecision}" />
<authz:authorize access="hasAnyRole('VISU_ALL', 'VISU_FORS')">
	<c:set var="showTimelineLink" value="${not empty command.forsFiscaux && command.natureTiers != 'DebiteurPrestationImposable'}" />
</authz:authorize>
<authz:authorize access="hasAnyRole('SUPERGRA')">
	<c:set var="showTimelineLink" value="${showTimelineLink || command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant'}"/>
</authz:authorize>

<!-- Debut Fiscal -->
<c:if test="${showEditLink || showTimelineLink || showMessagePresenceDecision}">
	<table border="0">
		<tr>
                <c:if test="${showEditLink}">
                    <td>
                        <c:choose>
                            <c:when test="${command.natureTiers == 'DebiteurPrestationImposable'}">
                                <unireg:raccourciModifier link="../debiteur/edit.do?id=${command.tiers.numero}" tooltip="Modifier les caractéristiques fiscales du débiteur"
                                                          display="label.bouton.modifier"/>
                            </c:when>
                            <c:otherwise>
                                <unireg:raccourciModifier link="../fiscal/edit.do?id=${command.tiers.numero}" tooltip="Modifier la partie fiscale" display="label.bouton.modifier"/>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:if>
                <c:if test="${showMessagePresenceDecision}">
	                <td>
		                <div class="flash" id="messageDecision">
			                <fmt:message key="label.presence.decision.aci"/>
		                </div>
	                </td>
            </c:if>
			<c:if test="${showTimelineLink}">
				<td id="timeline" align="right">
					<a href='<c:url value="/fors/timeline.do?id=" /><c:out value="${command.tiers.numero}" />'><fmt:message key="title.vue.chronologique"/></a>
				</td>
			</c:if>
		</tr>
	</table>
</c:if>

<c:choose>
	<c:when test="${command.natureTiers == 'DebiteurPrestationImposable'}">
		<jsp:include page="debiteur.jsp"/>
		<c:if test="${autorisations.donneesFiscales}">
			<table border="0" style="margin-top: 0.5em;">
				<tr><td>
					<unireg:raccourciModifier link="../fors/debiteur/list.do?id=${command.tiers.numero}" tooltip="Modifier les fors du débiteur" display="label.bouton.modifier"/>
				</td></tr>
			</table>
		</c:if>
		
		<jsp:include page="for-debiteur.jsp"/>
	</c:when>
	<c:otherwise>
		<authz:authorize access="hasAnyRole('VISU_ALL')">
			<jsp:include page="decision-aci.jsp"/>
		</authz:authorize>
		<jsp:include page="for.jsp"/>
		<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
		<authz:authorize access="hasAnyRole('VISU_ALL')">
			<jsp:include page="situation-famille.jsp"/>
		</authz:authorize>
	</c:otherwise>
</c:choose>

<!-- Fin Fiscal -->
