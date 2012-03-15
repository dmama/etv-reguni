<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
<tiles:put name="title"><fmt:message key="label.caracteristiques.evenement.ech"/></tiles:put>
<tiles:put name="fichierAide">
    <c:url var="doc" value="/docs/evenements-ech.pdf"/>
    <a href="#" onClick="javascript:ouvrirAide('${doc}');" title="AccessKey: a" accesskey="e">Aide</a>
</tiles:put>
<tiles:put name="body">

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
            <td width="25%"><unireg:regdate regdate="${command.evtDate}"/></td>

        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.type.evenement"/> :</td>
            <td width="25%"><fmt:message key="option.type.evenement.ech.${command.evtType}"/></td>
            <td width="25%"><fmt:message key="label.date.traitement"/> :</td>
            <td width="25%"><fmt:formatDate value="${command.evtDateTraitement}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.etat.evenement"/> :</td>
            <td width="25%"><fmt:message key="option.etat.evenement.${command.evtEtat}"/></td>
            <td width="25%"><fmt:message key="label.commune.evenement"/> :</td>
            <td width="25%">&nbsp;</td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="25%"><fmt:message key="label.commentaire.traitement"/> :</td>
            <td colspan="3"><i><c:out value="${command.evtCommentaireTraitement}"/></i></td>
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

<!-- Debut Individu -->
<fieldset>
    <legend><span><fmt:message key="label.individu"/></span></legend>
    <table>
        <tr class="<unireg:nextRowClass/>">
            <td width="50%"><fmt:message key="label.numero.registre.habitant"/> :</td>
            <td width="50%">${command.individu.numeroIndividu}</td>
        </tr>
        <tr class="<unireg:nextRowClass/>">
            <td width="50%"><fmt:message key="label.nom.prenom"/> :</td>
            <td width="50%">${command.individu.nom}&nbsp;${command.individu.prenom}</td>
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
            <td><fmt:message key="label.date.naissance"/>&nbsp;:</td>
            <td><fmt:formatDate value="${command.individu.dateNaissance}" pattern="dd.MM.yyyy"/></td>
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
            <display:column titleKey="label.numero.tiers" href="../tiers/visu.do" paramId="id" paramProperty="numero">
                <unireg:numCTB numero="${tiersAssocie.numero}" link="true"/>
            </display:column>
            <display:column titleKey="label.prenom.nom">
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

<!-- Debut List evenement associes -->
<c:if test="${command.totalAutresEvenementsAssocies > 0}">
    <fieldset>
        <legend><span>Événements Associés</span></legend>

        <c:if test="${!command.recyclable}">
            <h4>
                Événement Prioritaire
            </h4>
            <table>
                <tr>
                    <th>Id</th>
                    <th>Type</th>
                    <th>Etat</th>
                    <th>Date</th>
                </tr>
                <tr>
                    <td>
                        <a href="visu.do?id=${command.evtPrioritaire.id}">${command.evtPrioritaire.id}</a>
                    </td>
                    <td>
                        <fmt:message key="option.type.evenement.ech.${command.evtPrioritaire.type}"/>
                    </td>
                    <td>
                        <fmt:message key="option.etat.evenement.${command.evtPrioritaire.etat}"/>
                    </td>
                    <td>
                        <unireg:regdate regdate="${command.evtPrioritaire.date}"/>
                    </td>
                <tr>
            </table>
        </c:if>
        <div>
        <c:if test="${command.totalAutresEvenementsAssocies > 1}">
            <a href='rechercher.do?numeroIndividuFormatte=${command.individu.numeroIndividu}&modeLotEvenement=true'>${command.totalAutresEvenementsAssocies} autres événements</a>
        </c:if>
        <c:if test="${command.totalAutresEvenementsAssocies == 1}">
            <a href='rechercher.do?numeroIndividuFormatte=${command.individu.numeroIndividu}&modeLotEvenement=true'>1 autre événement</a>
        </c:if>
         dans le lot associé à cet individu.
        </div>
    </fieldset>
</c:if>
<!-- Debut Boutons -->
<input type="button" value="<fmt:message key='label.bouton.retour'/>" onClick="document.location='list.do';"/>

<c:if test="${command.recyclable}">
    <form:form method="post" action="recycler.do" style="display: inline">
        <input type="hidden" name="id" value="${command.evtId}"/>
        <fmt:message key="label.bouton.recycler" var="labelBoutonRecyler"/>
        <input type="submit" name="recycler" value="${labelBoutonRecyler}"/>
    </form:form>
</c:if>
<c:if test="${command.evtEtat != 'TRAITE' && command.evtEtat != 'FORCE'}">
    <form:form method="post" action="forcer.do" style="display: inline">
        <input type="hidden" name="id" value="${command.evtId}"/>
        <fmt:message key="label.bouton.forcer" var="labelBoutonForcer"/>
        <input type="submit" name="forcer" value="${labelBoutonForcer}" onclick="return confirm('Voulez-vous réellement forcer l\'état de cet événement civil ?');"/>
    </form:form>
</c:if>
<!-- Fin Boutons -->

</tiles:put>
</tiles:insert>
