/* ==========================================================================
 *   1. Variables partagées et utilitaires
 *   2. Composants UI (SlimSelect, SunEditor, modales, fichiers)
 *   3. Calendrier (FullCalendar)
 *   4. Présence / QR code / SSE
 *   5. Statistiques
 *   6. ADE (jsTree, import d'événements, affinage)
 *   7. DataTables
 *   8. Initialisation par page (DOMContentLoaded)
 *   9. Écouteurs HTMX
 * ========================================================================== */


/* ==========================================================================
 * 1. Variables partagées et utilitaires
 * ========================================================================== */
var ID = function() {
    return '_' + Math.random().toString(36).substr(2, 9);
};
var allSelect = selectAll;
var suneditor0 = null;
var sortDate;
var editor1 = null; // SunEditor du PDF de convocation
var editor2 = null; // SunEditor du corps de l'e-mail
let slimSelectInstance = null;

//==jQuery remove()
function remove(id) {
    var elem = document.getElementById(id);
    if (elem != null) {
        return elem.parentNode.removeChild(elem);
    }
}

function backToTop() {
    if (scrollTop == 'true') {
        document.body.scrollTop = 0;
        document.documentElement.scrollTop = 0;
    }
}

function stripHTMLTagsUsingDOM(html) {
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;
    return tempDiv.textContent || tempDiv.innerText || "";
}

//Récupération des paramètres GET
function $_GET(param) {
    var vars = {};
    window.location.href.replace(location.hash, '').replace(
        /[?&]+([^=&]+)=?([^&]*)?/gi, // regexp
        function(m, key, value) { // callback
            vars[key] = value !== undefined ? value : '';
        }
    );

    if (param) {
        return vars[param] ? vars[param] : null;
    }
    return vars;
}

function deleteParam(urlLocation, name) {
    const url = new URL(urlLocation);
    url.searchParams.delete(name);
    return url.toString();
}

function confirmSubmission() {
    return confirm("Confirrmez-vous cette action?");
}

function updateHidden() {
    let isChecked = $('#includeLogo').is(':checked');
    $('#hiddenIncludeLogo').val(isChecked ? 'true' : 'false');
}

function copySelectedIds(targetFieldId, formId) {
    var selected = [];

    document.querySelectorAll("input[name='case']:checked").forEach(function(checkbox) {
        selected.push(checkbox.value);
    });

    // Remplit le champ caché du formulaire concerné
    document.getElementById(targetFieldId).value = selected.join(",");

    // Vérif pour éviter d'envoyer une sélection vide SI on a choisi "selected"
    var formElement = document.getElementById(formId);
    var checkedRadio = formElement.querySelector('input[name="population"]:checked');

    if (checkedRadio && checkedRadio.value === 'selected' && selected.length === 0) {
        alert("Aucun étudiant sélectionné ! Veuillez cocher des étudiants dans la liste.");
        event.preventDefault();
    }
}


/* ==========================================================================
 * 2. Composants UI
 * ========================================================================== */

// ---------- SlimSelect ----------
function initSlimSelects() {
    document.querySelectorAll('select.slimSelectClass').forEach(function(el) {
        if (!el.slimSelect) { // prevent double init
            el.slimSelect = new SlimSelect({
                select: el,
                placeholder: el.getAttribute('data-placeholder') || 'Choisir...'
            });
        }
    });
}

// Page d'extraction : (ré)initialise les SlimSelect, en détruisant l'ancienne instance si besoin
function initExtractionSelects() {
    if (document.getElementById('extractionPage') === null) return;
    const slimConfigs = [
        { elements: ['codCmp', 'sessionEpreuve', 'sessionEpreuveLdap', 'sessionEpreuveGroupe', 'sessionEpreuveCsv', 'groupe', 'sessionLocationCsv', 'sessionLocationLdap', 'sessionLocationGroupe', 'sessionLocation'] },
        { elements: ['codSes', 'codAnu'], options: { showSearch: false } },
        { elements: ['codEtp', 'codElp', 'codExtGpe'], options: { allowDeselect: true, enabled: true } }
    ];
    slimConfigs.forEach(config => {
        config.elements.forEach(id => {
            const element = document.getElementById(id);
            if (!element) return;
            if (element.slim) {
                element.slim.destroy();
            }
            const options = config.options || {};
            const slim = new SlimSelect({ select: `#${id}`, ...options });
            if (options.enabled) {
                slim.enable();
            }
            element.slim = slim;
        });
    });
}

function initPageSlimSelects() {
    initExtractionSelects();

    const slimSelectConfigs = [
        { id: 'suList', placeholder: 'Rechercher Utilisateur' },
        { id: 'gpId', placeholder: 'Rechercher groupe' },
        { id: 'sessionLocationExpected', placeholder: 'Rechercher Lieu' },
        { id: 'sessionLocationExpected2', placeholder: 'Rechercher Lieu' },
        { id: 'blackListGroupe', placeholder: 'Rechercher Groupe' },
        { id: 'motifAbsence' }
    ];
    slimSelectConfigs.forEach(config => {
        if (document.getElementById(config.id)) {
            new SlimSelect({
                select: `#${config.id}`,
                placeholder: config.placeholder
            });
        }
    });

    //Groupes
    if (document.getElementById('addMembersGroupe') != null) {
        ['sessionEpreuveGroupes', 'groupes', 'groupes2'].forEach(function(id) {
            if (document.getElementById(id) != null) {
                new SlimSelect({
                    select: '#' + id,
                    placeholder: '--Choisir--'
                });
            }
        });
    }
}

// ---------- SunEditor ----------
function createSunEditor(id) {
    const editor = SUNEDITOR.create(id, {
        // height/width of the editor
        width: '100%',
        height: '150px',

        // show/hide toolbar icons
        buttonList: [
            ['font', 'fontSize', 'formatBlock'],
            ['bold', 'underline', 'italic'],
            ['fontColor', 'hiliteColor'],
            ['align', 'horizontalRule', 'list', 'link', 'image'],
            ['codeView']
        ]
    });
    return editor;
}

function initConvocationEditors() {
    if (document.getElementById("pdfArea") != null) {
        editor1 = createSunEditor('pdfArea');
        editor2 = createSunEditor('emailArea');
    }
}

//Configs (AppliConfig)
function initConfigForm() {
    if (document.getElementById('valeur') == null) return;
    suneditor0 = createSunEditor('valeur');
    var appliConfig = document.getElementById('configForm');
    if (appliConfig != null) {
        appliConfig.addEventListener('submit', function(e) {
            e.preventDefault();
            var radioType = document.querySelectorAll('input[type=radio][name=type]:checked')[0].value;
            if (radioType == "HTML") {
                document.getElementById("valeur").value = suneditor0.getContents();
            }
            appliConfig.submit();
        });
    }
}

function initHelpForm() {
    var helpForm = document.getElementById('helpForm');
    if (helpForm != null) {
        var suneditor = createSunEditor('value');
        helpForm.addEventListener('submit', function(e) {
            e.preventDefault();
            document.getElementById("value").value = suneditor.getContents();
            helpForm.submit();
        });
    }
}

// Communication : synchronise les contenus PDF quand on coche "inclure le PDF"
function initCommunicationPdf() {
    var includePdf = document.getElementById("includePdf");
    if (includePdf) {
        includePdf.addEventListener("change", function() {
            var htmltemplate = document.getElementById("htmltemplate");
            htmltemplate.value = editor1.getContents();
            var htmltemplatePdf = document.getElementById("htmltemplatePdf");
            htmltemplatePdf.value = editor1.getContents();
        });
    }
}

// ---------- Plugins divers ----------
function initSelectCheckBoxes(id, searchplaceholder) {
    $('#' + id).searchableOptionList({
        maxHeight: '250px',
        showSelectAll: true,
        texts: {
            noItemsAvailable: 'Aucun résultat',
            selectAll: 'Tout sélectionner',
            selectNone: 'Tout effacer',
            searchplaceholder: searchplaceholder
        }
    });
}

function setupModal(modalId, fields) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.addEventListener('show.bs.modal', event => {
            const button = event.relatedTarget;
            fields.forEach(({ attr, elementId }) => {
                const value = button.getAttribute(attr);
                const element = document.getElementById(elementId);
                if (element) {
                    if (element.tagName === 'INPUT') {
                        element.value = value;
                    } else {
                        element.textContent = value;
                    }
                }
            });
        });
    }
}

function initBootstrapAndPlugins() {
    //Couleur motif absence
    $('.demo').minicolors({
        theme: 'bootstrap',  // Use Bootstrap theme
        position: 'bottom left',
        format: 'hex',
        opacity: false
    });

    //enable tooltips
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));

    $(".contextualHelp").on("mouseover", function(event) {
        $(this).tooltip("show");
        $(this).prop("title", "");
    });
}

function showInitialModals() {
    [
        ['#modalBilanCsvBody', '#modalBilanCsv'], //Messages modal bilan CSV
        ['#modalMsg', '#modalMsg'],
        ['#modalUser', '#modalUser']
    ].forEach(function(pair) {
        if (document.querySelector(pair[0]) != null) {
            $(pair[1]).modal('show');
        }
    });
}

// ---------- Fichiers (bootstrap-fileinput) ----------
function initializeFileInput(fileInput) {
    if (seId === "") {
        $("#files").fileinput({
            theme: "fas",
            language: "fr",
            maxFileSize: 1000,
            maxFileCount: 10,
            mainClass: "input-group-lg",
            dropZoneEnabled: false
        });
    } else {
        let rootUrlSe = `${emargementContextUrl}/supervisor/storedFile/`;
        let deleteUrl = `${emargementContextUrl}/supervisor/storedFile/delete`;

        fetch(`${rootUrlSe}${typePj}/${seId}`)
            .then(response => response.json())
            .then(data => {
                let urls = [];
                let configsArray = [];

                data.forEach(value => {
                    urls.push(`${rootUrlSe}${value.id}/photo`);
                    configsArray.push({
                        type: value.contentType.includes("pdf") ? "pdf" : "image",
                        filetype: value.contentType,
                        caption: value.filename,
                        downloadUrl: `${rootUrlSe}${value.id}/photo`,
                        size: value.fileSize,
                        key: value.id
                    });
                });

                $("#files").fileinput({
                    theme: "fas",
                    initialPreview: urls,
                    initialPreviewAsData: true,
                    initialPreviewConfig: configsArray,
                    deleteUrl: deleteUrl,
                    overwriteInitial: false,
                    language: "fr",
                    maxFileSize: 1000,
                    maxFileCount: 10,
                    mainClass: "input-group-lg",
                    allowedPreviewTypes: ['image', 'html', 'text', 'video', 'audio', 'flash', 'object', 'pdf']
                });
            })
            .catch(error => console.error("Error loading files:", error));
    }
    $("#files").on("filepredelete", function(event, key, jqXHR, data) {
        return !confirm("Êtes-vous sûr de vouloir supprimer ce fichier ?");
    });
}

// ---------- Cases à cocher ----------
// Sélection multiple avec champs cachés (checkedValues / checkedValues2)
function checkAll() {
    var all = document.getElementById('selectAllCheckbox');
    var hiddenInput = document.getElementById('checkedValues');
    var hiddenInput2 = document.getElementById('checkedValues2');

    // Helper function to update the hidden input with selected values
    function updateHiddenInput() {
        let checkedCheckboxes = document.querySelectorAll('.checkboxes:checked');
        let values = Array.from(checkedCheckboxes).map(checkbox => checkbox.value);
        hiddenInput.value = values.join(',');
        if (hiddenInput2) {
            hiddenInput2.value = values.join(',');
        }
    }

    if (all != null) {
        all.addEventListener('change', function() {
            let checkboxes = document.querySelectorAll('.checkboxes');
            checkboxes.forEach(function(checkbox) {
                checkbox.checked = all.checked;
            });
            updateHiddenInput();
        });
    }

    // Add event listeners to individual checkboxes
    let checkboxes = document.querySelectorAll('.checkboxes');
    checkboxes.forEach(function(checkbox) {
        checkbox.addEventListener('change', function() {
            updateHiddenInput();
        });
    });
}

// Sélection des cases ".case" (liste d'identifiants dans #listeIds)
function initCaseSelection() {
    var idArray = [];
    var checkboxes = document.querySelectorAll('.case');
    var selectAll = document.getElementById("selectall");
    var listeIds = document.getElementById("listeIds");
    if (selectAll == null) return;

    selectAll.addEventListener("click", function() {
        idArray = [];
        for (var i = 0, n = checkboxes.length; i < n; i++) {
            if (!checkboxes[i].disabled) {
                checkboxes[i].checked = this.checked;
            }
        }
        idArray = Array.from(document.querySelectorAll("input[name='case']")).filter(function(checkbox) {
            return checkbox.checked;
        }).map(function(checkbox) {
            return checkbox.value;
        });
        if (listeIds != null) {
            listeIds.value = idArray;
        }
    });

    Array.from(checkboxes).forEach(function(link) {
        return link.addEventListener('click', function(event) {
            if (idArray.indexOf(this.value) < 0) {
                idArray.push(this.value);
            } else {
                idArray.splice(idArray.indexOf(this.value), 1);
            }
            if (!this.checked) {
                selectAll.checked = false;
            }
            if (listeIds != null) {
                listeIds.value = idArray;
            }
        });
    });
}

// "Tout sélectionner" des éléments non utilisés
function initNotUsedSelection() {
    $("#selectAll").click(function() {
        $('.notUsed').find("input[type=checkbox]").prop('checked', $(this).prop('checked'));
    });

    $('.notUsed').find("input[type=checkbox]").click(function() {
        if (!$(this).prop("checked")) {
            $("#selectAll").prop("checked", false);
        }
    });
}

// ---------- Clics délégués (présence, convocations) ----------
function initDelegatedClicks() {
    document.addEventListener('click', function(event) {
        if (event.target.matches('.presenceCheck')) {
            var isPresent = event.target.checked;
            var isPresentValue = isPresent + ',' + event.target.value;
            updatePresence(emargementContextUrl + "/updatePresents", isPresentValue, null);
        }
        if (event.target.matches('.presenceChecker')) {
            var isPresent = event.target.checked;
            var isPresentValue = isPresent + ',' + event.target.value;
            updatePresence(emargementContextUrl + "/updatePresentsTagChecker", isPresentValue, null);
        }
        if (event.target.matches('#pdfPreview')) {
            var htmltemplate = document.getElementById("htmltemplate");
            htmltemplate.value = editor1.getContents();
            var formPdfConvoc = document.getElementById("formPdfConvoc");
            formPdfConvoc.submit();
        }
        if (event.target.matches('#submitEmailConvocation')) {
            event.preventDefault();
            fillMailFields();
            var sendConvocation = document.getElementById("sendConvocation");
            var subject = document.getElementById("subject");
            var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
            if (subject.value.length == 0) {
                alert("Vous devez rentrer un sujet");
            } else if (isPreviewPdfOk != null && !isPreviewPdfOk.checked) {
                alert("Vous devez valider le PDF de convocation");
            } else if (sendConvocation != null) {
                sendConvocation.submit();
            } else if (document.getElementById("sendCommunication") != null) {
                document.getElementById("sendCommunication").submit();
            }
        }
        if (event.target.matches('#submitEmailConvsignes')) {
            event.preventDefault();
            fillMailFields();
            var sendConsignes = document.getElementById("sendConsignes");
            var subject = document.getElementById("subject");
            var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
            if (subject.value.length == 0) {
                alert("Vous devez rentrer un sujet");
            } else if (!isPreviewPdfOk.checked) {
                alert("Vous devez valider le PDF de consignes");
            } else {
                sendConsignes.submit();
            }
        }
    }, false);
}

// Recopie le contenu des éditeurs dans les champs cachés du formulaire d'e-mail
function fillMailFields() {
    document.getElementById("bodyMsg").value = editor2.getContents();
    document.getElementById("htmltemplatePdf").value = editor1.getContents();
}

// ---------- Formulaires d'import (export inscrits, LDAP, groupe, CSV) ----------
function initImportForms() {
    var submitExport = document.getElementById('submitExport');
    var importBtn;
    var nbSelected;

    if (submitExport) {
        importBtn = document.getElementById('import');
        $(".statusExport").hide();
        importBtn.addEventListener('click', function(e) {
            var sessionEpreuve = document.getElementById('sessionEpreuve');
            if (sessionEpreuve.value == "") {
                e.preventDefault();
                alert("Vous devez choisir une Session!");
            } else {
                $(".statusExport").show();
                submitExport.submit();
            }
        });
    }

    var importUsersLdapform = document.getElementById('importUsersLdapform');
    if (importUsersLdapform != null) {
        importBtn = document.getElementById('importLdap');
        nbSelected = $("#usersGroupLdap option:selected").length;
        $(".statusExportLdap").hide();
        importBtn.addEventListener('click', function(e) {
            var importAgents = $("#importTagchecker").prop('checked') ? true : false;
            var sessionEpreuveLdap = document.getElementById('sessionEpreuveLdap');
            if (sessionEpreuveLdap.value == "" && !importAgents) {
                e.preventDefault();
                alert("Vous devez choisr une Session!");
            } else {
                if (nbSelected > 0) {
                    $(".statusExportLdap").show();
                    importUsersLdapform.submit();
                }
            }
        });
    }

    var importGroupe = document.getElementById('importGroupe');
    $(".statusExportGroupe").hide();
    if (importGroupe != null) {
        importGroupe.addEventListener('click', function(e) {
            var sessionEpreuve = document.getElementById('sessionEpreuveGroupe');
            if (sessionEpreuve.value == "") {
                e.preventDefault();
                alert("Vous devez choisr une Session!");
            } else {
                $(".statusExportGroupe").show();
                submitExport.submit();
            }
        });
    }

    var importCsv = document.getElementById('importCsv');
    $(".statusExportCsv").hide();
    if (importCsv != null) {
        importCsv.addEventListener('click', function(e) {
            var importAgents = $("#importTagchecker").prop('checked') ? true : false;
            var sessionEpreuve = document.getElementById('sessionEpreuveCsv');
            if (sessionEpreuve.value == "" && !importAgents) {
                e.preventDefault();
                alert("Vous devez choisr une Session!");
            } else {
                $(".statusExportCsv").show();
                submitExport.submit();
            }
        });
    }

    $('#selectAll').on("click", function() {
        var selectedItems = 0;
        var nbSelected = $("#usersGroupLdap option:selected").length;
        $('#usersGroupLdap option').each(function() {
            if (!this.disabled && this.value != '') {
                $(this).prop('selected', (nbSelected == 0) ? true : false);
                selectedItems = $(this).prop('selected') ? $("#ldapMembersSize").text() : 0;
            }
        });
        $("#selectedUsers").text(selectedItems);
    });

    $('#usersGroupLdap option').on("click", function() {
        nbSelected = $("#usersGroupLdap option:selected").length;
        $("#selectedUsers").text(nbSelected);
    });
}

// ---------- Navigation (selects qui rechargent la page) ----------
function initNavigationSelects() {
    //select context
    var selectContext = document.getElementById('selectContext');
    if (selectContext != null) {
        selectContext.addEventListener("change", function() {
            var text = $(this).find("option:selected").text();
            if (text == "Ordre") {
                window.location.href = this.value;
            } else {
                this.form.submit();
            }
        });
    }

    //Changement année univ
    $("#anneeUnivSelect").on("change", function(e) {
        var annee = this.value;
        window.location.href = window.location.pathname + "?anneeUniv=" + annee;
    });

    //ADE : changement de projet
    [['projetParam', 'params'], ['projetTasks', 'tasks'], ['projetSalles', 'salles']].forEach(function(pair) {
        $("#" + pair[0]).on("change", function(event) {
            window.location.href = emargementContextUrl + "/manager/adeCampus/" + pair[1] + "?idProjet=" + this.value;
        });
    });

    $("#clearFilters").on("click", function(event) {
        window.location.href = window.location.origin + window.location.pathname;
    });
}

// ---------- Pagination : option "Tous" ----------
function initPaginationAll() {
    if (document.getElementById('pagination') == null) return;

    if (allSelect != null && allSelect > 10) {
        var loc = window.location;
        var dataUrl = loc.pathname;
        var allSelectUrl = "";
        if (loc.search == "") {
            allSelectUrl = dataUrl + "?size=" + allSelect;
        } else {
            var url = new URL(loc);
            var search_params = new URLSearchParams(url.search);
            search_params.set('size', allSelect);
            url.search = search_params;
            allSelectUrl = url.pathname + url.search;
        }
        $("#pagination select").prepend("<option value='" + allSelect + "' data-page-size-url='" + allSelectUrl + "'>Tous</option>");
    }
    $("#pagination option:selected").removeProp("selected");
    var sizeSelected = ($_GET("size") != null) ? $_GET("size") : allSelect;
    $("#pagination option[value='" + sizeSelected + "']").attr('selected', 'selected');
}

// ---------- Procuration ----------
function initProxy() {
    $(document).on('click', '.proxyPersonCheck', function(e) {
        e.preventDefault();
        var splitProxy = null;

        if (typeof $(this).attr("data-whatever") == "undefined") {
            splitProxy = this.value.split(",");
            $("#substituteId option[value='']").prop("selected", "selected");
        } else {
            splitProxy = $(this).attr("data-whatever").split(",");
            $("#substituteId option[value='" + splitProxy[2] + "']").prop("selected", "selected");
        }

        $("#procurationPerson").text(splitProxy[1]);
        $("#tcId").val(splitProxy[0]);

        const offcanvasElementList = document.getElementById('offcanvasExample');
        const offcanvasList = new bootstrap.Offcanvas(offcanvasElementList);
        offcanvasList.show();
    });

    if (document.getElementById('substituteId') != null) {
        new SlimSelect({
            select: '#substituteId'
        });
    }
}

// ---------- Suppression d'un tagCheck ----------
function initDeleteTagCheck() {
    $(document).on("click", ".btn-delete", function(e) {
        e.preventDefault();
        if (!confirm("Confirmez-vous la suppression ?")) {
            return;
        }
        const id = $(this).closest("tr").attr("id");
        $.ajax({
            url: emargementContextUrl + "/supervisor/tagCheck/" + id,
            type: "POST",
            success: function() {
                $("#" + id).remove();
            },
            error: function() {
                alert("Erreur lors de la suppression");
            }
        });
    });
}

// ---------- Sessions : duplication (flatpickr multi-dates) ----------
function initDuplicateSessions() {
    const inputVisible = document.getElementById("datepicker");
    if (inputVisible == null) return;

    const badgesContainer = document.getElementById("badgesContainer");

    // Met à jour les badges visibles avec un format lisible
    function updateDisplay(selectedDates) {
        badgesContainer.innerHTML = "";
        selectedDates.forEach(date => {
            const options = { weekday: 'short', year: 'numeric', month: 'long', day: 'numeric' };
            const literal = date.toLocaleDateString('fr-FR', options);
            const iso = date.toISOString().split('T')[0];

            const badge = document.createElement('span');
            badge.className = "badge bg-primary me-2 mb-2 badge-date";
            badge.innerHTML = `
                ${literal}
                <button type="button" class="btn-close btn-close-white btn-sm ms-2" aria-label="Supprimer" data-date="${iso}"></button>
            `;
            badgesContainer.appendChild(badge);
        });
    }

    const fp = flatpickr(inputVisible, {
        mode: "multiple",
        dateFormat: "Y-m-d", // format technique (pour le hidden)
        locale: "fr",
        altInput: false, // désactivé car on veut gérer nous-mêmes l'affichage
        onChange: updateDisplay
    });

    // Suppression d'une date via le badge
    badgesContainer.addEventListener("click", (e) => {
        if (e.target.classList.contains("btn-close")) {
            const dateToRemove = e.target.getAttribute("data-date");
            const newDates = fp.selectedDates.filter(d => d.toISOString().split('T')[0] !== dateToRemove);
            fp.setDate(newDates);
            updateDisplay(newDates);
        }
    });
}

// ---------- Liste de sessions : élément actif ----------
function initSessionListActive() {
    document.addEventListener('click', function(e) {
        const item = e.target.closest('.session-item');
        if (!item) return;
        document.querySelectorAll('.session-item')
            .forEach(el => el.classList.remove('active'));
        item.classList.add('active');
    });
}

// ---------- Autocomplete (Awesomplete + HTMX) ----------
function initAutocomplete() {
    if (!document.querySelector('#suPage, #presencePage, #userAppPage, #locationPage, #sessionEpreuvePage, #recherchePage, #addMembersPage, #extractionPage, #createSuperAdminPage, #createTagCheckPage, #createUserApp, #createAbsence, #assiduitePage, #tagChecksListPage')) {
        return;
    }

    var searchBoxes = document.querySelectorAll(".searchBox"); // Get all search inputs
    var formSearch = document.getElementById("formSearch");
    searchBoxes.forEach(function(searchBox) {
        var awesomplete = new Awesomplete(searchBox, { minChars: 3, maxItems: 100, autoFirst: true });

        searchBox.addEventListener("focus", function() {
            this.value = "";
        });

        document.body.addEventListener("htmx:afterSwap", function(event) {
            var searchResults = document.querySelector(`#${searchBox.getAttribute("hx-target").substring(1)}`);
            if (searchResults && event.detail.target === searchResults) {
                var items = Array.from(searchResults.getElementsByClassName("result-item"));
                var suggestions = items.map(item => ({
                    label: item.innerHTML,
                    value: item.dataset.value
                }));
                awesomplete.list = suggestions;
                searchResults.hidden = true;
            }
        });

        searchBox.addEventListener("awesomplete-selectcomplete", function(event) {
            var dataValue = event.text.value.trim();
            var selectedValue = dataValue.split("//")[0].trim();
            this.value = selectedValue;

            // Get the closest hidden input related to this search box
            var typeSearch = document.getElementById("typeSearch");
            if (typeSearch) {
                typeSearch.value = this.getAttribute("data-typeSearch") || "";
            }
            searchBoxes.forEach(input => {
                if (input !== searchBox) {
                    input.disabled = true;
                }
            });
            var splitResult = dataValue.split("//");
            if (document.querySelector("#createSuperAdminPage, #createTagCheckPage, #createUserApp, #createAbsence, #addMembersPage")) {
                this.value = splitResult[0].toString().trim();
                if (document.querySelector("#createTagCheckPage, #createUserApp, #createAbsence")) {
                    $("#eppn").val(splitResult[0].toString().trim());
                }
                if (document.getElementById("resultEppn")) {
                    $("#resultEppn").html(splitResult[1] + " " + splitResult[2]);
                }
                if (document.getElementById("tcIdentity")) {
                    $("#tcIdentity").html(splitResult[2].toString().trim() + ' ' + splitResult[1].toString().trim());
                }
            } else {
                if (document.getElementById("searchField")) {
                    var split3 = (splitResult[3] == undefined) ? '' : " // " + splitResult[3];
                    this.value = splitResult[1] + " " + splitResult[2] + split3;
                    $("#searchField").val(splitResult[1] + " " + splitResult[2] + split3);
                    $("#searchValue").val(splitResult[0]);
                } else if (document.getElementById("tagChecksListPage")) {
                    $("#eppn").val(splitResult[0].toString().trim());
                }
                formSearch.submit();
            }
        });
    });
}

function initCreateTagCheckForm() {
    var createTagCheckForm = document.getElementById("createTagCheckForm");
    if (createTagCheckForm != null) {
        createTagCheckForm.addEventListener("submit", function(e) {
            const eppn = document.getElementById("searchString").value.trim();
            const numIdentifiant = document.getElementById("numIdentifiant").value.trim();

            if (!eppn && !numIdentifiant) {
                e.preventDefault();
                alert("Veuillez renseigner soit l'Eppn, soit le N° identifiant.");
            }
        });
    }
}


/* ==========================================================================
 * 3. Calendrier (FullCalendar)
 * ========================================================================== */
function getCalendar(calendarEl, urlEvents, editable, initialView, useModal = false, initialDate = null) {
    const isMobile = () => window.innerWidth < 768;
    const fmtDate = d => d.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    const fmtTime = d => d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

    const VIEWS = {
        dayGridMonth: ['fa-calendar', 'Mois'],
        timeGridWeek: ['fa-calendar-week', 'Semaine'],
        timeGridDay:  ['fa-calendar-day', 'Jour'],
        listMonth:    ['fa-list', 'Planning']
    };
    const headerFor = () => ({
        start: 'prev,next today', center: 'title',
        end: isMobile() ? '' : Object.keys(VIEWS).join(',')
    });

    // ---------- QR code du modal ----------
    let calendarQrInterval = null;
    const stopCalendarQr = () => { clearInterval(calendarQrInterval); calendarQrInterval = null; };
    document.getElementById('eventModal')?.addEventListener('hidden.bs.modal', stopCalendarQr);

    // ---------- Modal de détail ----------
    function showEventModal(event) {
        const p = event.extendedProps;
        document.getElementById('eventModalTitle').textContent = event.title;

        let horaire = fmtDate(event.start);
        horaire += event.allDay
            ? ' · Journée'
            : ' · ' + fmtTime(event.start) + (event.end ? ' – ' + fmtTime(event.end) : '');

        const rows = [
            ['Horaire', horaire],
            ['Site', p.site],
            ['Salle', p.salle],
            ['Enseignant(s)', p.professeur]
        ].filter(r => r[1]);

        const body = document.getElementById('eventModalBody');
        body.replaceChildren();
        rows.forEach(([label, value]) => {
            const dt = document.createElement('dt');
            dt.className = 'col-4 text-muted fw-normal';
            dt.textContent = label;
            const dd2 = document.createElement('dd');
            dd2.className = 'col-8 fw-semibold';
            dd2.textContent = value;
            body.append(dt, dd2);
        });

        const qr = document.getElementById('eventModalQr');
        const img = document.getElementById('eventModalQrImg');
        stopCalendarQr();
        img.removeAttribute('src');
        qr.classList.toggle('d-none', !p.qrCodeAvailable);
        if (p.qrCodeAvailable) {
            const url = emargementContextUrl + '/user/qrCode/'
                + encodeURIComponent(p.qrEppn) + '/' + encodeURIComponent(p.qrSession);
            const load = () => getQrCodeSession(url, 'eventModalQrImg');
            load();
            calendarQrInterval = setInterval(load, qrcodeChange);
        }
        bootstrap.Modal.getOrCreateInstance(document.getElementById('eventModal')).show();
    }

    // ---------- Dropdown de vues (mobile uniquement) ----------
    const dd = document.createElement('div');
    dd.className = 'dropdown d-md-none position-absolute top-0 end-0';
    dd.innerHTML = `<button class="btn btn-outline-primary dropdown-toggle" data-bs-toggle="dropdown"></button>
        <ul class="dropdown-menu dropdown-menu-end">${Object.entries(VIEWS).map(([v, [i, l]]) =>
            `<li><button class="dropdown-item" data-view="${v}"><i class="fa-solid ${i} me-2"></i>${l}</button></li>`).join('')}</ul>`;
    dd.addEventListener('click', e => {
        const b = e.target.closest('[data-view]');
        if (b) calendar.changeView(b.dataset.view);
    });

    // Wrapper créé une seule fois ; si getCalendar est rappelé (changement du select), on nettoie l'ancien
    if (calendarEl._fc) {
        calendarEl._fc.destroy();
        calendarEl._dd.remove();
    } else {
        const wrap = document.createElement('div');
        wrap.className = 'position-relative';
        calendarEl.before(wrap);
        wrap.append(calendarEl);
    }
    calendarEl.parentElement.append(dd);

    // ---------- Calendrier ----------
    let wasMobile = isMobile();
    const calendar = new FullCalendar.Calendar(calendarEl, {
        height: 'auto',
        headerToolbar: headerFor(),
        buttonText: { today: 'Auj.', dayGridMonth: 'Mois', timeGridWeek: 'Semaine', timeGridDay: 'Jour', listMonth: 'Planning' },
        navLinks: true,
        editable: editable,
        allDayText: 'Journée',
        locale: 'fr',
        events: urlEvents,
        initialView: initialView || (isMobile() ? 'timeGridDay' : 'dayGridMonth'),
        initialDate: initialDate || undefined,
        themeSystem: 'bootstrap5',
        slotMinTime: '07:00:00',
        slotMaxTime: '21:00:00',
        scrollTime: '08:00:00',

        eventDataTransform: e => { if (useModal) delete e.url; return e; },

        datesSet: info => {
            const [i, l] = VIEWS[info.view.type] || VIEWS.dayGridMonth;
            dd.querySelector('.dropdown-toggle').innerHTML = `<i class="fa-solid ${i} me-2"></i>${l}`;
        },

        // Ne réagit qu'au passage du seuil mobile/desktop, et garde la date affichée
        windowResize: () => {
            if (isMobile() === wasMobile) return;
            wasMobile = isMobile();
            const d = calendar.getDate();
            calendar.setOption('headerToolbar', headerFor());
            calendar.changeView(calendar.view.type, d);
        },

        eventClick: info => {
            if (!useModal) return;
            info.jsEvent.preventDefault();
            showEventModal(info.event);
        },
        eventContent: arg => {
            const view = arg.view.type;

            // Vue mois et planning : rendu natif FullCalendar
            if (view === 'dayGridMonth' || view.startsWith('list')) {
                return true;
            }

            const shortType = isMobile() && view === 'timeGridWeek';

            const w = document.createElement('div');
            w.className = 'fc-event-custom';

            const title = document.createElement('div');
            title.className = 'fc-event-title-custom';
            title.textContent = shortType
                ? (arg.event.extendedProps.typeSession || arg.event.title)
                : arg.event.title;
            w.appendChild(title);

            if (!shortType && arg.timeText) {
                const t = document.createElement('div');
                t.className = 'fc-event-time-custom';
                t.textContent = arg.timeText;
                w.appendChild(t);
            }

            const salle = arg.event.extendedProps.salle;
            if (!useModal && salle) {
                const d = document.createElement('div');
                d.className = 'fc-event-details';
                d.textContent = salle;
                w.appendChild(d);
            }

            return { domNodes: [w] };
        },
    });

    calendar.render();
    calendarEl._fc = calendar;
    calendarEl._dd = dd;
    return calendar;
}

function initCalendars() {
    const VIEW_MAP = { today: 'timeGridDay', week: 'timeGridWeek', month: 'dayGridMonth', planning: 'listMonth' };
    const setupCalendar = (calendarId, urlPath) => {
        const calendarEl = document.getElementById(calendarId);
        const viewCalendar = document.getElementById('viewCalendar');

        if (calendarEl) {
            if (viewCalendar) {
                const getParam = () => {
                    if (calendarId === 'calendar') {
                        return viewCalendar.value === 'mine' ? '?view=mine' : '';
                    } else if (calendarId === 'calendarAll') {
                        return viewCalendar.value ? `?view=${viewCalendar.value}` : '';
                    }
                    return '';
                };

                const updateCalendar = () => {
                    const paramCalendar = getParam();
                    getCalendar(calendarEl, `${emargementContextUrl}${urlPath}${paramCalendar}`, true, 'dayGridMonth', false, null);
                };
                viewCalendar.addEventListener("change", updateCalendar);
                updateCalendar();
            } else {
                getCalendar(calendarEl, `${emargementContextUrl}${urlPath}`, true, VIEW_MAP[viewCal] || 'listMonth', true, dateCal);
            }
        }
    };
    setupCalendar('calendar', '/manager/calendar/events');
    setupCalendar('calendarAll', '/superadmin/calendar/events');
    setupCalendar('calendarSup', '/supervisor/calendar/events');
    setupCalendar('userCalendar', '/user/calendar/events');
}


/* ==========================================================================
 * 4. Présence / QR code / SSE
 * ========================================================================== */
function updatePresence(url, numEtu, currentLocation, qrCodeCam = null) {
    var request = new XMLHttpRequest();
    var location = (currentLocation != null) ? "&currentLocation=" + currentLocation : "";
    request.open('GET', url + "?presence=" + numEtu + location, true);
    request.onload = function() {
        if (null != qrCodeCam) {
            qrCodeCam.setAttribute("data-qrcode-isvalid", "false");
        }

        if (request.status >= 200 && request.status < 400) {
            if ((document.getElementById("newbie") != null) || (null !== qrCodeCam)) {
                const data = JSON.parse(request.responseText);
                if (data != null) {
                    if (document.getElementById("newbie") != null) {
                        var tc = data[0];
                        var tagDate = moment(tc.tagDate).format('DD-MM-YYYY HH:mm:ss');
                        var info = moment(tc.sessionEpreuve.dateExamen).format('DD-MM-YYYY') + " // " +
                            tc.sessionEpreuve.nomSessionEpreuve + " // ";
                        $("#norole").addClass("d-none");
                        $("#newbie").removeClass("d-none");
                        $("#tagDate").text(tagDate);
                        $("#sessionNewbie").text(info);
                        $("#accordionNewbie").hide();
                    }

                    if (data.length > 0) {
                        // Il y a bien eu validation de la présence
                        // On affiche un message dans le canvas de la camera
                        // TODO Voir sous quelles conditions on efface ce message (fermeture/ré-ouverture de la fenêtre)
                        document.getElementById("qrCodeScanOk").style.display = "block";
                        if (null != qrCodeCam) {
                            // On affiche le cadre du QR code en vert (tant que l'on ne change pas de QRCode)
                            qrCodeCam.setAttribute("data-qrcode-isvalid", "true");
                        }
                    } else {
                        document.getElementById("qrCodeScanUnknown").style.display = "block";
                        if (null != qrCodeCam) {
                            // On affiche le cadre du QR code en vert (tant que l'on ne change pas de QRCode)
                            qrCodeCam.setAttribute("data-qrcode-isvalid", "true");
                        }
                    }
                }
            }
            deleteParam(window.location.href, "tc");
            setTimeout(function() {
                $("#displayedIdentity").addClass("d-none");
            }, 2000);
        } else {
            if (null != qrCodeCam) {
                qrCodeCam.setAttribute("data-qrcode-isvalid", "false");
            }
            console.log("erreur du serveur!");
        }
    };
    request.send();
}

function displayToast() {
    // off() : évite d'empiler les écouteurs à chaque rechargement d'un bloc de présence
    $(".userToast").off("click.userToast").on("click.userToast", function(event) {
        const toastLiveExample = document.getElementById('liveToast');
        if (toastLiveExample != null) {
            const toast = new bootstrap.Toast(toastLiveExample);
            var splitField = this.getAttribute("data-whatever").split("//");
            $("#photoPresent").prop("src", emargementContextUrl + "/supervisor/" + splitField[0] + "/photo");
            var nom = "";
            var prenom = "";
            if (splitField[1] != "null") {
                nom = splitField[1];
                prenom = splitField[2];
            }
            $("#photoPresent").prop("alt", "Photo " + prenom + " " + nom);
            $("#nomPresence").text(nom);
            $("#prenomPresence").text(prenom);
            if (splitField[3] != "null") {
                $("#numIdentifiantPresence").text(splitField[3]);
            }
            if (splitField[4] != "null") {
                $("#codeEtape").text(splitField[4]);
            }
            toast.show();
        }
    });
}

function getQrCodeSession(url, idImg) {
    $.ajax({
        url: url,
        method: 'GET',
        success: function(response) {
            $("#" + idImg).attr("src", "data:image/png;base64, " + response);
        },
        error: function(xhr, status, error) {
            console.error('Error:', error);
        }
    });
}

// QR code affiché pour la session courante (rafraîchi périodiquement)
function initQrCodeDisplay() {
    var qrCodeDisplay = document.getElementById("qrCodeDisplay");
    if (qrCodeDisplay != null) {
        var url = emargementContextUrl + "/supervisor/qrCodeSession/" + currentLocation;
        getQrCodeSession(url, "imgQrCode");
        setInterval(function() { getQrCodeSession(url, "imgQrCode"); }, qrcodeChange);
    }
}

// Page de présence : abonnement SSE et rechargement des blocs
function initPresencePage() {
    if (document.getElementById('presencePage') == null && document.getElementById('unknownPage') == null) return;

    const uuid = ID();
    const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
    sortDate = (triBadgeage == 'true') ? [0, 'asc'] : [4, 'desc'];
    initTablePresence(sortDate);

    eventSource.addEventListener('tc', response => {
        var tagCheck = JSON.parse(response.data);
        if (!tagCheck.isBlacklisted) {
            var person = tagCheck.person;
            var guest = tagCheck.guest;
            var sessionId = tagCheck.sessionEpreuve.id;
            var sessionLocationExpected = (tagCheck.sessionLocationExpected != null) ? tagCheck.sessionLocationExpected.id : null;
            var identifiant = "";
            var varUrl = "";
            if (person != null) {
                identifiant = person.eppn;
                varUrl = person.eppn;
            } else if (guest != null) {
                identifiant = guest.email;
                varUrl = "inconnu";
            }
            var url = emargementContextUrl + "/supervisor/" + varUrl + "/photo";
            var nom = "";
            var prenom = "";
            if (person != null && person.nom != null) {
                nom = person.nom.toUpperCase();
                prenom = person.prenom;
            } else if (guest != null && guest.nom != null) {
                nom = guest.nom.toUpperCase();
                prenom = guest.prenom;
            }
            if (sessionLocationExpected != null) {
                if (tagCheck.tagChecker != null && tagCheck.tagChecker.userApp.eppn == eppnAuth) {
                    var displayedIdentity = sessionLocationExpected + "_displayedIdentity2";
                    var displayedIdentity2 = $("#" + displayedIdentity);
                    displayedIdentity2.removeClass("d-none");
                    displayedIdentity2.find("img").prop("src", url);
                    displayedIdentity2.find("img").prop("alt", identifiant);
                    displayedIdentity2.find('#prenomPresence3').text(prenom);
                    displayedIdentity2.find('#nomPresence3').text(nom);
                    if (person != null && person.numIdentifiant != null) {
                        displayedIdentity2.find('#numIdentifiantPresence2').text('N° ' + person.numIdentifiant);
                    }
                    const toastLiveExample = document.getElementById(displayedIdentity);
                    const toast = new bootstrap.Toast(toastLiveExample, { 'delay': 2000 });
                    toast.show();
                }
            }
            var urlLocation = (sessionLocationExpected != null) ? sessionLocationExpected : $_GET("location");
            var url = emargementContextUrl + "/supervisor/presence?sessionEpreuve=" + sessionId +
                "&location=" + urlLocation + "&update=" + tagCheck.id;
            var idBlock = tagCheck.isUnknown ? tagCheck.sessionLocationBadged.id : sessionLocationExpected;
            $("#resultsBlock" + idBlock).load(url, function(responseText, textStatus, XMLHttpRequest) {
                backToTop();
                if (tagCheck.tagChecker != null && tagCheck.tagChecker.userApp.eppn == eppnAuth) {
                    displayToast();
                }
                initTablePresence(sortDate);
            });
        }
    }, false);

    eventSource.addEventListener('sl', response => {
        var sl = JSON.parse(response.data);
        $("#sl" + sl.id).html(sl.nbPresentsSessionLocation);
    }, false);

    eventSource.addEventListener('message', function(e) {
        var splitData = e.data.split("@@");
        var percent = parseFloat(splitData[1]).toFixed(2);
        $("#progressBar" + splitData[0]).css("width", percent + '%');
        $("#progressBar" + splitData[0]).text(percent + '%');
    });

    eventSource.addEventListener('total', response => {
        var total = response.data;
        var splitData1 = total.split("@@");
        $("#totalPresent" + splitData1[0]).text(splitData1[1]);
    }, false);

    eventSource.addEventListener('customMsg', response => {
        var customMsg = response.data;
        if (customMsg != "") {
            $("#customMsg").removeClass("d-none").addClass("show");
            setTimeout(function() {
                $("#customMsg").addClass("d-none");
            }, 2000);
        }
    }, false);

    eventSource.addEventListener('tagChecker', response => {
        var tagChecker = JSON.parse(response.data);
        var sessionId = tagChecker.sessionLocation.sessionEpreuve.id;
        var urlLocation = $_GET("location");
        var url = emargementContextUrl + "/supervisor/presence?sessionEpreuve=" + sessionId +
            "&location=" + urlLocation + "&tcer=true&update=" + tagChecker.id;
        $("#resultsBlock" + tagChecker.sessionLocation.id).load(url, function(responseText, textStatus, XMLHttpRequest) {
            backToTop();
            displayToast();
            sortDate = (triBadgeage == 'true') ? [0, 'asc'] : [4, 'desc'];
            initTablePresence(sortDate);
        });
    }, false);
}

// Compteur d'imports ADE (SSE)
function initImportSessionCounter() {
    const uuid = ID();
    const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
    eventSource.addEventListener('nbImportSession', response => {
        var total = response.data;
        $("#nbImportSession").text("Imports ADE : " + total);
    }, false);
}

// Scan de QR code par webcam (offcanvas)
function initWebcamScanner() {
    const myOffcanvas = document.getElementById('offcanvasWebcam');
    if (myOffcanvas == null) return;

    myOffcanvas.addEventListener('shown.bs.offcanvas', event => {
        var qrCodeCam = document.getElementById("qrCodeCam");
        if (qrCodeCam != null) {
            startQrScanner(qrCodeCam);
        }
    });
    myOffcanvas.addEventListener('hidden.bs.offcanvas', event => {
        window.location.reload();
    });
}

function startQrScanner(qrCodeCam) {
    var video = document.createElement("video");
    var canvasElement = document.getElementById("canvas");
    var canvas = canvasElement.getContext("2d");
    var loadingMessage = document.getElementById("loadingMessage");

    function drawLine(begin, end, color) {
        canvas.beginPath();
        canvas.moveTo(begin.x, begin.y);
        canvas.lineTo(end.x, end.y);
        canvas.lineWidth = 4;
        canvas.strokeStyle = color;
        canvas.stroke();
    }

    // Use facingMode: environment to attemt to get the front camera on phones
    navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } }).then(function(stream) {
        video.srcObject = stream;
        video.setAttribute("playsinline", true); // required to tell iOS safari we don't want fullscreen
        video.play();
        requestAnimationFrame(tick);
    });
    let isCodeScanned = false;
    function tick() {
        var color;
        loadingMessage.innerText = "⌛ Loading video...";
        if (video.readyState === video.HAVE_ENOUGH_DATA) {
            loadingMessage.hidden = true;
            canvasElement.hidden = false;
            canvasElement.height = video.videoHeight;
            canvasElement.width = video.videoWidth;
            canvas.drawImage(video, 0, 0, canvasElement.width, canvasElement.height);
            var imageData = canvas.getImageData(0, 0, canvasElement.width, canvasElement.height);
            var code = jsQR(imageData.data, imageData.width, imageData.height, {
                inversionAttempts: "dontInvert",
            });
            if (code) {
                if ("true" == qrCodeCam.getAttribute("data-qrcode-isvalid")) {
                    color = "#00FF00";
                } else {
                    color = "#FF3B58";
                }
                drawLine(code.location.topLeftCorner, code.location.topRightCorner, color);
                drawLine(code.location.topRightCorner, code.location.bottomRightCorner, color);
                drawLine(code.location.bottomRightCorner, code.location.bottomLeftCorner, color);
                drawLine(code.location.bottomLeftCorner, code.location.topLeftCorner, color);
            }
            if (!isCodeScanned) {
                if (code) {
                    var value = code.data;
                    var sessionData = qrCodeCam.getAttribute("data-qrcode");
                    if (sessionData != null) {
                        var splitCode = value.split("value=");
                        value = splitCode[1] + "@@@" + sessionData;
                    }
                    var prefix = "";
                    if (qrCodeCam.hasAttribute('data-role')) {
                        prefix = emargementContextUrl + "/";
                    }
                    var locationInput = document.getElementById("location");
                    var valLocation = (locationInput != null) ? locationInput.value : null;
                    updatePresence(prefix + "updatePresents", value, valLocation, qrCodeCam);
                    isCodeScanned = true;
                    console.log("QR Code scanned once.");
                    setTimeout(() => {
                        isCodeScanned = false;
                        qrCodeCam.setAttribute("data-qrcode-isvalid", "false");
                        console.log("Simulated scan. Code scanned only once.");
                    }, 2000);
                } else {
                    isCodeScanned = false; // Reset the flag if no QR code is found
                    qrCodeCam.setAttribute("data-qrcode-isvalid", "false");
                }
            }
        }
        requestAnimationFrame(tick);
    }
    tick();
}


/* ==========================================================================
 * 5. Statistiques
 * ========================================================================== */
const MANAGER_IDS = ['sessionEpreuvesByCampus',
    'presenceByContext', 'sessionEpreuveByYearMonth', 'countTagCheckByYearMonth',
    'countTagChecksByTypeBadgeage', 'countSessionEpreuveByType'];

const SUPERADMIN_IDS = ['sessionEpreuvesByContext', 'countTagChecksByContext', 'countSessionsParMoisByContext',
    'countBadgeagesParMoisByContext', 'countSessionEpreuveByTypeByContext', 'countTagChecksByTypeEmargementByContext'];

async function renderChart(canvasId, baseUrl, { year, param } = {}) {
    const el = document.getElementById(canvasId);
    if (!el) return;

    const params = new URLSearchParams({ type: canvasId });
    if (year) params.set('anneeUniv', year);
    if (param) params.set('param', param);

    const res = await fetch(`${emargementContextUrl}/${baseUrl}/stats/json?${params}`);
    if (!res.ok) return;
    const config = await res.json();
    if (!config.data) return;
    new Chart(el.getContext('2d'), config);
}

function initStatsCharts() {
    const year = document.getElementById('anneeUnivSelect')?.value;

    if (document.getElementById('statsCharts') && year) {
        MANAGER_IDS.forEach(id => renderChart(id, 'manager', { year }));
    }
    if (document.getElementById('statsSuperAdminCharts') && year) {
        SUPERADMIN_IDS.forEach(id => renderChart(id, 'superadmin', { year }));
    }
}

// Graphique des badgeages d'une session (modale)
function initStatsSessionModal() {
    document.body.addEventListener('click', (e) => {
        const btn = e.target.closest('[id^=modalChartSeBtn]');
        if (!btn) return;

        const sessionId = btn.id.split('-')[1];

        const oldCanvas = document.getElementById('countTagChecksByTimeBadgeage');
        if (oldCanvas) {
            const fresh = document.createElement('canvas');
            fresh.id = 'countTagChecksByTimeBadgeage';
            fresh.style.height = '394px';
            fresh.style.width = '789px';
            fresh.height = 394;
            fresh.width = 789;
            oldCanvas.replaceWith(fresh);
        }

        renderChart('countTagChecksByTimeBadgeage', 'manager', { param: sessionId });
    });

    $('#modal-chart').on('shown.bs.modal', function(event) {
        var button = $(event.relatedTarget);
        var title = button.data('whatever');
        $(this).find('.modal-title').text(title);
    });
}


/* ==========================================================================
 * 6. ADE (jsTree, import d'événements, affinage)
 * ========================================================================== */
function updateJsTree(selectedData, category, idProject) {
    var selectedUrl = emargementContextUrl + "/manager/adeCampus/json?idProject=" + idProject + "&category=" + category + "&fatherId=" + selectedData;
    var request = new XMLHttpRequest();
    request.open('GET', selectedUrl, true);
    request.onload = function() {
        if (request.status >= 200 && request.status < 400) {
            const data = JSON.parse(request.responseText);
            $('#frmt').jstree(true).settings.core.data = data;
            $('#frmt').jstree(true).refresh();
            $('#frmt').removeClass("d-none");
            $("#spinnerComps").addClass("d-none");
        }
    };
    request.send();
}

function initJsTree() {
    $('#frmt').on('changed.jstree', function(e, data) {
        var checkedNodes = data.selected.slice(); // copie pour sécurité
        var nodesToRemove = [];

        // Supprimer les nœuds parents si leurs enfants sont sélectionnés
        checkedNodes.forEach(function(nodeId) {
            var node = data.instance.get_node(nodeId);
            if (node.children.length > 0) {
                node.children.forEach(function(childId) {
                    if (checkedNodes.includes(childId)) {
                        nodesToRemove.push(nodeId);
                    }
                });
            }
        });
        nodesToRemove.forEach(function(nodeId) {
            var index = checkedNodes.indexOf(nodeId);
            if (index > -1) checkedNodes.splice(index, 1);
        });

        var r = [];               // IDs des nœuds finaux sélectionnés
        var text = [];
        var allLibelles = [];     // libellés texte complets
        var firstLevelIds = [];   // premiers niveaux après la racine

        checkedNodes.forEach(function(nodeId) {
            var node = data.instance.get_node(nodeId);
            text.push(node.text);
            r.push(node.id);

            // Texte du chemin complet
            let hierarchy = [];
            node.parents.slice().reverse().forEach(function(parentId) {
                if (parentId !== "#") {
                    var parentNode = data.instance.get_node(parentId);
                    hierarchy.push(parentNode.text);
                }
            });
            hierarchy.push(node.text);
            allLibelles.push(hierarchy.join(">"));

            // Chemin en IDs de la racine au nœud (node.parents va de l'enfant vers '#')
            var pathFromRoot = node.parents.slice().reverse().filter(function(p) { return p !== "#"; });
            pathFromRoot.push(node.id);

            // Premier élément sous la racine (ex. [root, b, c, d] => b)
            var firstAfterRoot = pathFromRoot.length > 1 ? pathFromRoot[1] : pathFromRoot[0];
            firstLevelIds.push(firstAfterRoot);
        });

        // dédupliquer (plusieurs descendants de la même branche peuvent être cochés)
        var uniqueFirstLevel = Array.from(new Set(firstLevelIds));

        $("#idList").val(r.join(','));
        $("#idListImport").val(r.join(','));
        $("#idListTask").val(r.join(','));
        $("#textListTask").val(allLibelles.join(','));
        $("#firstLevelIds").val(uniqueFirstLevel.join(','));

    }).jstree({
        "checkbox": {
            "keep_selected_style": false,
            "three_state": false,
            "cascade": "up"
        },
        "plugins": ["checkbox", "sort", "state"],
        'core': {
            'data': []
        }
    });
}

function importEvents(url) {
    $("#importBtn").off('click').on('click', function(event) {
        event.preventDefault();
        const $form = $("#displayEventsImport");
        $("#spinnerLoad").removeClass("d-none");
        $.ajax({
            url: url,
            type: "POST",
            data: $form.serialize(),
            success: function() {
                $('#displayEvents').submit();
            },
            error: function(error) {
                console.log("Error:", error);
            },
            complete: function() {
                $("#spinnerLoad").addClass("d-none");
            }
        });
    });
}

// Page ADE : sélection de salles / événements
function initAdeCampus(tableSalles) {
    initJsTree();

    $("#codeSalleSelect").on("change", function(event) {
        document.getElementById("displaySalles").submit();
    });

    $("#displayEvents").on("submit", function(event) {
        $("#spinnerLoad").removeClass("d-none");
    });

    $("#strDateMin").on("change", function() {
        var strDateMin = $("#strDateMin").val();
        $("#strDateMinImport").val(strDateMin);
        if ($("#strDateMax").val() == "") {
            $("#strDateMax").val(strDateMin);
            $("#strDateMaxImport").val(strDateMin);
        }
    });
    $("#strDateMax").on("change", function() {
        $("#strDateMaxImport").val($("#strDateMax").val());
    });

    $("#codeComposante1").on("change", function(event) {
        const selectedData = $(this).val();
        $("#codeFormation").val($(this).val());
        $("input[id^=hiddenCodeComposante]").val(selectedData);
        if (selectedData == "myEvents") {
            $('#frmt').addClass("d-none");
        } else {
            if (selectedData != "") {
                $("#spinnerComps").removeClass("d-none");
                updateJsTree(selectedData, "trainee", $("#idProject").val());
            }
        }
        if (tableSalles) tableSalles.clear().draw();
    });
    $("#codeFormation").on("change", function(event) {
        const selectedData = $(this).val();
        $("#codeComposante1").val("");
        $("input[id^=hiddenCodeComposante]").val(selectedData);
        if (selectedData == "myEvents") {
            $('#frmt').addClass("d-none");
        } else {
            if (selectedData != "") {
                $("#spinnerComps").removeClass("d-none");
                updateJsTree(selectedData, category6, $("#idProject").val());
            }
        }
        if (tableSalles) tableSalles.clear().draw();
    });

    //Prevention de la soumission vide
    $("form[action*='/manager/adeCampus/importClassrooms']").on("submit", function(event) {
        if ($('.tableSalles').find('.data-checkbox:checked').length === 0) {
            event.preventDefault(); // Bloque l'envoi
            alert("Veuillez sélectionner au moins une salle à importer.");
        }
    });

    var userEvents = document.getElementById("userEvents") != null;
    displayEvents(emargementContextUrl + (userEvents ? "/supervisor/events/adeCampus" : "/manager/adeCampus/Events"), tableSalles);
    importEvents(emargementContextUrl + (userEvents ? "/supervisor/events/adeCampus/importEvents" : "/manager/adeCampus/importEvents"));
}

//affinage repartition
function displayAffinage(classes, isTiers) {
    var total = 0;
    classes.forEach(function(item) {
        var number = parseInt(item.value.replace(/\s/g, ''));
        total = (total + number);
    });
    var affinageId = null;
    var tempCapacite = null;
    var currentCapacite = null;
    if (isTiers) {
        affinageId = $("#affinageTiersTemps");
        tempCapacite = $("#tempCapaciteTiers");
        currentCapacite = $("#currentCapaciteTiers");
    } else {
        affinageId = $("#affinageNotTiersTemps");
        tempCapacite = $("#tempCapaciteNotTiers");
        currentCapacite = $("#currentCapaciteNotTiers");
    }
    affinageId.removeClass("bg-light");
    affinageId.removeClass("bg-success");
    affinageId.removeClass("bg-danger");
    affinageId.removeClass("text-white");
    affinageId.removeClass("text-dark");
    var realTotal = total / 2;
    if (realTotal == currentCapacite[0].textContent) {
        affinageId.addClass("bg-success text-white");
        $("#affinerButton").removeAttr("disabled");
    } else {
        affinageId.addClass("bg-danger text-white");
        $("#affinerButton").prop("disabled", "disabled");
    }
    tempCapacite.text(realTotal);
}

function initAffinage() {
    $(".affinage").inputSpinner();

    $(".affinage").on("input", function(event) {
        var max = parseInt($(this).prop("max"));
        var current = parseInt($(this).val());
        var newPercent = $(this)[0].closest("span").nextElementSibling;
        var percentVal = (current * 100) / max;
        newPercent.textContent = Math.round(percentVal) + ' %';
        $(this).removeClass('is-valid');
        $(this).removeClass('is-invalid');
        if (current <= max) {
            $(this).addClass('is-valid');
        } else {
            $(this).addClass('is-invalid');
        }

        //Not tiers
        var notTiers = document.querySelectorAll('[id^="spinner_false"]');
        if (notTiers.length > 0) {
            displayAffinage(notTiers, false);
        }

        //Tiers
        var isTiers = document.querySelectorAll('[id^="spinner_true"]');
        if (isTiers.length > 0) {
            displayAffinage(isTiers, true);
        }
    });

    // Enregistré une seule fois (il l'était à chaque saisie dans un champ)
    var submitAffinage = document.getElementById("affinerButton");
    if (submitAffinage != null) {
        submitAffinage.addEventListener('click', function(e) {
            var formAffinage = document.getElementById("formAffinage");

            // Récupère le bouton radio sélectionné
            var selectedRadio = document.querySelector('input[name="tagCheckOrder"]:checked');

            if (selectedRadio) {
                // Supprime les anciens inputs "tagCheckOrder" du formulaire
                var oldInputs = formAffinage.querySelectorAll('input[name="tagCheckOrder"]');
                oldInputs.forEach(el => el.remove());

                // Crée un hidden input pour transmettre la valeur
                var hiddenInput = document.createElement("input");
                hiddenInput.type = "hidden";
                hiddenInput.name = "tagCheckOrder";
                hiddenInput.value = selectedRadio.value;

                formAffinage.appendChild(hiddenInput);
            }

            formAffinage.submit();
        });
    }

    $('input[type=radio][name=tagCheckOrder]').on("change", function(event) {
        var tagCheckOrder = $(this).val();
        $("#alphaOrderAffinage").val(tagCheckOrder);
        $("#alphaOrderRepartition").val(tagCheckOrder);
    });
}


/* ==========================================================================
 * 7. DataTables
 * ========================================================================== */
/* ===== DATATABLES:START ===== */
// DataTables 3.x : plus de plugins de dates (datetime-moment / date-eu), `dom` remplacé par `layout`,
// options communes factorisées. Helpers préfixés "dt" pour éviter les collisions de noms globaux.

const DT_LANG_URL = '/webjars/datatables-plugins/i18n/fr-FR.json';

const DT_DATE_SHORT = 'DD/MM/YY';
const DT_DATE_LONG = 'DD/MM/YYYY';
const DT_DATETIME_SHORT = 'DD/MM/YY HH:mm:ss';
const DT_PRESENCE_FORMATS = [DT_DATETIME_SHORT, 'DD/MM/YY HH:mm', 'HH:mm'];
// Remplace l'ancien type de tri "date-eu" (plugin supprimé) ; adaptez si vos dates ont un autre format
const DT_EU_DATE_FORMATS = [
    'DD/MM/YYYY HH:mm:ss', 'DD/MM/YYYY HH:mm', DT_DATE_LONG,
    DT_DATETIME_SHORT, 'DD/MM/YY HH:mm', DT_DATE_SHORT
];
const DT_PRESENCE_DATE_COLUMN = 4;

const DT_BASE_OPTIONS = {
    responsive: true,
    info: false,
    language: { url: DT_LANG_URL }
};

const DT_NO_SORT_COLUMN = { targets: 'no-sort', orderable: false };

/** Initialise toutes les tables correspondant au sélecteur ; renvoie la première (ou null si aucune). */
function dtInitTable(selector, options = {}) {
    const instances = [...document.querySelectorAll(selector)]
        .map(el => new DataTable(el, { ...DT_BASE_OPTIONS, ...options }));
    return instances[0] ?? null;
}

/**
 * Rendu de colonne date via moment :
 * - tri/type : timestamp numérique
 * - autres : date reformatée avec `output` (ou valeur d'origine si `output` est omis)
 */
function dtMomentRender(formats, { output = null, strict = false } = {}) {
    return function(data, type) {
        if (!data) return '';
        const date = moment(String(data).trim(), formats, strict);
        if (type === 'sort' || type === 'type') {
            return date.isValid() ? date.valueOf() : 0;
        }
        return date.isValid() && output ? date.format(output) : data;
    };
}

const dtDateColumn = (formats, options) => ({
    targets: 'dateItem',
    render: dtMomentRender(formats, options)
});

const dtEscapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[c]));

/** Colonne de cases à cocher dont la valeur provient de la colonne `valueIndex`. */
const dtCheckboxColumn = (target, valueIndex) => ({
    targets: target,
    orderable: false,
    className: 'select-checkbox',
    render: (data, type, row) => type === 'display'
        ? `<input type="checkbox" class="data-checkbox" name="btSelectItem" value="${dtEscapeHtml(row[valueIndex])}">`
        : ''
});

// ---------- Options communes aux tables d'assiduité ----------
function buildDataTableOptions(customOrder) {
    return {
        order: customOrder ?? [[4, 'desc'], [7, 'asc']],
        columnDefs: [dtDateColumn(DT_DATE_SHORT, { output: DT_DATE_SHORT })],
        pageLength: 100,
        lengthMenu: [10, 25, 50, 100, { label: 'Tous', value: -1 }],
        layout: {
            topStart: 'search',
            topEnd: {
                buttons: [{
                    extend: 'csv',
                    className: 'btn btn-success btn-sm ms-2',
                    title: 'assiduite',
                    exportOptions: {
                        orthogonal: 'filter',
                        columns: ':not(.exclude)'
                    }
                }]
            },
            bottomStart: 'pageLength',
            bottomEnd: 'paging'
        }
    };
}

// ---------- Table de présence ----------
function initTablePresence(sortDate) {
    const table = dtInitTable('#tablePresence', {
        paging: false,
        order: [sortDate],
        columnDefs: [dtDateColumn(DT_PRESENCE_FORMATS, { output: DT_DATETIME_SHORT, strict: true })],
        drawCallback() {
            const hasData = this.api().rows().count() > 0;
            const search = document.querySelector('#tablePresence_wrapper .dt-search');
            if (search) search.style.display = hasData ? '' : 'none';
        }
    });
    if (!table) return;

    // Les lignes prioritaires restent en haut, triées chronologiquement.
    table.on('draw.dt', () => {
        const body = document.querySelector('#tablePresence tbody');
        const rows = [...body.querySelectorAll('tr.priority')];
        if (!rows.length) return;

        const sign = (table.order()[0]?.[1] ?? 'asc') === 'asc' ? 1 : -1;
        const timeOf = row => {
            const text = (row.cells[DT_PRESENCE_DATE_COLUMN]?.textContent ?? '').trim();
            const date = moment(text, DT_PRESENCE_FORMATS, true);
            return date.isValid() ? date.valueOf() : 0;
        };

        const sorted = rows
            .map(row => ({ row, time: timeOf(row) }))
            .sort((a, b) => sign * (a.time - b.time))
            .map(({ row }) => row);

        body.prepend(...sorted);
    });
}

// ---------- Liste des événements (chargée en AJAX) ----------
const dtEventsTableOptions = () => ({
    columnDefs: [
        dtCheckboxColumn(1, 2),
        dtDateColumn(DT_EU_DATE_FORMATS),
        { targets: 0, className: 'dtr-control', orderable: false }
    ],
    select: { style: 'multi' },
    order: [[6, 'desc']],
    responsive: { details: { type: 'column' } }
});

function displayEvents(url, table = null) {
    let eventsTable = table;

    // Écouteur jQuery (et non addEventListener) : importEvents() déclenche `$('#displayEvents').submit()`,
    // qui n'appelle que les écouteurs jQuery.
    $('#displayEvents').off('submit.events').on('submit.events', async function(event) {
        event.preventDefault();
        const params = new URLSearchParams(new FormData(this));
        const target = url + (url.includes('?') ? '&' : '?') + params;

        try {
            const response = await fetch(target);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const html = await response.text();

            eventsTable?.destroy();
            document.getElementById('tableEvents').innerHTML = html;
            eventsTable = dtInitTable('.tableFoo', dtEventsTableOptions());
        } catch (error) {
            console.error('Erreur de chargement des événements :', error);
        } finally {
            document.getElementById('spinnerLoad')?.classList.add('d-none');
        }
    });
}

// ---------- Filtres déroulants de la table des absences ----------
const DT_ABSENCE_FILTERS = [
    { column: 3 },
    { column: 4 },
    { column: 5, exact: true } // correspondance stricte : "JUSTIFIE" ≠ "INJUSTIFIE"
];

function dtSetupAbsenceFilters() {
    const api = this.api();
    const searchBox = api.table().container().querySelector('.dt-search');
    if (!searchBox) return;
    const searchInput = searchBox.querySelector('input');

    const filters = document.createElement('div');
    filters.className = 'd-flex gap-2';

    DT_ABSENCE_FILTERS.forEach(({ column: index, exact }) => {
        const column = api.column(index);
        const select = document.createElement('select');
        select.className = 'form-control form-select w-auto';
        select.add(new Option(column.header().textContent.trim(), ''));

        const values = new Set(column.data().toArray().map(d => stripHTMLTagsUsingDOM(d)));
        [...values].sort().forEach(value => select.add(new Option(value)));

        select.addEventListener('change', () => {
            const value = DataTable.util.escapeRegex(select.value);
            column.search(value && exact ? `^${value}$` : value, true, false).draw();
        });
        filters.appendChild(select);
    });

    const searchWrapper = document.createElement('div');
    searchWrapper.className = 'ms-auto';
    if (searchInput) searchWrapper.appendChild(searchInput);

    searchBox.classList.add('d-flex', 'justify-content-between', 'align-items-center', 'w-100');
    searchBox.replaceChildren(filters, searchWrapper);
}

// ---------- Initialisation des tables de la page ; renvoie la table des salles (ou null) ----------
function initDataTables() {
    // Assiduité
    dtInitTable('table.assiduite', buildDataTableOptions([[4, 'desc'], [6, 'asc']]));
    dtInitTable('table.assiduite2', buildDataTableOptions([[4, 'desc'], [7, 'asc']]));
    dtInitTable('table.assiduite3', buildDataTableOptions([[0, 'asc'], [1, 'asc']]));

    // Tâches / nettoyage / sessions HTTP
    dtInitTable('.tableTasks', {
        columnDefs: [dtDateColumn(DT_DATE_LONG, { output: DT_DATE_LONG }), DT_NO_SORT_COLUMN]
    });
    dtInitTable('.tableCleanup', {
        columnDefs: [dtDateColumn(DT_DATE_SHORT, { output: DT_DATE_SHORT }), DT_NO_SORT_COLUMN]
    });
    dtInitTable('#tableSessionsHttp', {
        columnDefs: [dtDateColumn(DT_DATETIME_SHORT, { output: DT_DATETIME_SHORT }), DT_NO_SORT_COLUMN]
    });

    // Absences
    dtInitTable('.tableAbsence', {
        info: true,
        columnDefs: [dtDateColumn(DT_DATE_SHORT, { output: DT_DATE_SHORT }), DT_NO_SORT_COLUMN],
        initComplete: dtSetupAbsenceFilters
    });

    // Salles
    return dtInitTable('.tableSalles', {
        columnDefs: [dtCheckboxColumn(0, 1), dtDateColumn(DT_EU_DATE_FORMATS)]
    });
}

// ---------- Cases à cocher des tables Salles et Événements ----------
// Une seule délégation pour toute la page : les tables .tableFoo sont recréées à chaque recherche.
const DT_CHECKBOX_TABLES = '.tableSalles, .tableFoo';

function initCheckboxSelection() {
    document.addEventListener('change', event => {
        const target = event.target;
        if (!(target instanceof HTMLInputElement)) return;

        if (target.id === 'checkAll') {
            document.querySelectorAll(DT_CHECKBOX_TABLES.split(',').map(s => `${s.trim()} .data-checkbox`).join(','))
                .forEach(box => { box.checked = target.checked; });
        } else if (target.classList.contains('data-checkbox')) {
            const table = target.closest(DT_CHECKBOX_TABLES);
            const checkAll = document.getElementById('checkAll');
            if (table && checkAll) {
                checkAll.checked = [...table.querySelectorAll('.data-checkbox')].every(box => box.checked);
            }
        }
    });
}
/* ===== DATATABLES:END ===== */


/* ==========================================================================
 * 8. Initialisation (DOMContentLoaded)
 * ========================================================================== */

// Assiduité : filtres, période, recherche
function initAssiduite() {
    $(".searchEtu").on('click', function() {
        $("#searchField").val(this.text);
    });

    //Select sessionEpreuve
    checkAll();

    //Assiduité dateRange
    var start = moment();
    var end = moment();
    if (datesRangeSelect != '') {
        var splitRange = datesRangeSelect.split("@");
        start = moment(splitRange[0], "YYYY-MM-DD");
        end = moment(splitRange[1], "YYYY-MM-DD");
    }

    function cb(start, end) {
        if (start && end) {
            $('#reportrange span').html(start.format('DD-MM-YYYY') + ' - ' + end.format('DD-MM-YYYY'));
        }
    }

    $('#reportrange').daterangepicker({
        startDate: start,
        endDate: end,
        ranges: {
            "Aujourd'hui": [moment(), moment()],
            "Hier": [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
            'Cette semaine': [
                moment().startOf('isoWeek'),  // Monday of the current week
                moment().endOf('isoWeek')     // Sunday of the current week
            ],
            'Mois courant': [moment().startOf('month'), moment().endOf('month')],
            'Mois dernier': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
            'Année universitaire': [
                // If current month is January-August, show the range of the previous September to this August
                moment().month(8).startOf('month').subtract(moment().month() < 8 ? 1 : 0, 'year').startOf('day'),
                moment().month(7).endOf('month').add(moment().month() >= 8 ? 1 : 0, 'year').endOf('day')
            ]
        },
        locale: {
            customRangeLabel: 'Dates personnalisées',
            applyLabel: 'Valider',
            cancelLabel: 'Annuler'
        }
    }, cb);

    cb(start, end);

    $('#reportrange').on('apply.daterangepicker', function(ev, picker) {
        $("#datesRange").val(picker.startDate.format('YYYY-MM-DD') + "@" + picker.endDate.format('YYYY-MM-DD'));
        $("#formSearch").submit();
    });

    $(".assiduiteControl").on("change", function(event) {
        $("#formSearch").submit();
    });

    $('#searchAssiduite').focus(function() {
        if ($(this).val() !== '') {
            $(this).val('');
            $("#searchField").val("");
            $("#searchValue").val("");
        }
    });

    //assiduité page
    if (document.getElementById("assiduitePage")) {
        ['groupe', 'sessionEpreuve'].forEach(id => {
            if (document.getElementById(id)) {
                new SlimSelect({ select: `#${id}` });
            }
        });
        document.getElementById("searchUrl").value = window.location.search;
        document.getElementById("searchUrl2").value = window.location.search;
    }

    initSelectCheckBoxes('my-select', 'Rechercher dans tous les agents');
    initSelectCheckBoxes('my-select2', 'Rechercher dans ceux déjà utilisés');
}

// Formulaires de recherche de sessions
function initSessionSearch() {
    if (document.getElementById("sessionSearch") != null) {
        $("#formSearch select").on("change", function(event) {
            $("#searchBox").val(null);
            document.getElementById("formSearch").submit();
        });
    }
}

// Option "inclure le logo"
function initIncludeLogo() {
    updateHidden();
    $('#includeLogo').on('change', function() {
        updateHidden();
    });
}

document.addEventListener('DOMContentLoaded', function() {
    // Statistiques et modales
    initStatsCharts();
    showInitialModals();
    initStatsSessionModal();

    // Formulaires et éditeurs
    initCaseSelection();
    initConvocationEditors();
    initDelegatedClicks();
    initConfigForm();
    initImportForms();
    initHelpForm();
    initCommunicationPdf();
    initCreateTagCheckForm();
    initDuplicateSessions();
    initIncludeLogo();
    initSessionSearch();

    // Navigation, calendrier, pagination
    initCalendars();
    initPaginationAll();
    initNavigationSelects();
    initSessionListActive();

    // SlimSelect
    initPageSlimSelects();
    initSlimSelects();
    initProxy();

    // Présence
    initPresencePage();
    initWebcamScanner();
    initQrCodeDisplay();
    initDeleteTagCheck();
    displayToast();

    // Fichiers, cases à cocher, répartition
    initializeFileInput(document.querySelector('#files'));
    initNotUsedSelection();
    initAffinage();

    // ADE + DataTables
    initImportSessionCounter();
    var tableSalles = initDataTables();
    initCheckboxSelection();
    initAdeCampus(tableSalles);

    // Assiduité, plugins, autocomplete
    initAssiduite();
    initBootstrapAndPlugins();
    setupModal('absenceTagCheckModal', [
        { attr: 'data-bs-tcid', elementId: 'tcIdAbsence' },
        { attr: 'data-bs-nom', elementId: 'nomAbsence' }
    ]);
    setupModal('commentTagCheckModal', [
        { attr: 'data-bs-tcid', elementId: 'tcIdComment' },
        { attr: 'data-bs-nom', elementId: 'nomComment' },
        { attr: 'data-bs-comment', elementId: 'tcComment' }
    ]);
    initAutocomplete();
});


/* ==========================================================================
 * 9. Écouteurs HTMX
 * ========================================================================== */

//absences dans assiduité
document.addEventListener('htmx:afterSwap', function(event) {

    if (document.getElementById("motifAbsence") != null) {
        if (event.detail.target.id !== "searchResults" && document.getElementById("motifAbsence") && document.getElementById("createAbsence") == null) {
            setTimeout(() => {
                if (slimSelectInstance) {
                    slimSelectInstance.destroy();
                }
                slimSelectInstance = new SlimSelect({ select: '#motifAbsence' });
            }, 100);
        }
    }
    if (document.getElementById("blackListGroupe") != null) {
        if (event.target.id === "modal-content") {
            new SlimSelect({
                select: '#blackListGroupe'
            });

            let fileInput = document.querySelector('#files');
            if (fileInput) {
                initializeFileInput(fileInput);
            }
        }
    }
    if (document.getElementById("modal-step3")) {
        initSelectCheckBoxes('my-select', 'Rechercher dans tous les agents');
        initSelectCheckBoxes('my-select2', 'Rechercher dans ceux déjà utilisés');
    }
    if (event.target.querySelector('select.slimSelectClass')) {
        initSlimSelects();
    }
    initExtractionSelects();

    //liste sessions
    const match = window.location.pathname.match(/\/([^/]+)\/sessionEpreuve\/(\d+)/);
    if (!match) return;

    const sessionType = match[1]; // ex: sessionLocation
    const id = match[2];
    document.querySelectorAll('.session-item')
        .forEach(el => {
            const isMatch =
                el.dataset.sessionEpreuveId === id &&
                el.dataset.sessionType === sessionType;

            el.classList.toggle('active', isMatch);
        });
});

document.addEventListener('htmx:afterSettle', function(evt) {
    if (document.getElementById("modal-step3")) {
        $('#my-select').hide();
        $('#my-select2').hide();
    }
});

document.addEventListener("htmx:afterRequest", function(event) {
    if (document.getElementById("presencePage")) {
        let locationSelect = document.getElementById("location");
        if (locationSelect != null) {
            if (locationSelect.children.length > 1) {
                locationSelect.removeAttribute("disabled");
            } else {
                locationSelect.setAttribute("disabled", "true");
            }
        }
    }
});
