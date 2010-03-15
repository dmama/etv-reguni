<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="message" value="${status.value}"  scope="request"/>
</spring:bind>
<!-- Debut Caracteristiques identification -->
<fieldset class="information">
	<legend><span>
		<fmt:message key="caracteristiques.message.identification" />
	</span></legend>

	<table cellspacing="0" cellpadding="0" border="0">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.type.message" />&nbsp;:</td>
			<td>
				<c:if test="${message.typeMessage != null }">
					<fmt:message key="option.type.message.${message.typeMessage}" />
				</c:if>
			</td>
			<td><fmt:message key="label.navs13" />&nbsp;:</td>
			<td>${message.navs13}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
			<td>${message.periodeFiscale}</td>
			<td><fmt:message key="label.nom" />&nbsp;:</td>
			<td>${message.nom}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.emetteur" />&nbsp;:</td>
			<td>${message.emetteurId}</td>
			<td><fmt:message key="label.prenoms" />&nbsp;:</td>
			<td>${message.prenoms}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.date.message" />&nbsp;:</td>
			<td>
				<c:if test="${message.dateMessage != null }">
					<fmt:formatDate value="${message.dateMessage}" pattern="dd.MM.yyyy"/>
				</c:if>
			</td>
			<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
			<td>
				<c:if test="${message.dateNaissance != null }">
					<unireg:date date="${message.dateNaissance}" />
				</c:if>
			</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.etat.message" />&nbsp;:</td>
			<td>
				<c:if test="${message.etatMessage != null }">
					<fmt:message key="option.etat.message.${message.etatMessage}" />
				</c:if>
			</td>
			<td><fmt:message key="label.sexe" />&nbsp;:</td>
			<td>
				<c:if test="${message.sexe != null }">
					<fmt:message key="option.sexe.${message.sexe}" />
				</c:if>
			</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td></td>
			<td></td>
			<td></td>
			<td></td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td></td>
			<td></td>
			<td><fmt:message key="label.adresse" />&nbsp;:</td>
			<td>
				<c:if test="${message.rue != null }">
					${message.rue}
				</c:if>
			</td>
		</tr>
		<c:if test="${message.npa != null }">
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td></td>
				<td></td>
				<td></td>
				<td>${message.npa}</td>
			</tr>
		</c:if>
		<c:if test="${message.npaEtranger != null }">
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td></td>
				<td></td>
				<td></td>
				<td>${message.npaEtranger}</td>
			</tr>
		</c:if>
		<c:if test="${message.lieu != null }">
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td></td>
				<td></td>
				<td></td>
				<td>${message.lieu}</td>
			</tr>
		</c:if>
		<c:if test="${message.pays != null }">
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td></td>
				<td></td>
				<td></td>
				<td>${message.pays}</td>
			</tr>
		</c:if>
	</table>
	
</fieldset>
<!-- Fin Caracteristiques identification -->