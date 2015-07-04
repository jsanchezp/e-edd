package es.ucm.fdi.emf.model.ed2.diagram.edit.policies;

import org.eclipse.gef.commands.Command;
import org.eclipse.gmf.runtime.emf.type.core.commands.DestroyReferenceCommand;
import org.eclipse.gmf.runtime.emf.type.core.requests.DestroyReferenceRequest;

import es.ucm.fdi.emf.model.ed2.diagram.providers.Ed2ElementTypes;

/**
 * @generated
 */
public class NodeLeavesItemSemanticEditPolicy extends
		Ed2BaseItemSemanticEditPolicy {

	/**
	 * @generated
	 */
	public NodeLeavesItemSemanticEditPolicy() {
		super(Ed2ElementTypes.NodeLeaves_4003);
	}

	/**
	 * @generated
	 */
	protected Command getDestroyReferenceCommand(DestroyReferenceRequest req) {
		return getGEFWrapper(new DestroyReferenceCommand(req));
	}

}
