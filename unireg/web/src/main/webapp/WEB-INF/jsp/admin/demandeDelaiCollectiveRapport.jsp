<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		<fmt:message key="title.demande.delai.collective.rapport"/>   	
	</tiles:put>
  	<tiles:put name="head">
	</tiles:put>
  	<tiles:put name="body">
  	 	<fieldset>
  	 	<legend><fmt:message key="label.demande.delai.collective.rapport.fieldset.legend"/></legend>
  	 			<fmt:message key="label.type.erreur.NO_ERROR"/> : ${rapport.okCount}
 	 		<display:table requestURI="/admin/demande-delai-collective-rapport.do" name="rapport.errors" id="error" pagesize="15" class="display" sort="list" export="true">
 	 			<display:setProperty name="export.csv" value="true"/>
 	 			<display:setProperty name="export.pdf" value="false"/>
 	 			<display:setProperty name="export.excel" value="false"/>
 	 			<display:setProperty name="export.csv.filename" value="rapportDemandeDelaiCollective.csv"/>
				<display:column sortable ="true" titleKey="label.numero.contribuable" property="numCtb">
				</display:column>
				<display:column sortable ="true" titleKey="label.type.erreur" >
					<fmt:message key="label.type.erreur.${error.errorType}"/>
				</display:column>
				
			</display:table>
		</fieldset>			
	</tiles:put>
</tiles:insert>