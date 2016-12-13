<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.evenement.ech.view.EvenementEchDetailView"--%>

<unireg:nextRowClass reset="1"/>
<!-- Debut Caracteristiques generales -->
<fieldset>
    <legend><span><fmt:message key="label.caracteristiques.evenement.ech"/></span></legend>
    <table>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.numero.evenement"/> :</td>
            <td width="25%">
                    ${command.evtId}
                <unireg:consulterLog entityNature="EvenementEch" entityId="${command.evtId}"/>
            </td>
            <td width="25%"><fmt:message key="label.date.evenement"/> :</td>
            <td width="25%">
	            <c:if test="${command.grappeComplete.effectiveDate == null || command.grappeComplete.effectiveDate == command.evtDate}">
		            <unireg:regdate regdate="${command.evtDate}"/>
	            </c:if>
	            <c:if test="${command.grappeComplete.effectiveDate != null && command.grappeComplete.effectiveDate != command.evtDate}">
		            <span title="<fmt:message key='label.modification.date.par.correction'/>">
			            <unireg:regdate regdate="${command.evtDate}"/>
			            <img src="<c:url value='/images/right-arrow.png'/>" alt="<fmt:message key='label.modification.date.par.correction'/>" height="16px"/>
			            <unireg:regdate regdate="${command.grappeComplete.effectiveDate}"/>
				    </span>
	            </c:if>
            </td>

        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.type.evenement"/> :</td>
            <td width="25%"><fmt:message key="option.type.evenement.ech.${command.evtType}"/></td>
            <td width="25%"><fmt:message key="label.date.traitement"/> :</td>
            <td width="25%"><fmt:formatDate value="${command.evtDateTraitement}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.action.evenement"/> :</td>
            <td width="25%">
                <fmt:message key="option.action.evenement.ech.${command.evtAction}"/>
                <c:if test="${command.refEvtId != null}">
                    <a href="visu.do?id=<c:out value='${command.refEvtId}'/>" class="extlink">&nbsp;</a>
                </c:if>
            </td>
            <td width="25%"><fmt:message key="label.etat.evenement"/> :</td>
            <td width="25%"><fmt:message key="option.etat.evenement.${command.evtEtat}"/></td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.commentaire.traitement"/> :</td>
            <td colspan="3"><em><c:out value="${command.evtCommentaireTraitement}"/></em></td>
        </tr>
    </table>

    <c:if test="${not empty command.evtErreurs}">
        <display:table name="command.evtErreurs" id="row" class="error">
            <c:if test="${empty row.callstack}">
                <display:column property="message" titleKey="label.erreur"/>
            </c:if>
            <c:if test="${not empty row.callstack}">
                <display:column titleKey="label.erreur">
                    <unireg:callstack headerMessage="${row.message} " callstack="${row.callstack}"/>
                </display:column>
            </c:if>
        </display:table>
    </c:if>

</fieldset>
<!-- Fin Caracteristiques generales -->

<!-- Début visualisation grappe -->
<c:if test="${command.grappeComplete.multiElement}">
	<fieldset>
		<legend><span><fmt:message key="label.grappe"/></span></legend>
		<display:table name="command.grappeComplete" id="elt">
			<display:column style="width: 3em; text-align: center;">
				<c:if test="${elt.id == command.evtId}">
					<img src="<c:url value='/images/pin.png'/>"/>
				</c:if>
			</display:column>
			<display:column titleKey="label.numero.evenement">
				<c:if test="${elt.id == command.evtId}">
					<c:out value="${elt.id}"/>
				</c:if>
				<c:if test="${elt.id != command.evtId}">
					<a href="visu.do?id=<c:out value='${elt.id}'/>"><c:out value="${elt.id}"/></a>
				</c:if>
			</display:column>
			<display:column titleKey="label.action.evenement">
				<fmt:message key="option.action.evenement.ech.${elt.action}"/>
			</display:column>
			<display:column titleKey="label.etat.evenement">
				<fmt:message key="option.etat.evenement.${elt.etat}"/>
			</display:column>
			<display:column titleKey="label.date.evenement">
				<unireg:regdate regdate="${elt.date}"/>
			</display:column>
		</display:table>
	</fieldset>
</c:if>
<!-- Fin visualisation grappe -->

<!-- Debut Individu -->
<fieldset>
    <legend><span><fmt:message key="label.individu"/></span></legend>
    <table>
        <tr class="<unireg:nextRowClass/>">
            <td width="50%"><fmt:message key="label.numero.registre.habitant"/> :</td>
            <td width="50%">${command.noIndividu}</td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="50%"><fmt:message key="label.nom.prenom"/> :</td>
	        <c:if test="${command.individu == null}">
		        <td width="50%" class="error"><c:out value="${command.individuError}"/></td>
	        </c:if>
	        <c:if test="${command.individu != null}">
		        <td width="50%">${command.individu.nom}&nbsp;${command.individu.prenomUsuel}</td>
	        </c:if>
        </tr>
	    <c:if test="${command.individu != null}">
		    <tr class="<unireg:nextRowClass/>">
			    <td><fmt:message key="label.nouveau.numero.avs"/>&nbsp;:</td>
			    <td><unireg:numAVS numeroAssureSocial="${command.individu.numeroAssureSocial}"></unireg:numAVS></td>
		    </tr>
	    </c:if>
	    <c:if test="${command.individu != null}">
		    <tr class="<unireg:nextRowClass/>">
			    <td><fmt:message key="label.ancien.numero.avs"/>&nbsp;:</td>
			    <td><unireg:ancienNumeroAVS ancienNumeroAVS="${command.individu.ancienNumeroAVS}"></unireg:ancienNumeroAVS></td>
		    </tr>
	    </c:if>
        <c:if test="${command.adresse.ligne1 != null}">
            <tr class="<unireg:nextRowClass/>">
                <td><fmt:message key="label.adresse.courrier.active"/>&nbsp;:</td>
                <td>${command.adresse.ligne1}</td>
            </tr>
        </c:if>
        <c:if test="${command.adresse.ligne2 != null }">
            <tr class="<unireg:nextRowClass/>">
                <td>&nbsp;</td>
                <td>${command.adresse.ligne2}</td>
            </tr>
        </c:if>
        <c:if test="${command.adresse.ligne3 != null }">
            <tr class="<unireg:nextRowClass/>">
                <td>&nbsp;</td>
                <td>${command.adresse.ligne3}</td>
            </tr>
        </c:if>
        <c:if test="${command.adresse.ligne4 != null }">
            <tr class="<unireg:nextRowClass/>">
                <td>&nbsp;</td>
                <td>${command.adresse.ligne4}</td>
            </tr>
        </c:if>
        <c:if test="${command.adresse.ligne5 != null}">
            <tr class="<unireg:nextRowClass/>">
                <td>&nbsp;</td>
                <td>${command.adresse.ligne5}</td>
            </tr>
        </c:if>
        <c:if test="${command.adresse.ligne6 != null}">
            <tr class="<unireg:nextRowClass/>">
                <td>&nbsp;</td>
                <td>${command.adresse.ligne6}</td>
            </tr>
        </c:if>
        <tr class="<unireg:nextRowClass/>">
            <td><fmt:message key="label.date.naissance"/>&nbsp;:</td>
	        <c:if test="${command.individu == null}">
		        <td class="error"><c:out value="${command.individuError}"/></td>
	        </c:if>
	        <c:if test="${command.individu != null}">
		        <td><unireg:regdate regdate="${command.individu.dateNaissance}"/></td>
	        </c:if>
        </tr>
    </table>
</fieldset>
<!-- Fin Individu -->

<!-- Debut List tiers -->
<c:if test="${not empty command.tiersAssocies || not empty command.erreursTiersAssocies}">
    <fieldset>
        <legend><span><fmt:message key="label.tiers.associes"/></span></legend>
        <c:if test="${not empty command.erreursTiersAssocies}">
            <ul class="warnings iepngfix">
                <c:forEach items="${command.erreursTiersAssocies}" var="err">
                    <li class="warn"><c:out value="${err}"/></li>
                </c:forEach>
            </ul>
        </c:if>
        <display:table name="command.tiersAssocies" id="tiersAssocie" pagesize="25">
            <display:column titleKey="label.numero.tiers" href="../../tiers/visu.do" paramId="id" paramProperty="numero">
                <unireg:numCTB numero="${tiersAssocie.numero}"/>
            </display:column>
            <display:column titleKey="label.prenom.nom">
	            <unireg:multiline lines="${tiersAssocie.nomCourrier}"/>
            </display:column>
            <display:column property="localiteOuPays" titleKey="label.localitePays"/>
            <display:column property="forPrincipal" titleKey="label.for.principal"/>
            <display:column titleKey="label.date.ouverture.for">
                <unireg:regdate regdate="${tiersAssocie.dateOuvertureFor}"/>
            </display:column>
            <display:column titleKey="label.date.fermeture.for">
                <unireg:regdate regdate="${tiersAssocie.dateFermetureFor}"/>
            </display:column>
        </display:table>
    </fieldset>
</c:if>
<!-- Fin List tiers -->

<!-- Début de la liste des événements dans un état non final sur ce même individu -->
<c:if test="${fn:length(command.nonTraitesSurMemeIndividu) > 0}">
	<fieldset>
		<legend><span><fmt:message key="label.evenements.non.traites"/></span></legend>
		<display:table name="command.nonTraitesSurMemeIndividu" id="aTraiter">
			<display:column style="width: 3em; text-align: center;">
				<c:if test="${aTraiter.id == command.evtId}">
					<img src="<c:url value='/images/pin.png'/>"/>
				</c:if>
			</display:column>
			<display:column titleKey="label.numero.evenement">
				<c:if test="${aTraiter.id == command.evtId}">
					<c:out value="${aTraiter.id}"/>
				</c:if>
				<c:if test="${aTraiter.id != command.evtId}">
					<a href="visu.do?id=<c:out value='${aTraiter.id}'/>"><c:out value="${aTraiter.id}"/></a>
				</c:if>
			</display:column>
			<display:column titleKey="label.type.evenement">
				<fmt:message key="option.type.evenement.ech.${aTraiter.type}"/>
			</display:column>
			<display:column titleKey="label.action.evenement">
				<fmt:message key="option.action.evenement.ech.${aTraiter.action}"/>
			</display:column>
			<display:column titleKey="label.etat.evenement">
				<fmt:message key="option.etat.evenement.${aTraiter.etat}"/>
			</display:column>
			<display:column titleKey="label.date.evenement">
				<c:if test="${aTraiter.date == aTraiter.dateOriginale}">
					<unireg:regdate regdate="${aTraiter.dateOriginale}"/>
				</c:if>
				<c:if test="${aTraiter.date != aTraiter.dateOriginale}">
					<span title="<fmt:message key='label.modification.date.par.correction'/>">
						<unireg:regdate regdate="${aTraiter.dateOriginale}"/>
						<img src="<c:url value='/images/right-arrow.png'/>" alt="<fmt:message key='label.modification.date.par.correction'/>" height="16px"/>
						<unireg:regdate regdate="${aTraiter.date}"/>
					</span>
				</c:if>
			</display:column>
			<display:column titleKey="label.taille.grappe.traitement.associee">
				<c:out value="${fn:length(aTraiter.referrers) + 1}"/>
			</display:column>
		</display:table>
	</fieldset>
</c:if>
<!-- Fin de la liste des événements dans un état non final sur ce même individu -->

<!-- Debut Boutons -->
<c:choose>
	<c:when test="${!command.embedded}">
		<input type="button" value="<fmt:message key='label.bouton.retour'/>" onClick="document.location='list.do';"/>

		<c:if test="${command.recyclable}">
			<form:form method="post" action="recycler.do" style="display: inline">
				<input type="hidden" name="id" value="${command.evtId}"/>
				<fmt:message key="label.bouton.recycler" var="labelBoutonRecyler"/>
				<input type="submit" name="recycler" value="${labelBoutonRecyler}"/>
			</form:form>
		</c:if>
		<c:if test="${command.evtEtat != 'TRAITE' && command.evtEtat != 'FORCE' && command.evtEtat != 'REDONDANT'}">
			<form:form method="post" action="forcer.do" style="display: inline">
				<input type="hidden" name="id" value="${command.evtId}"/>
				<fmt:message key="label.bouton.forcer" var="labelBoutonForcer"/>
				<input type="submit" name="forcer" value="${labelBoutonForcer}" onclick="return confirm('Voulez-vous réellement forcer l\'état de cet événement civil ?');"/>
			</form:form>
		</c:if>
	</c:when>
	<c:otherwise>
		<c:if test="${command.recyclable}">
			<form:form method="post" action="#" style="display: inline">
				<fmt:message key="label.bouton.recycler" var="labelBoutonRecyler"/>
				<input type="submit" name="recycler" value="${labelBoutonRecyler}" onclick="EvtCivil.doRecycle(${command.evtId}); return false"/>
			</form:form>
		</c:if>
		<c:if test="${command.evtEtat != 'TRAITE' && command.evtEtat != 'FORCE' && command.evtEtat != 'REDONDANT'}">
			<form:form method="post" action="#" style="display: inline">
				<fmt:message key="label.bouton.forcer" var="labelBoutonForcer"/>
				<input type="submit" name="forcer" value="${labelBoutonForcer}"  onclick="EvtCivil.doForce(${command.evtId}); return false"/>
			</form:form>
		</c:if>
	</c:otherwise>
</c:choose>
<!-- Fin Boutons -->
