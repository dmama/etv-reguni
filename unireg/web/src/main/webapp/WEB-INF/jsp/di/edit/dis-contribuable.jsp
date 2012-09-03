<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Caracteristiques generales -->
<fieldset>
<FONT COLOR="#FF0000">${erreurCommunicationEditique}</FONT>
	<legend><span><fmt:message key="caracteristiques.di" /></span></legend>
		<c:if test="${command.allowedEmission}">
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="&nbsp;Ajouter" action="/decl/choisir.do" method="get" params="{tiersId:${command.contribuable.numero}}" title="Ajouter une déclaration" link_class="add noprint"/>
				</td>
			</tr>
		</table>
		</c:if>
	
	<jsp:include page="../../tiers/common/di/dis.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>	
<!-- Fin Caracteristiques generales -->
