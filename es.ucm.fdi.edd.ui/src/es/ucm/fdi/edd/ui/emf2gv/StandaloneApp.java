package es.ucm.fdi.edd.ui.emf2gv;

import java.io.File;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emftools.emf2gv.graphdesc.GVFigureDescription;
import org.emftools.emf2gv.graphdesc.GraphdescPackage;
import org.emftools.emf2gv.processor.core.StandaloneProcessor;

import es.ucm.fdi.edd.ui.views.utils.RegisterPackage;
import es.ucm.fdi.emf.model.ed2.Ed2Package;

public class StandaloneApp {
	
	private static final String UTF_8 = "UTF-8";
	private static final String PREFIX = "file:/";
	
	private URI modelURI;
	private URI graphDescriptorURI;
	private String workDirectory;
	private String graphFilename;
	
	public static void main(String[] args) {
		RegisterPackage.initializeMetamodel();
		
		String model = StandaloneApp.class.getResource("ackermann.ed2").toString();
		String graphDescriptor = StandaloneApp.class.getResource("ed2.graphdesc").toString();
		String workDirectory = "edd";
		String graphFilename = "ackermann.jpg";
		
		StandaloneApp app = new StandaloneApp(model, graphDescriptor, workDirectory, graphFilename);
		app.execute();
	}
	
	public StandaloneApp(String model, String workDirectory, String graphFilename) {
		String graphDescriptor = StandaloneApp.class.getResource("ed2.graphdesc").toString();
		this.modelURI = URI.createURI(PREFIX + model, true);
		this.graphDescriptorURI = URI.createURI(graphDescriptor, true); 
		this.workDirectory = workDirectory;
		this.graphFilename = graphFilename;
	}
	
	private StandaloneApp(String model, String graphDescriptor, String workDirectory, String graphFilename) {
		this.modelURI = URI.createURI(model, true);
		this.graphDescriptorURI = URI.createURI(graphDescriptor, true);
		this.workDirectory = workDirectory;
		this.graphFilename = graphFilename;
	}
	
	public void execute() {
		try {
			// Packages initialization
			Ed2Package.eINSTANCE.eClass();
			GraphdescPackage.eINSTANCE.eClass();

			// Load the model resource
			System.out.println("Loading the model...");
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION,	new XMIResourceFactoryImpl());
			Resource modelResource = rs.getResource(modelURI, true);

			// Load the graphical description
			System.out.println("Loading the graphical description...");
			Resource graphDescResource = rs.getResource(graphDescriptorURI, true);

			// Graphical description retrieval
			GVFigureDescription gvFigureDescription = (GVFigureDescription) graphDescResource.getContents().get(0);

			// Diagram generation
			System.out.println("Diagram generation...");
			File wsDirectory = new File(workDirectory);
			StandaloneProcessor.process(modelResource.getContents(), // model
					gvFigureDescription, // Figure description
					wsDirectory, // Work directory
					graphFilename, // diagram file
					null, // Callback
					null, // Icon provider
					null, // dot command
					true, // Add validation decorators ?
					false, // Keep generated Graphviz source file ?
					UTF_8, // Graphviz source encoding
					null, // Additional filters
					null, // ILogger
					null); // Progress monitor
			System.out.println("Done." + wsDirectory + File.separator + graphFilename);
		} catch (Throwable t) {
			System.err.println("An unexpected error occured");
			t.printStackTrace();
		}
	}

	public URI getModelURI() {
		return modelURI;
	}

	public void setModelURI(URI modelURI) {
		this.modelURI = modelURI;
	}

	public URI getGraphDescriptorURI() {
		return graphDescriptorURI;
	}

	public void setGraphDescriptorURI(URI graphDescriptorURI) {
		this.graphDescriptorURI = graphDescriptorURI;
	}

	public String getWorkDirectory() {
		return workDirectory;
	}

	public void setWorkDirectory(String workDirectory) {
		this.workDirectory = workDirectory;
	}

	public String getGraphFilename() {
		return graphFilename;
	}

	public void setGraphFilename(String graphFilename) {
		this.graphFilename = graphFilename;
	}
}