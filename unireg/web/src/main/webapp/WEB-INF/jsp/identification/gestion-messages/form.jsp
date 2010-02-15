<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<b><fmt:message key="label.message" /></b>
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<b><fmt:message key="label.personne" /></b>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.type.message" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="typeMessage">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${typesMessage}" />
			</form:select>	
		</td>
		<td width="25%">
			<fmt:message key="label.nom" />&nbsp;:
		</td>
		<td width="25%">
			<form:input  path="nom" id="nom" cssErrorClass="input-with-errors" />
			<form:errors path="nom" cssClass="error"/>
		</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.periode.fiscale" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="periodeFiscale">
				<form:option value="-1" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${periodesFiscales}" />
			</form:select>
		</td>
		<td width="25%">
			<fmt:message key="label.prenoms" />&nbsp;:
		</td>
		<td width="25%">
			<form:input  path="prenoms" id="prenoms" cssErrorClass="input-with-errors" />
			<form:errors path="prenoms" cssClass="error"/>
		</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.emetteur" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="emetteurId">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${emetteurs}" />
			</form:select>
		</td>
		<td width="25%">
			<fmt:message key="label.navs13" />&nbsp;:
		</td>
		<td width="25%">
			<form:input  path="NAVS13" id="NAVS13" cssErrorClass="input-with-errors" />
			<form:errors path="NAVS13" cssClass="error"/>
		</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.priorite" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="prioriteEmetteur">
				<form:option value="TOUS" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${priorites}" />
			</form:select>
		</td>
		<td width="25%">
			<fmt:message key="label.date.naissance" />&nbsp;:
		</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateNaissance" />
				<jsp:param name="id" value="dateNaissance" />
			</jsp:include>
		</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.date.message" />&nbsp;:
		</td>
		<td width="25%">
			<fmt:message key="label.date.message.debut" />&nbsp;
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateMessageDebut" />
				<jsp:param name="id" value="dateMessageDebut" />
			</jsp:include>&nbsp;
			<fmt:message key="label.date.message.fin" />&nbsp;
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateMessageFin" />
				<jsp:param name="id" value="dateMessageFin" />
			</jsp:include>
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>
	</tr>
	
		<c:choose>
			<c:when test="${messageEnCours}">
				<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_MW_IDENT_CTB_VISU">
		
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
								
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%">
						<fmt:message key="label.etat.message" />&nbsp;:
					</td>
					<td width="25%">
						<form:select path="etatMessage">
							<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
							<form:options items="${etatsMessage}" />
						</form:select>
					</td>
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
				</tr>	
		
				</authz:authorize>
			</c:when>		
		
			<c:otherwise>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
									
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td width="25%">
							<fmt:message key="label.etat.message" />&nbsp;:
						</td>
						<td width="25%">
							<form:select path="etatMessage">
								<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
								<form:options items="${etatsMessage}" />
							</form:select>
						</td>
						<td width="25%">&nbsp;</td>
						<td width="25%">&nbsp;</td>
					</tr>	
			</c:otherwise>
		</c:choose>
	
	
	
</table>
<!-- Debut Boutons -->
<table border="0">
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher"/></div>
		</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.effacer"/>" name="effacer" /></div>		
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->
