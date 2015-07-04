package es.ucm.fdi.emf.model.ed2.diagram.edit.policies;

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gef.commands.Command;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.diagram.core.commands.DeleteCommand;
import org.eclipse.gmf.runtime.emf.commands.core.command.CompositeTransactionalCommand;
import org.eclipse.gmf.runtime.emf.type.core.commands.DestroyElementCommand;
import org.eclipse.gmf.runtime.emf.type.core.commands.DestroyReferenceCommand;
import org.eclipse.gmf.runtime.emf.type.core.requests.CreateRelationshipRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.DestroyElementRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.DestroyReferenceRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.ReorientReferenceRelationshipRequest;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.View;

import es.ucm.fdi.emf.model.ed2.diagram.edit.commands.ED2TreeElementsCreateCommand;
import es.ucm.fdi.emf.model.ed2.diagram.edit.commands.ED2TreeElementsReorientCommand;
import es.ucm.fdi.emf.model.ed2.diagram.edit.commands.NodeLeavesCreateCommand;
import es.ucm.fdi.emf.model.ed2.diagram.edit.commands.NodeLeavesReorientCommand;
import es.ucm.fdi.emf.model.ed2.diagram.edit.parts.ED2TreeElementsEditPart;
import es.ucm.fdi.emf.model.ed2.diagram.edit.parts.NodeLeavesEditPart;
import es.ucm.fdi.emf.model.ed2.diagram.part.Ed2VisualIDRegistry;
import es.ucm.fdi.emf.model.ed2.diagram.providers.Ed2ElementTypes;

/**
 * @generated
 */
public class LeafItemSemanticEditPolicy extends Ed2BaseItemSemanticEditPolicy {

	/**
	 * @generated
	 */
	public LeafItemSemanticEditPolicy() {
		super(Ed2ElementTypes.Leaf_2007);
	}

	/**
	 * @generated
	 */
	protected Command getDestroyElementCommand(DestroyElementRequest req) {
		View view = (View) getHost().getModel();
		CompositeTransactionalCommand cmd = new CompositeTransactionalCommand(
				getEditingDomain(), null);
		cmd.setTransactionNestingEnabled(false);
		for (Iterator<?> it = view.getTargetEdges().iterator(); it.hasNext();) {
			Edge incomingLink = (Edge) it.next();
			if (Ed2VisualIDRegistry.getVisualID(incomingLink) == ED2TreeElementsEditPart.VISUAL_ID) {
				DestroyReferenceRequest r = new DestroyReferenceRequest(
						incomingLink.getSource().getElement(), null,
						incomingLink.getTarget().getElement(), false);
				cmd.add(new DestroyReferenceCommand(r) {
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						EObject referencedObject = getReferencedObject();
						Resource resource = referencedObject.eResource();
						CommandResult result = super.doExecuteWithResult(
								progressMonitor, info);
						if (resource != null) {
							resource.getContents().add(referencedObject);
						}
						return result;
					}
				});
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
			if (Ed2VisualIDRegistry.getVisualID(incomingLink) == NodeLeavesEditPart.VISUAL_ID) {
				DestroyReferenceRequest r = new DestroyReferenceRequest(
						incomingLink.getSource().getElement(), null,
						incomingLink.getTarget().getElement(), false);
				cmd.add(new DestroyReferenceCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
		}
		EAnnotation annotation = view.getEAnnotation("Shortcut"); //$NON-NLS-1$
		if (annotation == null) {
			// there are indirectly referenced children, need extra commands: false
			addDestroyShortcutsCommand(cmd, view);
			// delete host element
			cmd.add(new DestroyElementCommand(req));
		} else {
			cmd.add(new DeleteCommand(getEditingDomain(), view));
		}
		return getGEFWrapper(cmd.reduce());
	}

	/**
	 * @generated
	 */
	protected Command getCreateRelationshipCommand(CreateRelationshipRequest req) {
		Command command = req.getTarget() == null ? getStartCreateRelationshipCommand(req)
				: getCompleteCreateRelationshipCommand(req);
		return command != null ? command : super
				.getCreateRelationshipCommand(req);
	}

	/**
	 * @generated
	 */
	protected Command getStartCreateRelationshipCommand(
			CreateRelationshipRequest req) {
		if (Ed2ElementTypes.ED2TreeElements_4001 == req.getElementType()) {
			return null;
		}
		if (Ed2ElementTypes.NodeLeaves_4003 == req.getElementType()) {
			return null;
		}
		return null;
	}

	/**
	 * @generated
	 */
	protected Command getCompleteCreateRelationshipCommand(
			CreateRelationshipRequest req) {
		if (Ed2ElementTypes.ED2TreeElements_4001 == req.getElementType()) {
			return getGEFWrapper(new ED2TreeElementsCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		if (Ed2ElementTypes.NodeLeaves_4003 == req.getElementType()) {
			return getGEFWrapper(new NodeLeavesCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		return null;
	}

	/**
	 * Returns command to reorient EReference based link. New link target or source
	 * should be the domain model element associated with this node.
	 * 
	 * @generated
	 */
	protected Command getReorientReferenceRelationshipCommand(
			ReorientReferenceRelationshipRequest req) {
		switch (getVisualID(req)) {
		case ED2TreeElementsEditPart.VISUAL_ID:
			return getGEFWrapper(new ED2TreeElementsReorientCommand(req));
		case NodeLeavesEditPart.VISUAL_ID:
			return getGEFWrapper(new NodeLeavesReorientCommand(req));
		}
		return super.getReorientReferenceRelationshipCommand(req);
	}

}
