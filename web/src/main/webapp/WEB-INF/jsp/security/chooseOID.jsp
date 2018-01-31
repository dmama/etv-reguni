<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateLight.jsp">
	<tiles:put name="menu" type="String"></tiles:put>
	
	<tiles:put name="title" type="String">Veuillez sélectionner un OID de travail</tiles:put>
	<tiles:put name="body" type="String">
		<form:form method="post">
			<p/>
			<form:hidden path="initialUrl"/>
			<form:select path="selectedOID">
				<c:forEach items="${command.officesImpot}" var="oi">
					<c:choose>
						<c:when test="${oi.collectiviteParDefaut}">
							<option value="${oi.noColAdm}" selected="selected"><c:out value="${oi.nomCourt}"/></option>
						</c:when>
						<c:otherwise>
							<option value="${oi.noColAdm}"><c:out value="${oi.nomCourt}"/></option>
						</c:otherwise>
					</c:choose>
				</c:forEach>
			</form:select>

			<input type="submit" value="Choisir"/>
		</form:form>
	</tiles:put>
</tiles:insert>
