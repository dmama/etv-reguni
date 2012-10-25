<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut For -->

<fieldset><legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>

	<table border="0">
		<tr>
			<td>
				<a href="for.do?numero=<c:out value="${command.tiers.numero}"></c:out>&index=" class="add" title="Ajouter for">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
			</td>
			<c:if test="${command.forsPrincipalActif != null && command.allowedOnglet.FOR_PRINC}">
				<td>
					<a href="for.do?idFor=<c:out value="${command.forsPrincipalActif.id}"/>&index=modeimposition" class="add" title="Changer le mode d'imposition">&nbsp;<fmt:message key="label.changer.mode.imposition" /></a>
				</td>
			</c:if>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
			

</fieldset>
<!-- Fin For -->

