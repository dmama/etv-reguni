<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<!-- Debut Adresse -->
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
  		<fmt:message key="title.fermeture.adresse">
  			<fmt:param><unireg:numCTB numero="${view.numCTB}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">
		<form:form name="formAddAdresse" commandName="closeCommand">
		<fieldset>
			<legend><span><fmt:message key="label.adresse.fermeture" /></span></legend>
			<input type="hidden" name="idTiers" value="${view.numCTB}"/>
			<input type="hidden" name="usage" value="${view.usage}"/>
			<input type="hidden" name="dateDebut" value="<unireg:regdate regdate="${view.dateDebut}"/>"/>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.utilisationAdresse"/>&nbsp;:</td>
					<td><fmt:message key="option.usage.${view.usage}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >					
					<td><fmt:message key="label.adresse.complement"/>&nbsp;:</td>
					<td>${view.complements}</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.rueCasePostale"/>&nbsp;:</td>
					<td>
						${view.rue}
						<c:if test="${view.numeroMaison != null}">
							<c:out value="${view.numeroMaison}"/>
						</c:if>
					</td>
				</tr>
				<c:choose>
					<c:when test="${view.localite != null }">
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.localite"/>&nbsp;:</td>
							<td><c:out value="${view.localite}"/></td>
						</tr>
					</c:when>
					<c:when test="${view.localiteSuisse != null }">
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.localite"/>&nbsp;:</td>
							<td>${view.localiteSuisse}<td>
						</tr>
					</c:when>
				</c:choose>
				<c:if test="${view.paysOFS != null }">
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.pays"/>&nbsp;:</td>
						<td>
							<unireg:pays ofs="${view.paysOFS}" displayProperty="nomCourt" date="${view.dateDebut}"/>
						</td>
					</tr>
				</c:if>

				<c:if test="${view.source != null }">
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.adresse.source"/>&nbsp;:</td>
						<td><fmt:message key="option.source.${view.source}" /></td>
					</tr>
				</c:if>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.date.debut"/>&nbsp;:</td>
					<td><unireg:regdate regdate="${view.dateDebut}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td ><fmt:message key="label.date.fermeture" />&nbsp;:</td>
					<td>
					   <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						   <jsp:param  name="path" value="dateFin" />
						   <jsp:param name="id" value="dateFin" />
						   <jsp:param name="mandatory" value="true" />
					   </jsp:include>
					</td>
				</tr>
			</table>
		</fieldset>
		<table border="0">
			<tr>
				<td width="25%">&nbsp;</td>
				<td width="25%"><input type="submit" id="fermerAdresse" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
				<td width="25%">&nbsp;</td>
				<td width="25%"><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='edit.do?id=${view.numCTB}'" /></td>
			</tr>
		</table>
		</form:form>
	</tiles:put>
</tiles:insert>




<!-- Fin Adresse -->
