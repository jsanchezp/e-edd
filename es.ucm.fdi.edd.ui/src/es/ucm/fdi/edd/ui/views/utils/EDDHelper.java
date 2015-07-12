package es.ucm.fdi.edd.ui.views.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.diagram.core.services.ViewService;
import org.eclipse.gmf.runtime.emf.commands.core.command.AbstractTransactionalCommand;
import org.eclipse.gmf.runtime.emf.core.GMFEditingDomainFactory;
import org.eclipse.gmf.runtime.notation.Bounds;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.NotationFactory;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.gmf.runtime.notation.RelativeBendpoints;
import org.eclipse.gmf.runtime.notation.datatype.RelativeBendpoint;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import es.ucm.fdi.edd.core.erlang.Erlang2Java;
import es.ucm.fdi.edd.core.erlang.ErlangDeclarativeDebugger;
import es.ucm.fdi.edd.core.exception.EDDException;
import es.ucm.fdi.edd.core.graphviz.GraphViz;
import es.ucm.fdi.edd.core.json.model.Edges;
import es.ucm.fdi.edd.core.json.model.JsonDocument;
import es.ucm.fdi.edd.core.json.model.Vertices;
import es.ucm.fdi.edd.core.json.utils.JsonHelper;
import es.ucm.fdi.edd.core.json.utils.JsonUtils;
import es.ucm.fdi.edd.core.util.FileManager;
import es.ucm.fdi.edd.ui.Activator;
import es.ucm.fdi.emf.model.ed2.ED2;
import es.ucm.fdi.emf.model.ed2.Ed2Factory;
import es.ucm.fdi.emf.model.ed2.Leaf;
import es.ucm.fdi.emf.model.ed2.Model;
import es.ucm.fdi.emf.model.ed2.Node;
import es.ucm.fdi.emf.model.ed2.TreeElement;
import es.ucm.fdi.emf.model.ed2.diagram.edit.parts.ModelEditPart;
import es.ucm.fdi.emf.model.ed2.diagram.part.Ed2DiagramEditorPlugin;
import es.ucm.fdi.emf.model.ed2.diagram.part.Ed2DiagramEditorUtil;
import es.ucm.fdi.emf.model.ed2.diagram.part.Ed2VisualIDRegistry;

/**
 * EDD Helper 
 */
public class EDDHelper {
	
	private static final int TMAX = 10; // 10 seconds.
	
	private Erlang2Java e2j;
	private JsonDocument document;
	
	/**
	 * 
	 */
	public EDDHelper() {
		e2j = new Erlang2Java(); 
	}

	/**
	 * @throws Exception 
	 */
	public boolean startEDDebugger(String buggyCall, String location) {
		e2j.initialize(buggyCall, location);
//		if (e2j.isLoaded()) {
		{
			// FIXME Revisar éste algoritmo, espera 5s. 
			boolean finished = false;
			int wait = 0;
			while(!finished && wait<TMAX) {
				String debugTree = e2j.getDebugTree();
				if (debugTree != null) {
					document = getJsonDocument(debugTree);
					finished = true;
						
					return true;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				wait++;
			}
		}
		
		return false;
	}
	
	private JsonDocument getJsonDocument(String debugTree) {
		try {
			JsonDocument document = (JsonDocument) JsonHelper.readJsonFromString(debugTree, JsonDocument.class);	
			return document;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void writeJsonDocument(IPath path) throws EDDException {
		if (document == null) {
			throw new EDDException("The json document must not be null");
		}
		
		JsonUtils jsu = new JsonUtils();
		String content = jsu.toString(document);
		writeFile(content, path);
	}
	
	public void writeFile(String content, IPath path) {
		try {
			IFile iFile = Activator.getRoot().getFile(path);
			if (iFile.exists()) {
				iFile.delete(IResource.NONE, new NullProgressMonitor());
			}
			
			InputStream source = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
			iFile.create(source, IResource.NONE, new NullProgressMonitor());
			iFile.refreshLocal(IResource.DEPTH_INFINITE, null);			
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return
	 */
	public Integer getQuestionsSize() {
		if (document != null) {
			return document.getVertices().size();
		}
		
		return 0;
	}

	/**
	 * @param i
	 * @return
	 * 
	 * @throws EDDException 
	 */
	public String getQuestion(int index) throws EDDException {
		if (document == null) {
			throw new EDDException("The json document must not be null");
		}
		
		LinkedList<Vertices> vertices = document.getVertices();
		for (Vertices vertice : vertices) {
			int node = Integer.parseInt(vertice.getNode());
			if (node == index) {
				return vertice.getQuestion();
			}
		}
		
		return null;
	}
	
	public Integer getCurrentQuestion() {
		return e2j.getQuestionIndex();
	}
	
	public void setAnswer(String reply) {
		e2j.setAnswer(reply);
	}
	
	public boolean isBuggyNode() {
		return e2j.isBuggyNode();
	}
	
	public Integer getBuggyNode() {
		return e2j.getBuggyNodeIndex();
	}
	
	public String getOutput() {
		return e2j.getOutput();
	}
	
	/**
	 * @throws IOException 
	 * @throws EDDException 
	 */
	public String buildDOT(Integer highlightNode, boolean cw) throws IOException, EDDException {
		if (document == null) {
			throw new EDDException("The json document must not be null");
		}
		
		GraphViz gv = new GraphViz();
		gv.addln(gv.start_graph());
		gv.addln("\tgraph [dpi = 600]; ");
//		gv.addln("\tratio=\"fill\"; ");
		gv.addln("\tsize=\"8.3,11.7!\"; ");
		gv.addln("\tmargin = 0; ");
//		gv.addln("\tnode [shape=circle]; ");
		gv.addln("\tnode [style=filled]; ");
		gv.addln("\tnode [fillcolor=\"#EEEEEE\"]; "); // Relleno gris
//		gv.addln("\tnode [color=\"#EEEEEE\"]; "); // Borde gris
		gv.addln("\tedge [color=\"#7092BE\"]; ");
		
		LinkedList<Vertices> vertices = document.getVertices();
		for (int i=0; i<vertices.size(); i++) {
			Vertices vertice = vertices.get(i);
			String node = vertice.getNode();
			if (highlightNode !=null && Integer.parseInt(node) == highlightNode) {
				gv.addln(node + " [label=\"" + node + ". " + vertice.getQuestion() + "\", style=filled, fillcolor=\"#ED1C3A\"];");
			}
			else {
				gv.addln(node + " [label=\"" + node + ". " + vertice.getQuestion() + "\"];");
			}
		}
		
		LinkedList<Edges> edges = document.getEdges();
		for (Edges edge : edges) {
			int from = edge.getFrom();
			int to = edge.getTo();
			if (highlightNode !=null && from == highlightNode) {
				gv.addln(from + " -> "+ to + " [style=filled, color=\"#ED1C3A\", fillcolor=\"#ED1C3A\"];");
			}
			else {
				gv.addln(from + " -> "+ to + ";");
			}
		}
		
		// Para cambiar el sentido
		if (cw) {
			gv.addln("rankdir=LR;"); 
		}
		gv.addln(gv.end_graph());
		
		String dotSource = gv.getDotSource();
//		FileManager.writeToFile(file.getAbsolutePath(), dotSource);
		
//		String type = "gif";
//		File out = new File("files/output/images/sample1." + type);
//		gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
		
		return dotSource;
	}
	
	public Model buildEMF(String modelName, IPath ed2Path, IPath ed2DiagramPath) throws EDDException {
		if (document == null) {
			throw new EDDException("The json document must not be null");
		}
		
		Model model = Ed2Factory.eINSTANCE.createModel();
		ED2 ed2 = Ed2Factory.eINSTANCE.createED2();
		ed2.setName(modelName);
		model.setEd2(ed2);
		
		Map<Integer, Node> nodesMap = new HashMap<Integer, Node>();
		LinkedList<Vertices> vertices = document.getVertices();
		for (Vertices vertice : vertices) {
			Node node = Ed2Factory.eINSTANCE.createNode();
			int index = Integer.parseInt(vertice.getNode());
			node.setIndex(index);
			node.setName(index + ": " +vertice.getQuestion());
			
			nodesMap.put(index, node);
		}
		
		LinkedList<Edges> edges = document.getEdges();
		for (Edges edge : edges) {
			int from = edge.getFrom();
			int to = edge.getTo();
			
			Node source = nodesMap.get(from);
			Node target = nodesMap.get(to);
			source.getNodes().add(target);
		}
		
		
		EList<TreeElement> elements = ed2.getTreeElements();
//		elements.addAll(nodesMap.values());
		elements.add(nodesMap.get(90)); //FIXME Obtener nodos raíz...
		
		try {
			saveModel(ed2Path.toPortableString(), model);
			performFinish(ed2Path, ed2DiagramPath);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return model;
	}
	
	/**
	 * 
	 */
	public boolean performFinish(IPath ed2Path, IPath ed2DiagramPath) {
		List<EObject> root = null;
		try {
			IFile ed2File = Activator.getRoot().getFile(ed2Path);  
			root = loadModel(ed2File.getLocation().toPortableString());
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
		
		final Model model = (Model) root.get(0);
		LinkedList<IFile> affectedFiles = new LinkedList<IFile>();
		IFile diagramFile = createNewFile(ed2DiagramPath.toPortableString());
		Ed2DiagramEditorUtil.setCharset(diagramFile);
		affectedFiles.add(diagramFile);
		URI diagramModelURI = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		TransactionalEditingDomain editingDomain = GMFEditingDomainFactory.INSTANCE.createEditingDomain();
		ResourceSet resourceSet = editingDomain.getResourceSet();
		final Resource diagramResource = resourceSet.createResource(diagramModelURI);
		AbstractTransactionalCommand command = new AbstractTransactionalCommand(editingDomain, "Initializing diagram contents", affectedFiles) {
			@Override
			protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
				int diagramVID = Ed2VisualIDRegistry.getDiagramVisualID(model);
				if (diagramVID != ModelEditPart.VISUAL_ID) {
					return CommandResult.newErrorCommandResult("Incorrect model object stored as a root resource object");
				}
				Diagram diagram = ViewService.createDiagram(model, ModelEditPart.MODEL_ID, Ed2DiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT);
				diagramResource.getContents().add(diagram);
				return CommandResult.newOKCommandResult();
			}
		};
		
		try {
			OperationHistoryFactory.getOperationHistory().execute(command, new NullProgressMonitor(), null);
			diagramResource.save(Ed2DiagramEditorUtil.getSaveOptions());
			Ed2DiagramEditorUtil.openDiagram(diagramResource);
		} catch (ExecutionException e) {
			Ed2DiagramEditorPlugin.getInstance().logError("Unable to create model and diagram", e);
		} catch (IOException ex) {
			Ed2DiagramEditorPlugin.getInstance().logError("Save operation failed for: " + diagramModelURI, ex);
		} catch (PartInitException ex) {
			Ed2DiagramEditorPlugin.getInstance().logError("Unable to open editor", ex);
		}
		
		return true;
	}
	
	public static List<EObject> loadModel(String fileName) throws IOException {
		File source = new File(fileName);
		FileInputStream fis = new FileInputStream(source);
		
		URI uri = URI.createFileURI(fileName);
		Resource resource = RegisterPackage.resourceSet.createResource(uri);
		assert resource != null : "No se ha creado un resource para la ruta: " + fileName;
		resource.load(null);
		return resource.getContents();
	}
	
	public static void saveModel(String fileName, Model model) throws IOException {
		List<EObject> contents = new ArrayList<EObject>();
		contents.add(model);
		
		URI uri = URI.createFileURI(fileName);
		XMIResource resource = (XMIResource) RegisterPackage.resourceSet.createResource(uri);
		assert resource != null : "No se ha creado un resource para la ruta: " + fileName;
		List<EObject> contentsCopy = (List<EObject>) EcoreUtil.copyAll(contents);
		//generateIds(contentsCopy, (XMIResource) resource);
		resource.setEncoding(StandardCharsets.UTF_8.toString());
		resource.getContents().addAll(contentsCopy);
		resource.save(Collections.EMPTY_MAP);
	}
	
	protected EObject loadData(String fileName) throws FileNotFoundException, IOException {
		XMIResourceImpl resource = new XMIResourceImpl();
		File source = new File(fileName);
		System.out.println(source.getAbsolutePath());
		resource.load(new FileInputStream(source), new HashMap<Object, Object>());
		EObject data = resource.getContents().get(0);
		
		return data;
	}
	
	protected void saveData(String fileName, EObject data) throws IOException {
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("key", new XMIResourceFactoryImpl());

		ResourceSet resSet = new ResourceSetImpl();
		Resource resource = resSet.createResource(URI.createFileURI(fileName));
		resource.getContents().add(data);
		resource.save(Collections.EMPTY_MAP);
	}
	
	private Resource buildGMF(String name) {
//		URI diagramURI = URI.createPlatformResourceURI("platform:/resource/E-EDD/ed2/test.ed2_diagram", false);
//		URI modelURI = URI.createPlatformResourceURI("platform:/resource/E-EDD/ed2/test.ed2", false);
		URI diagramURI = URI.createPlatformResourceURI("/E-EDD/ed2/testGMF.ed2_diagram", false);
		URI modelURI = URI.createPlatformResourceURI("/E-EDD/ed2/testGMF.ed2", false);
		Resource diagramResource = Ed2DiagramEditorUtil.createDiagram(diagramURI, modelURI, new NullProgressMonitor());
		
		Diagram diagram = (Diagram) diagramResource.getContents().get(0);
		
		// ---------------------------------------------------------------------------
		TransactionalEditingDomain editingDomain = GMFEditingDomainFactory.INSTANCE.createEditingDomain();
		AbstractTransactionalCommand command = new AbstractTransactionalCommand(editingDomain, "Create content...", Collections.EMPTY_LIST) {
			protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
				int numberOfNodes = 10;
				for (int i = 0; i < numberOfNodes; i++) {
					diagram.createChild(NotationPackage.eINSTANCE.getNode());
				}
		
				for (int i = 0; i < 5; i++) {
					org.eclipse.gmf.runtime.notation.Node nodeA = (org.eclipse.gmf.runtime.notation.Node) diagram.getChildren().get(i);
					org.eclipse.gmf.runtime.notation.Node nodeB = (org.eclipse.gmf.runtime.notation.Node) diagram.getChildren().get(numberOfNodes - 1 - i);
		
					Edge edge = diagram.createEdge(NotationPackage.eINSTANCE.getEdge());
		
					edge.setSource(nodeA);
					edge.setTarget(nodeB);
					System.out.println("break");
				}
				
				return CommandResult.newOKCommandResult();
			}
		};
		
		TransactionalEditingDomain editDomain = TransactionUtil.getEditingDomain(diagramResource); //UMLModeler.getEditingDomain();
		editDomain.getCommandStack().execute(new RecordingCommand(editDomain, "Modify Operation") {
			@Override
			protected void doExecute() {
//				Model newModel = Ed2Factory.eINSTANCE.createModel();
				ED2 newED2 = Ed2Factory.eINSTANCE.createED2();
				Node newNode = Ed2Factory.eINSTANCE.createNode();
				Leaf newElement = Ed2Factory.eINSTANCE.createLeaf();
				
				diagramResource.getContents().add(newED2);
				diagramResource.getContents().add(newNode);
				diagramResource.getContents().add(newElement);
			}
		});

		// ----------------------------------------------------
		try {
			OperationHistoryFactory.getOperationHistory().execute(command, new NullProgressMonitor(), null);
			
			boolean openDiagram = Ed2DiagramEditorUtil.openDiagram(diagramResource);
//			 IWorkbenchPage.openEditor(URIEditorInput(), <editorID>); 
			
			diagramResource.save(Ed2DiagramEditorUtil.getSaveOptions());
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			Ed2DiagramEditorPlugin.getInstance().logError("Unable to create diagram content...", e);
		}
		
		
		return diagramResource;
	}
	
	private org.eclipse.gmf.runtime.notation.Node createExampleNode() {
		org.eclipse.gmf.runtime.notation.Node node = NotationFactory.eINSTANCE.createNode();
		Bounds bounds = NotationFactory.eINSTANCE.createBounds();
	    bounds.setHeight(1503);
	    bounds.setWidth(1979);
	    bounds.setX(777);
	    bounds.setY(888);

	    node.setVisible(true);
	    node.setMutable(false);
	    node.setLayoutConstraint(bounds);
	    return node;
	}
	
	private Edge createExampleEdge() {
		Edge edge = NotationFactory.eINSTANCE.createEdge();
	    edge.setVisible(true);
	    edge.setMutable(false);
	    
	    RelativeBendpoints bendpoints = NotationFactory.eINSTANCE.createRelativeBendpoints();
	    edge.setBendpoints(bendpoints);

	    List<RelativeBendpoint> points = new ArrayList<RelativeBendpoint>();
	    for (int i = 0; i < 10; i++) {
	      RelativeBendpoint bendPoint = new RelativeBendpoint(1, 2, 3, 4);
	      points.add(bendPoint);
	    }
	    
	    bendpoints.setPoints(points);
	    edge.setBendpoints(bendpoints);
	    
	    return edge;
	}

	/**
	 * @param model
	 * @param diagramPath
	 * @throws CoreException
	 */
	private void generateDiagram(Model model, String diagramPath) throws CoreException {
//		URI modelURI = URI.createPlatformResourceURI("/E-EDD/ed2/testAutoGMF.ed2", false);
//		URI diagramURI = URI.createPlatformResourceURI("/E-EDD/ed2/testAutoGMF.ed2_diagram", false);
//		Resource diagramResource = Ed2DiagramEditorUtil.createDiagram(diagramURI, modelURI, new NullProgressMonitor());
		
		LinkedList<IFile> affectedFiles = new LinkedList<IFile>();
		IFile diagramFile = createNewFile(diagramPath);
		System.out.println("Existe: " + diagramFile.exists());
		Ed2DiagramEditorUtil.setCharset(diagramFile);
		affectedFiles.add(diagramFile);
		
		TransactionalEditingDomain editingDomain = GMFEditingDomainFactory.INSTANCE.createEditingDomain();
		URI diagramModelURI = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		ResourceSet resourceSet = editingDomain.getResourceSet();
		final Resource diagramResource = resourceSet.createResource(diagramModelURI);
		AbstractTransactionalCommand command = new AbstractTransactionalCommand(editingDomain, "Initializing diagram contents", affectedFiles) {
			protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
				int diagramVID = Ed2VisualIDRegistry.getDiagramVisualID(model);
				if (diagramVID != ModelEditPart.VISUAL_ID) {
					return CommandResult.newErrorCommandResult("Incorrect model object stored as a root resource object");
				}
				Diagram diagram = ViewService.createDiagram(model, ModelEditPart.MODEL_ID, Ed2DiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT);
				diagramResource.getContents().add(diagram);
				return CommandResult.newOKCommandResult();
			}
		};
		try {
			OperationHistoryFactory.getOperationHistory().execute(command, new NullProgressMonitor(), null);
			diagramResource.save(Ed2DiagramEditorUtil.getSaveOptions());
			Ed2DiagramEditorUtil.openDiagram(diagramResource);
		} catch (ExecutionException e) {
			Ed2DiagramEditorPlugin.getInstance().logError("Unable to create model and diagram", e);
		} catch (IOException ex) {
			Ed2DiagramEditorPlugin.getInstance().logError("Save operation failed for: " + diagramModelURI, ex);
		} catch (PartInitException ex) {
			Ed2DiagramEditorPlugin.getInstance().logError("Unable to open editor", ex);
		}
	}
	
	/**
	 * @param path
	 * @return
	 */
	public IFile createNewFile(String path) {
		final IPath filePath = new Path(path);
		final IFile newFile = Activator.getRoot().getFile(filePath);
		final InputStream initialContents = new ByteArrayInputStream(new byte[0]);
		
		try {
			CreateFileOperation op = new CreateFileOperation(newFile, null, initialContents, "New File");
			op.execute(new NullProgressMonitor(), WorkspaceUndoUtil.getUIInfoAdapter(new Shell()));
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return newFile;
	}	
}