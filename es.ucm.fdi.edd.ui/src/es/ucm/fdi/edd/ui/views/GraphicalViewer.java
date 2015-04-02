package es.ucm.fdi.edd.ui.views;

import java.awt.geom.AffineTransform;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.ConstructorUtils;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

import com.abstratt.imageviewer.Activator;
import com.abstratt.imageviewer.IGraphicalContentProvider;

/**
 * A viewer that knows how to display graphical contents.
 * 
 * @see IGraphicalContentProvider
 */
public class GraphicalViewer extends ContentViewer {

	private Canvas canvas;

	private boolean adjustToCanvas = true;

	private boolean imageRedrawRequested;
	
	private float scale = 1;
	final Point origin = new Point (0, 0);

	public GraphicalViewer(Composite parent) {
		canvas = new Canvas(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BACKGROUND);
		parent.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				canvas.setBounds(canvas.getParent().getClientArea());
				requestImageRedraw();
			}
		});
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				redrawImageIfRequested();
				GC gc = e.gc;
				paintCanvas(gc);
			}
		});
		initScrollBars();
		
		Listener listener = new Listener() {
			int zoomFactor = 50;
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.MouseWheel:
					if (event.count > 0) { 
						scale += .2f;
					} else {
						scale -= .2f;
					}
					scale = Math.max(0, scale + event.count);
					
					zoomFactor = Math.max(0, zoomFactor + event.count);
//					Canvas canvas = (Canvas)event.widget;
					canvas.redraw();
					break;
				case SWT.Paint:
					event.gc.drawText("Zoom = " + zoomFactor + " : " +scale, 10, 10);
					break;
				}
			}
		};
		canvas.addListener(SWT.MouseWheel, listener);
		canvas.addListener(SWT.Paint, listener);
	}
	
	/* Initalize the scrollbar and register listeners. */
	private void initScrollBars() {
		ScrollBar horizontal = canvas.getHorizontalBar();
		horizontal.setEnabled(false);
		horizontal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				scrollHorizontally((ScrollBar) event.widget);
			}
		});
		ScrollBar vertical = canvas.getVerticalBar();
		vertical.setEnabled(false);
		vertical.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				scrollVertically((ScrollBar) event.widget);
			}
		});
	}
	
	/* Scroll horizontally */
	private void scrollHorizontally(ScrollBar scrollBar) {
		int hSelection = scrollBar.getSelection();
		int destX = -hSelection - origin.x;
		Rectangle rect = getImage(canvas.getSize()).getBounds();
		canvas.scroll(destX, 0, 0, 0, rect.width, rect.height, false);
		origin.x = -hSelection;
	}

	/* Scroll vertically */
	private void scrollVertically(ScrollBar scrollBar) {
		int vSelection = scrollBar.getSelection ();
		int destY = -vSelection - origin.y;
		Rectangle rect = getImage(canvas.getSize()).getBounds();
		canvas.scroll(0, destY, 0, 0, rect.width, rect.height, false);
		origin.y = -vSelection;
	}
	
	public Canvas getCanvas() {
		return canvas;
	}
	
	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
		canvas.redraw();
	}

	@Override
	public Control getControl() {
		return canvas;
	}

	private Rectangle getDrawingBounds(Image image) {
		Rectangle imageBounds = image.getBounds();
		Rectangle canvasBounds = canvas.getBounds();

		double hScale = (double) canvasBounds.width / imageBounds.width;
		double vScale = (double) canvasBounds.height / imageBounds.height;

		double scale = Math.min(1, Math.min(hScale, vScale));

		int width = (int) (imageBounds.width * scale);
		int height = (int) (imageBounds.height * scale);

		int x = (canvasBounds.width - width) / 2;
		int y = (canvasBounds.height - height) / 2;

		return new Rectangle(x, y, width, height);
	}

	private Image getImage(Point point) {
		IGraphicalContentProvider provider = (IGraphicalContentProvider) getContentProvider();
		if (provider == null)
			return null;
		return provider.getImage();
	}

	@Override
	public ISelection getSelection() {
		// no selection supported
		return null;
	}

	@Override
	protected void inputChanged(Object newInput, Object oldInput) {
		refresh();
	}

	public boolean isAdjustToCanvas() {
		return adjustToCanvas;
	}

	private void paintCanvas(GC gc) {
		Image image = getImage(canvas.getSize());
		if (image == null || image.isDisposed())
			return;
		Rectangle drawingBounds = getDrawingBounds(image);
		gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, drawingBounds.x, drawingBounds.y,
				(int)(drawingBounds.width * scale), (int)(drawingBounds.height * scale));
	}

	public void redrawImage() {
		if (getContentProvider() == null)
			return;
		Class<? extends IContentProvider> contentProviderClass = getContentProvider().getClass();
		try {
			setContentProvider((IContentProvider) ConstructorUtils.invokeConstructor(contentProviderClass, new Object[0]));
		} catch (NoSuchMethodException e) {
			Activator.logUnexpected(null, e);
		} catch (IllegalAccessException e) {
			Activator.logUnexpected(null, e);
		} catch (InvocationTargetException e) {
			Activator.logUnexpected(null, e);
		} catch (InstantiationException e) {
			Activator.logUnexpected(null, e);
		}
	}

	protected void redrawImageIfRequested() {
		if (imageRedrawRequested)
			redrawImage();
		imageRedrawRequested = false;
	}

	@Override
	public void refresh() {
		if (!canvas.isDisposed())
			canvas.redraw();
	}

	private void requestImageRedraw() {
		imageRedrawRequested = true;
	}

	public void setAdjustToCanvas(boolean adjustToCanvas) {
		this.adjustToCanvas = adjustToCanvas;
		requestImageRedraw();
	}

	@Override
	public void setContentProvider(IContentProvider contentProvider) {
		if (contentProvider != null) {
			if (!(contentProvider instanceof IGraphicalContentProvider))
				throw new IllegalArgumentException(IGraphicalContentProvider.class.getName());
			if (adjustToCanvas && !canvas.isDisposed())
				((IGraphicalContentProvider) contentProvider).setSuggestedSize(canvas.getSize());
		}
		super.setContentProvider(contentProvider);
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		throw new UnsupportedOperationException();
	}
}