package org.coolreader.crengine;

import java.util.ArrayList;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.CoverpageManager.CoverpageReadyListener;
import org.coolreader.db.CRDBService;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devsmart.android.ui.HorizontalListView;

public class CRRootView extends ViewGroup {

	public static final Logger log = L.create("cr");

	private final CoolReader mActivity;
	private ViewGroup mView;
	private HorizontalListView mRecentBooksScroll;
	private CoverpageManager mCoverpageManager;
	private int coverWidth;
	private int coverHeight;
	private BookInfo currentBook;
	private CoverpageReadyListener coverpageListener;
	public CRRootView(CoolReader activity) {
		super(activity);
		this.mActivity = activity;
		this.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.mCoverpageManager = Services.getCoverpageManager();


		int screenHeight = mActivity.getWindowManager().getDefaultDisplay().getHeight();
		int screenWidth = mActivity.getWindowManager().getDefaultDisplay().getWidth();
		int h = screenHeight / 4;
		int w = screenWidth / 4;
		if (h > w)
			h = w;
    	w = h * 3 / 4;
    	coverWidth = w;
    	coverHeight = h;
		createViews();
		BackgroundThread.instance().postGUI(new Runnable() {
			@Override
			public void run() {
				Services.getHistory().getOrLoadRecentBooks(new CRDBService.RecentBooksLoadingCallback() {
					@Override
					public void onRecentBooksListLoaded(ArrayList<BookInfo> bookList) {
						updateCurrentBook(bookList != null && bookList.size() > 0 ? bookList.get(0) : null);
						updateRecentBooks(bookList);
					}
				});
			}
		});

		coverpageListener =	new CoverpageReadyListener() {
			@Override
			public void onCoverpagesReady(ArrayList<CoverpageManager.ImageItem> files) {
				mView.invalidate();
			}
		};
		this.mCoverpageManager.addCoverpageReadyListener(coverpageListener);
		super.onAttachedToWindow();
	}
	
	public void onClose() {
		this.mCoverpageManager.removeCoverpageReadyListener(coverpageListener);
		coverpageListener = null;
		super.onDetachedFromWindow();
	}

	private void setBookInfoItem(ViewGroup baseView, int viewId, String value) {
		TextView view = (TextView)baseView.findViewById(viewId);
		if (view != null) {
			if (value != null && value.length() > 0) {
				view.setText(value);
			} else {
				view.setText("");
			}
		}
	}
	
	private void updateCurrentBook(BookInfo book) {
    	currentBook = book;
    	
    	// set current book cover page
		ImageView cover = (ImageView)mView.findViewById(R.id.book_cover);
		if (currentBook != null) {
			cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(currentBook.getFileInfo(), coverWidth, coverHeight));
			cover.setMinimumHeight(coverHeight);
			cover.setMinimumWidth(coverWidth);
			cover.setMaxHeight(coverHeight);
			cover.setMaxWidth(coverWidth);
			
			setBookInfoItem(mView, R.id.lbl_book_author, Utils.formatAuthors(currentBook.getFileInfo().authors));
			setBookInfoItem(mView, R.id.lbl_book_title, currentBook.getFileInfo().title);
			setBookInfoItem(mView, R.id.lbl_book_series, Utils.formatSeries(currentBook.getFileInfo().series, currentBook.getFileInfo().seriesNumber));
		} else {
			log.w("No current book in history");
			cover.setImageDrawable(null);
			cover.setMinimumHeight(0);
			cover.setMinimumWidth(0);
			cover.setMaxHeight(0);
			cover.setMaxWidth(0);

			setBookInfoItem(mView, R.id.lbl_book_author, "");
			setBookInfoItem(mView, R.id.lbl_book_title, "No last book"); // TODO: i18n
			setBookInfoItem(mView, R.id.lbl_book_series, "");
		}
	}	
	
	private ArrayList<BookInfo> mRecentBooks = new ArrayList<BookInfo>();
	private void updateRecentBooks(ArrayList<BookInfo> books) {
		mRecentBooks = new ArrayList<BookInfo>();
		for (BookInfo bi : books)
			mRecentBooks.add(new BookInfo(bi));
		mRecentBooksScroll.setAdapter(new BaseListAdapter() {

			@Override
			public boolean areAllItemsEnabled() {
				return true;
			}

			@Override
			public boolean isEnabled(int position) {
				return true;
			}

			@Override
			public int getCount() {
				return mRecentBooks.size() > 1 ? mRecentBooks.size() - 1 : 0;
			}

			@Override
			public Object getItem(int position) {
				return mRecentBooks.get(position - 1);
			}

			@Override
			public long getItemId(int position) {
				return position + 1;
			}

			@Override
			public int getItemViewType(int position) {
				return 0;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					LayoutInflater inflater = LayoutInflater.from(mActivity);
					View view = inflater.inflate(R.layout.root_item_recent_book, null);
					convertView = (ViewGroup)view;
				}
				ImageView cover = (ImageView)convertView.findViewById(R.id.book_cover);
				TextView label = (TextView)convertView.findViewById(R.id.book_name);
				BookInfo book = mRecentBooks.get(position + 1);
				cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(book.getFileInfo(), coverWidth, coverHeight));
				cover.setMinimumHeight(coverHeight);
				cover.setMaxHeight(coverHeight);
				cover.setMinimumWidth(coverWidth);
				cover.setMaxWidth(coverWidth);
				if (label != null) {
					String title = book.getFileInfo().title;
					String authors = Utils.formatAuthors(book.getFileInfo().authors);
					String s = book.getFileInfo().getFileNameToDisplay();
					if (!Utils.empty(title) && !Utils.empty(authors))
						s = title + " - " + authors;
					else if (!Utils.empty(title))
						s = title;
					else if (!Utils.empty(authors))
						s = authors;
					label.setText(s != null ? s : "");
					label.setMaxWidth(coverWidth);
				}
				return convertView;
			}

			@Override
			public int getViewTypeCount() {
				return 1;
			}

			@Override
			public boolean hasStableIds() {
				return true;
			}

			@Override
			public boolean isEmpty() {
				return mRecentBooks.size() <= 1;
			}
		
		});
	}
	
	private void createViews() {
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		View view = inflater.inflate(R.layout.root_window, null);
		mView = (ViewGroup)view;
		
		updateCurrentBook(Services.getHistory().getLastBook());
		
		LinearLayout recentLayout = (LinearLayout)mView.findViewById(R.id.scroll_recent_books);
		recentLayout.removeAllViews();
		mRecentBooksScroll = new HorizontalListView(mActivity, null);
		mRecentBooksScroll.setMinimumHeight(coverHeight);
		mRecentBooksScroll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		recentLayout.addView(mRecentBooksScroll);

		removeAllViews();
		addView(mView);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		mView.layout(l, t, r, b);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		mView.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mView.getMeasuredWidth(), mView.getMeasuredHeight());
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}
}