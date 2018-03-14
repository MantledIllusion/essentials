package com.mantledillusion.essentials.vaadin.component;

import java.lang.reflect.Constructor;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;

/**
 * {@link Composite} {@link Component} that is able to handle paging.
 */
public final class PagingSelector extends Composite {

	private static final long serialVersionUID = 1L;
	private static final String TEXT_LEFT = "<";
	private static final String TEXT_RIGHT = ">";

	private static class PagingMenuItem extends MenuItem {

		private static final long serialVersionUID = 1L;

		private int index;

		PagingMenuItem(MenuBar menuBar, String caption, Resource icon, Command command) {
			menuBar.super(caption, icon, command);
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

	}

	private final Command command = new Command() {

		private static final long serialVersionUID = 1L;

		@Override
		public void menuSelected(MenuItem selectedItem) {
			if (selectedItem == rangeBefore) {
				selectedPageRangeAnchorIndex -= displayRange;
				rebuildItems();
			} else if (selectedItem == rangeAfter) {
				selectedPageRangeAnchorIndex += displayRange;
				rebuildItems();
			} else {
				if (selectedItem != rangeSelected) {
					selectedPageIndex = ((PagingMenuItem) selectedItem).getIndex();
					if (rangeSelected != null) {
						restoreCaptionUnselected((PagingMenuItem) rangeSelected);
					}
					rangeSelected = selectedItem;
					restoreCaptionSelected((PagingMenuItem) rangeSelected);
					firePageChosenEvent();
				}
			}
		}
	};

	private MenuItem rangeSelected;
	private MenuItem rangeBefore;
	private MenuItem rangeAfter;

	private final MenuBar bar;

	private int pageSize, entryCount;
	private int displayRange, selectedPageRangeAnchorIndex, selectedPageIndex;

	private boolean showPageDescription = true;

	/**
	 * {@link Constructor}.
	 */
	public PagingSelector() {
		this(10);
	}

	/**
	 * {@link Constructor}.
	 * 
	 * @param pageSize
	 *            The maximum entry count of a single page; has to be 1<=pageSize
	 */
	public PagingSelector(int pageSize) {
		if (pageSize < 1) {
			throw new IllegalArgumentException("The page size must be >=1 !");
		}

		this.pageSize = pageSize;
		this.entryCount = 0;
		this.displayRange = 10;
		this.selectedPageRangeAnchorIndex = 0;
		this.selectedPageIndex = 0;

		this.bar = new MenuBar();
		this.bar.setHtmlContentAllowed(true);
		this.bar.addStyleName(ValoTheme.MENUBAR_SMALL);
		this.bar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

		setCompositionRoot(this.bar);

		rebuildItems();
	}

	// ##################################################################################
	// ################################### MENU ITEMS ###################################
	// ##################################################################################

	private void applyIndexUnselected(PagingMenuItem item, int pageIndex) {
		setDescription(item, pageIndex);
		setCaptionUnselected(item, pageIndex);
		item.setIndex(pageIndex);
	}

	private void restoreCaptionUnselected(PagingMenuItem item) {
		setCaptionUnselected(item, item.getIndex());
	}

	private void setCaptionUnselected(MenuItem item, int pageIndex) {
		item.setText(String.valueOf(pageIndex + 1));
		item.setStyleName(null);
	}

	private void applyIndexSelected(PagingMenuItem item, int pageIndex) {
		setDescription(item, pageIndex);
		setCaptionSelected(item, pageIndex);
		item.setIndex(pageIndex);
	}

	private void restoreCaptionSelected(PagingMenuItem item) {
		setCaptionSelected(item, item.getIndex());
	}

	private void setCaptionSelected(MenuItem item, int pageIndex) {
		item.setText("<b>" + (pageIndex + 1) + "</b>");
		item.setStyleName("highlight");
	}

	private void setDescription(MenuItem item, int pageIndex) {
		if (this.showPageDescription) {
			int lastPageOnSite = (pageIndex + 1) * this.pageSize;
			String itemRange = String.valueOf((lastPageOnSite - this.pageSize) + 1) + "-"
					+ String.valueOf(lastPageOnSite);
			item.setDescription(itemRange);
		} else {
			item.setDescription(null);
		}
	}

	// ##################################################################################
	// #################################### REBUILD #####################################
	// ##################################################################################

	private void rebuildItems() {
		this.bar.removeItems();

		int pageCount = getPageCount();
		int pageCountOnCurrentRange = Math.min(pageCount - this.selectedPageRangeAnchorIndex, this.displayRange);

		this.rangeBefore = this.bar.addItem(TEXT_LEFT, this.command);
		this.rangeBefore.setEnabled(this.selectedPageRangeAnchorIndex > 0);

		for (int i = 0; i < pageCountOnCurrentRange; i++) {
			int pageIndex = this.selectedPageRangeAnchorIndex + i;

			if (pageIndex == this.selectedPageIndex) {
				this.rangeSelected = new PagingMenuItem(this.bar, "-", null, this.command);
				this.bar.getItems().add(this.rangeSelected);
				applyIndexSelected((PagingMenuItem) this.rangeSelected, pageIndex);

			} else {
				PagingMenuItem newItem = new PagingMenuItem(this.bar, "-", null, this.command);
				this.bar.getItems().add(newItem);
				applyIndexUnselected(newItem, pageIndex);
			}
		}

		this.rangeAfter = this.bar.addItem(TEXT_RIGHT, this.command);
		this.rangeAfter.setEnabled(this.selectedPageRangeAnchorIndex + this.displayRange < pageCount);
	}

	// ##################################################################################
	// ############################# GETTERS AND SETTERS ################################
	// ##################################################################################

	/**
	 * Returns the count of pages in context of the currently set entry count and
	 * page size.
	 * 
	 * @return The page count
	 */
	public int getPageCount() {
		return entryCount == 0 ? 1 : entryCount / pageSize + (entryCount % pageSize > 0 ? 1 : 0);
	}

	/**
	 * Returns the index of the currently selected page.
	 * 
	 * @return The selected page index
	 */
	public int getSelectedPage() {
		return selectedPageIndex;
	}

	/**
	 * Sets the index of the currently selected page.
	 * 
	 * @param pageIndex
	 *            The selected page index; has to be
	 *            0<=pageIndex<{@link #getPageCount()}
	 */
	public void setSelectedPage(int pageIndex) {
		if (pageIndex < 0) {
			throw new IndexOutOfBoundsException("Unable to set the selected page to a negative index!");
		} else if (pageIndex != this.selectedPageIndex) {
			int pageCount = getPageCount();
			if (pageIndex >= pageCount) {
				throw new IndexOutOfBoundsException("Unable to set the selected page to index " + pageIndex
						+ ", max allowed index is " + (pageCount - 1));
			}

			this.selectedPageIndex = pageIndex;
			rebuildItems();
			firePageChosenEvent();
		}
	}

	/**
	 * Returns the index of the first entry of the currently selected page.
	 * 
	 * @return The index of the first entry of the currently selected page.
	 */
	public int getSelectedPageEntryIndexFrom() {
		return this.selectedPageIndex * this.pageSize;
	}

	/**
	 * Returns the entry count of the currently selected page.
	 * 
	 * @return The size of the currently selected page.
	 */
	public int getSelectedPageSize() {
		return Math.min(entryCount - getSelectedPageEntryIndexFrom(), pageSize);
	}

	/**
	 * Sets the overall entry count to page.
	 * <P>
	 * NOTE: Setting this value causes the {@link PagingSelector} to recalculate its
	 * pages, which will result in the first page to be selected.
	 * 
	 * @param entryCount
	 *            The count of the entries to page; has to be 0<=entryCount.
	 */
	public void setEntryCount(int entryCount) {
		if (entryCount < 0) {
			throw new RuntimeException("The entry count cannot be negative!");
		}

		this.entryCount = entryCount;
		this.selectedPageRangeAnchorIndex = 0;
		this.selectedPageIndex = 0;

		rebuildItems();
		firePageChosenEvent();
	}

	/**
	 * Determines whether to show a range description on every page.
	 * 
	 * @param showPageDescription
	 *            True if a range description should be shown on the pages, false
	 *            othwerwise
	 */
	public void setShowPageDescription(boolean showPageDescription) {
		this.showPageDescription = showPageDescription;
		rebuildItems();
	}

	// ##################################################################################
	// ################################### EVENTS #######################################
	// ##################################################################################

	private void firePageChosenEvent() {
		fireEvent(new PageChosenEvent(PagingSelector.this, selectedPageIndex, selectedPageIndex * pageSize,
				getSelectedPageSize()));
	}

	/**
	 * Event that is thrown when a page is selected.
	 */
	public static final class PageChosenEvent extends Event {

		private static final long serialVersionUID = 1L;

		private final int page;
		private final int entryIndexFrom;
		private final int pageSize;

		private PageChosenEvent(PagingSelector component, int page, int entryIndexFrom, int pageSize) {
			super(component);
			this.page = page;
			this.entryIndexFrom = entryIndexFrom;
			this.pageSize = pageSize;
		}

		/**
		 * Returns the page that was selected.
		 * <p>
		 * NOTE: similar to {@link PagingSelector#getSelectedPage()}.
		 * 
		 * @return The page index
		 */
		public int getPage() {
			return page;
		}

		/**
		 * Returns the index of the first entry of the selected page.
		 * <p>
		 * NOTE: similar to {@link PagingSelector#getSelectedPageEntryIndexFrom()}
		 * 
		 * @return The index of the first entry of the page
		 */
		public int getEntryIndexFrom() {
			return entryIndexFrom;
		}

		/**
		 * Returns the entry count of the selected page.
		 * <p>
		 * NOTE: similar to {@link PagingSelector#getSelectedPageSize()}
		 * 
		 * @return The page size
		 */
		public int getPageSize() {
			return pageSize;
		}
	}
}
