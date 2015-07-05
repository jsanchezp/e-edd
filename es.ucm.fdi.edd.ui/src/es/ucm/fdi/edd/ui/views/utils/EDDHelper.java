package es.ucm.fdi.edd.ui.views.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import es.ucm.fdi.edd.core.graphviz.GraphViz;
import es.ucm.fdi.edd.core.json.model.Edges;
import es.ucm.fdi.edd.core.json.model.JsonDocument;
import es.ucm.fdi.edd.core.json.model.Vertices;
import es.ucm.fdi.edd.core.json.utils.JsonHelper;
import es.ucm.fdi.edd.core.util.FileManager;
import es.ucm.fdi.emf.model.ed2.ED2;
import es.ucm.fdi.emf.model.ed2.Ed2Factory;
import es.ucm.fdi.emf.model.ed2.Leaf;
import es.ucm.fdi.emf.model.ed2.Model;
import es.ucm.fdi.emf.model.ed2.Node;
import es.ucm.fdi.emf.model.ed2.TreeElement;
import es.ucm.fdi.emf.model.ed2.diagram.edit.parts.ModelEditPart;
import es.ucm.fdi.emf.model.ed2.diagram.part.Ed2DiagramEditorPlugin;
import es.ucm.fdi.emf.model.ed2.diagram.part.Ed2DiagramEditorUtil;
import es.ucm.fdi.emf.model.ed2.diagram.part.Messages;

/**
 * EDD Helper 
 */
public class EDDHelper {
	
	private File file;
	private JsonDocument document;

	/**
	 * @param file
	 */
	public EDDHelper(File file) {
		this.file = file;
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	public void startEDDebugger(String buggyCall, String location) throws Exception {
//		String[] args = new String[] {"ackermann:main([3,4])", "../examples/ackermann/"};
		String[] args = new String[] {buggyCall, location};
//		EDDJInterface.main(args);
	}
	
	/**
	 * 
	 */
	public void readJson() {
		this.document = readJsonFromFile();
	}
	
	/**
	 * @return
	 */
	private JsonDocument readJsonFromFile() {
		JsonDocument document = null;
		try {
			document = (JsonDocument) JsonHelper.readJson(file.getAbsolutePath(), JsonDocument.class);
			return document;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return document;
	}
	
	/**
	 * @throws IOException 
	 */
	public void buildDOT(File file, int highlightNode, boolean cw) throws IOException {
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
			if (Integer.parseInt(node) == highlightNode) {
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
			if (from == highlightNode) {
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
		FileManager.writeToFile(file.getAbsolutePath(), dotSource);
		
//		String type = "gif";
//		File out = new File("files/output/images/sample1." + type);
//		gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
	}
	
	public ED2 buildEMF(String name) {
		ED2 root = Ed2Factory.eINSTANCE.createED2();
		root.setName(name);
		
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
		
		EList<TreeElement> elements = root.getTreeElements();
		elements.add(nodesMap.get(90)); // Raíz
		
		return root;
	}
	
	public Resource buildGMF(String name) {
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
	 * @return
	 */
	public Integer getQuestionsSize() {
		return document.getVertices().size();
	}

	/**
	 * @param i
	 * @return
	 */
	public String getQuestion(int index) {
		LinkedList<Vertices> vertices = document.getVertices();
		for (Vertices vertice : vertices) {
			int node = Integer.parseInt(vertice.getNode());
			if (node == index) {
				return vertice.getQuestion();
			}
		}
		
		return null;
	}
}