<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.receptionner.full"/></tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />

        <display:table name="command.bordereaux" id="bordereau" pagesize="25" requestURI="/mouvement/receptionner-bordereaux.do" class="display_table" sort="list">

            <display:column sortable="true" titleKey="label.envoi.colladm.emettrice">
                <c:out value="${bordereau.nomCollAdmEmettrice}"/>
            </display:column>
            <c:if test="${command.montreDestinataire}">
                <display:column sortable="true" titleKey="label.envoi.colladm.destinataire">
                    <c:out value="${bordereau.nomCollAdmDestinataire}"/>
                </display:column>
            </c:if>
            <display:column titleKey="label.nombre.dossiers.envoyes">
                <c:out value="${bordereau.nbMouvementsEnvoyes}"/>
            </display:column>
            <display:column titleKey="label.nombre.dossiers.recus">
                <c:out value="${bordereau.nbMouvementsRecus}"/>
            </display:column>
            <display:column titleKey="label.nombre.dossiers.non.recus">
                <c:out value="${bordereau.nbMouvementsEnvoyes - bordereau.nbMouvementsRecus}"/>
            </display:column>

            <display:column style="action">
                <a href="detail-reception-bordereau.do?height=600&width=900&id=${bordereau.id}&TB_iframe=true&modal=true" class="detail thickbox" title="Détail d'un bordereau">&nbsp;</a>
            </display:column>

        </display:table>

		<script type="text/javascript">
			function AppSelect_OnChange(select) {
				var value = select.options[select.selectedIndex].value;
				if ( value && value !== '') {
					//window.open(value, '_blank') ;
					window.location.href = value;
				}
			}
		</script>

   </tiles:put>

</tiles:insert>