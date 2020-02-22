package edu.wpi.axon.ui

import arrow.core.Either
import edu.wpi.axon.db.JobDb
import edu.wpi.axon.db.data.Job
import edu.wpi.axon.db.data.InternalJobTrainingMethod
import edu.wpi.axon.db.data.ModelSource
import edu.wpi.axon.db.data.TrainingScriptProgress
import edu.wpi.axon.dsl.defaultBackendModule
import edu.wpi.axon.plugin.DatasetPlugins
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.tfdata.loss.Loss
import edu.wpi.axon.tfdata.optimizer.Optimizer
import edu.wpi.axon.tflayerloader.ModelLoaderFactory
import edu.wpi.axon.training.ModelDeploymentTarget
import edu.wpi.axon.ui.main.defaultFrontendModule
import edu.wpi.axon.util.FilePath
import java.io.File
import java.nio.file.Paths
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.get

class MainUI : Application(), KoinComponent {

    override fun start(primaryStage: Stage) {
        startKoin {
            modules(
                listOf(
                    defaultBackendModule(), defaultFrontendModule()
                )
            )
        }

        val root = AnchorPane().apply {
            children.add(BorderPane().apply {
                AnchorPane.setTopAnchor(this, 0.0)
                AnchorPane.setRightAnchor(this, 0.0)
                AnchorPane.setBottomAnchor(this, 0.0)
                AnchorPane.setLeftAnchor(this, 0.0)

                center = JobTableView().apply {
                    maxWidth = Double.MAX_VALUE
                    selectionModel.selectedItemProperty().addListener { _, _, newValue: Job? ->
                        newValue?.let {
                            right = JobDetailView(newValue) {
                                right = null
                                selectionModel.clearSelection()
                            }
                        }
                    }
                }
            })
        }

        primaryStage.apply {
            title = "Hello, World!"
            scene = Scene(root, 1000.0, 800.0).apply {
                stylesheets.add("/*\n * The MIT License (MIT)\n *\n * Copyright (c) 2015 - AGIX | Innovative Engineering\n *\n * Permission is hereby granted, free of charge, to any person obtaining a copy\n * of this software and associated documentation files (the \"Software\"), to deal\n * in the Software without restriction, including without limitation the rights\n * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n * copies of the Software, and to permit persons to whom the Software is\n * furnished to do so, subject to the following conditions:\n *\n * The above copyright notice and this permission notice shall be included in all\n * copies or substantial portions of the Software.\n *\n * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT     SHALL THE\n * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n * SOFTWARE.\n *\n */\n\n/*\n *   This is a Material Design CSS for JavaFX\n */\n\n/*******************************************************************************\n *                                                                             *\n * Root                                                                        *\n *                                                                             *\n ******************************************************************************/\n.root {\n    -fx-font-family: \"\";\n    /* Swatch Colors - Blue*/\n    -swatch-100: #BBDEFB;\n    -swatch-200: #90CAF9;\n    -swatch-300: #64BEF6;\n    -swatch-400: #42A5F5;\n    -swatch-500: #2196F3;\n    /*default text */\n    -fx-text-base-color: rgb(100.0, 100.0, 100.0);\n    -fx-text-button-normal: -swatch-500;\n    -fx-text-button-colored: rgb(255.0, 255.0, 255.0);\n    -fx-text-button-text: rgb(100.0, 100.0, 100.0);\n    -fx-text-title-color: rgb(45.0, 45.0, 45.0);\n    -fx-text-subtitle-color: rgb(65.0, 65.0, 65.0);\n    -fx-text-control-title-color: rgb(130.0, 130.0, 130.0);\n    -fx-text-fill: -fx-text-base-color;\n    -dark: rgb(47.0, 52.0, 57.0);\n    -fx-disabled-opacity: 60%;\n    /*default colors */\n    -swatch-gray: rgb(200.0, 200.0, 200.0);\n    -swatch-dark-gray: rgb(150.0, 150.0, 150.0);\n    -swatch-light-gray: rgb(230.0, 230.0, 230.0);\n    -swatch-toolbar: rgb(245.0, 245.0, 245.0);\n    -swatch-toolbar-selected: rgb(215.0, 215.0, 215.0);\n    /*\n     Modena colors\n     */\n    -fx-dark-text-color: white; /* Text color when selected*/\n    -fx-mid-text-color: -fx-text-base-color;\n    -fx-light-text-color: -swatch-light-gray;\n    -fx-body-color: white;\n    /* A bright blue for the focus indicator of objects. Typically used as the\n     * first color in -fx-background-color for the \"focused\" pseudo-class. Also\n     * typically used with insets of -1.4 to provide a glowing effect.\n     */\n    -fx-focus-color: -swatch-400;\n    -fx-faint-focus-color: -swatch-200;\n    /* A bright blue for highlighting/accenting objects.  For example: selected\n     * text; selected items in menus, lists, trees, and tables; progress bars */\n    -fx-accent: -swatch-400;\n    -confirmation-color-confirmed: lightgreen;\n    -confirmation-color-success: rgba(0, 195, 0, 0.76);\n    -confirmation-color-warning: yellow;\n    -confirmation-color-error: rgba(230, 0, 0, 0.75);\n    -validation-color-error-text: rgb(230, 0, 0);\n    -validation-color-error-border: rgb(230, 0, 0);\n}\n\n/*******************************************************************************\n *                                                                             *\n * Material Design - Cards                                                     *\n *                                                                             *\n ******************************************************************************/\n.card {\n    -fx-background-color: rgb(255.0, 255.0, 255.0);\n    -fx-background-radius: 4.0;\n    -fx-effect: dropshadow(gaussian, rgba(0.0, 0.0, 0.0, 0.15), 6.0, 0.7, 0.0, 1.5);\n}\n\n.card-title .text {\n    -fx-fill: -fx-text-title-color;\n}\n\n.card-subtitle .text {\n    -fx-fill: -fx-text-subtitle-color;\n}\n\n.control-label .text {\n    -fx-fill: -fx-text-control-title-color;\n}\n\n.card-button {\n    -fx-effect: null;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Button & ToggleButton                                                       *\n *                                                                             *\n ******************************************************************************/\n.button-raised {\n    -fx-effect: dropshadow(gaussian, rgba(0.0, 0.0, 0.0, 0.30), 6.0, 0.3, 0, 1);\n    -fx-background-color: rgb(250, 250, 250);\n}\n\n.button-flat {\n    -fx-effect: null;\n    -fx-background-color: transparent;\n}\n\n.toggle-button, .button {\n    -fx-text-fill: -fx-text-button-normal;\n    -fx-font-weight: bold;\n    -fx-background-insets: 0.0;\n    -fx-background-radius: 0.0;\n    -fx-alignment: CENTER;\n    -fx-background-color: rgba(250, 250, 250);\n    -fx-effect: dropshadow(gaussian, rgba(0.0, 0.0, 0.0, 0.30), 6.0, 0.3, 0, 2);\n}\n\n.toggle-button:selected, .button:selected {\n    -fx-background-color: -swatch-500;\n    -fx-text-fill: white;\n}\n\n.button-raised .button .text, .button-flat .button .text {\n    -fx-text-weight: Bold;\n}\n\n.button:default {\n    -fx-background-color: -swatch-500;\n    -fx-text-fill: white;\n}\n\n.toggle-button:focused, .button:focused, .button:default:focused {\n    -fx-background-color: rgb(240, 240, 240);\n    -fx-text-fill: -swatch-500;\n}\n\n.toggle-button:focused:selected {\n    -fx-background-color: derive(-swatch-500, -12%);\n    -fx-text-fill: white;\n}\n\n.toggle-button:armed, .toggle-button:selected, .button:armed, .button:default:armed {\n    -fx-background-color: -swatch-500;\n    -fx-text-fill: white;\n}\n\n.icon-button {\n    -fx-background-color: transparent;\n    -fx-padding: 0;\n}\n\n.icon-button .text {\n    -fx-font-family: 'MaterialDesignIconicFont';\n}\n\n/*******************************************************************************\n *                                                                             *\n * ComboBox, ChoiceBox COMMON                                                  *\n *                                                                             *\n ******************************************************************************/\n.combo-box-base, .choice-box {\n    -fx-background-color: transparent;\n    -fx-border-color: -swatch-gray;\n    -fx-border-width: 0 0 2 0;\n    -fx-background-radius: 0;\n    -fx-border-radius: 0;\n}\n\n.combo-box:focused, .choice-box:focused {\n    -fx-border-color: -swatch-500;\n}\n\n.combo-box-base > .arrow-button, .choice-box > .open-button {\n    -fx-background-radius: 0.0 2.0 2.0 0.0;\n}\n\n.combo-box-base > .arrow-button > .arrow, .choice-box > .open-button > .arrow {\n    -fx-background-color: -swatch-200;\n}\n\n.combo-box-base .arrow-button:hover .arrow, .spinner .increment-arrow-button:hover .increment-arrow,\n.spinner .decrement-arrow-button:hover .decrement-arrow {\n    -fx-background-color: -swatch-400;\n}\n\n.menu-item:focused {\n    -fx-background-color: -swatch-light-gray;\n}\n\n/*******************************************************************************\n *                                                                             *\n * CheckBox                                                                    *\n *                                                                             *\n ******************************************************************************/\n.check-box .text {\n    -fx-fill: -fx-text-base-color;\n}\n\n.check-box > .box, .check-box > .box.unfocused, .check-box:disabled > .box,\n.check-box:indeterminate > .box {\n    -fx-border-radius: 4.0;\n    -fx-border-color: -swatch-gray;\n    -fx-border-width: 2;\n    -fx-background-radius: 4;\n    -fx-background-color: transparent;\n}\n\n.check-box:selected > .box {\n    -fx-border-color: -swatch-500;\n    -fx-background-color: -swatch-500;\n}\n\n.check-box:focused > .box {\n    -fx-effect: dropshadow(gaussian, rgba(0.0, 0.0, 0.0, 0.30), 6.0, 0.3, 0, 0);\n}\n\n.check-box:selected > .box > .mark {\n    -fx-background-color: white;\n}\n\n.check-box:indeterminate > .box > .mark {\n    -fx-background-color: -swatch-gray;\n}\n\n/*******************************************************************************\n *                                                                             *\n * ChoiceBox                                                                   *\n *                                                                             *\n ******************************************************************************/\n.context-menu {\n    -fx-background-color: rgba(255.0, 255.0, 255.0, 0.95);\n}\n\n.radio-menu-item:checked .label, .check-menu-item:checked .label {\n    -fx-text-fill: -swatch-500;\n}\n\n.radio-menu-item:checked > .left-container > .radio, .check-menu-item:checked > .left-container > .check {\n    -fx-background-color: -swatch-dark-gray;\n    -fx-effect: dropshadow(gaussian, rgba(0.0, 0.0, 0.0, 0.2), 0.0, 0.0, 0.0, 1.0);\n}\n\n.radio-menu-item > .left-container > .radio, .check-menu-item > .left-container > .check {\n    -fx-background-color: transparent;\n}\n\n/*******************************************************************************\n *                                                                             *\n * ComboBox                                                                    *\n *                                                                             *\n ******************************************************************************/\n.popup-overlay {\n    -fx-background-color: white;\n    -fx-border-color: -swatch-gray;\n    -fx-border-width: 0 0 2 0;\n    -fx-background-radius: 0;\n    -fx-border-radius: 0;\n}\n\n.title-bar .icon {\n    -fx-alignment: center-left;\n    -fx-effect: dropshadow(gaussian, rgba(0.0, 0.0, 0.0, 0.2), 0.0, 0.0, 0.0, 1.0);\n}\n\n.title-bar .title-label {\n    -fx-alignment: center;\n    -fx-font-weight: bolder;\n}\n\n.content-area {\n    -fx-background-color: -dark;\n}\n\n.content-background {\n    -fx-background-color: white;\n    -fx-background-radius: 0.0 0.0 11.0 11.0;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Date Picker                                                                 *\n *                                                                             *\n ******************************************************************************/\n.date-picker-popup .button {\n    -fx-background-color: -swatch-500;\n}\n\n.date-picker-popup > .month-year-pane {\n    -fx-background-color: -swatch-500;\n}\n\n.date-picker-popup > * > .spinner > .button > .left-arrow, .date-picker-popup > * > .spinner > .button > .right-arrow {\n    -fx-background-color: white;\n}\n\n.date-picker-popup > * > .spinner {\n    -fx-border-width: 0;\n}\n\n.date-picker-popup > * > .spinner > .label {\n    -fx-text-fill: white;\n    -fx-font-weight: bold;\n}\n\n.date-picker-popup > * > .day-name-cell, .date-picker-popup > * > .week-number-cell {\n    -fx-font-weight: normal;\n    -fx-text-fill: -swatch-dark-gray;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Date picker                                                                 *\n *                                                                             *\n ******************************************************************************/\n.date-picker .arrow-button {\n    -fx-background-color: transparent;\n}\n\n.date-picker .arrow-button .arrow {\n    -fx-background-insets: -4;\n}\n\n.date-picker .date-picker-display-node {\n    -fx-border-width: 0;\n}\n\n/*******************************************************************************\n *                                                                             *\n * HTML Editor                                                                 *\n *                                                                             *\n ******************************************************************************/\n.html-editor {\n    -fx-background-color: white;\n    -fx-border-width: 2 0 2 0;\n    -fx-border-color: -swatch-gray;\n}\n\n.html-editor .web-view {\n    -fx-border-color: gray;\n    -fx-border-width: gray;\n}\n\n.web-view {\n    -fx-font-smoothing-type: gray;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Label                                                                       *\n *                                                                             *\n ******************************************************************************/\n.label {\n    -fx-text-fill: -fx-text-base-color;\n}\n\n.label:disabled {\n    -fx-opacity: -fx-disabled-opacity;\n}\n\n.label:show-mnemonics > .mnemonic-underline {\n    -fx-stroke: -fx-text-base-color;\n}\n\n/*******************************************************************************\n *                                                                             *\n * List, Tree, Table COMMON                                                    *\n *                                                                             *\n ******************************************************************************/\n.list-view:focused .list-cell:filled:focused:selected {\n    -fx-background-color: -swatch-300;\n    -fx-text-fill: -fx-text-base-color;\n}\n\n.list-view:hover .list-cell:filled:hover {\n    -fx-background-color: -swatch-light-gray;\n    -fx-text-fill: -fx-text-base-color;\n}\n\n.list-view .list-cell:filled:selected {\n    -fx-background-color: -swatch-200;\n}\n\n.list-view {\n    -fx-background-color: transparent;\n}\n\n.list-view .list-cell {\n    -fx-background-color: derive(-fx-base, 26.4%);\n}\n\n/*******************************************************************************\n *                                                                             *\n * ProgressBar                                                                 *\n *                                                                             *\n ******************************************************************************/\n.progress-bar > .track {\n    -fx-background-color: derive(-swatch-gray, 50.0%);\n    -fx-background-radius: 7.0;\n    -fx-padding: 0.0;\n    -fx-background-insets: 8 0 8 0;\n}\n\n.progress-bar > .bar {\n    -fx-background-color: -swatch-500;\n    -fx-background-radius: 7.0;\n    -fx-background-insets: 8 0 8 0;\n    -fx-border-width: 0.0;\n    -fx-effect: null;\n}\n\n.progress-bar:indeterminate > .bar {\n    -fx-background-color: derive(-swatch-500, 50.0%);\n    -fx-background-radius: 7.0;\n    -fx-background-insets: 8 0 8 0;\n    -fx-border-width: 0.0;\n    -fx-effect: null;\n}\n\n.progress-bar-success > .bar {\n    -fx-background-color: -confirmation-color-success;\n}\n\n.progress-bar-error > .bar {\n    -fx-background-color: -confirmation-color-error;\n}\n\n/*******************************************************************************\n *                                                                             *\n * ProgressIndicator                                                           *\n *                                                                             *\n ******************************************************************************/\n.progress-indicator > .spinner {\n    -fx-border-width: 0;\n}\n\n.progress-indicator > .determinate-indicator > .indicator {\n    -fx-background-color: rgba(255.0, 255.0, 255.0, 0.5);\n    -fx-padding: 0.0;\n}\n\n.progress-indicator > .determinate-indicator > .progress {\n    -fx-background-color: -swatch-500;\n}\n\n.progress-indicator > .determinate-indicator > .percentage {\n    -fx-fill: -fx-text-base-color;\n    -fx-translate-y: 0em;\n    -fx-padding: 0.0;\n}\n\n/*******************************************************************************\n *                                                                             *\n * RadioButton                                                                 *\n *                                                                             *\n ******************************************************************************/\n.radio-button .text {\n    -fx-fill: -fx-text-base-color;\n}\n\n.radio-button > .radio, .radio-button > .radio.unfocused, .radio-button:disabled > .radio,\n.radio-button:selected > .radio {\n    -fx-border-radius: 100.0;\n    -fx-border-color: -swatch-gray;\n    -fx-border-width: 2;\n    -fx-background-radius: 100;\n    -fx-background-color: transparent;\n}\n\n.radio-button:focused > .radio {\n    -fx-background-color: -swatch-100;\n}\n\n.radio-button:focused:armed > .radio {\n    -fx-background-color: -swatch-100;\n}\n\n.radio-button:selected > .radio > .dot {\n    -fx-background-color: -swatch-500;\n    -fx-background-insets: 0;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Separators                                                                  *\n *                                                                             *\n ******************************************************************************/\n.separator {\n}\n\n/*******************************************************************************\n *                                                                             *\n * Split panes                                                                 *\n *                                                                             *\n ******************************************************************************/\n.split-pane {\n}\n\n.split-pane > .split-pane-divider {\n    -fx-background-color: -swatch-light-gray;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Scroll Bar                                                                  *\n *                                                                             *\n ******************************************************************************/\n.scroll-bar:vertical > .track-background, .scroll-bar:horizontal > .track-background {\n    -fx-background-color: -swatch-light-gray;\n    -fx-background-insets: 0.0;\n}\n\n.scroll-bar:vertical > .thumb, .scroll-bar:horizontal > .thumb {\n    -fx-background-color: -swatch-gray;\n    -fx-background-insets: 0.0;\n    -fx-background-radius: 4.0;\n}\n\n.scroll-bar > .increment-button, .scroll-bar > .decrement-button,\n.scroll-bar:hover > .increment-button, .scroll-bar:hover > .decrement-button {\n    -fx-background-color: transparent;\n}\n\n.scroll-bar > .increment-button > .increment-arrow, .scroll-bar > .decrement-button > .decrement-arrow {\n    -fx-background-color: -swatch-dark-gray;\n}\n\n.scroll-bar > .track-background {\n    -fx-background-color: transparent;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Slider                                                                      *\n *                                                                             *\n ******************************************************************************/\n\n.slider > .track {\n    -fx-background-color: -swatch-gray;\n    -fx-background-insets: 1.5;\n}\n\n.slider > .thumb {\n    -fx-background-color: -swatch-500;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Spinner                                                                     *\n *                                                                             *\n ******************************************************************************/\n.spinner {\n    -fx-background-color: transparent;\n    -fx-border-width: 0 0 2 0;\n    -fx-border-color: -swatch-gray;\n}\n\n.spinner:focused {\n    -fx-border-color: -swatch-500;\n}\n\n.spinner .text-field {\n    -fx-background-color: transparent;\n    -fx-background-radius: 0;\n    -fx-border-width: 0;\n    -fx-prompt-text-fill: derive(-dark, 50.0%);\n    -fx-highlight-fill: rgb(94.0, 203.0, 234.0);\n}\n\n.spinner .increment-arrow-button, .spinner .decrement-arrow-button {\n    -fx-background-color: transparent;\n    -fx-fill: swatch-500;\n}\n\n.spinner .increment-arrow-button .increment-arrow, .spinner .decrement-arrow-button .decrement-arrow {\n    -fx-background-color: -swatch-gray;\n}\n\n.spinner .increment-arrow-button, .spinner .decrement-arrow-button {\n    -fx-background-color: transparent;\n    -fx-fill: swatch-500;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Tables                                                                      *\n *                                                                             *\n ******************************************************************************/\n.table-view, .tree-table-view {\n    /* Constants used throughout the tableview. */\n    -fx-table-header-border-color: transparent;\n    -fx-table-cell-border-color: -fx-box-border; /* Horizontal Lines*/\n    -fx-background-color: transparent;\n}\n\n/* The column header row is made up of a number of column-header, one for each\n   TableColumn, and a 'filler' area that extends from the right-most column\n   to the edge of the tableview, or up to the 'column control' button. */\n.table-view .filler, .tree-table-view .filler, .table-view .column-header,\n.tree-table-view .column-header {\n    -fx-border-style: null;\n    -fx-border-color: -swatch-gray;\n    -fx-border-width: 0 0 2 0;\n    -fx-background-color: transparent;\n}\n\n.table-view .show-hide-columns-button, .tree-table-view .show-hide-columns-button {\n    -fx-background-color: transparent;\n}\n\n.table-view .column-header .label, .table-view .filler .label,\n.table-view .column-drag-header .label, .tree-table-view .column-header .label,\n.tree-table-view .filler .label, .tree-table-view .column-drag-header .label {\n    -fx-alignment: CENTER_LEFT;\n}\n\n.table-view .column-header-background, .tree-table-view .column-header-background {\n    -fx-background-color: transparent;\n}\n\n.table-cell {\n    -fx-border-color: transparent; /* Vertical Lines*/\n    -fx-border-width: 1;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Text, Text field & Text area                                                *\n *                                                                             *\n ******************************************************************************/\n.text {\n    -fx-font-smoothing-type: gray;\n}\n\n.text-area, .text-field {\n    -underline-color: -swatch-gray;\n    -fx-background-color: transparent;\n    -fx-background-radius: 2.0;\n    -fx-border-color: -underline-color;\n    -fx-border-width: 0 0 2 0;\n    -fx-prompt-text-fill: derive(-dark, 50.0%);\n    -fx-highlight-fill: rgb(94.0, 203.0, 234.0);\n}\n\n.text-area .text, .text-field > * > .text {\n    -fx-effect: null;\n    -fx-fill: -dark;\n}\n\n.text-area .content {\n    -fx-border-width: 0.0;\n    -fx-background-color: transparent;\n}\n\n.text-area:focused .content {\n    -fx-background-color: transparent;\n}\n\n.text-area:focused, .text-field:focused {\n    -fx-border-color: -swatch-500;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Tool bar    & Menu bar                                                      *\n *                                                                             *\n ******************************************************************************/\n.tool-bar, .menu-bar { /* top */\n    -fx-background-color: -swatch-toolbar;\n    -fx-border-width: 0 0 2 0;\n    -fx-border-color: -swatch-gray;\n    -fx-alignment: CENTER_LEFT;\n}\n\n.tool-bar .combo-box-base, .menu-bar .combo-base {\n    -fx-border-width: 0;\n}\n\n.tool-bar .button, .tool-bar .toggle-button {\n    -fx-background-color: -swatch-toolbar;\n    -fx-text-fill: -fx-text-base-color;\n    -fx-padding: 0;\n    -fx-background-radius: 0;\n}\n\n.tool-bar .button:pressed, .tool-bar .toggle-button:pressed, .tool-bar .toggle-button:selected {\n    -fx-background-color: -swatch-gray;\n}\n\n.tool-bar .toggle-button {\n    -fx-background-color: -swatch-toolbar;\n}\n\n.toolbar-colored {\n    -fx-background-color: -swatch-500;\n    -fx-border-width: 0 0 2 0;\n    -fx-border-color: -swatch-gray;\n}\n\n.toolbar-colored .button, .toolbar-colored .toggle-button {\n    -fx-background-color: -swatch-500;\n    -fx-text-fill: white;\n}\n\n.toolbar-colored .button:pressed, .toolbar-colored .toggle-button:pressed,\n.toolbar-colored .toggle-button:selected {\n    -fx-background-color: -swatch-200;\n}\n\n.toolbar-colored .text {\n    -fx-fill: white;\n}\n\n/*******************************************************************************\n *                                                                             *\n * Tabs                                                                        *\n *                                                                             *\n ******************************************************************************/\n.tab-pane {\n    -fx-border-color: black;\n}\n\n.tab-header-background {\n    -fx-background-color: -swatch-500;\n}\n\n.tab {\n    -fx-background-color: transparent, -swatch-300, -swatch-dark-gray;\n}\n\n.tab:selected {\n    -fx-background-color: transparent;\n}\n\n.tab .tab-close-button {\n    -fx-background-color: white;\n}\n\n.tab .tab-label {\n    -fx-text-fill: white;\n}\n\n.tab .tab-label {\n    /*\n     * Set a border to the same color as the background to keep the label text in the same position\n     * when the tab is selected.\n     */\n    -fx-border-width: 0 0 0.125em 0;\n    -fx-border-color: -swatch-500;\n}\n\n.tab:selected .tab-label {\n    -fx-border-color: -fx-text-fill;\n}\n")
            }
            show()
        }

        val modelName = "32_32_1_conv_sequential.h5"
        val (model, path) = loadModel(modelName)

        get<JobDb>().create(
            name = "AWS Job6",
            status = TrainingScriptProgress.NotStarted,
            userOldModelPath = ModelSource.FromFile(FilePath.S3(modelName)),
            userDataset = Dataset.ExampleDataset.FashionMnist,
            userOptimizer = Optimizer.Adam(
                learningRate = 0.001,
                beta1 = 0.9,
                beta2 = 0.999,
                epsilon = 1e-7,
                amsGrad = false
            ),
            userLoss = Loss.SparseCategoricalCrossentropy,
            userMetrics = setOf("accuracy"),
            userEpochs = 1,
            userNewModel = model,
            generateDebugComments = false,
            datasetPlugin = DatasetPlugins.datasetPassthroughPlugin,
            internalTrainingMethod = InternalJobTrainingMethod.Untrained,
            target = ModelDeploymentTarget.Desktop
        )
    }

    companion object {
        fun loadModel(modelName: String): Pair<Model, String> {
            val localModelPath =
                Paths.get("/Users/austinshalit/git/Axon/training/src/test/resources/edu/wpi/axon/training/$modelName")
                    .toString()
            val layers =
                ModelLoaderFactory().createModelLoader(localModelPath).load(File(localModelPath))
            val model = layers.attempt().unsafeRunSync()
            check(model is Either.Right)
            return model.b to localModelPath
        }
    }
}
