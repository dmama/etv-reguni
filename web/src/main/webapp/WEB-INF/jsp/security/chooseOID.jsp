<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateLight.jsp">
	<tiles:put name="title" type="String">Veuillez sélectionner un OID de travail</tiles:put>
	<tiles:put name="body" type="String">
		<form:form method="post">
			<p/>
			<form:hidden path="initialUrl"/>
			<%--@elvariable id="command" type="ch.vd.unireg.security.ChooseOIDView"--%>
			<form:select path="selectedOID" items="${command.officesImpot}" itemLabel="nomCourt" itemValue="noColAdm"/>
			<input type="submit" value="Choisir"/>
		</form:form>
	</tiles:put>
</tiles:insert>
