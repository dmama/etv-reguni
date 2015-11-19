<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
<tiles:put name="title"><fmt:message key="label.caracteristiques.evenement.organisation"/></tiles:put>
<tiles:put name="fichierAide">
	<li>
		<c:url var="doc" value="/docs/evenements.pdf"/>
		<a href="#" onClick="ouvrirAide('${doc}');" title="AccessKey: a" accesskey="e">Aide</a>
	</li>
</tiles:put>
<tiles:put name="body">

<unireg:nextRowClass reset="1"/>
<!-- Debut Caracteristiques generales -->
<fieldset>
    <legend><span><fmt:message key="label.caracteristiques.evenement.organisation"/></span></legend>
    <table>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.numero.evenement"/> :</td>
            <td width="25%">
                    ${command.evtId}
                <unireg:consulterLog entityNature="EvenementOrganisation" entityId="${command.evtId}"/>
            </td>
            <td width="25%"><fmt:message key="label.date.evenement"/> :</td>
            <td width="25%">
		            <unireg:regdate regdate="${command.evtDate}"/>
            </td>

        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.type.evenement"/> :</td>
            <td width="25%"><fmt:message key="${command.evtType}"/></td>
            <td width="25%"><fmt:message key="label.date.traitement"/> :</td>
            <td width="25%"><fmt:formatDate value="${command.evtDateTraitement}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"></td>
            <td width="25%"></td>
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
<%--
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
--%>
<!-- Fin visualisation grappe -->

<!-- Debut Organisation -->
<fieldset>
    <legend><span><fmt:message key="label.organisation"/></span></legend>
    <table>
        <tr class="<unireg:nextRowClass/>">
            <td width="50%"><fmt:message key="label.numero.registre.entreprises"/> :</td>
            <td width="50%">${command.noOrganisation}</td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="50%"><fmt:message key="label.raison.sociale"/> :</td>
	        <c:if test="${command.organisation == null}">
		        <td width="50%" class="error"><c:out value="${command.organisationError}"/></td>
	        </c:if>
	        <c:if test="${command.organisation != null}">
		        <td width="50%">${command.organisation.nom}</td>
	        </c:if>
        </tr>
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
            <td><fmt:message key="label.forme.juridique"/>&nbsp;:</td>
	        <c:if test="${command.organisation == null}">
		        <td class="error"><c:out value="${command.organisationError}"/></td>
	        </c:if>
	        <c:if test="${command.organisation != null}">
		        <td>${command.organisation.formeJuridique}</td>
	        </c:if>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td><fmt:message key="label.categorie"/>&nbsp;:</td>
	        <c:if test="${command.organisation == null}">
		        <td class="error"><c:out value="${command.organisationError}"/></td>
	        </c:if>
	        <c:if test="${command.organisation != null}">
		        <td>${command.organisation.categorie}</td>
	        </c:if>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td><fmt:message key="label.siege"/>&nbsp;:</td>
            <c:if test="${command.organisation == null}">
                <td class="error"><c:out value="${command.organisationError}"/></td>
            </c:if>
            <c:if test="${command.organisation != null}">
                <td><unireg:commune ofs="${command.organisation.autoriteFiscale}" displayProperty="nomOfficielAvecCanton"/></td>
            </c:if>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td><fmt:message key="label.numero.ide"/>&nbsp;:</td>
	        <c:if test="${command.organisation == null}">
		        <td class="error"><c:out value="${command.organisationError}"/></td>
	        </c:if>
	        <c:if test="${command.organisation != null}">
		        <td><unireg:numIDE numeroIDE="${command.organisation.numeroIDE}"/></td>
	        </c:if>
        </tr>
    </table>
</fieldset>
<!-- Fin Organisation -->

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
            <display:column titleKey="label.raison.sociale">
                ${tiersAssocie.nomCourrier1}
                <c:if test="${tiersAssocie.nomCourrier2 != null }">
                    <br>${tiersAssocie.nomCourrier2}
                </c:if>
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
<c:if test="${fn:length(command.nonTraitesSurMemeOrganisation) > 0}">
	<fieldset>
		<legend><span><fmt:message key="label.evenements.non.traites"/></span></legend>
		<display:table name="command.nonTraitesSurMemeOrganisation" id="aTraiter">
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
				<fmt:message key="${aTraiter.type}"/>
			</display:column>
			<display:column titleKey="label.etat.evenement">
				<fmt:message key="option.etat.evenement.${aTraiter.etat}"/>
			</display:column>
			<display:column titleKey="label.date.evenement">
				<c:if test="${aTraiter.date == aTraiter.date}">
					<unireg:regdate regdate="${aTraiter.date}"/>
				</c:if>
				<c:if test="${aTraiter.date != aTraiter.date}">
					<span title="<fmt:message key='label.modification.date.par.correction'/>">
						<unireg:regdate regdate="${aTraiter.date}"/>
						<img src="<c:url value='/images/right-arrow.png'/>" alt="<fmt:message key='label.modification.date.par.correction'/>" height="16px"/>
						<unireg:regdate regdate="${aTraiter.date}"/>
					</span>
				</c:if>
			</display:column>
		</display:table>
	</fieldset>
</c:if>
<!-- Fin de la liste des événements dans un état non final sur ce même individu -->

<!-- Debut Boutons -->
<input type="button" value="<fmt:message key='label.bouton.retour'/>" onClick="document.location='list.do';"/>

<c:if test="${command.recyclable}">
    <form:form method="post" action="recycler.do" style="display: inline">
        <input type="hidden" name="id" value="${command.evtId}"/>
        <fmt:message key="label.bouton.recycler" var="labelBoutonRecyler"/>
        <input type="submit" name="recycler" value="${labelBoutonRecyler}"/>
    </form:form>
</c:if>
<c:if test="${command.recyclable && command.evtEtat != 'TRAITE' && command.evtEtat != 'FORCE' && command.evtEtat != 'REDONDANT'}">
    <form:form method="post" action="forcer.do" style="display: inline">
        <input type="hidden" name="id" value="${command.evtId}"/>
        <fmt:message key="label.bouton.forcer" var="labelBoutonForcer"/>
        <input type="submit" name="forcer" value="${labelBoutonForcer}" onclick="return confirm('Voulez-vous réellement forcer l\'état de cet événement civil ?');"/>
    </form:form>
</c:if>
<!-- Fin Boutons -->

</tiles:put>
</tiles:insert>
