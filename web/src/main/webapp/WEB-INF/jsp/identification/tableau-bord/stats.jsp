<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.tableaubord.messages" /></tiles:put>
    <tiles:put name="head">
        <script type="text/javascript">
            $(function () {

                $('#calculer').click( function () {
                    $('#formRechercheMessage').attr('action','stats.do');
                })
            });
        </script>
    </tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheMessage" modelAttribute="statsCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche.messages"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp" />
			</fieldset>
			<fieldset class="info">
			<legend><span><fmt:message key="label.info.resultat" /></span></legend>
			<display:table name="statistiques" id="resultat" pagesize="25"  requestURI="/identification/tableau-bord/stats.do" class="display_table" sort="external">
				<display:column sortable ="false" titleKey="label.identification.etat.message">
					${resultat.etat}
				</display:column>
				<display:column sortable ="false" titleKey="label.identification.resultat">
					${resultat.resultatIdentification}
				</display:column>
				<display:column sortable ="false" titleKey="label.identification.nombre">				
				<c:choose>
					<c:when test="${resultat.etatTechnique == 'A_TRAITER_MANUELLEMENT' || resultat.etatTechnique == 'A_EXPERTISER'}">
                        <unireg:linkTo name="${resultat.nombre}" action="/identification/gestion-messages/listEnCoursFromStats.do"
                                       params="{etat:'${resultat.etatTechnique}',typeMessage:'${resultat.typeMessage}',periode:${resultat.periode}}" method="GET"/>
					</c:when>
                    <c:when test="${resultat.etatTechnique == 'A_EXPERTISER_SUSPENDU' || resultat.etatTechnique == 'A_TRAITER_MAN_SUSPENDU'}">
                        <unireg:linkTo name="${resultat.nombre}" action="/identification/gestion-messages/listSuspenduFromStats.do"
                                       params="{etat:'${resultat.etatTechnique}',typeMessage:'${resultat.typeMessage}',periode:${resultat.periode}}" method="GET"/>
                    </c:when>
					
					<c:when test="${resultat.etatTechnique == 'TRAITE_AUTOMATIQUEMENT' || resultat.etatTechnique == 'TRAITE_MANUELLEMENT' || resultat.etatTechnique == 'TRAITE_MAN_EXPERT' || resultat.etatTechnique == 'NON_IDENTIFIE'}">
                        <unireg:linkTo name="${resultat.nombre}" action="/identification/gestion-messages/listTraiteFromStats.do"
                                       params="{etat:'${resultat.etatTechnique}',typeMessage:'${resultat.typeMessage}',periode:${resultat.periode}}" method="GET"/>
					</c:when>
					<c:when test="${resultat.etatTechnique == 'EXCEPTION'}">
                        <unireg:linkTo name="${resultat.nombre}" action="/identification/gestion-messages/listEnCoursFromStats.do"
                                       params="{etat:'${resultat.etatTechnique}',typeMessage:'${resultat.typeMessage}',periode:${resultat.periode}}" method="GET"/>
					</c:when>
				</c:choose>
				</display:column>
			</display:table>
		</fieldset>
		</form:form>
		
	</tiles:put>
</tiles:insert>
