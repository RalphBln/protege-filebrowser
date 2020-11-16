package de.pooky.protege.plugin.menu;

import org.protege.editor.core.ProtegeManager;
import org.protege.editor.core.platform.OSUtils;
import org.protege.editor.core.ui.workspace.Workspace;
import org.protege.editor.core.ui.workspace.WorkspaceFrame;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class ShowInFileBrowserAction extends ProtegeOWLAction implements OWLModelManagerListener {

	private static final Logger log = LoggerFactory.getLogger(ShowInFileBrowserAction.class);

	public void initialise() {
		getOWLModelManager().addListener(this);
	}

	public void dispose() {
		getOWLModelManager().removeListener(this);
	}

	public void actionPerformed(ActionEvent event) {
		OWLOntology activeOntology = getOWLEditorKit().getOWLModelManager().getActiveOntology();
		IRI physicalLocationIRI = activeOntology.getOWLOntologyManager().getOntologyDocumentIRI(activeOntology);
		if ("file".equalsIgnoreCase(physicalLocationIRI.getScheme())) {
			URI physicalLocation = physicalLocationIRI.toURI();
			String path = physicalLocation.getPath();
			if (path != null) {
				File ontologyFile = new File(path);
				if (ontologyFile.exists()) {
					if (OSUtils.isOSX()) {
						try {
							Runtime.getRuntime().exec(new String[] { "/usr/bin/open", "-R", ontologyFile.getAbsolutePath() });
						} catch (IOException e) {
							log.error("Could not show file in Finder" , e);
						}
					} else if (OSUtils.isWindows()) {
						try {
							Runtime.getRuntime().exec(new String[] { "explorer.exe", "/select,\"" + ontologyFile.getAbsolutePath() + "\"" } );
						} catch (IOException e) {
							log.error("Could not show file in Explorer" , e);
						}
					} else if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().open(ontologyFile);
						} catch (IOException e) {
							log.error("Could not show file in file browser" , e);
						}
					}
				}
			}
		}
	}

	@Override
	public void handleChange(OWLModelManagerChangeEvent event) {
		try {
			if (event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED) || event.isType(EventType.ONTOLOGY_LOADED)) {
				OWLOntology activeOntology = event.getSource().getActiveOntology();

				// Apparently, the ACTIVE_ONTOLOGY_CHANGED event is fired before the UI is completely constructed.
				// As of now, I don't know of a better way to wait until the UI is ready and the menu is there.
				// TODO if there is something like a "UI ready" event, listen for that instead
				new Thread( () ->  {
					// Enable/disable menu item depending on whether currently active ontology file is stored locally or not
					WorkspaceFrame frame = ProtegeManager.getInstance().getFrame(getWorkspace());
					while (frame == null) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							log.error("Interrupted", e);
						}
						frame = ProtegeManager.getInstance().getFrame(getWorkspace());
					}
					JMenu fileMenu = frame.getMenu(Workspace.FILE_MENU_NAME);
					int count = fileMenu.getItemCount();
					// find the menu item we want to enable/disable
					for (int i = 0; i != count; i++) {
						JMenuItem item = fileMenu.getItem(i);
						// weirdly, item may be null (null represents a separator)
						if (item != null && item.getText().equals("Show in file browser")) {
							// find out if ontology location is on local hdd and enable menu item, otherwise diable
							IRI physicalLocation = activeOntology.getOWLOntologyManager().getOntologyDocumentIRI(activeOntology);
							item.setEnabled("file".equalsIgnoreCase(physicalLocation.getScheme()));
							break;
						}
					}
				}).start();
			}
		} catch (Throwable t) {
			log.error("Oops.", t);
		}
	}

}
