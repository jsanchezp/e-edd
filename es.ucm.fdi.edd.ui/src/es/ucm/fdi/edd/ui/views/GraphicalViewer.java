package es.ucm.fdi.edd.ui.views;

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
import org.eclipse.swt.graphics.ImageData;
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
	private boolean fitWindow = false;
	private Point origin = new Point (0, 0);

	public GraphicalViewer(Composite parent) {
		canvas = new Canvas(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BACKGROUND);
		parent.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				requestImageRedraw();
				syncScrollBars();
			}
		});
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				redrawImageIfRequested();
				GC gc = e.gc;
				if (fitWindow) {
					paintFitCanvas(gc);
				}
				else {
					paintCanvas(gc);
				}
				
				Rectangle rect = canvas.getBounds ();
				Rectangle client = canvas.getClientArea ();
				int marginWidth = client.width - rect.width;
				if (marginWidth > 0) {
					gc.fillRectangle (rect.width, 0, marginWidth, client.height);
				}
				int marginHeight = client.height - rect.height;
				if (marginHeight > 0) {
					gc.fillRectangle (0, rect.height, client.width, marginHeight);
				}
			}
		});
		initScrollBars();
	}
	
	/* Initalize the scrollbar and register listeners. */
	private void initScrollBars() {
		Rectangle rect = canvas.getBounds();
		Rectangle client = canvas.getClientArea ();
	
		ScrollBar horizontal = canvas.getHorizontalBar();
		horizontal.setEnabled(false);
		horizontal.setThumb(Math.min (rect.width, client.width));
		horizontal.setMaximum (rect.width);
		horizontal.setMinimum(0);
		horizontal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				scrollHorizontally((ScrollBar) event.widget);
			}
		});
		ScrollBar vertical = canvas.getVerticalBar();
		vertical.setEnabled(false);
		vertical.setThumb (Math.min (rect.height, client.height));
		vertical.setMaximum (rect.height);
	    vertical.setMinimum(0);
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
		Rectangle rect = canvas.getBounds();
		canvas.scroll(destX, 0, 0, 0, rect.width, rect.height, false);
		origin.x = -hSelection;
		
		syncScrollBars();
	}

	/* Scroll vertically */
	private void scrollVertically(ScrollBar scrollBar) {
		int vSelection = scrollBar.getSelection ();
		int destY = -vSelection - origin.y;
		Rectangle rect = canvas.getBounds();
		canvas.scroll(0, destY, 0, 0, rect.width, rect.height, false);
		origin.y = -vSelection; 
		
		syncScrollBars();
	}
	
	private void syncScrollBars() {
		Rectangle bounds = canvas.getBounds();
		Rectangle clientArea = canvas.getClientArea();
		
		ScrollBar horizontal = canvas.getHorizontalBar();
		horizontal.setIncrement((int) (clientArea.width / 100));
		horizontal.setPageIncrement(clientArea.width);
		
		if (bounds.width > clientArea.width) { /* image is wider than client area */
			horizontal.setEnabled(true);
		} else { /* image is narrower than client area */
			horizontal.setEnabled(false);
		}
		
		ScrollBar vertical = canvas.getVerticalBar();
		vertical.setIncrement((int) (clientArea.height / 100));
		vertical.setPageIncrement((int) (clientArea.height));
		
		if (bounds.height > clientArea.height) { /* image is higher than client area */
			vertical.setEnabled(true);
		} else { /* image is less higher than client area */
			vertical.setEnabled(false);
		}
	}
	
	public Canvas getCanvas() {
		return canvas;
	}
	
	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
		fitWindow = false;
		refresh();
		
		syncScrollBars();
	}

	@Override
	public Control getControl() {
		return canvas;
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
//		gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, drawingBounds.x, drawingBounds.y,
//				(int)(drawingBounds.width * scale), (int)(drawingBounds.height * scale));
		gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 
				origin.x, origin.y, (int)(drawingBounds.width * scale), (int)(drawingBounds.height * scale));
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
	
	private void paintFitCanvas(GC gc) {
		Image image = getImage(canvas.getSize());
		if (image == null || image.isDisposed())
			return;
		Rectangle imageBound = image.getBounds();
		Rectangle destRect = canvas.getClientArea();
		double hRatio = (double) destRect.width / (double) imageBound.width;
		double vRatio = (double) destRect.height / (double) imageBound.height;
		double ratio = Math.min(hRatio, vRatio);
//		double center_x = ( destRect.width - imageBound.width*ratio ) / 2;
//		double center_y = ( destRect.height - imageBound.height*ratio ) / 2;  
//		gc.drawImage(image, 0, 0, imageBound.width, imageBound.height, 
//				(int)center_x, (int)center_y, (int)(imageBound.width * ratio), (int)(imageBound.height * ratio));
		gc.drawImage(image, 0, 0, imageBound.width, imageBound.height, 
				0, 0, (int)(imageBound.width * ratio), (int)(imageBound.height * ratio));
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
		if (!canvas.isDisposed()) {
			canvas.redraw();
		}
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

	public void setFitCanvas(boolean fit) {
		fitWindow = fit;
		refresh();
	}
	
	public Image getImage() {
//		return image;
		return null;
	}
	
	public ImageData getImageData() {
//		return image.getImageData();
		return null;
	}
	
	public void setImageData(ImageData dest) {
//		image = new Image(canvas.getDisplay(), dest);
//		refresh();
	}
}