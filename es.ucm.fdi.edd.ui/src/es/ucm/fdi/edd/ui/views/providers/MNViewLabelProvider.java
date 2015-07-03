package es.ucm.fdi.edd.ui.views.providers;

import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import es.ucm.fdi.emf.model.ed2.Node;

/**
 * A decorating label provider is a label provider which combines a nested label provider and an optional decorator.
 * The decorator decorates the label text, image, font and colors provided by the nested label provider.
 */
public class MNViewLabelProvider extends AdapterFactoryLabelProvider {
	
    /** Image descriptor for warning overlay. */
    private static final ImageDescriptor WARNING_IMG = AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipse.ui", "$nl$/icons/full/ovr16/warning_ovr.gif");

    /** The image decorator. */
    private Image decoratedImage;	

	/**
	 * Creates a new instance of {@link MNViewLabelProvider}.
	 */
	public MNViewLabelProvider() {
		super(MNComposedAdapterFactory.getAdapterFactory());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		Image image = super.getImage(element);
		if (element instanceof Node) {
			Node item = (Node) element;
            if (item.getNodes().size() > 0) {
                if (decoratedImage == null) {
                    DecorationOverlayIcon icon = new DecorationOverlayIcon(image, WARNING_IMG, IDecoration.TOP_LEFT);
                    decoratedImage = icon.createImage();
                }
                return decoratedImage;
            }
        }

        return image;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		return super.getText(element);
	}
	
	/** {@inheritDoc} */
    @Override
    public void dispose() {
        super.dispose();
        decoratedImage.dispose();
    }
}